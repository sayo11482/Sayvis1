package com.example.sayvis.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.ChatMessage
import com.example.sayvis.ui.components.SayvisAvatar
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * Modernized text assistant conversation screen.
 *
 * Typography has been upgraded for sleek readability:
 * - Refined font sizing, line heights and letter spacing.
 * - Distinct, elevated speech bubbles with subtle cybernetic borders.
 * - In-line thinking bubble right after user message tagged with Code 01 ([کد ۰۱]).
 * - Clear diagnostic message formatting for reasoning and error states.
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    avatarState: AvatarState,
    isPersian: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var isVoiceListening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Smooth pulse animation for thinking status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen")
    ) {
        // Chat Header with Avatar Pulse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SayvisSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SayvisAvatar(state = avatarState, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isPersian) "هوش مصنوعی و دستیار سایویس" else "SAYVIS Autonomous Agent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp,
                    color = Color.White
                )
                Text(
                    text = if (avatarState == AvatarState.THINKING) {
                        if (isPersian) "[کد ۰۱] در حال پردازش شناختی و تفکر عمیق..." else "[Code 01] Reasoning over UIC context..."
                    } else {
                        if (isPersian) "آنلاین • آمادهٔ پاسخگویی و اجرای فرامین" else "Online • Multi-Provider Orchestrator"
                    },
                    fontSize = 11.5.sp,
                    color = if (avatarState == AvatarState.THINKING) SayvisGold else SayvisCyan
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { msg ->
                val isOwner = msg.sender == "OWNER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOwner) Arrangement.End else Arrangement.Start
                ) {
                    if (!isOwner) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (msg.isThinking) SayvisGold.copy(alpha = 0.2f) else SayvisCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (msg.isThinking) Icons.Default.Psychology else Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (msg.isThinking) SayvisGold else SayvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        horizontalAlignment = if (isOwner) Alignment.End else Alignment.Start
                    ) {
                        if (msg.isThinking) {
                            // In-line thinking bubble right after user message tagged with Code 01
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SayvisGold.copy(alpha = 0.12f))
                                    .border(BorderStroke(1.dp, SayvisGold.copy(alpha = 0.45f)), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .scale(pulseScale)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = SayvisGold,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SayvisGold.copy(alpha = 0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isPersian) "کد ۰۱" else "Code 01",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SayvisGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPersian) "در حال فکر کردن و تحلیل شناختی..." else "Reasoning & cognitive processing...",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SayvisGold
                                    )
                                }
                            }
                        } else {
                            // Standard message bubble with modernized font & styling
                            val isError = msg.errorCode != null
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isOwner) 16.dp else 3.dp,
                                            bottomEnd = if (isOwner) 3.dp else 16.dp
                                        )
                                    )
                                    .background(
                                        when {
                                            isOwner -> SayvisCyan
                                            isError -> SayvisRedAlert.copy(alpha = 0.14f)
                                            else -> SayvisSurfaceVariant
                                        }
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            when {
                                                isOwner -> Color.Transparent
                                                isError -> SayvisRedAlert.copy(alpha = 0.5f)
                                                else -> SayvisBorder
                                            }
                                        ),
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isOwner) 16.dp else 3.dp,
                                            bottomEnd = if (isOwner) 3.dp else 16.dp
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Column {
                                    if (msg.errorCode != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = SayvisRedAlert,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SayvisRedAlert.copy(alpha = 0.25f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isPersian) "کد ${msg.errorCode}" else "Code ${msg.errorCode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SayvisRedAlert
                                                )
                                            }
                                        }
                                    }

                                    if (isOwner) {
                                        Text(
                                            text = msg.text,
                                            color = Color.Black,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 21.sp,
                                            letterSpacing = 0.2.sp
                                        )
                                    } else {
                                        SayvisText(
                                            source = msg.text,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            lineHeight = 21.sp,
                                            letterSpacing = 0.2.sp,
                                            markTranslated = true
                                        )
                                    }
                                }
                            }

                            if (msg.providerUsed != null && !msg.isThinking) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${if (isPersian) "از راهِ " else "via "}${msg.providerUsed.display(isPersian)}",
                                    fontSize = 10.5.sp,
                                    color = SayvisSilverMuted
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Voice Listening Overlay Bar
        if (isVoiceListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SayvisGold.copy(alpha = 0.15f))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPersian) "در حال شنود صدا (VAD + پشتیبانی فارسی)..." else "Listening... (VAD active)",
                    fontSize = 12.sp,
                    color = SayvisGold,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SayvisSurface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Push-To-Talk Voice Input Button
            IconButton(
                onClick = {
                    isVoiceListening = !isVoiceListening
                    if (isVoiceListening) {
                        inputText = if (isPersian) "وضعیت مأموریت‌های استراتژیک را گزارش بده" else "Report status of active strategic missions"
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isVoiceListening) SayvisGold else SayvisSurfaceVariant)
                    .testTag("chat_voice_btn")
            ) {
                Icon(
                    imageVector = if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice Input",
                    tint = if (isVoiceListening) Color.Black else SayvisSilverMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isPersian) "گفتگو با سایویس..." else "Type message to SAYVIS...",
                        fontSize = 13.5.sp,
                        color = SayvisSilverMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SayvisCyan,
                    unfocusedBorderColor = SayvisBorder,
                    focusedContainerColor = SayvisSurfaceVariant,
                    unfocusedContainerColor = SayvisSurfaceVariant
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                        isVoiceListening = false
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SayvisCyan)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
    }
}
