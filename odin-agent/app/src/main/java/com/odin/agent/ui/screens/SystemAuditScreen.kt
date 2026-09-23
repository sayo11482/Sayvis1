package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.testing.FullSystemAudit
import com.odin.agent.testing.SystemAuditReport
import com.odin.agent.trading.GapImpact
import com.odin.agent.trading.GapStatus
import com.odin.agent.trading.SoftwareGapAnalyzer
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ODIN v1.0.27 - System Audit Screen - Odin.trade
 * تست کامل سیستم: اینترنت + امنیت + معامله دمو + بک‌تست + تحلیل کمبودها
 * گزارش حرفه‌ای فارسی - چه چیز کم داره تا سودسازتر بشه
 */

@Composable
fun SystemAuditScreen(isPersian: Boolean) {
    val scope = rememberCoroutineScope()
    val audit = remember { FullSystemAudit() }
    val gapAnalyzer = remember { SoftwareGapAnalyzer() }
    var report by remember { mutableStateOf<SystemAuditReport?>(null) }
    var running by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------- هدر + دکمه اجرا ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGreen), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "تست کامل سیستم - آدیت حرفه‌ای" else "Full System Audit - Professional", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "اینترنت + امنیت + معامله حساب دمو + بک‌تست + تحلیل کمبودها" else "Internet + Security + Demo Trading + Backtest + Gap Analysis", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            running = true; report = null
                            scope.launch {
                                report = audit.runFullAudit()
                                running = false
                            }
                        },
                        enabled = !running,
                        colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, OdinGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (running) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = OdinGreen, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPersian) "در حال اجرای تست کامل..." else "Running full audit...", color = OdinGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        } else {
                            Text(if (isPersian) "▶ اجرای تست کامل - اینترنت + امنیت + دمو + بک‌تست" else "▶ Run Full Audit", color = OdinGreen, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ---------- گزارش ----------
        report?.let { rep ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (rep.allPassed) OdinGreen else Color(0xFFFFB74D)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${rep.totalPassed}/${rep.totalTests}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (rep.allPassed) OdinGreen else Color(0xFFFFB74D))
                                Text(if (isPersian) "تست پاس شد" else "tests passed", fontSize = 8.sp, color = OdinSilverMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${rep.durationMs}ms", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                                Text(rep.jalali.take(15), fontSize = 8.sp, color = OdinSilverMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(rep.verdictFa, fontSize = 10.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // بخش‌ها
            items(rep.sections) { sec ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (sec.allPassed) OdinGreen.copy(alpha = 0.5f) else Color(0xFFFF5252).copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(sec.sectionFa, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("${sec.passed}/${sec.total}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (sec.allPassed) OdinGreen else Color(0xFFFF5252))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        sec.results.forEach { r ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (r.passed) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (r.passed) OdinGreen else Color(0xFFFF5252), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(r.testName.take(46), fontSize = 9.sp, color = OdinSilver, fontWeight = FontWeight.Bold)
                                }
                                Text("${r.durationMs}ms", fontSize = 8.sp, color = OdinSilverMuted)
                            }
                            Text(r.details.take(110), fontSize = 8.sp, color = OdinSilverMuted, modifier = Modifier.padding(start = 18.dp))
                        }
                    }
                }
            }

            // کمبودها
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isPersian) "تحلیل کمبودها - چه چیزهایی کم داره تا سودسازتر بشه" else "Gap Analysis - What's Missing To Be More Profitable", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("${rep.gapSummary.missing}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252)); Text(if (isPersian) "نصب نیست" else "Missing", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${rep.gapSummary.partial}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB74D)); Text(if (isPersian) "نیمه‌کاره" else "Partial", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${rep.gapSummary.exists}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGreen); Text(if (isPersian) "موجود" else "Exists", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column(horizontalAlignment = Alignment.End) { Text("${rep.gapSummary.totalEffortDays}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinCyan); Text(if (isPersian) "روز کاری" else "Work days", fontSize = 8.sp, color = OdinSilverMuted) }
                        }
                    }
                }
            }

            items(gapAnalyzer.analyze().take(12)) { gap ->
                val statusColor = when (gap.status) { GapStatus.EXISTS -> OdinGreen; GapStatus.PARTIAL -> Color(0xFFFFB74D); GapStatus.MISSING -> Color(0xFFFF5252) }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (gap.impact == GapImpact.CRITICAL) Color(0xFFFF5252).copy(alpha = 0.6f) else OdinSilverDim.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (gap.impact == GapImpact.CRITICAL) Icons.Default.Warning else Icons.Default.Info, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(gap.titleFa, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Text(gap.status.labelFa, fontSize = 9.sp, fontWeight = FontWeight.Black, color = statusColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(gap.descriptionFa, fontSize = 8.sp, color = OdinSilver)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("✓ ${gap.solutionFa}", fontSize = 8.sp, color = OdinGreen)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isPersian) "تاثیر سود: ${gap.impact.labelFa}" else "Impact: ${gap.impact.name}", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                            Text("${gap.category} | ${gap.effortDays} روز | اولویت ${gap.priorityScore}", fontSize = 8.sp, color = OdinSilverMuted)
                        }
                    }
                }
            }

            // نقشه راه
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isPersian) "نقشه راه نسخه سودسازتر" else "Roadmap - More Profitable Version", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        rep.roadmap.forEach { step ->
                            Text("→ $step", fontSize = 9.sp, color = OdinCyan, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }
        }

        item {
            Text(if (isPersian) "تست کامل Odin.trade v1.0.27 - اینترنت واقعی + امنیت زنجیره هش + معامله دمو + بک‌تست + تحلیل کمبود - 100% REAL" else "Odin.trade v1.0.27 Full Audit - real internet + hash security + demo trading + backtest + gaps - 100% REAL", fontSize = 8.sp, color = OdinSilverMuted, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}
