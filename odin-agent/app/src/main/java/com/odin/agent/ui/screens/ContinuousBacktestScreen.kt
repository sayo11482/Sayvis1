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
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.trading.ContinuousBacktestEngine
import com.odin.agent.trading.StrategyPower
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ContinuousBacktestScreen(isPersian: Boolean, engine: ContinuousBacktestEngine = remember { ContinuousBacktestEngine() }) {
    var powers by remember { mutableStateOf<Map<QuantStrategyType, StrategyPower>>(emptyMap()) }
    var isRunning by remember { mutableStateOf(false) }
    var totalTests by remember { mutableStateOf(0) }
    var bestStrategy by remember { mutableStateOf<QuantStrategyType?>(null) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            engine.startContinuous()
            while (isRunning) {
                val result = engine.runBacktestCycle()
                powers = result
                totalTests = engine.state.value.totalTests
                bestStrategy = engine.state.value.bestStrategy
                delay(2000)
            }
        } else engine.stopContinuous()
    }
    LaunchedEffect(Unit) {
        powers = engine.runBacktestCycle()
        totalTests = engine.state.value.totalTests
        bestStrategy = engine.state.value.bestStrategy
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (isPersian) "بک‌تست دائمی - امتیاز قدرت" else "Continuous Backtest - Power Score", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = if (isPersian) "تمام استراتژی‌ها مجاز - امتیازدهی بدون ممنوعیت" else "All Strategies Allowed - Scored No Ban", fontSize = 10.sp, color = OdinGreen)
                }
                Button(onClick = { isRunning = !isRunning }, colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) OdinRed else OdinGreen), shape = RoundedCornerShape(10.dp)) {
                    Icon(imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isRunning) if (isPersian) "توقف" else "Stop" else if (isPersian) "شروع" else "Start", fontSize = 11.sp)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCardSmall(label = if (isPersian) "کل تست" else "Total Tests", value = "$totalTests", color = OdinCyan, modifier = Modifier.weight(1f))
                StatCardSmall(label = if (isPersian) "بهترین" else "Best", value = bestStrategy?.name?.take(8) ?: "-", color = OdinGold, modifier = Modifier.weight(1f))
                StatCardSmall(label = if (isPersian) "استراتژی‌ها" else "Strategies", value = "${powers.size}", color = OdinGreen, modifier = Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "قانون جدید: بدون ممنوعیت - فقط امتیاز قدرت" else "New Rule v1.0.14: No Ban - Only Power Score", fontWeight = FontWeight.Bold, color = OdinGreen, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian) "• تمام استراتژی‌ها همیشه مجاز ✅\n• امتیاز قدرت = سود ۴۰% + وین‌ریت ۳۰% + پروفیت فکتور ۲۰% + شارپ ۱۰%\n• پایداری = ثبات نتایج\n• بک‌تست دائمی ۵۰ ترید با ریسک ۱%\n• رتبه‌بندی بر اساس قدرت - بهترین برای هر نماد"
                        else "• All strategies always allowed ✅\n• Power Score = Profit 40% + WR 30% + PF 20% + Sharpe 10%\n• Stability = consistency\n• Continuous backtest 50 trades 1% risk\n• Ranked by power - best per symbol",
                        fontSize = 10.sp, color = OdinSilver, lineHeight = 13.sp
                    )
                }
            }
        }
        items(powers.values.sortedByDescending { it.powerScore }) { power -> StrategyPowerCard(power = power, isPersian = isPersian) }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCardSmall(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun StrategyPowerCard(power: StrategyPower, isPersian: Boolean) {
    val isTop = power.rank == 1
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = when { isTop -> OdinGold.copy(alpha = 0.08f); power.results.size < 3 -> Color(0xFF0A0A0A); else -> OdinGreen.copy(alpha = 0.05f) }),
        border = BorderStroke(1.dp, when { isTop -> OdinGold.copy(alpha = 0.5f); power.results.size < 3 -> OdinBorder; else -> OdinGreen.copy(alpha = 0.3f) }),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(when { isTop -> OdinGold.copy(alpha = 0.3f); power.results.size < 3 -> OdinSilverMuted.copy(alpha = 0.2f); else -> OdinGreen.copy(alpha = 0.2f) }), contentAlignment = Alignment.Center) {
                        Text(text = if (isTop) "🏆" else "#${power.rank}", fontSize = if (isTop) 14.sp else 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = power.strategy.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${power.results.size} ${if (isPersian) "تست" else "tests"} • ${if (isPersian) "رتبه" else "Rank"} #${power.rank}", fontSize = 8.sp, color = OdinSilverMuted)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "${if (isPersian) "قدرت" else "Power"} ${power.powerScore.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (isTop) OdinGold else OdinGreen)
                    Text(text = "${if (isPersian) "پایداری" else "Stability"} ${power.stability.toInt()}%", fontSize = 8.sp, color = OdinCyan)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (power.results.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(text = if (isPersian) "میانگین نهایی" else "Avg Final", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "$${String.format("%.1f", power.avgFinal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (power.avgFinal >= 100) OdinGreen else OdinSilver) }
                    Column { Text(text = if (isPersian) "میانگین سود" else "Avg Profit", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.1f", power.avgProfit)}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (power.avgProfit >= 0) OdinGreen else OdinRed) }
                    Column { Text(text = if (isPersian) "وین‌ریت" else "WR", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${power.avgWinrate.toInt()}%", fontSize = 10.sp, color = OdinGold, fontWeight = FontWeight.Bold) }
                    Column { Text(text = "PF", fontSize = 8.sp, color = OdinSilverMuted); Text(text = String.format("%.2f", power.avgProfitFactor), fontSize = 10.sp, color = OdinCyan) }
                    Column { Text(text = if (isPersian) "شارپ" else "Sharpe", fontSize = 8.sp, color = OdinSilverMuted); Text(text = String.format("%.2f", power.avgSharpe), fontSize = 10.sp, color = OdinSilver) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    power.results.takeLast(5).forEach { res ->
                        Box(modifier = Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(4.dp)).background(if (res.profit >= 0) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text(text = "$${res.finalCapital.toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (res.profit >= 0) OdinGreen else OdinRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = if (isPersian) "قدرت: سود ${String.format("%.0f", power.avgProfit)}% + وین‌ریت ${power.avgWinrate.toInt()}% + PF ${String.format("%.1f", power.avgProfitFactor)}" else "Power: Profit ${String.format("%.0f", power.avgProfit)}% + WR ${power.avgWinrate.toInt()}% + PF ${String.format("%.1f", power.avgProfitFactor)}", fontSize = 9.sp, color = OdinSilverMuted, lineHeight = 11.sp)
            }
        }
    }
}
