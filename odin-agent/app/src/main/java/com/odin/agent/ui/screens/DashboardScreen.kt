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
            .background(
                Brush.verticalGradient(
                    colors = listOf(OdinDeepSpace, Color(0xFF0F172A), OdinDeepSpace)
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header - Based on image: "قدرت واقعی در ترید مالی با ODIN"
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    OdinGold.copy(alpha = 0.15f),
                                    OdinDeepSpace,
                                    OdinCyan.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ODIN",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = OdinGoldLight,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPersian) "قدرت واقعی در ترید مالی با" else "Real Power in Trading with",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian) "استراتژی‌های پیشرفته و تست شده برای سودسازی مستمر" else "Advanced Tested Strategies for Consistent Profit",
                            fontSize = 12.sp,
                            color = OdinSilverMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(OdinGold.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPersian) "🐕 پیتبول ODIN - زنجیر طلا - نماد قدرت" else "🐕 ODIN Pitbull - Gold Chain - Power Symbol",
                                fontSize = 10.sp,
                                color = OdinGoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Performance Chart Card - Like image PERFORMANCE section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurface),
                border = BorderStroke(1.dp, OdinBorderGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PERFORMANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = OdinSilver,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OdinGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Ichimoku + R. Channels", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mock chart area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OdinDeepSpace)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "BTC/USDT 1D", fontSize = 9.sp, color = OdinSilverMuted)
                                Text(text = "+2.39%", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simplified candle representation
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                repeat(15) { i ->
                                    val height = (20 + (i * 3) % 40).dp
                                    val isGreen = i % 3 != 0
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(height)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isGreen) OdinGreen else OdinRed)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(OdinGold.copy(alpha = 0.3f), OdinGoldDark.copy(alpha = 0.2f)))
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "AUTO-TRADE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OdinSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "STRATEGY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinSilver)
                            }
                        }
                    }
                }
            }
        }

        // Portfolio Value + Pitbull Center - Like image
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.5.dp, OdinGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    OdinGold.copy(alpha = 0.25f),
                                    OdinGoldDark.copy(alpha = 0.15f),
                                    OdinSurface
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PORTFOLIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OdinSilverMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "VALUE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OdinSilverMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$${riskStatus.currentCapital.toInt()}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OdinGoldLight
                                )
                                Text(
                                    text = "${if (riskStatus.totalPnl >= 0) "+" else ""}${riskStatus.totalPnlPercent.toInt()}% ${if (isPersian) "سود" else "Profit"}",
                                    fontSize = 11.sp,
                                    color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Pitbull representation with ODIN chain
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(40.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(OdinSurfaceVariant, OdinDeepSpace)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🐕", fontSize = 36.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(OdinGold)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(text = "ODIN", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                VerifiedBadge(text = if (isPersian) "داده تایید شده" else "DATA VERIFIED")
                                Spacer(modifier = Modifier.height(20.dp))
                                VerifiedBadge(text = if (isPersian) "داده تایید شده" else "DATA VERIFIED")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerifiedBadge(text = if (isPersian) "داده تایید شده" else "DATA VERIFIED")
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isPersian) "به زودی" else "COMING SOON",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OdinGoldLight
                                )
                                Text(
                                    text = "COMING SOON",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OdinSilverMuted,
                                    letterSpacing = 1.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OdinGreen.copy(alpha = 0.2f))
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Risk Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurface),
                border = BorderStroke(1.dp, if (riskStatus.killSwitchActive) OdinRed else OdinBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "وضعیت ریسک - بقا > سود رویایی" else "Risk Status - Survival > Dream Profit",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 12.sp
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (riskStatus.isSafeToTrade()) OdinGreen else OdinRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        RiskItem(label = if (isPersian) "سرمایه" else "Capital", value = "$${riskStatus.currentCapital.toInt()}", color = Color.White)
                        RiskItem(label = if (isPersian) "سود کل" else "Total PnL", value = "${riskStatus.totalPnlPercent.toInt()}%", color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed)
                        RiskItem(label = if (isPersian) "DD کل" else "Total DD", value = "${riskStatus.totalDrawdown.toInt()}%", color = OdinAmber)
                    }
                }
            }
        }

        // Quick Actions - 4 main including new ones
        item {
            Text(
                text = if (isPersian) "اقدامات سریع - دوزبانه + بک‌تست دائمی + آلارم" else "Quick Actions - Bilingual + Cont Backtest + Alarm",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionCard(
                    title = if (isPersian) "چارت زنده" else "Live Chart",
                    subtitle = "100ms μs",
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
                    title = if (isPersian) "اسکنر آلارم" else "Scanner Alarm",
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
                    subtitle = "TV 20+ | RR 1:2",
                    icon = Icons.Default.Radar,
                    tint = OdinCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPaperTrade
                )
                ActionCard(
                    title = if (isPersian) "استراتژی‌ها" else "Strategies",
                    subtitle = "7 استراتژی",
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

        // New Rule: 10$ -> 15$ else banned
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = OdinRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "قانون جدید: 10$ → زیر 15$ بعد 5 تست = ممنوع 🚫" else "NEW Rule: $10 → <$15 after 5 tests = BANNED 🚫",
                            fontWeight = FontWeight.Black,
                            color = OdinRed,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ تمام استراتژی‌ها دائم بک‌تست برای قدرت\n✅ هر استراتژی 5 بار با 10 دلار تست\n✅ اگر میانگین یا کمترین زیر 15$ → ممنوع\n✅ WR تست‌ها زیر 60% → ممنوع\n✅ پایداری و امتیاز قدرت لحظه‌ای"
                        else
                            "✅ All strategies continuous backtest for power\n✅ Each strategy 5 times with $10\n✅ If avg or min < $15 → BANNED\n✅ Test WR <60% → BANNED\n✅ Stability & power score live",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Scanner + Auto Trade
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationImportant, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "اسکنر + آلارم تک بوق + ترید اتومات" else "Scanner + Single Beep Alarm + Auto Trade",
                            fontWeight = FontWeight.Bold,
                            color = OdinCyan,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ جستجوی دائمی نقطه ورود مناسب\n✅ هر موقع پیدا کرد تک بوق صوتی 🔊\n✅ اگر ترید اتومات روشن: بر اساس تعداد مجاز ترید می‌کند\n✅ تنظیم: حداکثر ترید باز + روزانه + حداقل اعتماد 80%"
                        else
                            "✅ Continuous search for suitable entry\n✅ Single beep audio alarm when found 🔊\n✅ If auto trade ON: trades based on allowed count\n✅ Config: Max open + daily + min conf 80%",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // New TV 80% Feature
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurface),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "جدید: فیلتر 80% WR + تمام اندیکاتورهای TradingView" else "NEW: 80% WR Filter + All TradingView Indicators",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ 20+ اندیکاتور TV + LIT + RR 1:2 + Confluence 5 + WR 80% وگرنه BLOCKED\n✅ Breakeven RR 1:2 فقط 33% WR لازم - پس 80% طلایی"
                        else
                            "✅ 20+ TV indicators + LIT + RR 1:2 + Confluence 5 + WR 80% else BLOCKED\n✅ Breakeven RR 1:2 needs only 33% WR - so 80% is golden",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Gmail + News Access Card - NEW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurface),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "دسترسی جیمیل + اخبار + اینترنت" else "Gmail + News + Internet Access",
                            fontWeight = FontWeight.Bold,
                            color = OdinCyan,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ لاگین گوگل امن (Firebase Auth)\n✅ دسترسی جیمیل برای بررسی اخبار بازار\n✅ اینترنت برای دیتای زنده + تحلیل\n✅ اعلان‌های هوشمند ترید"
                        else
                            "✅ Secure Google Login (Firebase Auth)\n✅ Gmail access for market news\n✅ Internet for live data + analysis\n✅ Smart trading notifications",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigateToGmailNews?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "اتصال جیمیل و بررسی اخبار" else "Connect Gmail & Check News",
                            fontSize = 11.sp,
                            color = OdinCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom 4 Cards - Like image: XAU, Global Currency, IRR Exchange, Multi-Wallet
        item {
            Text(
                text = if (isPersian) "ویژگی‌های یکپارچه - تم طلایی" else "Integrated Features - Gold Theme",
                fontWeight = FontWeight.Bold,
                color = OdinGoldLight,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldFeatureCard(
                    icon = Icons.Default.Star,
                    title = "XAU",
                    subtitle = if (isPersian) "قیمت طلا\n(13.0 USD)" else "GOLD PRICE\n(13.0 USD)",
                    desc = if (isPersian) "ترید پرحجم" else "HIGH-VOLUME",
                    modifier = Modifier.weight(1f)
                )
                GoldFeatureCard(
                    icon = Icons.Default.AttachMoney,
                    title = "$",
                    subtitle = if (isPersian) "جفت ارز جهانی" else "GLOBAL CURRENCY",
                    desc = "KEY RATE = 0.0000",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldFeatureCard(
                    icon = Icons.Default.Language,
                    title = "﷼",
                    subtitle = if (isPersian) "نرخ ریال ایران" else "IRANIAN RIAL",
                    desc = "1 USD = ... 30 IRR",
                    modifier = Modifier.weight(1f)
                )
                GoldFeatureCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "WALLET",
                    subtitle = if (isPersian) "کیف پول جهانی" else "GLOBAL MULTI-WALLET",
                    desc = if (isPersian) "ذخیره امن چندزنجیره" else "MULTI-CHAIN SECURE",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subscribe Now - Like image
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.5.dp, OdinGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(OdinGoldDark, OdinGold, OdinGoldLight)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isPersian) "به زودی" else "COMING SOON",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text(
                            text = "SUBSCRIBE NOW",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isPersian) "نسخه نهایی یکپارچه - تم پیتبول طلایی ODIN" else "Final Integrated Version - ODIN Pitbull Gold Theme",
                            fontSize = 10.sp,
                            color = Color.Black.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isPersian) "اودین ایجنت v1.0.9-final-gold - بقا > سود رویایی - دوزبانه کامل - جیمیل + اینترنت" else "ODIN AGENT v1.0.9-final-gold - Survival > Dream Profit - Fully Bilingual - Gmail + Internet",
                fontSize = 9.sp,
                color = OdinSilverMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VerifiedBadge(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OdinGold.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OdinGold, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = text, fontSize = 7.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RiskItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
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
        colors = CardDefaults.cardColors(containerColor = OdinSurface),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(text = subtitle, fontSize = 9.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            OdinGold.copy(alpha = 0.2f),
                            OdinGoldDark.copy(alpha = 0.1f),
                            OdinSurface
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, lineHeight = 9.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, fontSize = 6.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
            }
        }
    }
}
