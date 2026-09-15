package com.example.sayvis.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sayvis.ai.TranslationSource
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.screentranslate.ScreenPlateStyle
import com.example.sayvis.screentranslate.ScreenTextColorMode
import com.example.sayvis.screentranslate.ScreenTranslateGranularity
import com.example.sayvis.screentranslate.ScreenTranslatePhase
import com.example.sayvis.screentranslate.ScreenTranslateStatus
import com.example.sayvis.screentranslate.ScreenTranslationMode
import com.example.sayvis.screentranslate.ScreenTranslationSettings
import com.example.sayvis.screentranslate.ScreenTranslationStats
import com.example.sayvis.screentranslate.TranslatedSegment
import com.example.sayvis.settings.AppSettings
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisDivider
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.components.SayvisLinkButton
import com.example.sayvis.ui.components.SayvisOptionRow
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.components.SayvisStatRow
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * Control room for the live screen translator.
 *
 * Everything the feature does is visible and owned here: the two consents it needs, what the
 * Persian will look like, how hard it is allowed to work, what it has already written on the
 * screen, and an honest label for where each translation came from (offline dictionary,
 * memory, or the owner's AI provider).
 */
@Composable
fun ScreenTranslatorScreen(
    settings: AppSettings,
    status: ScreenTranslateStatus,
    stats: ScreenTranslationStats,
    preview: TranslatedSegment?,
    previewBusy: Boolean,
    dictionarySize: Int,
    cacheSize: Int,
    aiReady: Boolean,
    isPersian: Boolean,
    onCreateCaptureIntent: () -> Intent,
    onStart: (Int, Intent) -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSettingsChange: ((ScreenTranslationSettings) -> ScreenTranslationSettings) -> Unit,
    onPreview: (String) -> Unit,
    onClearPreview: () -> Unit,
    onRefreshLayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val screen = settings.screenTranslation
    val persianDigits = settings.localization.persianDigits

    var overlayGranted by remember { mutableStateOf(canDrawOverlays(context)) }
    var notificationsAllowed by remember { mutableStateOf(notificationsEnabled(context)) }
    var consentDenied by remember { mutableStateOf(false) }
    var testerText by remember { mutableStateOf("Sign in to continue and save your changes") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val overlay = canDrawOverlays(context)
                val changed = overlay != overlayGranted
                overlayGranted = overlay
                notificationsAllowed = notificationsEnabled(context)
                if (changed) onRefreshLayers()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            consentDenied = false
            onStart(result.resultCode, data)
        } else {
            consentDenied = true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsAllowed = granted }

    fun beginSession() {
        if (!overlayGranted) {
            openOverlaySettings(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        captureLauncher.launch(onCreateCaptureIntent())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("screen_translator_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Translate, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = s.stTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.stSubtitle, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        // ------------------------------------------------------------- live status
        SayvisCard(
            modifier = Modifier.padding(top = 12.dp),
            borderColor = when (status.phase) {
                ScreenTranslatePhase.RUNNING -> SayvisGreenSuccess
                ScreenTranslatePhase.PAUSED -> SayvisAmberWarning
                ScreenTranslatePhase.ERROR -> SayvisRedAlert
                ScreenTranslatePhase.BLOCKED -> SayvisRedAlert
                else -> SayvisCyan
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SayvisStatusPill(
                    text = status.phase.label(isPersian),
                    color = phaseColor(status.phase)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SayvisStatusPill(
                    text = if (status.dictionaryOnly || screen.dictionaryOnly) s.stOnlineOffline else s.stOnlineReady,
                    color = if (status.engineOnline && !screen.dictionaryOnly) SayvisGreenSuccess else SayvisGold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (status.isSessionActive) {
                SayvisStatRow(
                    label = s.stPhaseLabel,
                    value = formatDuration(status.uptimeMillis(), persianDigits),
                    valueColor = SayvisCyan
                )
                SayvisStatRow(
                    label = s.stStatSegments,
                    value = PersianFormat.number(status.segmentsTranslated, persianDigits)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SayvisButton(
                        label = if (status.phase == ScreenTranslatePhase.PAUSED) s.stResume else s.stPause,
                        icon = if (status.phase == ScreenTranslatePhase.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        tone = ButtonTone.NEUTRAL,
                        modifier = Modifier.weight(1f),
                        onClick = { if (status.phase == ScreenTranslatePhase.PAUSED) onResume() else onPause() }
                    )
                    SayvisButton(
                        label = s.stStop,
                        icon = Icons.Default.Stop,
                        tone = ButtonTone.DANGER,
                        modifier = Modifier.weight(1f),
                        onClick = onStop
                    )
                }
            } else {
                SayvisButton(
                    label = s.stStart,
                    icon = Icons.Default.PlayArrow,
                    tone = ButtonTone.SUCCESS,
                    modifier = Modifier.fillMaxWidth().testTag("start_screen_translation"),
                    enabled = overlayGranted && !settings.emergencyLockActive,
                    onClick = { beginSession() }
                )
                if (!overlayGranted) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = s.stNeedAllPermissions, fontSize = 11.sp, color = SayvisAmberWarning)
                }
            }

            if (settings.emergencyLockActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SayvisRedAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = s.stEmergencyBlocked, fontSize = 11.sp, color = SayvisRedAlert)
                }
            }

            if (consentDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = s.stConsentDenied, fontSize = 11.sp, color = SayvisAmberWarning)
            }

            status.message(isPersian)?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, fontSize = 11.sp, color = SayvisAmberWarning)
            }

            if (status.secureContentSuspected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = s.stProtectedContent, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        // ------------------------------------------------------------ permissions
        SayvisSectionHeader(title = s.stPermissionsTitle, icon = Icons.Default.Security)
        SayvisCard {
            PermissionRow(
                title = s.stOverlayPermission,
                hint = s.stOverlayPermissionHint,
                granted = overlayGranted,
                grantedLabel = s.stGranted,
                grantLabel = s.stGrant,
                onGrant = { openOverlaySettings(context) }
            )
            SayvisDivider()
            PermissionRow(
                title = s.stCapturePermission,
                hint = s.stCapturePermissionHint,
                granted = status.captureConsentGranted && status.isSessionActive,
                grantedLabel = s.stGranted,
                grantLabel = s.stGrant,
                onGrant = { beginSession() }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SayvisDivider()
                PermissionRow(
                    title = s.stNotificationPermission,
                    hint = s.stNotificationPermissionHint,
                    granted = notificationsAllowed,
                    grantedLabel = s.stGranted,
                    grantLabel = s.stGrant,
                    onGrant = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }
        }

        // -------------------------------------------------------------- appearance
        SayvisSectionHeader(title = s.stAppearanceTitle, icon = Icons.Default.Language)
        SayvisCard {
            SayvisOptionRow(
                label = s.stModeLabel,
                options = ScreenTranslationMode.entries.map { it.label(isPersian) },
                selectedIndex = ScreenTranslationMode.entries.indexOf(screen.mode),
                onSelect = { index ->
                    onSettingsChange { it.copy(mode = ScreenTranslationMode.entries[index]) }
                }
            )
            SayvisOptionRow(
                label = s.stGranularityLabel,
                options = ScreenTranslateGranularity.entries.map { it.label(isPersian) },
                selectedIndex = ScreenTranslateGranularity.entries.indexOf(screen.granularity),
                onSelect = { index ->
                    onSettingsChange { it.copy(granularity = ScreenTranslateGranularity.entries[index]) }
                }
            )
            SayvisOptionRow(
                label = s.stPlateStyle,
                options = ScreenPlateStyle.entries.map { it.label(isPersian) },
                selectedIndex = ScreenPlateStyle.entries.indexOf(screen.plateStyle),
                onSelect = { index ->
                    onSettingsChange { it.copy(plateStyle = ScreenPlateStyle.entries[index]) }
                }
            )
            SayvisOptionRow(
                label = s.stTextColor,
                options = ScreenTextColorMode.entries.map { it.label(isPersian) },
                selectedIndex = ScreenTextColorMode.entries.indexOf(screen.textColorMode),
                onSelect = { index ->
                    onSettingsChange { it.copy(textColorMode = ScreenTextColorMode.entries[index]) }
                }
            )
            SliderRow(
                label = s.stPlateOpacity,
                value = screen.plateOpacityPercent.toFloat(),
                range = 0f..100f,
                display = PersianFormat.number(screen.plateOpacityPercent, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(plateOpacityPercent = value.toInt()) }
            }
            SliderRow(
                label = s.stTextScale,
                value = screen.textScalePercent.toFloat(),
                range = 60f..170f,
                display = PersianFormat.number(screen.textScalePercent, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(textScalePercent = value.toInt()) }
            }
            SayvisToggleRow(
                label = s.stPersianNumbers,
                hint = null,
                checked = screen.persianNumbers,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(persianNumbers = enabled) } }
            )
            SayvisDivider()
            OverlayPreview(
                plateStyle = screen.plateStyle,
                plateOpacity = screen.plateOpacityPercent,
                textColorMode = screen.textColorMode,
                customPlate = screen.customPlateColor,
                customText = screen.customTextColor,
                textScale = screen.textScalePercent
            )
        }

        // ------------------------------------------------------------- performance
        SayvisSectionHeader(title = s.stPerformanceTitle, icon = Icons.Default.Visibility)
        SayvisCard {
            SliderRow(
                label = s.stPollIntervalLabel,
                value = screen.pollIntervalMs.toFloat(),
                range = ScreenTranslationSettings.MIN_POLL_MS.toFloat()..ScreenTranslationSettings.MAX_POLL_MS.toFloat(),
                display = PersianFormat.number(screen.pollIntervalMs, persianDigits),
                hint = s.stPollIntervalHint
            ) { value ->
                onSettingsChange { it.copy(pollIntervalMs = value.toInt()) }
            }
            SliderRow(
                label = s.stMaxPerFrame,
                value = screen.maxSegmentsPerFrame.toFloat(),
                range = 8f..96f,
                display = PersianFormat.number(screen.maxSegmentsPerFrame, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(maxSegmentsPerFrame = value.toInt()) }
            }
            SliderRow(
                label = s.stMaxPerRequest,
                value = screen.maxSegmentsPerRequest.toFloat(),
                range = 1f..40f,
                display = PersianFormat.number(screen.maxSegmentsPerRequest, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(maxSegmentsPerRequest = value.toInt()) }
            }
            SliderRow(
                label = s.stMinWordLength,
                value = screen.minWordLength.toFloat(),
                range = 2f..6f,
                display = PersianFormat.number(screen.minWordLength, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(minWordLength = value.toInt()) }
            }
            SayvisDivider()
            SayvisToggleRow(
                label = s.stBubble,
                hint = s.stBubbleHint,
                checked = screen.showControlBubble,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(showControlBubble = enabled) } }
            )
            SliderRow(
                label = s.stBubbleOpacity,
                value = screen.bubbleOpacityPercent.toFloat(),
                range = 20f..100f,
                display = PersianFormat.number(screen.bubbleOpacityPercent, persianDigits)
            ) { value ->
                onSettingsChange { it.copy(bubbleOpacityPercent = value.toInt()) }
            }
            SayvisToggleRow(
                label = s.stKeepAwake,
                hint = null,
                checked = screen.keepScreenOn,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(keepScreenOn = enabled) } }
            )
            SayvisToggleRow(
                label = s.stResumeBoot,
                hint = s.stResumeBootHint,
                checked = screen.resumeAfterBoot,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(resumeAfterBoot = enabled) } }
            )
            SayvisToggleRow(
                label = s.stSingleAppCapture,
                hint = null,
                checked = screen.preferSingleAppCapture,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(preferSingleAppCapture = enabled) } }
            )
        }

        // --------------------------------------------------------------- privacy
        SayvisSectionHeader(title = s.stPrivacyTitle, icon = Icons.Default.Security)
        SayvisCard {
            SayvisToggleRow(
                label = s.stDictionaryOnly,
                hint = s.stDictionaryOnlyHint,
                checked = screen.dictionaryOnly,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(dictionaryOnly = enabled) } }
            )
            SayvisToggleRow(
                label = s.stCacheTranslations,
                hint = null,
                checked = screen.cacheTranslations,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(cacheTranslations = enabled) } }
            )
            SayvisToggleRow(
                label = s.stSkipOwnApp,
                hint = null,
                checked = screen.skipOwnApp,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(skipOwnApp = enabled) } }
            )
            SayvisDivider()
            Text(text = s.stPrivacyBody, fontSize = 11.sp, color = SayvisSilverMuted)
        }

        // ------------------------------------------------------------- statistics
        SayvisSectionHeader(title = s.stStatsTitle, icon = Icons.Default.CheckCircle)
        SayvisCard {
            SayvisStatRow(s.stStatFrames, PersianFormat.number(stats.frames, persianDigits))
            SayvisStatRow(s.stStatSkipped, PersianFormat.number(status.framesSkipped, persianDigits))
            SayvisStatRow(
                s.stStatSegments,
                PersianFormat.number(stats.translatedSegments, persianDigits),
                valueColor = SayvisCyan
            )
            SayvisStatRow(
                s.stStatOcr,
                if (status.lastOcrMillis > 0) "${PersianFormat.number(status.lastOcrMillis, persianDigits)} ms" else "—"
            )
            SayvisStatRow(s.stStatDictionary, PersianFormat.number(dictionarySize, persianDigits))
            SayvisStatRow(s.stStatCache, PersianFormat.number(cacheSize, persianDigits))
            SayvisStatRow(
                s.stStatOnline,
                if (aiReady && !screen.dictionaryOnly) s.stOnlineReady else s.stOnlineOffline,
                valueColor = if (aiReady && !screen.dictionaryOnly) SayvisGreenSuccess else SayvisGold
            )
        }

        // --------------------------------------------------- recently translated
        SayvisSectionHeader(title = s.stRecentTitle, icon = Icons.Default.Translate)
        SayvisCard {
            if (status.recentSegments.isEmpty()) {
                Text(text = s.stNothingTranslated, fontSize = 11.sp, color = SayvisSilverMuted)
            } else {
                status.recentSegments.take(8).forEach { segment ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = segment.sourceText, fontSize = 11.sp, color = SayvisSilverMuted)
                        Text(text = segment.persianText, fontSize = 12.5.sp, color = SayvisSilver)
                        Text(
                            text = "${s.stTierLabel}: ${tierLabel(segment.provenance, isPersian, s)}",
                            fontSize = 9.5.sp,
                            color = SayvisGold
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------------- dictionary
        SayvisSectionHeader(title = s.stTesterTitle, icon = Icons.Default.Language)
        SayvisCard {
            Text(text = s.stTesterHint, fontSize = 11.sp, color = SayvisSilverMuted)
            SayvisField(
                label = s.stSourceText,
                value = testerText,
                onValueChange = { testerText = it },
                singleLine = false,
                minLines = 2
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SayvisButton(
                    label = s.stTestMethod,
                    icon = Icons.Default.Translate,
                    busy = previewBusy,
                    modifier = Modifier.weight(1f),
                    onClick = { onPreview(testerText) }
                )
                SayvisLinkButton(label = s.reset, onClick = onClearPreview)
            }
            preview?.let { segment ->
                SayvisDivider()
                Text(text = s.stResultText, fontSize = 11.sp, color = SayvisSilverMuted)
                Text(text = segment.persianText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SayvisCyan)
                SayvisStatRow(
                    label = s.stTierLabel,
                    value = tierLabel(segment.provenance, isPersian, s),
                    valueColor = SayvisGold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(
    title: String,
    hint: String,
    granted: Boolean,
    grantedLabel: String,
    grantLabel: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (granted) SayvisGreenSuccess else SayvisAmberWarning,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = SayvisSilver)
            Text(text = hint, fontSize = 10.5.sp, color = SayvisSilverMuted)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (granted) {
            SayvisStatusPill(text = grantedLabel, color = SayvisGreenSuccess)
        } else {
            SayvisButton(label = grantLabel, tone = ButtonTone.GOLD, onClick = onGrant)
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    hint: String? = null,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, color = SayvisSilver)
                if (hint != null) {
                    Text(text = hint, fontSize = 10.sp, color = SayvisSilverMuted)
                }
            }
            Text(text = display, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisCyan)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = SayvisCyan,
                activeTrackColor = SayvisCyan,
                inactiveTrackColor = SayvisSilverMuted.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * A literal preview of what the overlay will look like: the same plate colour, opacity,
 * ink and scale the renderer uses, so the owner can judge the result without leaving the app.
 */
@Composable
private fun OverlayPreview(
    plateStyle: ScreenPlateStyle,
    plateOpacity: Int,
    textColorMode: ScreenTextColorMode,
    customPlate: Int,
    customText: Int,
    textScale: Int
) {
    val s = LocalStrings.current
    val basePlate = when (plateStyle) {
        ScreenPlateStyle.SAMPLED -> 0x101820
        ScreenPlateStyle.DARK -> 0x0B0F14
        ScreenPlateStyle.LIGHT -> 0xF2F4F8
        ScreenPlateStyle.CUSTOM -> customPlate or 0xFF000000.toInt()
    }
    val plate = Color(basePlate).copy(alpha = plateOpacity.coerceIn(0, 100) / 100f)
    val ink = when (textColorMode) {
        ScreenTextColorMode.AUTO -> if (luminance(basePlate) > 0.58) Color(0xFF10151C) else Color(0xFFF4F7FB)
        ScreenTextColorMode.LIGHT -> Color(0xFFF4F7FB)
        ScreenTextColorMode.DARK -> Color(0xFF10151C)
        ScreenTextColorMode.CUSTOM -> Color(customText or 0xFF000000.toInt())
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = s.stResultText, fontSize = 11.sp, color = SayvisSilverMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF20262E), RoundedCornerShape(9.dp))
                .padding(10.dp)
        ) {
            Text(
                text = "سطح باتری ۱۷٪ است. برای ادامه، شارژر را وصل کنید.",
                color = ink,
                fontSize = (12 * (textScale.coerceIn(60, 170) / 100f)).sp,
                modifier = Modifier
                    .background(plate, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

private fun luminance(rgb: Int): Double {
    val r = ((rgb shr 16) and 0xFF) / 255.0
    val g = ((rgb shr 8) and 0xFF) / 255.0
    val b = (rgb and 0xFF) / 255.0
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private fun phaseColor(phase: ScreenTranslatePhase): Color = when (phase) {
    ScreenTranslatePhase.RUNNING -> SayvisGreenSuccess
    ScreenTranslatePhase.PAUSED -> SayvisAmberWarning
    ScreenTranslatePhase.BLOCKED, ScreenTranslatePhase.ERROR -> SayvisRedAlert
    ScreenTranslatePhase.STARTING -> SayvisCyan
    else -> SayvisSilverMuted
}

private fun tierLabel(
    source: TranslationSource,
    isPersian: Boolean,
    strings: com.example.sayvis.i18n.SayvisStrings
): String = when (source) {
    TranslationSource.DICTIONARY -> strings.stTierDictionary
    TranslationSource.CACHE -> strings.stTierCache
    TranslationSource.MACHINE -> strings.stTierMachine
    TranslationSource.ORIGINAL -> if (isPersian) "بدون تغییر" else "Unchanged"
    TranslationSource.FAILED -> strings.stTierNone
}

private fun formatDuration(millis: Long, persianDigits: Boolean): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val text = when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
    return PersianFormat.toPersianNumerals(text).ifEmpty { if (persianDigits) "۰s" else "0s" }
}

private fun canDrawOverlays(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

private fun notificationsEnabled(context: Context): Boolean =
    runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(true)

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
