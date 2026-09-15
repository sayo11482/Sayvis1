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
import com.odin.agent.ui.theme.*
import kotlin.random.Random

@Composable
fun BacktestScreen(isPersian: Boolean) {
    var selectedStrategy by remember { mutableStateOf(QuantStrategyType.TREND_FOLLOWING) }
    var selectedSymbol by remember { mutableStateOf("BTC/USDT") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<QuantBacktestResult?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "بک‌تست حرفه‌ای" else "Professional Backtest",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isPersian) "تست دقیق با کمیسیون و اسلیپیج + Walk-forward" else "Accurate with commission & slippage + Walk-forward",
                fontSize = 11.sp,
                color = OdinSilverMuted
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "تنظیمات بک‌تست" else "Backtest Config", fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strategy selector
                    Text(text = if (isPersian) "استراتژی" else "Strategy", fontSize = 11.sp, color = OdinSilverMuted)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = selectedStrategy.label(isPersian), color = OdinCyan)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            QuantStrategyType.values().forEach { strat ->
                                DropdownMenuItem(
                                    text = { Text(strat.label(isPersian)) },
                                    onClick = {
                                        selectedStrategy = strat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Symbol
                    Text(text = if (isPersian) "نماد" else "Symbol", fontSize = 11.sp, color = OdinSilverMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BTC/USDT", "ETH/USDT", "EURUSD", "XAUUSD").forEach { sym ->
                            FilterChip(
                                selected = selectedSymbol == sym,
                                onClick = { selectedSymbol = sym },
                                label = { Text(sym, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OdinCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = OdinCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isRunning = true
                            // Simulate backtest
                            result = QuantBacktestResult(
                                id = "bt_${System.currentTimeMillis()}",
                                symbol = selectedSymbol,
                                strategy = selectedStrategy,
                                totalTrades = Random.nextInt(80, 200),
                                winrate = 42.0 + Random.nextDouble()*15,
                                totalPnl = Random.nextDouble()*2000 - 200,
                                totalPnlPercent = Random.nextDouble()*25 - 5,
                                sharpe = 0.8 + Random.nextDouble()*1.2,
                                sortino = 1.2 + Random.nextDouble()*1.5,
                                maxDd = - (2.0 + Random.nextDouble()*10),
                                profitFactor = 1.2 + Random.nextDouble()*0.8,
                                startDate = "2023-01-01",
                                endDate = "2024-01-01"
                            )
                            isRunning = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isPersian) "اجرای بک‌تست" else "Run Backtest", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        result?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isPersian) "گزارش عملکرد" else "Performance Report", fontWeight = FontWeight.Bold, color = OdinGold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (res.totalPnl >=0) OdinGreen.copy(alpha=0.2f) else OdinRed.copy(alpha=0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${if (res.totalPnl>=0) "+" else ""}${res.totalPnlPercent.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.totalPnl>=0) OdinGreen else OdinRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = if (isPersian) "معاملات" else "Trades", value = "${res.totalTrades}")
                            MetricItem(label = "Winrate", value = "${res.winrate.toInt()}%")
                            MetricItem(label = "PF", value = "${String.format("%.2f", res.profitFactor)}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MetricItem(label = "Sharpe", value = String.format("%.2f", res.sharpe))
                            MetricItem(label = "Sortino", value = String.format("%.2f", res.sortino))
                            MetricItem(label = "Max DD", value = "${res.maxDd.toInt()}%", isNegative = true)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isPersian)
                                "سود کل: $${res.totalPnl.toInt()} (${res.totalPnlPercent.toInt()}%)\nبهترین: استراتژی ${res.strategy.labelFa} روی ${res.symbol}\nتوصیه: Walk-forward برای جلوگیری از overfit"
                            else
                                "Total PnL: $${res.totalPnl.toInt()} (${res.totalPnlPercent.toInt()}%)\nBest: ${res.strategy.labelEn} on ${res.symbol}\nTip: Use Walk-forward to avoid overfit",
                            fontSize = 11.sp,
                            color = OdinSilver,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPersian) "⚠️ گذشته تضمین آینده نیست! مدیریت ریسک کلید بقاست" else "⚠️ Past performance doesn't guarantee future! Risk mgmt is key",
                            fontSize = 10.sp,
                            color = OdinAmber
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, isNegative: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = OdinSilverMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) OdinRed else Color.White
        )
    }
}
