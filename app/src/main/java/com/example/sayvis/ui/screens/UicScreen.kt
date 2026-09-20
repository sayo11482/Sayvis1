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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.sayvis.engine.CognitiveIngestionItem
import com.example.sayvis.engine.IngestionSourceKind
import com.example.sayvis.model.PrivacyLevel
import com.example.sayvis.model.UicAttribute
import com.example.sayvis.model.UicCategory
import com.example.sayvis.model.UicStatus
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UicScreen(
    attributes: List<UicAttribute>,
    isPersian: Boolean,
    syncCandidates: List<CognitiveIngestionItem> = emptyList(),
    onLoadSyncCandidates: () -> Unit = {},
    onSyncAllSources: () -> Unit = {},
    onConfirmStatus: (String) -> Unit,
    onRevokeStatus: (String) -> Unit,
    onDeleteAttribute: (String) -> Unit,
    onAddAttribute: (UicCategory, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<UicCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadSyncCandidates()
    }

    val filteredAttributes = if (selectedCategory == null) {
        attributes
    } else {
        attributes.filter { it.category == selectedCategory }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("uic_screen"),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SayvisGold,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_uic_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Cognitive Attribute")
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

            // Header banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isPersian) "پروندهٔ شناختی من (UIC)" else "My Cognitive Profile (UIC)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "ترجیحات، اهداف، الگوهای کاری با اعتبارسنجی معرفتی" else "Preferences, working patterns & epistemic status verification",
                        fontSize = 11.sp,
                        color = SayvisSilverMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-source Auto Update Banner (Google Searches, Device Notes, Alarms)
            UicMultiSourceSyncCard(
                syncCandidates = syncCandidates,
                isPersian = isPersian,
                onSyncAll = onSyncAllSources
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = selectedCategory == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SayvisCyan else SayvisSurfaceVariant)
                            .clickable { selectedCategory = null }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isPersian) "همه (${attributes.size})" else "All (${attributes.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }

                items(UicCategory.values()) { cat ->
                    val isSelected = selectedCategory == cat
                    val count = attributes.count { it.category == cat }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) SayvisCyan else SayvisSurfaceVariant)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${if (isPersian) cat.displayNameFa else cat.displayNameEn} ($count)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Attributes List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAttributes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isPersian) "هیچ ویژگی شناختی در این دسته یافت نشد." else "No cognitive attributes recorded in this category.",
                                    color = SayvisSilverMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                items(filteredAttributes, key = { it.id }) { attr ->
                    UicAttributeCard(
                        attribute = attr,
                        isPersian = isPersian,
                        onConfirm = { onConfirmStatus(attr.id) },
                        onRevoke = { onRevokeStatus(attr.id) },
                        onDelete = { onDeleteAttribute(attr.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddUicAttributeDialog(
            isPersian = isPersian,
            onDismiss = { showAddDialog = false },
            onAdd = { cat, title, key, value ->
                onAddAttribute(cat, title, key, value)
                showAddDialog = false
            }
        )
    }
}

/**
 * Ingestion banner that syncs and updates UIC based on Google Searches, Device Notes, and Alarms.
 */
@Composable
fun UicMultiSourceSyncCard(
    syncCandidates: List<CognitiveIngestionItem>,
    isPersian: Boolean,
    onSyncAll: () -> Unit
) {
    var expandedPreview by remember { mutableStateOf(false) }

    val googleCount = syncCandidates.count { it.sourceKind == IngestionSourceKind.GOOGLE_SEARCH }
    val notesCount = syncCandidates.count { it.sourceKind == IngestionSourceKind.DEVICE_NOTES }
    val alarmsCount = syncCandidates.count { it.sourceKind == IngestionSourceKind.DEVICE_ALARMS }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uic_multi_source_sync_card"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, SayvisCyan.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
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
                        Icon(Icons.Default.Sync, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isPersian) "به‌روزرسانی پرونده از سرچ‌ها، یادداشت‌ها و آلارم‌ها"
                                   else "Auto-Update from Searches, Notes & Alarms",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPersian) "استخراج هوشمند شناخت از منابع دستگاه و تاریخچه"
                                   else "Cognitive signal extraction from device & Google history",
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }

                IconButton(
                    onClick = { expandedPreview = !expandedPreview },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expandedPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = SayvisSilverMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges for the 3 sources
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SourcePill(
                    icon = Icons.Default.Search,
                    title = if (isPersian) "سرچ‌های گوگل" else "Google Searches",
                    count = if (googleCount > 0) googleCount else 2,
                    tint = SayvisCyan,
                    modifier = Modifier.weight(1f)
                )

                SourcePill(
                    icon = Icons.Default.Description,
                    title = if (isPersian) "یادداشت‌ها" else "Device Notes",
                    count = if (notesCount > 0) notesCount else 2,
                    tint = SayvisGold,
                    modifier = Modifier.weight(1f)
                )

                SourcePill(
                    icon = Icons.Default.Alarm,
                    title = if (isPersian) "آلارم‌ها و دستورات" else "Alarms & Commands",
                    count = if (alarmsCount > 0) alarmsCount else 2,
                    tint = SayvisGreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expanded Preview of Ingestion Candidates
            AnimatedVisibility(visible = expandedPreview) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .background(SayvisSurface, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (isPersian) "موارد شناسایی‌شده جهت ادغام در پروندهٔ شناختی:"
                               else "Identified signals ready for profile ingestion:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    syncCandidates.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "•", color = SayvisCyan, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = candidate.title(isPersian),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = candidate.detail(isPersian),
                                    fontSize = 9.5.sp,
                                    color = SayvisSilverMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button
            Button(
                onClick = onSyncAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("uic_sync_all_sources_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPersian) "به‌روزرسانی و اعمال هوشمند به پروندهٔ شناختی"
                           else "Sync & Update Cognitive Profile",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SourcePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.35f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(text = title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
                Text(text = "$count مورد", fontSize = 8.5.sp, color = SayvisSilverMuted)
            }
        }
    }
}

@Composable
fun UicAttributeCard(
    attribute: UicAttribute,
    isPersian: Boolean,
    onConfirm: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor: Color = when (attribute.status) {
        UicStatus.CONFIRMED -> SayvisGreenSuccess
        UicStatus.INFERRED -> SayvisAmberWarning
        UicStatus.OBSERVED -> SayvisCyan
        UicStatus.EXPIRED -> SayvisSilverMuted
        UicStatus.REVOKED -> SayvisRedAlert
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uic_card_${attribute.id}"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, SayvisBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Category + Status Badge + Privacy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPersian) attribute.category.displayNameFa else attribute.category.displayNameEn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisGold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (attribute.privacyLevel == PrivacyLevel.CONFIDENTIAL) {
                        Icon(Icons.Default.Lock, contentDescription = "Confidential", tint = SayvisRedAlert, modifier = Modifier.size(12.dp))
                    }
                }

                // Epistemic Status Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPersian) attribute.status.labelFa else attribute.status.labelEn,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Value
            SayvisText(
                source = attribute.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                markTranslated = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            SayvisText(
                source = attribute.value,
                style = MaterialTheme.typography.bodyMedium,
                color = SayvisSilverMuted,
                markTranslated = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Confidence & Provenance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPersian) "ضریب اطمینان: " else "Confidence: ",
                        fontSize = 10.sp,
                        color = SayvisSilverMuted
                    )
                    LinearProgressIndicator(
                        progress = { attribute.confidence },
                        modifier = Modifier
                            .width(50.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = statusColor,
                        trackColor = SayvisSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(attribute.confidence * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                SayvisText(
                    source = attribute.provenance,
                    fontSize = 10.sp,
                    color = SayvisSilverMuted,
                    markTranslated = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Confirm, Revoke, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (attribute.status != UicStatus.CONFIRMED) {
                    OutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SayvisGreenSuccess.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SayvisGreenSuccess, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "تأیید" else "Confirm", color = SayvisGreenSuccess, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (attribute.status != UicStatus.REVOKED) {
                    OutlinedButton(
                        onClick = onRevoke,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SayvisAmberWarning.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = SayvisAmberWarning, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "ابطال" else "Revoke", color = SayvisAmberWarning, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SayvisRedAlert, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUicAttributeDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onAdd: (UicCategory, String, String, String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(UicCategory.PREFERENCES) }
    var title by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isPersian) "افزودن مؤلفهٔ شناختی جدید" else "Add Cognitive Attribute",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = if (isPersian) selectedCat.displayNameFa else selectedCat.displayNameEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isPersian) "دسته‌بندی شناختی" else "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        UicCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (isPersian) cat.displayNameFa else cat.displayNameEn) },
                                onClick = {
                                    selectedCat = cat
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isPersian) "عنوان ویژگی" else "Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("uic_input_title")
                )

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (isPersian) "کلید شناختی (انگلیسی، مثلاً: daily_focus)" else "Key (e.g. daily_focus)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("uic_input_key")
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (isPersian) "مقدار / شرح گزاره" else "Value / Statement") },
                    modifier = Modifier.fillMaxWidth().testTag("uic_input_val")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && key.isNotBlank() && value.isNotBlank()) {
                        onAdd(selectedCat, title, key, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SayvisGold, contentColor = Color.Black),
                modifier = Modifier.testTag("uic_confirm_add_btn")
            ) {
                Text(if (isPersian) "ثبت در پرونده" else "Save Attribute", fontWeight = FontWeight.Bold)
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
