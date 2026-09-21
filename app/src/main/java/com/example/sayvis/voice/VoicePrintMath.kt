package com.example.sayvis.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin DSP toolkit behind the floating avatar's ambient listening and
 * owner-voice recognition.
 *
 * The whole pipeline runs on-device and on the JVM (so it is unit-testable):
 *
 *  1. PCM frames (16 kHz mono) -> RMS/dBFS level with an adaptive noise floor (VAD).
 *  2. Voiced frames -> a compact feature vector: 24 log-mel band energies plus a
 *     normalised pitch (F0) and spectral-centroid component, L2-normalised so that
 *     recording gain cancels out.
 *  3. A short utterance -> mean of the top per-frame cosine similarities against the
 *     owner's enrolled templates.
 *
 * This is deliberately a lightweight, explainable speaker matcher — not a
 * production-grade biometric system — and it never leaves the device.
 */
object VoicePrintMath {

    const val SAMPLE_RATE = 16000
    const val FRAME_SIZE = 1024 // 64 ms
    const val HOP_SIZE = 512    // 32 ms
    const val MEL_BANDS = 24
    const val F0_MIN_HZ = 60f
    const val F0_MAX_HZ = 400f
    const val FEATURE_DIM = MEL_BANDS + 2 // mel bands + normalised F0 + centroid

    /** Minimum normalised autocorrelation peak for a frame to count as voiced. */
    const val VOICED_CORRELATION = 0.30f

    private val hann: FloatArray by lazy {
        FloatArray(FRAME_SIZE) { i -> (0.5f * (1f - cos(2.0 * PI * i / (FRAME_SIZE - 1))).toFloat()) }
    }

    private val filterbank: Array<FloatArray> by lazy { buildMelBank() }

    // ------------------------------------------------------------------- level

    fun rms(frame: FloatArray): Float {
        var sum = 0.0
        for (v in frame) sum += v.toDouble() * v.toDouble()
        return sqrt((sum / frame.size)).toFloat()
    }

    /** dBFS of an RMS value, floored at -100 dB so silence stays representable. */
    fun dbfs(rmsLevel: Float): Float =
        max(-100f, 20f * log10(max(rmsLevel, 1e-10f)))

    // --------------------------------------------------------------------- FFT

    /** In-place iterative radix-2 Cooley–Tukey FFT; [real].size must be a power of two. */
    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        // Bit-reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val re = real[i]; real[i] = real[j]; real[j] = re
                val im = imag[i]; imag[i] = imag[j]; imag[j] = im
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val stepRe = cos(ang).toFloat()
            val stepIm = sin(ang).toFloat()
            val half = len shr 1
            var blockStart = 0
            while (blockStart < n) {
                var curRe = 1f
                var curIm = 0f
                for (k in 0 until half) {
                    val aRe = real[blockStart + k]
                    val aIm = imag[blockStart + k]
                    val tRe = real[blockStart + k + half] * curRe - imag[blockStart + k + half] * curIm
                    val tIm = real[blockStart + k + half] * curIm + imag[blockStart + k + half] * curRe
                    real[blockStart + k] = aRe + tRe
                    imag[blockStart + k] = aIm + tIm
                    real[blockStart + k + half] = aRe - tRe
                    imag[blockStart + k + half] = aIm - tIm
                    val nextRe = curRe * stepRe - curIm * stepIm
                    curIm = curRe * stepIm + curIm * stepRe
                    curRe = nextRe
                }
                blockStart += len
            }
            len = len shl 1
        }
    }

    // -------------------------------------------------------------- filterbank

    private fun hzToMel(hz: Float): Float = 2595f * log10(1f + hz / 700f)
    private fun melToHz(mel: Float): Float = 700f * (10f.pow(mel / 2595f) - 1f)

    private fun buildMelBank(): Array<FloatArray> {
        val bins = FRAME_SIZE / 2 + 1
        val bank = Array(MEL_BANDS) { FloatArray(bins) }
        val melLow = hzToMel(F0_MIN_HZ)
        val melHigh = hzToMel(7600f)
        val centerHz = FloatArray(MEL_BANDS + 2) { b ->
            melToHz(melLow + (melHigh - melLow) * b / (MEL_BANDS + 1))
        }
        for (b in 0 until MEL_BANDS) {
            val left = centerHz[b]
            val center = centerHz[b + 1]
            val right = centerHz[b + 2]
            for (i in 0 until bins) {
                val f = i * SAMPLE_RATE.toFloat() / FRAME_SIZE
                bank[b][i] = when {
                    f <= left || f >= right -> 0f
                    f <= center -> (f - left) / max(center - left, 1e-6f)
                    else -> (right - f) / max(right - center, 1e-6f)
                }
            }
        }
        return bank
    }

    // ---------------------------------------------------------------- features

    /**
     * Feature vector for one frame, or `null` when the frame carries no usable
     * voiced energy (silence, breath, sibilants…). Only voiced frames belong in a
     * voice-print, so callers simply skip the nulls.
     */
    fun frameFeature(frame: FloatArray): FloatArray? {
        if (frame.size != FRAME_SIZE) return null
        var mean = 0f
        for (v in frame) mean += v
        mean /= frame.size

        val re = FloatArray(FRAME_SIZE)
        val im = FloatArray(FRAME_SIZE)
        for (i in 0 until FRAME_SIZE) re[i] = (frame[i] - mean) * hann[i]
        fft(re, im)

        val bins = FRAME_SIZE / 2 + 1
        val feature = FloatArray(FEATURE_DIM)
        var totalMag = 0f
        var centroidNum = 0f
        for (i in 0 until bins) {
            val m = sqrt(re[i] * re[i] + im[i] * im[i])
            totalMag += m
            centroidNum += m * i
        }
        if (totalMag < 1e-4f) return null

        for (b in 0 until MEL_BANDS) {
            var energy = 0f
            val bankRow = filterbank[b]
            for (i in 0 until bins) energy += bankRow[i] * sqrt(re[i] * re[i] + im[i] * im[i])
            feature[b] = ln(1f + energy)
        }

        val f0 = estimateF0(frame)
        if (f0 <= 0f) return null
        feature[MEL_BANDS] = ((f0 - F0_MIN_HZ) / (F0_MAX_HZ - F0_MIN_HZ)) * 2f
        feature[MEL_BANDS + 1] = (centroidNum / totalMag / bins) * 2f

        return l2normalize(feature)
    }

    /** Autocorrelation pitch estimate; 0 when the frame is unvoiced. */
    fun estimateF0(frame: FloatArray): Float {
        var mean = 0f
        for (v in frame) mean += v
        mean /= frame.size
        var energy = 0f
        val x = FloatArray(frame.size)
        for (i in frame.indices) {
            x[i] = frame[i] - mean
            energy += x[i] * x[i]
        }
        if (energy < 1e-6f) return 0f

        val minLag = max(1, (SAMPLE_RATE / F0_MAX_HZ).toInt())
        val maxLag = (SAMPLE_RATE / F0_MIN_HZ).toInt().coerceAtMost(frame.size - 1)
        var bestLag = -1
        var bestVal = 0f
        for (lag in minLag..maxLag) {
            var sum = 0f
            for (i in 0 until frame.size - lag) sum += x[i] * x[i + lag]
            val normalised = sum / energy
            if (normalised > bestVal) {
                bestVal = normalised
                bestLag = lag
            }
        }
        if (bestLag <= 0 || bestVal < VOICED_CORRELATION) return 0f
        return SAMPLE_RATE.toFloat() / bestLag
    }

    // ------------------------------------------------------------ similarity

    fun l2normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm > 1e-9f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na <= 1e-12f || nb <= 1e-12f) return 0f
        return (dot / (sqrt(na) * sqrt(nb))).coerceIn(-1f, 1f)
    }

    /**
     * Score of one utterance against the enrolled templates: mean of the top 60 %
     * of per-frame best cosine similarities — robust to a few noisy frames.
     */
    fun utteranceScore(features: List<FloatArray>, templates: List<FloatArray>): Float {
        if (features.isEmpty() || templates.isEmpty()) return 0f
        val sims = FloatArray(features.size)
        for (i in features.indices) {
            var best = 0f
            for (t in templates) {
                val c = cosine(features[i], t)
                if (c > best) best = c
            }
            sims[i] = best
        }
        sims.sort()
        val take = max(1, (features.size * 0.6f).toInt())
        var sum = 0f
        for (i in sims.size - take until sims.size) sum += sims[i]
        return sum / take
    }

    /** One averaged template from every voiced feature of a recording. */
    fun meanTemplate(features: List<FloatArray>): FloatArray? {
        if (features.isEmpty()) return null
        val mean = FloatArray(FEATURE_DIM)
        for (f in features) {
            for (i in mean.indices) mean[i] += f[i]
        }
        for (i in mean.indices) mean[i] /= features.size
        return l2normalize(mean)
    }

    /** All voiced features inside a raw PCM recording (windowed analysis frames). */
    fun extractVoicedFeatures(pcm: FloatArray): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        if (pcm.size < FRAME_SIZE) return out
        var start = 0
        while (start + FRAME_SIZE <= pcm.size) {
            val frame = pcm.copyOfRange(start, start + FRAME_SIZE)
            if (dbfs(rms(frame)) > -45f) {
                val feature = frameFeature(frame)
                if (feature != null) out.add(feature)
            }
            start += HOP_SIZE
        }
        return out
    }

    /** Helper used by tests and by the enrollment summary row. */
    fun percentileLabel(score: Float): Int = (score.coerceIn(0f, 1f) * 100f).toInt()
}
