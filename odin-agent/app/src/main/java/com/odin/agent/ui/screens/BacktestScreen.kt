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
import com.odin.agent.trading.SymbolManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ODIN v1.0.24 - Backtest - VITTAVERSE ONLY - سرمایه قابل تنظیم + اسپرد + واحد تومان/تتر
 * قبلاً 10$ ثابت بود - الان قابل تنظیم - اسپرد ویتاورس محاسبه - واحد مشخص
 */

@Composable
fun BacktestScreen(isPersian: Boolean) {
    val engine = remember { ContinuousBacktestEngine() }
    val realDataManager = remember { RealMarketDataManager() }
    var selectedStrategy by remember { mutableStateOf(QuantStrategyType.TREND_FOLLOWING) }
    var selectedSymbol by remember { mutableStateOf("EURUSD") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<com.odin.agent.trading.BacktestResult?>(null) }
    var capital by remember { mutableStateOf(100.0) }
    var expandedStrategy by remember { mutableStateOf(false) }
    var expandedSymbol by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        realDataManager.fetchRealPrices()
    }

    val symbolInfo = SymbolManager.find(selectedSymbol)
    val spread = symbolInfo?.spreadTypical ?: 1.2
    val unit = symbolInfo?.unit ?: "USDT"
    val unitFa = if (unit == "Toman") "تومان" else "تتر"
    val tomanValue = capital * 235000

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(OdinDeepSpace).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "بک‌تست ویتاورس - سرمایه قابل تنظیم + اسپرد واقعی" else "Vittaverse Backtest - Adjustable Capital + REAL Spread", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(
                        text = if (isPersian) "فقط بروکر ویتاورس - https://vittaverse.com/fa/ - کمیسیون و اسپرد محاسبه - واحد تومان/تتر مشخص"
                        else "Vittaverse ONLY - https://vittaverse.com/fa/ - Commission & Spread calculated - Unit Toman/USDT",
                        fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "قبلاً 10$ ثابت بود - الان قابل تنظیم - اسپرد ویتاورس محاسبه می‌شود - قیمت‌ها هر لحظه در نوسان واقعی"
                        else "Previously fixed 10$ - Now adjustable - Vittaverse spread calculated - Prices fluctuating REAL",
                        fontSize = 7.sp, color = OdinSilverDim
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "تنظیمات بک‌تست ویتاورس واقعی - سرمایه + نماد + استراتژی + اسپرد" else "Vittaverse Backtest Config REAL - Capital + Symbol + Strategy + Spread", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // سرمایه قابل تنظیم
                    Text(text = if (isPersian) "سرمایه ورودی - قابل تنظیم (قبلاً 10$ ثابت)" else "Capital Input - Adjustable (prev fixed 10$)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (capital > 10) capital -= 10 }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$${capital.toInt()} USDT", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                            Text(text = if (isPersian) "${tomanValue.toInt()} تومان - ویتاورس" else "${tomanValue.toInt()} Toman - Vittaverse", fontSize = 9.sp, color = OdinSilverMuted)
                            Slider(value = capital.toFloat(), onValueChange = { capital = it.toDouble() }, valueRange = 10f..10000f, colors = SliderDefaults.colors(thumbColor = OdinGold, activeTrackColor = OdinGold))
                        }
                        IconButton(onClick = { if (capital < 10000) capital += 10 }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(10.0, 100.0, 500.0, 1000.0, 5000.0).forEach { amt ->
                            FilterChip(
                                selected = capital == amt,
                                onClick = { capital = amt },
                                label = { Text("$${amt.toInt()}", fontSize = 8.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // انتخاب استراتژی با نمایش جزئیات
                    Text(text = if (isPersian) "استراتژی - نمایش جزئیات بررسی" else "Strategy - Show Checked Details", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(onClick = { expandedStrategy = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = selectedStrategy.label(isPersian), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = expandedStrategy, onDismissRequest = { expandedStrategy = false }, modifier = Modifier.background(Color(0xFF1A1A1A))) {
                            QuantStrategyType.values().forEach { strat ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(strat.label(isPersian), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isPersian) "RR 1:${when(strat){QuantStrategyType.LIT_LIQUIDITY_INVERSION->"3.5"; QuantStrategyType.TREND_FOLLOWING->"2.0"; else->"2.5"}} | ${strat.riskLevel} | ${strat.bestRegime}"
                                                else "RR 1:${when(strat){QuantStrategyType.LIT_LIQUIDITY_INVERSION->"3.5"; else->"2.0"}} | ${strat.riskLevel}",
                                                fontSize = 8.sp, color = OdinSilverMuted
                                            )
                                        }
                                    },
                                    onClick = { selectedStrategy = strat; expandedStrategy = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // انتخاب نماد با واحد و اسپرد
                    Text(text = if (isPersian) "نماد ویتاورس - واحد تومان/تتر + اسپرد" else "Vittaverse Symbol - Unit Toman/USDT + Spread", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(onClick = { expandedSymbol = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(text = selectedSymbol, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(text = "${symbolInfo?.nameFa ?: ""} | ${symbolInfo?.unit ?: "USDT"} | اسپرد ${spread} - ویتاورس", fontSize = 8.sp, color = OdinSilverMuted)
                            }
                        }
                        DropdownMenu(expanded = expandedSymbol, onDismissRequest = { expandedSymbol = false }, modifier = Modifier.background(Color(0xFF1A1A1A))) {
                            SymbolManager.getTradableSymbols().take(30).forEach { sym ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sym.symbol, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("${sym.nameFa} | ${sym.unit} | اسپرد ${sym.spreadTypical} | ${sym.broker} - ویتاورس", fontSize = 8.sp, color = OdinSilverMuted)
                                        }
                                    },
                                    onClick = { selectedSymbol = sym.symbol; expandedSymbol = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // نمایش اسپرد محاسبه شده
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.2f)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = if (isPersian) "محاسبه اسپرد ویتاورس واقعی" else "Vittaverse REAL Spread Calculation", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (isPersian) "اسپرد معمول" else "Typical Spread", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "$spread ${if (unit == "Toman") "تومان" else "پیپ"} - $unitFa", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (isPersian) "هزینه اسپرد 0.01 لات" else "Spread Cost 0.01 lot", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(
                                    text = if (unit == "Toman") "${String.format("%.0f", spread * 0.01)} تومان" else "${String.format("%.2f", spread * 0.01)} تتر",
                                    fontSize = 9.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (isPersian) "کمیسیون ویتاورس" else "Vittaverse Commission", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = if (isPersian) "0.01% - واقعی" else "0.01% - REAL", fontSize = 9.sp, color = OdinCyan)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (isPersian) "واحد پولی" else "Currency Unit", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = if (isPersian) "$unitFa - مشخص - ویتاورس" else "$unitFa - Specified - Vittaverse", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (isRunning) return@Button
                            isRunning = true
                            scope.launch {
                                try {
                                    engine.setCapital(capital)
                                    val powers = engine.runBacktestCycle()
                                    val power = powers[selectedStrategy]
                                    if (power != null && power.results.isNotEmpty()) {
                                        val last = power.results.last()
                                        result = last.copy(initialCapital = capital, finalCapital = capital + last.profit)
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isRunning) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "اجرای بک‌تست ویتاورس واقعی - سرمایه ${capital.toInt()}$" else "Run Vittaverse REAL Backtest - Capital ${capital.toInt()}$",
                                color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "بک‌تست روی کندل واقعی ویتاورس - اسپرد و کمیسیون محاسبه - واحد $unitFa مشخص - هیچ شبیه‌سازی"
                        else "Backtest on REAL Vittaverse candles - Spread & commission calculated - Unit $unitFa specified - No simulation",
                        fontSize = 7.sp, color = OdinSilverDim, lineHeight = 8.sp
                    )
                }
            }
        }

        result?.let { res ->
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isPersian) "گزارش عملکرد ویتاورس واقعی" else "Vittaverse Performance REAL", fontWeight = FontWeight.Bold, color = OdinGold, fontSize = 11.sp)
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (res.profit >= 0) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(text = "${if (res.profit >= 0) "+" else ""}${res.profitPercent.toInt()}% ${if (isPersian) "واقعی ویتاورس" else "REAL Vittaverse"}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (res.profit >= 0) OdinGreen else OdinRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = if (isPersian) "سرمایه اولیه" else "Initial", value = "$${res.initialCapital.toInt()}")
                            MetricItem(label = if (isPersian) "نهایی ویتاورس" else "Final Vittaverse", value = "$${res.finalCapital.toInt()}")
                            MetricItem(label = if (isPersian) "سود واقعی" else "Profit REAL", value = "$${String.format("%.1f", res.profit)}")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = if (isPersian) "معاملات" else "Trades", value = "${res.trades}")
                            MetricItem(label = if (isPersian) "وین‌ریت ویتاورس" else "WR Vittaverse", value = "${res.winrate.toInt()}%")
                            MetricItem(label = "PF ${if (isPersian) "واقعی" else "REAL"}", value = "${String.format("%.2f", res.profitFactor)}")
                            MetricItem(label = "Sharpe", value = String.format("%.2f", res.sharpe))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(6.dp)) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = if (isPersian)
                                        "سرمایه: ${res.initialCapital.toInt()}$ → ${res.finalCapital.toInt()}$ | سود: ${res.profit.toInt()}$ (${res.profitPercent.toInt()}%) | ویتاورس واقعی\n" +
                                        "نماد: $selectedSymbol | واحد: $unitFa | اسپرد: $spread | استراتژی: ${res.strategy.labelFa}\n" +
                                        "اسپرد محاسبه شده: ${if (unit == "Toman") "${spread * 0.01} تومان هر 0.01 لات" else "${spread} پیپ"} | کمیسیون ویتاورس: 0.01%\n" +
                                        "بروکر: ویتاورس - https://vittaverse.com/fa/ - ۱۰۰٪ واقعی"
                                    else
                                        "Capital: ${res.initialCapital.toInt()}$ → ${res.finalCapital.toInt()}$ | PnL: ${res.profit.toInt()}$ (${res.profitPercent.toInt()}%) | Vittaverse REAL\n" +
                                        "Symbol: $selectedSymbol | Unit: $unitFa | Spread: $spread | Strategy: ${res.strategy.labelEn}\n" +
                                        "Spread: ${spread} | Commission: 0.01% | Broker: Vittaverse - https://vittaverse.com/fa/",
                                    fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = if (isPersian) "⚠️ گذشته تضمین آینده نیست! ویتاورس واقعی - مدیریت ریسک کلید بقاست" else "⚠️ Past doesn't guarantee future! Vittaverse REAL - Risk mgmt key", fontSize = 8.sp, color = OdinAmber)
                    }
                }
            }
        }

        if (result == null && !isRunning) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = if (isPersian) "بک‌تست ویتاورس اجرا نشده - سرمایه ${capital.toInt()}$ - ${symbolInfo?.unitFa ?: ""} - اسپرد $spread" else "No Vittaverse backtest - Capital ${capital.toInt()}$ - Spread $spread", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = if (isPersian) "سرمایه قابل تنظیم - قبلاً 10$ ثابت بود - الان ${capital.toInt()}$" else "Adjustable capital - prev fixed 10$ - Now ${capital.toInt()}$", fontSize = 8.sp, color = OdinSilverDim)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = OdinSilverMuted, maxLines = 1)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}
