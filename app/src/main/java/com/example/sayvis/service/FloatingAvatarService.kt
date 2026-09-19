package com.example.sayvis.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.sayvis.voice.AmbientListenerCore
import com.example.sayvis.voice.AvatarListenController
import com.example.sayvis.voice.ListenBus
import com.example.sayvis.voice.VoicePrintStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * Foreground service that keeps a small SAYVIS avatar floating over the launcher
 * (and every other app) after the owner presses Home, while a live microphone loop
 * listens for the owner's enrolled voice.
 *
 * - Overlay: TYPE_APPLICATION_OVERLAY chat-head style bubble — tap opens SAYVIS,
 *   drag moves it, long-press stops it.
 * - Mic: [AmbientListenerCore] runs a VAD + voice-print matcher; an owner hit turns
 *   the bubble green and is logged to the on-device history.
 *
 * Everything is on-device; Android's own microphone indicator stays visible while
 * the service listens, and the notification offers a permanent stop action.
 */
class FloatingAvatarService : Service() {

    companion object {
        const val ACTION_START = "com.example.sayvis.avatar.START"
        const val ACTION_STOP = "com.example.sayvis.avatar.STOP"

        private const val CHANNEL_ID = "sayvis_avatar_listen"
        private const val NOTIFICATION_ID = 4211
        private const val AVATAR_SIZE_DP = 56f
    }

    private var mainScope: CoroutineScope? = null
    private var core: AmbientListenerCore? = null
    private var overlayView: AvatarOverlayView? = null
    private var windowManager: WindowManager? = null
    private var promotedToForeground = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mainScope == null) {
            mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
        if (intent?.action == ACTION_STOP) {
            shutDown()
            return START_NOT_STICKY
        }

        // Must always reach startForeground promptly after a startForegroundService.
        promoteToForeground()

        val micOk = AvatarListenController.hasMicPermission(this)
        val overlayOk = AvatarListenController.hasOverlayPermission(this)
        if (!micOk || !overlayOk) {
            Toast.makeText(this, R.string.avatar_missing_permissions, Toast.LENGTH_LONG).show()
            shutDown()
            return START_NOT_STICKY
        }

        if (!addOverlay()) {
            shutDown()
            return START_NOT_STICKY
        }

        startMicLoop()
        ListenBus.setMode(ListenBus.Mode.LISTENING)
        return START_STICKY
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------ pieces

    private fun startMicLoop() {
        if (core?.isRunning == true) return
        val store = VoicePrintStore.get(this)
        val listener = AmbientListenerCore(
            store,
            onLevel = { ListenBus.publishLevel(it) },
            onOwnerVoice = { score ->
                val at = System.currentTimeMillis()
                store.addHistory(ListenBus.ListenEvent(at, score))
                ListenBus.pushOwnerEvent(at, score)
                com.example.sayvis.voice.RoboticAudio.playAck(this@FloatingAvatarService)
            }
        )
        if (listener.start()) {
            core = listener
        } else {
            Toast.makeText(this, R.string.avatar_mic_start_failed, Toast.LENGTH_LONG).show()
            shutDown()
        }
    }

    private fun addOverlay(): Boolean {
        if (overlayView != null) return true
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val sizePx = (resources.displayMetrics.density * AVATAR_SIZE_DP).toInt()
        val store = VoicePrintStore.get(this)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = store.overlayX()
        params.y = store.overlayY()

        return try {
            val view = AvatarOverlayView(this)
            wm.addView(view, params)
            overlayView = view
            windowManager = wm
            view.onTap = { openApp() }
            view.onStopped = {
                Toast.makeText(this, R.string.avatar_stopped_toast, Toast.LENGTH_SHORT).show()
                shutDown()
            }
            val scope = mainScope
            if (scope != null) {
                scope.launch { ListenBus.level.collect { view.setAmplitude(it) } }
                scope.launch { ListenBus.mode.collect { view.setMode(it) } }
            }
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            false
        }
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun promoteToForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        promotedToForeground = true
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingAvatarService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notif_avatar_title))
            .setContentText(getString(R.string.notif_avatar_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_action_open), openIntent)
            .addAction(0, getString(R.string.notif_action_stop), stopIntent)
            .build()
    }

    private fun createChannel() {
        val manager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(getString(R.string.notif_channel_avatar))
            .setShowBadge(false)
            .build()
        manager.createNotificationChannel(channel)
    }

    private fun shutDown() {
        core?.stop()
        core = null
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        windowManager = null
        mainScope?.cancel()
        mainScope = null
        ListenBus.setMode(ListenBus.Mode.OFF)
        ListenBus.publishLevel(0f)
        if (promotedToForeground) {
            promotedToForeground = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    /**
     * Chat-head style overlay view: dark disc, rotating orbital ring with nodes and
     * an amplitude-driven core. Cyan while listening, green right after the owner's
     * voice is recognised, silver when no voice-print is enrolled yet.
     */
    private class AvatarOverlayView(context: Context) : View(context) {

        var onTap: (() -> Unit)? = null
        var onStopped: (() -> Unit)? = null

        private val handler = Handler(Looper.getMainLooper())

        @Volatile
        private var amplitude = 0f
        private var mode = ListenBus.Mode.OFF
        private var rotation = 0f
        private var smoothAmplitude = 0f

        private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = BACKDROP
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = resources.displayMetrics.density * 2f
        }
        private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        private val frameTicker = object : Runnable {
            override fun run() {
                rotation = (rotation + 1.2f) % 360f
                smoothAmplitude += (amplitude - smoothAmplitude) * 0.25f
                invalidate()
                handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }

        // ------------------------------------------------------------ inputs

        fun setAmplitude(value: Float) {
            amplitude = value.coerceIn(0f, 1f)
        }

        fun setMode(next: ListenBus.Mode) {
            mode = next
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            handler.post(frameTicker)
        }

        override fun onDetachedFromWindow() {
            handler.removeCallbacks(frameTicker)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val center = width / 2f
            val radius = width / 2f * 0.78f

            val owner = mode == ListenBus.Mode.OWNER_VOICE
            val active = mode != ListenBus.Mode.OFF
            val hasPrint = VoicePrintStore.get(context).hasPrint()
            val accent = when {
                owner -> GREEN
                active && hasPrint -> CYAN
                active -> SILVER
                else -> SILVER
            }

            canvas.drawCircle(center, center, radius * 1.04f, discPaint)

            // Soft ambient glow, scaled by the live level.
            val glowRadius = radius * (1.0f + 0.35f * smoothAmplitude)
            glowPaint.shader = RadialGradient(
                center, center, glowRadius,
                withAlpha(accent, if (active) 0.35f else 0.15f),
                Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(center, center, glowRadius, glowPaint)

            // Rotating orbital ring with alternating nodes.
            canvas.save()
            canvas.rotate(rotation, center, center)
            ringPaint.color = withAlpha(accent, if (active) 0.85f else 0.4f)
            canvas.drawCircle(center, center, radius * 0.86f, ringPaint)
            val nodeRadius = resources.displayMetrics.density * 2.6f
            for (i in 0 until 6) {
                val angle = Math.toRadians((i * 60.0))
                nodePaint.color = if (i % 2 == 0) GOLD else accent
                canvas.drawCircle(
                    (center + radius * 0.86f * Math.cos(angle)).toFloat(),
                    (center + radius * 0.86f * Math.sin(angle)).toFloat(),
                    nodeRadius,
                    nodePaint
                )
            }
            canvas.restore()

            // Amplitude-driven core.
            val coreRadius = radius * (0.34f + 0.30f * smoothAmplitude)
            corePaint.shader = RadialGradient(
                center, center, max(coreRadius, 1f),
                withAlpha(accent, if (active) 0.95f else 0.5f),
                withAlpha(accent, 0.08f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(center, center, coreRadius, corePaint)
        }

        // ------------------------------------------------------------- touch

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startWindowX = 0
        private var startWindowY = 0
        private var dragging = false
        private var longPressFired = false

        private val longPressRunnable = Runnable {
            if (!dragging) {
                longPressFired = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onStopped?.invoke()
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    val params = layoutParams as WindowManager.LayoutParams
                    startWindowX = params.x
                    startWindowY = params.y
                    dragging = false
                    longPressFired = false
                    handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                        handler.removeCallbacks(longPressRunnable)
                    }
                    if (dragging) {
                        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                        val params = layoutParams as WindowManager.LayoutParams
                        val metrics = resources.displayMetrics
                        params.x = (startWindowX + dx).toInt().coerceIn(0, max(0, metrics.widthPixels - width))
                        params.y = (startWindowY + dy).toInt().coerceIn(0, max(0, metrics.heightPixels - height))
                        runCatching { wm.updateViewLayout(this, params) }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (dragging) {
                        val params = layoutParams as WindowManager.LayoutParams
                        VoicePrintStore.get(context).saveOverlayPosition(params.x, params.y)
                    } else if (!longPressFired && event.actionMasked == MotionEvent.ACTION_UP) {
                        onTap?.invoke()
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        companion object {
            private const val FRAME_INTERVAL_MS = 33L
            private const val LONG_PRESS_MS = 600L

            // SAYVIS theme colours mirrored for the classic-View overlay canvas.
            private val CYAN = 0xFFD4AF37.toInt()
            private val GOLD = 0xFFF3CA68.toInt()
            private val GREEN = 0xFF10B981.toInt()
            private val SILVER = 0xFF94A3B8.toInt()
            private val BACKDROP = 0xD90B1220.toInt()

            private fun withAlpha(color: Int, alphaFraction: Float): Int =
                (color and 0x00FFFFFF) or (((alphaFraction.coerceIn(0f, 1f) * 255f).toInt()) shl 24)
        }
    }
}
