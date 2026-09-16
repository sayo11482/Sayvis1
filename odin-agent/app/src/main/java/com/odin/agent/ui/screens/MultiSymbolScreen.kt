package com.odin.agent.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MultiSymbolScreen(
    isPersian: Boolean,
    monitor: MultiSymbolMonitor = remember { MultiSymbolMonitor() },
    realDataManager: RealMarketDataManager = remember { RealMarketDataManager() }
) {
    var analyses by remember { mutableStateOf<List<SymbolAnalysis>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var realPrices by remember { mutableStateOf<Map<String, RealPrice>>(emptyMap()) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            monitor.startMonitoring()
            while (isScanning) {
                try {
                    realPrices = realDataManager.fetchRealPrices()
                    monitor.updateWithRealPrices(realPrices)
                    analyses = monitor.state.value.symbols
                } catch (e: Exception) {
                    // REAL ONLY - no fake updatePrices - keep existing real data
                    analyses = monitor.state.value.symbols
                }
                delay(2000)
            }
        } else {
            monitor.stopMonitoring()
        }
    }

    LaunchedEffect(Unit) {
        try {
            realPrices = realDataManager.fetchRealPrices()
            monitor.updateWithRealPrices(realPrices)
        } catch (e: Exception) {
            // REAL ONLY - no fake - empty if no real
        }
        analyses = monitor.state.value.symbols
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
                        text = if (isPersian) "اودین - مانیتور واقعی ${analyses.size} نماد" else "ODIN - Real Monitor ${analyses.size} symbols",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "فارکس + طلا + کریپتو + ریال ایران - ویتاورس واقعی" else "Forex + Gold + Crypto + IRR - Vittaverse REAL",
                        fontSize = 10.sp,
                        color = OdinGreen
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
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isScanning) "توقف" else "شروع", fontSize = 11.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(label = if (isPersian) "کل نمادها" else "Total Symbols", value = "${SymbolManager.allSymbols.size}", color = OdinCyan, modifier = Modifier.weight(1f))
                StatCard(label = if (isPersian) "فارکس" else "Forex", value = "${SymbolManager.getForexAll().size}", color = OdinGold, modifier = Modifier.weight(1f))
                StatCard(label = if (isPersian) "ریال ایران" else "IRR Pairs", value = "${SymbolManager.getIRRPairs().size}", color = OdinRed, modifier = Modifier.weight(1f))
                StatCard(label = if (isPersian) "فعال" else "Active", value = "${analyses.size}", color = OdinGreen, modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "v1.0.14 - بدون ممنوعیت - چارت واقعی - ریال ایران" else "v1.0.14 - No Ban - Real Chart - IRR",
                            fontWeight = FontWeight.Bold,
                            color = OdinGreen,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian)
                            "• تمام ${SymbolManager.allSymbols.size} نماد مجاز - بدون قانون ۱۰→۱۵ دلار\n• چارت واقعی از Binance + Forex API + بازار آزاد ایران\n• USD/IRR ~590,000 واقعی + EUR/IRR GBP/IRR AED/IRR\n• اتصال واقعی MT5 ویتاورس - معامله واقعی"
                        else
                            "• All ${SymbolManager.allSymbols.size} symbols allowed - No $10->$15 ban\n• Real chart from Binance + Forex API + Iran Free Market\n• USD/IRR ~590K real + EUR/IRR GBP/IRR AED/IRR\n• Real MT5 Vittaverse connection - REAL trading",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        items(analyses.sortedBy { it.category.name }) { analysis ->
            SymbolRealCard(analysis = analysis, isPersian = isPersian)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 8.sp, color = OdinSilverMuted, maxLines = 1)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun SymbolRealCard(analysis: SymbolAnalysis, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (analysis.category) {
                SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.08f)
                SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.08f)
                SymbolCategory.METALS -> OdinGold.copy(alpha = 0.08f)
                else -> Color(0xFF0A0A0A)
            }
        ),
        border = BorderStroke(
            1.dp,
            when (analysis.category) {
                SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.3f)
                SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.3f)
                SymbolCategory.METALS -> OdinGold.copy(alpha = 0.3f)
                else -> OdinBorder
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = analysis.symbol, fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(
                            when (analysis.category) {
                                SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.2f)
                                SymbolCategory.FOREX_MAJOR -> OdinGold.copy(alpha = 0.2f)
                                SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.2f)
                                SymbolCategory.METALS -> OdinGold.copy(alpha = 0.2f)
                                else -> OdinSilverMuted.copy(alpha = 0.2f)
                            }
                        ).padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = analysis.category.labelEn,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (analysis.category) {
                                SymbolCategory.FOREX_IRR -> OdinRed
                                SymbolCategory.CRYPTO -> OdinCyan
                                else -> OdinGold
                            }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (analysis.symbol.contains("IRR")) String.format("%,.0f", analysis.currentPrice) else String.format("%.2f", analysis.currentPrice),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${if (analysis.changePercent >= 0) "+" else ""}${String.format("%.2f", analysis.changePercent)}%",
                        fontSize = 9.sp,
                        color = if (analysis.changePercent >= 0) OdinGreen else OdinRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Bid/Ask", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(
                        text = if (analysis.symbol.contains("IRR")) "${String.format("%,.0f", analysis.bid)}/${String.format("%,.0f", analysis.ask)}" else "${String.format("%.2f", analysis.bid)}/${String.format("%.2f", analysis.ask)}",
                        fontSize = 8.sp,
                        color = OdinSilver,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Column {
                    Text(text = "Best Strat", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = analysis.bestStrategy.name.take(10), fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "WR", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = "${analysis.bestWinrate.toInt()}%", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Source", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = analysis.source.take(12), fontSize = 7.sp, color = OdinCyan)
                }
            }
        }
    }
}
