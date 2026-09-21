package com.example.sayvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.agent.ArenaAgent
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.*
import com.example.sayvis.ui.theme.*

/**
 * SAYVIS Arena Agent — graphical environment.
 *
 * This is the “even graphical environment” screen: the agent works exactly
 * like Arena AI's Agent Mode, but inside the Android app — with a
 * file explorer, bash terminal, live HTML preview (WebView), image canvas,
 * diff viewer and a tool timeline, all visible at once.
 *
 * Layout mirrors Arena:
 *   ┌─────────────────────────────────────────────┐
 *   │ Header: Arena Agent ● LIVE + goal input      │
 *   │─────────────────────────────────────────────│
 *   │ Left: Plan & tool timeline (steps)           │ Right: Graphical Canvas │
 *   │  - 1. Explore workspace                      │  [Live preview /       │
 *   │  - 2. Web search      ▶ terminal/file tree   │   terminal / file tree]│
 *   │  - 3. Build HTML/CSS  ▶ HTML preview (WebView)│                       │
 *   │  - 4. Polish                                  │  File tree below       │
 *   │─────────────────────────────────────────────│
 *   │ Bottom: Message + result                      │
 *   └─────────────────────────────────────────────┘
 */
@Composable
fun ArenaAgentScreen(
    viewModel: SayvisViewModel,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var goal by remember { mutableStateOf("") }
    val busy by viewModel.arenaAgentBusy.collectAsState()
    val steps by viewModel.arenaAgentSteps.collectAsState()
    val toolCalls by viewModel.arenaAgentToolCalls.collectAsState()
    val result by viewModel.arenaAgentResult.collectAsState()
    val plan by viewModel.arenaAgentPlan.collectAsState()
    val htmlPreview by viewModel.arenaHtmlPreview.collectAsState()
    val selectedTool by viewModel.arenaSelectedTool.collectAsState()
    val exampleGoals = listOf(
        "یک لندینگ مینیمال برای کافی‌شاپ بساز با پیش‌نمایش گرافیکی" to Icons.Default.Language,
        "داشبورد تحلیل قیمت طلا با نمودار زنده" to Icons.Default.ShowChart,
        "۳ تصویر گرافیکی برای پست اینستاگرام تولید کن" to Icons.Default.Image,
        "اپ مدیریت مأموریت‌ها با تم طلایی" to Icons.Default.Apps
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SayvisDeepSpace)
            .padding(horizontal = 12.dp)
            .testTag("arena_agent_screen")
    ) {
        Spacer(Modifier.height(8.dp))

        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Arena Agent", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SayvisSilver)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (busy) SayvisGreenSuccess.copy(alpha = 0.18f) else SayvisSurfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (busy) "● LIVE" else "● READY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (busy) SayvisGreenSuccess else SayvisSilverMuted
                        )
                    }
                }
                Text(
                    "مثل Arena AI — با محیط گرافیکی زنده: فایل، ترمینال، پیش‌نمایش",
                    fontSize = 10.5.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Goal input + examples
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisField(
                    label = "هدف را بگو — ایجنت گرافیک می‌سازد",
                    value = goal,
                    onValueChange = { goal = it },
                    hint = "مثال: یک سایت مینیمال برای بوتیک لباس بساز..."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    SayvisButton(
                        label = if (busy) "در حال اجرا..." else "▶ اجرا مثل Arena",
                        onClick = { viewModel.runArenaAgent(goal) },
                        busy = busy,
                        enabled = goal.trim().length >= 6,
                        tone = ButtonTone.GOLD,
                        modifier = Modifier.weight(1f).testTag("arena_run_button")
                    )
                    if (busy) {
                        OutlinedButton(
                            onClick = { viewModel.stopArenaAgent() },
                            modifier = Modifier.testTag("arena_stop_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("توقف", fontSize = 12.sp)
                        }
                    }
                }
                // Example chips
                Text("مثال‌های گرافیکی:", fontSize = 10.sp, color = SayvisSilverMuted, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    exampleGoals.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (title, icon) ->
                                ExampleChip(
                                    title = title,
                                    icon = icon,
                                    onClick = { goal = title },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (plan != null) {
            Spacer(Modifier.height(10.dp))
            SayvisCard(containerColor = SayvisCyan.copy(alpha = 0.06f), borderColor = SayvisCyan.copy(alpha = 0.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "پلنِ Arena: ${plan!!.steps.size} گام • ~${plan!!.estimatedMinutes} دقیقه",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisCyan
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    plan!!.steps.joinToString(" → ") { it.tool.name.lowercase() },
                    fontSize = 10.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Main graphical layout: left timeline, right canvas
        if (steps.isNotEmpty() || toolCalls.isNotEmpty() || busy) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left: tool timeline
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SayvisSurfaceVariant)
                        .border(1.dp, SayvisBorder, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Timeline — مثل Arena", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                        Spacer(Modifier.weight(1f))
                        Text("${toolCalls.size}/${plan?.steps?.size ?: 0}", fontSize = 10.sp, color = SayvisSilverMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(toolCalls) { call ->
                            ArenaToolCard(
                                call = call,
                                isSelected = selectedTool?.id == call.id,
                                onClick = { viewModel.selectArenaTool(call.id) }
                            )
                        }
                        if (busy && toolCalls.isEmpty()) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SayvisGold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Arena در حال فکر کردن...", fontSize = 11.sp, color = SayvisSilverMuted)
                                }
                            }
                        }
                    }
                }

                // Right: graphical canvas
                ArenaCanvas(
                    toolCall = selectedTool ?: toolCalls.lastOrNull(),
                    htmlContent = htmlPreview,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .testTag("arena_canvas")
                )
            }
        }

        // Steps textual log (like Arena's log)
        if (steps.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SayvisCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("گزارشِ زنده:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    steps.forEachIndexed { i, step ->
                        Text(
                            "${i + 1}. $step",
                            fontSize = 11.sp,
                            color = SayvisCyan,
                            modifier = Modifier.testTag("arena_step_$i")
                        )
                    }
                }
            }
        }

        // Final result
        result?.let { answer ->
            Spacer(Modifier.height(10.dp))
            SayvisCard(containerColor = SayvisGreenSuccess.copy(alpha = 0.06f), borderColor = SayvisGreenSuccess.copy(alpha = 0.25f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SayvisGreenSuccess, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("نتیجهٔ گرافیکی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisGreenSuccess)
                }
                Spacer(Modifier.height(8.dp))
                Text(answer, fontSize = 12.sp, color = SayvisSilver, modifier = Modifier.testTag("arena_result"))
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ArenaToolCard(call: ArenaAgent.ToolCall, isSelected: Boolean, onClick: () -> Unit) {
    val bg = when {
        isSelected -> SayvisGold.copy(alpha = 0.12f)
        call.isRunning -> SayvisCyan.copy(alpha = 0.08f)
        call.isSuccess -> SayvisSurface
        else -> SayvisRedAlert.copy(alpha = 0.08f)
    }
    val border = when {
        isSelected -> SayvisGold.copy(alpha = 0.5f)
        call.isRunning -> SayvisCyan.copy(alpha = 0.4f)
        else -> SayvisBorder
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (call.kind) {
                            ArenaAgent.ToolKind.BASH -> Color(0xFF0B0D12)
                            ArenaAgent.ToolKind.WEB_SEARCH -> SayvisCyan.copy(alpha = 0.15f)
                            ArenaAgent.ToolKind.GENERATE_IMAGE -> SayvisGold.copy(alpha = 0.15f)
                            ArenaAgent.ToolKind.PREVIEW -> SayvisGreenSuccess.copy(alpha = 0.15f)
                            else -> SayvisSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForTool(call.kind),
                    contentDescription = null,
                    tint = when (call.kind) {
                        ArenaAgent.ToolKind.BASH -> Color(0xFF00D4FF)
                        ArenaAgent.ToolKind.WEB_SEARCH -> SayvisCyan
                        ArenaAgent.ToolKind.GENERATE_IMAGE -> SayvisGold
                        ArenaAgent.ToolKind.PREVIEW -> SayvisGreenSuccess
                        else -> SayvisSilver
                    },
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(call.kind.name.lowercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = SayvisSilver)
            Spacer(Modifier.weight(1f))
            if (call.isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = SayvisGold)
            } else {
                Icon(
                    if (call.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (call.isSuccess) SayvisGreenSuccess else SayvisRedAlert,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(call.input.take(60), fontSize = 10.sp, color = SayvisSilverMuted, maxLines = 1)
        Text(call.output.take(80), fontSize = 10.5.sp, color = SayvisSilver, maxLines = 2)
    }
}

@Composable
private fun ExampleChip(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SayvisSurface)
            .border(1.dp, SayvisBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 10.sp, color = SayvisSilver, maxLines = 1)
    }
}

private fun iconForTool(kind: ArenaAgent.ToolKind): ImageVector = when (kind) {
    ArenaAgent.ToolKind.BASH -> Icons.Default.Code
    ArenaAgent.ToolKind.READ -> Icons.Default.Folder
    ArenaAgent.ToolKind.WRITE -> Icons.Default.Edit
    ArenaAgent.ToolKind.EDIT -> Icons.Default.EditNote
    ArenaAgent.ToolKind.WEB_SEARCH -> Icons.Default.TravelExplore
    ArenaAgent.ToolKind.IMAGE_SEARCH -> Icons.Default.ImageSearch
    ArenaAgent.ToolKind.GENERATE_IMAGE -> Icons.Default.Image
    ArenaAgent.ToolKind.PREVIEW -> Icons.Default.Preview
    ArenaAgent.ToolKind.DONE -> Icons.Default.Done
}
