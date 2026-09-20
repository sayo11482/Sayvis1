package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

/**
 * ODIN v1.0.24 - Scanner با انتخاب نماد + نمایش استراتژی + اسپرد - VITTAVERSE ONLY
 * فقط بروکر ویتاورس - https://vittaverse.com/fa/
 * قیمت‌ها هر لحظه در نوسان - واحد مشخص تومان/تتر - اسپرد محاسبه
 */

@Composable
fun EntryScannerScreen(
    isPersian: Boolean,
    realDataManager: RealMarketDataManager = remember { RealMarketDataManager() },
    onSignalClick: ((EntrySignal) -> Unit)? = null
) {
    val context = LocalContext.current
    val scanner = remember { EntryScannerWithAlarm(context) }
    var isScanning by remember { mutableStateOf(false) }
    var signals by remember { mutableStateOf<List<EntrySignal>>(emptyList()) }
    var totalAlarms by remember { mutableStateOf(0) }
    var alarmEnabled by remember { mutableStateOf(true) }
    var autoTradeEnabled by remember { mutableStateOf(false) }
    var maxTrades by remember { mutableStateOf(5) }
    var maxTradesPerDay by remember { mutableStateOf(10) }
    var currentTrades by remember { mutableStateOf(0) }
    var tradesToday by remember { mutableStateOf(0) }
    var realPrices by remember { mutableStateOf<Map<String, RealPrice>>(emptyMap()) }
    var selectedSymbol by remember { mutableStateOf("ALL") }
    var capital by remember { mutableStateOf(100.0) }
    var strategiesChecked by remember { mutableStateOf(0) }
    var expandedSymbol by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning, selectedSymbol) {
        if (isScanning) {
            scanner.startScanning()
            scanner.setSelectedSymbol(selectedSymbol)
            while (isScanning) {
                realPrices = realDataManager.fetchRealPrices()
                val candlesMap = realPrices.keys.associateWith { sym -> realDataManager.getCandles(sym) }.filter { it.value.size >= 20 }

                val newSignals = scanner.scanForEntries(
                    minConfidence = 75.0,
                    realPrices = realPrices,
                    candlesMap = candlesMap,
                    selectedSymbolFilter = selectedSymbol
                )
                signals = scanner.getRecentSignals(30)
                totalAlarms = scanner.state.value.totalAlarms
                currentTrades = scanner.state.value.autoTradeConfig.currentTrades
                tradesToday = scanner.state.value.autoTradeConfig.tradesToday
                strategiesChecked = scanner.state.value.strategiesCheckedCount
                delay(1500) // هر 1.5 ثانیه آپدیت واقعی
            }
        } else {
            scanner.stopScanning()
        }
    }

    LaunchedEffect(Unit) {
        try {
            realPrices = realDataManager.fetchRealPrices()
        } catch (e: Exception) {}
    }

    val symbolList = remember { listOf("ALL") + SymbolManager.getTradableSymbols().map { it.symbol } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPersian) "اسکنر ویتاورس - انتخاب نماد + استراتژی + اسپرد" else "Vittaverse Scanner - Symbol + Strategy + Spread",
                                fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White
                            )
                            Text(
                                text = if (isPersian) "فقط بروکر ویتاورس - قیمت لحظه‌ای در نوسان - واحد تومان/تتر مشخص" else "Vittaverse ONLY - Live fluctuating prices - Unit Toman/USDT",
                                fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { isScanning = !isScanning },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) OdinRed else OdinGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                                contentDescription = null, modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isScanning) if (isPersian) "توقف" else "Stop" else if (isPersian) "اسکن واقعی" else "Scan REAL", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) "بروکر: ویتاورس - https://vittaverse.com/fa/ - داده واقعی جابه‌جا می‌شود: ${realPrices.size} نماد، ${scanner.state.value.strategiesCheckedCount} استراتژی بررسی شده"
                        else "Broker: Vittaverse - https://vittaverse.com/fa/ - REAL data moving: ${realPrices.size} symbols, ${scanner.state.value.strategiesCheckedCount} strategies checked",
                        fontSize = 7.sp, color = OdinCyan, lineHeight = 9.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "انتخاب نماد برای اسکن - ویتاورس" else "Select Symbol to Scan - Vittaverse", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedSymbol = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OdinGold),
                                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (selectedSymbol == "ALL") if (isPersian) "همه نمادها (${symbolList.size-1})" else "All Symbols (${symbolList.size-1})" else selectedSymbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            DropdownMenu(expanded = expandedSymbol, onDismissRequest = { expandedSymbol = false }, modifier = Modifier.background(Color(0xFF1A1A1A))) {
                                symbolList.take(50).forEach { sym ->
                                    val info = SymbolManager.find(sym)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = sym, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                if (info != null) {
                                                    val rp = realPrices[sym]
                                                    Text(
                                                        text = "${info.nameFa} | ${info.unit} | اسپرد ${info.spreadTypical} | ${rp?.let { if (it.unit == "Toman") "${String.format("%.0f", it.price)} تومان" else "${String.format("%.2f", it.price)} تتر" } ?: "واقعی"}",
                                                        fontSize = 8.sp, color = OdinSilverMuted
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedSymbol = sym
                                            scanner.setSelectedSymbol(sym)
                                            expandedSymbol = false
                                        }
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(OdinCyan.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(text = if (isPersian) "${realPrices.size} قیمت زنده" else "${realPrices.size} live", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SymbolManager.getPopular()) { sym ->
                            val rp = realPrices[sym.symbol]
                            FilterChip(
                                selected = selectedSymbol == sym.symbol,
                                onClick = { selectedSymbol = sym.symbol; scanner.setSelectedSymbol(sym.symbol) },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(sym.symbol, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        if (rp != null) {
                                            Text(
                                                text = if (rp.unit == "Toman") "${String.format("%.0f", rp.price)} ت" else "${String.format("%.2f", rp.price)}",
                                                fontSize = 7.sp, color = if (rp.changePercent >= 0) OdinGreen else OdinRed
                                            )
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.25f), selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "آلارم واقعی" else "Alarms REAL", fontSize = 7.sp, color = OdinSilverMuted)
                        Text(text = "$totalAlarms", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGold)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "سیگنال واقعی" else "Signals REAL", fontSize = 7.sp, color = OdinSilverMuted)
                        Text(text = "${signals.size}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "استراتژی چک شده" else "Strategies Checked", fontSize = 6.sp, color = OdinSilverMuted)
                        Text(text = "$strategiesChecked", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "سرمایه" else "Capital", fontSize = 7.sp, color = OdinSilverMuted)
                        Text(text = "$${capital.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "سرمایه ورودی - قابل تنظیم - نه 10$ ثابت" else "Capital Input - Adjustable - Not fixed 10$", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(text = if (isPersian) "ویتاورس" else "Vittaverse", fontSize = 8.sp, color = OdinCyan, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinCyan.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (capital > 10) { capital -= 10; scanner.setCapital(capital) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$${capital.toInt()} USDT", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                            Text(text = if (isPersian) "${(capital * 235000).toInt()} تومان - ویتاورس واقعی" else "${(capital * 235000).toInt()} Toman - Vittaverse REAL", fontSize = 9.sp, color = OdinSilverMuted)
                            Slider(value = capital.toFloat(), onValueChange = { capital = it.toDouble(); scanner.setCapital(capital) }, valueRange = 10f..10000f, colors = SliderDefaults.colors(thumbColor = OdinGold, activeTrackColor = OdinGold))
                        }
                        IconButton(onClick = { if (capital < 10000) { capital += 10; scanner.setCapital(capital) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = if (isPersian) "قبلاً همه 10$ ثابت بود - الان قابل تنظیم 10 تا 10000 تتر - اسپرد ویتاورس محاسبه می‌شود"
                        else "Previously fixed 10$ - Now adjustable 10-10000 USDT - Vittaverse spread calculated",
                        fontSize = 8.sp, color = OdinSilverDim
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "آلارم صوتی" else "Beep Alarm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it; scanner.setAlarmEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = OdinGold, checkedTrackColor = OdinGold.copy(alpha = 0.3f)))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "ترید اتومات ویتاورس واقعی" else "Auto Trade Vittaverse REAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(checked = autoTradeEnabled, onCheckedChange = { autoTradeEnabled = it; scanner.setAutoTradeEnabled(it) }, colors = SwitchDefaults.colors(checkedThumbColor = OdinCyan, checkedTrackColor = OdinCyan.copy(alpha = 0.3f)))
                    }
                    if (autoTradeEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian) "✅ اعتماد 80%+ و RR 1:2+ و Confluence 5+ و اسپرد ویتاورس محاسبه → ترید واقعی\n✅ سرمایه ${capital.toInt()}$ - $currentTrades/$maxTrades باز"
                            else "✅ Conf 80%+ RR 1:2+ Confl 5+ Spread Vittaverse → REAL trade\n✅ Capital ${capital.toInt()}$ - $currentTrades/$maxTrades open",
                            fontSize = 8.sp, color = OdinSilver
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isPersian) "سیگنال‌های زنده ویتاورس - ${if (selectedSymbol == "ALL") "همه نمادها" else selectedSymbol} - ${signals.size} سیگنال - با استراتژی و اسپرد"
                else "Live Vittaverse Signals - ${if (selectedSymbol == "ALL") "All" else selectedSymbol} - ${signals.size} signals - With Strategy & Spread",
                fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp
            )
        }

        if (signals.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPersian) "در حال اسکن ${if (selectedSymbol == "ALL") "${SymbolManager.allSymbols.size} نماد" else selectedSymbol} با 5 استراتژی واقعی..."
                                else "Scanning ${if (selectedSymbol == "ALL") "${SymbolManager.allSymbols.size} symbols" else selectedSymbol} with 5 REAL strategies...",
                                fontSize = 11.sp, color = OdinSilverMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPersian) "قیمت‌ها هر لحظه در نوسان - داده واقعی ویتاورس جابه‌جا می‌شود - ${realPrices.size} قیمت زنده"
                                else "Prices fluctuating every moment - REAL Vittaverse data moving - ${realPrices.size} live",
                                fontSize = 8.sp, color = OdinCyan
                            )
                            if (isScanning) {
                                Spacer(modifier = Modifier.height(6.dp))
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OdinCyan, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        } else {
            items(signals.takeLast(20).reversed()) { signal ->
                SignalCardVittaverse(signal = signal, isPersian = isPersian, onClick = { onSignalClick?.invoke(signal) })
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SignalCardVittaverse(signal: EntrySignal, isPersian: Boolean, onClick: (() -> Unit)? = null) {
    val isBuy = signal.side == SignalSide.BUY
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        onClick = { onClick?.invoke() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isBuy) OdinGreen else OdinRed).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(text = if (isBuy) if (isPersian) "خرید" else "BUY" else if (isPersian) "فروش" else "SELL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = signal.symbol, fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                        Text(text = "${signal.unitFa} - ${signal.broker}", fontSize = 7.sp, color = OdinCyan)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OdinGold, modifier = Modifier.size(12.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "${signal.confidence.toInt()}% ${if (isPersian) "اطمینان" else "Conf"}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGold.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    Text(text = "RR 1:${String.format("%.1f", signal.rr)}", fontSize = 8.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(6.dp)) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "${if (isPersian) "قیمت" else "Price"} ${if (signal.unit == "Toman") "${String.format("%.0f", signal.price)} ${signal.unitFa}" else "${String.format("%.4f", signal.price)} ${signal.unitFa}"}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "Confl ${signal.confluence}", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Bid ${String.format(if (signal.unit == "Toman") "%.0f" else "%.4f", signal.bid)}", fontSize = 8.sp, color = OdinGreen)
                        Text(text = "Ask ${String.format(if (signal.unit == "Toman") "%.0f" else "%.4f", signal.ask)}", fontSize = 8.sp, color = OdinRed)
                        Text(text = "Spread ${String.format(if (signal.unit == "Toman") "%.0f" else "%.4f", signal.spread)} ${signal.unitFa}", fontSize = 8.sp, color = OdinGold)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) "هزینه اسپرد: ${if (signal.unit == "Toman") "${String.format("%.0f", signal.spreadCostToman)} تومان" else "${String.format("%.4f", signal.spreadCostUSDT)} تتر"} - ${signal.unitFa} - ویتاورس"
                        else "Spread Cost: ${if (signal.unit == "Toman") "${String.format("%.0f", signal.spreadCostToman)} Toman" else "${String.format("%.4f", signal.spreadCostUSDT)} USDT"} - Vittaverse",
                        fontSize = 7.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) "قیمت تومان: ${String.format("%.0f", signal.priceToman)} تومان | تتر: ${String.format("%.2f", signal.priceUSDT)} تتر"
                        else "Toman: ${String.format("%.0f", signal.priceToman)} | USDT: ${String.format("%.2f", signal.priceUSDT)}",
                        fontSize = 7.sp, color = OdinSilverDim
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGold.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.2f)), shape = RoundedCornerShape(6.dp)) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text(text = if (isPersian) "استراتژی بررسی شده: ${signal.strategy.name}" else "Strategy Checked: ${signal.strategy.name}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                    Text(text = if (isPersian) signal.strategyDetails else signal.strategyDetails, fontSize = 8.sp, color = Color.White, lineHeight = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = if (isPersian) "تمام استراتژی‌های بررسی شده (${signal.allStrategiesChecked.size}):" else "All Checked (${signal.allStrategiesChecked.size}):", fontSize = 7.sp, color = OdinSilverMuted, fontWeight = FontWeight.Bold)
                    signal.allStrategiesChecked.take(3).forEach { check ->
                        Text(text = "• $check", fontSize = 6.sp, color = OdinSilverDim, lineHeight = 8.sp, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = signal.reason, fontSize = 8.sp, color = OdinSilver, lineHeight = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${(System.currentTimeMillis() - signal.timestamp)/1000}${if (isPersian) "ثانیه پیش" else "s ago"} • ${signal.source}", fontSize = 7.sp, color = OdinSilverMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OdinGold, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = if (isPersian) "بوق ویتاورس واقعی" else "Beep Vittaverse REAL", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
