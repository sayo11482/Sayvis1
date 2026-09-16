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
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "تنظیمات اودین" else "Odin Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Google Auth Card - NEW
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, if (currentUser != null) OdinGreen.copy(alpha = 0.3f) else OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isPersian) "احراز هویت گوگل" else "Google Authentication", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser != null) {
                        // Logged in
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(OdinGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.displayName?.firstOrNull()?.toString() ?: currentUser?.email?.firstOrNull()?.toString() ?: "U",
                                        fontWeight = FontWeight.Bold,
                                        color = OdinGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = currentUser?.displayName ?: "Google User", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(text = currentUser?.email ?: "", fontSize = 11.sp, color = OdinSilverMuted)
                                    Text(text = "✓ ${if (isPersian) "تایید شده" else "Verified"} • google.com", fontSize = 10.sp, color = OdinGreen)
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
                        Text(
                            text = if (isPersian) "✅ ورود امن انجام شد - ترید واقعی فعال است" else "✅ Secure login - Live trading enabled",
                            fontSize = 11.sp,
                            color = OdinGreen
                        )
                    } else {
                        // Not logged in
                        Text(
                            text = if (isPersian) "برای ترید واقعی و همگام‌سازی، با گوگل وارد شوید" else "Sign in with Google for live trading & sync",
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
                                Text(text = if (isPersian) "ورود با گوگل" else "Sign in with Google", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OdinBorder)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "ادامه به عنوان مهمان" else "Continue as Guest", fontSize = 12.sp)
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
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isPersian) "اتصال بک‌اند" else "Backend Connection", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = if (isPersian) "آدرس بک‌اند اودین" else "Odin Backend URL", fontSize = 11.sp, color = OdinSilverMuted)

                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = { backendUrl = it },
                        placeholder = { Text("http://192.168.1.100:8000", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OdinCyan,
                            unfocusedBorderColor = OdinBorder,
                            focusedContainerColor = OdinDeepSpace,
                            unfocusedContainerColor = OdinDeepSpace,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPersian)
                            "💡 اگر روی گوشی تست می‌کنی، به جای localhost از IP کامپیوتر استفاده کن (مثلاً 192.168.1.100:8000)"
                        else
                            "💡 If testing from phone, use PC's IP instead of localhost (e.g. 192.168.1.100:8000)",
                        fontSize = 10.sp,
                        color = OdinGold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "تست اتصال" else "Test Connection", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "مدیریت ریسک" else "Risk Management", fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(8.dp))

                    RiskSettingRow(label = if (isPersian) "ریسک هر معامله" else "Risk per trade", value = "1%")
                    RiskSettingRow(label = if (isPersian) "حد ضرر روزانه" else "Daily DD limit", value = "3% → Kill-switch")
                    RiskSettingRow(label = if (isPersian) "حد ضرر کلی" else "Total DD limit", value = "15% → Stop")
                    RiskSettingRow(label = if (isPersian) "حداکثر پوزیشن باز" else "Max open positions", value = "5")
                    RiskSettingRow(label = if (isPersian) "کمیسیون" else "Commission", value = "0.1%")
                    RiskSettingRow(label = if (isPersian) "اسلیپیج" else "Slippage", value = "0.05%")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isPersian) "اطلاع‌رسانی تلگرام" else "Telegram Alerts", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(
                            checked = telegramEnabled,
                            onCheckedChange = { telegramEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OdinCyan, checkedTrackColor = OdinCyan.copy(alpha = 0.3f))
                        )
                    }

                    if (telegramEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Bot Token", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OdinCyan,
                                unfocusedBorderColor = OdinBorder,
                                focusedContainerColor = OdinDeepSpace,
                                unfocusedContainerColor = OdinDeepSpace,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "⚠️ هشدارهای امنیتی" else "⚠️ Security Warnings", fontWeight = FontWeight.Bold, color = OdinRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• هیچ سود تضمینی وجود ندارد\n• فقط با سرمایه قابل از دست دادن ترید کن\n• همیشه پیپر ترید اول (1 ماه)\n• اخبار مهم NFP/CPI رو فیلتر کن\n• Walk-forward برای جلوگیری از overfit\n• مسئولیت ترید با خودت!"
                        else
                            "• No guaranteed profit\n• Only trade with affordable loss\n• Paper trade first (1 month)\n• Filter NFP/CPI news\n• Walk-forward to avoid overfit\n• You are responsible for trading!",
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
                text = "ODIN AGENT v1.0.0-odin-quant\nCompletely independent from SAYVIS\nPackage: com.odin.agent\n\nBuilt with ❤️ for survival > dream profit",
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
private fun RiskSettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinSilver)
    }
}
