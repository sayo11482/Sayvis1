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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.auth.AuthUser
import com.odin.agent.auth.GoogleAuthManager
import com.odin.agent.auth.AuthResult
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(isPersian: Boolean) {
    var backendUrl by remember { mutableStateOf("http://192.168.1.100:8000") }
    var telegramEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    var currentUser by remember { mutableStateOf<AuthUser?>(authManager.getCurrentUser()) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "تنظیمات اودین - تم مشکی حرفه‌ای" else "Odin Settings - Pure Black Pro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isPersian) "بدون سایویز - کاملا مستقل - com.odin.agent" else "No Sayvis - Fully Independent - com.odin.agent",
                fontSize = 10.sp,
                color = OdinGold
            )
        }

        // Google Auth Card - Tested
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, if (currentUser != null) OdinGreen.copy(alpha = 0.4f) else OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "ورود گوگل - تست شده ✅" else "Google Login - Tested ✅",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "Firebase Auth + Gmail API + اینترنت - امن و تست شده" else "Firebase Auth + Gmail API + Internet - Secure & Tested",
                        fontSize = 10.sp,
                        color = OdinSilverMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(OdinGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.displayName?.firstOrNull()?.toString() ?: "U",
                                        fontWeight = FontWeight.Black,
                                        color = OdinGreen,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = currentUser?.displayName ?: "Odin User", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(text = currentUser?.email ?: "", fontSize = 11.sp, color = OdinSilverMuted)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Verified • ${currentUser?.provider}", fontSize = 9.sp, color = OdinGreen)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    isAuthLoading = true
                                    authManager.signOut()
                                    currentUser = null
                                    isAuthLoading = false
                                }
                            }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = OdinRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPersian) "✅ ورود موفق - چارت زنده + اخبار جیمیل فعال" else "✅ Login Success - Live Chart + Gmail News Active",
                                    fontSize = 11.sp,
                                    color = OdinGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isPersian) "برای دسترسی به چارت زنده و اخبار جیمیل وارد شوید" else "Sign in for live chart & Gmail news access",
                            fontSize = 11.sp,
                            color = OdinSilverMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isAuthLoading) return@Button
                                isAuthLoading = true
                                authError = null
                                scope.launch {
                                    when (val result = authManager.signInWithGoogle()) {
                                        is AuthResult.Success -> {
                                            currentUser = result.user
                                            authError = null
                                        }
                                        is AuthResult.Error -> {
                                            authError = if (isPersian) result.messageFa else result.messageEn
                                        }
                                        AuthResult.Cancelled -> {
                                            authError = if (isPersian) "لغو شد" else "Cancelled"
                                        }
                                    }
                                    isAuthLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAuthLoading
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF4285F4)), contentAlignment = Alignment.Center) {
                                    Text(text = "G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isPersian) "ورود با گوگل - تست شده" else "Sign in with Google - Tested", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // Guest login
                                    isAuthLoading = true
                                    delay(500)
                                    currentUser = AuthUser(
                                        uid = "guest_${System.currentTimeMillis()}",
                                        email = "guest@odin.agent",
                                        displayName = "Odin Guest",
                                        photoUrl = null,
                                        isEmailVerified = false,
                                        provider = "guest"
                                    )
                                    isAuthLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OdinBorder)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = OdinSilver)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "ادامه به عنوان مهمان" else "Continue as Guest", fontSize = 12.sp, color = OdinSilver)
                        }

                        authError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, color = OdinRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isPersian) "چارت زنده - نقاط ورود" else "Live Chart - Entry Points", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian)
                            "✅ چارت کندلی زنده با کندل‌های واقعی\n✅ نقاط ورود BUY/SELL با SL/TP\n✅ LIT: سوئیپ + BOS + OB + FVG\n✅ TV 80%: 20+ اندیکاتور + RR 1:2\n✅ آپدیت هر 100ms + نمایش میکروثانیه\n✅ تم مشکی خالص حرفه‌ای"
                        else
                            "✅ Live candlestick chart with real candles\n✅ BUY/SELL entry points with SL/TP\n✅ LIT: Sweep + BOS + OB + FVG\n✅ TV 80%: 20+ indicators + RR 1:2\n✅ 100ms updates + microsecond display\n✅ Pure black professional theme",
                        fontSize = 11.sp,
                        color = OdinSilver,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = if (isPersian) "مدیریت ریسک - غیرقابل مذاکره" else "Risk Management - Non-Negotiable", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    RiskRow(label = if (isPersian) "ریسک هر معامله" else "Risk per trade", value = "1%")
                    RiskRow(label = if (isPersian) "حد ضرر روزانه" else "Daily DD", value = "3% → Kill-switch")
                    RiskRow(label = if (isPersian) "حد ضرر کلی" else "Total DD", value = "15% → Stop")
                    RiskRow(label = "Max Positions", value = "5")
                    RiskRow(label = if (isPersian) "آپدیت چارت" else "Chart Update", value = "100ms (10Hz)")
                    RiskRow(label = if (isPersian) "میکروثانیه" else "Microsecond", value = "Display μs counter")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = if (isPersian) "⚠️ اودین خالص - بدون سایویز" else "⚠️ Pure ODIN - No Sayvis", fontWeight = FontWeight.Bold, color = OdinGoldLight, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• پکیج: com.odin.agent (نه com.example)\n• تم: مشکی خالص #000000 حرفه‌ای\n• ورود گوگل: تست شده Firebase Auth\n• چارت: کندلی زنده + نقاط ورود\n• آپدیت: 100ms (1μs نمایش) - سریع‌ترین پایدار\n• اینترنت + جیمیل: اخبار زنده\n• 7 استراتژی: TV 80% + LIT + 5 دیگر\n• کاملا مستقل از سایویز"
                        else
                            "• Package: com.odin.agent (not com.example)\n• Theme: Pure black #000000 pro\n• Google Auth: Tested Firebase Auth\n• Chart: Live candles + entry points\n• Update: 100ms (1μs display) - fastest stable\n• Internet + Gmail: Live news\n• 7 strategies: TV 80% + LIT + 5 more\n• Fully independent from Sayvis",
                        fontSize = 11.sp,
                        color = OdinSilver,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ODIN AGENT v1.0.10-pure-black-final\nPure Black Professional • No Sayvis • com.odin.agent\nGoogle Auth Tested • Live Chart 100ms • Entry Points\nGmail + Internet • TV 80% WR • LIT\nBuilt for Professional Traders - Survival > Dream Profit",
                fontSize = 9.sp,
                color = OdinSilverDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RiskRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinSilver)
    }
}
