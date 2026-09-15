package com.example.sayvis.screentranslate

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

/**
 * Owns the two windows SAYVIS draws above other apps:
 *
 *  1. the **plan layer** — the Persian text written exactly where the English was;
 *  2. the **control bubble** — tap to pause, drag to move, long-press to stop.
 *
 * Both are `TYPE_APPLICATION_OVERLAY` windows, which is why the owner must grant
 * "display over other apps". The plan layer is deliberately non-touchable, so nothing about
 * the app underneath becomes unusable while it is being translated.
 */
class ScreenOverlayHost(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var planView: ScreenOverlayPlanView? = null
    private var bubbleView: ScreenControlBubbleView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    var bubbleListener: ScreenControlBubbleView.Listener?
        get() = bubbleView?.listener
        set(value) {
            bubbleView?.listener = value
        }

    fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    /** Adds the plan layer. Returns false when the owner has not granted overlay access. */
    fun attachPlanLayer(): Boolean {
        if (planView != null) return true
        if (!canDrawOverlays()) return false
        val view = ScreenOverlayPlanView(context)
        return runCatching {
            windowManager.addView(view, planLayerParams())
            planView = view
            true
        }.getOrElse {
            Log.w(TAG, "Could not attach the translation layer: ${it.message}")
            false
        }
    }

    fun render(plan: FrameTranslationPlan?) {
        planView?.setPlan(plan)
    }

    fun setPainting(painting: Boolean) {
        planView?.painting = painting
        if (!painting) planView?.setPlan(null)
    }

    /**
     * Keeps the display awake for as long as the translator runs. Applied to the overlay
     * window, so it works even while the owner is reading another app.
     */
    fun setKeepScreenOn(on: Boolean) {
        planView?.keepScreenOn = on
    }

    /** True when the translation layer is currently attached to the window manager. */
    fun isPlanLayerAttached(): Boolean = planView != null

    fun detachPlanLayer() {
        val view = planView ?: return
        planView = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    /** Adds the floating control bubble, remembering its last position for this process. */
    fun attachBubble(opacityPercent: Int, paused: Boolean, badge: String): Boolean {
        if (bubbleView != null) {
            updateBubble(opacityPercent, paused, badge)
            return true
        }
        if (!canDrawOverlays()) return false
        val view = ScreenControlBubbleView(context)
        view.paused = paused
        view.badge = badge
        val params = bubbleParams()
        return runCatching {
            windowManager.addView(view, params)
            bubbleView = view
            bubbleParams = params
            updateBubble(opacityPercent, paused, badge)
            true
        }.getOrElse {
            Log.w(TAG, "Could not attach the control bubble: ${it.message}")
            false
        }
    }

    fun updateBubble(opacityPercent: Int, paused: Boolean, badge: String) {
        val view = bubbleView ?: return
        view.paused = paused
        view.badge = badge
        val alpha = (opacityPercent.coerceIn(20, 100) * 255 / 100)
        view.setColors(
            bodyArgb = (0x0B0F14 and 0x00FFFFFF) or (alpha shl 24),
            ringArgb = (0x18D2FF and 0x00FFFFFF) or ((alpha * 3 / 4).coerceIn(0, 255) shl 24),
            glyphArgb = (0xEAF7FF and 0x00FFFFFF) or (255 shl 24)
        )
    }

    fun detachBubble() {
        val view = bubbleView ?: return
        bubbleView = null
        bubbleParams = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    /** Moves the bubble by a delta reported by the view, clamped to the screen. */
    fun moveBubble(dx: Float, dy: Float) {
        val params = bubbleParams ?: return
        val view = bubbleView ?: return
        val metrics = context.resources.displayMetrics
        params.x = (params.x + dx.toInt()).coerceIn(0, (metrics.widthPixels - view.width).coerceAtLeast(0))
        params.y = (params.y + dy.toInt()).coerceIn(0, (metrics.heightPixels - view.height).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /** Snaps the bubble to the nearest horizontal edge, so it stops covering content. */
    fun settleBubble() {
        val params = bubbleParams ?: return
        val view = bubbleView ?: return
        val metrics = context.resources.displayMetrics
        val next = if (params.x + view.width / 2 < metrics.widthPixels / 2) 12 else {
            (metrics.widthPixels - view.width - 12).coerceAtLeast(12)
        }
        if (next == params.x) return
        params.x = next
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    fun detachAll() {
        detachPlanLayer()
        detachBubble()
    }

    // ------------------------------------------------------------------ internals

    private fun planLayerParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private fun bubbleParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 12
        y = (context.resources.displayMetrics.heightPixels * 0.62f).toInt()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    companion object {
        private const val TAG = "ScreenOverlay"
    }
}
