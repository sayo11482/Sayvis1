package com.example.sayvis.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * The “بوق” the owner hears the instant a professional LIT entry is found —
 * regardless of timeframe.
 *
 * Two layers, both fire-and-forget on a daemon thread:
 *  1. [ToneGenerator] — the classic system beep (instant, reliable).
 *  2. A custom 3-tone chirp (like RoboticAudio) — the SAYVIS signature.
 *
 * Either layer failing is silently ignored — the chart is still generated.
 */
object ChartBeep {

    private val exec = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sayvis-chart-beep").apply { isDaemon = true }
    }

    /** Beep now — call from ViewModel when a chart signal is produced. */
    fun beep() {
        exec.execute {
            // Layer 1: system tone (fast)
            runCatching {
                val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 88)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 320)
                Thread.sleep(360)
                tg.release()
            }
            // Layer 2: SAYVIS chirp (880Hz → 1200Hz → 880Hz)
            runCatching { playChirp() }
        }
    }

    private fun playChirp() {
        val sampleRate = 16000
        val chirp = buildChirp(sampleRate)
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            chirp.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        try {
            track.write(chirp, 0, chirp.size)
            track.setVolume(0.9f)
            track.play()
            Thread.sleep(chirp.size * 1000L / sampleRate + 40)
            track.stop()
        } finally {
            runCatching { track.release() }
        }
    }

    private fun buildChirp(sampleRate: Int): ShortArray {
        fun tone(freq: Double, ms: Int, vol: Double): ShortArray {
            val n = sampleRate * ms / 1000
            return ShortArray(n) { i ->
                val env = when {
                    i < n * 0.06 -> i / (n * 0.06) // attack
                    i > n * 0.94 -> (n - i) / (n * 0.06) // release
                    else -> 1.0
                }
                (sin(2 * PI * freq * i / sampleRate) * 28000 * vol * env).toInt().toShort()
            }
        }
        val a = tone(880.0, 110, 0.9)
        val b = tone(1200.0, 110, 0.95)
        val c = tone(880.0, 140, 0.85)
        val gap = ShortArray(sampleRate * 20 / 1000) // 20ms silence
        return a + gap + b + gap + c
    }
}
