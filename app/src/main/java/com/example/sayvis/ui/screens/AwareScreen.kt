package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.i18n.ContextLocalization
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.PatternDetection
import com.example.sayvis.ui.components.ActionProposalCard
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun AwareScreen(
    contextSnapshot: ContextSnapshot,
    opportunities: List<AwareOpportunity>,
    patterns: List<PatternDetection>,
    isPersian: Boolean,
    persianDigits: Boolean = isPersian,
    onApproveOpportunity: (String) -> Unit,
    onDismissOpportunity: (String) -> Unit,
    onRunScan: () -> Unit,
    sportsSuggestions: List<com.example.sayvis.ai.SearchTasteEngine.Suggestion> = emptyList(),
    tasteSearchCount: Int = 0,
    onImportTaste: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pendingOpps = opportunities.filter { it.status == OpportunityStatus.PENDING }
    val resolvedOpps = opportunities.filter { it.status != OpportunityStatus.PENDING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("aware_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isPersian) "موتور ادراک و فرصت AWARE" else "AWARE Context & Opportunity Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPersian) "تشخیص الگو • نظارت بر بار شناختی • پیشنهادات کنشی" else "Pattern detection • Cognitive load monitor • Proactive interventions",
                            fontSize = 11.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }
            }
        }

        // Live Context & Baseline Telemetry Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
                border = BorderStroke(1.dp, SayvisBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "وضعیت زنده زمینه و شناخت" else "Live Context & Baseline Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SayvisCyan
                        )

                        Button(
                            onClick = onRunScan,
                            colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("run_aware_scan_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "اسکن فوری" else "Scan Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryMetric(
                            title = if (isPersian) "پنجرهٔ تمرکز" else "Focus window",
                            value = ContextLocalization.focusWindow(contextSnapshot, isPersian, persianDigits),
                            icon = Icons.Default.Psychology,
                            tint = SayvisGold,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetric(
                            title = if (isPersian) "بار شناختی" else "Cognitive load",
                            value = ContextLocalization.cognitiveLoad(contextSnapshot, isPersian),
                            icon = Icons.Default.AutoGraph,
                            tint = if (contextSnapshot.blockedTasksCount > 0) SayvisAmberWarning else SayvisGreenSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryMetric(
                            title = if (isPersian) "مأموریت‌های فعال" else "Active missions",
                            value = if (isPersian) {
                                "${PersianFormat.digits(contextSnapshot.activeMissionsCount.toString(), persianDigits)} فعال " +
                                    "(${PersianFormat.digits(contextSnapshot.blockedTasksCount.toString(), persianDigits)} مسدود)"
                            } else {
                                "${contextSnapshot.activeMissionsCount} active (${contextSnapshot.blockedTasksCount} blocked)"
                            },
                            icon = Icons.Default.Bolt,
                            tint = SayvisCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMetric(
                            title = if (isPersian) "امنیت شبکه" else "Network gateway",
                            value = ContextLocalization.network(contextSnapshot, isPersian),
                            icon = Icons.Default.Shield,
                            tint = if (contextSnapshot.isOnline) SayvisGreenSuccess else SayvisSilverMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Detected Patterns
        if (patterns.isNotEmpty()) {
            item {
                Text(
                    text = if (isPersian) "الگوهای کشف‌شده توسط AWARE" else "Detected Behavioral & Work Patterns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SayvisGold
                )
            }

            items(patterns) { pat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, SayvisBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SayvisText(
                                source = pat.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                markTranslated = true
                            )
                            Text(
                                text = if (isPersian) {
                                    "${PersianFormat.percent(pat.confidence.toDouble(), 0, persianDigits)} اطمینان"
                                } else {
                                    "${(pat.confidence * 100).toInt()}% conf"
                                },
                                fontSize = 11.sp,
                                color = SayvisCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SayvisText(
                            source = pat.description,
                            fontSize = 12.sp,
                            color = SayvisSilverMuted,
                            markTranslated = true
                        )
                    }
                }
            }
        }

        // Pending Action Proposals
        item {
            Text(
                text = if (isPersian) {
                    "فرصت‌های در انتظار تأیید (${PersianFormat.digits(pendingOpps.size.toString(), persianDigits)})"
                } else {
                    "Pending Action Proposals (${pendingOpps.size})"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (pendingOpps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isPersian) "در حال حاضر هیچ پیشنهاد اقدامی معلق نیست." else "No pending proactive action proposals.",
                            color = SayvisSilverMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(pendingOpps, key = { it.id }) { opp ->
                ActionProposalCard(
                    opportunity = opp,
                    isPersian = isPersian,
                    onApprove = { onApproveOpportunity(opp.id) },
                    onDismiss = { onDismissOpportunity(opp.id) }
                )
            }
        }

        // Executed or Dismissed History
        if (resolvedOpps.isNotEmpty()) {
            item {
                Text(
                    text = if (isPersian) "اقدامات پیشین AWARE" else "Executed & Dismissed History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SayvisSilverMuted
                )
            }

            items(resolvedOpps.take(3), key = { it.id }) { opp ->
                ActionProposalCard(
                    opportunity = opp,
                    isPersian = isPersian,
                    onApprove = {},
                    onDismiss = {}
                )
            }
        }

        // =================== SPORTS TASTE SUGGESTIONS (v5.0.0) ===================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.example.sayvis.ui.theme.SayvisSurface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = com.example.sayvis.ui.theme.SayvisCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "پیشنهاد ورزشی بر اساس سلیقهٔ شما" else "Sports picks from your taste",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = com.example.sayvis.ui.theme.SayvisSilver
                    )
                }
                Text(
                    text = if (isPersian)
                        "بر اساس $tasteSearchCount جستجوی اخیر شما (کالیستنیکس، تنیس و…) — تاریخچهٔ گوگل هم قابل چسباندن است."
                    else
                        "From your last $tasteSearchCount searches (calisthenics, tennis, …) — you can also paste your Google activity.",
                    fontSize = 10.5.sp,
                    color = com.example.sayvis.ui.theme.SayvisSilverMuted
                )
                sportsSuggestions.forEach { sug ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(com.example.sayvis.ui.theme.SayvisSurfaceVariant)
                            .padding(10.dp)
                            .testTag("sports_suggestion")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sug.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = com.example.sayvis.ui.theme.SayvisCyan,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = sug.tag,
                                fontSize = 9.5.sp,
                                color = com.example.sayvis.ui.theme.SayvisSilverMuted
                            )
                        }
                        Text(
                            text = sug.body,
                            fontSize = 11.sp,
                            color = com.example.sayvis.ui.theme.SayvisSilver
                        )
                        Text(
                            text = "💡 " + sug.reason,
                            fontSize = 9.5.sp,
                            color = com.example.sayvis.ui.theme.SayvisSilverMuted
                        )
                    }
                }
                var importText by remember { mutableStateOf("") }
                androidx.compose.material3.OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (isPersian) "متن فعالیت اخیر گوگل (اختیاری)…" else "Paste recent Google activity (optional)…",
                            fontSize = 11.sp,
                            color = com.example.sayvis.ui.theme.SayvisSilverMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sports_import_field")
                )
                Button(
                    onClick = { if (importText.isNotBlank()) onImportTaste(importText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.sayvis.ui.theme.SayvisSurfaceVariant,
                        contentColor = com.example.sayvis.ui.theme.SayvisSilver
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("sports_import_btn")
                ) {
                    Text(
                        if (isPersian) "شخصی‌سازی از تاریخچهٔ چسبانده‌شده" else "Personalise from pasted history",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun TelemetryMetric(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070A0F))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 10.sp, color = SayvisSilverMuted)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
