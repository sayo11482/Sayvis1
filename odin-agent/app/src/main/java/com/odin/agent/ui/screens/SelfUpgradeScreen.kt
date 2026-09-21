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
import com.odin.agent.trading.CapabilityCategory
import com.odin.agent.trading.GitHubCapability
import com.odin.agent.trading.GitHubSelfUpgradeManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ODIN v1.0.26 - Self Upgrade Screen - Odin.trade
 * قابلیت خود ارتقایی گیت هاب - فیلتر ترید و مالی - فارسی - خواندن + تایید نصب - گرافیک حرفه‌ای
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfUpgradeScreen(isPersian: Boolean) {
    val manager = remember { GitHubSelfUpgradeManager() }
    var state by remember { mutableStateOf(manager.state.value) }
    var selectedCapability by remember { mutableStateOf<GitHubCapability?>(null) }
    var searchQuery by remember { mutableStateOf("trading quant forex") }
    var showInstalledOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        manager.state.collect { newState -> state = newState }
    }

    LaunchedEffect(Unit) {
        manager.searchTradingCapabilities(searchQuery)
    }

    if (selectedCapability != null) {
        CapabilityDetailScreen(
            capability = selectedCapability!!,
            isPersian = isPersian,
            onBack = { selectedCapability = null },
            onInstall = { cap ->
                scope.launch {
                    manager.installCapability(cap)
                    selectedCapability = null
                }
            }
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = OdinGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = if (isPersian) "Odin.trade - خود ارتقایی گیت هاب - ترید و مالی" else "Odin.trade - GitHub Self-Upgrade - Trading & Finance", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(text = if (isPersian) "فقط برنامه‌های مرتبط با ترید و مالی فیلتر می‌شود - فارسی - تایید نصب اتومات" else "Only trading & finance filtered - Persian - Confirm auto install", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) "قابلیت خود ارتقایی: جستجوی گیت هاب برای استراتژی، اندیکاتور، ربات، مدیریت ریسک، بک‌تست، هوش مصنوعی ترید - فقط مرتبط با ترید و مالی - پس از انتخاب قابلیت را به فارسی بخوانید و تایید نصب کنید - اتومات به ابزارها اضافه می‌شود - محیط گرافیکی حرفه‌ای - Odin.trade"
                        else "Self-upgrade: Search GitHub for strategies, indicators, bots, risk, backtest, AI trading - Only trading & finance - Read in Persian after selection and confirm install - Auto add to tools - Professional graphical - Odin.trade",
                        fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isPersian) "جستجوی قابلیت‌های ترید گیت هاب" else "Search GitHub Trading Capabilities", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (state.isSearching) OdinGold.copy(alpha = 0.15f) else OdinGreen.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(text = if (state.isSearching) if (isPersian) "در حال جستجو..." else "Searching..." else "${state.totalFound} یافت شد", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (state.isSearching) OdinGold else OdinGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(if (isPersian) "جستجو: trading, forex, crypto, quant, indicator" else "Search: trading, forex, crypto, quant, indicator", fontSize = 9.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (state.isSearching) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OdinGold, strokeWidth = 2.dp)
                            else IconButton(onClick = { scope.launch { manager.searchTradingCapabilities(searchQuery) } }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.filterTradingOnly, onClick = {}, label = { Text(if (isPersian) "فقط ترید و مالی" else "Only Trading & Finance", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGreen.copy(alpha = 0.2f), selectedLabelColor = OdinGreen))
                        FilterChip(selected = showInstalledOnly, onClick = { showInstalledOnly = !showInstalledOnly }, label = { Text(if (isPersian) "نصب شده‌ها" else "Installed", fontSize = 8.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f), selectedLabelColor = OdinGold))
                        Text(text = if (isPersian) "نصب شده: ${state.installedCapabilities.size}" else "Installed: ${state.installedCapabilities.size}", fontSize = 8.sp, color = OdinGoldLight, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = state.error!!, fontSize = 7.sp, color = OdinGold)
                    }
                }
            }
        }

        val displayList = if (showInstalledOnly) state.installedCapabilities else state.capabilities

        if (displayList.isEmpty() && !state.isSearching) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = if (isPersian) "قابلیت ترید یافت نشد - جستجو کنید" else "No trading capability found - Search", fontSize = 10.sp, color = OdinSilverMuted)
                        }
                    }
                }
            }
        }

        items(displayList) { capability ->
            CapabilityCard(
                capability = capability,
                isPersian = isPersian,
                onClick = { selectedCapability = capability },
                onInstall = { scope.launch { manager.installCapability(capability) } },
                onUninstall = { scope.launch { manager.uninstallCapability(capability.repoName) } }
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun CapabilityCard(
    capability: GitHubCapability,
    isPersian: Boolean,
    onClick: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (capability.isInstalled) OdinGreen.copy(alpha = 0.08f) else Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, if (capability.isInstalled) OdinGreen.copy(alpha = 0.4f) else when (capability.category) {
            CapabilityCategory.STRATEGY -> OdinGold.copy(alpha = 0.3f)
            CapabilityCategory.AI -> OdinCyan.copy(alpha = 0.3f)
            CapabilityCategory.RISK -> OdinGreen.copy(alpha = 0.3f)
            else -> OdinBorder
        }),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = capability.category.icon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (isPersian) capability.nameFa else capability.name, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        Text(text = capability.repoName, fontSize = 8.sp, color = OdinCyan, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = OdinGold, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${capability.stars}", fontSize = 8.sp, color = OdinGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = capability.language, fontSize = 7.sp, color = OdinSilverMuted)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGold.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                Text(text = if (isPersian) capability.category.labelFa else capability.category.labelEn, fontSize = 6.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (capability.isInstalled) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(text = if (isPersian) "● نصب شده" else "● Installed", fontSize = 7.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = if (isPersian) capability.descriptionFa else capability.description, fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp, maxLines = 3)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                capability.topics.take(3).forEach { topic ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1A1A)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(text = topic, fontSize = 7.sp, color = OdinSilverMuted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onClick, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = OdinSilver, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isPersian) "خواندن فارسی" else "Read Details", fontSize = 9.sp, color = OdinSilver)
                }
                if (capability.isInstalled) {
                    Button(onClick = onUninstall, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinRed.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = OdinRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isPersian) "حذف" else "Uninstall", fontSize = 9.sp, color = OdinRed)
                    }
                } else {
                    Button(onClick = onInstall, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.2f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isPersian) "نصب اتومات" else "Auto Install", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityDetailScreen(
    capability: GitHubCapability,
    isPersian: Boolean,
    onBack: () -> Unit,
    onInstall: (GitHubCapability) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text(text = if (isPersian) "جزئیات قابلیت - فارسی" else "Capability Details", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGold.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = capability.category.icon + " " + if (isPersian) capability.category.labelFa else capability.category.labelEn, fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isPersian) capability.nameFa else capability.name, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = capability.repoName, fontSize = 10.sp, color = OdinCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${capability.stars} Stars", fontSize = 10.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                        }
                        Text(text = capability.language, fontSize = 10.sp, color = OdinSilver)
                        Text(text = if (capability.isTradingRelated) if (isPersian) "مرتبط با ترید" else "Trading Related" else "General", fontSize = 9.sp, color = if (capability.isTradingRelated) OdinGreen else OdinSilverMuted)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "توضیحات فارسی - قابلیت ترید و مالی" else "Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = if (isPersian) capability.descriptionFa else capability.description, fontSize = 11.sp, color = Color.White, lineHeight = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = if (isPersian) "توضیحات اصلی:" else "Original:", fontSize = 9.sp, color = OdinSilverMuted)
                    Text(text = capability.description, fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "کد حرفه‌ای - محیط گرافیکی" else "Professional Code - Graphical", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black), border = BorderStroke(1.dp, Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                        Text(text = capability.codeSnippet, fontSize = 8.sp, color = OdinGreen, modifier = Modifier.padding(8.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, lineHeight = 10.sp)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "اطلاعات گیت هاب" else "GitHub Info", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "URL: ${capability.repoUrl}", fontSize = 8.sp, color = OdinCyan)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = if (isPersian) "موضوعات: ${capability.topics.joinToString()}" else "Topics: ${capability.topics.joinToString()}", fontSize = 8.sp, color = OdinSilverMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = if (isPersian) "دسته: ${capability.category.labelFa} - فقط ترید و مالی فیلتر شده" else "Category: ${capability.category.labelEn} - Only trading & finance filtered", fontSize = 8.sp, color = OdinSilverDim)
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(10.dp)) {
                    Text(text = if (isPersian) "بازگشت" else "Back", color = Color.White)
                }
                Button(
                    onClick = { onInstall(capability) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !capability.isInstalled
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isPersian) "تایید و نصب اتومات - اضافه به ابزارها" else "Confirm & Auto Install - Add to Tools", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isPersian) "پس از تایید، قابلیت به صورت اتومات نصب و به قسمت قابلیت‌ها/ابزار اضافه می‌شود با محیط گرافیکی حرفه‌ای - فقط ترید و مالی - Odin.trade"
                else "After confirm, capability auto installs and adds to Capabilities/Tools with professional graphical - Only trading & finance - Odin.trade",
                fontSize = 8.sp, color = OdinSilverDim, lineHeight = 10.sp
            )
        }
    }
}
