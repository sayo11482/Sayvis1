package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ai.ConnectivityProbe
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.SayvisViewModel.AvatarState
import com.example.sayvis.ui.components.SayvisRobotFace
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * The minimal core view of the SAYVIS machine: a quiet dark stage, the live
 * face, a mono state readout and the honest network pill. No clutter.
 */
@Composable
fun RobotScreen(
    avatarState: AvatarState,
    listenLevel: Float,
    connectivity: ConnectivityProbe.Result?,
    isPersian: Boolean,
    onAskAssistant: (String) -> Unit,
    onRefreshConnectivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var previewOverride by remember { mutableStateOf<AvatarState?>(null) }
    val shownState = previewOverride ?: avatarState

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SayvisDeepSpace)
            .testTag("robot_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // ---- header: mono caption + live network pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAYVIS // CORE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = SayvisSilverMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                NetworkPill(connectivity = connectivity, isPersian = isPersian, onRefresh = onRefreshConnectivity)
            }

            Spacer(modifier = Modifier.weight(1f))

            // ---- the face
            SayvisRobotFace(
                state = shownState,
                level = listenLevel,
                modifier = Modifier.size(330.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stateLabel(shownState, isPersian),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                color = SayvisSilverMuted,
                modifier = Modifier.testTag("robot_state_label")
            )

            Spacer(modifier = Modifier.weight(1f))

            // ---- minimal state chips
            Text(
                text = s.robotPreview,
                fontSize = 10.sp,
                color = SayvisSilverMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val states = listOf(
                    AvatarState.IDLE to s.robotStateIdle,
                    AvatarState.THINKING to s.robotStateThinking,
                    AvatarState.SPEAKING to s.robotStateSpeaking,
                    AvatarState.OPPORTUNITY_AWARE to s.robotStateAware,
                    AvatarState.EMERGENCY_LOCKED to s.robotStateLocked
                )
                states.forEachIndexed { index, (state, label) ->
                    val selected = shownState == state
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        color = if (selected) SayvisSilver else SayvisSilverMuted,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .border(
                                BorderStroke(1.dp, if (selected) SayvisGold.copy(alpha = 0.6f) else SayvisSilverMuted.copy(alpha = 0.25f)),
                                RoundedCornerShape(50)
                            )
                            .clickable { previewOverride = if (selected) null else state }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("robot_preview_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = s.robotAsk,
                fontSize = 11.sp,
                color = SayvisSilver,
                modifier = Modifier
                    .border(
                        BorderStroke(1.dp, SayvisGold.copy(alpha = 0.55f)),
                        RoundedCornerShape(50)
                    )
                    .clickable {
                        onAskAssistant(if (isPersian) "جستجو کن درباره آخرین اخبار هوش مصنوعی" else "search for the latest AI news")
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
                    .testTag("robot_ask_button")
            )

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun NetworkPill(
    connectivity: ConnectivityProbe.Result?,
    isPersian: Boolean,
    onRefresh: () -> Unit
) {
    val s = LocalStrings.current
    val color = when (connectivity?.state) {
        ConnectivityProbe.NetState.ONLINE -> Color(0xFF10B981)
        ConnectivityProbe.NetState.ONLINE_NO_GOOGLE -> SayvisGold
        ConnectivityProbe.NetState.OFFLINE -> SayvisRedAlert
        null -> SayvisSilverMuted
    }
    val label = when (connectivity?.state) {
        ConnectivityProbe.NetState.ONLINE -> if (isPersian) "آنلاین" else "ONLINE"
        ConnectivityProbe.NetState.ONLINE_NO_GOOGLE -> if (isPersian) "گوگل مسدود" else "GOOGLE BLOCKED"
        ConnectivityProbe.NetState.OFFLINE -> if (isPersian) "آفلاین" else "OFFLINE"
        null -> if (isPersian) "بررسی…" else "CHECKING"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(50))
            .clickable { onRefresh() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("robot_network_pill")
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            color = color
        )
    }
}

private fun stateLabel(state: AvatarState, isPersian: Boolean): String = when (state) {
    AvatarState.IDLE -> if (isPersian) "IDLE — آماده" else "IDLE"
    AvatarState.THINKING -> if (isPersian) "THINKING — در حال تفکر" else "THINKING"
    AvatarState.SPEAKING -> if (isPersian) "SPEAKING — در حال گفتار" else "SPEAKING"
    AvatarState.OPPORTUNITY_AWARE -> if (isPersian) "AWARE — هوشیار" else "AWARE"
    AvatarState.EMERGENCY_LOCKED -> if (isPersian) "LOCKED — قفل اضطراری" else "LOCKED"
    AvatarState.OFFLINE -> if (isPersian) "OFFLINE — آفلاین" else "OFFLINE"
}
