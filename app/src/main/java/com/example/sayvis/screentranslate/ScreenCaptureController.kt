package com.example.sayvis.screentranslate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the screen-capture session: one `MediaProjection` → one `VirtualDisplay` → one
 * `ImageReader` → a stream of throttled frames.
 *
 * Deliberate engineering choices:
 *  - **Captured smaller than the display.** Frames are mirrored into a virtual display whose
 *    long edge is ~1180 px. OCR quality is unaffected (text stays well above the legibility
 *    threshold) while the per-frame copy cost drops by an order of magnitude on a 1440p
 *    panel — the difference between a translator that can run *permanently* and one that
 *    drains the battery in an hour.
 *  - **One frame in flight.** A new frame is only produced after the previous one has been
 *    fully processed, so a slow model call can never build a queue of stale screens.
 *  - **Rotation without a second projection.** Android 14+ allows exactly one
 *    `createVirtualDisplay()` per projection, so a configuration change resizes the existing
 *    display and swaps its surface instead (the officially documented approach).
 */
class ScreenCaptureController(
    private val context: Context,
    private val intervalMillis: () -> Long,
    private val onFrame: (ScreenFrame) -> Unit,
    private val onCaptureStopped: () -> Unit
) {

    private val thread = HandlerThread("sayvis-screen-capture").apply { start() }
    private val handler = Handler(thread.looper)
    private val busy = AtomicBoolean(false)

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null

    private var captureWidth = 0
    private var captureHeight = 0
    private var lastFrameAt = 0L

    val isRunning: Boolean get() = projection != null
    val frameWidth: Int get() = captureWidth
    val frameHeight: Int get() = captureHeight

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by the system or the owner")
            release()
            onCaptureStopped()
        }
    }

    /** Starts mirroring [mediaProjection]. The caller must already run as a foreground service. */
    @SuppressLint("WrongConstant")
    fun start(mediaProjection: MediaProjection) {
        if (projection != null) return
        projection = mediaProjection
        mediaProjection.registerCallback(projectionCallback, handler)

        val size = captureSize()
        captureWidth = size[0]
        captureHeight = size[1]
        reader = createReader(captureWidth, captureHeight)
        display = mediaProjection.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            captureWidth,
            captureHeight,
            densityDpi(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface,
            null,
            handler
        )
        lastFrameAt = 0L
        busy.set(false)
    }

    /** Called by the consumer once a frame has been fully processed. */
    fun frameConsumed() {
        busy.set(false)
    }

    /** Resizes the mirror after a rotation or a fold. Never creates a second projection. */
    fun onDisplayChanged() {
        val virtualDisplay = display ?: return
        val size = captureSize()
        if (size[0] == captureWidth && size[1] == captureHeight) return

        val previous = reader
        captureWidth = size[0]
        captureHeight = size[1]
        val fresh = createReader(captureWidth, captureHeight)
        reader = fresh
        runCatching { virtualDisplay.resize(captureWidth, captureHeight, densityDpi()) }
            .onFailure { Log.w(TAG, "VirtualDisplay resize failed: ${it.message}") }
        runCatching { virtualDisplay.setSurface(fresh.surface) }
            .onFailure { Log.w(TAG, "VirtualDisplay surface swap failed: ${it.message}") }
        runCatching { previous?.setOnImageAvailableListener(null, null) }
        runCatching { previous?.close() }
        busy.set(false)
    }

    /** Tears the session down. Safe to call twice. */
    fun release() {
        val virtualDisplay = display
        display = null
        runCatching { virtualDisplay?.release() }
        val currentReader = reader
        reader = null
        runCatching { currentReader?.setOnImageAvailableListener(null, null) }
        runCatching { currentReader?.close() }
        val currentProjection = projection
        projection = null
        if (currentProjection != null) {
            runCatching { currentProjection.unregisterCallback(projectionCallback) }
        }
        busy.set(false)
    }

    /** Stops the capture thread; the controller is not reusable afterwards. */
    fun shutdown() {
        release()
        runCatching { thread.quitSafely() }
    }

    // ------------------------------------------------------------------ internals

    private fun createReader(width: Int, height: Int): ImageReader {
        val fresh = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        fresh.setOnImageAvailableListener({ source -> handleImage(source) }, handler)
        return fresh
    }

    private fun handleImage(source: ImageReader) {
        val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return
        try {
            val now = SystemClock.elapsedRealtime()
            if (busy.get()) return
            if (now - lastFrameAt < intervalMillis()) return
            lastFrameAt = now

            val bitmap = image.toBitmap() ?: return
            val grid = luminanceGrid(bitmap)
            busy.set(true)
            onFrame(
                ScreenFrame(
                    bitmap = bitmap,
                    width = bitmap.width,
                    height = bitmap.height,
                    luminanceGrid = grid,
                    timestampNanos = image.timestamp,
                    releasedBy = { busy.set(false) }
                )
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Frame handling failed: ${error.message}")
        } finally {
            runCatching { image.close() }
        }
    }

    /** Row-stride aware conversion from the capture plane to an ARGB bitmap. */
    private fun Image.toBitmap(): Bitmap? {
        val plane = planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val padded = Bitmap.createBitmap(
            width + rowPadding / pixelStride.coerceAtLeast(1),
            height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        padded.copyPixelsFromBuffer(buffer)
        return if (padded.width == width) padded else Bitmap.createBitmap(padded, 0, 0, width, height)
    }

    /**
     * 16x16 luminance grid of the frame: the change detector, the blank-frame detector and
     * the fingerprint all work off this single cheap pass.
     */
    private fun luminanceGrid(bitmap: Bitmap): IntArray {
        val grid = IntArray(GRID * GRID)
        val stepX = (bitmap.width / GRID).coerceAtLeast(1)
        val stepY = (bitmap.height / GRID).coerceAtLeast(1)
        var index = 0
        for (row in 0 until GRID) {
            for (column in 0 until GRID) {
                val x = (column * stepX + stepX / 2).coerceIn(0, bitmap.width - 1)
                val y = (row * stepY + stepY / 2).coerceIn(0, bitmap.height - 1)
                val pixel = runCatching { bitmap.getPixel(x, y) }.getOrDefault(0)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                grid[index] = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt().coerceIn(0, 255)
                index++
            }
        }
        return grid
    }

    private fun captureSize(): IntArray {
        val metrics = displaySize()
        val displayWidth = metrics[0].coerceAtLeast(1)
        val displayHeight = metrics[1].coerceAtLeast(1)
        val longEdge = maxOf(displayWidth, displayHeight)
        val factor = if (longEdge > ScreenTextRecognizer.OCR_TARGET_LONG_EDGE) {
            ScreenTextRecognizer.OCR_TARGET_LONG_EDGE.toFloat() / longEdge.toFloat()
        } else {
            1f
        }
        val width = (((displayWidth * factor).toInt() / 2) * 2).coerceAtLeast(320)
        val height = (((displayHeight * factor).toInt() / 2) * 2).coerceAtLeast(320)
        return intArrayOf(width, height)
    }

    private fun displaySize(): IntArray {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            intArrayOf(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display.getRealMetrics(metrics)
            intArrayOf(metrics.widthPixels, metrics.heightPixels)
        }
    }

    private fun densityDpi(): Int = context.resources.displayMetrics.densityDpi

    companion object {
        private const val TAG = "ScreenCapture"
        private const val VIRTUAL_DISPLAY_NAME = "sayvis-screen-translate"
        const val GRID = 16
    }
}

/**
 * One captured frame plus its cheap fingerprint data.
 *
 * The bitmap is owned by the consumer: call [release] when the frame has been read,
 * translated and painted, otherwise the capture loop stays idle (which is intentional — it
 * keeps memory flat and never translates a screen the owner has already left).
 */
class ScreenFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val luminanceGrid: IntArray,
    val timestampNanos: Long,
    private val releasedBy: () -> Unit
) {
    var released: Boolean = false
        private set

    fun release() {
        if (released) return
        released = true
        runCatching { bitmap.recycle() }
        releasedBy()
    }

    /** Display pixels per frame pixel, given the current display width. */
    fun scaleTo(displayWidth: Int): Float =
        if (width <= 0) 1f else displayWidth.toFloat() / width.toFloat()

    fun fingerprint(): Long = ScreenTextPlanner.fingerprint(luminanceGrid)

    fun isBlank(): Boolean = ScreenTextPlanner.looksLikeBlankFrame(luminanceGrid)
}
