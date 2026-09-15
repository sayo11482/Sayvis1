package com.example.sayvis.screentranslate

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device OCR. ML Kit's bundled Latin model ships inside the APK, so reading the screen
 * needs no network, no Google Play Services download and no third party.
 *
 * Coordinates come back in *frame* space and are scaled to display space once, here, so the
 * rest of the pipeline only ever deals with the pixels the owner is actually looking at.
 */
class ScreenTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognises [bitmap] and returns word-level fragments in display coordinates.
     *
     * @param scale display pixels per frame pixel (`displayWidth / bitmap.width`).
     */
    suspend fun recognize(bitmap: Bitmap, scale: Float): List<RecognizedFragment> =
        suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) continuation.resume(flatten(text, scale))
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
                .addOnCanceledListener {
                    if (continuation.isActive) continuation.cancel()
                }
        }

    fun close() {
        runCatching { recognizer.close() }
    }

    private fun flatten(visionText: Text, scale: Float): List<RecognizedFragment> {
        val fragments = ArrayList<RecognizedFragment>(64)
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val elements = line.elements
                if (elements.isEmpty()) {
                    val box = line.boundingBox ?: continue
                    val value = line.text.trim()
                    if (value.isNotEmpty()) {
                        fragments.add(RecognizedFragment(value, box.toScreenRect(scale), word = false))
                    }
                    continue
                }
                for (element in elements) {
                    val box = element.boundingBox ?: continue
                    val value = element.text.trim()
                    if (value.isEmpty()) continue
                    fragments.add(RecognizedFragment(value, box.toScreenRect(scale), word = true))
                }
            }
        }
        return fragments
    }

    private fun Rect.toScreenRect(scale: Float): ScreenRect =
        ScreenRect.of(left, top, right, bottom).scaled(scale)

    companion object {
        /** Long edge of the frame handed to the recogniser; smaller = faster and cooler. */
        const val OCR_TARGET_LONG_EDGE = 1180
    }
}
