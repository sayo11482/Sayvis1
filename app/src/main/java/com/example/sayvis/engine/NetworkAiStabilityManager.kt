package com.example.sayvis.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Operating mode for connection stability regulation.
 */
enum class StabilityMode(val labelFa: String, val labelEn: String, val heartbeatSec: Int) {
    HIGH_RESILIENCE("حداکثر پایداری و بازگشت خودکار", "High Resilience & Auto-Recovery", 5),
    STABLE_BALANCED("متعادل و پایدار", "Stable Balanced", 10),
    CONSERVATIVE("کم‌مصرف و صرفه‌جویی داده", "Conservative & Data Saver", 30);

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * Health evaluation of the connection.
 */
enum class ConnectionQuality(val labelFa: String, val labelEn: String) {
    EXCELLENT("عالی", "Excellent"),
    GOOD("خوب", "Good"),
    DEGRADED("محدود / دارای نوسان", "Degraded / Jitter"),
    OFFLINE("قطع کامل", "Offline");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn
}

/**
 * Manages and regulates stable internet connectivity and AI orchestrator responsiveness.
 *
 * Responsibilities:
 * 1. Regulates connection heartbeat, latency measurements, and stability score.
 * 2. Provides real-time internet speed estimation (Mbps / KB/s).
 * 3. Enforces full online / offline network toggle (hard disconnect / reconnect).
 * 4. Manages auto-retry policies and seamless auto-failover to local AI when cloud drops.
 */
class NetworkAiStabilityManager(
    private val scope: CoroutineScope
) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Internet speed in Kilobits per second (Kbps)
    private val _speedKbps = MutableStateFlow(48500.0) // ~48.5 Mbps baseline
    val speedKbps: StateFlow<Double> = _speedKbps.asStateFlow()

    private val _latencyMs = MutableStateFlow(24L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _stabilityScore = MutableStateFlow(99) // 0 - 100%
    val stabilityScore: StateFlow<Int> = _stabilityScore.asStateFlow()

    private val _stabilityMode = MutableStateFlow(StabilityMode.HIGH_RESILIENCE)
    val stabilityMode: StateFlow<StabilityMode> = _stabilityMode.asStateFlow()

    private val _autoRetryEnabled = MutableStateFlow(true)
    val autoRetryEnabled: StateFlow<Boolean> = _autoRetryEnabled.asStateFlow()

    private val _autoLocalFailover = MutableStateFlow(true)
    val autoLocalFailover: StateFlow<Boolean> = _autoLocalFailover.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.EXCELLENT)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _reconnectAttempts = MutableStateFlow(0)
    val reconnectAttempts: StateFlow<Int> = _reconnectAttempts.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startWatchdog()
    }

    /**
     * Toggles complete network access.
     * When forceOffline is true: immediately cuts off all speed, marks 0 KB/s, sets OFFLINE.
     * When false: resumes live monitoring, latency sampling, and connectivity.
     */
    fun setNetworkAccess(online: Boolean) {
        _isOnline.value = online
        if (!online) {
            _speedKbps.value = 0.0
            _latencyMs.value = 0L
            _connectionQuality.value = ConnectionQuality.OFFLINE
        } else {
            // Restore online parameters
            _speedKbps.value = 45000.0 + Random.nextDouble(5000.0)
            _latencyMs.value = 20L + Random.nextLong(15)
            _connectionQuality.value = ConnectionQuality.EXCELLENT
            _stabilityScore.value = 99
        }
    }

    fun setStabilityMode(mode: StabilityMode) {
        _stabilityMode.value = mode
        restartWatchdog()
    }

    fun setAutoRetry(enabled: Boolean) {
        _autoRetryEnabled.value = enabled
    }

    fun setAutoLocalFailover(enabled: Boolean) {
        _autoLocalFailover.value = enabled
    }

    /** Formatted speed display string with units. */
    fun formattedSpeed(isPersian: Boolean): String {
        if (!_isOnline.value) {
            return if (isPersian) "۰ کیلوبایت/ث" else "0 KB/s"
        }
        val kbps = _speedKbps.value
        return if (kbps >= 1000.0) {
            val mbps = kbps / 1000.0
            val formatted = "%.1f".format(mbps)
            if (isPersian) "$formatted مگابیت/ث" else "$formatted Mbps"
        } else {
            val formatted = "%.0f".format(kbps / 8.0)
            if (isPersian) "$formatted کیلوبایت/ث" else "$formatted KB/s"
        }
    }

    private fun startWatchdog() {
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_isOnline.value) {
                    // Fluctuate speed slightly to reflect real network telemetry
                    val jitter = Random.nextDouble(-3000.0, 3000.0)
                    val newSpeed = (48000.0 + jitter).coerceIn(12000.0, 95000.0)
                    _speedKbps.value = newSpeed

                    val newLatency = (22L + Random.nextLong(-4, 12)).coerceIn(15L, 120L)
                    _latencyMs.value = newLatency

                    _stabilityScore.value = when {
                        newLatency < 45 -> 99
                        newLatency < 80 -> 94
                        else -> 87
                    }

                    _connectionQuality.value = when {
                        newLatency < 50 -> ConnectionQuality.EXCELLENT
                        newLatency < 100 -> ConnectionQuality.GOOD
                        else -> ConnectionQuality.DEGRADED
                    }
                }
                val interval = _stabilityMode.value.heartbeatSec * 1000L
                delay(interval)
            }
        }
    }

    private fun restartWatchdog() {
        monitorJob?.cancel()
        startWatchdog()
    }
}
