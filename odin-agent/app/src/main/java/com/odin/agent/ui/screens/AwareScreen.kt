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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.aware.AwareLearningEngine
import com.odin.agent.aware.StrategyLearningStats
import com.odin.agent.aware.SymbolBestStrategy
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AwareScreen(
    isPersian: Boolean,
    engine: AwareLearningEngine = remember { AwareLearningEngine() }
) {
    var isLearning by remember { mutableStateOf(false) }
    var awareness by remember { mutableStateOf(0.0) }
    var bestPerSymbol by remember { mutableStateOf<Map<String, SymbolBestStrategy>>(emptyMap()) }
    var strategyStats by remember { mutableStateOf<Map<com.odin.agent.models.QuantStrategyType, StrategyLearningStats>>(emptyMap()) }
    var totalLearnings by remember { mutableStateOf(0) }

    LaunchedEffect(isLearning) {
        if (isLearning) {
            engine.startLearning()
            while (isLearning) {
                // Generate mock experience every 1.5 seconds
                engine.generateMockExperience()
                awareness = engine.state.value.awarenessLevel
                bestPerSymbol = engine.state.value.bestPerSymbol
                strategyStats = engine.state.value.strategyStats
                totalLearnings = engine.state.value.totalLearnings
                delay(1500)
            }
        } else {
            engine.stopLearning()
        }
    }

    LaunchedEffect(Unit) {
        // Initial learning
        repeat(5) { engine.generateMockExperience() }
        awareness = engine.state.value.awarenessLevel
        bestPerSymbol = engine.state.value.bestPerSymbol
        strategyStats = engine.state.value.strategyStats
        totalLearnings = engine.state.value.totalLearnings
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
                        text = if (isPersian) "AWARE - موتور یادگیری دقیق" else "AWARE - Precise Learning Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "آنلاین دائم در حال یادگیری استراتژی‌ها و مدیریت مالی" else "Online continuous learning of strategies & money management",
                        fontSize = 9.sp,
                        color = OdinGold
                    )
                }

                Button(
                    onClick = { isLearning = !isLearning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLearning) OdinRed else OdinGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isLearning) Icons.Default.Stop else Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isLearning) "توقف" else "یادگیری", fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF151000), Color(0xFF0A0A0A)))
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isPersian) "سطح آگاهی" else "Awareness Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "${awareness.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (awareness / 100).toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = OdinGold,
                            trackColor = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isPersian) "تجربه‌ها: $totalLearnings" else "Experiences: $totalLearnings", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = if (isLearning) "● ONLINE LEARNING" else "○ PAUSED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isLearning) OdinGreen else OdinSilverMuted)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050A05)),
                border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "بهترین تنظیمات LIT برای RR 1:5 - تحقیق شده" else "Best LIT for RR 1:5 - Researched", fontWeight = FontWeight.Bold, color = OdinGreen, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val lit = engine.getOptimalLITSettings()
                    Text(
                        text = if (isPersian)
                            "HTF ${lit.htfBias} | OB ${lit.obEntryPercent*100}% | FVG ${if (lit.fvgRequired) "الزامی" else "اختیاری"} | RR ${lit.minRR}-${lit.maxRR} | Risk ${lit.riskPerTrade}% | TF ${lit.htfTimeframe}→${lit.ltfTimeframe}→${lit.entryTimeframe}"
                        else
                            "HTF ${lit.htfBias} | OB ${lit.obEntryPercent*100}% | FVG ${if (lit.fvgRequired) "Req" else "Opt"} | RR ${lit.minRR}-${lit.maxRR} | Risk ${lit.riskPerTrade}% | TF ${lit.htfTimeframe}→${lit.ltfTimeframe}→${lit.entryTimeframe}",
                        fontSize = 9.sp,
                        color = OdinSilver,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        item {
            Text(text = if (isPersian) "بهترین استراتژی برای هر نماد - حافظه" else "Best Strategy Per Symbol - Memory", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        if (bestPerSymbol.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        Text(text = if (isPersian) "در حال یادگیری و کشف بهترین..." else "Learning & discovering best...", fontSize = 11.sp, color = OdinSilverMuted)
                    }
                }
            }
        } else {
            items(bestPerSymbol.values.toList()) { best ->
                BestStrategyCard(best = best, isPersian = isPersian)
            }
        }

        item {
            Text(text = if (isPersian) "آمار یادگیری استراتژی‌ها" else "Strategy Learning Stats", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(strategyStats.values.sortedByDescending { it.winrate }) { stats ->
            StrategyLearningCard(stats = stats, isPersian = isPersian)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "یادگیری مدیریت مالی" else "Money Management Learning", fontWeight = FontWeight.Bold, color = OdinCyan, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val mm = engine.state.value.moneyManagement
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Optimal Risk", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = "${String.format("%.2f", mm.optimalRisk)}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                        }
                        Column {
                            Text(text = "Best RR", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = "1:${String.format("%.1f", mm.bestRR)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                        }
                        Column {
                            Text(text = "Best Confl", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = "${mm.bestConfluence}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                        }
                        Column {
                            Text(text = "Kelly", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = String.format("%.3f", mm.kellyCriterion), fontSize = 10.sp, color = OdinSilver)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BestStrategyCard(best: SymbolBestStrategy, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = best.symbol, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGold.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = best.bestStrategy.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "WR ${best.winrate.toInt()}%", fontSize = 10.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                Text(text = "RR 1:${String.format("%.1f", best.bestRR)}", fontSize = 10.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                Text(text = "Confl ${best.bestConfluence}", fontSize = 10.sp, color = OdinCyan)
                Text(text = "Conf ${best.confidence.toInt()}%", fontSize = 10.sp, color = OdinSilver)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = if (isPersian) "دلیل: ${best.reason}" else "Reason: ${best.reason}", fontSize = 9.sp, color = OdinSilverMuted, lineHeight = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "${best.backtestCount} backtests • ${(System.currentTimeMillis() - best.discoveredAt)/1000}s ago", fontSize = 8.sp, color = OdinSilverDim)
        }
    }
}

@Composable
private fun StrategyLearningCard(stats: StrategyLearningStats, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, if (stats.winrate >= 60) OdinGreen.copy(alpha = 0.3f) else OdinBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stats.strategy.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${stats.winrate.toInt()}% WR • ${stats.totalTrades} trades", fontSize = 9.sp, color = if (stats.winrate >= 60) OdinGreen else OdinSilverMuted, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Avg RR 1:${String.format("%.1f", stats.avgRR)}", fontSize = 9.sp, color = OdinGold)
                Text(text = "PF ${String.format("%.2f", stats.profitFactor)}", fontSize = 9.sp, color = OdinCyan)
                Text(text = "PnL ${String.format("%.1f", stats.totalPnL)}", fontSize = 9.sp, color = if (stats.totalPnL >= 0) OdinGreen else OdinRed)
                Text(text = "Opt Risk ${String.format("%.1f", stats.optimalRisk)}%", fontSize = 9.sp, color = OdinSilver)
            }
            if (stats.learnedLessons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "💡 ${stats.learnedLessons.last()}", fontSize = 8.sp, color = OdinSilverDim, lineHeight = 10.sp)
            }
        }
    }
}
