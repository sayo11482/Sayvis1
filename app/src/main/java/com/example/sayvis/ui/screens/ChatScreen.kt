package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.ui.ChatMessage
import com.example.sayvis.ui.components.SayvisAvatar
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    avatarState: AvatarState,
    isPersian: Boolean,
    onSendMessage: (String) -> Unit,
    pendingAction: com.example.sayvis.ui.AssistantAction? = null,
    onApproveAction: () -> Unit = {},
    onDismissAction: () -> Unit = {},
    voiceListening: Boolean = false,
    voiceAvailable: Boolean = true,
    onVoiceInput: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val isVoiceListening = voiceListening
    val listState = rememberLazyListState()

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
                    text = if (isPersian) "هوش مصنوعی سایویس" else "SAYVIS Autonomous Agent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Text(
                    text = if (avatarState == AvatarState.THINKING) {
                        if (isPersian) "در حال پردازش شناختی..." else "Reasoning over UIC context..."
                    } else {
                        if (isPersian) "آنلاین • مدل ارکستراسیون هوش مصنوعی" else "Online • Multi-Provider Orchestrator"
                    },
                    fontSize = 11.sp,
                    color = SayvisCyan
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages) { msg ->
                val isOwner = msg.sender == "OWNER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOwner) Arrangement.End else Arrangement.Start
                ) {
                    if (!isOwner) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SayvisCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.85f),
                        horizontalAlignment = if (isOwner) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isOwner) 14.dp else 2.dp,
                                        bottomEnd = if (isOwner) 2.dp else 14.dp
                                    )
                                )
                                .background(if (isOwner) SayvisCyan else SayvisSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            if (isOwner) {
                                Text(
                                    text = msg.text,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            } else {
                                // Assistant answers are routed through the hybrid translator so a
                                // Persian UI never shows an English reply.
                                SayvisText(
                                    source = msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    markTranslated = true
                                )
                            }
                        }

                        if (msg.providerUsed != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (isPersian) "از راهِ " else "via "}${msg.providerUsed.display(isPersian)}",
                                fontSize = 10.sp,
                                color = SayvisSilverMuted
                            )
                        }
                    }
                }
            }

            if (avatarState == AvatarState.THINKING) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SayvisGold,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "سایویس در حال اندیشیدن است..." else "SAYVIS is reasoning...",
                            fontSize = 12.sp,
                            color = SayvisGold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Zero-trust consent card for a privileged assistant action.
        if (pendingAction != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
                    border = BorderStroke(1.dp, SayvisGold)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isPersian) "🔐 نیاز به تأیید شما: " + pendingAction.titleFa
                                   else "🔐 Your approval required: " + pendingAction.titleEn,
                            fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SayvisGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPersian) pendingAction.detailFa else pendingAction.detailEn,
                            fontSize = 10.5.sp, color = SayvisSilverMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onApproveAction,
                                colors = ButtonDefaults.buttonColors(containerColor = SayvisGreenSuccess, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isPersian) "تأیید و اجرا" else "Approve & run", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onDismissAction,
                                colors = ButtonDefaults.buttonColors(containerColor = SayvisRedAlert, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isPersian) "رد" else "Deny", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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
            // Voice Input Button (real platform speech recognition)
            IconButton(
                onClick = { onVoiceInput() },
                enabled = voiceAvailable,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isVoiceListening) SayvisGold else SayvisSurfaceVariant)
                    .testTag("chat_voice_btn")
            ) {
                Icon(
                    imageVector = if (voiceAvailable) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice Input",
                    tint = if (isVoiceListening) Color.Black else if (voiceAvailable) SayvisCyan else SayvisSilverMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (isPersian) "گفتگو با سایویس..." else "Type message to SAYVIS...",
                        fontSize = 13.sp,
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
