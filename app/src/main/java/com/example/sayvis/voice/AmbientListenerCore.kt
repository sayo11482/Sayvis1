package com.example.sayvis.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * The live microphone loop behind the floating avatar.
 *
 * Reads 32 ms hops of 16 kHz mono PCM and:
 *  - publishes a smoothed 0..1 ambient level (drives the avatar pulse);
 *  - runs a simple adaptive-noise-floor VAD;
 *  - extracts features from voiced frames and accumulates them into utterances;
 *  - at the end of each utterance scores it against the owner's voice-print and
 *    fires [onOwnerVoice] (debounced to at most one hit every [DETECT_DEBOUNCE_MS]).
 *
 * Nothing is recorded to disk and nothing leaves the process.
 */
class AmbientListenerCore(
    private val store: VoicePrintStore,
    private val onLevel: (Float) -> Unit,
    private val onOwnerVoice: (Float) -> Unit
) {

    @Volatile
    private var running = false

    private var record: AudioRecord? = null
    private var worker: Thread? = null

    private var noiseFloorDb = -55f
    private var lastLevelPublishAt = 0L
    private var lastDetectAt = 0L
    private var silenceHops = 0
    private val utterance = ArrayList<FloatArray>(64)

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission") // the service refuses to start without RECORD_AUDIO
    fun start(): Boolean {
        if (running) return true
        val minBuffer = AudioRecord.getMinBufferSize(
            VoicePrintMath.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        return try {
            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                VoicePrintMath.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuffer * 2, VoicePrintMath.FRAME_SIZE * 4)
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                false
            } else {
                record = rec
                running = true
                rec.startRecording()
                worker = thread(name = "sayvis-ambient-listen", isDaemon = true) { loop() }
                true
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            false
        }
    }

    fun stop() {
        running = false
        worker?.let { w ->
            runCatching { w.join(1_000) }
        }
        worker = null
        record?.let { rec ->
            runCatching { rec.stop() }
            rec.release()
        }
        record = null
    }

    private fun loop() {
        val hop = ShortArray(VoicePrintMath.HOP_SIZE)
        val buffer = FloatArray(VoicePrintMath.FRAME_SIZE)
        while (running) {
            val rec = record ?: break
            val n = runCatching { rec.read(hop, 0, hop.size) }.getOrDefault(-1)
            if (n <= 0) continue
            // Slide the analysis window and append the freshly read hop.
            System.arraycopy(buffer, n, buffer, 0, buffer.size - n)
            for (i in 0 until n) buffer[buffer.size - n + i] = hop[i] / 32768f
            processFrame(buffer)
        }
    }

    private fun processFrame(frame: FloatArray) {
        val levelDb = VoicePrintMath.dbfs(VoicePrintMath.rms(frame))

        // Adaptive noise floor: falls fast towards quiet floors, creeps up slowly.
        noiseFloorDb = if (levelDb < noiseFloorDb) {
            0.9f * noiseFloorDb + 0.1f * levelDb
        } else {
            min(noiseFloorDb + 0.01f, -35f)
        }

        // 0..1 amplitude for the pulse; -45 dB maps to 0.
        val amplitude = ((levelDb + 45f) / 45f).coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        if (now - lastLevelPublishAt > LEVEL_PUBLISH_INTERVAL_MS) {
            lastLevelPublishAt = now
            onLevel(amplitude)
        }

        val voiced = levelDb > max(noiseFloorDb + 9f, -42f)
        if (voiced) {
            silenceHops = 0
            val feature = VoicePrintMath.frameFeature(frame)
            if (feature != null) utterance.add(feature)
            if (utterance.size >= MAX_UTTERANCE_FEATURES) finishUtterance()
        } else if (utterance.isNotEmpty()) {
            silenceHops++
            if (silenceHops >= UTTERANCE_GAP_HOPS) finishUtterance()
        }
    }

    private fun finishUtterance() {
        val features = ArrayList(utterance)
        utterance.clear()
        silenceHops = 0

        val templates = store.enrolledTemplates()
        if (features.size < MIN_UTTERANCE_FEATURES || templates.isEmpty()) return

        val score = VoicePrintMath.utteranceScore(features, templates)
        if (score >= store.threshold()) {
            val now = System.currentTimeMillis()
            if (now - lastDetectAt >= DETECT_DEBOUNCE_MS) {
                lastDetectAt = now
                onOwnerVoice(score)
            }
        }
    }

    companion object {
        /** Silence gap that closes an utterance (≈290 ms). */
        const val UTTERANCE_GAP_HOPS = 9

        /** Cap an utterance at ≈2.2 s of voiced audio so long speech still scores. */
        const val MAX_UTTERANCE_FEATURES = 70

        /** Fewer voiced frames than this and the utterance is too thin to trust. */
        const val MIN_UTTERANCE_FEATURES = 6

        const val DETECT_DEBOUNCE_MS = 6_000L
        const val LEVEL_PUBLISH_INTERVAL_MS = 60L
    }
}
