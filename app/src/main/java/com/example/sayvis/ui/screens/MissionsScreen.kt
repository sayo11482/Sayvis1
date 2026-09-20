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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.sayvis.engine.MissionSolutionPlan
import com.example.sayvis.engine.SolutionStep
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionPriority
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import java.util.UUID

@Composable
fun MissionsScreen(
    missions: List<Mission>,
    isPersian: Boolean,
    currentMissionPlan: MissionSolutionPlan? = null,
    missionAgentBusy: Boolean = false,
    onSearchSolution: (String) -> Unit = {},
    onExecuteSolution: (MissionSolutionPlan) -> Unit = {},
    onDismissPlan: () -> Unit = {},
    onToggleTask: (String, String, Boolean) -> Unit,
    onAddMission: (Mission) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("missions_screen"),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SayvisCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_mission_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Mission")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isPersian) "موتور مأموریت‌های استراتژیک" else "Strategic Mission Engine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "ردیابی اهداف، وظایف، موانع و اجرای خودکار توسط ایجنت راهکار"
                               else "Goal tracking, task decomposition & autonomous execution agent",
                        fontSize = 11.sp,
                        color = SayvisSilverMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Autonomous Mission Agent Section
            AutonomousMissionAgentPanel(
                missions = missions,
                currentPlan = currentMissionPlan,
                isBusy = missionAgentBusy,
                isPersian = isPersian,
                onSearchForMission = onSearchSolution,
                onExecutePlan = onExecuteSolution,
                onDismissPlan = onDismissPlan
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (missions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isPersian) "هیچ مأموریتی ثبت نشده است." else "No active strategic missions defined.",
                                    color = SayvisSilverMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                items(missions, key = { it.id }) { mission ->
                    MissionItemCard(
                        mission = mission,
                        isPersian = isPersian,
                        hasActivePlan = currentMissionPlan?.missionId == mission.id,
                        onToggleTask = { taskId, completed ->
                            onToggleTask(mission.id, taskId, completed)
                        },
                        onTriggerAgent = { onSearchSolution(mission.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateMissionDialog(
            isPersian = isPersian,
            onDismiss = { showCreateDialog = false },
            onCreate = { mission ->
                onAddMission(mission)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Autonomous Solution & Execution Agent Panel.
 * Not just a voice assistant: diagnoses problems, finds solutions, and executes steps.
 */
@Composable
fun AutonomousMissionAgentPanel(
    missions: List<Mission>,
    currentPlan: MissionSolutionPlan?,
    isBusy: Boolean,
    isPersian: Boolean,
    onSearchForMission: (String) -> Unit,
    onExecutePlan: (MissionSolutionPlan) -> Unit,
    onDismissPlan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("autonomous_mission_agent_panel"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, if (currentPlan != null) SayvisGold else SayvisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SayvisCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SayvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isPersian) "ایجنت کاوشگر و مجری راهکار" else "Autonomous Solution & Execution Agent",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPersian) "یافتن راهکار برای موانع مأموریت‌ها و اجرای مستقیم اقدامات"
                                   else "Diagnoses blockers, invents workarounds & executes actions",
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }

                if (currentPlan != null) {
                    IconButton(
                        onClick = onDismissPlan,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SayvisSilverMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isBusy -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SayvisGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SayvisGold,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPersian) "ایجنت در حال کاوش راهکار و تدوین برنامه عملیاتی..." else "Agent is searching solutions & synthesizing plan...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SayvisGold
                        )
                    }
                }

                currentPlan != null -> {
                    // Display Synthesized Solution Plan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SayvisSurface, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SayvisGreenSuccess.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isPersian) "راهکار شناسایی شد" else "Solution Devised",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SayvisGreenSuccess
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentPlan.missionTitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SayvisCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Diagnosis
                        Text(
                            text = if (isPersian) "تشخیص مسأله:" else "Diagnosis:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGold
                        )
                        Text(
                            text = currentPlan.diagnosis(isPersian),
                            fontSize = 11.5.sp,
                            color = SayvisSilver,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Strategy
                        Text(
                            text = if (isPersian) "راهکار اجرایی:" else "Action Strategy:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisCyan
                        )
                        Text(
                            text = currentPlan.strategy(isPersian),
                            fontSize = 11.5.sp,
                            color = Color.White,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Steps list
                        Text(
                            text = if (isPersian) "مراحل انجام و اجرا:" else "Action Steps:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilverMuted
                        )

                        currentPlan.steps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(SayvisCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SayvisCyan
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = step.title(isPersian),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = step.description(isPersian),
                                        fontSize = 10.sp,
                                        color = SayvisSilverMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Execute Button
                        Button(
                            onClick = { onExecutePlan(currentPlan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("agent_execute_solution_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SayvisGreenSuccess,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "انجام و اجرای راهکار توسط ایجنت (+${currentPlan.projectedProgressGain}٪)"
                                       else "Execute Solution via Agent (+${currentPlan.projectedProgressGain}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    // Idle state with prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPersian) "یک مأموریت را برای بررسی موانع، یافتن راهکار و اجرای خودکار انتخاب کنید:"
                                   else "Select a mission to diagnose blockers, find solutions & execute:",
                            fontSize = 11.sp,
                            color = SayvisSilverMuted,
                            modifier = Modifier.weight(1f)
                        )

                        val target = missions.firstOrNull { it.tasks.any { t -> t.isBlocked } } ?: missions.firstOrNull()
                        if (target != null) {
                            Button(
                                onClick = { onSearchForMission(target.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SayvisCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("agent_quick_search_btn")
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPersian) "کاوش راهکار" else "Search Solution",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionItemCard(
    mission: Mission,
    isPersian: Boolean,
    hasActivePlan: Boolean = false,
    onToggleTask: (String, Boolean) -> Unit,
    onTriggerAgent: () -> Unit = {}
) {
    val statusColor = when (mission.status) {
        MissionStatus.ACTIVE -> SayvisCyan
        MissionStatus.PLANNED -> SayvisSilverMuted
        MissionStatus.AT_RISK -> SayvisAmberWarning
        MissionStatus.BLOCKED -> SayvisRedAlert
        MissionStatus.COMPLETED -> SayvisGreenSuccess
        MissionStatus.CANCELLED -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_card_${mission.id}"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, if (hasActivePlan) SayvisGold else SayvisBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Priority + Status + Agent Trigger Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(SayvisGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPersian) mission.priority.labelFa else mission.priority.labelEn,
                            color = SayvisGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPersian) mission.status.labelFa else mission.status.labelEn,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Agent Action Trigger
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SayvisCyan.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, SayvisCyan.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .clickable { onTriggerAgent() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("mission_agent_trigger_${mission.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPersian) "ایجنت راهکار" else "Solution Agent",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SayvisCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${mission.progressPercent}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SayvisText(
                source = mission.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                markTranslated = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            SayvisText(
                source = mission.description,
                style = MaterialTheme.typography.bodySmall,
                color = SayvisSilverMuted,
                markTranslated = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { mission.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (mission.progressPercent >= 100) SayvisGreenSuccess else SayvisCyan,
                trackColor = SayvisSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row {
                Text(
                    text = if (isPersian) "مهلت: " else "Deadline: ",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
                SayvisText(
                    source = mission.deadline,
                    fontSize = 11.sp,
                    color = SayvisSilverMuted,
                    markTranslated = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tasks Decomposition List
            Text(
                text = if (isPersian) "وظایف تفکیک‌شده:" else "Decomposed Tasks:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            mission.tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleTask(task.id, task.isCompleted) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task.id, task.isCompleted) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SayvisGreenSuccess,
                            uncheckedColor = SayvisSilverMuted,
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.size(28.dp).testTag("task_chk_${task.id}")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        SayvisText(
                            source = task.title,
                            fontSize = 12.sp,
                            color = if (task.isCompleted) SayvisSilverMuted else Color.White,
                            markTranslated = true
                        )
                        if (task.isBlocked) {
                            Row {
                                Text(
                                    text = "⚠️ ",
                                    fontSize = 10.sp,
                                    color = SayvisRedAlert,
                                    fontWeight = FontWeight.SemiBold
                                )
                                SayvisText(
                                    source = task.blockerReason ?: "Blocked dependency",
                                    fontSize = 10.sp,
                                    color = SayvisRedAlert,
                                    fontWeight = FontWeight.SemiBold,
                                    markTranslated = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateMissionDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onCreate: (Mission) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("End of Week") }
    var task1 by remember { mutableStateOf("") }
    var task2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isPersian) "تعریف مأموریت استراتژیک جدید" else "Create Strategic Mission",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isPersian) "عنوان مأموریت" else "Mission Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mission_input_title")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isPersian) "شرح راهبردی" else "Strategic Context") },
                    modifier = Modifier.fillMaxWidth().testTag("mission_input_desc")
                )

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text(if (isPersian) "مهلت زمانی" else "Deadline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isPersian) "وظایف اولیه:" else "Initial Sub-Tasks:",
                    fontSize = 12.sp,
                    color = SayvisCyan,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = task1,
                    onValueChange = { task1 = it },
                    placeholder = { Text(if (isPersian) "وظیفه ۱..." else "Task 1...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = task2,
                    onValueChange = { task2 = it },
                    placeholder = { Text(if (isPersian) "وظیفه ۲..." else "Task 2...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val tasks = mutableListOf<MissionTask>()
                        if (task1.isNotBlank()) tasks.add(MissionTask(id = "t_" + UUID.randomUUID().toString().take(4), title = task1))
                        if (task2.isNotBlank()) tasks.add(MissionTask(id = "t_" + UUID.randomUUID().toString().take(4), title = task2))
                        if (tasks.isEmpty()) tasks.add(MissionTask(id = "t_init", title = "Initial scoping & requirement definition"))

                        val newMission = Mission(
                            id = "mission_" + UUID.randomUUID().toString().take(6),
                            title = title,
                            description = description,
                            priority = MissionPriority.HIGH,
                            status = MissionStatus.ACTIVE,
                            progressPercent = 0,
                            deadline = deadline,
                            tasks = tasks
                        )
                        onCreate(newMission)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("mission_confirm_create_btn")
            ) {
                Text(if (isPersian) "ثبت مأموریت" else "Create Mission", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel", color = SayvisSilverMuted)
            }
        },
        containerColor = SayvisSurfaceVariant
    )
}
