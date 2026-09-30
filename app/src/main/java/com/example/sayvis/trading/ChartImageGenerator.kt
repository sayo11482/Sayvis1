package com.example.sayvis.trading

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a professional live-chart bitmap with LIT entry/SL/TP drawn —
 * the “عکس از چارت زنده” the owner asked for.
 *
 * No TradingView WebView screenshot needed — the chart is drawn natively
 * with Canvas so it is instant (<50ms), offline-capable and pixel-perfect
 * for the SL/TP the engine actually uses (including Vitaverse day-spread).
 *
 * The bitmap is 1200×720, dark SAYVIS theme, and is both:
 *  1. saved to `context.cacheDir/sayvis_charts/chart_<symbol>_<ts>.png` so
 *     it can be shared/sent, and
 *  2. returned as a [Bitmap] for immediate in-app display (StateFlow).
 */
object ChartImageGenerator {

    const val WIDTH = 1200
    const val HEIGHT = 720
    const val PADDING_LEFT = 70
    const val PADDING_RIGHT = 90
    const val PADDING_TOP = 70
    const val PADDING_BOTTOM = 70

    data class RenderResult(
        val bitmap: Bitmap,
        val file: File
    )

    /**
     * Renders [closes] (earliest → latest) with [plan] overlay.
     * [symbol] + [spreadLabel] + [backtestSummary] are drawn in the footer.
     * Returns both the [Bitmap] and the saved [File].
     */
    fun render(
        context: Context,
        closes: List<Double>,
        plan: LitStrategyEngine.TradePlan,
        symbol: MarketDataService.Symbol,
        spreadLabel: String,
        backtestSummary: String,
        timeframeLabel: String = "M15"
    ): RenderResult {
        require(closes.size >= 10) { "need ≥10 closes" }
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#0B0D14"))

        val chartLeft = PADDING_LEFT.toFloat()
        val chartRight = (WIDTH - PADDING_RIGHT).toFloat()
        val chartTop = PADDING_TOP.toFloat()
        val chartBottom = (HEIGHT - PADDING_BOTTOM).toFloat()
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        // Range
        val displayCloses = closes.takeLast(60)
        val minPrice = displayCloses.minOrNull() ?: 0.0
        val maxPrice = displayCloses.maxOrNull() ?: 0.0
        // Expand range to include SL/TP so they stay visible
        val planPrices = if (plan.side != LitStrategyEngine.Side.WAIT) {
            listOf(plan.entry, plan.stop) + plan.targets.map { it.price }
        } else emptyList()
        val allMin = min(minPrice, planPrices.minOrNull() ?: minPrice)
        val allMax = max(maxPrice, planPrices.maxOrNull() ?: maxPrice)
        val padding = (allMax - allMin) * 0.12
        val low = allMin - padding
        val high = allMax + padding
        val range = (high - low).coerceAtLeast(1e-9)

        fun y(price: Double): Float = (chartBottom - ((price - low) / range * chartH)).toFloat()
        fun x(idx: Int): Float = chartLeft + (idx.toFloat() / (displayCloses.size - 1).coerceAtLeast(1).toFloat() * chartW)

        // Grid
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C2030")
            strokeWidth = 1f
        }
        val gridText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 18f
            typeface = Typeface.MONOSPACE
        }
        // Horizontal grid (price)
        for (k in 0..4) {
            val yy = chartTop + k * chartH / 4
            canvas.drawLine(chartLeft, yy, chartRight, yy, gridPaint)
            val price = high - k * range / 4
            val label = when (symbol) {
                MarketDataService.Symbol.XAUUSD -> "%.1f".format(price)
                MarketDataService.Symbol.EURUSD -> "%.5f".format(price)
                else -> "%.2f".format(price)
            }
            canvas.drawText(label, chartRight + 8, yy + 6, gridText)
        }
        // Vertical grid (time)
        for (k in 0..5) {
            val xx = chartLeft + k * chartW / 5
            canvas.drawLine(xx, chartTop, xx, chartBottom, gridPaint)
        }
        // Border
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2A3441")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, border)

        // Price line (poly)
        val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00D4FF")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A2B3D")
            style = Paint.Style.FILL
            alpha = 80
        }
        val path = Path()
        displayCloses.forEachIndexed { idx, price ->
            val xx = x(idx)
            val yy = y(price)
            if (idx == 0) path.moveTo(xx, yy) else path.lineTo(xx, yy)
        }
        // Fill under
        val fillPath = Path(path)
        fillPath.lineTo(x(displayCloses.size - 1), chartBottom)
        fillPath.lineTo(x(0), chartBottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, pricePaint)

        // Entry / SL / TP lines — only if tradeable
        if (plan.side != LitStrategyEngine.Side.WAIT) {
            // SL — red dashed
            drawDashedLine(canvas, chartLeft, y(plan.stop), chartRight, y(plan.stop), Color.parseColor("#EF4444"), 3f)
            drawLabel(canvas, "SL  ${fmt(priceForLabel(plan.stop, symbol))}", chartRight - 160, y(plan.stop) - 10, Color.parseColor("#EF4444"))

            // Entry — green solid + dot on last candle
            val entryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#10B981")
                strokeWidth = 3f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(chartLeft, y(plan.entry), chartRight, y(plan.entry), entryPaint)
            drawLabel(canvas, "ENTRY ${fmt(priceForLabel(plan.entry, symbol))}  ${plan.side}", chartLeft + 8, y(plan.entry) - 10, Color.parseColor("#10B981"))
            // Dot
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
            canvas.drawCircle(x(displayCloses.size - 1), y(displayCloses.last()), 8f, dotPaint)
            canvas.drawCircle(x(displayCloses.size - 1), y(displayCloses.last()), 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); alpha = 60 })

            // TPs — blue dashed (up to 3)
            val tpColors = listOf("#3B82F6", "#60A5FA", "#93C5FD")
            plan.targets.forEachIndexed { idx, target ->
                val col = Color.parseColor(tpColors.getOrElse(idx) { "#3B82F6" })
                drawDashedLine(canvas, chartLeft, y(target.price), chartRight, y(target.price), col, 2.5f)
                drawLabel(canvas, "TP${idx + 1} ${fmt(priceForLabel(target.price, symbol))}  (1:${target.rr.toInt()})", chartRight - 200, y(target.price) - 10, col)
            }

            // RR badge on entry
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#065F46") }
            val rrText = "RR 1:${"%.1f".format(plan.rr)}"
            val rrW = 110f
            canvas.drawRoundRect(chartLeft + chartW / 2 - rrW / 2, y(plan.entry) - 28, chartLeft + chartW / 2 + rrW / 2, y(plan.entry) - 6, 8f, 8f, badgePaint)
            val rrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText(rrText, chartLeft + chartW / 2, y(plan.entry) - 14, rrPaint)
        }

        // Header
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 16f
        }
        val tfBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
        canvas.drawRoundRect(20f, 14f, 210f, 50f, 10f, 10f, tfBadgePaint)
        val tfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#60A5FA"); textSize = 16f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        canvas.drawText(timeframeLabel, 115f, 36f, tfPaint)

        canvas.drawText("${symbol.labelEn}  •  LIT  •  ${plan.side}", 230f, 38f, titlePaint)
        canvas.drawText(spreadLabel, 230f, 58f, subPaint)

        // Footer — backtest + time
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1D5DB")
            textSize = 15f
            typeface = Typeface.MONOSPACE
        }
        val timeLabel = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText(backtestSummary.take(80), 20f, HEIGHT - 30f, footerPaint)
        val timeW = footerPaint.measureText(timeLabel)
        canvas.drawText(timeLabel, WIDTH - timeW - 20, HEIGHT - 30f, footerPaint)

        // Watermark
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 12f
            alpha = 90
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SAYVIS  •  Vitaverse  •  LIT  •  Generated live — not financial advice", WIDTH / 2f, HEIGHT - 12f, wmPaint)

        // Save to cache
        val dir = File(context.cacheDir, "sayvis_charts").apply { mkdirs() }
        val file = File(dir, "chart_${symbol.name}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return RenderResult(bitmap, file)
    }

    private fun drawDashedLine(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = width
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        canvas.drawLine(x1, y1, x2, y2, p)
    }

    private fun drawLabel(canvas: Canvas, text: String, x: Float, y: Float, bg: Int) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.MONOSPACE }
        val w = tp.measureText(text) + 16
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg; alpha = 230 }
        canvas.drawRoundRect(x, y - 16, x + w, y + 4, 6f, 6f, bgPaint)
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 13f; typeface = Typeface.MONOSPACE }
        canvas.drawText(text, x + 8, y, fg)
    }

    private fun fmt(v: Double): String = if (v >= 100) "%.2f".format(v) else "%.5f".format(v)
    private fun priceForLabel(v: Double, symbol: MarketDataService.Symbol): Double = v
}
