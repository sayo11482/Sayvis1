package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.LifeScenario
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.model.LitAnalysisSignal
import com.example.sayvis.model.TradingGateState
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

@Composable
fun SimulationTradingScreen(
    lifeScenarios: List<LifeScenario>,
    tradingGate: TradingGateState,
    litSignals: List<LitAnalysisSignal>,
    isPersian: Boolean,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("simulation_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isPersian) "شبیه‌سازی مسیر زندگی و تحلیل LIT" else "Life Simulation & LIT Intelligence",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isPersian) "مدل‌سازی پیامد تصمیمات و معاملات هوشمند با بیشترین بازدهی" else "Trajectory forecasting & smart trading for highest yield",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SayvisSurface,
            contentColor = SayvisGold,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (isPersian) "شبیه‌سازی زندگی" else "Life Trajectory",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isPersian) "تحلیل معاملاتی LIT" else "LIT Intelligence",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SayvisCyan.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, SayvisCyan.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "پیش‌بینی‌های این بخش بر اساس برون‌یابی داده‌های مدل شناختی (UIC) و اهداف شما است و ماهیت سناریوسازی دارد." else "These trajectories are predictive scenario simulations derived from your UIC attributes and strategic constraints.",
                                fontSize = 11.sp,
                                color = SayvisCyan
                            )
                        }
                    }
                }

                items(lifeScenarios, key = { it.id }) { scenario ->
                    LifeScenarioCard(scenario = scenario, isPersian = isPersian)
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // === INTELLIGENT TRADING GATE — LIVE ENABLED (هوشمند) ===
                item {
                    val gateColor: Color
                    val gateIcon = when {
                        tradingGate.killSwitchEngaged -> {
                            gateColor = SayvisRedAlert
                            Icons.Default.Lock
                        }
                        tradingGate.liveTradingBlocked -> {
                            gateColor = SayvisAmberWarning
                            Icons.Default.Warning
                        }
                        else -> {
                            gateColor = SayvisGreenSuccess
                            Icons.Default.CheckCircle
                        }
                    }
                    if (tradingGate.killSwitchEngaged) gateColor else if (tradingGate.liveTradingBlocked) SayvisAmberWarning else SayvisGreenSuccess
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                tradingGate.killSwitchEngaged -> SayvisRedAlert.copy(alpha = 0.12f)
                                tradingGate.liveTradingBlocked -> SayvisAmberWarning.copy(alpha = 0.10f)
                                else -> SayvisGreenSuccess.copy(alpha = 0.10f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                tradingGate.killSwitchEngaged -> SayvisRedAlert.copy(alpha = 0.5f)
                                tradingGate.liveTradingBlocked -> SayvisAmberWarning.copy(alpha = 0.5f)
                                else -> SayvisGreenSuccess.copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(gateIcon, contentDescription = null, tint = gateColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            tradingGate.killSwitchEngaged -> if (isPersian) "دروازه ایمنی: قفل اضطراری فعال" else "Safety Gate: Kill Switch ENGAGED"
                                            tradingGate.liveTradingBlocked -> if (isPersian) "حالت تمرین — کاغذی" else "Paper Practice Mode"
                                            else -> if (isPersian) "معاملات زندهٔ هوشمند: فعال ✅" else "Smart Live Trading: ACTIVE ✅"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = gateColor
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            when {
                                                tradingGate.killSwitchEngaged -> SayvisRedAlert.copy(alpha = 0.18f)
                                                tradingGate.liveTradingBlocked -> SayvisAmberWarning.copy(alpha = 0.18f)
                                                else -> SayvisGreenSuccess.copy(alpha = 0.18f)
                                            }, RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            tradingGate.killSwitchEngaged -> if (isPersian) "متوقف" else "PAUSED"
                                            tradingGate.liveTradingBlocked -> if (isPersian) "بدون ریسک" else "RISK-FREE"
                                            else -> if (isPersian) "هوش فعال" else "AI ACTIVE"
                                        },
                                        color = gateColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when {
                                    tradingGate.killSwitchEngaged -> if (isPersian)
                                        "هوش مصنوعی معاملات زنده را با قفل اضطراری متوقف کرده است. برای ادامه، قفل را غیرفعال کنید — سقف زیان روزانه ${tradingGate.maxDailyDrawdownLimitUsd.toInt()} دلار و Kill Switch برای محافظت فعال می‌مانند. همهٔ تحلیل‌های LIT و بک‌تست‌ها همچنان برای یافتن بهترین ورود (RR≥۱:۳) در دسترس‌اند."
                                    else "AI live trading is paused by the Kill Switch. Disengage it to resume — daily loss cap \$${tradingGate.maxDailyDrawdownLimitUsd.toInt()} and Kill Switch stay armed. All LIT analysis & backtests keep finding the best entry (RR≥1:3)."
                                    tradingGate.liveTradingBlocked -> if (isPersian)
                                        "در حالت فعلی «شبیه‌سازی کاغذی»، هوش سایویس بازار را لحظه‌ای می‌خواند، بک‌تست می‌گیرد و بهترین معامله با بیشترین بازدهی (RR≥۱:۳) را پیدا می‌کند — بدون ارسال سفارش واقعی. برای ورود زنده: در «درگاه معاملاتی» سطح را به «اجرای زنده روی حساب واقعی» تغییر دهید و با هوش بک‌تست‌شده وارد شوید."
                                    else "In paper mode, SAYVIS AI reads live markets, backtests and finds the trade with the highest yield (RR≥1:3) — without routing a real order. To go live: set the level to “Live on REAL account” in the Trading Gateway and enter with the backtested AI."
                                    else -> if (isPersian)
                                        "✅ معاملات زندهٔ هوشمند فعال است: هوش سایویس از آموزش‌ها و بک‌تست‌ها بهترین نقطهٔ ورود با بیشترین بازدهی و RR≥۱:۳ را پیدا کرده و با تأیید شما مستقیم وارد می‌شود. Kill Switch و سقف زیان روزانه ${tradingGate.maxDailyDrawdownLimitUsd.toInt()} دلار همواره محافظ سرمایه‌اند."
                                    else "✅ Smart live trading is ACTIVE: SAYVIS AI — trained on your lessons & backtests — finds the highest-yield entry with RR≥1:3 and enters on your confirmation. Kill Switch & daily cap \$${tradingGate.maxDailyDrawdownLimitUsd.toInt()} guard capital at all times."
                                },
                                fontSize = 11.sp,
                                color = SayvisSilverMuted
                            )
                            if (!tradingGate.killSwitchEngaged) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPersian) "محافظت: حد ضررِ ساختاری + سقف لات + بستن خودکار در سقف زیان + ممیزی کامل هر سفارش زنده"
                                        else "Guards: structural stop + lot cap + auto-flatten on drawdown + full audit of every live order",
                                        fontSize = 10.sp,
                                        color = SayvisCyan
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = if (isPersian) "سیگنال‌های تحلیلی ساختار نقدینگی (LIT Signals)" else "LIT Liquidity Inversion Analysis Signals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                items(litSignals, key = { it.id }) { sig ->
                    LitSignalCard(signal = sig, isPersian = isPersian)
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
fun LifeScenarioCard(scenario: LifeScenario, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, SayvisBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersian) scenario.domain.labelFa else scenario.domain.labelEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SayvisGold
                )
                Text(
                    text = "${(scenario.confidenceScore * 100).toInt()}% ${if (isPersian) "قابلیت تحقق" else "Model Confidence"}",
                    fontSize = 11.sp,
                    color = SayvisCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row {
                Text(
                    text = if (isPersian) "فرضیه تصمیم: " else "Hypothesis: ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                SayvisText(
                    source = scenario.decisionHypothesis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    markTranslated = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row {
                Text(
                    text = if (isPersian) "مسیر پیش‌بینی‌شده: " else "Projected Trajectory: ",
                    fontSize = 12.sp,
                    color = SayvisSilverMuted
                )
                SayvisText(
                    source = scenario.projectedTrajectory,
                    fontSize = 12.sp,
                    color = SayvisSilverMuted,
                    markTranslated = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (isPersian) "ریسک‌های شناسایی‌شده:" else "Detected Risks:", fontSize = 10.sp, color = SayvisAmberWarning, fontWeight = FontWeight.Bold)
                    scenario.detectedRisks.forEach { r ->
                        Row {
                            Text(text = "• ", fontSize = 10.sp, color = SayvisSilverMuted)
                            SayvisText(source = r, fontSize = 10.sp, color = SayvisSilverMuted, markTranslated = true)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (isPersian) "فرصت‌های رشد:" else "Opportunities:", fontSize = 10.sp, color = SayvisGreenSuccess, fontWeight = FontWeight.Bold)
                    scenario.projectedOpportunities.forEach { o ->
                        Row {
                            Text(text = "• ", fontSize = 10.sp, color = SayvisSilverMuted)
                            SayvisText(source = o, fontSize = 10.sp, color = SayvisSilverMuted, markTranslated = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LitSignalCard(signal: LitAnalysisSignal, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, SayvisBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CandlestickChart, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    SayvisText(
                        source = "${signal.assetSymbol} (${signal.timeframe})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(SayvisCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPersian) signal.signalType.labelFa else signal.signalType.labelEn,
                        fontSize = 10.sp,
                        color = SayvisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = if (isPersian) "ورود فرضی" else "Entry", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = "$${signal.entryPrice}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
                Column {
                    Text(text = if (isPersian) "حد ابطال" else "Stop", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = "$${signal.invalidationStop}", fontWeight = FontWeight.Bold, color = SayvisRedAlert, fontSize = 12.sp)
                }
                Column {
                    Text(text = if (isPersian) "هدف نقدینگی" else "Target", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = "$${signal.takeProfitTarget}", fontWeight = FontWeight.Bold, color = SayvisGreenSuccess, fontSize = 12.sp)
                }
                Column {
                    Text(text = "R:R", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = "1:${signal.riskRewardRatio}", fontWeight = FontWeight.Bold, color = SayvisGold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SayvisText(
                source = signal.notes,
                fontSize = 11.sp,
                color = SayvisSilverMuted,
                markTranslated = true
            )
        }
    }
}
