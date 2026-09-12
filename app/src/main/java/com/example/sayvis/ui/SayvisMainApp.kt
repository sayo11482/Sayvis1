package com.example.sayvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.ui.screens.AwareScreen
import com.example.sayvis.ui.screens.ChatScreen
import com.example.sayvis.ui.screens.HomeScreen
import com.example.sayvis.ui.screens.MissionsScreen
import com.example.sayvis.ui.screens.SecurityDevicesScreen
import com.example.sayvis.ui.screens.SimulationTradingScreen
import com.example.sayvis.ui.screens.UicScreen
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SayvisMainApp(viewModel: SayvisViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val isPersian by viewModel.isPersian.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val emergencyLockActive by viewModel.emergencyLockActive.collectAsState()
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsState()
    val avatarState by viewModel.avatarState.collectAsState()
    val contextSnapshot by viewModel.contextSnapshot.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val uicAttributes by viewModel.uicAttributes.collectAsState()
    val awareOpportunities by viewModel.awareOpportunities.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val auditEvents by viewModel.auditEvents.collectAsState()
    val tradingGate by viewModel.tradingGate.collectAsState()

    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            containerColor = SayvisDeepSpace,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SAYVIS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = SayvisCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سایویس",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SayvisGold
                            )
                        }
                    },
                    actions = {
                        // Offline Mode Toggle Chip
                        IconButton(
                            onClick = { viewModel.toggleOfflineMode() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_offline_button")
                        ) {
                            Icon(
                                imageVector = if (forceOfflineMode) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = "Offline Mode",
                                tint = if (forceOfflineMode) SayvisAmberWarning else SayvisGreenSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Emergency Lock Killswitch Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (emergencyLockActive) SayvisRedAlert.copy(alpha = 0.25f) else SayvisSurfaceVariant)
                                .clickable { viewModel.toggleEmergencyLock() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("topbar_emergency_lock")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (emergencyLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Emergency Lock",
                                    tint = if (emergencyLockActive) SayvisRedAlert else SayvisSilverMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (emergencyLockActive) (if (isPersian) "قفل اضطراری" else "LOCKED") else (if (isPersian) "امنیت عادی" else "UNLOCKED"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (emergencyLockActive) SayvisRedAlert else SayvisSilver
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Language Toggle Button
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("language_toggle_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = SayvisGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isPersian) "FA" else "EN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SayvisGold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SayvisSurface,
                        titleContentColor = SayvisSilver
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = SayvisSurface,
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SayvisBorder)
                ) {
                    val navItems = listOf(
                        NavDest(SayvisScreen.HOME, if (isPersian) "مرکز" else "Home", Icons.Default.Home, "nav_home"),
                        NavDest(SayvisScreen.CHAT, if (isPersian) "هوش سایو" else "AI Chat", Icons.Default.Chat, "nav_chat"),
                        NavDest(SayvisScreen.UIC, if (isPersian) "شناختی" else "UIC", Icons.Default.Psychology, "nav_uic"),
                        NavDest(SayvisScreen.AWARE, if (isPersian) "ادراک" else "AWARE", Icons.Default.Radar, "nav_aware"),
                        NavDest(SayvisScreen.MISSIONS, if (isPersian) "مأموریت‌ها" else "Missions", Icons.Default.Flag, "nav_missions"),
                        NavDest(SayvisScreen.SECURITY, if (isPersian) "امنیت" else "Security", Icons.Default.Security, "nav_security"),
                        NavDest(SayvisScreen.SIMULATION, if (isPersian) "شبیه‌ساز" else "Simulation", Icons.Default.AutoGraph, "nav_sim")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentScreen == item.screen
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.navigateTo(item.screen) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag(item.testTag),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) SayvisCyan else SayvisSilverMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SayvisCyan else SayvisSilverMuted
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    SayvisScreen.HOME -> HomeScreen(
                        avatarState = avatarState,
                        contextSnapshot = contextSnapshot,
                        activeMission = missions.firstOrNull { it.status == MissionStatus.ACTIVE },
                        pendingOpportunities = awareOpportunities.filter { it.status == OpportunityStatus.PENDING },
                        isPersian = isPersian,
                        emergencyLockActive = emergencyLockActive,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSendMessage = { viewModel.sendMessage(it) },
                        onApproveOpportunity = { viewModel.approveOpportunity(it) },
                        onDismissOpportunity = { viewModel.dismissOpportunity(it) },
                        onToggleEmergencyLock = { viewModel.toggleEmergencyLock() }
                    )

                    SayvisScreen.CHAT -> ChatScreen(
                        messages = chatMessages,
                        avatarState = avatarState,
                        isPersian = isPersian,
                        onSendMessage = { viewModel.sendMessage(it) }
                    )

                    SayvisScreen.UIC -> UicScreen(
                        attributes = uicAttributes,
                        isPersian = isPersian,
                        onConfirmStatus = { viewModel.confirmUicAttribute(it) },
                        onRevokeStatus = { viewModel.revokeUicAttribute(it) },
                        onDeleteAttribute = { viewModel.deleteUicAttribute(it) },
                        onAddAttribute = { cat, title, key, value ->
                            viewModel.addCustomUicAttribute(cat, title, key, value)
                        }
                    )

                    SayvisScreen.AWARE -> {
                        val patterns = remember(uicAttributes, missions) {
                            viewModel.awareEngine.detectPatterns(uicAttributes, missions)
                        }
                        AwareScreen(
                            contextSnapshot = contextSnapshot,
                            opportunities = awareOpportunities,
                            patterns = patterns,
                            isPersian = isPersian,
                            onApproveOpportunity = { viewModel.approveOpportunity(it) },
                            onDismissOpportunity = { viewModel.dismissOpportunity(it) },
                            onRunScan = { viewModel.runAwareScan() }
                        )
                    }

                    SayvisScreen.MISSIONS -> MissionsScreen(
                        missions = missions,
                        isPersian = isPersian,
                        onToggleTask = { mId, tId, comp -> viewModel.toggleMissionTask(mId, tId, comp) },
                        onAddMission = { mission ->
                            coroutineScope.launch {
                                viewModel.repository.addMission(mission)
                            }
                        }
                    )

                    SayvisScreen.SECURITY -> SecurityDevicesScreen(
                        devices = devices,
                        auditEvents = auditEvents,
                        emergencyLockActive = emergencyLockActive,
                        isPersian = isPersian,
                        onToggleEmergencyLock = { viewModel.toggleEmergencyLock() },
                        onToggleDeviceTrust = { devId, trust -> viewModel.toggleDeviceTrust(devId, trust) },
                        onRevokeDevice = { devId -> viewModel.revokeDevice(devId) }
                    )

                    SayvisScreen.SIMULATION -> SimulationTradingScreen(
                        lifeScenarios = viewModel.lifeScenarios,
                        tradingGate = tradingGate,
                        litSignals = viewModel.litSignals,
                        isPersian = isPersian
                    )
                }
            }
        }
    }
}

private data class NavDest(
    val screen: SayvisScreen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)
