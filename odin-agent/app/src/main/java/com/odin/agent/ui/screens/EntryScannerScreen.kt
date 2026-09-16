package com.odin.agent.ui.screens

import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.ContinuousBacktestEngine
import com.odin.agent.trading.EntryScannerWithAlarm
import com.odin.agent.trading.EntrySignal
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EntryScannerScreen(
    isPersian: Boolean,
    backtestEngine: ContinuousBacktestEngine = remember { ContinuousBacktestEngine() }
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
    var bannedStrategies by remember { mutableStateOf<Set<QuantStrategyType>>(emptySet()) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanner.startScanning()
            while (isScanning) {
                // Get banned from backtest engine
                val banned = backtestEngine.getBannedStrategies().map { it.strategy }.toSet()
                bannedStrategies = banned

                val newSignals = scanner.scanForEntries(bannedStrategies = banned, minConfidence = 80.0)
                if (newSignals.isNotEmpty()) {
                    signals = scanner.getRecentSignals(20)
                    totalAlarms = scanner.state.value.totalAlarms
                    currentTrades = scanner.state.value.autoTradeConfig.currentTrades
                    tradesToday = scanner.state.value.autoTradeConfig.tradesToday
                }
                delay(1000) // Scan every 1 second
            }
        } else {
            scanner.stopScanning()
        }
    }

    LaunchedEffect(Unit) {
        // Initial backtest to get banned
        backtestEngine.runBacktestCycle()
        bannedStrategies = backtestEngine.getBannedStrategies().map { it.strategy }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPersian) "اسکنر ورود + آلارم + ترید اتومات" else "Entry Scanner + Alarm + Auto Trade",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "جستجوی دائمی نقطه ورود مناسب" else "Continuous search for entry points",
                        fontSize = 9.sp,
                        color = OdinSilverMuted
                    )
                }

                Button(
                    onClick = { isScanning = !isScanning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) OdinRed else OdinGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isScanning) "توقف" else "اسکن", fontSize = 11.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "آلارم‌ها" else "Alarms", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "$totalAlarms", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "سیگنال‌ها" else "Signals", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "${signals.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "ترید امروز" else "Trades Today", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "$tradesToday/$maxTradesPerDay", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                    }
                }
            }
        }

        item {
            // Alarm + Auto Trade Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "آلارم صوتی تک بوق" else "Single Beep Alarm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = {
                                alarmEnabled = it
                                scanner.setAlarmEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = OdinGold, checkedTrackColor = OdinGold.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) "هر موقع نقطه ورود مناسب پیدا کرد تک بوق می‌زند" else "Single beep whenever suitable entry found",
                        fontSize = 10.sp,
                        color = OdinSilverMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = OdinBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "ترید اتومات" else "Auto Trade",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = autoTradeEnabled,
                            onCheckedChange = {
                                autoTradeEnabled = it
                                scanner.setAutoTradeEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = OdinCyan, checkedTrackColor = OdinCyan.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (autoTradeEnabled) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = if (isPersian) "حداکثر ترید باز" else "Max Open Trades", fontSize = 9.sp, color = OdinSilverMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (maxTrades > 1) { maxTrades--; scanner.setMaxTrades(maxTrades) } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = OdinSilver, modifier = Modifier.size(14.dp))
                                    }
                                    Text(text = "$maxTrades", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OdinCyan, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { if (maxTrades < 20) { maxTrades++; scanner.setMaxTrades(maxTrades) } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = OdinSilver, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Column {
                                Text(text = if (isPersian) "حداکثر روزانه" else "Max Per Day", fontSize = 9.sp, color = OdinSilverMuted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (maxTradesPerDay > 1) { maxTradesPerDay--; scanner.setMaxTradesPerDay(maxTradesPerDay) } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = OdinSilver, modifier = Modifier.size(14.dp))
                                    }
                                    Text(text = "$maxTradesPerDay", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OdinGreen, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(onClick = { if (maxTradesPerDay < 50) { maxTradesPerDay++; scanner.setMaxTradesPerDay(maxTradesPerDay) } }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = OdinSilver, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Column {
                                Text(text = if (isPersian) "فعلی" else "Current", fontSize = 9.sp, color = OdinSilverMuted)
                                Text(text = "$currentTrades", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "$tradesToday today", fontSize = 8.sp, color = OdinSilverMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPersian)
                                "✅ فقط اگر اعتماد 80%+ و RR 1:2+ و Confluence 5+ باشد ترید می‌کند\n✅ بر اساس تعداد مجاز: $currentTrades/$maxTrades باز، $tradesToday/$maxTradesPerDay روزانه"
                            else
                                "✅ Only trades if Conf 80%+ and RR 1:2+ and Confluence 5+\n✅ Based on allowed: $currentTrades/$maxTrades open, $tradesToday/$maxTradesPerDay daily",
                            fontSize = 9.sp,
                            color = OdinSilver,
                            lineHeight = 11.sp
                        )
                    } else {
                        Text(
                            text = if (isPersian) "ترید اتومات خاموش - فقط آلارم" else "Auto trade OFF - Alarm only",
                            fontSize = 10.sp,
                            color = OdinSilverMuted
                        )
                    }
                }
            }
        }

        item {
            // Banned strategies info
            if (bannedStrategies.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = OdinRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "استراتژی‌های ممنوع (10$→زیر 15$): ${bannedStrategies.size}" else "Banned Strategies ($10→<15$): ${bannedStrategies.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OdinRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bannedStrategies.joinToString(", ") { it.name },
                            fontSize = 9.sp,
                            color = OdinSilverMuted
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isPersian) "سیگنال‌های زنده - آلارم صوتی" else "Live Signals - Audio Alarm",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
        }

        if (signals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPersian) "در حال جستجوی نقطه ورود..." else "Searching for entry points...",
                                fontSize = 11.sp,
                                color = OdinSilverMuted
                            )
                            if (isScanning) {
                                Spacer(modifier = Modifier.height(4.dp))
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OdinCyan, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        } else {
            items(signals.takeLast(10).reversed()) { signal ->
                SignalCardWithAlarm(signal = signal, isPersian = isPersian)
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun SignalCardWithAlarm(signal: EntrySignal, isPersian: Boolean) {
    val isBuy = signal.side == SignalSide.BUY
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBuy) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, if (isBuy) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBuy) OdinGreen else OdinRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = if (isBuy) "BUY" else "SELL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = signal.symbol, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                }

                Text(
                    text = "${signal.confidence.toInt()}% Conf",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OdinGold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OdinGold.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Price ${String.format("%.2f", signal.price)}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "RR 1:${String.format("%.1f", signal.rr)}", fontSize = 10.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                Text(text = "Confl ${signal.confluence}", fontSize = 10.sp, color = OdinCyan)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = signal.reason,
                fontSize = 9.sp,
                color = OdinSilver,
                lineHeight = 11.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(System.currentTimeMillis() - signal.timestamp) / 1000}s ago • ${signal.strategy.name}",
                    fontSize = 8.sp,
                    color = OdinSilverMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = OdinGold, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = if (isPersian) "بوق!" else "Beep!", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
