package com.example.sayvis.screentranslate

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * The layer that actually rewrites the screen.
 *
 * Every rectangle in the plan is covered with a plate whose colour was sampled from the
 * pixels underneath (so the translation looks like the app's own text) and the Persian is
 * laid out inside that plate in the original line's position — Persian in place of English,
 * character-for-character where the English used to be.
 *
 * The view is not touchable, so the owner keeps using the app underneath it normally.
 */
class ScreenOverlayPlanView(context: Context) : View(context) {

    private val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        isSubpixelText = true
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private var plan: FrameTranslationPlan? = null

    /** When false the overlay is invisible but the session stays alive (paused). */
    var painting: Boolean = true

    fun setPlan(next: FrameTranslationPlan?) {
        plan = next
        invalidate()
    }

    fun clear() {
        plan = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = plan ?: return
        if (!painting) return
        for (region in current.regions) {
            drawRegion(canvas, region)
        }
    }

    private fun drawRegion(canvas: Canvas, region: PlannedRegion) {
        if (!region.bounds.isValid || region.persianText.isBlank()) return
        val rect = RectF(
            region.bounds.left,
            region.bounds.top,
            region.bounds.right,
            region.bounds.bottom
        )
        val radius = (region.bounds.height * 0.28f).coerceIn(2f, 18f)
        platePaint.color = region.plateColor
        canvas.drawRoundRect(rect, radius, radius, platePaint)

        if (Color.alpha(region.plateColor) <= 8) {
            // Fully transparent plate: a hairline keeps the text readable on busy screens.
            borderPaint.color = (region.textColor and 0x00FFFFFF) or (90 shl 24)
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }

        val target = RectF(
            region.textBounds.left,
            region.textBounds.top,
            region.textBounds.right,
            region.textBounds.bottom
        )
        if (target.width() <= 1f || target.height() <= 1f) return

        val maxLines = max(1, ceil(target.height() / (region.fontSizePx * 1.25f)).toInt())
        textPaint.color = region.textColor
        val size = fitFontSize(region.persianText, textPaint, target.width(), target.height(), region.fontSizePx, maxLines)
        textPaint.textSize = size

        val layout = StaticLayout.Builder
            .obtain(region.persianText, 0, region.persianText.length, textPaint, target.width().toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.05f)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

        val dy = target.top + max(0f, (target.height() - layout.height) / 2f)
        canvas.save()
        canvas.translate(target.left, dy)
        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * Largest font size at which [text] still fits [maxLines] inside the box. Measured with
     * `Paint` first and rendered once, so a dense screen does not pay for twenty layouts.
     */
    private fun fitFontSize(
        text: String,
        paint: TextPaint,
        maxWidth: Float,
        maxHeight: Float,
        preferred: Float,
        maxLines: Int
    ): Float {
        var size = preferred.coerceAtLeast(MIN_FONT_PX)
        var guard = 0
        while (size > MIN_FONT_PX && guard < 60) {
            paint.textSize = size
            val metrics = paint.fontMetrics
            val lineHeight = metrics.descent - metrics.ascent + metrics.leading
            val estimatedLines = max(1, ceil(paint.measureText(text) / maxWidth.coerceAtLeast(1f)).toInt())
            if (estimatedLines <= maxLines && estimatedLines * lineHeight <= maxHeight) break
            if (maxLines == 1 && estimatedLines == 1 && lineHeight <= maxHeight) break
            size -= 0.5f
            guard++
        }
        return size
    }

    companion object {
        private const val MIN_FONT_PX = 9f
    }
}

/**
 * Small draggable control bubble: tap to pause/resume, long-press to stop the whole session.
 *
 * A permanent translator must be controllable from inside whatever app the owner is using,
 * without going back to SAYVIS. The bubble is that affordance — and it is the reason the
 * feature can be left running while the owner browses, reads or works.
 */
class ScreenControlBubbleView(context: Context) : View(context) {

    interface Listener {
        fun onBubbleTap()
        fun onBubbleHold()
        /** Requests a move by the given pixel delta; the host applies it to the window. */
        fun onBubbleDrag(dx: Float, dy: Float)
        fun onBubbleDragEnd()
    }

    var listener: Listener? = null

    /** Show the pause bars instead of the label when the session is paused. */
    var paused: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** Short label drawn inside the bubble (frame statistics). */
    var badge: String = "فا"
        set(value) {
            field = value
            invalidate()
        }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glyphPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isSubpixelText = true
    }

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var downAt = 0L
    private var pressed = false

    fun setColors(bodyArgb: Int, ringArgb: Int, glyphArgb: Int) {
        bodyPaint.color = bodyArgb
        ringPaint.color = ringArgb
        glyphPaint.color = glyphArgb
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (52 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val radius = minOf(width, height) / 2f - 2f * density
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radius, bodyPaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)

        if (paused) {
            val barWidth = radius * 0.26f
            val barHeight = radius * 0.86f
            barPaint.color = glyphPaint.color
            canvas.drawRoundRect(
                RectF(cx - barWidth * 1.25f, cy - barHeight / 2f, cx - barWidth * 0.25f, cy + barHeight / 2f),
                2f,
                2f,
                barPaint
            )
            canvas.drawRoundRect(
                RectF(cx + barWidth * 0.25f, cy - barHeight / 2f, cx + barWidth * 1.25f, cy + barHeight / 2f),
                2f,
                2f,
                barPaint
            )
            return
        }

        glyphPaint.textSize = radius * (if (badge.length > 3) 0.42f else 0.62f)
        val metrics = glyphPaint.fontMetrics
        val baseline = cy - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(badge, cx, baseline, glyphPaint)
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
                downAt = System.currentTimeMillis()
                dragging = false
                pressed = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!pressed) return false
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (dragging || abs(event.rawX - downX) > 12f || abs(event.rawY - downY) > 12f) {
                    dragging = true
                    listener?.onBubbleDrag(dx, dy)
                }
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                val wasDragging = dragging
                pressed = false
                dragging = false
                listener?.onBubbleDragEnd()
                if (!wasDragging) {
                    if (System.currentTimeMillis() - downAt >= 650L) {
                        listener?.onBubbleHold()
                    } else {
                        listener?.onBubbleTap()
                    }
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = false
                dragging = false
                listener?.onBubbleDragEnd()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
