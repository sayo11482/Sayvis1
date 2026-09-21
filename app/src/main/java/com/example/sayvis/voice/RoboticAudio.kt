package com.example.sayvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.sayvis.ui.components.AiStyleMath
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tiny on-device robotic sound effects for voice interactions — no asset files:
 * the chirps are synthesised as soft-clipped square-wave PCM
 * (see [AiStyleMath.replyPcm]/[AiStyleMath.ackPcm], pure and unit-tested).
 *
 *  - [playReply]  : rising three-chirp motif when SAYVIS answers a voice input.
 *  - [playAck]    : short two-tone blip when the owner's voice is recognised.
 */
object RoboticAudio {

    private const val SAMPLE_RATE = 16000

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "sayvis-robotic-audio").apply { isDaemon = true }
    }

    /** Only one chirp at a time — overlapping blips would sound broken. */
    private val playing = AtomicBoolean(false)

    fun playReply(context: Context) = play(context, AiStyleMath.replyPcm())

    fun playAck(context: Context) = play(context, AiStyleMath.ackPcm())

    private fun play(context: Context, pcm: ShortArray) {
        if (!playing.compareAndSet(false, true)) return
        executor.execute {
            val track = try {
                val bufferSize = pcm.size * 2
                AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    bufferSize,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                playing.set(false)
                return@execute
            }
            try {
                track.write(pcm, 0, pcm.size)
                track.setVolume(0.85f)
                track.play()
                // MODE_STATIC clips loop forever until stopped — stop after one pass.
                Thread.sleep(pcm.size.toLong() * 1000 / SAMPLE_RATE + 60)
                track.stop()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // interrupted or audio stack unavailable — nothing sensible to do
            } finally {
                runCatching { track.release() }
                playing.set(false)
            }
        }
    }
}
