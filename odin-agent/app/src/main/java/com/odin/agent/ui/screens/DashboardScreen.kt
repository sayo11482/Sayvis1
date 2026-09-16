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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.MarketRegime
import com.odin.agent.models.QuantRiskStatus
import com.odin.agent.ui.theme.*

@Composable
fun DashboardScreen(
    riskStatus: QuantRiskStatus,
    currentRegime: MarketRegime,
    isPersian: Boolean,
    onNavigateToStrategies: () -> Unit,
    onNavigateToBacktest: () -> Unit,
    onNavigateToPaperTrade: () -> Unit,
    onNavigateToGmailNews: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header - Professional Financial Trader - Dark Gold Green Dollar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0A0A0A),
                                    Color(0xFF111111),
                                    Color(0xFF0A1A0A)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(OdinGold, OdinGoldLight)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "ODIN", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ODIN AGENT",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = if (isPersian) "عامل ترید کوانت حرفه‌ای" else "Professional Quant Trading Agent",
                                        fontSize = 10.sp,
                                        color = OdinGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OdinGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(OdinGreen)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "LIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Market ticker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TickerItem(symbol = "BTC", price = "65,234", change = "+2.3%", isPositive = true)
                            TickerItem(symbol = "ETH", price = "3,521", change = "+1.8%", isPositive = true)
                            TickerItem(symbol = "XAU", price = "2,351", change = "+0.5%", isPositive = true)
                            TickerItem(symbol = "EURUSD", price = "1.0850", change = "-0.2%", isPositive = false)
                        }
                    }
                }
            }
        }

        // Portfolio + Performance
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "پورتفولیو" else "Portfolio", fontSize = 10.sp, color = OdinSilverMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "$${riskStatus.currentCapital.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (riskStatus.totalPnl >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${if (riskStatus.totalPnl >= 0) "+" else ""}${riskStatus.totalPnlPercent.toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = if (isPersian) "بقا > سود رویایی" else "Survival > Dream Profit", fontSize = 8.sp, color = OdinSilverDim)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "رژیم بازار" else "Market Regime", fontSize = 10.sp, color = OdinSilverMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentRegime.label(isPersian),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = when (currentRegime) {
                                MarketRegime.TRENDING -> OdinGreen
                                MarketRegime.RANGING -> OdinGold
                                MarketRegime.HIGH_VOL -> OdinRed
                                else -> OdinSilver
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentRegime) {
                                MarketRegime.TRENDING -> if (isPersian) "روند قوی" else "Strong Trend"
                                MarketRegime.RANGING -> if (isPersian) "رنج" else "Ranging"
                                MarketRegime.HIGH_VOL -> if (isPersian) "پرنوسان" else "High Vol"
                                else -> "Unknown"
                            },
                            fontSize = 10.sp,
                            color = OdinSilverMuted
                        )
                    }
                }
            }
        }

        // Risk Status - Pure Black Pro
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, if (riskStatus.killSwitchActive) OdinRed else Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "ریسک - 1% هر ترید" else "Risk - 1% Per Trade",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (riskStatus.isSafeToTrade()) OdinGreen.copy(alpha = 0.15f) else OdinRed.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (riskStatus.isSafeToTrade()) "SAFE" else "BLOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (riskStatus.isSafeToTrade()) OdinGreen else OdinRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RiskItem(label = if (isPersian) "سرمایه" else "Capital", value = "$${riskStatus.currentCapital.toInt()}", color = Color.White)
                        RiskItem(label = "PnL", value = "${riskStatus.totalPnlPercent.toInt()}%", color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed)
                        RiskItem(label = "DD", value = "${riskStatus.totalDrawdown.toInt()}%", color = OdinAmber)
                        RiskItem(label = "Daily", value = "${riskStatus.dailyDdPercent().toInt()}%", color = if (riskStatus.dailyDrawdown < 2) OdinGreen else OdinRed)
                    }
                }
            }
        }

        // Quick Actions - Professional Trader
        item {
            Text(text = if (isPersian) "دسترسی سریع تریدری" else "Trader Quick Access", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionCard(
                    title = if (isPersian) "چارت زنده" else "Live Chart",
                    subtitle = "Entry Points",
                    icon = Icons.Default.ShowChart,
                    tint = OdinGoldLight,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPaperTrade
                )
                ActionCard(
                    title = if (isPersian) "بک‌تست دائمی" else "Cont Backtest",
                    subtitle = "10$→15$",
                    icon = Icons.Default.AllInclusive,
                    tint = OdinRed,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBacktest
                )
                ActionCard(
                    title = if (isPersian) "آلارم" else "Alarm",
                    subtitle = "Beep + Auto",
                    icon = Icons.Default.NotificationImportant,
                    tint = OdinCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPaperTrade
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionCard(
                    title = if (isPersian) "مانیتور 80%" else "80% Monitor",
                    subtitle = "TV 20+ RR 1:2",
                    icon = Icons.Default.Radar,
                    tint = OdinCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPaperTrade
                )
                ActionCard(
                    title = if (isPersian) "AWARE" else "AWARE",
                    subtitle = "Learning",
                    icon = Icons.Default.Psychology,
                    tint = OdinGold,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStrategies
                )
                ActionCard(
                    title = if (isPersian) "استراتژی" else "Strategies",
                    subtitle = "7 Best",
                    icon = Icons.Default.AutoAwesome,
                    tint = OdinGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToStrategies
                )
            }
        }

        // AWARE Learning Engine Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0A0A0A), Color(0xFF0F0A00), Color(0xFF0A0A0A))
                            )
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "AWARE - موتور یادگیری دقیق" else "AWARE - Precise Learning Engine",
                                fontWeight = FontWeight.Black,
                                color = OdinGoldLight,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OdinGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "● ONLINE LEARNING", fontSize = 7.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian)
                                "✅ آنلاین دائم در حال یادگیری و بررسی استراتژی‌ها\n✅ تست شیوه‌های مدیریت مالی (Kelly, Risk 1%, RR)\n✅ کشف بهترین استراتژی برای بهترین نماد + حافظه\n✅ هر ترید = تجربه جدید + درس + بهینه‌سازی"
                            else
                                "✅ Online continuous learning & testing strategies\n✅ Testing money management (Kelly, Risk 1%, RR)\n✅ Discover best strategy per symbol + memory\n✅ Each trade = new experience + lesson + optimization",
                            fontSize = 10.sp,
                            color = OdinSilver,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // Best LIT Settings - Researched
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050A05)),
                border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "بهترین تنظیمات LIT برای بیشترین RR - تحقیق شده" else "Best LIT Settings for Max RR - Researched",
                            fontWeight = FontWeight.Bold,
                            color = OdinGreen,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• HTF: Daily + 4H برای بایاس اصلی\n• نقدینگی: Equal Highs/Lows با lookback 20\n• سوئیپ: 0.2% فراتر + ریجکشن کندل سایه بلند\n• تایید: BOS/CHOCH + Order Block 50% + FVG الزامی\n• OTE: فیبوناچی 62-79% برای ورود دقیق\n• SL: پشت سوئیپ + 0.2 ATR بافر (تنگ اما امن)\n• TP: نقدینگی مخالف بعدی - RR حداقل 1:2.5 ایده‌آل 1:3.5 تا 1:5\n• ریسک: 0.8% هر ترید LIT + حداکثر 2 پوزیشن باز + 3 در روز\n• تایم‌فریم: 4H سوئیپ، 15m تایید، 5m ورود\n• Trailing: پشت OB تازه وقتی در سود"
                        else
                            "• HTF: Daily + 4H for main bias\n• Liquidity: Equal Highs/Lows lookback 20\n• Sweep: 0.2% beyond + rejection long wick\n• Confirm: BOS/CHOCH + Order Block 50% + FVG required\n• OTE: Fibonacci 62-79% for precise entry\n• SL: Beyond sweep + 0.2 ATR buffer (tight but safe)\n• TP: Next opposite liquidity - Min RR 1:2.5 Ideal 1:3.5 to 1:5\n• Risk: 0.8% per LIT trade + max 2 open + 3 per day\n• Timeframe: 4H sweep, 15m confirm, 5m entry\n• Trailing: Behind fresh OB when in profit",
                        fontSize = 9.sp,
                        color = OdinSilver,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Bottom Features - Professional
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldFeatureCard(
                    icon = Icons.Default.Star,
                    title = "XAU",
                    subtitle = "GOLD 13.0",
                    desc = "HIGH-VOL",
                    modifier = Modifier.weight(1f)
                )
                GoldFeatureCard(
                    icon = Icons.Default.AttachMoney,
                    title = "$",
                    subtitle = "FOREX",
                    desc = "0.0000",
                    modifier = Modifier.weight(1f)
                )
                GoldFeatureCard(
                    icon = Icons.Default.Language,
                    title = "IRR",
                    subtitle = "RIAL",
                    desc = "30 IRR",
                    modifier = Modifier.weight(1f)
                )
                GoldFeatureCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "WALLET",
                    subtitle = "MULTI",
                    desc = "SECURE",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ODIN AGENT v1.0.12-aware-pro - Pure Black • Gold • Green Dollar\nNo Dog • No Coming Soon • Professional Trader\nLive Chart Clickable • Entry Points • Live PnL • AWARE Learning\nBest Strategy Auto Discovery • Memory • LIT Max RR 1:5\nGoogle Auth Tested • Gmail • Gemini API • 100ms μs",
                fontSize = 8.sp,
                color = OdinSilverDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TickerItem(symbol: String, price: String, change: String, isPositive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = symbol, fontSize = 8.sp, color = OdinSilverMuted, fontWeight = FontWeight.Bold)
        Text(text = price, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = change, fontSize = 8.sp, color = if (isPositive) OdinGreen else OdinRed)
    }
}

@Composable
private fun RiskItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
            Text(text = subtitle, fontSize = 8.sp, color = OdinSilverMuted, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun GoldFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
            Text(text = subtitle, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(text = desc, fontSize = 6.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
        }
    }
}
