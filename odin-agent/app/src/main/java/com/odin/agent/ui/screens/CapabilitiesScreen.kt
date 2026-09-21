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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.trading.GitHubSelfUpgradeManager
import com.odin.agent.ui.theme.*

/**
 * ODIN v1.0.26 - Capabilities Screen - Odin.trade
 * قابلیت‌ها و ابزار - نمایش قابلیت‌های نصب شده از گیت هاب + گرافیک
 */

@Composable
fun CapabilitiesScreen(isPersian: Boolean, onNavigateToUpgrade: () -> Unit) {
    val manager = remember { GitHubSelfUpgradeManager() }
    var state by remember { mutableStateOf(manager.state.value) }

    LaunchedEffect(Unit) {
        manager.state.collect { newState -> state = newState }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = OdinGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = if (isPersian) "Odin.trade - قابلیت‌ها و ابزار - نصب شده" else "Odin.trade - Capabilities & Tools - Installed", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(text = if (isPersian) "${state.installedCapabilities.size} قابلیت نصب شده از گیت هاب - ترید و مالی" else "${state.installedCapabilities.size} capabilities from GitHub - Trading & Finance", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) "ابزارهای اصلی Odin.trade" else "Core Tools Odin.trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Button(onClick = onNavigateToUpgrade, colors = ButtonDefaults.buttonColors(containerColor = OdinGold.copy(alpha = 0.2f)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "افزودن قابلیت گیت هاب" else "Add GitHub Capability", fontSize = 9.sp, color = OdinGold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // ابزارهای اصلی
                    val coreTools = listOf(
                        Triple("Arena AI Agent", "ایجنت آرنا AI", "🤖"),
                        Triple("Real Market Data Vittaverse", "داده واقعی ویتاورس", "📊"),
                        Triple("Scanner Symbol Select", "اسکنر انتخاب نماد", "🔍"),
                        Triple("Strategy Checked Display", "نمایش استراتژی بررسی", "📈"),
                        Triple("Spread Calculation", "محاسبه اسپرد", "💰"),
                        Triple("Capital Adjustable", "سرمایه قابل تنظیم", "💵"),
                        Triple("Backtest 10$ Base", "بک‌تست پایه 10$", "📉"),
                        Triple("Investment Outcome 10$", "برآیند 10$ واقعی", "💼")
                    )
                    coreTools.forEach { (en, fa, icon) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isPersian) fa else en, fontSize = 9.sp, color = Color.White, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGreen.copy(alpha = 0.15f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                Text(text = if (isPersian) "فعال" else "Active", fontSize = 7.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (state.installedCapabilities.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ExtensionOff, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = if (isPersian) "هیچ قابلیت گیت هاب نصب نشده - از خود ارتقایی اضافه کنید" else "No GitHub capability installed - Add from self-upgrade", fontSize = 10.sp, color = OdinSilverMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = onNavigateToUpgrade, colors = ButtonDefaults.buttonColors(containerColor = OdinGold.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Text(text = if (isPersian) "رفتن به خود ارتقایی گیت هاب" else "Go to GitHub Self-Upgrade", fontSize = 9.sp, color = OdinGold)
                            }
                        }
                    }
                }
            }
        }

        items(state.installedCapabilities) { cap ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = cap.category.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = if (isPersian) cap.nameFa else cap.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = cap.repoName, fontSize = 8.sp, color = OdinCyan)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = if (isPersian) "نصب شده" else "Installed", fontSize = 7.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = if (isPersian) cap.descriptionFa else cap.description, fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black), border = BorderStroke(1.dp, Color(0xFF1A1A1A)), shape = RoundedCornerShape(6.dp)) {
                        Text(text = cap.codeSnippet.take(200) + "...", fontSize = 7.sp, color = OdinGreen, modifier = Modifier.padding(6.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
