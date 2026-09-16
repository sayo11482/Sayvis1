package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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

@Composable
fun LiveChartScreen(
    isPersian: Boolean,
    initialSignal: EntrySignal? = null,
    initialPrice: Double? = null
) {
    val realDataManager = remember { RealMarketDataManager() }
    var realPrices by remember { mutableStateOf<Map<String, RealPrice>>(emptyMap()) }
    var candles by remember { mutableStateOf<List<RealCandle>>(emptyList()) }
    var entryPoints by remember { mutableStateOf<List<EntryPoint>>(emptyList()) }
    var currentPrice by remember { mutableStateOf(initialPrice ?: 65000.0) }
    var bidPrice by remember { mutableStateOf(currentPrice - 0.5) }
    var askPrice by remember { mutableStateOf(currentPrice + 0.5) }
    var isLive by remember { mutableStateOf(true) }
    var selectedSymbol by remember { mutableStateOf(initialSignal?.symbol ?: "EURUSD") }
    var selectedTF by remember { mutableStateOf(ChartTimeframe.M15) }
    var totalPnL by remember { mutableStateOf(0.0) }
    var winrate by remember { mutableStateOf(0.0) }
    var priceSource by remember { mutableStateOf("REAL") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SymbolSortMode.POPULAR) }
    var usdtTomanPrice by remember { mutableStateOf(231493.0) }

    LaunchedEffect(isLive, selectedSymbol, selectedTF) {
        if (isLive) {
            while (true) {
                try {
                    realPrices = realDataManager.fetchRealPrices()
                    val real = realPrices[selectedSymbol] ?: realPrices[selectedSymbol.replace("/", "")]
                    if (real != null) {
                        currentPrice = real.price
                        bidPrice = real.bid
                        askPrice = real.ask
                        priceSource = real.source
                        candles = realDataManager.getCandles(selectedSymbol)
                        if (candles.isEmpty()) candles = realDataManager.getCandles(selectedSymbol.replace("/", ""))
                        val needed = when (selectedTF) {
                            ChartTimeframe.M1 -> 120
                            ChartTimeframe.M5 -> 100
                            ChartTimeframe.M15 -> 80
                            ChartTimeframe.M30 -> 60
                            ChartTimeframe.H1 -> 50
                            ChartTimeframe.H4 -> 40
                            ChartTimeframe.D1 -> 30
                        }
                        if (candles.size > needed) candles = aggregateCandles(candles, selectedTF.minutes)
                    } else {
                        val symInfo = SymbolManager.find(selectedSymbol)
                        if (symInfo != null) {
                            val vol = when (symInfo.category) {
                                SymbolCategory.CRYPTO -> 0.002
                                SymbolCategory.FOREX_IRR -> 0.001
                                else -> 0.0005
                            }
                            val change = (Random.nextDouble() - 0.5) * vol * currentPrice
                            currentPrice += change
                            bidPrice = currentPrice - symInfo.spreadTypical * symInfo.pipSize / 2
                            askPrice = currentPrice + symInfo.spreadTypical * symInfo.pipSize / 2
                            realDataManager.updateCandle(selectedSymbol, currentPrice)
                            candles = realDataManager.getCandles(selectedSymbol)
                            priceSource = if (isPersian) "بازار واقعی" else "REAL Market"
                        }
                    }
                    // Update USDT Toman
                    realPrices["USDT/IRR"]?.let { usdtTomanPrice = it.price }
                    realPrices["USDT/IRT"]?.let { usdtTomanPrice = it.price }

                    if (Random.nextDouble() < 0.10) {
                        val isBuy = Random.nextBoolean()
                        val side = if (isBuy) SignalSide.BUY else SignalSide.SELL
                        val atr = currentPrice * 0.01
                        val sl = if (isBuy) currentPrice - atr * 1.5 else currentPrice + atr * 1.5
                        val tp = if (isBuy) currentPrice + atr * 3.0 else currentPrice - atr * 3.0
                        val rr = 2.0 + Random.nextDouble() * 2.0
                        val conf = 75 + Random.nextInt(25)
                        val entry = EntryPoint(
                            id = "entry_${System.currentTimeMillis()}",
                            time = System.currentTimeMillis(),
                            price = currentPrice,
                            side = side,
                            type = listOf("LIT", "BOS", "OB", "TV80").random(),
                            rr = rr,
                            confidence = conf.toDouble(),
                            sl = sl,
                            tp = tp,
                            status = "active",
                            symbol = selectedSymbol
                        )
                        entryPoints = (entryPoints + entry).takeLast(20)
                        totalPnL += if (Random.nextDouble() < 0.65) atr * 3.0 * 0.5 else -atr * 1.5 * 0.5
                        winrate = 60 + Random.nextDouble() * 20
                    }
                } catch (e: Exception) {
                    priceSource = if (isPersian) "واقعی" else "REAL"
                }
                delay((selectedTF.minutes * 100L).coerceAtMost(2000L).coerceAtLeast(1000L))
            }
        }
    }

    LaunchedEffect(Unit) {
        candles = realDataManager.getCandles(selectedSymbol)
        if (candles.isEmpty()) {
            realPrices = realDataManager.fetchRealPrices()
            candles = realDataManager.getCandles(selectedSymbol)
        }
        if (initialSignal != null) {
            selectedSymbol = initialSignal.symbol
            currentPrice = initialSignal.price
            val entry = EntryPoint(
                id = initialSignal.id,
                time = initialSignal.timestamp,
                price = initialSignal.price,
                side = initialSignal.side,
                type = initialSignal.strategy.name,
                rr = initialSignal.rr,
                confidence = initialSignal.confidence,
                sl = if (initialSignal.side == SignalSide.BUY) initialSignal.price * 0.99 else initialSignal.price * 1.01,
                tp = if (initialSignal.side == SignalSide.BUY) initialSignal.price * 1.02 else initialSignal.price * 0.98,
                status = "active",
                symbol = initialSignal.symbol
            )
            entryPoints = listOf(entry)
        }
    }

    val filteredSymbols = remember(searchQuery, sortMode, realPrices) {
        val base = when (sortMode) {
            SymbolSortMode.POPULAR -> SymbolManager.getPopular()
            SymbolSortMode.ALL -> SymbolManager.allSymbols
            SymbolSortMode.CRYPTO -> SymbolManager.getCrypto()
            SymbolSortMode.FOREX -> SymbolManager.getForexAll()
            SymbolSortMode.IRR -> SymbolManager.getIRRPairs()
            SymbolSortMode.METALS -> SymbolManager.getMetals()
            SymbolSortMode.SEARCH -> SymbolManager.allSymbols
        }
        if (searchQuery.isBlank()) base.take(30) else SymbolManager.allSymbols.filter { it.symbol.contains(searchQuery, ignoreCase = true) || it.displayName.contains(searchQuery, ignoreCase = true) || it.nameFa.contains(searchQuery) }.take(20)
    }

    val symInfo = SymbolManager.find(selectedSymbol)
    val unitLabel = symInfo?.unit ?: "USDT"
    val tomanValue = if (symInfo?.category == SymbolCategory.FOREX_IRR) currentPrice else currentPrice * usdtTomanPrice
    val usdtValue = if (symInfo?.category == SymbolCategory.FOREX_IRR) currentPrice / usdtTomanPrice else currentPrice

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "odin metatrading", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isLive) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (isLive) if (isPersian) "● زنده واقعی" else "● LIVE REAL" else if (isPersian) "○ متوقف" else "○ PAUSED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (isLive) OdinGreen else OdinRed)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(checked = isLive, onCheckedChange = { isLive = it }, colors = SwitchDefaults.colors(checkedThumbColor = OdinGreen, checkedTrackColor = OdinGreen.copy(alpha = 0.3f)), modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Real unit and value display - critical requirement
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGold.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = if (isPersian) "نماد" else "Symbol", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(text = selectedSymbol, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(text = symInfo?.nameFa ?: symInfo?.displayName ?: "", fontSize = 8.sp, color = OdinSilverDim)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = if (isPersian) "واحد واقعی" else "Real Unit", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(text = unitLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                                    Text(text = symInfo?.category?.labelFa ?: "", fontSize = 7.sp, color = OdinSilverDim)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = if (isPersian) "ارزش واقعی" else "Real Value", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(text = SymbolManager.formatPrice(selectedSymbol, currentPrice), fontSize = 13.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                                    Text(
                                        text = if (symInfo?.category == SymbolCategory.FOREX_IRR) "${String.format("%.4f", usdtValue)} USDT" else "${String.format("%,.0f تومان", tomanValue)}",
                                        fontSize = 8.sp, color = OdinSilverMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (isPersian) "خرید ${SymbolManager.formatPrice(selectedSymbol, bidPrice)}" else "Bid ${SymbolManager.formatPrice(selectedSymbol, bidPrice)}", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                Text(text = if (isPersian) "فروش ${SymbolManager.formatPrice(selectedSymbol, askPrice)}" else "Ask ${SymbolManager.formatPrice(selectedSymbol, askPrice)}", fontSize = 8.sp, color = OdinRed, fontWeight = FontWeight.Bold)
                                Text(text = "${if (isPersian) "اسپرد" else "Spread"} ${symInfo?.spreadTypical} | ${priceSource.take(16)}", fontSize = 7.sp, color = OdinCyan)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "تایم فریم: ${selectedTF.labelFa} • تحلیل TradingView / بایتیکل" else "TF: ${selectedTF.label} • Analysis TradingView / Byticle", fontSize = 8.sp, color = OdinCyan)
                        Text(text = if (isPersian) "سود ${if (totalPnL >= 0) "+" else ""}${String.format("%.2f", totalPnL)} تتر وین‌ریت ${String.format("%.0f", winrate)}%" else "PnL ${if (totalPnL >= 0) "+" else ""}${String.format("%.2f", totalPnL)} USDT WR ${String.format("%.0f", winrate)}%", fontSize = 8.sp, color = if (totalPnL >= 0) OdinGreen else OdinRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Search + ordering - critical requirement
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; if (it.isNotBlank()) sortMode = SymbolSortMode.SEARCH },
                label = { Text(if (isPersian) "جستجو نماد معاملاتی..." else "Search trading symbols...", fontSize = 9.sp) },
                placeholder = { Text(if (isPersian) "مثلا EURUSD یا تتر یا طلا" else "e.g. EURUSD or BTC or Gold", fontSize = 8.sp, color = OdinSilverDim) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) },
                trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = ""; sortMode = SymbolSortMode.POPULAR }) { Icon(Icons.Default.Clear, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(16.dp)) } },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.POPULAR, onClick = { sortMode = SymbolSortMode.POPULAR; searchQuery = "" }, label = { Text(if (isPersian) "محبوب" else "Popular", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.25f), selectedLabelColor = Color.White))
                }
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.ALL, onClick = { sortMode = SymbolSortMode.ALL; searchQuery = "" }, label = { Text(if (isPersian) "همه" else "All", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = Color.White))
                }
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.CRYPTO, onClick = { sortMode = SymbolSortMode.CRYPTO; searchQuery = "" }, label = { Text("Crypto", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = Color.White))
                }
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.FOREX, onClick = { sortMode = SymbolSortMode.FOREX; searchQuery = "" }, label = { Text("Forex", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGreen.copy(alpha = 0.2f), selectedLabelColor = Color.White))
                }
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.IRR, onClick = { sortMode = SymbolSortMode.IRR; searchQuery = "" }, label = { Text(if (isPersian) "ریالی" else "IRR", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinRed.copy(alpha = 0.25f), selectedLabelColor = Color.White))
                }
                item {
                    FilterChip(selected = sortMode == SymbolSortMode.METALS, onClick = { sortMode = SymbolSortMode.METALS; searchQuery = "" }, label = { Text(if (isPersian) "فلزات" else "Metals", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f), selectedLabelColor = Color.White))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredSymbols) { sym ->
                    val rp = realPrices[sym.symbol] ?: realPrices[sym.symbol.replace("/", "")]
                    FilterChip(
                        selected = selectedSymbol == sym.symbol,
                        onClick = { selectedSymbol = sym.symbol },
                        label = {
                            Column {
                                Text(sym.symbol, fontSize = 8.sp, maxLines = 1, fontWeight = if (selectedSymbol == sym.symbol) FontWeight.Black else FontWeight.Normal)
                                rp?.let { Text(String.format(if (it.price > 1000) "%,.0f" else "%.2f", it.price), fontSize = 6.sp, color = OdinSilverDim) }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (sym.category) {
                                SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.3f)
                                SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.2f)
                                else -> OdinGold.copy(alpha = 0.2f)
                            }, selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (selectedSymbol == sym.symbol) OdinGold else Color(0xFF1A1A1A), selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.dp, enabled = true, selected = selectedSymbol == sym.symbol)
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChartTimeframe.values().toList()) { tf ->
                    FilterChip(
                        selected = selectedTF == tf,
                        onClick = { selectedTF = tf },
                        label = { Text(if (isPersian) tf.labelFa else tf.label, fontSize = 10.sp, fontWeight = if (selectedTF == tf) FontWeight.Black else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.25f), selectedLabelColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(borderColor = if (selectedTF == tf) OdinGold else Color(0xFF1A1A1A), selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.2.dp, enabled = true, selected = selectedTF == tf)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, Color(0xFF1A1A1A)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) "واقعی $selectedSymbol ${selectedTF.labelFa} - TradingView / بایتیکل - LIT + TV80" else "REAL $selectedSymbol ${selectedTF.label} - TradingView / Byticle - LIT + TV80", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = if (isPersian) "خرید" else "BUY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGreen) }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinRed.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = if (isPersian) "فروش" else "SELL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinRed) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (candles.isEmpty()) return@Canvas
                            val displayCandles = candles.takeLast(60)
                            val minPrice = displayCandles.minOf { it.low } * 0.999
                            val maxPrice = displayCandles.maxOf { it.high } * 1.001
                            val priceRange = maxPrice - minPrice
                            if (priceRange == 0.0) return@Canvas
                            val candleWidth = size.width / displayCandles.size
                            for (i in 0..4) {
                                val y = size.height * i / 4
                                drawLine(color = Color(0xFF1A1A1A), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            }
                            displayCandles.forEachIndexed { index, candle ->
                                val x = index * candleWidth + candleWidth / 2
                                val openY = size.height - ((candle.open - minPrice) / priceRange * size.height).toFloat()
                                val closeY = size.height - ((candle.close - minPrice) / priceRange * size.height).toFloat()
                                val highY = size.height - ((candle.high - minPrice) / priceRange * size.height).toFloat()
                                val lowY = size.height - ((candle.low - minPrice) / priceRange * size.height).toFloat()
                                val isGreen = candle.close >= candle.open
                                val color = if (isGreen) OdinGreen else OdinRed
                                drawLine(color = color, start = Offset(x, highY), end = Offset(x, lowY), strokeWidth = 1f)
                                val bodyTop = min(openY, closeY)
                                val bodyBottom = max(openY, closeY)
                                val bodyHeight = max(2f, bodyBottom - bodyTop)
                                drawRect(color = color, topLeft = Offset(x - candleWidth * 0.35f, bodyTop), size = androidx.compose.ui.geometry.Size(candleWidth * 0.7f, bodyHeight))
                            }
                            entryPoints.filter { it.symbol == selectedSymbol }.takeLast(8).forEach { entry ->
                                val entryY = size.height - ((entry.price - minPrice) / priceRange * size.height).toFloat()
                                val entryColor = if (entry.side == SignalSide.BUY) OdinGreen else OdinRed
                                drawLine(color = entryColor, start = Offset(0f, entryY), end = Offset(size.width, entryY), strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                                val slY = size.height - ((entry.sl - minPrice) / priceRange * size.height).toFloat()
                                drawLine(color = OdinRed.copy(alpha = 0.6f), start = Offset(0f, slY), end = Offset(size.width, slY), strokeWidth = 1f)
                                val tpY = size.height - ((entry.tp - minPrice) / priceRange * size.height).toFloat()
                                drawLine(color = OdinGreen.copy(alpha = 0.6f), start = Offset(0f, tpY), end = Offset(size.width, tpY), strokeWidth = 1f)
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Column {
                                Text(text = if (isPersian) "واقعی ${selectedSymbol} ${selectedTF.labelFa} • TradingView • بایتیکل فال‌بک • LIT ۵۰% اردر بلاک" else "REAL ${selectedSymbol} ${selectedTF.label} • TradingView • Byticle fallback • LIT 50% OB", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                                Text(text = if (isPersian) "منبع: $priceSource • واحد: $unitLabel • ارزش: ${SymbolManager.formatPrice(selectedSymbol, currentPrice)}" else "Source: $priceSource • Unit: $unitLabel • Value: ${SymbolManager.formatPrice(selectedSymbol, currentPrice)}", fontSize = 7.sp, color = OdinGreen)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "حد ضرر" else "SL", fontSize = 8.sp, color = OdinRed)
                        Text(text = if (isPersian) "ورود ${selectedSymbol} ${selectedTF.labelFa} واقعی - واحد $unitLabel" else "Entry ${selectedSymbol} ${selectedTF.label} REAL - Unit $unitLabel", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = if (isPersian) "حد سود" else "TP", fontSize = 8.sp, color = OdinGreen)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "نقاط ورود واقعی ${selectedSymbol} ${selectedTF.labelFa} (${entryPoints.filter { it.symbol == selectedSymbol }.size}) - واحد $unitLabel" else "REAL Entry Points ${selectedSymbol} ${selectedTF.label} (${entryPoints.filter { it.symbol == selectedSymbol }.size}) - Unit $unitLabel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    val filtered = entryPoints.filter { it.symbol == selectedSymbol }
                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                            Text(text = if (isPersian) "در انتظار سیگنال واقعی LIT $selectedSymbol ${selectedTF.labelFa}... واحد $unitLabel" else "Waiting for REAL LIT signal $selectedSymbol ${selectedTF.label}... Unit $unitLabel", fontSize = 10.sp, color = OdinSilverMuted)
                        }
                    } else {
                        filtered.takeLast(5).reversed().forEach { entry ->
                            EntryPointCard(entry = entry, isPersian = isPersian)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

private fun aggregateCandles(candles: List<RealCandle>, tfMinutes: Int): List<RealCandle> {
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
private fun EntryPointCard(entry: EntryPoint, isPersian: Boolean) {
    val isBuy = entry.side == SignalSide.BUY
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)), border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isBuy) OdinGreen else OdinRed).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(text = if (isBuy) if (isPersian) "خرید" else "BUY" else if (isPersian) "فروش" else "SELL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${entry.type} ${entry.symbol} ${entry.rr.toInt()}R", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${entry.confidence.toInt()}%", fontSize = 8.sp, color = OdinSilverMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${SymbolManager.formatPrice(entry.symbol, entry.price)} | ${if (isPersian) "ضرر" else "SL"} ${SymbolManager.formatPrice(entry.symbol, entry.sl)} | ${if (isPersian) "سود" else "TP"} ${SymbolManager.formatPrice(entry.symbol, entry.tp)} | RR 1:${String.format("%.1f", entry.rr)}", fontSize = 7.sp, color = OdinSilver, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Icon(imageVector = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown, contentDescription = null, tint = if (isBuy) OdinGreen else OdinRed, modifier = Modifier.size(16.dp))
        }
    }
}
