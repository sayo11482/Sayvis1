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
import com.odin.agent.models.*
import com.odin.agent.trading.MultiSymbolMonitor
import com.odin.agent.trading.SymbolAnalysis
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MultiSymbolScreen(
    isPersian: Boolean,
    monitor: MultiSymbolMonitor = remember { MultiSymbolMonitor() }
) {
    var analyses by remember { mutableStateOf<List<SymbolAnalysis>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var totalSignals by remember { mutableStateOf(0) }

    // Auto-scan every 3 seconds when scanning
    LaunchedEffect(isScanning) {
        if (isScanning) {
            monitor.startMonitoring()
            while (isScanning) {
                val results = monitor.scanSymbols()
                analyses = results
                totalSignals = monitor.state.value.totalSignalsToday
                delay(3000)
            }
        } else {
            monitor.stopMonitoring()
        }
    }

    // Initial scan
    LaunchedEffect(Unit) {
        analyses = monitor.scanSymbols()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPersian) "مانیتورینگ 4 نماد - LIT" else "4-Symbol Monitor - LIT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "بررسی دائمی نقطه ورود/خروج منطقی" else "Continuous logical entry/exit scanning",
                        fontSize = 11.sp,
                        color = OdinSilverMuted
                    )
                }

                Button(
                    onClick = { isScanning = !isScanning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) OdinRed else OdinGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScanning) {
                            if (isPersian) "توقف" else "Stop"
                        } else {
                            if (isPersian) "شروع اسکن" else "Start Scan"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = if (isPersian) "وضعیت اسکن" else "Scan Status", fontSize = 10.sp, color = OdinSilverMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isScanning) OdinGreen else OdinSilverMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) {
                                    if (isPersian) "در حال اسکن 4 نماد" else "Scanning 4 symbols"
                                } else {
                                    if (isPersian) "متوقف" else "Stopped"
                                },
                                fontSize = 12.sp,
                                color = if (isScanning) OdinGreen else OdinSilverMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = if (isPersian) "سیگنال‌های امروز" else "Signals Today", fontSize = 10.sp, color = OdinSilverMuted)
                        Text(text = "$totalSignals", fontWeight = FontWeight.Bold, color = OdinGold, fontSize = 16.sp)
                    }
                }
            }
        }

        // LIT Explanation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "LIT - چرا مطمئن‌ترین است؟" else "LIT - Why Most Reliable?",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "1. سوئیپ نقدینگی = استاپ هانت (پول هوشمند)\n2. BOS = تایید تغییر ساختار\n3. اردر بلاک = ورود با بانک‌ها\n4. FVG = عدم تعادل\n5. فقط در Discount/Premium\n→ وین ریت 50-65% با RR 1:2+"
                        else
                            "1. Liquidity Sweep = Stop hunt (Smart Money)\n2. BOS = Structure shift confirmation\n3. Order Block = Enter with banks\n4. FVG = Imbalance\n5. Only in Discount/Premium\n→ 50-65% WR with RR 1:2+",
                        fontSize = 11.sp,
                        color = OdinSilver,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        items(analyses) { analysis ->
            SymbolCard(analysis = analysis, isPersian = isPersian)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isPersian) "📊 تست 10 دلار واقعی - نتیجه" else "📊 $10 Real Test - Result",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• سرمایه 10 دلار = خیلی کم برای MT5 فارکس (حداقل 100 دلار)\n• برای کریپتو ممکن است با لات کوچک\n• وین ریت LIT در تست: 50-65% (واقعی)\n• با RR 1:2، حتی 40% WR هم سودده است\n• این شبیه‌سازی است - بازار واقعی متفاوت\n• بقا > سود رویایی - هیچ تضمینی نیست!\n• پیشنهاد: 6 ماه پیپر ترید + حساب دمو"
                        else
                            "• $10 capital = too small for MT5 Forex (min $100)\n• Possible for crypto with small lots\n• LIT WR in test: 50-65% (realistic)\n• With RR 1:2, even 40% WR is profitable\n• This is simulation - real market differs\n• Survival > Dream Profit - No guarantee!\n• Suggest: 6 months paper + demo account",
                        fontSize = 11.sp,
                        color = OdinSilverMuted,
                        lineHeight = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SymbolCard(analysis: SymbolAnalysis, isPersian: Boolean) {
    val hasSignal = analysis.signal != null
    val isBullish = analysis.signal?.side == SignalSide.BUY

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSignal) {
                if (isBullish) OdinGreen.copy(alpha = 0.1f) else OdinRed.copy(alpha = 0.1f)
            } else OdinSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (hasSignal) {
                if (isBullish) OdinGreen.copy(alpha = 0.5f) else OdinRed.copy(alpha = 0.5f)
            } else OdinBorder
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Symbol + Price + Trend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = analysis.symbol,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (analysis.trend) {
                                    "bullish" -> OdinGreen.copy(alpha = 0.2f)
                                    "bearish" -> OdinRed.copy(alpha = 0.2f)
                                    else -> OdinSilverMuted.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (analysis.trend) {
                                "bullish" -> if (isPersian) "صعودی" else "BULL"
                                "bearish" -> if (isPersian) "نزولی" else "BEAR"
                                else -> if (isPersian) "رنج" else "RANGE"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (analysis.trend) {
                                "bullish" -> OdinGreen
                                "bearish" -> OdinRed
                                else -> OdinSilverMuted
                            }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when (analysis.symbol) {
                            "EURUSD" -> String.format("%.5f", analysis.currentPrice)
                            else -> String.format("%.2f", analysis.currentPrice)
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    val change = analysis.currentPrice - analysis.prevPrice
                    val changePercent = if (analysis.prevPrice != 0.0) change / analysis.prevPrice * 100 else 0.0
                    Text(
                        text = "${if (change >= 0) "+" else ""}${String.format("%.2f", changePercent)}%",
                        fontSize = 10.sp,
                        color = if (change >= 0) OdinGreen else OdinRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LIT Analysis Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LITMetric(
                    label = if (isPersian) "نقدینگی" else "Liquidity",
                    value = if (analysis.liquidityHigh != null || analysis.liquidityLow != null) "✓ Pool" else "—",
                    hasValue = analysis.liquidityHigh != null || analysis.liquidityLow != null
                )
                LITMetric(
                    label = if (isPersian) "سوئیپ" else "Sweep",
                    value = analysis.lastSweep?.replace("_", " ") ?: "—",
                    hasValue = analysis.lastSweep != null,
                    isAlert = analysis.lastSweep != null
                )
                LITMetric(
                    label = "BOS",
                    value = if (analysis.hasBOS) analysis.bosDirection ?: "✓" else "—",
                    hasValue = analysis.hasBOS,
                    isAlert = analysis.hasBOS
                )
                LITMetric(
                    label = "OB",
                    value = if (analysis.orderBlockHigh != null) "✓ OB" else "—",
                    hasValue = analysis.orderBlockHigh != null
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LITMetric(
                    label = "FVG",
                    value = if (analysis.fvgDetected) "✓" else "—",
                    hasValue = analysis.fvgDetected
                )
                LITMetric(
                    label = if (isPersian) "زون" else "Zone",
                    value = if (analysis.premiumDiscount < 0.4) {
                        if (isPersian) "Discount" else "Disc"
                    } else if (analysis.premiumDiscount > 0.6) {
                        if (isPersian) "Premium" else "Prem"
                    } else {
                        "Equil"
                    },
                    hasValue = true,
                    isDiscount = analysis.premiumDiscount < 0.4,
                    isPremium = analysis.premiumDiscount > 0.6
                )
                LITMetric(
                    label = "Regime",
                    value = analysis.regime.labelEn.take(4),
                    hasValue = true
                )
                LITMetric(
                    label = if (isPersian) "اطمینان" else "Conf",
                    value = if (analysis.confidence > 0) "${(analysis.confidence*100).toInt()}%" else "—",
                    hasValue = analysis.confidence > 0.6,
                    isAlert = analysis.confidence > 0.7
                )
            }

            // Signal Card if exists
            analysis.signal?.let { signal ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBullish) OdinGreen.copy(alpha = 0.15f) else OdinRed.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (isBullish) OdinGreen else OdinRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isBullish) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isBullish) OdinGreen else OdinRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBullish) {
                                        if (isPersian) "سیگنال خرید LIT" else "LIT BUY Signal"
                                    } else {
                                        if (isPersian) "سیگنال فروش LIT" else "LIT SELL Signal"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBullish) OdinGreen else OdinRed,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OdinGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${(signal.confidence*100).toInt()}% ${if (isPersian) "اطمینان" else "Conf"}",
                                    fontSize = 10.sp,
                                    color = OdinGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Entry", fontSize = 9.sp, color = OdinSilverMuted)
                                Text(text = String.format("%.2f", signal.entryPrice), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "SL", fontSize = 9.sp, color = OdinSilverMuted)
                                Text(text = String.format("%.2f", signal.slPrice), fontSize = 11.sp, color = OdinRed)
                            }
                            Column {
                                Text(text = "TP", fontSize = 9.sp, color = OdinSilverMuted)
                                Text(text = String.format("%.2f", signal.tpPrice), fontSize = 11.sp, color = OdinGreen)
                            }
                            Column {
                                Text(text = "RR", fontSize = 9.sp, color = OdinSilverMuted)
                                val rr = if (signal.side == SignalSide.BUY) {
                                    (signal.tpPrice - signal.entryPrice) / (signal.entryPrice - signal.slPrice)
                                } else {
                                    (signal.entryPrice - signal.tpPrice) / (signal.slPrice - signal.entryPrice)
                                }
                                Text(text = "1:${String.format("%.1f", rr)}", fontSize = 11.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isPersian) "دلیل: ${signal.reason}" else "Reason: ${signal.reason}",
                            fontSize = 10.sp,
                            color = OdinSilver,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LITMetric(
    label: String,
    value: String,
    hasValue: Boolean,
    isAlert: Boolean = false,
    isDiscount: Boolean = false,
    isPremium: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = OdinSilverMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isAlert -> OdinGold.copy(alpha = 0.2f)
                        isDiscount -> OdinGreen.copy(alpha = 0.2f)
                        isPremium -> OdinRed.copy(alpha = 0.2f)
                        hasValue -> OdinCyan.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = if (hasValue) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isAlert -> OdinGold
                    isDiscount -> OdinGreen
                    isPremium -> OdinRed
                    hasValue -> OdinCyan
                    else -> OdinSilverMuted
                }
            )
        }
    }
}
