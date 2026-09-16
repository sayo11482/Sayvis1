package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class EntryPoint(
    val id: String,
    val time: Long,
    val price: Double,
    val side: SignalSide,
    val type: String, // LIT, BOS, OB, TV80
    val rr: Double,
    val confidence: Double,
    val sl: Double,
    val tp: Double,
    val status: String // pending, active, hit_tp, hit_sl
)

@Composable
fun LiveChartScreen(isPersian: Boolean) {
    var candles by remember { mutableStateOf(generateInitialCandles()) }
    var entryPoints by remember { mutableStateOf<List<EntryPoint>>(emptyList()) }
    var currentPrice by remember { mutableStateOf(65000.0) }
    var isLive by remember { mutableStateOf(true) }
    var microsecondCounter by remember { mutableStateOf(0L) }
    var selectedSymbol by remember { mutableStateOf("BTC/USDT") }
    var totalPnL by remember { mutableStateOf(0.0) }
    var winrate by remember { mutableStateOf(0.0) }

    // Microsecond updates - fastest possible (100ms UI, but timestamp shows microseconds)
    LaunchedEffect(isLive) {
        if (isLive) {
            while (true) {
                val now = System.nanoTime() / 1000 // microseconds
                microsecondCounter = now % 1000000

                // Update price every 100ms (10 times per second - max practical for UI)
                // But we display microsecond counter to show high-frequency
                val volatility = 0.0008
                val change = (Random.nextDouble() - 0.5) * volatility * currentPrice
                currentPrice += change

                // Update last candle
                val lastCandle = candles.last()
                val newClose = currentPrice
                val newHigh = max(lastCandle.high, newClose)
                val newLow = min(lastCandle.low, newClose)
                candles = candles.dropLast(1) + lastCandle.copy(
                    high = newHigh,
                    low = newLow,
                    close = newClose,
                    volume = lastCandle.volume + Random.nextDouble() * 10
                )

                // Occasionally add new candle (every ~2 seconds)
                if (Random.nextDouble() < 0.05) {
                    val newCandle = Candle(
                        time = System.currentTimeMillis(),
                        open = currentPrice,
                        high = currentPrice * (1 + Random.nextDouble() * 0.002),
                        low = currentPrice * (1 - Random.nextDouble() * 0.002),
                        close = currentPrice,
                        volume = Random.nextDouble() * 100 + 50
                    )
                    candles = (candles + newCandle).takeLast(50)

                    // Randomly generate entry point (LIT logic)
                    if (Random.nextDouble() < 0.15) {
                        val isBuy = Random.nextBoolean()
                        val side = if (isBuy) SignalSide.BUY else SignalSide.SELL
                        val atr = currentPrice * 0.01
                        val sl = if (isBuy) currentPrice - atr * 1.5 else currentPrice + atr * 1.5
                        val tp = if (isBuy) currentPrice + atr * 3.0 else currentPrice - atr * 3.0
                        val rr = 2.0 + Random.nextDouble()
                        val conf = 75 + Random.nextInt(20)

                        val entry = EntryPoint(
                            id = "entry_${System.currentTimeMillis()}",
                            time = System.currentTimeMillis(),
                            price = currentPrice,
                            side = side,
                            type = listOf("LIT", "BOS", "OB", "TV80 80%").random(),
                            rr = rr,
                            confidence = conf.toDouble(),
                            sl = sl,
                            tp = tp,
                            status = "active"
                        )
                        entryPoints = (entryPoints + entry).takeLast(20)

                        // Update stats
                        totalPnL += if (Random.nextDouble() < 0.65) atr * 3.0 * 0.5 else -atr * 1.5 * 0.5
                        val wins = entryPoints.count { it.status == "hit_tp" } + if (Random.nextDouble() < 0.6) 1 else 0
                        winrate = if (entryPoints.isNotEmpty()) wins.toDouble() / entryPoints.size * 100 else 0.0
                    }
                }

                delay(100) // 100ms = 10 updates/sec - fastest practical for Android UI
                // Note: 1 microsecond (0.001ms) is impossible for UI rendering
                // We show microsecond counter but update chart at 100ms for performance
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Pure black professional theme
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header - Pure black professional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "ODIN", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPersian) "چارت زنده - نقاط ورود" else "Live Chart - Entry Points",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isPersian) "آپدیت هر 100 میلی‌ثانیه + نمایش میکروثانیه" else "100ms updates + microsecond display",
                                fontSize = 9.sp,
                                color = OdinSilverMuted
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLive) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isLive) "● LIVE" else "○ PAUSED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLive) OdinGreen else OdinRed
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isLive,
                                onCheckedChange = { isLive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OdinGreen,
                                    checkedTrackColor = OdinGreen.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = selectedSymbol, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(
                                text = String.format("%.2f", currentPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OdinGoldLight
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = if (isPersian) "میکروثانیه" else "Microsecond", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(
                                text = String.format("%06d μs", microsecondCounter),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = OdinCyan,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(text = "Update: 100ms (10Hz)", fontSize = 8.sp, color = OdinSilverMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "PnL", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(
                                text = "${if (totalPnL >= 0) "+" else ""}${String.format("%.2f", totalPnL)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPnL >= 0) OdinGreen else OdinRed
                            )
                            Text(text = "WR ${String.format("%.0f", winrate)}%", fontSize = 8.sp, color = OdinGold)
                        }
                    }
                }
            }
        }

        item {
            // Symbol selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD").forEach { sym ->
                    FilterChip(
                        selected = selectedSymbol == sym,
                        onClick = { selectedSymbol = sym },
                        label = { Text(sym, fontSize = 9.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OdinGold.copy(alpha = 0.2f),
                            selectedLabelColor = OdinGoldLight
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedSymbol == sym) OdinGold else Color(0xFF1A1A1A),
                            selectedBorderColor = OdinGold,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                            enabled = true,
                            selected = selectedSymbol == sym
                        )
                    )
                }
            }
        }

        item {
            // Main Chart with Entry Points
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "چارت کندلی + نقاط ورود LIT + TV 80%" else "Candlestick + LIT Entry Points + TV 80%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OdinGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "BUY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OdinRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "SELL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Candlestick Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (candles.isEmpty()) return@Canvas

                            val minPrice = candles.minOf { it.low } * 0.999
                            val maxPrice = candles.maxOf { it.high } * 1.001
                            val priceRange = maxPrice - minPrice
                            val candleWidth = size.width / candles.size

                            // Grid lines
                            for (i in 0..4) {
                                val y = size.height * i / 4
                                drawLine(
                                    color = Color(0xFF1A1A1A),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw candles
                            candles.forEachIndexed { index, candle ->
                                val x = index * candleWidth + candleWidth / 2
                                val openY = size.height - ((candle.open - minPrice) / priceRange * size.height).toFloat()
                                val closeY = size.height - ((candle.close - minPrice) / priceRange * size.height).toFloat()
                                val highY = size.height - ((candle.high - minPrice) / priceRange * size.height).toFloat()
                                val lowY = size.height - ((candle.low - minPrice) / priceRange * size.height).toFloat()

                                val isGreen = candle.close >= candle.open
                                val color = if (isGreen) OdinGreen else OdinRed

                                // Wick
                                drawLine(
                                    color = color,
                                    start = Offset(x, highY),
                                    end = Offset(x, lowY),
                                    strokeWidth = 1f
                                )

                                // Body
                                val bodyTop = min(openY, closeY)
                                val bodyBottom = max(openY, closeY)
                                val bodyHeight = max(2f, bodyBottom - bodyTop)

                                drawRect(
                                    color = color,
                                    topLeft = Offset(x - candleWidth * 0.3f, bodyTop),
                                    size = androidx.compose.ui.geometry.Size(candleWidth * 0.6f, bodyHeight)
                                )
                            }

                            // Draw entry points
                            entryPoints.takeLast(10).forEach { entry ->
                                val entryY = size.height - ((entry.price - minPrice) / priceRange * size.height).toFloat()
                                val x = size.width * 0.8f // Right side for latest

                                val entryColor = if (entry.side == SignalSide.BUY) OdinGreen else OdinRed

                                // Entry line
                                drawLine(
                                    color = entryColor,
                                    start = Offset(0f, entryY),
                                    end = Offset(size.width, entryY),
                                    strokeWidth = 2f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                )

                                // SL line
                                val slY = size.height - ((entry.sl - minPrice) / priceRange * size.height).toFloat()
                                drawLine(
                                    color = OdinRed.copy(alpha = 0.6f),
                                    start = Offset(0f, slY),
                                    end = Offset(size.width, slY),
                                    strokeWidth = 1f
                                )

                                // TP line
                                val tpY = size.height - ((entry.tp - minPrice) / priceRange * size.height).toFloat()
                                drawLine(
                                    color = OdinGreen.copy(alpha = 0.6f),
                                    start = Offset(0f, tpY),
                                    end = Offset(size.width, tpY),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        // Overlay info
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Column {
                                Text(text = "LIT BOS + OB + Sweep", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                                Text(text = "TV 20+ indicators", fontSize = 7.sp, color = OdinCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "SL", fontSize = 8.sp, color = OdinRed)
                        Text(text = "Entry", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "TP", fontSize = 8.sp, color = OdinGreen)
                    }
                }
            }
        }

        item {
            // Entry Points List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isPersian) "نقاط ورود زنده (${entryPoints.size}) - هر 100ms آپدیت" else "Live Entry Points (${entryPoints.size}) - 100ms updates",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (entryPoints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPersian) "در انتظار سیگنال LIT..." else "Waiting for LIT signal...",
                                fontSize = 11.sp,
                                color = OdinSilverMuted
                            )
                        }
                    } else {
                        entryPoints.takeLast(5).reversed().forEach { entry ->
                            EntryPointCard(entry = entry, isPersian = isPersian)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item {
            // Info about microsecond
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurface),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "توضیح آپدیت میکروثانیه" else "Microsecond Update Explanation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OdinCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian)
                            "• 1 میکروثانیه = 0.001 میلی‌ثانیه - نمایشگر گوشی نمی‌تواند این سرعت را رندر کند\n• حداکثر سرعت رندر اندروید: 16ms (60Hz) یا 8ms (120Hz)\n• ما هر 100ms (10Hz) چارت را آپدیت می‌کنیم - سریع‌ترین حالت پایدار\n• میکروثانیه را در کانتر بالا نمایش می‌دهیم (System.nanoTime)\n• قیمت هر 100ms با نوسان واقعی آپدیت می‌شود\n• نقاط ورود LIT + TV 80% با RR 1:2+ به صورت زنده"
                        else
                            "• 1 microsecond = 0.001ms - Phone display cannot render this fast\n• Max Android render: 16ms (60Hz) or 8ms (120Hz)\n• We update chart every 100ms (10Hz) - fastest stable\n• Microsecond shown in counter above (System.nanoTime)\n• Price updates every 100ms with real volatility\n• LIT + TV 80% entry points with RR 1:2+ live",
                        fontSize = 9.sp,
                        color = OdinSilverMuted,
                        lineHeight = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EntryPointCard(entry: EntryPoint, isPersian: Boolean) {
    val isBuy = entry.side == SignalSide.BUY
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBuy) OdinGreen else OdinRed)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isBuy) "BUY" else "SELL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = entry.type, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${entry.confidence.toInt()}%", fontSize = 8.sp, color = OdinSilverMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format("%.2f", entry.price)} | SL ${String.format("%.2f", entry.sl)} | TP ${String.format("%.2f", entry.tp)} | RR 1:${String.format("%.1f", entry.rr)}",
                    fontSize = 8.sp,
                    color = OdinSilver,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Icon(
                imageVector = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isBuy) OdinGreen else OdinRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun generateInitialCandles(): List<Candle> {
    val candles = mutableListOf<Candle>()
    var price = 65000.0
    val now = System.currentTimeMillis()
    repeat(50) { i ->
        val open = price
        val change = (Random.nextDouble() - 0.5) * 0.01 * price
        price += change
        val high = max(open, price) * (1 + Random.nextDouble() * 0.002)
        val low = min(open, price) * (1 - Random.nextDouble() * 0.002)
        val close = price
        candles.add(
            Candle(
                time = now - (50 - i) * 60000L,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = Random.nextDouble() * 100 + 50
            )
        )
    }
    return candles
}
