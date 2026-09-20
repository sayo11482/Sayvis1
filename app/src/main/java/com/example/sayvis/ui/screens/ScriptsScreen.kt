package com.example.sayvis.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.scripts.AutomationScript
import com.example.sayvis.scripts.GitHubScriptCandidate
import com.example.sayvis.scripts.ScriptEngine
import com.example.sayvis.scripts.ScriptRunResult
import com.example.sayvis.scripts.ScriptTrigger
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisConfirmDialog
import com.example.sayvis.ui.components.SayvisDivider
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.components.SayvisOptionRow
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.components.SayvisToggleRow
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

/**
 * Script editor + runner + GitHub script integrator for SAYVIS.
 */
@Composable
fun ScriptsScreen(
    scripts: List<AutomationScript>,
    lastRun: ScriptRunResult?,
    automationEnabled: Boolean,
    persianDigits: Boolean,
    isPersian: Boolean,
    gitHubCandidates: List<GitHubScriptCandidate> = emptyList(),
    onLoadGitHubCandidates: (String) -> Unit = {},
    onMergeGitHubScripts: (List<GitHubScriptCandidate>) -> Unit = {},
    onRun: (AutomationScript) -> Unit,
    onSave: (AutomationScript) -> Unit,
    onDelete: (String) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onAskAssistant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val engine = remember { ScriptEngine() }

    var editing by remember { mutableStateOf<AutomationScript?>(null) }
    var showReference by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AutomationScript?>(null) }

    LaunchedEffect(Unit) {
        onLoadGitHubCandidates("sayo11482/Sayvis1")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("scripts_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = s.scriptsTitle, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Text(text = s.scriptsSubtitle, fontSize = 10.5.sp, color = SayvisSilverMuted)
                }
                IconButton(onClick = { editing = newDraftScript() }, modifier = Modifier.testTag("script_new_button")) {
                    Icon(Icons.Default.Add, contentDescription = s.newScript, tint = SayvisCyan)
                }
            }
        }

        if (!automationEnabled) {
            item {
                SayvisCard(borderColor = SayvisAmberWarning.copy(alpha = 0.5f), containerColor = SayvisAmberWarning.copy(alpha = 0.08f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SayvisAmberWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = s.scriptsDisabled, fontSize = 11.sp, color = SayvisAmberWarning)
                    }
                }
            }
        }

        // ------------------------------------ GitHub Script Integrator Tool
        item {
            GitHubScriptIntegratorSection(
                candidates = gitHubCandidates,
                isPersian = isPersian,
                onRefresh = onLoadGitHubCandidates,
                onMergeSelected = onMergeGitHubScripts
            )
        }

        // ------------------------------------------------------------ editor
        editing?.let { draft ->
            item {
                ScriptEditor(
                    draft = draft,
                    engine = engine,
                    isPersian = isPersian,
                    onDraftChange = { editing = it },
                    onCancel = { editing = null },
                    onSave = {
                        onSave(it)
                        editing = null
                    },
                    onRunNow = {
                        onSave(it)
                        onRun(it)
                        editing = null
                    },
                    onAskAssistant = { idea ->
                        onAskAssistant(assistantPromptFor(idea, isPersian))
                        editing = null
                    }
                )
            }
        }

        // ------------------------------------------------------------- console
        lastRun?.let { result ->
            item {
                SayvisSectionHeader(title = s.scriptConsole)
                SayvisCard(
                    borderColor = if (result.success) SayvisGreenSuccess.copy(alpha = 0.45f) else SayvisRedAlert.copy(alpha = 0.5f),
                    containerColor = if (result.success) SayvisGreenSuccess.copy(alpha = 0.06f) else SayvisRedAlert.copy(alpha = 0.08f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (result.success) SayvisGreenSuccess else SayvisRedAlert,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                result.success -> s.scriptRanOk
                                isPersian -> "اجرا ناموفق بود"
                                else -> "Run failed"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.success) SayvisGreenSuccess else SayvisRedAlert
                        )
                        result.errorLine?.let { line ->
                            Spacer(modifier = Modifier.width(8.dp))
                            SayvisStatusPill(
                                text = if (isPersian) "خط ${PersianFormat.digits(line.toString(), persianDigits)}" else "Line $line",
                                color = SayvisRedAlert
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (result.success) result.log(isPersian) else result.error(isPersian),
                        fontSize = 11.sp,
                        color = SayvisSilverMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // -------------------------------------------------------------- list
        item {
            SayvisSectionHeader(
                title = if (isPersian) "اسکریپت‌های شما" else "Your scripts",
                subtitle = PersianFormat.digits(scripts.size.toString(), persianDigits)
            )
            if (scripts.isEmpty()) {
                SayvisCard {
                    Text(text = s.noScripts, fontSize = 11.5.sp, color = SayvisSilverMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SayvisButton(
                            label = s.newScript,
                            onClick = { editing = newDraftScript() },
                            modifier = Modifier.weight(1f),
                            tone = ButtonTone.PRIMARY,
                            icon = Icons.Default.Add
                        )
                        SayvisButton(
                            label = s.scriptReference,
                            onClick = { showReference = true },
                            modifier = Modifier.weight(1f),
                            tone = ButtonTone.NEUTRAL
                        )
                    }
                }
            }
        }

        items(scripts, key = { it.id }) { script ->
            ScriptRow(
                script = script,
                persianDigits = persianDigits,
                isPersian = isPersian,
                onRun = { onRun(script) },
                onEdit = { editing = script },
                onDelete = { pendingDelete = script },
                onToggle = { enabled -> onToggleEnabled(script.id, enabled) }
            )
        }

        item {
            SayvisButton(
                label = s.scriptReference,
                onClick = { showReference = true },
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.NEUTRAL
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showReference) {
        SayvisConfirmDialog(
            title = s.scriptReference,
            body = ScriptEngine.reference(isPersian),
            confirmLabel = s.close,
            dismissLabel = s.cancel,
            onConfirm = { showReference = false },
            onDismiss = { showReference = false }
        )
    }

    pendingDelete?.let { victim ->
        SayvisConfirmDialog(
            title = s.deleteScript,
            body = if (isPersian) "«${victim.name}» برای همیشه حذف می‌شود." else "\"${victim.name}\" will be permanently deleted.",
            confirmLabel = s.delete,
            dismissLabel = s.cancel,
            danger = true,
            onConfirm = {
                onDelete(victim.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

/**
 * Tool that connects to GitHub, identifies compatible automation scripts, lets the owner
 * select desired items with checkboxes, and merges them directly into Sayvis.
 */
@Composable
fun GitHubScriptIntegratorSection(
    candidates: List<GitHubScriptCandidate>,
    isPersian: Boolean,
    onRefresh: (String) -> Unit,
    onMergeSelected: (List<GitHubScriptCandidate>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var repoName by remember { mutableStateOf("sayo11482/Sayvis1") }
    val selectedMap = remember { mutableStateMapOf<String, Boolean>() }

    // Pre-select first 2 candidates for convenient onboarding
    LaunchedEffect(candidates) {
        candidates.take(2).forEach {
            if (!selectedMap.containsKey(it.id)) selectedMap[it.id] = true
        }
    }

    val selectedCandidates = candidates.filter { selectedMap[it.id] == true }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("github_script_integrator_card"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, SayvisCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SayvisCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isPersian) "ادغام اسکریپت از گیت‌هاب" else "GitHub Script Integrator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPersian) "شناسایی موارد خاص در گیت‌هاب و ادغام با سایویس"
                                   else "Identify specific GitHub scripts & merge into Sayvis",
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = SayvisSilverMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Repo target & status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GitHub: $repoName",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SayvisGold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SayvisCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${candidates.size} " + (if (isPersian) "اسکریپت شناسایی شد" else "scripts identified"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisCyan
                    )
                }
            }

            // Expandable List of GitHub Candidates with Selection Checkboxes
            AnimatedVisibility(visible = expanded || candidates.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = if (isPersian) "موارد شناسایی‌شده را انتخاب کرده و روی ادغام با سایویس بزنید:"
                               else "Select identified items and tap Merge into Sayvis:",
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    candidates.forEach { candidate ->
                        val isChecked = selectedMap[candidate.id] ?: false
                        var showSnippet by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = SayvisSurface),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) SayvisCyan.copy(alpha = 0.6f) else SayvisBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedMap[candidate.id] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SayvisCyan,
                                            uncheckedColor = SayvisSilverMuted,
                                            checkmarkColor = Color.Black
                                        ),
                                        modifier = Modifier.size(26.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = candidate.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SayvisStatusPill(text = candidate.trigger.label(isPersian), color = SayvisGold)
                                        }
                                        Text(
                                            text = candidate.description(isPersian),
                                            fontSize = 10.sp,
                                            color = SayvisSilverMuted,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { showSnippet = !showSnippet },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showSnippet) Icons.Default.KeyboardArrowUp else Icons.Default.Code,
                                            contentDescription = "Code",
                                            tint = SayvisCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                if (showSnippet) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = candidate.source,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SayvisSilver
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Merge Action Button
                    Button(
                        onClick = { onMergeSelected(selectedCandidates) },
                        enabled = selectedCandidates.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("github_merge_scripts_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SayvisCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = SayvisSurfaceVariant,
                            disabledContentColor = SayvisSilverMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "ادغام کدهای انتخابی با سایویس (${selectedCandidates.size} مورد)"
                                   else "Merge Selected Code into Sayvis (${selectedCandidates.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptRow(
    script: AutomationScript,
    persianDigits: Boolean,
    isPersian: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val s = LocalStrings.current
    SayvisCard(borderColor = if (script.enabled) SayvisBorder else SayvisBorder.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = script.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Spacer(modifier = Modifier.width(6.dp))
                    SayvisStatusPill(text = script.trigger.label(isPersian), color = SayvisCyan)
                }
                if (script.description.isNotBlank()) {
                    Text(text = script.description, fontSize = 10.5.sp, color = SayvisSilverMuted)
                }
                Text(
                    text = "${PersianFormat.digits(script.source.lines().filter { it.isNotBlank() }.size.toString(), persianDigits)} " +
                        (if (isPersian) "خط کد" else "lines"),
                    fontSize = 10.sp,
                    color = SayvisSilverMuted
                )
            }
            SayvisToggleRow(
                label = "",
                checked = script.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.width(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SayvisButton(
                label = s.runScript,
                onClick = onRun,
                modifier = Modifier.weight(1f),
                tone = ButtonTone.SUCCESS,
                icon = Icons.Default.PlayArrow
            )
            SayvisButton(
                label = s.editScript,
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                tone = ButtonTone.NEUTRAL,
                icon = Icons.Default.Edit
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = s.deleteScript, tint = SayvisRedAlert)
            }
        }
    }
}

@Composable
private fun ScriptEditor(
    draft: AutomationScript,
    engine: ScriptEngine,
    isPersian: Boolean,
    onDraftChange: (AutomationScript) -> Unit,
    onCancel: () -> Unit,
    onSave: (AutomationScript) -> Unit,
    onRunNow: (AutomationScript) -> Unit,
    onAskAssistant: (String) -> Unit
) {
    val s = LocalStrings.current
    val validation = remember(draft.source) { engine.validate(draft.source) }
    var idea by remember { mutableStateOf("") }

    SayvisCard(borderColor = SayvisCyan, containerColor = SayvisSurfaceVariant) {
        Text(
            text = if (draft.id.startsWith("script_new_")) s.newScript else s.editScript,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SayvisCyan
        )
        Spacer(modifier = Modifier.height(8.dp))

        SayvisField(
            label = s.scriptName,
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            hint = if (isPersian) "مثال: هشدار افت باتری" else "e.g. Low Battery Alert",
            singleLine = true
        )

        SayvisField(
            label = s.scriptDescription,
            value = draft.description,
            onValueChange = { onDraftChange(draft.copy(description = it)) },
            hint = s.optional,
            singleLine = true
        )

        SayvisOptionRow(
            label = s.scriptTrigger,
            options = ScriptTrigger.entries.map { it.label(isPersian) to it },
            selected = draft.trigger,
            onSelect = { onDraftChange(draft.copy(trigger = it)) }
        )

        SayvisField(
            label = s.scriptCode,
            value = draft.source,
            onValueChange = { onDraftChange(draft.copy(source = it)) },
            hint = "# WHEN battery < 20 THEN notify \"...\"",
            singleLine = false,
            minLines = 5,
            isMonospace = true
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (validation.valid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (validation.valid) SayvisGreenSuccess else SayvisRedAlert,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = validation.message(isPersian),
                fontSize = 10.5.sp,
                color = if (validation.valid) SayvisGreenSuccess else SayvisRedAlert
            )
        }

        SayvisDivider()

        Text(
            text = if (isPersian) "می‌خواهید سایو کد را بنویسد؟" else "Want SAYO to write the code?",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = SayvisSilver
        )
        SayvisField(
            label = if (isPersian) "خواستهٔ شما به زبان ساده" else "Describe what you want in plain words",
            value = idea,
            onValueChange = { idea = it },
            hint = if (isPersian) "مثلاً: وقتی طلا از ۲۶۰۰ رد شد خبرم کن" else "e.g. tell me when gold crosses 2600",
            singleLine = false,
            minLines = 2
        )
        Spacer(modifier = Modifier.height(6.dp))
        SayvisButton(
            label = s.askAiToWrite,
            onClick = { onAskAssistant(idea) },
            modifier = Modifier.fillMaxWidth(),
            enabled = idea.isNotBlank(),
            tone = ButtonTone.GOLD,
            icon = Icons.Default.AutoFixHigh
        )

        SayvisDivider()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SayvisButton(
                label = s.cancel,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                tone = ButtonTone.NEUTRAL
            )
            SayvisButton(
                label = s.saveScript,
                onClick = { onSave(draft) },
                modifier = Modifier.weight(1f),
                enabled = validation.valid && draft.name.isNotBlank(),
                tone = ButtonTone.PRIMARY
            )
            SayvisButton(
                label = s.runScript,
                onClick = { onRunNow(draft) },
                modifier = Modifier.weight(1f),
                enabled = validation.valid && draft.name.isNotBlank(),
                tone = ButtonTone.SUCCESS
            )
        }

        if (draft.name.isBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = s.scriptInvalidName, fontSize = 10.sp, color = SayvisAmberWarning)
        }
    }
}

private fun newDraftScript(): AutomationScript = AutomationScript(
    id = "script_new_" + System.currentTimeMillis().toString().takeLast(6),
    name = "",
    description = "",
    trigger = ScriptTrigger.MANUAL,
    source = "# " + "WHEN battery < 20 THEN notify \"...\""
)

private fun assistantPromptFor(idea: String, isPersian: Boolean): String = if (isPersian) {
    "برای این خواسته یک اسکریپت با زبان اسکریپت‌نویسی سایویس بنویس و فقط کد را برگردان: $idea"
} else {
    "Write a SAYVIS scripting-language script for this request and return only the code: $idea"
}
