package com.example.sayvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisRobotFace
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * The standalone live view of the launcher-art robot: breathing chrome head with
 * colour-shifting glowing eyes, a mouth vent that glows in shifting purple while
 * SAYVIS thinks, and the hexagonal lattice chest core. Owners can preview every
 * avatar state and jump to the assistant for a live test.
 */
@Composable
fun RobotScreen(
    avatarState: AvatarState,
    listenLevel: Float,
    isPersian: Boolean,
    onAskAssistant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var previewOverride by remember { mutableStateOf<AvatarState?>(null) }
    val shownState = previewOverride ?: avatarState

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("robot_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            SayvisRobotFace(
                state = shownState,
                level = listenLevel,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = s.robotTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SayvisSilver,
                    modifier = Modifier.weight(1f)
                )
                SayvisStatusPill(
                    text = when (shownState) {
                        AvatarState.IDLE -> s.robotStateIdle
                        AvatarState.THINKING -> s.robotStateThinking
                        AvatarState.SPEAKING -> s.robotStateSpeaking
                        AvatarState.OPPORTUNITY_AWARE -> s.robotStateAware
                        AvatarState.EMERGENCY_LOCKED -> s.robotStateLocked
                        AvatarState.OFFLINE -> s.robotStateOffline
                    },
                    color = when (shownState) {
                        AvatarState.THINKING, AvatarState.SPEAKING -> SayvisCyan
                        AvatarState.EMERGENCY_LOCKED -> com.example.sayvis.ui.theme.SayvisRedAlert
                        AvatarState.OPPORTUNITY_AWARE -> com.example.sayvis.ui.theme.SayvisGold
                        else -> SayvisGreenSuccess
                    }
                )
            }
            Text(
                text = s.robotHintLive,
                fontSize = 11.sp,
                color = SayvisSilverMuted,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = s.robotPreview, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val previews = listOf(
                    AvatarState.IDLE, AvatarState.THINKING, AvatarState.SPEAKING,
                    AvatarState.OPPORTUNITY_AWARE, AvatarState.EMERGENCY_LOCKED
                )
                previews.forEach { previewState ->
                    val selected = previewOverride == previewState
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) SayvisCyan.copy(alpha = 0.15f) else com.example.sayvis.ui.theme.SayvisSurfaceVariant)
                            .clickable { previewOverride = if (selected) null else previewState }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("robot_preview_" + previewState.name.lowercase())
                    ) {
                        Text(
                            text = when (previewState) {
                                AvatarState.IDLE -> if (isPersian) "عادی" else "Idle"
                                AvatarState.THINKING -> if (isPersian) "تفکر" else "Think"
                                AvatarState.SPEAKING -> if (isPersian) "گفتار" else "Speak"
                                AvatarState.OPPORTUNITY_AWARE -> if (isPersian) "هوشیار" else "Aware"
                                AvatarState.EMERGENCY_LOCKED -> if (isPersian) "قفل" else "Lock"
                                else -> ""
                            },
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) SayvisCyan else SayvisSilverMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            SayvisButton(
                label = s.robotAsk,
                onClick = { onAskAssistant(if (isPersian) "وضعیت رو گزارش بده" else "report status") },
                tone = com.example.sayvis.ui.components.ButtonTone.PRIMARY,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("robot_ask_button")
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
