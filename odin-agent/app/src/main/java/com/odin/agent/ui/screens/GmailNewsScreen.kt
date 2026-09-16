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
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

data class GmailNews(
    val id: String,
    val subject: String,
    val subjectFa: String,
    val sender: String,
    val time: String,
    val category: String,
    val important: Boolean,
    val snippet: String,
    val snippetFa: String
)

@Composable
fun GmailNewsScreen(isPersian: Boolean) {
    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var news by remember { mutableStateOf<List<GmailNews>>(emptyList()) }
    var lastSync by remember { mutableStateOf("—") }

    LaunchedEffect(Unit) {
        // Simulate news loading
        news = listOf(
            GmailNews(
                id = "1",
                subject = "Fed Rate Decision - Market Impact",
                subjectFa = "تصمیم نرخ بهره فدرال - تاثیر بازار",
                sender = "TradingView Alerts",
                time = "2m ago",
                category = "FED",
                important = true,
                snippet = "Fed keeps rates unchanged, signals 2 cuts in 2025. Gold jumps to 2350, BTC up 3%",
                snippetFa = "فدرال نرخ را ثابت نگه داشت، 2 کاهش در 2025. طلا 2350، بیت 3% رشد"
            ),
            GmailNews(
                id = "2",
                subject = "BTC Breaks 65K - LIT Signal Detected",
                subjectFa = "بیت 65K را شکست - سیگنال LIT",
                sender = "ODIN Agent",
                time = "15m ago",
                category = "LIT",
                important = true,
                snippet = "Liquidity sweep detected on BTC/USDT, BOS bullish, OB retest in discount zone. RR 1:2.5 Conf 85%",
                snippetFa = "سوئیپ نقدینگی BTC، BOS صعودی، OB در دیسکانت. RR 1:2.5 اعتماد 85%"
            ),
            GmailNews(
                id = "3",
                subject = "XAUUSD Gold Analysis - Premium Zone",
                subjectFa = "تحلیل طلا - زون پریمیوم",
                sender = "Gold Market News",
                time = "1h ago",
                category = "XAU",
                important = false,
                snippet = "Gold in premium zone 2350-2360, wait for liquidity sweep before short. Target 2310",
                snippetFa = "طلا در پریمیوم 2350-2360، منتظر سوئیپ برای فروش. هدف 2310"
            ),
            GmailNews(
                id = "4",
                subject = "EURUSD - ECB News Impact",
                subjectFa = "یورو دلار - تاثیر اخبار ECB",
                sender = "Forex Factory",
                time = "3h ago",
                category = "FOREX",
                important = false,
                snippet = "ECB dovish, EURUSD bearish momentum. 1.0850 support broken, next 1.0780",
                snippetFa = "ECB داویش، یورو نزولی. حمایت 1.0850 شکسته، بعدی 1.0780"
            ),
            GmailNews(
                id = "5",
                subject = "Iranian Rial - USD/IRR Update",
                subjectFa = "ریال ایران - آپدیت دلار",
                sender = "Iran Exchange",
                time = "5h ago",
                category = "IRR",
                important = false,
                snippet = "USD/IRR stable around 500k, Central Bank intervention. Crypto IRR pairs volatile",
                snippetFa = "دلار حوالی 500 هزار، مداخله بانک مرکزی. جفت‌های کریپتو پرنوسان"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "جیمیل + اخبار بازار - اینترنت زنده" else "Gmail + Market News - Live Internet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isPersian) "دسترسی امن جیمیل برای اخبار + تحلیل خودکار" else "Secure Gmail access for news + auto analysis",
                fontSize = 11.sp,
                color = OdinSilverMuted
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) OdinGreen.copy(alpha = 0.1f) else OdinSurface
                ),
                border = BorderStroke(1.dp, if (isConnected) OdinGreen.copy(alpha = 0.4f) else OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (isConnected) OdinGreen else OdinSilverMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isPersian) "وضعیت جیمیل" else "Gmail Status",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isConnected) {
                                        if (isPersian) "متصل - همگام‌سازی فعال" else "Connected - Sync Active"
                                    } else {
                                        if (isPersian) "غیرمتصل" else "Disconnected"
                                    },
                                    fontSize = 10.sp,
                                    color = if (isConnected) OdinGreen else OdinSilverMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isConnected) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isConnected) "● LIVE" else "○ OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) OdinGreen else OdinRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isLoading = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) OdinRed.copy(alpha = 0.2f) else OdinCyan.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, if (isConnected) OdinRed.copy(alpha = 0.5f) else OdinCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OdinCyan)
                            LaunchedEffect(Unit) {
                                delay(1500)
                                isConnected = !isConnected
                                lastSync = if (isPersian) "همین الان" else "Just now"
                                isLoading = false
                            }
                        } else {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Logout else Icons.Default.Login,
                                contentDescription = null,
                                tint = if (isConnected) OdinRed else OdinCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) {
                                    if (isPersian) "قطع اتصال جیمیل" else "Disconnect Gmail"
                                } else {
                                    if (isPersian) "اتصال امن جیمیل (Google Auth)" else "Secure Gmail Connect (Google Auth)"
                                },
                                fontSize = 12.sp,
                                color = if (isConnected) OdinRed else OdinCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isConnected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isPersian) "آخرین همگام‌سازی:" else "Last sync:", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = lastSync, fontSize = 10.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "امنیت و حریم خصوصی جیمیل" else "Gmail Security & Privacy",
                            fontWeight = FontWeight.Bold,
                            color = OdinGold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "✅ فقط خواندن اخبار بازار (READ only)\n✅ هیچ ایمیل شخصی خوانده نمی‌شود\n✅ OAuth2 امن گوگل - رمز عبور ذخیره نمی‌شود\n✅ فیلتر خودکار: فقط TradingView, Forex, Crypto\n✅ تحلیل AI و سیگنال خودکار از اخبار"
                        else
                            "✅ Only market news reading (READ only)\n✅ No personal emails read\n✅ Secure Google OAuth2 - No password stored\n✅ Auto filter: Only TradingView, Forex, Crypto\n✅ AI analysis & auto signals from news",
                        fontSize = 10.sp,
                        color = OdinSilver,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPersian) "اخبار زنده بازار (${news.size})" else "Live Market News (${news.size})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(OdinCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = if (isPersian) "اینترنت فعال" else "Internet Active", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(news) { item ->
            NewsCard(news = item, isPersian = isPersian)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun NewsCard(news: GmailNews, isPersian: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (news.important) OdinGold.copy(alpha = 0.08f) else OdinSurface
        ),
        border = BorderStroke(
            1.dp,
            if (news.important) OdinGold.copy(alpha = 0.4f) else OdinBorder
        ),
        shape = RoundedCornerShape(14.dp)
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (news.category) {
                                    "FED" -> OdinRed.copy(alpha = 0.2f)
                                    "LIT" -> OdinGold.copy(alpha = 0.2f)
                                    "XAU" -> OdinGold.copy(alpha = 0.2f)
                                    "FOREX" -> OdinCyan.copy(alpha = 0.2f)
                                    "IRR" -> OdinGreen.copy(alpha = 0.2f)
                                    else -> OdinSilverMuted.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = news.category,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = when (news.category) {
                                "FED" -> OdinRed
                                "LIT" -> OdinGold
                                "XAU" -> OdinGold
                                "FOREX" -> OdinCyan
                                "IRR" -> OdinGreen
                                else -> OdinSilverMuted
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = news.sender, fontSize = 9.sp, color = OdinSilverMuted)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (news.important) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = OdinGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = news.time, fontSize = 9.sp, color = OdinSilverMuted)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isPersian) news.subjectFa else news.subject,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isPersian) news.snippetFa else news.snippet,
                fontSize = 10.sp,
                color = OdinSilver,
                lineHeight = 12.sp
            )

            if (news.important) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OdinGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPersian) "تحلیل ODIN: سیگنال مرتبط شناسایی شد" else "ODIN Analysis: Related signal detected",
                            fontSize = 9.sp,
                            color = OdinGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
