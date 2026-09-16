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
    var blockedByWR by remember { mutableStateOf(0) }
    var avgConfluence by remember { mutableStateOf(0.0) }
    var showTVDetails by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            monitor.startMonitoring()
            while (isScanning) {
                val results = monitor.scanSymbols()
                analyses = results
                totalSignals = monitor.state.value.totalSignalsToday
                blockedByWR = monitor.state.value.blockedByWRFilter
                avgConfluence = monitor.state.value.avgConfluence
                delay(3000)
            }
        } else {
            monitor.stopMonitoring()
        }
    }

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
                        text = if (isPersian) "اودین - مانیتور 80% WR" else "ODIN - 80% WR Monitor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "TradingView 20+ اندیکاتور + RR 1:2 + WR 80%" else "TradingView 20+ indicators + RR 1:2 + WR 80%",
                        fontSize = 10.sp,
                        color = OdinGold
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

        // Stats row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    label = if (isPersian) "سیگنال امروز" else "Signals Today",
                    value = "$totalSignals",
                    color = OdinGold,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = if (isPersian) "بلوکه WR<80%" else "Blocked WR<80%",
                    value = "$blockedByWR",
                    color = OdinRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = if (isPersian) "میانگین تاییدیه" else "Avg Confluence",
                    value = String.format("%.1f", avgConfluence),
                    color = OdinCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 80% WR Explanation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "فیلتر 80% - قانون طلایی" else "80% Filter - Golden Rule",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ حداقل RR 1:2 (ریسک 1 به ریوارد 2)\n✅ حداقل 5 تاییدیه از 20+ اندیکاتور TV\n✅ اعتماد Confluence >=80%\n✅ تاریخچه WR >=80% (وگرنه بلوکه)\n→ WR 80% + RR 1:2 = بسیار نادر اما طلایی\n→ 2-5 ترید در 90 روز با کیفیت بالا\n→ فرمول: Breakeven WR برای RR 1:2 = 33%"
                        else
                            "✅ Min RR 1:2 (Risk 1 Reward 2)\n✅ Min 5 confirmations from 20+ TV indicators\n✅ Confluence Confidence >=80%\n✅ Historical WR >=80% else BLOCKED\n→ WR 80% + RR 1:2 = Very rare but golden\n→ 2-5 trades per 90 days high quality\n→ Formula: Breakeven WR for RR 1:2 = 33%",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // TradingView Indicators Coverage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isPersian) "📊 20+ اندیکاتور TradingView پوشش داده شده" else "📊 20+ TradingView Indicators Covered",
                        fontWeight = FontWeight.Bold,
                        color = OdinCyan,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trend: SMA, EMA, WMA, HMA, VWMA, Ichimoku, PSAR, SuperTrend\nMomentum: RSI, Stochastic, MACD, CCI, Williams %R, AO, Stoch RSI\nVolatility: Bollinger, ATR, Keltner, Donchian\nVolume: OBV, VWAP, MFI\nLIT: Liquidity Sweep, BOS, Order Block, FVG (3x weight)",
                        fontSize = 9.sp,
                        color = OdinSilverMuted,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        items(analyses) { analysis ->
            SymbolCardWithTV(analysis = analysis, isPersian = isPersian, showTVDetails = showTVDetails, onToggleTV = { sym ->
                showTVDetails = if (showTVDetails == sym) null else sym
            })
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
                        text = if (isPersian) "📊 تحلیل 10 دلار + 80% WR" else "📊 $10 Analysis + 80% WR",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• 10$ خیلی کم برای فارکس (حداقل 100$)\n• کریپتو با لات کوچک ممکن\n• WR 80% + RR 1:2 = تئوری عالی، عمل نادر\n• LIT با 7+ تاییدیه → 75-80% WR واقعی\n• Breakeven: RR 1:2 فقط 33% WR لازم دارد\n• پس حتی 50% WR با RR 1:2 سودده است!\n• پیشنهاد: به جای WR تاریخی 80%، از Confluence 80% استفاده کن"
                        else
                            "• $10 too small for Forex (min $100)\n• Crypto with small lots possible\n• WR 80% + RR 1:2 = Great theory, rare practice\n• LIT with 7+ confirmations → 75-80% real WR\n• Breakeven: RR 1:2 needs only 33% WR\n• So even 50% WR with RR 1:2 is profitable!\n• Suggestion: Use Confluence 80% not Historical WR 80%",
                        fontSize = 10.sp,
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
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SymbolCardWithTV(analysis: SymbolAnalysis, isPersian: Boolean, showTVDetails: String?, onToggleTV: (String) -> Unit) {
    val hasSignal = analysis.signal != null
    val isBullish = analysis.signal?.side == SignalSide.BUY
    val confluence = analysis.confluence

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasSignal && isBullish -> OdinGreen.copy(alpha = 0.1f)
                hasSignal && !isBullish -> OdinRed.copy(alpha = 0.1f)
                analysis.wrBlocked -> OdinRed.copy(alpha = 0.05f)
                else -> OdinSurfaceVariant
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                hasSignal && isBullish -> OdinGreen.copy(alpha = 0.5f)
                hasSignal && !isBullish -> OdinRed.copy(alpha = 0.5f)
                analysis.wrBlocked -> OdinRed.copy(alpha = 0.3f)
                else -> OdinBorder
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
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

            Spacer(modifier = Modifier.height(8.dp))

            // TV Confluence + RR + WR Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ConfluenceMetric(
                    label = "Confluence",
                    value = "${confluence?.score ?: 0}",
                    hasValue = (confluence?.score ?: 0) >=5,
                    isAlert = (confluence?.score ?: 0) >=7
                )
                ConfluenceMetric(
                    label = "RR",
                    value = "1:${String.format("%.1f", analysis.rr)}",
                    hasValue = analysis.rr >=2.0,
                    isAlert = analysis.rr >=2.5
                )
                ConfluenceMetric(
                    label = "WR Hist",
                    value = if (analysis.historicalWR>0) "${analysis.historicalWR.toInt()}%" else "New",
                    hasValue = analysis.historicalWR >=80.0 || analysis.historicalWR==0.0,
                    isAlert = analysis.historicalWR >=80.0
                )
                ConfluenceMetric(
                    label = if (isPersian) "اعتماد" else "Conf",
                    value = "${(confluence?.confidence ?: 0.0).toInt()}%",
                    hasValue = (confluence?.confidence ?: 0.0) >=80,
                    isAlert = (confluence?.confidence ?: 0.0) >=80
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LIT Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LITMetricSmall(
                    label = if (isPersian) "نقدینگی" else "Liquidity",
                    value = if (analysis.liquidityHigh != null || analysis.liquidityLow != null) "✓ Pool" else "—",
                    hasValue = analysis.liquidityHigh != null || analysis.liquidityLow != null
                )
                LITMetricSmall(
                    label = if (isPersian) "سوئیپ" else "Sweep",
                    value = analysis.lastSweep?.replace("_", " ") ?: "—",
                    hasValue = analysis.lastSweep != null,
                    isAlert = analysis.lastSweep != null
                )
                LITMetricSmall(
                    label = "BOS",
                    value = if (analysis.hasBOS) analysis.bosDirection ?: "✓" else "—",
                    hasValue = analysis.hasBOS,
                    isAlert = analysis.hasBOS
                )
                LITMetricSmall(
                    label = "OB",
                    value = if (analysis.orderBlockHigh != null) "✓ OB" else "—",
                    hasValue = analysis.orderBlockHigh != null
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Toggle TV Details
            TextButton(onClick = { onToggleTV(analysis.symbol) }, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (showTVDetails == analysis.symbol) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OdinCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showTVDetails == analysis.symbol) {
                        if (isPersian) "مخفی اندیکاتورها" else "Hide Indicators"
                    } else {
                        if (isPersian) "نمایش ${analysis.tvIndicators.size} اندیکاتور TV" else "Show ${analysis.tvIndicators.size} TV Indicators"
                    },
                    fontSize = 10.sp,
                    color = OdinCyan
                )
            }

            if (showTVDetails == analysis.symbol) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinDeepSpace),
                    border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        analysis.tvIndicators.forEach { ind ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = ind.name, fontSize = 9.sp, color = OdinSilverMuted, modifier = Modifier.weight(1f))
                                Text(
                                    text = ind.signal,
                                    fontSize = 9.sp,
                                    color = when {
                                        ind.bullish -> OdinGreen
                                        ind.bearish -> OdinRed
                                        else -> OdinSilverMuted
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian) "تاییدیه‌ها: ${confluence?.confirmations?.joinToString(\", \") ?: \"—\"}" else "Confirmations: ${confluence?.confirmations?.joinToString(\", \") ?: \"—\"}",
                            fontSize = 9.sp,
                            color = OdinGold,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            // WR Blocked Warning
            if (analysis.wrBlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = OdinRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian)
                                "بلوکه شد: WR ${analysis.historicalWR.toInt()}% < 80% - با اینکه Confluence ${confluence?.score} و RR 1:${String.format(\"%.1f\", analysis.rr)} دارد"
                            else
                                "BLOCKED: WR ${analysis.historicalWR.toInt()}% < 80% - Despite Confluence ${confluence?.score} and RR 1:${String.format(\"%.1f\", analysis.rr)}",
                            fontSize = 10.sp,
                            color = OdinRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Signal Card if exists
            analysis.signal?.let { signal ->
                Spacer(modifier = Modifier.height(10.dp))
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
                                        if (isPersian) "سیگنال 80% خرید" else "80% BUY Signal"
                                    } else {
                                        if (isPersian) "سیگنال 80% فروش" else "80% SELL Signal"
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
                                    text = "${(signal.confidence*100).toInt()}% ${if (isPersian) "اعتماد" else "Conf"}",
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
private fun ConfluenceMetric(label: String, value: String, hasValue: Boolean, isAlert: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isAlert -> OdinGold.copy(alpha = 0.2f)
                        hasValue -> OdinGreen.copy(alpha = 0.15f)
                        else -> OdinRed.copy(alpha = 0.1f)
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = value,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isAlert -> OdinGold
                    hasValue -> OdinGreen
                    else -> OdinRed
                }
            )
        }
    }
}

@Composable
private fun LITMetricSmall(label: String, value: String, hasValue: Boolean, isAlert: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isAlert -> OdinGold.copy(alpha = 0.2f)
                        hasValue -> OdinCyan.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = value,
                fontSize = 9.sp,
                fontWeight = if (hasValue) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isAlert -> OdinGold
                    hasValue -> OdinCyan
                    else -> OdinSilverMuted
                }
            )
        }
    }
}
