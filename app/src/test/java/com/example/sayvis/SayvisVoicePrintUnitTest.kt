package com.example.sayvis

import com.example.sayvis.voice.VoicePrintMath
import com.example.sayvis.voice.VoicePrintStore
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the pure on-device DSP behind the floating avatar: the FFT, the
 * voice-print feature pipeline, cosine-based utterance scoring and the tiny
 * template serialisation — all without touching any Android API.
 */
class SayvisVoicePrintUnitTest {

    private val sampleRate = VoicePrintMath.SAMPLE_RATE

    private fun sine(freqHz: Double, cyclesMs: Int, amplitude: Double = 0.4): FloatArray {
        val n = sampleRate * cyclesMs / 1000
        return FloatArray(n) { i -> (amplitude * sin(2.0 * PI * freqHz * i / sampleRate)).toFloat() }
    }

    /** Speech-like harmonic stack: fundamental + decaying overtones, slowly enveloped. */
    private fun harmonicVoice(f0Hz: Double, cyclesMs: Int, seed: Int, tilt: Double = 0.6): FloatArray {
        val rng = Random(seed)
        val n = sampleRate * cyclesMs / 1000
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            var v = 0.0
            for (harm in 1..6) {
                val amp = tilt.pow(harm - 1) / harm
                v += amp * sin(2.0 * PI * f0Hz * harm * t + rng.nextDouble() * 0.05)
            }
            // gentle syllable envelope so silence gaps exist
            val envelope = 0.55 + 0.45 * sin(2.0 * PI * 1.7 * t)
            out[i] = (v * envelope * 0.3).toFloat()
        }
        return out
    }

    private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())

    // ------------------------------------------------------------------- FFT

    @Test
    fun `fft peaks at the correct bin for a pure sine`() {
        val n = VoicePrintMath.FRAME_SIZE
        val toneHz = 250.0 // 250 Hz at 16 kHz over 1024 samples -> bin 16
        val real = FloatArray(n) { i -> (0.8 * sin(2.0 * PI * toneHz * i / sampleRate)).toFloat() }
        val imag = FloatArray(n)
        VoicePrintMath.fft(real, imag)

        val magnitudes = FloatArray(n / 2) { i -> real[i] * real[i] + imag[i] * imag[i] }
        var bestBin = 0
        for (i in magnitudes.indices) if (magnitudes[i] > magnitudes[bestBin]) bestBin = i
        assertEquals(16, bestBin)
    }

    // --------------------------------------------------------------- features

    @Test
    fun `pitch estimator recovers the fundamental of a voiced frame`() {
        val pcm = sine(120.0, cyclesMs = 200)
        val frame = pcm.copyOfRange(0, VoicePrintMath.FRAME_SIZE)
        val f0 = VoicePrintMath.estimateF0(frame)
        assertTrue("expected ~120 Hz, got $f0", f0 in 110f..132f)
    }

    @Test
    fun `silent frame yields no pitch`() {
        val frame = FloatArray(VoicePrintMath.FRAME_SIZE)
        assertEquals(0f, VoicePrintMath.estimateF0(frame))
    }

    @Test
    fun `frame feature is unit normalised`() {
        val pcm = harmonicVoice(140.0, cyclesMs = 100, seed = 1)
        val frame = pcm.copyOfRange(0, VoicePrintMath.FRAME_SIZE)
        val feature = VoicePrintMath.frameFeature(frame)
        assertNotNull(feature)
        var sum = 0f
        for (v in feature!!) sum += v * v
        assertEquals(1f, kotlin.math.sqrt(sum), 1e-3f)
    }

    @Test
    fun `quiet frame has no feature`() {
        val quiet = FloatArray(VoicePrintMath.FRAME_SIZE) { 1e-9f * it }
        assertNull(VoicePrintMath.frameFeature(quiet))
    }

    // ------------------------------------------------------------- similarity

    @Test
    fun `cosine of identical vectors is one`() {
        val a = floatArrayOf(0.3f, -0.5f, 0.8f, 0.1f)
        val b = a.copyOf()
        assertEquals(1f, VoicePrintMath.cosine(a, b), 1e-5f)
    }

    @Test
    fun `utterance score is high for the same speaker and lower across speakers`() {
        val ownerA = VoicePrintMath.extractVoicedFeatures(harmonicVoice(120.0, cyclesMs = 1200, seed = 7))
        val ownerB = VoicePrintMath.extractVoicedFeatures(harmonicVoice(122.0, cyclesMs = 1200, seed = 8))
        val stranger = VoicePrintMath.extractVoicedFeatures(harmonicVoice(320.0, cyclesMs = 1200, seed = 9, tilt = 0.08))

        val templateA = VoicePrintMath.meanTemplate(ownerA)
        val templateB = VoicePrintMath.meanTemplate(ownerB)
        assertNotNull(templateA)
        assertNotNull(templateB)

        val selfScore = VoicePrintMath.utteranceScore(ownerB, listOf(templateA!!, templateB!!))
        val crossScore = VoicePrintMath.utteranceScore(stranger, listOf(templateA, templateB))

        assertTrue("self=$selfScore should exceed cross=$crossScore", selfScore > crossScore)
        assertTrue("self score should be strong: $selfScore", selfScore > 0.90f)
        assertTrue("default threshold should separate speakers", crossScore < VoicePrintStore.DEFAULT_THRESHOLD)
    }

    @Test
    fun `enrollment from a sample produces a usable template`() {
        val pcm = harmonicVoice(150.0, cyclesMs = 3500, seed = 21)
        val template = VoicePrintMath.meanTemplate(VoicePrintMath.extractVoicedFeatures(pcm))
        assertNotNull(template)
        assertEquals(VoicePrintMath.FEATURE_DIM, template!!.size)

        // The same speaker's new utterance must match the print strongly.
        val again = VoicePrintMath.extractVoicedFeatures(harmonicVoice(150.0, cyclesMs = 1200, seed = 22))
        assertTrue(VoicePrintMath.utteranceScore(again, listOf(template)) > 0.90f)
    }

    // --------------------------------------------------------- serialisation

    @Test
    fun `template encoding round-trips bit-exactly`() {
        val rng = Random(42)
        val templates = listOf(
            FloatArray(VoicePrintMath.FEATURE_DIM) { rng.nextFloat() },
            FloatArray(VoicePrintMath.FEATURE_DIM) { rng.nextFloat() },
            FloatArray(VoicePrintMath.FEATURE_DIM) { rng.nextFloat() }
        )
        val blob = VoicePrintStore.encodeTemplates(templates)
        val decoded = VoicePrintStore.decodeTemplates(blob)
        assertEquals(templates.size, decoded.size)
        for (t in templates.indices) {
            assertNotEquals(0, decoded[t].size)
            for (d in templates[t].indices) {
                assertEquals(templates[t][d], decoded[t][d], 0f)
            }
        }
    }

    @Test
    fun `decoding garbage yields an empty template list`() {
        assertTrue(VoicePrintStore.decodeTemplates("not,a|print").isEmpty())
        assertTrue(VoicePrintStore.decodeTemplates("").isEmpty())
    }
}
