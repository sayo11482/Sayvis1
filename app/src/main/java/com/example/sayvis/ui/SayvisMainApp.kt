package com.example.sayvis.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.model.MissionStatus
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.ui.components.LocalTranslation
import com.example.sayvis.ui.screens.AwareScreen
import com.example.sayvis.ui.screens.AvatarListenScreen
import com.example.sayvis.ui.screens.ChatScreen
import com.example.sayvis.ui.screens.HomeScreen
import com.example.sayvis.ui.screens.MissionsScreen
import com.example.sayvis.ui.screens.ScriptsScreen
import com.example.sayvis.ui.screens.SecurityDevicesScreen
import com.example.sayvis.ui.screens.SettingsScreen
import com.example.sayvis.ui.screens.SimulationTradingScreen
import com.example.sayvis.ui.screens.ToolCounts
import com.example.sayvis.ui.screens.ToolsScreen
import com.example.sayvis.ui.screens.TradingGatewayScreen
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

/**
 * Application shell.
 *
 * The bottom bar now carries exactly four general categories — Home, Assistant, Tools
 * and Settings — instead of the previous seven feature tabs. Every concrete capability
 * is reached from Tools or Settings, where there is room to label it properly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SayvisMainApp(viewModel: SayvisViewModel) {
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val isPersian by viewModel.isPersian.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val emergencyLockActive by viewModel.emergencyLockActive.collectAsState()
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsState()
    val avatarState by viewModel.avatarState.collectAsState()
    val contextSnapshot by viewModel.contextSnapshot.collectAsState()
    val ownerAccount by viewModel.ownerAccount.collectAsState()
    val ownerSignedIn by viewModel.ownerSignedIn.collectAsState()
    val pairingOffer by viewModel.pairingOffer.collectAsState()
    val accountMessage by viewModel.accountMessage.collectAsState()

    val chatMessages by viewModel.chatMessages.collectAsState()
    val uicAttributes by viewModel.uicAttributes.collectAsState()
    val awareOpportunities by viewModel.awareOpportunities.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val auditEvents by viewModel.auditEvents.collectAsState()

    val gatewayState by viewModel.gatewayState.collectAsState()
    val gatewayBusy by viewModel.gatewayBusy.collectAsState()
    val lastOrder by viewModel.lastOrder.collectAsState()
    val scripts by viewModel.scripts.collectAsState()
    val lastScriptRun by viewModel.lastScriptRun.collectAsState()

    val probe by viewModel.probe.collectAsState()
    val isProbing by viewModel.isProbing.collectAsState()

    val strings = remember(isPersian) { SayvisStrings.of(isPersian) }
    val layoutDirection = if (isPersian && settings.localization.forceRtlForPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalStrings provides strings,
        LocalTranslation provides viewModel.translationBridge
    ) {
        Scaffold(
            containerColor = SayvisDeepSpace,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (!currentScreen.isPrimary()) {
                            IconButton(
                                onClick = { viewModel.navigateBack() },
                                modifier = Modifier.testTag("nav_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.backToTools,
                                    tint = SayvisSilver
                                )
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentScreen.isPrimary() && currentScreen == SayvisScreen.HOME) {
                                Text(
                                    text = "SAYVIS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = SayvisCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.appName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SayvisGold
                                )
                            } else {
                                Text(
                                    text = currentScreen.title(isPersian),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SayvisSilver
                                )
                            }
                        }
                    },
                    actions = {
                        // Offline mode toggle
                        IconButton(
                            onClick = { viewModel.toggleOfflineMode() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("toggle_offline_button")
                        ) {
                            Icon(
                                imageVector = if (forceOfflineMode) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = strings.offlineMode,
                                tint = if (forceOfflineMode) SayvisAmberWarning else SayvisGreenSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Emergency lock kill switch
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
                                    contentDescription = strings.emergencyLock,
                                    tint = if (emergencyLockActive) SayvisRedAlert else SayvisSilverMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (emergencyLockActive) strings.emergencyLockOn else strings.emergencyLockOff,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (emergencyLockActive) SayvisRedAlert else SayvisSilver
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Language toggle
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("language_toggle_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = strings.language,
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
                        NavDest(SayvisScreen.HOME, strings.navHome, Icons.Default.Home, "nav_home"),
                        NavDest(SayvisScreen.ASSISTANT, strings.navAssistant, Icons.Default.Chat, "nav_assistant"),
                        NavDest(SayvisScreen.TOOLS, strings.navTools, Icons.Default.Apps, "nav_tools"),
                        NavDest(SayvisScreen.SETTINGS, strings.navSettings, Icons.Default.Settings, "nav_settings")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentScreen == item.screen ||
                                (!currentScreen.isPrimary() && currentScreen.primaryTab() == item.screen)
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.navigateTo(item.screen) }
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                                    .testTag(item.testTag),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) SayvisCyan else SayvisSilverMuted,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.5.sp,
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
                        persianDigits = settings.localization.persianDigits,
                        onNavigate = { viewModel.navigateTo(it) },
                        onSendMessage = { viewModel.sendMessage(it) },
                        onApproveOpportunity = { viewModel.approveOpportunity(it) },
                        onDismissOpportunity = { viewModel.dismissOpportunity(it) },
                        onToggleEmergencyLock = { viewModel.toggleEmergencyLock() }
                    )

                    SayvisScreen.ASSISTANT -> ChatScreen(
                        messages = chatMessages,
                        avatarState = avatarState,
                        isPersian = isPersian,
                        onSendMessage = { viewModel.sendMessage(it) },
                        pendingAction = viewModel.pendingAction.collectAsState().value,
                        onApproveAction = { viewModel.approvePendingAction() },
                        onDismissAction = { viewModel.dismissPendingAction() },
                        voiceListening = viewModel.voiceListening.collectAsState().value,
                        voiceAvailable = viewModel.voiceAvailable,
                        onVoiceInput = { viewModel.startVoiceInput() }
                    )

                    SayvisScreen.TOOLS -> ToolsScreen(
                        counts = ToolCounts(
                            missions = missions.count { it.status == MissionStatus.ACTIVE },
                            opportunities = awareOpportunities.count { it.status == OpportunityStatus.PENDING },
                            uicAttributes = uicAttributes.size,
                            devices = devices.count { it.isTrusted && !it.isRevoked },
                            scripts = scripts.size,
                            gatewayConnected = gatewayState.isConnected,
                            persianDigits = settings.localization.persianDigits
                        ),
                        isPersian = isPersian,
                        onNavigate = { viewModel.navigateTo(it) }
                    )

                    SayvisScreen.SETTINGS -> SettingsScreen(
                        settings = settings,
                        onSettingsChange = { next -> viewModel.updateSettings { next } },
                        probe = probe,
                        isProbing = isProbing,
                        gatewayState = gatewayState,
                        scriptCount = scripts.size,
                        vaultHardwareBacked = viewModel.vaultHardwareBacked,
                        translationCacheSize = viewModel.translationCacheSize,
                        appVersion = com.example.BuildConfig.VERSION_NAME,
                        onTestConnection = { viewModel.testAiConnection() },
                        onOpenGateway = { viewModel.navigateTo(SayvisScreen.GATEWAY) },
                        onOpenScripts = { viewModel.navigateTo(SayvisScreen.SCRIPTS) },
                        onClearTranslationCache = { viewModel.clearTranslationCache() },
                        onResetAll = { viewModel.resetAllSettings() },
                        onToggleEmergencyLock = { viewModel.toggleEmergencyLock() },
                        isPersian = isPersian
                    )

                    SayvisScreen.MISSIONS -> MissionsScreen(
                        missions = missions,
                        isPersian = isPersian,
                        onToggleTask = { missionId, taskId, completed -> viewModel.toggleMissionTask(missionId, taskId, completed) },
                        onAddMission = { mission -> coroutineScope.launch { viewModel.repository.addMission(mission) } }
                    )

                    SayvisScreen.UIC -> UicScreen(
                        attributes = uicAttributes,
                        isPersian = isPersian,
                        onConfirmStatus = { viewModel.confirmUicAttribute(it) },
                        onRevokeStatus = { viewModel.revokeUicAttribute(it) },
                        onDeleteAttribute = { viewModel.deleteUicAttribute(it) },
                        onAddAttribute = { category, title, key, value ->
                            viewModel.addCustomUicAttribute(category, title, key, value)
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
                            persianDigits = settings.localization.persianDigits,
                            onApproveOpportunity = { viewModel.approveOpportunity(it) },
                            onDismissOpportunity = { viewModel.dismissOpportunity(it) },
                            onRunScan = { viewModel.runAwareScan() }
                        )
                    }

                    SayvisScreen.SECURITY -> SecurityDevicesScreen(
                        devices = devices,
                        auditEvents = auditEvents,
                        emergencyLockActive = emergencyLockActive,
                        isPersian = isPersian,
                        onToggleEmergencyLock = { viewModel.toggleEmergencyLock() },
                        onToggleDeviceTrust = { deviceId, trust -> viewModel.toggleDeviceTrust(deviceId, trust) },
                        onRevokeDevice = { viewModel.revokeDevice(it) },
                        ownerAccount = ownerAccount,
                        ownerSignedIn = ownerSignedIn,
                        pairingOffer = pairingOffer,
                        accountMessage = accountMessage,
                        onClearAccountMessage = { viewModel.clearAccountMessage() },
                        onRegisterOwner = { e, p -> viewModel.registerOwner(e, p) },
                        onSignInOwner = { e, p -> viewModel.signInOwner(e, p) },
                        onSignOutOwner = { viewModel.signOutOwner() },
                        onChangePassword = { c, n -> viewModel.changeOwnerPassword(c, n) },
                        onStartPairing = { viewModel.startPairing() },
                        onCancelPairing = { viewModel.cancelPairing() },
                        onCompletePairing = { name, type, fp, proof -> viewModel.completePairing(name, type, fp, proof) }
                    )

                    SayvisScreen.TRADING -> SimulationTradingScreen(
                        lifeScenarios = viewModel.lifeScenarios,
                        tradingGate = viewModel.tradingGate,
                        litSignals = viewModel.litSignals,
                        isPersian = isPersian,
                        initialTab = 1
                    )

                    SayvisScreen.SIMULATION -> SimulationTradingScreen(
                        lifeScenarios = viewModel.lifeScenarios,
                        tradingGate = viewModel.tradingGate,
                        litSignals = viewModel.litSignals,
                        isPersian = isPersian,
                        initialTab = 0
                    )

                    SayvisScreen.GATEWAY -> TradingGatewayScreen(
                        profile = settings.trading,
                        state = gatewayState,
                        lastOrder = lastOrder,
                        isBusy = gatewayBusy,
                        emergencyLockActive = emergencyLockActive,
                        persianDigits = settings.localization.persianDigits,
                        isPersian = isPersian,
                        onConnect = { viewModel.connectGateway(it) },
                        onDisconnect = { viewModel.disconnectGateway() },
                        onTest = { viewModel.testGateway(it) },
                        onSaveProfile = { viewModel.saveGatewayProfile(it) },
                        onRefresh = { viewModel.refreshGateway() },
                        onExecutionModeChange = { viewModel.changeExecutionMode(it) },
                        onPlaceOrder = { viewModel.placeOrder(it) },
                        onClosePosition = { viewModel.closePosition(it) }
                    )

                    SayvisScreen.SCRIPTS -> ScriptsScreen(
                        scripts = scripts,
                        lastRun = lastScriptRun,
                        automationEnabled = settings.runAutomationScripts,
                        persianDigits = settings.localization.persianDigits,
                        isPersian = isPersian,
                        onRun = { viewModel.runScript(it) },
                        onSave = { viewModel.saveScript(it) },
                        onDelete = { viewModel.deleteScript(it) },
                        onToggleEnabled = { id, enabled -> viewModel.toggleScript(id, enabled) },
                        onAskAssistant = { viewModel.askAssistantToScript(it) }
                    )

                    SayvisScreen.AVATAR -> AvatarListenScreen(
                        isPersian = isPersian,
                        persianDigits = settings.localization.persianDigits
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
