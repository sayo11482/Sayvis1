package com.example.sayvis.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SAYVIS v5.3.0 — LINK CENTER (مرکز اتصال)
 *
 * One owner-visible switch + one source of truth for the whole app:
 *
 *  1) GATE   — when the owner presses the ONLINE/OFFLINE button, every
 *              outgoing request built on [SayvisNet.shared] (AI, market,
 *              web search, GitHub, Google, gateway…) is rejected inside the
 *              client itself, BEFORE any socket opens. This is a real,
 *              app-wide cut — not a UI flag. (An OS-wide cut would need a
 *              VPN service with user consent; the honest boundary.)
 *  2) SPEED  — a small live download measurement (Cloudflare down-link),
 *              refreshed while online, surfaced on the Home card.
 *  3) CODES  — a status/fault-code registry the whole app reports through,
 *              e.g. ۰۱ = «در حال فکر کردن». Visible history, testable labels.
 *  4) TICK   — a 5-second liveness heartbeat the UI collects so data stays
 *              current without anyone re-opening a screen.
 */
object LinkCenter {

    // ------------------------------------------------------------------ gate

    /** Thrown by the SayvisNet gate when the owner switched the app offline. */
    class OfflineException : java.io.IOException("SAYVIS gate: owner set the app OFFLINE")

    private val gateOpenFlag = AtomicBoolean(true)

    /** True = requests may leave the app. Flipped only by the owner's switch. */
    val gateOpen: Boolean get() = gateOpenFlag.get()

    /** Called by the ViewModel when the owner presses the green/red pill. */
    fun setGateOpen(open: Boolean) {
        gateOpenFlag.set(open)
        push(if (open) CODE.NET_CHECKING else CODE.OFFLINE_SWITCH)
    }

    /** The interceptor installed in [SayvisNet.shared] — throws before any I/O. */
    fun gateInterceptor(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        if (!gateOpenFlag.get()) throw OfflineException()
        return chain.proceed(chain.request())
    }

    // ----------------------------------------------------------------- speed

    data class SpeedResult(
        val mbps: Double,
        val bytes: Int,
        val millis: Long,
        val at: Long = System.currentTimeMillis()
    )

    private val _speed = MutableStateFlow<SpeedResult?>(null)
    val speed: StateFlow<SpeedResult?> = _speed.asStateFlow()

    /** Pure maths (unit-tested): bytes over millis → Mbps (network megabits). */
    fun computeMbps(bytes: Int, millis: Long): Double? {
        if (bytes <= 0 || millis <= 0) return null
        val bits = bytes * 8.0
        val mbps = bits / (millis / 1000.0) / 1_000_000.0
        return if (mbps.isFinite() && mbps > 0.0) (mbps * 10).let { kotlin.math.round(it) / 10.0 } else null
    }

    /**
     * Real download measurement. Runs on its OWN client (not SayvisNet.shared)
     * but only ever while the gate is OPEN — when the owner cuts the internet,
     * this measurer goes silent too (no hidden traffic).
     */
    suspend fun measureSpeed(): SpeedResult? = withContext(Dispatchers.IO) {
        if (!gateOpenFlag.get()) return@withContext null
        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val urls = listOf(
            "https://speed.cloudflare.com/__down?bytes=262144",
            "https://www.baidu.com/favicon.ico"
        )
        for (url in urls) {
            val started = System.currentTimeMillis()
            runCatching {
                client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val bytes = resp.body?.bytes()?.size ?: 0
                    val ms = System.currentTimeMillis() - started
                    computeMbps(bytes, ms)?.let { mbps ->
                        val result = SpeedResult(mbps, bytes, ms)
                        _speed.value = result
                        return@withContext result
                    }
                }
            }
        }
        null
    }

    // ----------------------------------------------------------------- codes

    /** Owner-facing operation/fault codes (کد ۰۱ = در حال فکر کردن). */
    enum class CODE(val code: String, val fa: String, val en: String) {
        NET_CHECKING("00", "در حال بررسی اتصال", "Checking link"),
        THINKING("01", "در حال فکر کردن", "Thinking"),
        OFFLINE_SWITCH("02", "آفلاین — کل اتصال اپ قطع است", "Offline — app links cut"),
        NET_DOWN("03", "اینترنت در دسترس نیست", "No internet"),
        AI_LINKED("04", "هوش مصنوعی متصل شد (به حافظه سپرده شد)", "AI linked (remembered)"),
        AI_FALLBACK("05", "تعویض مدل هوش مصنوعی", "AI model switched"),
        NET_NO_GOOGLE("06", "اینترنت هست؛ گوگل مسدود", "Internet ok; Google blocked"),
        MARKET_LIVE("07", "مارکت لایو به‌روز شد", "Live market refreshed"),
        AGENT_WORKING("08", "ایجنت در حال کار", "Agent working"),
        DOSSIER_SYNC("09", "پروندهٔ شناختی به‌روز شد", "Cognitive file updated"),
        GITHUB_SCAN("10", "اسکن گیت‌هاب", "GitHub scan");

        fun label(persian: Boolean): String = if (persian) fa else en
    }

    data class StatusEvent(val code: CODE, val detail: String = "", val at: Long = System.currentTimeMillis())

    private val _currentStatus = MutableStateFlow(StatusEvent(CODE.NET_CHECKING))
    val currentStatus: StateFlow<StatusEvent> = _currentStatus.asStateFlow()

    private val _statusHistory = MutableStateFlow<List<StatusEvent>>(emptyList())
    val statusHistory: StateFlow<List<StatusEvent>> = _statusHistory.asStateFlow()

    /** Report an operation/fault code from anywhere in the app (never throws). */
    fun push(code: CODE, detail: String = "") {
        val event = StatusEvent(code, detail)
        _currentStatus.value = event
        val next = ArrayList<StatusEvent>(_statusHistory.value)
        next.add(0, event)
        _statusHistory.value = next.take(30)
    }

    // ------------------------------------------------------------------ tick

    private val _liveTick = MutableStateFlow(0L)
    val liveTick: StateFlow<Long> = _liveTick.asStateFlow()

    /** Heartbeat tick — called from the ViewModel's liveness loop. */
    fun tick() {
        _liveTick.value = _liveTick.value + 1
    }

    // --------------------------------------------------------- ai memory hook

    private val _aiMemoryNotice = MutableStateFlow<String>("")
    val aiMemoryNotice: StateFlow<String> = _aiMemoryNotice.asStateFlow()

    /** One-shot notice when the first successful AI round-trip is remembered. */
    fun noteAiMemory(message: String) {
        _aiMemoryNotice.value = message
    }
}
