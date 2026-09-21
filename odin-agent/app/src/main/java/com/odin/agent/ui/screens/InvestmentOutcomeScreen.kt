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
import com.odin.agent.trading.InvestmentOutcomeCalculator
import com.odin.agent.trading.JalaliCalendar
import com.odin.agent.trading.StrategyOutcome
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ODIN v1.0.26 - Investment Outcome Screen - Odin.trade
 * برآیند سرمایه گذاری هر استراتژی بر پایه 10$ - سود و زیان واقعی نهایی - ساعت و روز میلادی و شمسی
 */

@Composable
fun InvestmentOutcomeScreen(isPersian: Boolean) {
    val calculator = remember { InvestmentOutcomeCalculator() }
    var state by remember { mutableStateOf(calculator.state.value) }
    var selectedOutcome by remember { mutableStateOf<StrategyOutcome?>(null) }
    var days by remember { mutableStateOf(30) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        calculator.state.collect { newState -> state = newState }
    }

    LaunchedEffect(days) {
        calculator.calculateOutcomes(days = days)
    }

    if (selectedOutcome != null) {
        OutcomeDetailScreen(outcome = selectedOutcome!!, isPersian = isPersian, onBack = { selectedOutcome = null })
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = if (isPersian) "Odin.trade - برآیند سرمایه گذاری 10$ پایه هر استراتژی" else "Odin.trade - Investment Outcome 10$ Base Per Strategy", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(text = if (isPersian) "سود و زیان واقعی نهایی - ساعت و روز میلادی و شمسی - ویتاورس" else "Real final PnL - Gregorian & Jalali date/time - Vittaverse", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(text = if (isPersian) "سرمایه کل پایه" else "Total Initial", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${state.totalInitial.toInt()}$ (5×10$)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White) }
                        Column { Text(text = if (isPersian) "نهایی واقعی" else "Final REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", state.totalFinal)}$", fontSize = 12.sp, fontWeight = FontWeight.Black, color = OdinGoldLight) }
                        Column { Text(text = if (isPersian) "سود/زیان واقعی" else "PnL REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${if (state.totalPnL >= 0) "+" else ""}${String.format("%.2f", state.totalPnL)}$ (${String.format("%.1f", state.totalPnLPercent)}%)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (state.totalPnL >= 0) OdinGreen else OdinRed) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = JalaliCalendar.getCurrentDateTimeBoth(), fontSize = 7.sp, color = OdinCyan, lineHeight = 8.sp)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) "بازه زمانی - میلادی و شمسی" else "Period - Gregorian & Jalali", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (state.isCalculating) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = OdinGold, strokeWidth = 2.dp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 30, 90, 180, 365).forEach { d ->
                            FilterChip(selected = days == d, onClick = { days = d }, label = { Text("${d} ${if (isPersian) "روز" else "days"}", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
        }

        state.bestStrategy?.let { best ->
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "بهترین استراتژی: ${best.strategy.labelFa} - ${best.profitLossPercent.toInt()}% سود واقعی" else "Best: ${best.strategy.labelEn} - ${best.profitLossPercent.toInt()}% REAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                        }
                        Text(text = if (isPersian) "10$ → ${String.format("%.2f", best.finalCapital)}$ | سود ${String.format("%.2f", best.profitLoss)}$ واقعی | ${best.trades} معامله | ${best.wins} برد" else "10$ → ${String.format("%.2f", best.finalCapital)}$ | PnL ${String.format("%.2f", best.profitLoss)}$ REAL | ${best.trades} trades", fontSize = 9.sp, color = OdinSilver)
                    }
                }
            }
        }

        items(state.outcomes) { outcome ->
            OutcomeCard(outcome = outcome, isPersian = isPersian, onClick = { selectedOutcome = outcome })
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun OutcomeCard(outcome: StrategyOutcome, isPersian: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (outcome.profitLoss >= 0) Color(0xFF0A0F0A) else Color(0xFF0F0A0A)),
        border = BorderStroke(1.dp, if (outcome.profitLoss >= 0) OdinGreen.copy(alpha = 0.3f) else OdinRed.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (isPersian) outcome.strategy.labelFa else outcome.strategy.labelEn, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (outcome.profitLoss >= 0) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = "${if (outcome.profitLoss >= 0) "+" else ""}${String.format("%.1f", outcome.profitLossPercent)}% ${if (isPersian) "واقعی" else "REAL"}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (outcome.profitLoss >= 0) OdinGreen else OdinRed)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text(text = if (isPersian) "سرمایه پایه" else "Base", fontSize = 7.sp, color = OdinSilverMuted); Text(text = "${outcome.initialCapital.toInt()}$", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                Column { Text(text = if (isPersian) "نهایی واقعی" else "Final REAL", fontSize = 7.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", outcome.finalCapital)}$", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight) }
                Column { Text(text = if (isPersian) "سود/زیان واقعی" else "PnL REAL", fontSize = 7.sp, color = OdinSilverMuted); Text(text = "${if (outcome.profitLoss >= 0) "+" else ""}${String.format("%.2f", outcome.profitLoss)}$", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (outcome.profitLoss >= 0) OdinGreen else OdinRed) }
                Column { Text(text = if (isPersian) "معاملات" else "Trades", fontSize = 7.sp, color = OdinSilverMuted); Text(text = "${outcome.trades}", fontSize = 11.sp, color = Color.White) }
                Column { Text(text = "WR", fontSize = 7.sp, color = OdinSilverMuted); Text(text = "${outcome.winrate.toInt()}%", fontSize = 11.sp, color = OdinGreen) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "RR 1:${outcome.avgRR} | PF ${String.format("%.2f", outcome.profitFactor)} | Sharpe ${String.format("%.2f", outcome.sharpe)} | DD ${outcome.maxDrawdown.toInt()}%", fontSize = 8.sp, color = OdinSilverMuted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${outcome.gregorianStart} | ${outcome.jalaliStart.take(30)}", fontSize = 7.sp, color = OdinCyan, maxLines = 1)
            Text(text = if (isPersian) "اسپرد ${String.format("%.2f", outcome.totalSpreadCost)}$ + کمیسیون ${String.format("%.2f", outcome.totalCommission)}$ = سود خالص ${String.format("%.2f", outcome.netProfit)}$ واقعی ویتاورس" else "Spread ${String.format("%.2f", outcome.totalSpreadCost)}$ + Comm ${String.format("%.2f", outcome.totalCommission)}$ = Net ${String.format("%.2f", outcome.netProfit)}$ REAL Vittaverse", fontSize = 7.sp, color = OdinSilverDim)
        }
    }
}

@Composable
private fun OutcomeDetailScreen(outcome: StrategyOutcome, isPersian: Boolean, onBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
                Text(text = if (isPersian) "جزئیات برآیند ${outcome.strategy.labelFa} - 10$ پایه" else "Outcome ${outcome.strategy.labelEn} - 10$ Base", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (outcome.profitLoss >= 0) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(text = "${String.format("%.1f", outcome.profitLossPercent)}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (outcome.profitLoss >= 0) OdinGreen else OdinRed)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isPersian) "برآیند سرمایه گذاری واقعی - Odin.trade" else "Real Investment Outcome - Odin.trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(text = if (isPersian) "سرمایه اولیه" else "Initial", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${outcome.initialCapital}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White) }
                        Column { Text(text = if (isPersian) "نهایی واقعی" else "Final REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", outcome.finalCapital)}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGoldLight) }
                        Column { Text(text = if (isPersian) "سود واقعی" else "Profit REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${if (outcome.profitLoss >= 0) "+" else ""}${String.format("%.2f", outcome.profitLoss)}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (outcome.profitLoss >= 0) OdinGreen else OdinRed) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isPersian) "سود خالص بعد کسر اسپرد و کمیسیون ویتاورس واقعی: ${String.format("%.2f", outcome.netProfit)}$ - برآیند 10$ پایه" else "Net profit after Vittaverse REAL spread & commission: ${String.format("%.2f", outcome.netProfit)}$ - 10$ base outcome", fontSize = 9.sp, color = OdinSilver, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "آمار معاملات واقعی - ویتاورس" else "Real Trading Stats - Vittaverse", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "کل معاملات: ${outcome.trades}" else "Total: ${outcome.trades}", fontSize = 9.sp, color = Color.White)
                        Text(text = if (isPersian) "برد: ${outcome.wins}" else "Wins: ${outcome.wins}", fontSize = 9.sp, color = OdinGreen)
                        Text(text = if (isPersian) "باخت: ${outcome.losses}" else "Losses: ${outcome.losses}", fontSize = 9.sp, color = OdinRed)
                        Text(text = "WR ${outcome.winrate.toInt()}%", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "RR 1:${outcome.avgRR}", fontSize = 9.sp, color = OdinGold)
                        Text(text = "PF ${String.format("%.2f", outcome.profitFactor)}", fontSize = 9.sp, color = OdinCyan)
                        Text(text = "Sharpe ${String.format("%.2f", outcome.sharpe)}", fontSize = 9.sp, color = OdinSilver)
                        Text(text = "DD ${outcome.maxDrawdown.toInt()}%", fontSize = 9.sp, color = OdinAmber)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "بهترین: +${String.format("%.2f", outcome.bestTrade)}$" else "Best: +${String.format("%.2f", outcome.bestTrade)}$", fontSize = 8.sp, color = OdinGreen)
                        Text(text = if (isPersian) "بدترین: ${String.format("%.2f", outcome.worstTrade)}$" else "Worst: ${String.format("%.2f", outcome.worstTrade)}$", fontSize = 8.sp, color = OdinRed)
                        Text(text = if (isPersian) "اسپرد: ${String.format("%.2f", outcome.totalSpreadCost)}$" else "Spread: ${String.format("%.2f", outcome.totalSpreadCost)}$", fontSize = 8.sp, color = OdinGoldLight)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "تاریخ و ساعت - میلادی و شمسی - دقیق" else "Date & Time - Gregorian & Jalali - Precise", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = outcome.gregorianStart, fontSize = 9.sp, color = Color.White)
                    Text(text = outcome.jalaliStart, fontSize = 9.sp, color = OdinGoldLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = outcome.gregorianEnd, fontSize = 9.sp, color = Color.White)
                    Text(text = outcome.jalaliEnd, fontSize = 9.sp, color = OdinGoldLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = if (isPersian) "مدت: ${outcome.durationDays} روز | بروکر: ${outcome.broker} | منبع: ${outcome.source} - Odin.trade" else "Duration: ${outcome.durationDays} days | Broker: ${outcome.broker} | Source: ${outcome.source} - Odin.trade", fontSize = 8.sp, color = OdinSilverDim)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = JalaliCalendar.getCurrentDateTimeBoth(), fontSize = 7.sp, color = OdinCyan)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.05f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.2f)), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "تحلیل برآیند 10$ - Odin.trade" else "10$ Outcome Analysis - Odin.trade", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "استراتژی ${outcome.strategy.labelFa} با سرمایه پایه 10$ در ${outcome.durationDays} روز ${outcome.trades} معامله انجام داد - ${outcome.wins} برد و ${outcome.losses} باخت - وین‌ریت ${outcome.winrate.toInt()}% - RR 1:${outcome.avgRR} - سود خالص واقعی بعد کسر اسپرد ویتاورس ${String.format("%.2f", outcome.totalSpreadCost)}$ و کمیسیون ${String.format("%.2f", outcome.totalCommission)}$ برابر ${String.format("%.2f", outcome.netProfit)}$ شد - سرمایه نهایی ${String.format("%.2f", outcome.finalCapital)}$ - ${outcome.profitLossPercent.toInt()}% - ویتاورس واقعی https://vittaverse.com/fa/ - Odin.trade"
                        else "Strategy ${outcome.strategy.labelEn} with 10$ base in ${outcome.durationDays} days did ${outcome.trades} trades - ${outcome.wins} wins ${outcome.losses} losses - WR ${outcome.winrate.toInt()}% - RR 1:${outcome.avgRR} - Net REAL after Vittaverse spread ${String.format("%.2f", outcome.totalSpreadCost)}$ & commission ${String.format("%.2f", outcome.totalCommission)}$ = ${String.format("%.2f", outcome.netProfit)}$ - Final ${String.format("%.2f", outcome.finalCapital)}$ - ${outcome.profitLossPercent.toInt()}% - Vittaverse REAL",
                        fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp
                    )
                }
            }
        }
    }
}
