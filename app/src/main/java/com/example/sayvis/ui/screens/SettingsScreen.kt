package com.example.sayvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.sayvis.ai.GoogleLinkManager
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AppLanguage
import com.example.sayvis.settings.AiVisualStyle
import com.example.sayvis.settings.AppSettings
import com.example.sayvis.settings.AppearanceMode
import com.example.sayvis.settings.ProviderProbe
import com.example.sayvis.trading.MtConnectionPhase
import com.example.sayvis.trading.MtGatewayState
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisConfirmDialog
import com.example.sayvis.ui.components.SayvisDivider
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.components.SayvisOptionRow
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.components.AiStyleCanvas
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.components.pnlColor
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import kotlinx.coroutines.launch

/**
 * The single place where every owner-editable control lives.
 *
 * Replaces the previous situation where settings were scattered across the top bar or
 * simply did not exist: language, AI/API credentials, the trading gateway, automation,
 * security and data are all reachable from here.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    probe: ProviderProbe?,
    isProbing: Boolean,
    gatewayState: MtGatewayState,
    scriptCount: Int,
    vaultHardwareBacked: Boolean,
    translationCacheSize: Int,
    appVersion: String,
    onTestConnection: () -> Unit,
    onOpenGateway: () -> Unit,
    onOpenScripts: () -> Unit,
    onClearTranslationCache: () -> Unit,
    onResetAll: () -> Unit,
    onToggleEmergencyLock: () -> Unit,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val listenLevel by com.example.sayvis.voice.ListenBus.level.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var revealKey by remember { mutableStateOf(false) }
    var maxTokensText by remember(settings.ai.maxOutputTokens) { mutableStateOf(settings.ai.maxOutputTokens.toString()) }
    var timeoutText by remember(settings.ai.timeoutSeconds) { mutableStateOf(settings.ai.timeoutSeconds.toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = s.settingsTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.settingsSubtitle, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        // ============================================================ LANGUAGE
        SayvisSectionHeader(title = s.sectionLanguage, icon = Icons.Default.Language)

        SayvisCard {
            SayvisOptionRow(
                label = s.language,
                hint = s.languageHint,
                options = AppLanguage.entries.map { it.label(isPersian) },
                selectedIndex = AppLanguage.entries.indexOf(settings.localization.language),
                onSelect = { index ->
                    onSettingsChange(
                        settings.copy(localization = settings.localization.copy(language = AppLanguage.entries[index]))
                    )
                }
            )

            SayvisDivider()

            SayvisToggleRow(
                label = s.persianDigits,
                hint = s.persianDigitsHint,
                checked = settings.localization.persianDigits,
                onCheckedChange = { onSettingsChange(settings.copy(localization = settings.localization.copy(persianDigits = it))) }
            )

            SayvisToggleRow(
                label = s.autoTranslate,
                hint = s.autoTranslateHint,
                checked = settings.localization.autoTranslateFreeText,
                onCheckedChange = { onSettingsChange(settings.copy(localization = settings.localization.copy(autoTranslateFreeText = it))) }
            )

            SayvisToggleRow(
                label = s.markTranslated,
                hint = null,
                checked = settings.localization.markMachineTranslated,
                onCheckedChange = { onSettingsChange(settings.copy(localization = settings.localization.copy(markMachineTranslated = it))) }
            )

            SayvisToggleRow(
                label = s.forceRtl,
                hint = null,
                checked = settings.localization.forceRtlForPersian,
                onCheckedChange = { onSettingsChange(settings.copy(localization = settings.localization.copy(forceRtlForPersian = it))) }
            )

            SayvisDivider()

            SayvisOptionRow(
                label = s.appearance,
                options = AppearanceMode.entries.map { it.label(isPersian) },
                selectedIndex = AppearanceMode.entries.indexOf(settings.appearance),
                onSelect = { onSettingsChange(settings.copy(appearance = AppearanceMode.entries[it])) }
            )

            SayvisToggleRow(
                label = s.haptics,
                hint = null,
                checked = settings.hapticFeedback,
                onCheckedChange = { onSettingsChange(settings.copy(hapticFeedback = it)) }
            )
        }

        // ================================================== DEVICE PERMISSIONS
        SayvisSectionHeader(title = s.sectionPermissions, icon = Icons.Default.Security)

        SayvisCard {
            val appContext = androidx.compose.ui.platform.LocalContext.current
            var gateState by remember { mutableStateOf(0) }
            val cameraOk = remember(gateState) { com.example.sayvis.ui.components.checkPermission(appContext, android.Manifest.permission.CAMERA) }
            val locationOk = remember(gateState) {
                com.example.sayvis.ui.components.checkPermission(appContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    com.example.sayvis.ui.components.checkPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            val galleryOk = remember(gateState) { com.example.sayvis.ui.components.checkPermission(appContext, com.example.sayvis.ui.components.galleryPermission()) }
            val contactsOk = remember(gateState) { com.example.sayvis.ui.components.checkPermission(appContext, android.Manifest.permission.READ_CONTACTS) }
            val micOk = remember(gateState) { com.example.sayvis.ui.components.checkPermission(appContext, android.Manifest.permission.RECORD_AUDIO) }
            val notifOk = remember(gateState) { com.example.sayvis.voice.AvatarListenController.hasNotificationPermission(appContext) }

            val requestLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { gateState++ }

            PermissionStatusRow(s.permMic, micOk, s) { requestLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO)) }
            PermissionStatusRow(s.permCamera, cameraOk, s) { requestLauncher.launch(arrayOf(android.Manifest.permission.CAMERA)) }
            PermissionStatusRow(s.permLocation, locationOk, s) {
                requestLauncher.launch(arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
            PermissionStatusRow(s.permGallery, galleryOk, s) { requestLauncher.launch(arrayOf(com.example.sayvis.ui.components.galleryPermission())) }
            PermissionStatusRow(s.permContacts, contactsOk, s) { requestLauncher.launch(arrayOf(android.Manifest.permission.READ_CONTACTS)) }
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                PermissionStatusRow(s.permNotif, notifOk, s) { requestLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)) }
            }
            PermissionStatusRow(s.permOverlay, com.example.sayvis.voice.AvatarListenController.hasOverlayPermission(appContext), s) {
                appContext.startActivity(com.example.sayvis.voice.AvatarListenController.overlaySettingsIntent(appContext))
            }
        }

        // ==================================================== AI VISUAL STYLE
        SayvisSectionHeader(
            title = s.sectionVisual,
            subtitle = s.visualSubtitle,
            icon = Icons.Default.AutoAwesome
        )

        SayvisCard {
            // Live multidimensional preview of the selected style (voice-reactive).
            AiStyleCanvas(
                style = settings.visualStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("ai_style_preview"),
                level = listenLevel
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The four selectable views.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiVisualStyle.entries.forEach { style ->
                    val selected = style == settings.visualStyle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .clickable {
                                onSettingsChange(settings.copy(visualStyle = style))
                            }
                            .background(
                                if (selected) SayvisCyan.copy(alpha = 0.10f)
                                else com.example.sayvis.ui.theme.SayvisSurface.copy(alpha = 0.6f)
                            )
                            .padding(vertical = 8.dp)
                            .testTag("ai_style_" + style.name.lowercase()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AiStyleCanvas(
                            style = style,
                            modifier = Modifier.size(56.dp),
                            animate = true,
                            level = listenLevel
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = style.label(isPersian),
                            fontSize = 9.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) SayvisCyan else SayvisSilverMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            SayvisDivider()

            SayvisToggleRow(
                label = s.roboticSound,
                hint = s.roboticSoundHint,
                checked = settings.roboticVoiceReplies,
                onCheckedChange = { onSettingsChange(settings.copy(roboticVoiceReplies = it)) }
            )
        }

        // ================================================================= AI
        SayvisSectionHeader(
            title = s.sectionAi,
            subtitle = if (settings.ai.isProviderConfigured()) s.aiActive else s.aiInactive,
            icon = Icons.Default.Link
        )

        SayvisCard {
            // Live status strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SayvisStatusPill(
                    text = settings.ai.provider.label(isPersian),
                    color = if (settings.ai.isProviderConfigured()) SayvisGreenSuccess else SayvisAmberWarning
                )
                SayvisStatusPill(
                    text = if (settings.ai.isProviderConfigured()) s.apiStatusReady else s.apiStatusNeedsKey,
                    color = if (settings.ai.isProviderConfigured()) SayvisCyan else SayvisAmberWarning
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SayvisOptionRow(
                label = s.aiProvider,
                hint = s.aiProviderHint,
                options = AiProviderKind.entries.map { it.label(isPersian) },
                selectedIndex = AiProviderKind.entries.indexOf(settings.ai.provider),
                onSelect = { index -> onSettingsChange(settings.copy(ai = settings.ai.copy(provider = AiProviderKind.entries[index]))) }
            )

            SayvisDivider()

            // Provider-specific credentials
            when (settings.ai.provider) {
                AiProviderKind.LOCAL -> Text(
                    text = if (isPersian) "هستهٔ محلی نیازی به کلید یا اینترنت ندارد و همیشه در دسترس است."
                    else "The local core needs no key and no internet; it is always available.",
                    fontSize = 11.5.sp,
                    color = SayvisSilverMuted
                )

                AiProviderKind.GEMINI -> {
                    SecretField(
                        label = s.apiKey,
                        hint = "AIza…",
                        value = settings.ai.geminiApiKey,
                        reveal = revealKey,
                        onRevealChange = { revealKey = it },
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(geminiApiKey = it.trim()))) },
                        isPersian = isPersian
                    )
                    SayvisField(
                        label = s.model,
                        value = settings.ai.geminiModel,
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(geminiModel = it.trim()))) },
                        hint = "gemini-2.5-flash / gemini-2.5-pro",
                        monospace = true
                    )

                    // Sign in with Google → AI Studio → automatic key capture,
                    // live verification and vault storage.
                    GoogleAccountLinkCard(
                        activeKey = settings.ai.geminiApiKey,
                        isPersian = isPersian,
                        onKeyLinked = { linked ->
                            onSettingsChange(
                                settings.copy(
                                    ai = settings.ai.copy(
                                        geminiApiKey = linked,
                                        provider = AiProviderKind.GEMINI
                                    )
                                )
                            )
                        }
                    )
                }

                AiProviderKind.OPENROUTER -> {
                    SecretField(
                        label = s.apiKey,
                        hint = "sk-or-…",
                        value = settings.ai.openRouterApiKey,
                        reveal = revealKey,
                        onRevealChange = { revealKey = it },
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(openRouterApiKey = it.trim()))) },
                        isPersian = isPersian
                    )
                    SayvisField(
                        label = s.model,
                        value = settings.ai.openRouterModel,
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(openRouterModel = it.trim()))) },
                        hint = "anthropic/claude-3.5-sonnet",
                        monospace = true
                    )
                }

                AiProviderKind.GROQ -> {
                    SecretField(
                        label = s.apiKey,
                        hint = "gsk_…",
                        value = settings.ai.groqApiKey,
                        reveal = revealKey,
                        onRevealChange = { revealKey = it },
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(groqApiKey = it.trim()))) },
                        isPersian = isPersian
                    )
                    SayvisField(
                        label = s.model,
                        value = settings.ai.groqModel,
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(groqModel = it.trim()))) },
                        hint = "llama-3.3-70b-versatile",
                        monospace = true
                    )
                }

                AiProviderKind.CUSTOM -> {
                    SayvisField(
                        label = s.baseUrl,
                        value = settings.ai.customBaseUrl,
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(customBaseUrl = it.trim()))) },
                        hint = "https://api.example.com/v1",
                        keyboardType = KeyboardType.Uri,
                        monospace = true
                    )
                    SayvisField(
                        label = s.model,
                        value = settings.ai.customModel,
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(customModel = it.trim()))) },
                        hint = "model-id",
                        monospace = true
                    )
                    SecretField(
                        label = s.apiKey,
                        hint = if (isPersian) "اختیاری" else "optional",
                        value = settings.ai.customApiKey,
                        reveal = revealKey,
                        onRevealChange = { revealKey = it },
                        onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(customApiKey = it.trim()))) },
                        isPersian = isPersian
                    )
                }
            }

            SayvisDivider()

            SayvisField(
                label = s.persona,
                hint = s.personaHint,
                value = settings.ai.systemPersona,
                onValueChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(systemPersona = it))) },
                singleLine = false,
                minLines = 3
            )

            // Temperature
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = s.temperature, fontSize = 12.sp, color = SayvisSilver)
                    Text(
                        text = PersianFormat.number(settings.ai.temperature, 2, settings.localization.persianDigits),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisCyan
                    )
                }
                Slider(
                    value = settings.ai.temperature.toFloat(),
                    onValueChange = { value -> onSettingsChange(settings.copy(ai = settings.ai.copy(temperature = value.toDouble()))) },
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = SayvisCyan,
                        activeTrackColor = SayvisCyan,
                        inactiveTrackColor = SayvisSilverMuted.copy(alpha = 0.3f)
                    )
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisField(
                    label = s.maxTokens,
                    value = maxTokensText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(5)
                        maxTokensText = digits
                        digits.toIntOrNull()?.let { onSettingsChange(settings.copy(ai = settings.ai.copy(maxOutputTokens = it))) }
                    },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                SayvisField(
                    label = s.timeout,
                    value = timeoutText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(3)
                        timeoutText = digits
                        digits.toIntOrNull()?.let { onSettingsChange(settings.copy(ai = settings.ai.copy(timeoutSeconds = it))) }
                    },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            SayvisToggleRow(
                label = s.forceResponseLanguage,
                hint = null,
                checked = settings.ai.forceResponseLanguage,
                onCheckedChange = { onSettingsChange(settings.copy(ai = settings.ai.copy(forceResponseLanguage = it))) }
            )

            SayvisToggleRow(
                label = s.offlineMode,
                hint = s.offlineModeHint,
                checked = settings.forceOfflineMode,
                onCheckedChange = { onSettingsChange(settings.copy(forceOfflineMode = it)) }
            )

            SayvisDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisButton(
                    label = if (isProbing) s.testing else s.testConnection,
                    onClick = onTestConnection,
                    busy = isProbing,
                    modifier = Modifier.weight(1f).testTag("settings_test_connection"),
                    tone = ButtonTone.PRIMARY
                )
            }

            probe?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                SayvisCard(
                    borderColor = if (result.success) SayvisGreenSuccess.copy(alpha = 0.5f) else SayvisRedAlert.copy(alpha = 0.5f),
                    containerColor = if (result.success) SayvisGreenSuccess.copy(alpha = 0.08f) else SayvisRedAlert.copy(alpha = 0.08f)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (result.success) s.connectionOk else s.connectionFailed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.success) SayvisGreenSuccess else SayvisRedAlert
                        )
                        Text(
                            text = PersianFormat.digits("${result.latencyMs} ms", settings.localization.persianDigits),
                            fontSize = 11.sp,
                            color = SayvisSilverMuted
                        )
                    }
                    Text(text = result.message(isPersian), fontSize = 11.sp, color = SayvisSilverMuted)
                    if (result.model.isNotBlank()) {
                        Text(text = result.model, fontSize = 10.5.sp, color = SayvisSilverMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (vaultHardwareBacked) SayvisGreenSuccess else SayvisAmberWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (vaultHardwareBacked) s.vaultHardware else s.vaultSoftware,
                    fontSize = 10.5.sp,
                    color = if (vaultHardwareBacked) SayvisGreenSuccess else SayvisAmberWarning
                )
            }
        }

        // ============================================================ TRADING
        SayvisSectionHeader(title = s.sectionTrading, icon = Icons.Default.SwapHoriz)

        SayvisCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = s.gatewayTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Text(
                        text = gatewaySummary(gatewayState, settings, isPersian),
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )
                }
                SayvisStatusPill(
                    text = gatewayState.phaseLabel(isPersian),
                    color = when (gatewayState.phase) {
                        MtConnectionPhase.CONNECTED -> SayvisGreenSuccess
                        MtConnectionPhase.SIMULATED -> SayvisCyan
                        MtConnectionPhase.ERROR -> SayvisRedAlert
                        else -> SayvisSilverMuted
                    }
                )
            }

            gatewayState.account?.let { account ->
                SayvisDivider()
                Text(
                    text = "${s.balance}: ${PersianFormat.money(account.balance, account.currency, settings.localization.persianDigits)}",
                    fontSize = 11.5.sp,
                    color = SayvisSilverMuted
                )
                Text(
                    text = "${if (isPersian) "سود/زیان باز" else "Floating P/L"}: " +
                        PersianFormat.money(account.floatingPnl, account.currency, settings.localization.persianDigits),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor(account.floatingPnl)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            SayvisButton(
                label = s.gatewayTitle,
                onClick = onOpenGateway,
                modifier = Modifier.fillMaxWidth().testTag("settings_open_gateway"),
                tone = ButtonTone.GOLD,
                icon = Icons.Default.SwapHoriz
            )
        }

        // ========================================================== AUTOMATION
        SayvisSectionHeader(title = s.sectionAutomation, icon = Icons.Default.Code)

        SayvisCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = s.scriptsTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Text(
                        text = if (isPersian) "$scriptCount اسکریپت ذخیره شده" else "$scriptCount script(s) stored",
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )
                }
            }
            SayvisToggleRow(
                label = if (isPersian) "اجرای خودکار اسکریپت‌ها" else "Run automation scripts",
                hint = if (isPersian) "وقتی خاموش باشد، هیچ اسکریپتی خودکار اجرا نمی‌شود" else "When off, no script runs automatically",
                checked = settings.runAutomationScripts,
                onCheckedChange = { onSettingsChange(settings.copy(runAutomationScripts = it)) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SayvisButton(
                label = s.scriptsTitle,
                onClick = onOpenScripts,
                modifier = Modifier.fillMaxWidth().testTag("settings_open_scripts"),
                tone = ButtonTone.PRIMARY,
                icon = Icons.Default.Code
            )
        }

        // ============================================================ SECURITY
        SayvisSectionHeader(title = s.sectionSecurity, icon = Icons.Default.Security)

        SayvisCard {
            SayvisToggleRow(
                label = s.emergencyLock,
                hint = if (isPersian) "همهٔ اقدامات اجرایی، اسکریپت‌ها و ارسال سفارش را مسدود می‌کند"
                else "Blocks every execution, script and order routing",
                checked = settings.emergencyLockActive,
                onCheckedChange = { onToggleEmergencyLock() }
            )
            SayvisToggleRow(
                label = s.requireHighRiskConfirm,
                hint = null,
                checked = settings.requireConfirmationForHighRisk,
                onCheckedChange = { onSettingsChange(settings.copy(requireConfirmationForHighRisk = it)) }
            )
            SayvisToggleRow(
                label = s.keepAudit,
                hint = null,
                checked = settings.keepAuditLogOnDevice,
                onCheckedChange = { onSettingsChange(settings.copy(keepAuditLogOnDevice = it)) }
            )
        }

        // ================================================================ DATA
        SayvisSectionHeader(title = s.sectionData, icon = Icons.Default.Storage)

        SayvisCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = if (isPersian) "حافظهٔ نهان ترجمه" else "Translation cache", fontSize = 12.sp, color = SayvisSilver)
                SayvisStatusPill(
                    text = PersianFormat.digits(translationCacheSize.toString(), settings.localization.persianDigits),
                    color = SayvisCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SayvisButton(
                    label = if (isPersian) "پاک کردن حافظهٔ ترجمه" else "Clear translation cache",
                    onClick = onClearTranslationCache,
                    modifier = Modifier.weight(1f),
                    tone = ButtonTone.NEUTRAL
                )
                SayvisButton(
                    label = s.resetAll,
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f).testTag("settings_reset_all"),
                    tone = ButtonTone.DANGER
                )
            }
        }

        // ============================================================== ABOUT
        SayvisSectionHeader(title = s.sectionAbout, icon = Icons.Default.Info)

        SayvisCard {
            Text(text = "${s.appName} — ${s.appTagline}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisGold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${s.version} $appVersion",
                fontSize = 11.sp,
                color = SayvisSilverMuted
            )
            Text(
                text = if (isPersian) {
                    "سایویس یک لایهٔ عامل هوش مصنوعی شخصی با اصول «اعتماد صفر» و «حاکمیت داده» است. " +
                        "هیچ داده‌ای بدون اقدام صریح شما از دستگاه خارج نمی‌شود."
                } else {
                    "SAYVIS is a zero-trust personal AI operating layer. No data leaves the device " +
                        "without an explicit owner action."
                },
                fontSize = 11.sp,
                color = SayvisSilverMuted
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showResetDialog) {
        SayvisConfirmDialog(
            title = s.resetAll,
            body = s.resetAllWarning,
            confirmLabel = s.reset,
            dismissLabel = s.cancel,
            danger = true,
            onConfirm = {
                onResetAll()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}


/**
 * "Sign in with Google" → Gemini link card.
 *
 * Google has no in-app OAuth for the Gemini API; AI Studio (which works with
 * any personal Google account) is the official Google-login surface for keys.
 * This card opens AI Studio, then — once the owner copied the issued key —
 * picks it from the clipboard, verifies it live against Google and stores it
 * in the Keystore-backed vault, switching the brain to Gemini. For zero-typing
 * linking, the text-selection menu / share sheet route lands in
 * [com.example.sayvis.ui.GoogleLinkActivity].
 */
@Composable
private fun GoogleAccountLinkCard(
    activeKey: String,
    isPersian: Boolean,
    onKeyLinked: (String) -> Unit
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Pair<GoogleLinkManager.CheckStatus, String>?>(null) }

    SayvisDivider()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Link, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = s.googleLinkTitle, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
    }
    Text(text = s.googleLinkHow, fontSize = 11.5.sp, color = SayvisSilverMuted)
    Spacer(modifier = Modifier.height(8.dp))

    if (activeKey.isNotBlank()) {
        Text(
            text = s.googleLinkActiveKey + ": " + GoogleLinkManager.redact(activeKey) +
                "  (" + kindLabelOf(s, activeKey) + ")",
            fontSize = 11.sp,
            color = SayvisGreenSuccess,
            modifier = Modifier.testTag("google_link_active_key")
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SayvisButton(
            label = s.googleLinkSignIn,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GoogleLinkManager.STUDIO_KEY_URL)))
                }
            },
            tone = ButtonTone.PRIMARY,
            modifier = Modifier.weight(1f).testTag("google_link_signin")
        )
        SayvisButton(
            label = if (busy) s.googleLinkChecking else s.googleLinkFromClipboard,
            onClick = {
                if (busy) return@SayvisButton
                val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                    ?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                val key = GoogleLinkManager.extractKeyFromText(clip)
                if (key == null) {
                    status = GoogleLinkManager.CheckStatus.INVALID to s.googleLinkNoKey
                    return@SayvisButton
                }
                busy = true
                scope.launch {
                    val check = GoogleLinkManager.validateKey(key)
                    if (check.keyUsable || check.status == GoogleLinkManager.CheckStatus.NETWORK) {
                        onKeyLinked(key)
                    }
                    status = check.status to (if (isPersian) check.messageFa else check.messageEn)
                    busy = false
                }
            },
            busy = busy,
            tone = ButtonTone.SUCCESS,
            modifier = Modifier.weight(1f).testTag("google_link_clipboard")
        )
    }
    Text(text = s.googleLinkSelectionHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
    status?.let { (st, message) ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 11.sp,
            color = when (st) {
                GoogleLinkManager.CheckStatus.VALID -> SayvisGreenSuccess
                GoogleLinkManager.CheckStatus.INVALID -> SayvisRedAlert
                else -> SayvisAmberWarning
            },
            modifier = Modifier.testTag("google_link_status")
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}

private fun kindLabelOf(s: com.example.sayvis.i18n.SayvisStrings, key: String): String =
    when (GoogleLinkManager.classify(key)) {
        GoogleLinkManager.KeyKind.AUTH_AQ -> s.googleLinkKindAuth
        GoogleLinkManager.KeyKind.STANDARD_AIZA -> s.googleLinkKindStandard
        GoogleLinkManager.KeyKind.UNKNOWN -> s.googleLinkKindUnknown
    }

@Composable
private fun SecretField(
    label: String,
    hint: String,
    value: String,
    reveal: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    isPersian: Boolean
) {
    val s = LocalStrings.current
    SayvisField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        hint = hint,
        isSecret = true,
        revealSecret = reveal,
        monospace = true,
        trailing = {
            Text(
                text = if (reveal) s.hideKey else s.showKey,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisGold,
                modifier = Modifier
                    .clickable { onRevealChange(!reveal) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("secret_reveal_button")
            )
        }
    )
    if (value.isNotBlank() && !reveal) {
        Text(
            text = s.apiKeyMaskedHint,
            fontSize = 10.sp,
            color = SayvisSilverMuted
        )
    }
}

private fun gatewaySummary(state: MtGatewayState, settings: AppSettings, isPersian: Boolean): String {
    val profile = state.profile
    val terminal = profile.terminalVersion.label(isPersian)
    val mode = profile.executionMode.label(isPersian)
    val server = profile.serverAddress.ifBlank { if (isPersian) "سرور تنظیم‌نشده" else "no server set" }
    return "$terminal · $server · $mode"
}


@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    s: com.example.sayvis.i18n.SayvisStrings,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = SayvisSilver, modifier = Modifier.weight(1f))
        if (granted) {
            SayvisStatusPill(text = s.avatarPermGranted, color = SayvisGreenSuccess)
        } else {
            SayvisButton(label = s.avatarPermGrant, onClick = onGrant, tone = ButtonTone.GOLD)
        }
    }
}
