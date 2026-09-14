package com.example.sayvis.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * The SAYVIS research agent console: give it a goal and watch it search the
 * web, open and read the top pages, optionally consult the owner's Google
 * account (gmail/calendar/drive) and synthesise a cited answer. The same
 * engine is reachable from the assistant with "ایجنت: …".
 */
@Composable
fun AgentScreen(
    viewModel: SayvisViewModel,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var goal by remember { mutableStateOf("") }
    val busy by viewModel.agentBusy.collectAsState()
    val steps by viewModel.agentSteps.collectAsState()
    val result by viewModel.agentResult.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("agent_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = s.sectionAgent, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.toolAgentHint, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisField(
                    label = s.agentGoalLabel,
                    value = goal,
                    onValueChange = { goal = it },
                    hint = s.agentEmpty
                )
                SayvisButton(
                    label = if (busy) s.agentRunning else s.agentRun,
                    onClick = { viewModel.runAgent(goal) },
                    busy = busy,
                    enabled = goal.trim().length >= 6,
                    tone = ButtonTone.GOLD,
                    modifier = Modifier.fillMaxWidth().testTag("agent_run_button")
                )
            }
        }

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            SayvisSectionHeader(title = s.agentRunning, icon = Icons.Default.TravelExplore)
            SayvisCard(containerColor = SayvisSilver.copy(alpha = 0.04f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            fontSize = 11.5.sp,
                            color = SayvisCyan,
                            modifier = Modifier.testTag("agent_step_$index")
                        )
                    }
                }
            }
        }

        result?.let { answer ->
            Spacer(modifier = Modifier.height(12.dp))
            SayvisCard {
                Text(
                    text = answer,
                    fontSize = 12.5.sp,
                    color = SayvisSilver,
                    modifier = Modifier.testTag("agent_result")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}
