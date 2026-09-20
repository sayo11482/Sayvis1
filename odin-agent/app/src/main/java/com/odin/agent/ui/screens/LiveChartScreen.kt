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
 * ODIN v1.0.24 - LiveChart - VITTAVERSE ONLY - قیمت هر لحظه نوسان واقعی - واحد تومان/تتر مشخص - اسپرد محاسبه
 */

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
    var currentPrice by remember { mutableStateOf(initialPrice ?: 0.0) }
    var bidPrice by remember { mutableStateOf(0.0) }
    var askPrice by remember { mutableStateOf(0.0) }
    var isLive by remember { mutableStateOf(true) }
    var selectedSymbol by remember { mutableStateOf(initialSignal?.symbol ?: "EURUSD") }
    var selectedTF by remember { mutableStateOf(ChartTimeframe.M15) }
    var priceSource by remember { mutableStateOf(if (isPersian) "ویتاورس واقعی - در حال نوسان" else "Vittaverse REAL - Fluctuating") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SymbolSortMode.POPULAR) }
    var isLoadingReal by remember { mutableStateOf(true) }
    var priceChangeAnim by remember { mutableStateOf(false) }

    LaunchedEffect(isLive, selectedSymbol) {
        if (isLive) realDataManager.startPolling(1000L)
        else realDataManager.stopPolling()
    }

    LaunchedEffect(selectedSymbol, selectedTF) {
        while (true) {
            try {
                isLoadingReal = true
                marketState = realDataManager.state.value
                val real = marketState.prices[selectedSymbol] ?: marketState.prices[selectedSymbol.replace("/", "")]
                if (real != null) {
                    val oldPrice = currentPrice
                    currentPrice = real.price
                    bidPrice = real.bid
                    askPrice = real.ask
                    priceSource = "${real.source} - ویتاورس واقعی - ${real.unitFa} - اسپرد ${real.spread} - نوسان لحظه‌ای"
                    if (oldPrice != 0.0 && oldPrice != currentPrice) {
                        priceChangeAnim = true
                        delay(200)
                        priceChangeAnim = false
                    }
                    candles = realDataManager.getCandles(selectedSymbol)
                    if (candles.isEmpty()) candles = realDataManager.getCandles(selectedSymbol.replace("/", ""))
                    val needed = when (selectedTF) { ChartTimeframe.M1 -> 120; ChartTimeframe.M5 -> 100; ChartTimeframe.M15 -> 80; ChartTimeframe.M30 -> 60; ChartTimeframe.H1 -> 50; ChartTimeframe.H4 -> 40; ChartTimeframe.D1 -> 30 }
                    if (candles.size > needed) candles = aggregateCandlesV24(candles, selectedTF.minutes)

                    val allCandlesMap = marketState.prices.keys.associateWith { sym -> realDataManager.getCandles(sym) }.filter { it.value.size >= 20 }
                    val realSignals = scanner.scanForEntries(minConfidence = 75.0, realPrices = marketState.prices, candlesMap = allCandlesMap, selectedSymbolFilter = selectedSymbol)
                    if (realSignals.isNotEmpty()) {
                        val newEntries = realSignals.map { sig ->
                            EntryPoint(id = sig.id, time = sig.timestamp, price = sig.price, side = sig.side, type = "${sig.strategyDetails} | ${sig.strategy.labelFa}", rr = sig.rr, confidence = sig.confidence, sl = sig.sl, tp = sig.tp, status = "active", symbol = sig.symbol)
                        }
                        entryPoints = (entryPoints + newEntries).takeLast(20)
                    }
                } else {
                    priceSource = if (isPersian) "ویتاورس - ${selectedSymbol} - در حال دریافت قیمت واقعی..." else "Vittaverse - ${selectedSymbol} - Fetching REAL price..."
                }
                isLoadingReal = false
            } catch (e: Exception) {
                priceSource = if (isPersian) "خطا ویتاورس: ${e.message}" else "Vittaverse Error: ${e.message}"
                isLoadingReal = false
            }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        candles = realDataManager.getCandles(selectedSymbol)
        if (initialSignal != null) {
            selectedSymbol = initialSignal.symbol
            currentPrice = initialSignal.price
            val entry = EntryPoint(id = initialSignal.id, time = initialSignal.timestamp, price = initialSignal.price, side = initialSignal.side, type = "${initialSignal.strategyDetails} | ${initialSignal.broker} - ویتاورس", rr = initialSignal.rr, confidence = initialSignal.confidence, sl = initialSignal.sl, tp = initialSignal.tp, status = "active", symbol = initialSignal.symbol)
            entryPoints = listOf(entry)
        }
    }

    val filteredSymbols = remember(searchQuery, sortMode, marketState) {
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
    val realPriceInfo = marketState.prices[selectedSymbol] ?: marketState.prices[selectedSymbol.replace("/", "")]
    val unitLabel = realPriceInfo?.unit ?: symInfo?.unit ?: "USDT"
    val unitFaLabel = realPriceInfo?.unitFa ?: if (unitLabel == "Toman") "تومان" else "تتر"
    val spreadCostToman = realPriceInfo?.spreadCostToman ?: 0.0
    val spreadCostUSDT = realPriceInfo?.spreadCostUSDT ?: 0.0
    val tomanValue = realPriceInfo?.priceToman ?: if (symInfo?.category == SymbolCategory.FOREX_IRR) currentPrice else currentPrice * 235000
    val usdtValue = realPriceInfo?.priceUSDT ?: currentPrice

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ODIN × ویتاورس - چارت زنده واقعی", fontSize = 13.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isLive) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (isLive) if (isPersian) "● زنده ویتاورس نوسان" else "● LIVE Vittaverse Fluct" else if (isPersian) "○ متوقف" else "○ PAUSED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (isLive) OdinGreen else OdinRed)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(checked = isLive, onCheckedChange = { isLive = it }, colors = SwitchDefaults.colors(checkedThumbColor = OdinGreen, checkedTrackColor = OdinGreen.copy(alpha = 0.3f)), modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (priceChangeAnim) OdinGold.copy(alpha = 0.15f) else OdinGold.copy(alpha = 0.08f)), border = BorderStroke(1.dp, if (priceChangeAnim) OdinGold.copy(alpha = 0.5f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = if (isPersian) "نماد ویتاورس" else "Vittaverse Symbol", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(text = selectedSymbol, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Text(text = symInfo?.nameFa ?: symInfo?.displayName ?: "", fontSize = 8.sp, color = OdinSilverDim)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = if (isPersian) "واحد مشخص" else "Unit Specified", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(text = unitFaLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                                    Text(text = unitLabel, fontSize = 8.sp, color = OdinCyan)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = if (isPersian) "قیمت لحظه‌ای واقعی نوسان" else "Live REAL Fluctuating", fontSize = 8.sp, color = OdinSilverMuted)
                                    if (currentPrice > 0) {
                                        Text(text = SymbolManager.formatPrice(selectedSymbol, currentPrice), fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (priceChangeAnim) OdinGoldLight else Color.White)
                                        Text(text = if (unitLabel == "Toman") "${String.format("%,.0f تومان", tomanValue)} | ${String.format("%.2f", usdtValue)} تتر" else "${String.format("%,.0f تومان", tomanValue)}", fontSize = 7.sp, color = OdinSilverMuted)
                                    } else {
                                        Text(text = if (isPersian) "در حال نوسان ویتاورس..." else "Vittaverse fluctuating...", fontSize = 10.sp, color = OdinSilverMuted)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Bid/Ask/Spread واقعی ویتاورس
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (currentPrice > 0) {
                                    Column {
                                        Text(text = if (isPersian) "خرید Bid واقعی ویتاورس" else "Bid REAL Vittaverse", fontSize = 7.sp, color = OdinSilverMuted)
                                        Text(text = SymbolManager.formatPrice(selectedSymbol, bidPrice), fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(text = if (isPersian) "فروش Ask واقعی ویتاورس" else "Ask REAL Vittaverse", fontSize = 7.sp, color = OdinSilverMuted)
                                        Text(text = SymbolManager.formatPrice(selectedSymbol, askPrice), fontSize = 9.sp, color = OdinRed, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(text = if (isPersian) "اسپرد ویتاورس واقعی" else "Spread Vittaverse REAL", fontSize = 7.sp, color = OdinSilverMuted)
                                        Text(text = "${realPriceInfo?.spread ?: symInfo?.spreadTypical ?: 1.2} | ${if (unitLabel == "Toman") "${spreadCostToman.toInt()} تومان" else "${String.format("%.2f", spreadCostUSDT)} تتر"}", fontSize = 8.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(text = if (isPersian) "بروکر" else "Broker", fontSize = 7.sp, color = OdinSilverMuted)
                                        Text(text = "Vittaverse", fontSize = 8.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = priceSource, fontSize = 7.sp, color = OdinCyan, lineHeight = 8.sp)
                            if (marketState.isConnected) {
                                LinearProgressIndicator(progress = ((marketState.updateCount % 100) / 100f), modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).padding(top = 4.dp), color = OdinGreen, trackColor = Color(0xFF1A1A1A))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "تایم فریم: ${selectedTF.labelFa} • ویتاورس https://vittaverse.com/fa/ • قیمت هر لحظه نوسان" else "TF: ${selectedTF.label} • Vittaverse https://vittaverse.com/fa/ • Fluctuating every moment", fontSize = 8.sp, color = OdinCyan)
                        if (isLoadingReal) CircularProgressIndicator(modifier = Modifier.size(12.dp), color = OdinGold, strokeWidth = 1.5.dp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it; if (it.isNotBlank()) sortMode = SymbolSortMode.SEARCH }, label = { Text(if (isPersian) "جستجو نماد ویتاورس..." else "Search Vittaverse symbol...", fontSize = 9.sp) }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) }, trailingIcon = { if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = ""; sortMode = SymbolSortMode.POPULAR }) { Icon(Icons.Default.Clear, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(16.dp)) } }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, shape = RoundedCornerShape(10.dp))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = sortMode == SymbolSortMode.POPULAR, onClick = { sortMode = SymbolSortMode.POPULAR; searchQuery = "" }, label = { Text(if (isPersian) "محبوب ویتاورس" else "Popular Vittaverse", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.25f), selectedLabelColor = Color.White)) }
                item { FilterChip(selected = sortMode == SymbolSortMode.ALL, onClick = { sortMode = SymbolSortMode.ALL; searchQuery = "" }, label = { Text(if (isPersian) "همه ویتاورس" else "All Vittaverse", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = Color.White)) }
                item { FilterChip(selected = sortMode == SymbolSortMode.CRYPTO, onClick = { sortMode = SymbolSortMode.CRYPTO; searchQuery = "" }, label = { Text("Crypto", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = Color.White)) }
                item { FilterChip(selected = sortMode == SymbolSortMode.FOREX, onClick = { sortMode = SymbolSortMode.FOREX; searchQuery = "" }, label = { Text("Forex Vittaverse", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGreen.copy(alpha = 0.2f), selectedLabelColor = Color.White)) }
                item { FilterChip(selected = sortMode == SymbolSortMode.IRR, onClick = { sortMode = SymbolSortMode.IRR; searchQuery = "" }, label = { Text(if (isPersian) "ریالی تومان" else "IRR Toman", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinRed.copy(alpha = 0.25f), selectedLabelColor = Color.White)) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredSymbols) { sym ->
                    val rp = marketState.prices[sym.symbol] ?: marketState.prices[sym.symbol.replace("/", "")]
                    FilterChip(selected = selectedSymbol == sym.symbol, onClick = { selectedSymbol = sym.symbol }, label = {
                        Column {
                            Text(sym.symbol, fontSize = 8.sp, maxLines = 1, fontWeight = if (selectedSymbol == sym.symbol) FontWeight.Black else FontWeight.Normal)
                            if (rp != null) Text("${if (rp.unit == "Toman") String.format("%,.0f", rp.price) + " تومان" else String.format("%.2f", rp.price)} | ${rp.unitFa}", fontSize = 6.sp, color = OdinSilverDim)
                            else Text("ویتاورس", fontSize = 6.sp, color = OdinGold.copy(alpha = 0.6f))
                        }
                    }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = when (sym.category) { SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.3f); SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.2f); else -> OdinGold.copy(alpha = 0.2f) }, selectedLabelColor = Color.White), border = FilterChipDefaults.filterChipBorder(borderColor = if (selectedSymbol == sym.symbol) OdinGold else Color(0xFF1A1A1A), selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.dp, enabled = true, selected = selectedSymbol == sym.symbol))
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChartTimeframe.values().toList()) { tf ->
                    FilterChip(selected = selectedTF == tf, onClick = { selectedTF = tf }, label = { Text(if (isPersian) tf.labelFa else tf.label, fontSize = 10.sp, fontWeight = if (selectedTF == tf) FontWeight.Black else FontWeight.Normal) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.25f), selectedLabelColor = Color.White), border = FilterChipDefaults.filterChipBorder(borderColor = if (selectedTF == tf) OdinGold else Color(0xFF1A1A1A), selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.2.dp, enabled = true, selected = selectedTF == tf))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, Color(0xFF1A1A1A)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) "ویتاورس $selectedSymbol ${selectedTF.labelFa} - واحد $unitFaLabel - اسپرد واقعی - نوسان" else "Vittaverse $selectedSymbol ${selectedTF.label} - Unit $unitFaLabel - Spread REAL - Fluctuating", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = if (isPersian) "خرید" else "BUY", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGreen) }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinRed.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = if (isPersian) "فروش" else "SELL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinRed) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (candles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = if (isPersian) "کندل واقعی ویتاورس ${selectedSymbol} - در حال نوسان..." else "Vittaverse REAL candle ${selectedSymbol} - Fluctuating...", fontSize = 10.sp, color = OdinSilverMuted)
                                Text(text = if (isPersian) "قیمت هر لحظه نوسان - ویتاورس واقعی" else "Price fluctuating every moment - Vittaverse REAL", fontSize = 8.sp, color = OdinSilverDim)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
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
                                    Text(text = if (isPersian) "ویتاورس $selectedSymbol ${selectedTF.labelFa} • واحد $unitFaLabel • اسپرد ${realPriceInfo?.spread ?: symInfo?.spreadTypical} • نوسان لحظه‌ای" else "Vittaverse $selectedSymbol ${selectedTF.label} • Unit $unitFaLabel • Spread ${realPriceInfo?.spread} • Fluctuating", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                                    Text(text = if (isPersian) "منبع: $priceSource • داده منتقل: ${marketState.dataTransferred / 1024} KB" else "Source: $priceSource • Data: ${marketState.dataTransferred / 1024} KB", fontSize = 7.sp, color = OdinGreen)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "حد ضرر" else "SL", fontSize = 8.sp, color = OdinRed)
                        Text(text = if (isPersian) "ورود ${selectedSymbol} ویتاورس - واحد $unitFaLabel - اسپرد واقعی - نوسان" else "Entry ${selectedSymbol} Vittaverse - Unit $unitFaLabel - Spread REAL - Fluctuating", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = if (isPersian) "حد سود" else "TP", fontSize = 8.sp, color = OdinGreen)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "نقاط ورود ویتاورس واقعی ${selectedSymbol} (${entryPoints.filter { it.symbol == selectedSymbol }.size}) - استراتژی بررسی شده + اسپرد" else "Vittaverse REAL Entries ${selectedSymbol} (${entryPoints.filter { it.symbol == selectedSymbol }.size}) - Strategy Checked + Spread", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    val filtered = entryPoints.filter { it.symbol == selectedSymbol }
                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = if (isPersian) "سیگنال ویتاورس واقعی یافت نشد - ۵ استراتژی بررسی + اسپرد محاسبه" else "No Vittaverse REAL signal - 5 strategies checked + spread calc", fontSize = 9.sp, color = OdinSilverMuted)
                            }
                        }
                    } else {
                        filtered.takeLast(5).reversed().forEach { entry ->
                            EntryPointCardV24(entry = entry, isPersian = isPersian)
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
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)), border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isBuy) OdinGreen else OdinRed).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(text = if (isBuy) if (isPersian) "خرید" else "BUY" else if (isPersian) "فروش" else "SELL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${entry.type.take(40)} - ویتاورس", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${SymbolManager.formatPrice(entry.symbol, entry.price)} | SL ${SymbolManager.formatPrice(entry.symbol, entry.sl)} | TP ${SymbolManager.formatPrice(entry.symbol, entry.tp)} | RR 1:${String.format("%.1f", entry.rr)} | ${entry.confidence.toInt()}% | ویتاورس", fontSize = 7.sp, color = OdinSilver, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Icon(imageVector = if (isBuy) Icons.Default.TrendingUp else Icons.Default.TrendingDown, contentDescription = null, tint = if (isBuy) OdinGreen else OdinRed, modifier = Modifier.size(16.dp))
        }
    }
}
