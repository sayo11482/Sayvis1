package com.example.sayvis.screentranslate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import com.example.sayvis.MainActivity
import com.example.sayvis.R
import com.example.sayvis.data.local.SayvisDatabase
import com.example.sayvis.data.repository.SayvisRepository
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.AppSettings
import com.example.sayvis.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The permanent part of the translator: a foreground service that mirrors the screen,
 * reads the English on it, translates and paints the Persian **in place of the original
 * text**, and keeps doing that until the owner stops it.
 *
 * Pipeline (one frame in flight at a time):
 *
 * ```
 *  MediaProjection → VirtualDisplay → ImageReader
 *        → change detection (16x16 luminance fingerprint)
 *        → ML Kit OCR (on device, offline)
 *        → line assembly + prose filter
 *        → cache → dictionary → AI provider
 *        → overlay plan → drawn exactly over the English
 * ```
 *
 * Zero-trust rules honoured here:
 *  - nothing starts without an explicit screen-capture consent **and** overlay permission;
 *  - an engaged emergency lock stops the session immediately and blocks restarts;
 *  - "force offline" and "dictionary only" both keep every pixel and every sentence on the
 *    device — the AI tier is simply skipped;
 *  - start, stop, blocking and permission refusals are written to the audit chain.
 */
class ScreenTranslatorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsStore: SettingsStore
    private lateinit var overlay: ScreenOverlayHost
    private lateinit var recognizer: ScreenTextRecognizer
    private lateinit var engine: ScreenTranslationEngine
    private var repository: SayvisRepository? = null

    private var capture: ScreenCaptureController? = null
    private var projection: MediaProjection? = null
    private var settingsObserver: Job? = null

    private var current: AppSettings = AppSettings()
    private var paused = false
    private var lastFingerprint = 0L
    private var lastNotificationAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore.get(this)
        overlay = ScreenOverlayHost(this)
        recognizer = ScreenTextRecognizer()
        engine = ScreenTranslationEngine(cache = ScreenTranslationCache(PreferencesKeyValueStore(this)))
        current = settingsStore.current()
        repository = runCatching {
            SayvisRepository(SayvisDatabase.getDatabase(applicationContext, scope))
        }.getOrNull()
        ScreenTranslateRuntime.permissionsChanged(overlay.canDrawOverlays(), false)
        observeSettings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent)
            ACTION_STOP -> stopSession(null, null)
            ACTION_PAUSE -> setPaused(true)
            ACTION_RESUME -> setPaused(false)
            ACTION_REFRESH -> refreshLayers()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        capture?.onDisplayChanged()
        lastFingerprint = 0L
        overlay.setPainting(false)
    }

    override fun onDestroy() {
        releaseSession()
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------- start up

    private fun begin(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = IntentCompat.getParcelableExtra(intent, EXTRA_CAPTURE_DATA, Intent::class.java)

        // A foreground service with the mediaProjection type must exist *before* the
        // projection is obtained; on Android 14+ that ordering is mandatory.
        if (!promoteToForeground()) {
            ScreenTranslateRuntime.fail(
                "راه‌اندازی سرویس ترجمهٔ صفحه ممکن نشد.",
                "The screen-translation service could not be started."
            )
            stopSelf()
            return
        }

        if (data == null || resultCode != android.app.Activity.RESULT_OK) {
            ScreenTranslateRuntime.fail(
                "اجازهٔ ضبط صفحه داده نشد. برای ترجمهٔ زنده به این اجازه نیاز است.",
                "Screen-capture consent was not granted. The live translator needs it to read the screen."
            )
            audit("screen.translate.consent", RiskLevel.MEDIUM_RISK, "DENIED", "owner declined screen capture")
            stopSession(null, null)
            return
        }

        if (current.emergencyLockActive) {
            blockByEmergencyLock()
            return
        }

        if (!overlay.canDrawOverlays()) {
            ScreenTranslateRuntime.fail(
                "اجازهٔ «نمایش روی برنامه‌های دیگر» داده نشده است؛ متن ترجمه‌شده نمی‌تواند روی صفحه نوشته شود.",
                "The \"display over other apps\" permission is missing, so translated text cannot be drawn."
            )
            audit("screen.translate.permission", RiskLevel.HIGHER_RISK, "BLOCKED", "overlay permission missing")
            stopSession(null, null)
            return
        }

        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = runCatching { manager.getMediaProjection(resultCode, data) }
            .getOrElse {
                Log.w(TAG, "getMediaProjection failed: ${it.message}")
                null
            }
        if (mediaProjection == null) {
            ScreenTranslateRuntime.fail(
                "دسترسی به تصویر صفحه برقرار نشد. یک بار دیگر اجازه بدهید.",
                "Could not attach to the screen stream. Please grant capture access again."
            )
            audit("screen.translate.consent", RiskLevel.MEDIUM_RISK, "FAILED", "getMediaProjection returned null")
            stopSession(null, null)
            return
        }

        projection = mediaProjection
        paused = false
        lastFingerprint = 0L
        ScreenTranslateRuntime.setPhase(ScreenTranslatePhase.STARTING)
        ScreenTranslateRuntime.permissionsChanged(true, true)
        ScreenTranslateRuntime.clearError()
        ScreenTranslateRuntime.enginesChanged(
            engineOnline = current.ai.provider != AiProviderKind.LOCAL &&
                current.ai.isProviderConfigured() && !current.forceOfflineMode,
            dictionaryOnly = current.screenTranslation.dictionaryOnly || current.forceOfflineMode
        )

        val controller = ScreenCaptureController(
            context = this,
            intervalMillis = { current.screenTranslation.boundedPollInterval() },
            onFrame = { frame -> handleFrame(frame) },
            onCaptureStopped = {
                // The owner pressed "Stop sharing" in the system UI, or the platform ended it.
                ScreenTranslateRuntime.fail(
                    "اشتراک‌گذاری صفحه توسط سیستم یا کاربر پایان یافت.",
                    "Screen sharing ended by the system or the owner."
                )
                stopSession(null, null)
            }
        )
        controller.start(mediaProjection)
        capture = controller

        attachLayers()
        ScreenTranslateRuntime.setPhase(ScreenTranslatePhase.RUNNING)
        updateNotification(force = true)
        audit(
            action = "screen.translate.start",
            risk = RiskLevel.HIGHER_RISK,
            result = "SUCCESS",
            digest = "Live screen translation started (mode ${current.screenTranslation.mode.name}, " +
                "granularity ${current.screenTranslation.granularity.name}, " +
                "dictionaryOnly=${current.screenTranslation.dictionaryOnly}, " +
                "offline=${current.forceOfflineMode})"
        )
    }

    private fun promoteToForeground(): Boolean = runCatching {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    }.getOrElse {
        Log.w(TAG, "startForeground failed: ${it.message}")
        false
    }

    private fun attachLayers() {
        val translationSettings = current.screenTranslation
        if (!overlay.attachPlanLayer()) {
            ScreenTranslateRuntime.fail(
                "لایهٔ نمایش ترجمه ساخته نشد؛ دسترسی نمایش روی برنامه‌های دیگر را بررسی کنید.",
                "The translation layer could not be created; check the overlay permission."
            )
            return
        }
        overlay.setKeepScreenOn(translationSettings.keepScreenOn)
        overlay.setPainting(!paused)
        if (translationSettings.showControlBubble) {
            overlay.attachBubble(
                opacityPercent = translationSettings.bubbleOpacityPercent,
                paused = paused,
                badge = bubbleBadge()
            )
        }
        overlay.bubbleListener = object : ScreenControlBubbleView.Listener {
            override fun onBubbleTap() = setPaused(!paused)

            override fun onBubbleHold() {
                audit(
                    action = "screen.translate.stop",
                    risk = RiskLevel.MEDIUM_RISK,
                    result = "SUCCESS",
                    digest = "Stopped from the floating control bubble"
                )
                stopSession(null, null)
            }

            override fun onBubbleDrag(dx: Float, dy: Float) = overlay.moveBubble(dx, dy)

            override fun onBubbleDragEnd() = overlay.settleBubble()
        }
    }

    private fun refreshLayers() {
        if (!ScreenTranslateRuntime.status.value.isSessionActive) return
        attachLayers()
        overlay.setPainting(!paused)
    }

    // ------------------------------------------------------------------ the pipeline

    private fun handleFrame(frame: ScreenFrame) {
        val status = ScreenTranslateRuntime.status.value
        if (paused || !status.isSessionActive) {
            frame.release()
            return
        }
        if (current.emergencyLockActive) {
            frame.release()
            blockByEmergencyLock()
            return
        }
        ScreenTranslateRuntime.framesCaptured()

        if (frame.isBlank()) {
            // Protected content (FLAG_SECURE) or a display that is off: nothing to read.
            ScreenTranslateRuntime.framesSkipped()
            frame.release()
            return
        }

        val fingerprint = frame.fingerprint()
        if (fingerprint == lastFingerprint) {
            // The screen has not changed: keep the current overlay and spend nothing.
            ScreenTranslateRuntime.framesSkipped()
            frame.release()
            return
        }

        val displayWidth = displayWidthPx()
        val displayHeight = displayHeightPx()
        val snapshot = current
        val translationSettings = snapshot.screenTranslation
        val scale = frame.scaleTo(displayWidth)

        scope.launch {
            try {
                val startedAt = SystemClock.elapsedRealtime()
                val fragments = recognizer.recognize(frame.bitmap, scale)
                val lines = ScreenTextPlanner.groupIntoLines(fragments)
                val segments = collectSegments(lines, translationSettings)
                val translations = engine.translate(
                    segments = segments,
                    settings = translationSettings,
                    ai = snapshot.ai,
                    forceOffline = snapshot.forceOfflineMode,
                    emergencyLockActive = snapshot.emergencyLockActive,
                    languageFa = true
                )
                val sampler = samplerFor(frame, displayWidth, displayHeight)
                val plan = ScreenTextPlanner.plan(
                    lines = lines,
                    translations = translations,
                    settings = translationSettings,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    sampler = sampler
                ).copy(frameFingerprint = fingerprint)

                lastFingerprint = fingerprint
                overlay.render(plan)
                ScreenTranslateRuntime.ocrCompleted(SystemClock.elapsedRealtime() - startedAt)
                ScreenTranslateRuntime.publishPlan(plan)
                updateNotification(force = false)
            } catch (error: Throwable) {
                Log.w(TAG, "Frame pipeline failed: ${error.message}")
            } finally {
                frame.release()
            }
        }
    }

    /** Every string worth translating on this frame: the lines, plus words in word mode. */
    private fun collectSegments(
        lines: List<ScreenTextLine>,
        settings: ScreenTranslationSettings
    ): List<String> {
        val unique = LinkedHashSet<String>()
        val wordMode = settings.granularity != ScreenTranslationGranularity.LINE
        for (line in lines) {
            if (!ScreenTextPlanner.isTranslatableLine(line.text, settings.minWordLength)) continue
            unique.add(line.text)
            if (wordMode) {
                for (fragment in line.fragments) {
                    val word = fragment.text.trim()
                    if (word.isEmpty()) continue
                    if (!ScreenTextPlanner.isTranslatableToken(word, settings.minWordLength)) continue
                    unique.add(word)
                }
            }
        }
        return unique.toList().take(settings.maxSegmentsPerFrame * WORD_OVERSCAN)
    }

    private fun samplerFor(frame: ScreenFrame, displayWidth: Int, displayHeight: Int): PixelSampler =
        PixelSampler { x, y ->
            val px = (x * frame.width / displayWidth).toInt().coerceIn(0, frame.width - 1)
            val py = (y * frame.height / displayHeight).toInt().coerceIn(0, frame.height - 1)
            runCatching { frame.bitmap.getPixel(px, py) }.getOrNull()
        }

    // ------------------------------------------------------------- session control

    private fun setPaused(next: Boolean) {
        if (!ScreenTranslateRuntime.status.value.isSessionActive) return
        paused = next
        overlay.setPainting(!next)
        overlay.updateBubble(
            opacityPercent = current.screenTranslation.bubbleOpacityPercent,
            paused = next,
            badge = bubbleBadge()
        )
        ScreenTranslateRuntime.setPhase(
            if (next) ScreenTranslatePhase.PAUSED else ScreenTranslatePhase.RUNNING
        )
        updateNotification(force = true)
    }

    private fun blockByEmergencyLock() {
        ScreenTranslateRuntime.setPhase(ScreenTranslatePhase.BLOCKED)
        ScreenTranslateRuntime.fail(
            "قفل اضطراری سایویس فعال است؛ ترجمهٔ زندهٔ صفحه متوقف شد.",
            "The SAYVIS emergency lock is engaged; live screen translation was stopped."
        )
        audit(
            action = "screen.translate.blocked",
            risk = RiskLevel.CRITICAL,
            result = "BLOCKED",
            auth = "BLOCKED_EMERGENCY_LOCK",
            digest = "Live screen translation blocked by the emergency lock"
        )
        stopSession(null, null)
    }

    /** Tears everything down; [messageFa]/[messageEn] optionally replace the final phase. */
    private fun stopSession(messageFa: String?, messageEn: String?) {
        releaseSession()
        ScreenTranslateRuntime.setPhase(ScreenTranslatePhase.STOPPED)
        if (messageFa != null && messageEn != null) {
            ScreenTranslateRuntime.fail(messageFa, messageEn)
        }
        runCatching { stopForeground(Service.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun releaseSession() {
        capture?.let { controller ->
            controller.release()
            controller.shutdown()
        }
        capture = null
        projection?.let { active ->
            runCatching { active.stop() }
        }
        projection = null
        overlay.detachAll()
        lastFingerprint = 0L
    }

    // ------------------------------------------------------------------ settings IO

    private fun observeSettings() {
        settingsObserver?.cancel()
        settingsObserver = scope.launch {
            settingsStore.settings.collect { fresh -> onSettingsChanged(fresh) }
        }
    }

    private fun onSettingsChanged(fresh: AppSettings) {
        val previous = current
        current = fresh
        ScreenTranslateRuntime.permissionsChanged(overlay.canDrawOverlays(), ScreenTranslateRuntime.status.value.captureConsentGranted)
        ScreenTranslateRuntime.enginesChanged(
            engineOnline = fresh.ai.provider != AiProviderKind.LOCAL &&
                fresh.ai.isProviderConfigured() && !fresh.forceOfflineMode,
            dictionaryOnly = fresh.screenTranslation.dictionaryOnly || fresh.forceOfflineMode
        )
        if (fresh.emergencyLockActive && ScreenTranslateRuntime.status.value.isSessionActive) {
            blockByEmergencyLock()
            return
        }
        if (!fresh.screenTranslation.enabled && ScreenTranslateRuntime.status.value.isSessionActive) {
            stopSession(null, null)
            return
        }
        val status = ScreenTranslateRuntime.status.value
        if (status.isSessionActive) {
            if (previous.screenTranslation.mode != fresh.screenTranslation.mode ||
                previous.screenTranslation.textScalePercent != fresh.screenTranslation.textScalePercent ||
                previous.screenTranslation.persianNumbers != fresh.screenTranslation.persianNumbers
            ) {
                // Force the next frame to be re-planned so appearance changes take effect.
                lastFingerprint = 0L
            }
            overlay.setKeepScreenOn(fresh.screenTranslation.keepScreenOn)
            if (fresh.screenTranslation.showControlBubble) {
                if (!overlay.attachBubble(
                        opacityPercent = fresh.screenTranslation.bubbleOpacityPercent,
                        paused = paused,
                        badge = bubbleBadge()
                    )
                ) {
                    Log.w(TAG, "Bubble could not be attached")
                }
            } else {
                overlay.detachBubble()
            }
        }
    }

    // ---------------------------------------------------------------- notification

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            strings().t("ترجمهٔ زندهٔ صفحه", "Live screen translation"),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = strings().t(
                "وضعیت ترجمهٔ زندهٔ صفحه و کنترل توقف آن",
                "Live screen translation status and its stop control"
            )
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val s = strings()
        val status = ScreenTranslateRuntime.status.value
        val text = when {
            paused -> s.t("ترجمه موقتاً متوقف است", "Translation is paused")
            status.segmentsTranslated > 0 -> s.t(
                "روی صفحه نوشته شد: ${status.segmentsTranslated} عبارت",
                "Written on screen: ${status.segmentsTranslated} phrases"
            )
            else -> s.t("در حال خواندن صفحه…", "Reading the screen…")
        }
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_OPEN_SCREEN_TRANSLATOR, true)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ScreenTranslatorService::class.java).setAction(
                if (paused) ACTION_RESUME else ACTION_PAUSE
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, ScreenTranslatorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_screen_translate)
            .setContentTitle(s.t("مترجم زندهٔ صفحهٔ سایویس", "SAYVIS live screen translator"))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                if (paused) s.t("ادامه", "Resume") else s.t("توقف موقت", "Pause"),
                pauseIntent
            )
            .addAction(0, s.t("پایان ترجمه", "Stop translation"), stopIntent)
            .build()
    }

    private fun updateNotification(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationAt < NOTIFICATION_THROTTLE_MS) return
        lastNotificationAt = now
        ensureChannel()
        // The bubble carries the running counter, so the owner can see progress without
        // pulling down the notification shade.
        overlay.updateBubble(
            opacityPercent = current.screenTranslation.bubbleOpacityPercent,
            paused = paused,
            badge = bubbleBadge()
        )
        val manager = getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
    }

    // --------------------------------------------------------------------- helpers

    private fun strings(): SayvisStrings =
        SayvisStrings.of(current.isPersian(SayvisStrings.deviceIsPersian()))

    private fun bubbleBadge(): String {
        val status = ScreenTranslateRuntime.status.value
        return if (status.segmentsTranslated > 99) "+99" else status.segmentsTranslated.toString()
    }

    private fun displayWidthPx(): Int {
        val metrics = resources.displayMetrics
        return metrics.widthPixels.coerceAtLeast(1)
    }

    private fun displayHeightPx(): Int {
        val metrics = resources.displayMetrics
        return metrics.heightPixels.coerceAtLeast(1)
    }

    private fun audit(
        action: String,
        risk: RiskLevel,
        result: String,
        digest: String,
        auth: String = "OWNER_CONFIRMED"
    ) {
        if (!current.keepAuditLogOnDevice) return
        val target = repository ?: return
        scope.launch {
            runCatching {
                target.recordAuditEvent(
                    actor = "OWNER",
                    action = action,
                    riskLevel = risk,
                    auth = auth,
                    result = result,
                    digest = digest,
                    deviceId = "dev_android_primary"
                )
            }
        }
    }

    companion object {
        private const val TAG = "ScreenTranslator"
        private const val CHANNEL_ID = "sayvis_screen_translator"
        private const val NOTIFICATION_ID = 4211
        private const val NOTIFICATION_THROTTLE_MS = 2500L
        private const val WORD_OVERSCAN = 3

        const val ACTION_START = "com.example.sayvis.screentranslate.START"
        const val ACTION_STOP = "com.example.sayvis.screentranslate.STOP"
        const val ACTION_PAUSE = "com.example.sayvis.screentranslate.PAUSE"
        const val ACTION_RESUME = "com.example.sayvis.screentranslate.RESUME"
        const val ACTION_REFRESH = "com.example.sayvis.screentranslate.REFRESH"

        const val EXTRA_RESULT_CODE = "sayvis.capture.result_code"
        const val EXTRA_CAPTURE_DATA = "sayvis.capture.data"
        const val EXTRA_OPEN_SCREEN_TRANSLATOR = "sayvis.open_screen_translator"

        /** Starts the session. Must be called while the app is in the foreground. */
        fun start(context: Context, resultCode: Int, captureData: Intent) {
            val intent = Intent(context, ScreenTranslatorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_CAPTURE_DATA, captureData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun send(context: Context, action: String) {
            val intent = Intent(context, ScreenTranslatorService::class.java).setAction(action)
            runCatching { context.startService(intent) }
        }

        fun stop(context: Context) = send(context, ACTION_STOP)
    }
}
