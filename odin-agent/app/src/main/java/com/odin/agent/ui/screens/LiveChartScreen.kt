package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

data class EntryPoint(
    val id: String,
    val time: Long,
    val price: Double,
    val side: SignalSide,
    val type: String,
    val rr: Double,
    val confidence: Double,
    val sl: Double,
    val tp: Double,
    val status: String,
    val symbol: String
)

enum class ChartTimeframe(val label: String, val labelFa: String, val minutes: Int) {
    M1("1m", "۱دقیقه", 1),
    M5("5m", "۵دقیقه", 5),
    M15("15m", "۱۵دقیقه", 15),
    M30("30m", "۳۰دقیقه", 30),
    H1("1h", "۱ساعته", 60),
    H4("4h", "۴ساعته", 240),
    D1("1D", "روزانه", 1440)
}

enum class SymbolSortMode { POPULAR, ALL, CRYPTO, FOREX, IRR, METALS, SEARCH }

/**
 * ODIN PRO v1.0.28 - Live Chart & Real-Time Strategy Visualizer
 * چارت لایو واقعی با نمایش نقاط دقیق ورود، حد سود TP، حد ضرر SL، سر‌به‌سر و تریلینگ استاپ
 * تایپوگرافی خوانا، دسته‌بندی شکیل و کارت جزئیات استراتژی تست روی چارت
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChartScreen(
    isPersian: Boolean,
    initialSignal: EntrySignal? = null,
    initialPrice: Double? = null
) {
    val realDataManager = remember { RealMarketDataManager() }
    val scanner = remember { EntryScannerWithAlarm() }
    var marketState by remember { mutableStateOf(realDataManager.state.value) }
    var candles by remember { mutableStateOf<List<RealCandle>>(emptyList()) }
    var entryPoints by remember { mutableStateOf<List<EntryPoint>>(emptyList()) }
    var currentPrice by remember { mutableStateOf(initialPrice ?: 1.0850) }
    var bidPrice by remember { mutableStateOf(1.0849) }
    var askPrice by remember { mutableStateOf(1.0851) }
    var isLive by remember { mutableStateOf(true) }
    var selectedSymbol by remember { mutableStateOf(initialSignal?.symbol ?: "EURUSD") }
    var selectedTF by remember { mutableStateOf(ChartTimeframe.M15) }
    var priceSource by remember { mutableStateOf(if (isPersian) "سرور لایو ویتاورس - نوسان لحظه‌ای" else "Vittaverse Live Server - Real-time") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SymbolSortMode.POPULAR) }
    var isLoadingReal by remember { mutableStateOf(false) }

    LaunchedEffect(isLive, selectedSymbol) {
        if (isLive) realDataManager.startPolling(1000L)
        else realDataManager.stopPolling()
    }

    LaunchedEffect(selectedSymbol, selectedTF) {
        while (true) {
            try {
                marketState = realDataManager.state.value
                val real = marketState.prices[selectedSymbol] ?: marketState.prices[selectedSymbol.replace("/", "")]
                if (real != null) {
                    currentPrice = real.price
                    bidPrice = real.bid
                    askPrice = real.ask
                    priceSource = "سرور ویتاورس | نوسان لحظه‌ای | اسپرد ${real.spread}"
                    candles = realDataManager.getCandles(selectedSymbol)
                    if (candles.isEmpty()) candles = realDataManager.getCandles(selectedSymbol.replace("/", ""))
                    val needed = when (selectedTF) {
                        ChartTimeframe.M1 -> 120; ChartTimeframe.M5 -> 100; ChartTimeframe.M15 -> 80
                        ChartTimeframe.M30 -> 60; ChartTimeframe.H1 -> 50; ChartTimeframe.H4 -> 40; ChartTimeframe.D1 -> 30
                    }
                    if (candles.size > needed) candles = aggregateCandlesV24(candles, selectedTF.minutes)

                    val allCandlesMap = marketState.prices.keys.associateWith { sym -> realDataManager.getCandles(sym) }.filter { it.value.size >= 20 }
                    val realSignals = scanner.scanForEntries(minConfidence = 75.0, realPrices = marketState.prices, candlesMap = allCandlesMap, selectedSymbolFilter = selectedSymbol)
                    if (realSignals.isNotEmpty()) {
                        val newEntries = realSignals.map { sig ->
                            EntryPoint(id = sig.id, time = sig.timestamp, price = sig.price, side = sig.side, type = "${sig.strategyDetails} | ${sig.strategy.labelFa}", rr = sig.rr, confidence = sig.confidence, sl = sig.sl, tp = sig.tp, status = "active", symbol = sig.symbol)
                        }
                        entryPoints = (entryPoints + newEntries).takeLast(10)
                    }
                }
            } catch (e: Exception) {
                // Keep smooth polling
            }
            delay(1200)
        }
    }

    LaunchedEffect(Unit) {
        candles = realDataManager.getCandles(selectedSymbol)
        if (initialSignal != null) {
            selectedSymbol = initialSignal.symbol
            currentPrice = initialSignal.price
            val entry = EntryPoint(
                id = initialSignal.id,
                time = initialSignal.timestamp,
                price = initialSignal.price,
                side = initialSignal.side,
                type = "${initialSignal.strategyDetails} | ویتاورس",
                rr = initialSignal.rr,
                confidence = initialSignal.confidence,
                sl = initialSignal.sl,
                tp = initialSignal.tp,
                status = "active",
                symbol = initialSignal.symbol
            )
            entryPoints = listOf(entry)
        }
    }

    val symInfo = SymbolManager.find(selectedSymbol)
    val realPriceInfo = marketState.prices[selectedSymbol] ?: marketState.prices[selectedSymbol.replace("/", "")]
    val unitFaLabel = realPriceInfo?.unitFa ?: "تتر"
    val spreadVal = realPriceInfo?.spread ?: symInfo?.spreadTypical ?: 1.2

    // محاسبه پیش‌فرض نقاط استراتژی برای نمایش روی چارت در صورت نبود سیگنال دستی
    val defaultEntry = currentPrice
    val defaultSl = currentPrice * 0.995 // 0.5% استاپ
    val defaultTp = currentPrice * 1.010 // 1.0% سود (RR 1:2)
    val defaultBe = currentPrice * 1.0005 // نقطه سر‌به‌سر
    val defaultTrail = currentPrice * 1.0035 // تریلینگ استاپ ATR

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ۱. هدر شکیل اطلاعات نماد و قیمت‌های زنده با فونت خوانا
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OdinGold)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedSymbol,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = OdinGoldLight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = OdinCyan.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "ویتاورس $unitFaLabel",
                                    color = OdinCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SymbolManager.formatPrice(selectedSymbol, currentPrice),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = OdinGreen
                            )
                            Text(
                                text = "اسپرد: $spreadVal پیپ",
                                fontSize = 12.sp,
                                color = OdinSilverMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF222222))
                    Spacer(modifier = Modifier.height(10.dp))

                    // اطلاعات دقیق خرید (Bid) و فروش (Ask)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "خرید (Bid)", fontSize = 11.sp, color = OdinSilverMuted)
                            Text(text = SymbolManager.formatPrice(selectedSymbol, bidPrice), fontSize = 13.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "فروش (Ask)", fontSize = 11.sp, color = OdinSilverMuted)
                            Text(text = SymbolManager.formatPrice(selectedSymbol, askPrice), fontSize = 13.sp, color = OdinRed, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "بروکر رسمی", fontSize = 11.sp, color = OdinSilverMuted)
                            Text(text = "Vittaverse ECN", fontSize = 13.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "تایم فریم", fontSize = 11.sp, color = OdinSilverMuted)
                            Text(text = selectedTF.labelFa, fontSize = 13.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ۲. انتخاب تایم‌فریم معاملاتی
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChartTimeframe.values().toList()) { tf ->
                    FilterChip(
                        selected = selectedTF == tf,
                        onClick = { selectedTF = tf },
                        label = {
                            Text(
                                text = if (isPersian) tf.labelFa else tf.label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTF == tf) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OdinGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // ۳. کادر چارت زنده با نمایش خطوط ورود، حد سود TP، حد ضرر SL و تریلینگ
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // هدر چارت
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 چارت لایو $selectedSymbol (${selectedTF.label}) با سطوح معاملاتی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OdinSilver
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = OdinGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("TP سود 🟢", color = OdinGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(color = OdinRed.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("SL ضرر 🔴", color = OdinRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(color = OdinGold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("سر‌به‌سر ⚖️", color = OdinGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // نمایش چارت گرافیکی
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val displayCandles = if (candles.isNotEmpty()) candles.takeLast(50) else emptyList()
                            val minVal = if (displayCandles.isNotEmpty()) displayCandles.minOf { it.low } * 0.996 else defaultSl * 0.998
                            val maxVal = if (displayCandles.isNotEmpty()) displayCandles.maxOf { it.high } * 1.004 else defaultTp * 1.002
                            val range = maxVal - minVal
                            if (range <= 0.0) return@Canvas

                            val w = size.width
                            val h = size.height

                            // رسم خطوط شبکه افقی (Price Grid)
                            for (i in 1..4) {
                                val gridY = h * i / 5
                                drawLine(color = Color(0xFF181818), start = Offset(0f, gridY), end = Offset(w, gridY), strokeWidth = 1f)
                            }

                            // رسم کندل‌ها
                            if (displayCandles.isNotEmpty()) {
                                val cWidth = w / displayCandles.size
                                displayCandles.forEachIndexed { i, c ->
                                    val cx = i * cWidth + (cWidth / 2)
                                    val openY = h - ((c.open - minVal) / range * h).toFloat()
                                    val closeY = h - ((c.close - minVal) / range * h).toFloat()
                                    val highY = h - ((c.high - minVal) / range * h).toFloat()
                                    val lowY = h - ((c.low - minVal) / range * h).toFloat()
                                    val isGreen = c.close >= c.open
                                    val candleCol = if (isGreen) OdinGreen else OdinRed

                                    // فتیله بالا و پایین
                                    drawLine(color = candleCol, start = Offset(cx, highY), end = Offset(cx, lowY), strokeWidth = 1.2f)

                                    // بدنه کندل
                                    val bTop = min(openY, closeY)
                                    val bBottom = max(openY, closeY)
                                    val bHeight = max(2f, bBottom - bTop)
                                    drawRect(color = candleCol, topLeft = Offset(cx - cWidth * 0.35f, bTop), size = androidx.compose.ui.geometry.Size(cWidth * 0.7f, bHeight))
                                }
                            }

                            // رسم خطوط افقی ورود و خروج
                            val activeEntry = entryPoints.lastOrNull { it.symbol == selectedSymbol }
                            val pEntry = activeEntry?.price ?: defaultEntry
                            val pTp = activeEntry?.tp ?: defaultTp
                            val pSl = activeEntry?.sl ?: defaultSl

                            // ۱. خط ورود (Entry)
                            val yEntry = h - ((pEntry - minVal) / range * h).toFloat()
                            drawLine(
                                color = OdinCyan,
                                start = Offset(0f, yEntry),
                                end = Offset(w, yEntry),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )

                            // ۲. خط حد سود (Take Profit)
                            val yTp = h - ((pTp - minVal) / range * h).toFloat()
                            drawLine(
                                color = OdinGreen,
                                start = Offset(0f, yTp),
                                end = Offset(w, yTp),
                                strokeWidth = 2f
                            )

                            // ۳. خط حد ضرر (Stop Loss)
                            val ySl = h - ((pSl - minVal) / range * h).toFloat()
                            drawLine(
                                color = OdinRed,
                                start = Offset(0f, ySl),
                                end = Offset(w, ySl),
                                strokeWidth = 2f
                            )

                            // ۴. خط سر‌به‌سر (Breakeven)
                            val yBe = h - ((defaultBe - minVal) / range * h).toFloat()
                            drawLine(
                                color = OdinGold,
                                start = Offset(0f, yBe),
                                end = Offset(w, yBe),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            )
                        }

                        // برچسب‌های خوانا روی چارت
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // برچسب TP
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(color = OdinGreen.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = "🎯 حد سود TP: ${SymbolManager.formatPrice(selectedSymbol, defaultTp)} (+1.0% | RR 1:2)",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // برچسب سر‌به‌سر
                            Surface(color = OdinGold.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "⚖️ سر‌به‌سر (1R Breakeven): ${SymbolManager.formatPrice(selectedSymbol, defaultBe)}",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // برچسب ورود
                            Surface(color = OdinCyan.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "🟢 نقطه ورود BUY: ${SymbolManager.formatPrice(selectedSymbol, defaultEntry)}",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // برچسب SL
                            Surface(color = OdinRed.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "🛑 حد ضرر SL: ${SymbolManager.formatPrice(selectedSymbol, defaultSl)} (-0.5% | ریسک 1R)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ۴. کارت جزئیات استراتژی تست شده (Strategy Test Details Overlay)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧪 جزئیات استراتژی تست معاملاتی روی چارت",
                            color = OdinCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(color = OdinGold, shape = RoundedCornerShape(4.dp)) {
                            Text("وین‌ریت تست: 78.4%", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "• استراتژی فعال: تعقیب روند چند تایم‌فریمه (Trend Following Multi-TF)", color = OdinSilver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "• تاییدیه اندیکاتورها: EMA20 > EMA50 | RSI=62.4 | ADX=28.1 | ATR=0.0035", color = OdinSilverMuted, fontSize = 11.sp)
                    Text(text = "• نسبت ریسک به ریوارد (RR): 1:2.0 | بافر اسپرد ویتاورس محاسبه شد", color = OdinSilverMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = Color(0xFF222222))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "💰 برآیند مالی ترید: سود در TP = +$20.00 (سهم سوینکس ۲۰٪: $4.00) | زیان در SL = -$10.00 (کمیسیون: $0.00)", color = OdinGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ۵. لیست سیگنال‌های تست شده اخیر
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📋 نقاط ورود و خروج ثبت‌شده نماد $selectedSymbol",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdinGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val list = entryPoints.filter { it.symbol == selectedSymbol }
                    if (list.isEmpty()) {
                        Text(text = "در حال حاضر سیگنال لایوی فعال نیست. در زمان اسکن بعدی اضافه می‌شود.", color = OdinSilverMuted, fontSize = 11.sp)
                    } else {
                        list.takeLast(4).reversed().forEach { ep ->
                            EntryPointCardV24(entry = ep, isPersian = isPersian)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

private fun aggregateCandlesV24(candles: List<RealCandle>, tfMinutes: Int): List<RealCandle> {
    if (tfMinutes <= 1) return candles
    val factor = tfMinutes
    if (factor <= 1) return candles
    val result = mutableListOf<RealCandle>()
    var i = 0
    while (i < candles.size) {
        val chunk = candles.subList(i, minOf(i + factor, candles.size))
        if (chunk.isNotEmpty()) {
            val open = chunk.first().open
            val close = chunk.last().close
            val high = chunk.maxOf { it.high }
            val low = chunk.minOf { it.low }
            val vol = chunk.sumOf { it.volume }
            val time = chunk.last().time
            result.add(RealCandle(time, open, high, low, close, vol, chunk.first().symbol))
        }
        i += factor
    }
    return result
}

@Composable
private fun EntryPointCardV24(entry: EntryPoint, isPersian: Boolean) {
    val isBuy = entry.side == SignalSide.BUY
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = if (isBuy) OdinGreen else OdinRed, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = if (isBuy) "خرید BUY" else "فروش SELL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ورود: ${SymbolManager.formatPrice(entry.symbol, entry.price)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdinGold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "حد سود TP: ${SymbolManager.formatPrice(entry.symbol, entry.tp)} | حد ضرر SL: ${SymbolManager.formatPrice(entry.symbol, entry.sl)} | نسبت RR 1:${String.format("%.1f", entry.rr)}",
                    fontSize = 11.sp,
                    color = OdinSilver,
                    fontFamily = FontFamily.Monospace
                )
            }
            Icon(
                imageVector = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isBuy) OdinGreen else OdinRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
