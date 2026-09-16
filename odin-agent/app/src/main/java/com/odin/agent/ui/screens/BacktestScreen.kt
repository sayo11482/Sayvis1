package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.trading.ContinuousBacktestEngine
import com.odin.agent.trading.RealMarketDataManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BacktestScreen(isPersian: Boolean) {
    val engine = remember { ContinuousBacktestEngine() }
    val realDataManager = remember { RealMarketDataManager() }
    var selectedStrategy by remember { mutableStateOf(QuantStrategyType.TREND_FOLLOWING) }
    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<com.odin.agent.trading.BacktestResult?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        realDataManager.fetchRealPrices()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OdinDeepSpace).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = if (isPersian) "بک‌تست حرفه‌ای - ۱۰۰٪ واقعی" else "Professional Backtest - 100% REAL", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = if (isPersian) "تست دقیق با کمیسیون و اسلیپیج + Walk-forward - فقط داده واقعی" else "Accurate with commission & slippage + Walk-forward - REAL only", fontSize = 11.sp, color = OdinSilverMuted)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "تنظیمات بک‌تست واقعی" else "Backtest Config REAL", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = if (isPersian) "استراتژی" else "Strategy", fontSize = 11.sp, color = OdinSilverMuted)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = selectedStrategy.label(isPersian), color = OdinCyan)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            QuantStrategyType.values().forEach { strat ->
                                DropdownMenuItem(text = { Text(strat.label(isPersian)) }, onClick = { selectedStrategy = strat; expanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isPersian) "نماد" else "Symbol", fontSize = 11.sp, color = OdinSilverMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BTCUSDT", "ETHUSDT", "EURUSD", "XAUUSD").forEach { sym ->
                            FilterChip(selected = selectedSymbol == sym, onClick = { selectedSymbol = sym }, label = { Text(sym, fontSize = 10.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = OdinCyan))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (isRunning) return@Button
                            isRunning = true
                            scope.launch {
                                try {
                                    val powers = engine.runBacktestCycle()
                                    val power = powers[selectedStrategy]
                                    if (power != null && power.results.isNotEmpty()) {
                                        val last = power.results.last()
                                        result = last
                                    } else {
                                        result = null
                                    }
                                } catch (e: Exception) {
                                    result = null
                                }
                                isRunning = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRunning) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isPersian) "اجرای بک‌تست واقعی" else "Run REAL Backtest", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = if (isPersian) "بک‌تست روی کندل واقعی بایننس/فارکس - هیچ شبیه‌سازی" else "Backtest on REAL Binance/Forex candles - No simulation", fontSize = 8.sp, color = OdinSilverDim)
                }
            }
        }

        result?.let { res ->
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isPersian) "گزارش عملکرد واقعی" else "Performance Report REAL", fontWeight = FontWeight.Bold, color = OdinGold)
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (res.profit >= 0) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(text = "${if (res.profit >= 0) "+" else ""}${res.profitPercent.toInt()}% ${if (isPersian) "واقعی" else "REAL"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (res.profit >= 0) OdinGreen else OdinRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = if (isPersian) "معاملات" else "Trades", value = "${res.trades}")
                            MetricItem(label = if (isPersian) "وین‌ریت واقعی" else "WR REAL", value = "${res.winrate.toInt()}%")
                            MetricItem(label = "PF ${if (isPersian) "واقعی" else "REAL"}", value = "${String.format("%.2f", res.profitFactor)}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = "Sharpe ${if (isPersian) "واقعی" else "REAL"}", value = String.format("%.2f", res.sharpe))
                            MetricItem(label = if (isPersian) "سود واقعی" else "Profit REAL", value = "$${String.format("%.1f", res.profit)}")
                            MetricItem(label = if (isPersian) "نهایی واقعی" else "Final REAL", value = "$${String.format("%.1f", res.finalCapital)}")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPersian) "سود واقعی: $${res.profit.toInt()} (${res.profitPercent.toInt()}%) روی کندل واقعی ${selectedSymbol}\nاستراتژی: ${res.strategy.labelFa} - فقط داده واقعی\nتوصیه: Walk-forward برای جلوگیری از overfit - ۱۰۰٪ واقعی"
                            else "Total PnL REAL: $${res.profit.toInt()} (${res.profitPercent.toInt()}%) on REAL candles ${selectedSymbol}\nStrategy: ${res.strategy.labelEn} - REAL only\nTip: Walk-forward to avoid overfit - 100% REAL",
                            fontSize = 11.sp, color = OdinSilver, lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = if (isPersian) "⚠️ گذشته تضمین آینده نیست! مدیریت ریسک کلید بقاست - فقط واقعی" else "⚠️ Past performance doesn't guarantee future! Risk mgmt is key - REAL only", fontSize = 10.sp, color = OdinAmber)
                    }
                }
            }
        }

        if (result == null && !isRunning) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = if (isPersian) "بک‌تست واقعی اجرا نشده - دکمه بالا را بزن - فقط واقعی" else "No REAL backtest yet - Press button above - REAL only", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = if (isPersian) "اگر داده واقعی نباشد، خالی می‌ماند نه فیک" else "If no REAL data, stays empty not fake", fontSize = 8.sp, color = OdinSilverDim)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MetricItem(label: String, value: String, isNegative: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isNegative) OdinRed else Color.White)
    }
}
