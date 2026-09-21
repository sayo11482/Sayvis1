package com.example.sayvis.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisDivider
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.voice.AvatarListenController
import com.example.sayvis.voice.ListenBus
import com.example.sayvis.voice.VoicePrintStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Owner control surface for the floating avatar & ambient listening feature:
 * runtime permissions, the persistent bubble service toggle, voice-print
 * enrollment/test, sensitivity and the recent-recognition history.
 */
@Composable
fun AvatarListenScreen(
    isPersian: Boolean,
    persianDigits: Boolean,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { VoicePrintStore.get(context) }

    val mode by ListenBus.mode.collectAsState()
    val level by ListenBus.level.collectAsState()
    val lastOwnerAt by ListenBus.lastOwnerAt.collectAsState()
    val lastScore by ListenBus.lastScore.collectAsState()

    var micGranted by remember { mutableStateOf(AvatarListenController.hasMicPermission(context)) }
    var overlayGranted by remember { mutableStateOf(AvatarListenController.hasOverlayPermission(context)) }
    var notifGranted by remember { mutableStateOf(AvatarListenController.hasNotificationPermission(context)) }

    var threshold by remember { mutableFloatStateOf(store.threshold()) }
    var enrolledCount by remember { mutableIntStateOf(store.enrolledTemplates().size) }
    var enrolledAt by remember { mutableLongStateOf(store.enrolledAt()) }
    var history by remember { mutableStateOf(store.history()) }
    var pendingStart by remember { mutableStateOf(false) }

    var capturing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var captureLevel by remember { mutableFloatStateOf(0f) }
    var message by remember { mutableStateOf<String?>(null) }
    val enrollmentTemplates = remember { mutableStateListOf<FloatArray>() }

    val number: (Int) -> String = remember(persianDigits) {
        val convert: (Int) -> String = { value ->
            if (persianDigits) PersianFormat.toPersianNumerals(value.toString()) else value.toString()
        }
        convert
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    fun refreshStore() {
        threshold = store.threshold()
        enrolledCount = store.enrolledTemplates().size
        enrolledAt = store.enrolledAt()
        history = store.history()
    }

    // Re-check the overlay grant when the user comes back from system settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                micGranted = AvatarListenController.hasMicPermission(context)
                overlayGranted = AvatarListenController.hasOverlayPermission(context)
                notifGranted = AvatarListenController.hasNotificationPermission(context)
                if (pendingStart && overlayGranted && micGranted) {
                    pendingStart = false
                    AvatarListenController.startListening(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micGranted = granted
        if (granted) {
            if (!notifGranted && Build.VERSION.SDK_INT >= 33) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (overlayGranted) AvatarListenController.startListening(context)
        }
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayGranted = AvatarListenController.hasOverlayPermission(context)
        if (overlayGranted && micGranted) AvatarListenController.startListening(context)
    }

    val listening = mode != ListenBus.Mode.OFF
    val ownerHit = mode == ListenBus.Mode.OWNER_VOICE

    fun captureEnrollmentSample() {
        if (capturing) return
        capturing = true
        progress = 0f
        captureLevel = 0f
        message = null
        scope.launch {
            val wasListening = AvatarListenController.isListening()
            if (wasListening) AvatarListenController.stopListening(context)
            val pcm = AvatarListenController.captureSample(context, AvatarListenController.ENROLL_SAMPLE_MS) { fraction, lvl ->
                progress = fraction
                captureLevel = lvl
            }
            if (wasListening) AvatarListenController.startListening(context)
            capturing = false
            if (pcm == null) {
                message = s.avatarMicUnavailable
                return@launch
            }
            val template = AvatarListenController.templateFromSample(pcm)
            if (template == null) {
                message = s.avatarTooNoisy
                return@launch
            }
            enrollmentTemplates.add(template)
            if (enrollmentTemplates.size >= 3) {
                store.savePrint(enrollmentTemplates.toList())
                enrollmentTemplates.clear()
                refreshStore()
                message = s.avatarEnrollDone
            } else {
                message = s.avatarSampleCaptured(enrollmentTemplates.size)
            }
        }
    }

    fun runRecognitionTest() {
        if (capturing) return
        capturing = true
        progress = 0f
        captureLevel = 0f
        message = s.testing
        scope.launch {
            val wasListening = AvatarListenController.isListening()
            if (wasListening) AvatarListenController.stopListening(context)
            val pcm = AvatarListenController.captureSample(context, AvatarListenController.TEST_SAMPLE_MS) { fraction, lvl ->
                progress = fraction
                captureLevel = lvl
            }
            if (wasListening) AvatarListenController.startListening(context)
            capturing = false
            if (pcm == null) {
                message = s.avatarMicUnavailable
                return@launch
            }
            val score = AvatarListenController.scoreSample(pcm, store.enrolledTemplates())
            val percent = number(scorePercent(score))
            message = if (score >= store.threshold()) s.avatarTestOk(percent) else s.avatarTestNo(percent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("avatar_listen_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = s.avatarTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.avatarSubtitle, fontSize = 11.sp, color = SayvisSilverMuted, lineHeight = 15.sp)
            }
        }

        // ============================================================ STATUS
        SayvisSectionHeader(title = s.avatarStatusTitle, icon = Icons.Default.Info)

        SayvisCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                ownerHit -> SayvisGreenSuccess
                                listening -> SayvisCyan
                                else -> SayvisSilverMuted
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        ownerHit -> s.avatarModeOwner
                        listening -> s.avatarModeListening
                        else -> s.avatarModeOff
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        ownerHit -> SayvisGreenSuccess
                        listening -> SayvisCyan
                        else -> SayvisSilverMuted
                    },
                    modifier = Modifier.weight(1f)
                )
                if (listening) {
                    SayvisStatusPill(
                        text = number((level * 100).toInt()) + "٪",
                        color = if (ownerHit) SayvisGreenSuccess else SayvisCyan
                    )
                }
            }

            if (listening) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = s.avatarLevel, fontSize = 10.5.sp, color = SayvisSilverMuted)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { level.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (ownerHit) SayvisGreenSuccess else SayvisCyan,
                    trackColor = SayvisSilverMuted.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (enrolledCount == 0) {
                    Text(text = s.avatarNoPrintYet, fontSize = 11.sp, color = SayvisAmberWarning, lineHeight = 15.sp)
                }
            }

            if (lastOwnerAt > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = s.avatarHistoryTitle + ": " +
                        number(scorePercent(lastScore)) + "٪ · " +
                        (if (persianDigits) PersianFormat.toPersianNumerals(timeFormatter.format(Date(lastOwnerAt))) else timeFormatter.format(Date(lastOwnerAt))),
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        // ============================================================ SERVICE
        SayvisSectionHeader(title = s.avatarRunToggle, icon = Icons.Default.Mic)

        SayvisCard {
            SayvisToggleRow(
                label = s.avatarRunToggle,
                hint = s.avatarRunHint,
                checked = listening,
                onCheckedChange = { want ->
                    when {
                        want && !micGranted -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        want && !overlayGranted -> {
                            pendingStart = true
                            overlayLauncher.launch(AvatarListenController.overlaySettingsIntent(context))
                        }
                        want -> AvatarListenController.startListening(context)
                        else -> AvatarListenController.stopListening(context)
                    }
                }
            )
            Text(text = s.avatarTapHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
            Spacer(modifier = Modifier.height(6.dp))

            // Permission rows.
            PermissionRow(
                label = s.avatarPermMic,
                granted = micGranted,
                grantLabel = s.avatarPermGrant,
                grantedLabel = s.avatarPermGranted,
                missingLabel = s.avatarPermMissing,
                onGrant = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            PermissionRow(
                label = s.avatarPermOverlay,
                granted = overlayGranted,
                grantLabel = s.avatarPermGrant,
                grantedLabel = s.avatarPermGranted,
                missingLabel = s.avatarPermMissing,
                onGrant = { overlayLauncher.launch(AvatarListenController.overlaySettingsIntent(context)) }
            )
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    label = s.avatarPermNotif,
                    granted = notifGranted,
                    grantLabel = s.avatarPermGrant,
                    grantedLabel = s.avatarPermGranted,
                    missingLabel = s.avatarPermMissing,
                    onGrant = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }
        }

        // ======================================================= VOICE PRINT
        SayvisSectionHeader(title = s.avatarEnrollTitle, icon = Icons.Default.Mic)

        SayvisCard {
            Text(text = s.avatarEnrollHint, fontSize = 11.sp, color = SayvisSilverMuted, lineHeight = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < enrollmentTemplates.size -> SayvisCyan
                                    capturing && index == enrollmentTemplates.size -> SayvisGold
                                    else -> SayvisSilverMuted.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                SayvisButton(
                    label = when {
                        capturing -> s.avatarCapturing
                        enrolledCount > 0 -> s.avatarReEnroll
                        enrollmentTemplates.isEmpty() -> s.avatarEnrollStart
                        else -> s.avatarEnrollNext
                    },
                    onClick = { captureEnrollmentSample() },
                    tone = if (enrolledCount > 0) ButtonTone.NEUTRAL else ButtonTone.PRIMARY,
                    busy = capturing,
                    icon = Icons.Default.Mic,
                    modifier = Modifier.testTag("avatar_enroll_button")
                )
            }

            if (capturing) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SayvisGold,
                    trackColor = SayvisSilverMuted.copy(alpha = 0.25f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { captureLevel.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SayvisGreenSuccess,
                    trackColor = SayvisSilverMuted.copy(alpha = 0.2f)
                )
            }

            if (enrolledCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                SayvisDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.avatarEnrollTitle + " · " + number(enrolledCount) + "×",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGreenSuccess
                        )
                        if (enrolledAt > 0) {
                            Text(
                                text = s.avatarEnrolledAt + ": " +
                                    (if (persianDigits) PersianFormat.toPersianNumerals(
                                        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(enrolledAt))
                                    ) else SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(enrolledAt))),
                                fontSize = 10.5.sp,
                                color = SayvisSilverMuted
                            )
                        }
                    }
                    SayvisButton(
                        label = s.avatarDeletePrint,
                        onClick = {
                            store.clearPrint()
                            refreshStore()
                            message = null
                        },
                        tone = ButtonTone.DANGER,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.testTag("avatar_delete_print")
                    )
                }
            }

            // Recognition test
            if (enrolledCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = s.avatarTestTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                        Text(text = s.avatarTestHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SayvisButton(
                        label = if (capturing) s.testing else s.avatarTestRun,
                        onClick = { runRecognitionTest() },
                        tone = ButtonTone.GOLD,
                        busy = capturing,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.testTag("avatar_test_button")
                    )
                }
            }

            message?.let { text ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = text, fontSize = 11.5.sp, color = SayvisCyan, lineHeight = 15.sp)
            }
        }

        // ======================================================= SENSITIVITY
        SayvisSectionHeader(title = s.avatarThreshold, icon = Icons.Default.Refresh)

        SayvisCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = s.avatarThresholdLow, fontSize = 10.5.sp, color = SayvisSilverMuted)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = s.avatarThresholdHigh, fontSize = 10.5.sp, color = SayvisSilverMuted)
            }
            Slider(
                value = 1f - (threshold - 0.55f) / 0.40f,
                onValueChange = { sensitivity ->
                    threshold = 0.95f - sensitivity * 0.40f
                },
                onValueChangeFinished = { store.setThreshold(threshold) },
                colors = SliderDefaults.colors(
                    thumbColor = SayvisCyan,
                    activeTrackColor = SayvisCyan,
                    inactiveTrackColor = SayvisSilverMuted.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("avatar_sensitivity_slider")
            )
            Text(
                text = s.avatarThreshold + ": " + number((threshold * 100).toInt()) + "٪",
                fontSize = 11.sp,
                color = SayvisSilverMuted
            )
        }

        // =========================================================== HISTORY
        SayvisSectionHeader(title = s.avatarHistoryTitle, icon = Icons.Default.GraphicEq)

        SayvisCard {
            val liveHistory by ListenBus.history.collectAsState()
            val displayItems = if (liveHistory.isNotEmpty()) liveHistory else history
            if (displayItems.isEmpty()) {
                Text(text = s.avatarHistoryEmpty, fontSize = 11.5.sp, color = SayvisSilverMuted)
            } else {
                displayItems.forEachIndexed { index, event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SayvisGreenSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (persianDigits) {
                                PersianFormat.toPersianNumerals(timeFormatter.format(Date(event.at)))
                            } else {
                                timeFormatter.format(Date(event.at))
                            },
                            fontSize = 11.5.sp,
                            color = SayvisSilver,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = number(scorePercent(event.score)) + "٪",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGreenSuccess
                        )
                        if (index == 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            SayvisStatusPill(text = s.avatarModeOwner, color = SayvisGreenSuccess)
                        }
                    }
                }
            }
        }

        // =========================================================== PRIVACY
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = s.avatarPrivacyNote,
            fontSize = 10.5.sp,
            color = Color(0xFF64748B),
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    grantLabel: String,
    grantedLabel: String,
    missingLabel: String,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = if (granted) SayvisGreenSuccess else SayvisRedAlert,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = SayvisSilver, modifier = Modifier.weight(1f))
        if (granted) {
            SayvisStatusPill(text = grantedLabel, color = SayvisGreenSuccess)
        } else {
            SayvisButton(label = grantLabel, onClick = onGrant, tone = ButtonTone.GOLD)
            Spacer(modifier = Modifier.width(6.dp))
            SayvisStatusPill(text = missingLabel, color = SayvisRedAlert)
        }
    }
}

private fun scorePercent(score: Float): Int = (score.coerceIn(0f, 1f) * 100f).toInt()
