package com.example.sayvis.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.sayvis.service.FloatingAvatarService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * UI-facing helpers for the floating avatar & ambient listening feature:
 * permission checks, foreground-service control and enrollment capture.
 */
object AvatarListenController {

    const val ENROLL_SAMPLE_MS = 3_500L
    const val TEST_SAMPLE_MS = 6_000L

    // -------------------------------------------------------------- permissions

    fun hasMicPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun canRunListening(context: Context): Boolean =
        hasMicPermission(context) && hasOverlayPermission(context)

    /** Settings screen for the "display over other apps" grant. */
    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )

    // ----------------------------------------------------------- service control

    fun isListening(): Boolean = ListenBus.mode.value != ListenBus.Mode.OFF

    fun startListening(context: Context) {
        if (!canRunListening(context)) return
        val intent = Intent(context, FloatingAvatarService::class.java)
            .setAction(FloatingAvatarService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopListening(context: Context) {
        runCatching {
            context.stopService(Intent(context, FloatingAvatarService::class.java))
        }
        ListenBus.setMode(ListenBus.Mode.OFF)
        ListenBus.publishLevel(0f)
    }

    // ------------------------------------------------------------- voice capture

    /**
     * Records one microphone sample on the IO dispatcher.
     *
     * Returns raw mono PCM floats in [-1, 1] (null when the mic is unavailable or
     * the permission is missing). [onProgress] receives 0..1 completion and the live
     * 0..1 level, for the capture ring and meter.
     */
    suspend fun captureSample(
        context: Context,
        durationMs: Long,
        onProgress: (fraction: Float, level: Float) -> Unit = { _, _ -> }
    ): FloatArray? = withContext(Dispatchers.IO) {
        if (!hasMicPermission(context)) return@withContext null

        val minBuffer = AudioRecord.getMinBufferSize(
            VoicePrintMath.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return@withContext null

        val out = ArrayList<Float>((VoicePrintMath.SAMPLE_RATE * (durationMs / 1000 + 1)).toInt())
        val buffer = ShortArray(2048)
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                VoicePrintMath.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuffer * 2, VoicePrintMath.FRAME_SIZE * 4)
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            return@withContext null
        }

        try {
            if (rec.state != AudioRecord.STATE_INITIALIZED) return@withContext null
            rec.startRecording()
            val startedAt = SystemClock.elapsedRealtime()
            while (true) {
                val n = rec.read(buffer, 0, buffer.size)
                if (n <= 0) break
                var rmsSum = 0.0
                for (i in 0 until n) {
                    val v = buffer[i] / 32768f
                    out.add(v)
                    rmsSum += v.toDouble() * v.toDouble()
                }
                val rms = kotlin.math.sqrt(rmsSum / n).toFloat()
                val level = ((VoicePrintMath.dbfs(rms) + 45f) / 45f).coerceIn(0f, 1f)
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                onProgress(min(1f, elapsed / durationMs.toFloat()), level)
                if (elapsed >= durationMs) break
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            return@withContext null
        } finally {
            runCatching { rec.stop() }
            rec.release()
        }

        out.toFloatArray()
    }

    /** One averaged enrollment template from a captured sample, null when too quiet. */
    fun templateFromSample(pcm: FloatArray): FloatArray? =
        VoicePrintMath.meanTemplate(VoicePrintMath.extractVoicedFeatures(pcm))

    /** Scores a captured sample against the enrolled print (0..1 cosine similarity). */
    fun scoreSample(pcm: FloatArray, templates: List<FloatArray>): Float =
        VoicePrintMath.utteranceScore(VoicePrintMath.extractVoicedFeatures(pcm), templates)
}
