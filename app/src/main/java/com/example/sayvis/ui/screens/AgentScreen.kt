package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contacts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ai.SpecialistAgent
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import androidx.compose.foundation.background
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * The SAYVIS agent hub: five specialists behind one calm console. The owner
 * picks a mission type, states the goal, and the pipeline runs itself — best
 * free brain auto-selected, live web grounding, cited deliverable. The self-
 * evolution card scans GitHub for similar agents and keeps an adoption
 * backlog, so SAYVIS keeps absorbing the best patterns in the field.
 */
@Composable
fun AgentScreen(
    viewModel: SayvisViewModel,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var kind by remember { mutableStateOf(SpecialistAgent.Kind.RESEARCH) }
    var goal by remember { mutableStateOf("") }
    val busy by viewModel.agentBusy.collectAsState()
    val steps by viewModel.agentSteps.collectAsState()
    val result by viewModel.agentResult.collectAsState()
    val evolutionBusy by viewModel.evolutionBusy.collectAsState()
    val evolutionReport by viewModel.evolutionReport.collectAsState()
    val bizDirectory by viewModel.bizDirectory.collectAsState()
    val bizBusy by viewModel.bizBusy.collectAsState()
    val bizMessage by viewModel.bizMessage.collectAsState()
    var instaHandle by remember { mutableStateOf("") }
    var instaBio by remember { mutableStateOf("") }
    var instaCtx by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadBizDirectory() }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                Text(text = s.agentHubHint, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- mission type chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val kinds = listOf(
                SpecialistAgent.Kind.RESEARCH to s.agentKindResearch,
                SpecialistAgent.Kind.TRADE to s.agentKindTrade,
                SpecialistAgent.Kind.CONTENT to s.agentKindContent
            )
            kinds.forEachIndexed { index, (candidate, label) ->
                KindChip(
                    label = label,
                    selected = kind == candidate,
                    onClick = { kind = candidate },
                    tag = "agent_kind_$index",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val kinds = listOf(
                SpecialistAgent.Kind.WEBDESIGN to s.agentKindWeb,
                SpecialistAgent.Kind.APPDEV to s.agentKindApp
            )
            kinds.forEachIndexed { index, (candidate, label) ->
                KindChip(
                    label = label,
                    selected = kind == candidate,
                    onClick = { kind = candidate },
                    tag = "agent_kind_${index + 3}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisField(
                    label = s.agentGoalLabel,
                    value = goal,
                    onValueChange = { goal = it },
                    hint = goalHint(kind, s)
                )
                SayvisButton(
                    label = if (busy) s.agentRunning else s.agentRun,
                    onClick = { viewModel.runSpecialist(kind, goal) },
                    busy = busy,
                    enabled = goal.trim().length >= 6,
                    tone = ButtonTone.GOLD,
                    modifier = Modifier.fillMaxWidth().testTag("agent_run_button")
                )
                Text(
                    text = s.agentAutoBrain,
                    fontSize = 10.5.sp,
                    color = SayvisGreenSuccess
                )
            }
        }

        if (steps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            SayvisCard {
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

        // ---- self-evolution
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = s.evolutionTitle, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = s.evolutionHint, fontSize = 11.sp, color = SayvisSilverMuted)
                SayvisButton(
                    label = if (evolutionBusy) s.evolutionScanning else s.evolutionRun,
                    onClick = { viewModel.runEvolution() },
                    busy = evolutionBusy,
                    tone = ButtonTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth().testTag("evolution_run_button")
                )
                evolutionReport?.let { report ->
                    Text(
                        text = report,
                        fontSize = 11.sp,
                        color = SayvisSilver,
                        modifier = Modifier.testTag("evolution_report")
                    )
                }
            }
        }

        // ==================== MANAGER AGENT: BUSINESS DIRECTORY (v5.0.0) ====
        Spacer(modifier = Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Contacts, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = s.managerTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = s.managerSmsHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
                val smsPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) viewModel.scanSmsDirectory()
                }
                SayvisButton(
                    label = if (bizBusy) s.managerScanning else s.managerScan,
                    onClick = {
                        val ctx = context
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                            ctx, android.Manifest.permission.READ_SMS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.scanSmsDirectory() else smsPermission.launch(android.Manifest.permission.READ_SMS)
                    },
                    busy = bizBusy,
                    tone = ButtonTone.PRIMARY,
                    modifier = Modifier.fillMaxWidth().testTag("manager_sms_scan")
                )
                if (bizMessage.isNotBlank()) {
                    Text(
                        text = bizMessage,
                        fontSize = 11.sp,
                        color = SayvisGreenSuccess,
                        modifier = Modifier.testTag("manager_message")
                    )
                }
                // Category summary
                if (bizDirectory.isNotEmpty()) {
                    val counts = bizDirectory.groupingBy { it.category }.eachCount()
                    Text(
                        text = counts.entries.sortedBy { it.key.ordinal }.joinToString(" | ") { (cat, n) ->
                            cat.labelFa + ": " + n
                        },
                        fontSize = 10.5.sp,
                        color = SayvisSilver,
                        modifier = Modifier.testTag("manager_counts")
                    )
                }
                if (bizDirectory.isEmpty()) {
                    Text(text = s.managerEmpty, fontSize = 10.5.sp, color = SayvisSilverMuted)
                }
                bizDirectory.take(12).forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SayvisSurfaceVariant)
                            .padding(10.dp)
                            .testTag("manager_entry")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = (entry.name + " " + entry.family).trim().ifBlank { entry.phone },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SayvisSilver,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = entry.category.labelFa,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (entry.category) {
                                    com.example.sayvis.agent.BusinessDirectory.Category.SUPPLIER -> SayvisGreenSuccess
                                    com.example.sayvis.agent.BusinessDirectory.Category.DISTRIBUTION -> SayvisGold
                                    com.example.sayvis.agent.BusinessDirectory.Category.OFFICE -> SayvisCyan
                                    else -> SayvisSilverMuted
                                }
                            )
                        }
                        if (entry.company.isNotBlank()) {
                            Text(text = "🏢 " + entry.company, fontSize = 10.5.sp, color = SayvisSilver)
                        }
                        Text(text = "☎ " + entry.phone, fontSize = 10.5.sp, color = SayvisSilver)
                        if (entry.site.isNotBlank()) {
                            Text(text = "🌐 " + entry.site, fontSize = 10.5.sp, color = SayvisCyan)
                        }
                        if (entry.address.isNotBlank()) {
                            Text(text = "📍 " + entry.address, fontSize = 10.5.sp, color = SayvisSilver)
                        }
                        if (entry.signals.isNotEmpty()) {
                            Text(
                                text = "🔎 " + entry.signals.take(3).joinToString("، "),
                                fontSize = 9.5.sp,
                                color = SayvisSilverMuted
                            )
                        }
                    }
                }
            }
        }

        // ---- Instagram analysis
        Spacer(modifier = Modifier.height(12.dp))
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = s.managerInstaTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.managerInstaHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
                SayvisField(
                    label = s.managerInstaHandle,
                    value = instaHandle,
                    onValueChange = { instaHandle = it },
                    hint = "@business_page"
                )
                SayvisField(
                    label = s.managerInstaBio,
                    value = instaBio,
                    onValueChange = { instaBio = it },
                    hint = "تولید و پخش عمده مواد اولیه صنعتی…"
                )
                SayvisField(
                    label = s.managerInstaCtx,
                    value = instaCtx,
                    onValueChange = { instaCtx = it },
                    hint = "followers=12000;following=380"
                )
                SayvisButton(
                    label = s.managerInstaAdd,
                    onClick = { viewModel.addInstagramProfile(instaHandle, instaBio, instaCtx) },
                    enabled = instaHandle.isNotBlank() && instaBio.isNotBlank(),
                    tone = ButtonTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth().testTag("manager_insta_add")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun KindChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        fontSize = 10.5.sp,
        color = if (selected) SayvisSilver else SayvisSilverMuted,
        modifier = modifier
            .border(
                BorderStroke(1.dp, if (selected) SayvisGold.copy(alpha = 0.6f) else SayvisSilverMuted.copy(alpha = 0.25f)),
                androidx.compose.foundation.shape.RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag(tag)
    )
}

private fun goalHint(kind: SpecialistAgent.Kind, s: com.example.sayvis.i18n.SayvisStrings): String = when (kind) {
    SpecialistAgent.Kind.RESEARCH -> s.agentEmpty
    SpecialistAgent.Kind.TRADE -> s.agentGoalTrade
    SpecialistAgent.Kind.CONTENT -> s.agentGoalContent
    SpecialistAgent.Kind.WEBDESIGN -> s.agentGoalWeb
    SpecialistAgent.Kind.APPDEV -> s.agentGoalApp
}
