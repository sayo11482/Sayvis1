package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.sayvis.model.Mission
import com.example.sayvis.model.MissionPriority
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.MissionTask
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import java.util.UUID

@Composable
fun MissionsScreen(
    missions: List<Mission>,
    isPersian: Boolean,
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
                        text = if (isPersian) "ردیابی اهداف، وظایف، موانع و تحلیل پیشرفت" else "Goal-to-task decomposition, blocker detection & telemetry",
                        fontSize = 11.sp,
                        color = SayvisSilverMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        onToggleTask = { taskId, completed ->
                            onToggleTask(mission.id, taskId, completed)
                        }
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

@Composable
fun MissionItemCard(
    mission: Mission,
    isPersian: Boolean,
    onToggleTask: (String, Boolean) -> Unit
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
        border = BorderStroke(1.dp, SayvisBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Priority + Status
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

                Text(
                    text = "${mission.progressPercent}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SayvisCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = mission.description,
                style = MaterialTheme.typography.bodySmall,
                color = SayvisSilverMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { mission.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SayvisCyan,
                trackColor = SayvisSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${if (isPersian) "مهلت: " else "Deadline: "} ${mission.deadline}",
                fontSize = 11.sp,
                color = SayvisSilverMuted
            )

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
                        Text(
                            text = task.title,
                            fontSize = 12.sp,
                            color = if (task.isCompleted) SayvisSilverMuted else Color.White
                        )
                        if (task.isBlocked) {
                            Text(
                                text = "⚠️ ${task.blockerReason ?: "Blocked dependency"}",
                                fontSize = 10.sp,
                                color = SayvisRedAlert,
                                fontWeight = FontWeight.SemiBold
                            )
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
