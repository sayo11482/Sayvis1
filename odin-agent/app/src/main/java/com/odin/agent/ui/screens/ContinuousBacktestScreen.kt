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
fun ContinuousBacktestScreen(
    isPersian: Boolean,
    engine: ContinuousBacktestEngine = remember { ContinuousBacktestEngine() }
) {
    var powers by remember { mutableStateOf<Map<QuantStrategyType, StrategyPower>>(emptyMap()) }
    var isRunning by remember { mutableStateOf(false) }
    var totalTests by remember { mutableStateOf(0) }
    var bannedCount by remember { mutableStateOf(0) }
    var validCount by remember { mutableStateOf(0) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            engine.startContinuous()
            while (isRunning) {
                val result = engine.runBacktestCycle()
                powers = result
                totalTests = engine.state.value.totalTests
                bannedCount = engine.state.value.bannedCount
                validCount = engine.state.value.validCount
                delay(2000) // Every 2 seconds new cycle
            }
        } else {
            engine.stopContinuous()
        }
    }

    LaunchedEffect(Unit) {
        // Initial cycle
        powers = engine.runBacktestCycle()
        totalTests = engine.state.value.totalTests
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
                        text = if (isPersian) "بک‌تست دائمی - قدرت استراتژی" else "Continuous Backtest - Strategy Power",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "10$ → 15$ وگرنه ممنوع - 5 تست" else "$10 → $15 else BANNED - 5 tests",
                        fontSize = 10.sp,
                        color = OdinGold
                    )
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) OdinRed else OdinGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isRunning) "توقف" else "شروع", fontSize = 11.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCardSmall(label = if (isPersian) "کل تست" else "Total Tests", value = "$totalTests", color = OdinCyan, modifier = Modifier.weight(1f))
                StatCardSmall(label = if (isPersian) "مجاز" else "Valid", value = "$validCount", color = OdinGreen, modifier = Modifier.weight(1f))
                StatCardSmall(label = if (isPersian) "ممنوع" else "Banned", value = "$bannedCount", color = OdinRed, modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "قانون طلایی: 10$ → 15$ وگرنه ممنوع" else "Golden Rule: $10 → $15 else BANNED",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• هر استراتژی 5 بار بک‌تست با 10 دلار\n• اگر میانگین یا کمترین زیر 15 دلار → ممنوع 🚫\n• اگر WR تست‌ها زیر 60% → ممنوع\n• تمام استراتژی‌ها دائم بک‌تست برای تحلیل قدرت\n• پایداری و امتیاز قدرت محاسبه می‌شود"
                        else
                            "• Each strategy 5 backtests with $10\n• If avg or min < $15 → BANNED 🚫\n• If test WR <60% → BANNED\n• All strategies continuous backtest for power analysis\n• Stability & power score calculated",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        items(powers.values.sortedByDescending { it.powerScore }) { power ->
            StrategyPowerCard(power = power, isPersian = isPersian)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCardSmall(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun StrategyPowerCard(power: StrategyPower, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                power.isBanned -> OdinRed.copy(alpha = 0.08f)
                power.results.size < 5 -> Color(0xFF0A0A0A)
                else -> OdinGreen.copy(alpha = 0.08f)
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                power.isBanned -> OdinRed.copy(alpha = 0.4f)
                power.results.size < 5 -> OdinBorder
                else -> OdinGreen.copy(alpha = 0.4f)
            }
        ),
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    power.isBanned -> OdinRed.copy(alpha = 0.2f)
                                    power.results.size < 5 -> OdinSilverMuted.copy(alpha = 0.2f)
                                    else -> OdinGreen.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (power.isBanned) "🚫" else if (power.results.size < 5) "⏳" else "✅",
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = power.strategy.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${power.results.size}/5 tests",
                            fontSize = 8.sp,
                            color = OdinSilverMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (power.isBanned) {
                            if (isPersian) "ممنوع" else "BANNED"
                        } else if (power.results.size < 5) {
                            if (isPersian) "در حال تست" else "Testing"
                        } else {
                            if (isPersian) "مجاز" else "VALID"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = when {
                            power.isBanned -> OdinRed
                            power.results.size < 5 -> OdinSilverMuted
                            else -> OdinGreen
                        }
                    )
                    Text(
                        text = "Power ${power.powerScore.toInt()}%",
                        fontSize = 9.sp,
                        color = OdinGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (power.results.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Avg Final", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(
                            text = "$${String.format("%.2f", power.avgFinal)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (power.avgFinal >= 15.0) OdinGreen else OdinRed
                        )
                    }
                    Column {
                        Text(text = "Min", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "$${String.format("%.2f", power.minFinal)}", fontSize = 10.sp, color = OdinSilver)
                    }
                    Column {
                        Text(text = "Max", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "$${String.format("%.2f", power.maxFinal)}", fontSize = 10.sp, color = OdinSilver)
                    }
                    Column {
                        Text(text = "WR Tests", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(
                            text = "${power.winrateTests.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (power.winrateTests >= 60) OdinGreen else OdinRed
                        )
                    }
                    Column {
                        Text(text = "Stability", fontSize = 8.sp, color = OdinSilverMuted)
                        Text(text = "${power.stability.toInt()}%", fontSize = 10.sp, color = OdinCyan)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Visual finals
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    power.results.forEach { res ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (res.passed) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$${res.finalCapital.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.passed) OdinGreen else OdinRed
                            )
                        }
                    }
                    repeat(5 - power.results.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1A1A1A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "-", fontSize = 8.sp, color = OdinSilverDim)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isPersian) "دلیل: ${power.banReason}" else "Reason: ${power.banReason}",
                    fontSize = 9.sp,
                    color = if (power.isBanned) OdinRed else OdinSilverMuted,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
