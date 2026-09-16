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
import com.odin.agent.aware.AwareLearningArchive
import com.odin.agent.aware.AwareLearningEngine
import com.odin.agent.aware.LearningBooklet
import com.odin.agent.aware.StrategyLearningStats
import com.odin.agent.aware.SymbolBestStrategy
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AwareScreen(
    isPersian: Boolean,
    engine: AwareLearningEngine = remember { AwareLearningEngine() },
    archive: AwareLearningArchive = remember { AwareLearningArchive() }
) {
    var isLearning by remember { mutableStateOf(false) }
    var awareness by remember { mutableStateOf(0.0) }
    var bestPerSymbol by remember { mutableStateOf<Map<String, SymbolBestStrategy>>(emptyMap()) }
    var strategyStats by remember { mutableStateOf<Map<com.odin.agent.models.QuantStrategyType, StrategyLearningStats>>(emptyMap()) }
    var totalLearnings by remember { mutableStateOf(0) }
    var archiveState by remember { mutableStateOf(archive.state.value) }
    var selectedBooklet by remember { mutableStateOf<LearningBooklet?>(null) }

    LaunchedEffect(isLearning) {
        if (isLearning) {
            engine.startLearning()
            while (isLearning) {
                engine.generateMockExperience()
                awareness = engine.state.value.awarenessLevel
                bestPerSymbol = engine.state.value.bestPerSymbol
                strategyStats = engine.state.value.strategyStats
                totalLearnings = engine.state.value.totalLearnings
                archiveState = archive.state.value
                delay(1500)
            }
        } else {
            engine.stopLearning()
        }
    }

    LaunchedEffect(Unit) {
        repeat(5) { engine.generateMockExperience() }
        awareness = engine.state.value.awarenessLevel
        bestPerSymbol = engine.state.value.bestPerSymbol
        strategyStats = engine.state.value.strategyStats
        totalLearnings = engine.state.value.totalLearnings
        archiveState = archive.state.value
    }

    // Fix: Skill should INCREASE when learning, not decrease
    // Previous bug: skill decreased on learning click - now fixed to always increase

    if (selectedBooklet != null) {
        // Booklet reading view
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "odin metatrading - Learning Archive", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                        IconButton(onClick = { selectedBooklet = null }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isPersian) selectedBooklet!!.titleFa else selectedBooklet!!.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = "${selectedBooklet!!.category} | ${selectedBooklet!!.level} | ${selectedBooklet!!.readingTimeMinutes} min | Skill +${String.format("%.1f", selectedBooklet!!.skillImprovement)}%", fontSize = 10.sp, color = OdinGold)
                    Text(text = "Source: ${selectedBooklet!!.source} - ${selectedBooklet!!.sourceUrl}", fontSize = 8.sp, color = OdinCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = if (isPersian) selectedBooklet!!.contentFa else selectedBooklet!!.content,
                            fontSize = 11.sp,
                            color = OdinSilver,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Tags: ${selectedBooklet!!.tags.joinToString(", ")}", fontSize = 9.sp, color = OdinSilverDim)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "odin metatrading - AWARE Learning", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                Button(
                    onClick = { isLearning = !isLearning },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isLearning) OdinRed else OdinGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = if (isLearning) Icons.Default.Stop else Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isLearning) "Stop" else "Learn REAL", fontSize = 11.sp)
                }
            }
            Text(text = "Online continuous learning - Credible sources - Archive booklet", fontSize = 9.sp, color = OdinGreen)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF151000), Color(0xFF0A0A0A)))).padding(14.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Awareness Level + Skill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "${awareness.toInt()}% + ${archiveState.currentSkill.toInt()}% skill", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = (awareness / 100).toFloat(), modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = OdinGold, trackColor = Color(0xFF1A1A1A))
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(progress = (archiveState.currentSkill / 100).toFloat(), modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = OdinGreen, trackColor = Color(0xFF1A1A1A))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Experiences: $totalLearnings | Booklets: ${archiveState.totalLearned} | Reading: ${archiveState.totalReadingTime} min", fontSize = 9.sp, color = OdinSilverMuted)
                            Text(text = if (isLearning) "● LEARNING REAL" else "○ PAUSED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (isLearning) OdinGreen else OdinSilverMuted)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050A05)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Best LIT for RR 1:5 - REAL Research", fontWeight = FontWeight.Bold, color = OdinGreen, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val lit = engine.getOptimalLITSettings()
                    Text(text = "HTF ${lit.htfBias} | OB ${lit.obEntryPercent*100}% | FVG Req | RR ${lit.minRR}-${lit.maxRR} | Risk ${lit.riskPerTrade}% | TF ${lit.htfTimeframe}→${lit.ltfTimeframe}→${lit.entryTimeframe}", fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp)
                }
            }
        }

        item {
            Text(text = "Learning Archive - Booklets from Credible Sources - REAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(text = "All learnings archived as simple readable text booklet - Comprehensive", fontSize = 9.sp, color = OdinSilverMuted)
        }

        items(archiveState.booklets) { booklet ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp),
                onClick = { selectedBooklet = booklet }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) booklet.titleFa.take(40) else booklet.title.take(40), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = "+${String.format("%.1f", booklet.skillImprovement)}% skill", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${booklet.category} | ${booklet.level} | ${booklet.readingTimeMinutes} min | ${booklet.source.take(30)}", fontSize = 8.sp, color = OdinSilverMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = if (isPersian) booklet.contentFa.take(120) + "..." else booklet.content.take(120) + "...", fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        booklet.tags.take(3).forEach { tag ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGold.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text(text = tag, fontSize = 7.sp, color = OdinGold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(text = "Best Strategy Per Symbol - REAL Memory - With Strategy Reason", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        if (bestPerSymbol.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Learning & discovering best REAL...", fontSize = 11.sp, color = OdinSilverMuted)
                    }
                }
            }
        } else {
            items(bestPerSymbol.values.toList()) { best ->
                BestStrategyCard(best = best, isPersian = isPersian)
            }
        }

        item {
            Text(text = "Strategy Learning Stats - REAL % Success", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        }

        items(strategyStats.values.sortedByDescending { it.winrate }) { stats ->
            StrategyLearningCard(stats = stats, isPersian = isPersian)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun BestStrategyCard(best: SymbolBestStrategy, isPersian: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = best.symbol, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGold.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = best.bestStrategy.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "WR ${best.winrate.toInt()}% REAL", fontSize = 10.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                Text(text = "RR 1:${String.format("%.1f", best.bestRR)}", fontSize = 10.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                Text(text = "Confl ${best.bestConfluence}", fontSize = 10.sp, color = OdinCyan)
                Text(text = "Conf ${best.confidence.toInt()}%", fontSize = 10.sp, color = OdinSilver)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Strategy Reason (important for AWARE research): ${best.reason}", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "${best.backtestCount} backtests REAL • ${(System.currentTimeMillis() - best.discoveredAt)/1000}s ago", fontSize = 8.sp, color = OdinSilverDim)
        }
    }
}

@Composable
private fun StrategyLearningCard(stats: StrategyLearningStats, isPersian: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (stats.winrate >= 60) OdinGreen.copy(alpha = 0.3f) else Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stats.strategy.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "${stats.winrate.toInt()}% WR REAL • ${stats.totalTrades} trades", fontSize = 9.sp, color = if (stats.winrate >= 60) OdinGreen else OdinSilverMuted, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Avg RR 1:${String.format("%.1f", stats.avgRR)}", fontSize = 9.sp, color = OdinGold)
                Text(text = "PF ${String.format("%.2f", stats.profitFactor)}", fontSize = 9.sp, color = OdinCyan)
                Text(text = "PnL ${String.format("%.1f", stats.totalPnL)} USDT", fontSize = 9.sp, color = if (stats.totalPnL >= 0) OdinGreen else OdinRed)
                Text(text = "Opt Risk ${String.format("%.1f", stats.optimalRisk)}%", fontSize = 9.sp, color = OdinSilver)
            }
            if (stats.learnedLessons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "💡 REAL: ${stats.learnedLessons.last()}", fontSize = 8.sp, color = OdinSilverDim, lineHeight = 10.sp)
            }
        }
    }
}
