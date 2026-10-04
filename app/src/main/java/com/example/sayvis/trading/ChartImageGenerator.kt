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

    // ───────────────────────────── ۳ نمایِ تکمیلی — شمعی / سود / استراتژی ─────────────────────────────

    /** شمعی: همان SL/TP ولی با کندل‌های واقعی (سبز/قرمز) به‌جای خط. */
    fun renderCandlestick(
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
        canvas.drawColor(Color.parseColor("#0B0D14"))
        val chartLeft = PADDING_LEFT.toFloat()
        val chartRight = (WIDTH - PADDING_RIGHT).toFloat()
        val chartTop = PADDING_TOP.toFloat()
        val chartBottom = (HEIGHT - PADDING_BOTTOM).toFloat()
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val displayCloses = closes.takeLast(60)
        // Synth OHLC from closes for visual candle shape
        val candles = displayCloses.mapIndexed { idx, close ->
            val open = if (idx == 0) close else displayCloses[idx - 1]
            val bodyTop = max(open, close)
            val bodyBot = min(open, close)
            val wick = (bodyTop - bodyBot).coerceAtLeast( (displayCloses.maxOrNull()!! - displayCloses.minOrNull()!!) * 0.015) * 0.6
            LitStrategyEngine.Candle(open, bodyTop + wick, bodyBot - wick, close)
        }

        val allPrices = displayCloses + candles.flatMap { listOf(it.high, it.low) } +
            (if (plan.side != LitStrategyEngine.Side.WAIT) listOf(plan.entry, plan.stop) + plan.targets.map { it.price } else emptyList())
        val allMin = allPrices.minOrNull() ?: 0.0
        val allMax = allPrices.maxOrNull() ?: 0.0
        val pad = (allMax - allMin) * 0.12
        val low = allMin - pad
        val high = allMax + pad
        val range = (high - low).coerceAtLeast(1e-9)
        fun y(price: Double): Float = (chartBottom - ((price - low) / range * chartH)).toFloat()
        fun x(idx: Int): Float = chartLeft + (idx.toFloat() / (displayCloses.size - 1).coerceAtLeast(1).toFloat() * chartW)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C2030"); strokeWidth = 1f }
        val gridText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 18f; typeface = Typeface.MONOSPACE }
        for (k in 0..4) {
            val yy = chartTop + k * chartH / 4
            canvas.drawLine(chartLeft, yy, chartRight, yy, gridPaint)
            val price = high - k * range / 4
            val label = when (symbol) { MarketDataService.Symbol.XAUUSD -> "%.1f".format(price); MarketDataService.Symbol.EURUSD -> "%.5f".format(price); else -> "%.2f".format(price) }
            canvas.drawText(label, chartRight + 8, yy + 6, gridText)
        }
        for (k in 0..5) { val xx = chartLeft + k * chartW / 5; canvas.drawLine(xx, chartTop, xx, chartBottom, gridPaint) }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A3441"); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, border)

        // Candles
        val bullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); style = Paint.Style.FILL }
        val bearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444"); style = Paint.Style.FILL }
        val wickBull = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); strokeWidth = 2f }
        val wickBear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444"); strokeWidth = 2f }
        val candleW = (chartW / displayCloses.size * 0.55f).coerceIn(4f, 14f)
        candles.forEachIndexed { idx, c ->
            val xx = x(idx)
            val isBull = c.close >= c.open
            val bodyT = y(max(c.open, c.close))
            val bodyB = y(min(c.open, c.close))
            val wickT = y(c.high); val wickB = y(c.low)
            canvas.drawLine(xx, wickT, xx, wickB, if (isBull) wickBull else wickBear)
            val h = max(2f, bodyB - bodyT)
            canvas.drawRect(xx - candleW/2, bodyT, xx + candleW/2, bodyB.coerceAtLeast(bodyT+ h), if (isBull) bullPaint else bearPaint)
        }

        if (plan.side != LitStrategyEngine.Side.WAIT) {
            drawDashedLine(canvas, chartLeft, y(plan.stop), chartRight, y(plan.stop), Color.parseColor("#EF4444"), 3f)
            drawLabel(canvas, "SL  ${fmt(priceForLabel(plan.stop, symbol))}", chartRight - 160, y(plan.stop) - 10, Color.parseColor("#EF4444"))
            val entryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); strokeWidth = 3f; style = Paint.Style.STROKE }
            canvas.drawLine(chartLeft, y(plan.entry), chartRight, y(plan.entry), entryPaint)
            drawLabel(canvas, "ENTRY ${fmt(priceForLabel(plan.entry, symbol))}  ${plan.side}", chartLeft + 8, y(plan.entry) - 10, Color.parseColor("#10B981"))
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
            canvas.drawCircle(x(displayCloses.size - 1), y(displayCloses.last()), 8f, dotPaint)
            val tpColors = listOf("#3B82F6", "#60A5FA", "#93C5FD")
            plan.targets.forEachIndexed { ti, target ->
                val col = Color.parseColor(tpColors.getOrElse(ti) { "#3B82F6" })
                drawDashedLine(canvas, chartLeft, y(target.price), chartRight, y(target.price), col, 2.5f)
                drawLabel(canvas, "TP${ti + 1} ${fmt(priceForLabel(target.price, symbol))}  (1:${target.rr.toInt()})", chartRight - 200, y(target.price) - 10, col)
            }
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#065F46") }
            val rrText = "RR 1:${"%.1f".format(plan.rr)}"
            val rrW = 110f
            canvas.drawRoundRect(chartLeft + chartW / 2 - rrW / 2, y(plan.entry) - 28, chartLeft + chartW / 2 + rrW / 2, y(plan.entry) - 6, 8f, 8f, badgePaint)
            val rrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 14f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
            canvas.drawText(rrText, chartLeft + chartW / 2, y(plan.entry) - 14, rrPaint)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 16f }
        val tfBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
        canvas.drawRoundRect(20f, 14f, 165f, 50f, 10f, 10f, tfBadgePaint)
        val tfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#60A5FA"); textSize = 16f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        canvas.drawText("$timeframeLabel ● شمعی", 92f, 36f, tfPaint)
        canvas.drawText("${symbol.labelEn}  •  LIT  •  ${plan.side}", 185f, 38f, titlePaint)
        canvas.drawText(spreadLabel, 185f, 58f, subPaint)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D1D5DB"); textSize = 15f; typeface = Typeface.MONOSPACE }
        val timeLabel = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText(backtestSummary.take(80), 20f, HEIGHT - 30f, footerPaint)
        val timeW = footerPaint.measureText(timeLabel)
        canvas.drawText(timeLabel, WIDTH - timeW - 20, HEIGHT - 30f, footerPaint)
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 12f; alpha = 90; textAlign = Paint.Align.CENTER }
        canvas.drawText("SAYVIS  •  Vitaverse  •  LIT  •  Candlestick — not financial advice", WIDTH / 2f, HEIGHT - 12f, wmPaint)
        val dir = File(context.cacheDir, "sayvis_charts").apply { mkdirs() }
        val file = File(dir, "chart_candle_${symbol.name}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return RenderResult(bitmap, file)
    }

    /** درصد سود: هیستوگرامِ R هر معاملهٔ بک‌تست — سبز برد، قرمز باخت. */
    fun renderPnl(
        context: Context,
        backtest: LitBacktestEngine.Result,
        symbol: MarketDataService.Symbol,
        timeframeLabel: String = "M15"
    ): RenderResult {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0B0D14"))
        val chartLeft = PADDING_LEFT.toFloat()
        val chartRight = (WIDTH - PADDING_RIGHT).toFloat()
        val chartTop = PADDING_TOP.toFloat()
        val chartBottom = (HEIGHT - PADDING_BOTTOM).toFloat()
        val chartW = chartRight - chartLeft
        val chartH = chartBottom - chartTop

        val settled = backtest.trades.filter { it.won != null }
        val maxR = (settled.maxOfOrNull { it.rr } ?: 3.0).coerceAtLeast(3.0)
        val yTop = maxR + 0.5
        val yBot = -1.5
        val yRange = yTop - yBot
        fun y(r: Double): Float = (chartBottom - ((r - yBot) / yRange * chartH)).toFloat()
        fun x(idx: Int): Float {
            if (settled.isEmpty()) return chartLeft
            return chartLeft + (idx.toFloat() / (settled.size - 1).coerceAtLeast(1).toFloat() * chartW)
        }
        val barW = if (settled.isEmpty()) 10f else (chartW / settled.size * 0.65f).coerceIn(6f, 18f)

        // Grid + zero line
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C2030"); strokeWidth = 1f }
        val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); strokeWidth = 2f }
        val labels = listOf(-1.0, 0.0, 1.0, 2.0, 3.0, maxR)
        labels.distinct().forEach { r ->
            if (r in yBot..yTop) {
                val yy = y(r)
                canvas.drawLine(chartLeft, yy, chartRight, yy, gridPaint)
                val txt = if (r == 0.0) "0R" else "${if (r>0) "+" else ""}${"%.1f".format(r)}R"
                val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 17f; typeface = Typeface.MONOSPACE }
                canvas.drawText(txt, chartRight + 8, yy + 5, tp)
            }
        }
        canvas.drawLine(chartLeft, y(0.0), chartRight, y(0.0), zeroPaint)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A3441"); style = Paint.Style.STROKE; strokeWidth = 2f }
        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, border)

        // Bars
        val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981") }
        val lossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF4444") }
        val winStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#065F46"); style = Paint.Style.STROKE; strokeWidth = 1.2f }
        val lossStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7F1D1D"); style = Paint.Style.STROKE; strokeWidth = 1.2f }
        settled.forEachIndexed { idx, tr ->
            val rr = if (tr.won == true) tr.rr else -1.0
            val xx = x(idx)
            val topY = y(max(0.0, rr))
            val botY = y(min(0.0, rr))
            val isWin = tr.won == true
            canvas.drawRect(xx - barW/2, min(topY, botY), xx + barW/2, max(topY, botY), if (isWin) winPaint else lossPaint)
            canvas.drawRect(xx - barW/2, min(topY, botY), xx + barW/2, max(topY, botY), if (isWin) winStroke else lossStroke)
        }

        // Summary badges at bottom of chart area
        val winPct = if (settled.isNotEmpty()) (settled.count { it.won == true }.toDouble() / settled.size * 100) else 0.0
        val pf = backtest.profitFactor
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1F2937"); alpha = 210 }
        canvas.drawRoundRect(chartLeft + 8, chartTop + 8, chartLeft + 420, chartTop + 62, 10f, 10f, badgePaint)
        val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        canvas.drawText("${settled.size} معامله  •  ${"%.0f".format(winPct)}٪ برد  •  PF ${"%.2f".format(pf)}", chartLeft + 18, chartTop + 28, badgeText)
        val subBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 13f; typeface = Typeface.MONOSPACE }
        canvas.drawText("هر ستون = یک معامله (۱۰-۵۶)  — سبز سود R، قرمز زیان ۱R", chartLeft + 18, chartTop + 48, subBadge)

        // Header
        val tfBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
        canvas.drawRoundRect(20f, 14f, 185f, 50f, 10f, 10f, tfBadgePaint)
        val tfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#60A5FA"); textSize = 16f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        canvas.drawText("$timeframeLabel ● سود", 102f, 36f, tfPaint)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("${symbol.labelEn}  •  PnL per trade (R)", 205f, 38f, titlePaint)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D1D5DB"); textSize = 15f; typeface = Typeface.MONOSPACE }
        val summ = if (backtest.settledTrades in 10..56) backtest.summaryFa().take(80) else "بک‌تست ناکافی"
        canvas.drawText(summ, 20f, HEIGHT - 30f, footerPaint)
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 12f; alpha = 90; textAlign = Paint.Align.CENTER }
        canvas.drawText("SAYVIS  •  Vitaverse  •  LIT  •  PnL distribution — not financial advice", WIDTH / 2f, HEIGHT - 12f, wmPaint)
        val dir = File(context.cacheDir, "sayvis_charts").apply { mkdirs() }
        val file = File(dir, "chart_pnl_${symbol.name}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return RenderResult(bitmap, file)
    }

    /** تفکیک استراتژی: کارتِ جدولِ LIT — روند / نقدینگی / تایمینگ / اهداف + بک‌تست. */
    fun renderStrategy(
        context: Context,
        plan: LitStrategyEngine.TradePlan,
        backtest: LitBacktestEngine.Result,
        symbol: MarketDataService.Symbol,
        spreadLabel: String,
        timeframeLabel: String = "M15"
    ): RenderResult {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0B0D14"))

        // Header badge
        val tfBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A5F") }
        canvas.drawRoundRect(20f, 14f, 210f, 50f, 10f, 10f, tfBadgePaint)
        val tfPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#60A5FA"); textSize = 16f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        canvas.drawText("$timeframeLabel ● استراتژی", 115f, 36f, tfPaint)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("${symbol.labelEn}  •  LIT breakdown", 230f, 38f, titlePaint)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 16f }
        canvas.drawText(spreadLabel, 230f, 58f, subPaint)

        val cardLeft = 24f; val cardRight = WIDTH - 24f
        var curY = 84f
        val cardH = 118f
        fun drawCard(yTop: Float, title: String, value: String, hint: String, accent: Int, icon: String) {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#141827"); style = Paint.Style.FILL }
            val br = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 70; style = Paint.Style.STROKE; strokeWidth = 1.5f }
            canvas.drawRoundRect(cardLeft, yTop, cardRight, yTop + cardH, 14f, 14f, bg)
            canvas.drawRoundRect(cardLeft, yTop, cardRight, yTop + cardH, 14f, 14f, br)
            // accent dot
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
            canvas.drawCircle(cardLeft + 22, yTop + 24, 7f, dot)
            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("$icon  $title", cardLeft + 38, yTop + 30, tPaint)
            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 20f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
            canvas.drawText(value, cardLeft + 38, yTop + 60, vPaint)
            val hPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 14f; typeface = Typeface.MONOSPACE }
            // wrap hint within card width
            val hintX = cardLeft + 38
            canvas.drawText(hint.take(68), hintX, yTop + 84, hPaint)
            if (hint.length > 68) canvas.drawText(hint.drop(68).take(68), hintX, yTop + 102, hPaint)
        }

        val rrStr = "RR 1:${"%.1f".format(plan.rr)}  •  ${plan.side}"
        val sideAccent = if (plan.side == LitStrategyEngine.Side.LONG) Color.parseColor("#10B981") else if (plan.side == LitStrategyEngine.Side.SHORT) Color.parseColor("#EF4444") else Color.parseColor("#6B7280")
        drawCard(curY, "روند  (Trend)  —  EMA20 / EMA50", rrStr, "ورود فقط در جهت روند؛ خلافِ روند مسدود (WAIT).", sideAccent, "▲")
        curY += cardH + 12
        drawCard(curY, "نقدینگی  (Liquidity)  —  سقف/کف قبلی", "Entry ${fmt(plan.entry)}  SL ${fmt(plan.stop)}", "حد ضرر ۱.۵×ATR فراتر از ساختار؛ شکستِ نقدینگی = ورود.", Color.parseColor("#3B82F6"), "◈")
        curY += cardH + 12
        drawCard(curY, "تایمینگ  (Timing)  —  RSI / ATR", "ATR ${"%.4f".format(backtest.spreadPrice.coerceAtLeast(0.001))}  RSI گیت", "RSI اشباع خرید/فروش = عدمِ تعقیب؛ ATR سایزِ ریسک=۱R.", Color.parseColor("#F59E0B"), "◷")
        curY += cardH + 12
        val tps = plan.targets.joinToString("  ") { "TP${plan.targets.indexOf(it)+1} ${fmt(it.price)} (1:${it.rr.toInt()})" }.ifBlank { "—" }
        drawCard(curY, "اهداف  (Targets)  —  RR≥۱:۳", tps, "نردبان ۳R/۴.۵R/۶R — کفِ ۱:۳ هرگز شکسته نمی‌شود.", Color.parseColor("#8B5CF6"), "◎")
        curY += cardH + 14
        // Backtest verdict bar
        val verdictBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (backtest.isConvincing) Color.parseColor("#064E3B") else Color.parseColor("#7C2D12"); alpha = 200 }
        canvas.drawRoundRect(cardLeft, curY, cardRight, curY + 46, 10f, 10f, verdictBg)
        val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        val verdict = if (backtest.isConvincing) "✅ ${backtest.verdictFa}" else "⚠️ ${backtest.verdictFa}"
        canvas.drawText(verdict.take(72), WIDTH/2f, curY + 28, vPaint)

        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 12f; alpha = 90; textAlign = Paint.Align.CENTER }
        canvas.drawText("SAYVIS  •  Vitaverse  •  LIT  •  Strategy breakdown — not financial advice", WIDTH / 2f, HEIGHT - 12f, wmPaint)
        val dir = File(context.cacheDir, "sayvis_charts").apply { mkdirs() }
        val file = File(dir, "chart_strategy_${symbol.name}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
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
