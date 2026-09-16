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
import com.odin.agent.models.QuantRiskStatus
import com.odin.agent.models.MarketRegime
import com.odin.agent.ui.theme.*

@Composable
fun DashboardScreen(
    riskStatus: QuantRiskStatus,
    currentRegime: MarketRegime,
    isPersian: Boolean,
    onNavigateToStrategies: () -> Unit,
    onNavigateToBacktest: () -> Unit,
    onNavigateToPaperTrade: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isPersian) "اودین ایجنت" else "ODIN AGENT",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = OdinCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isPersian) "عامل ترید کوانت حرفه‌ای" else "Professional Quant Trading Agent",
                    fontSize = 13.sp,
                    color = OdinGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPersian) "بقا > سود رویایی | 1% ریسک هر معامله" else "Survival > Dream Profit | 1% Risk Per Trade",
                    fontSize = 11.sp,
                    color = OdinSilverMuted
                )
            }
        }

        item {
            // Risk Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, if (riskStatus.killSwitchActive) OdinRed else OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "وضعیت ریسک" else "Risk Status",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (riskStatus.isSafeToTrade()) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (riskStatus.isSafeToTrade()) {
                                    if (isPersian) "امن" else "SAFE"
                                } else {
                                    if (isPersian) "خطر" else "BLOCKED"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (riskStatus.isSafeToTrade()) OdinGreen else OdinRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RiskItem(label = if (isPersian) "سرمایه" else "Capital", value = "$${riskStatus.currentCapital.toInt()}", color = Color.White)
                        RiskItem(label = if (isPersian) "سود کل" else "Total PnL", value = "${riskStatus.totalPnlPercent.toInt()}%", color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed)
                        RiskItem(label = if (isPersian) "DD کل" else "Total DD", value = "${riskStatus.totalDrawdown.toInt()}%", color = OdinAmber)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RiskItem(label = if (isPersian) "سود روزانه" else "Daily PnL", value = "$${riskStatus.dailyPnl.toInt()}", color = if (riskStatus.dailyPnl >=0) OdinGreen else OdinRed)
                        RiskItem(label = if (isPersian) "DD روزانه" else "Daily DD", value = "${riskStatus.dailyDdPercent().toInt()}%", color = if (riskStatus.dailyDrawdown < 2) OdinGreen else OdinRed)
                        RiskItem(label = if (isPersian) "پوزیشن باز" else "Open Pos", value = "${riskStatus.openPositions}", color = OdinSilver)
                    }

                    if (riskStatus.killSwitchActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isPersian) "🔴 Kill-switch فعال! DD روزانه 3% رد شد - معاملات جدید مسدود" else "🔴 Kill-switch active! Daily DD 3% exceeded - New trades blocked",
                            fontSize = 11.sp,
                            color = OdinRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            // Market Regime
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = if (isPersian) "رژیم بازار" else "Market Regime", fontSize = 12.sp, color = OdinSilverMuted)
                        Text(text = currentRegime.label(isPersian), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = when(currentRegime) {
                            MarketRegime.TRENDING -> OdinGreen
                            MarketRegime.RANGING -> OdinGold
                            MarketRegime.HIGH_VOL -> OdinRed
                            else -> OdinSilver
                        })
                        Text(
                            text = when(currentRegime) {
                                MarketRegime.TRENDING -> if (isPersian) "استراتژی پیشنهادی: دنباله‌روی روند" else "Recommended: Trend Following"
                                MarketRegime.RANGING -> if (isPersian) "پیشنهادی: بازگشت به میانگین" else "Recommended: Mean Reversion"
                                MarketRegime.HIGH_VOL -> if (isPersian) "پیشنهادی: شکست مومنتوم" else "Recommended: Momentum Breakout"
                                else -> ""
                            },
                            fontSize = 11.sp,
                            color = OdinSilverMuted
                        )
                    }
                    Icon(
                        imageVector = when(currentRegime) {
                            MarketRegime.TRENDING -> Icons.Default.TrendingUp
                            MarketRegime.RANGING -> Icons.Default.HorizontalRule
                            MarketRegime.HIGH_VOL -> Icons.Default.Bolt
                            else -> Icons.Default.Help
                        },
                        contentDescription = null,
                        tint = OdinCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        item {
            // Quick Actions
            Text(text = if (isPersian) "اقدامات سریع" else "Quick Actions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCard(
                    title = if (isPersian) "مانیتور LIT" else "LIT Monitor",
                    subtitle = "4 نماد",
                    icon = Icons.Default.Radar,
                    tint = OdinCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPaperTrade
                )
                ActionCard(
                    title = if (isPersian) "استراتژی‌ها" else "Strategies",
                    subtitle = "6 استراتژی",
                    icon = Icons.Default.Psychology,
                    tint = OdinGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStrategies
                )
                ActionCard(
                    title = if (isPersian) "بک‌تست" else "Backtest",
                    subtitle = "Sharpe, DD",
                    icon = Icons.Default.Analytics,
                    tint = OdinGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBacktest
                )
            }
        }

        item {
            // $10 Real Test Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "تست 10 دلار واقعی - LIT" else "$10 Real Test - LIT",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian)
                            "سرمایه: 10 دلار (MT5)\nوین ریت LIT: 50-65% (واقعی) با RR 1:2\n4 نماد: BTC, ETH, EURUSD, XAUUSD\nمدیریت: 1% هر ترید, 3% DD روزانه, 15% کل\nآیا سود می‌دهد؟ LIT مطمئن‌ترین است اما تضمینی نیست!\nبقا > سود رویایی\nپیشنهاد: حداقل 100 دلار + 6 ماه پیپر"
                        else
                            "Capital: $10 (MT5)\nLIT Winrate: 50-65% (real) with RR 1:2\n4 Symbols: BTC, ETH, EURUSD, XAUUSD\nRisk: 1% per trade, 3% daily DD, 15% total\nProfitable? LIT most reliable but no guarantee!\nSurvival > Dream Profit\nSuggest: Min $100 + 6 months paper",
                        fontSize = 11.sp,
                        color = OdinSilver,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isPersian) "ورود/خروج منطقی" else "Logical Entry/Exit", fontSize = 10.sp, color = OdinCyan)
                        Text(text = "✓ Order Block + FVG + BOS", fontSize = 10.sp, color = OdinGreen)
                    }
                }
            }
        }

        item {
            // Risk Rules (Non-negotiable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPersian) "⚠️ قوانین ریسک غیرقابل مذاکره" else "⚠️ Non-Negotiable Risk Rules",
                        fontWeight = FontWeight.Bold,
                        color = OdinRed,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RiskRule(text = if (isPersian) "حداکثر 1% ریسک هر معامله" else "Max 1% risk per trade")
                    RiskRule(text = if (isPersian) "حداکثر 3% DD روزانه → Kill-switch" else "Max 3% daily DD → Kill-switch")
                    RiskRule(text = if (isPersian) "حداکثر 15% DD کلی → توقف کامل" else "Max 15% total DD → Full stop")
                    RiskRule(text = if (isPersian) "Position sizing بر اساس ATR" else "ATR-based position sizing")
                    RiskRule(text = if (isPersian) "هیچ سود تضمینی نیست!" else "No guaranteed profit!")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = if (isPersian) "اودین ایجنت v1.0.0-odin-quant - بقا > سود رویایی" else "ODIN AGENT v1.0.0-odin-quant - Survival > Dream Profit",
                fontSize = 10.sp,
                color = OdinSilverMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RiskItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
        border = BorderStroke(1.dp, OdinBorder),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = subtitle, fontSize = 10.sp, color = OdinSilverMuted)
        }
    }
}

@Composable
private fun RiskRule(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "•", color = OdinRed, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 11.sp, color = OdinSilver)
    }
}
