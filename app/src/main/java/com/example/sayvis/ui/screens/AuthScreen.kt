package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.auth.AuthResult
import com.example.sayvis.auth.AuthUser
import com.example.sayvis.auth.GoogleAuthManager
import com.example.sayvis.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    isPersian: Boolean,
    onAuthSuccess: (AuthUser) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(authManager.getCurrentUser()) }

    // Check if already logged in
    LaunchedEffect(Unit) {
        currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            onAuthSuccess(currentUser!!)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SayvisSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SayvisCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SayvisCyan,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isPersian) "اودین ایجنت" else "ODIN AGENT",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Text(
            text = if (isPersian) "عامل ترید کوانت حرفه‌ای" else "Professional Quant Trading Agent",
            fontSize = 14.sp,
            color = SayvisSilverMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isPersian) "ورود امن با حساب گوگل" else "Secure login with Google account",
            fontSize = 12.sp,
            color = SayvisGold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Current user card if logged in
        currentUser?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
                border = BorderStroke(1.dp, SayvisGreenSuccess.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SayvisGreenSuccess.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.displayName?.firstOrNull()?.toString() ?: user.email?.firstOrNull()?.toString() ?: "U",
                            fontWeight = FontWeight.Bold,
                            color = SayvisGreenSuccess,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.displayName ?: if (isPersian) "کاربر گوگل" else "Google User",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = user.email ?: "",
                            fontSize = 12.sp,
                            color = SayvisSilverMuted
                        )
                        Text(
                            text = if (user.isEmailVerified) "✓ ${if (isPersian) "تایید شده" else "Verified"}" else "",
                            fontSize = 10.sp,
                            color = SayvisGreenSuccess
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Google Sign-In Button
        Button(
            onClick = {
                if (isLoading) return@Button
                isLoading = true
                errorMessage = null
                scope.launch {
                    when (val result = authManager.signInWithGoogle()) {
                        is AuthResult.Success -> {
                            currentUser = result.user
                            isLoading = false
                            onAuthSuccess(result.user)
                        }
                        is AuthResult.Error -> {
                            errorMessage = if (isPersian) result.messageFa else result.messageEn
                            isLoading = false
                        }
                        AuthResult.Cancelled -> {
                            errorMessage = if (isPersian) "ورود لغو شد" else "Sign-in cancelled"
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Google G icon placeholder
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isPersian) "ورود با حساب گوگل" else "Sign in with Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Guest continue
        OutlinedButton(
            onClick = onContinueAsGuest,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SayvisSilver
            ),
            border = BorderStroke(1.dp, SayvisBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isPersian) "ادامه به عنوان مهمان" else "Continue as Guest")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        errorMessage?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SayvisRedAlert.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, SayvisRedAlert.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SayvisRedAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, color = SayvisRedAlert, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info about security
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, SayvisBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "امنیت و احراز هویت" else "Security & Authentication",
                        fontWeight = FontWeight.Bold,
                        color = SayvisCyan,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isPersian)
                        "• ورود با گوگل امن و رمزنگاری شده است\n• اطلاعات شما فقط روی دستگاه ذخیره می‌شود\n• می‌توانید همیشه به عنوان مهمان ادامه دهید\n• برای ترید واقعی، احراز هویت الزامی است"
                    else
                        "• Google Sign-In is secure & encrypted\n• Your data stays only on device\n• You can always continue as guest\n• Authentication required for live trading",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ODIN info
        Text(
            text = if (isPersian) "بقا > سود رویایی" else "Survival > Dream Profit",
            fontSize = 10.sp,
            color = SayvisGold.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthStatusCard(
    user: AuthUser?,
    isPersian: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user != null) SayvisSurfaceVariant else SayvisSurfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (user != null) SayvisGreenSuccess.copy(alpha = 0.3f) else SayvisBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (user != null) Icons.Default.VerifiedUser else Icons.Default.PersonOff,
                    contentDescription = null,
                    tint = if (user != null) SayvisGreenSuccess else SayvisSilverMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (user != null) {
                            user.displayName ?: user.email ?: if (isPersian) "کاربر" else "User"
                        } else {
                            if (isPersian) "مهمان - بدون ورود" else "Guest - Not signed in"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (user != null) {
                            "${user.email} • ${user.provider}"
                        } else {
                            if (isPersian) "برای ترید واقعی وارد شوید" else "Sign in for live trading"
                        },
                        fontSize = 10.sp,
                        color = SayvisSilverMuted
                    )
                }
            }

            if (user != null) {
                IconButton(onClick = onSignOut) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = SayvisRedAlert, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
