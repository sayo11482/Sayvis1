package com.example.sayvis.net

import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * SAYVIS rebuilt network layer (v4.0.0) — ONE shared engine for every
 * outgoing connection in the app.
 *
 * Before this class each service built its own OkHttpClient; now everything
 * (AI providers, market data, web search, GitHub scans, Google tokens, the
 * connectivity probe, the MT gateway) inherits from a single shared client:
 *
 *  - one connection pool + dispatcher  (faster, fewer sockets);
 *  - a uniform SAYVIS User-Agent on every request;
 *  - the owner's linked Google account attached as `X-Sayvis-Account`
 *    (identity centre: all AI/network usage is attributable to the account);
 *  - a pure, unit-tested retry/backoff calculator for idempotent calls.
 *
 * Per-call timeouts keep their meaning through [client], which derives from
 * the shared instance (pool and interceptors are inherited, not duplicated).
 */
object SayvisNet {

    /** The linked Google account e-mail — set by GoogleAccountHub at link time. */
    @Volatile
    var identityAccount: String = ""

    const val USER_AGENT: String = "SAYVIS/4.0 (Android; personal robot assistant)"

    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(6, 3, TimeUnit.MINUTES))
            .addInterceptor(IDENTITY)
            .addInterceptor { chain -> com.example.sayvis.net.LinkCenter.gateInterceptor(chain) }
            .build()
    }

    /** Short-timeout variant for probes (shares the same pool). */
    fun probeClient(connectSeconds: Int, readSeconds: Int): OkHttpClient =
        shared.newBuilder()
            .connectTimeout(connectSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(readSeconds.toLong(), TimeUnit.SECONDS)
            .callTimeout((connectSeconds + readSeconds + 2).toLong(), TimeUnit.SECONDS)
            .build()

    /** Standard-timeout variant for API calls (shares the same pool). */
    fun client(connectSeconds: Int, readSeconds: Int): OkHttpClient =
        shared.newBuilder()
            .connectTimeout(connectSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(readSeconds.toLong(), TimeUnit.SECONDS)
            .build()

    /**
     * Pure exponential backoff for idempotent retries: 600ms, 1200ms, 2400ms …
     * capped at [capMs]. Unit-tested — no jitter so CI stays deterministic.
     */
    fun backoffDelayMs(attempt: Int, baseMs: Long = 600L, capMs: Long = 8000L): Long {
        val safeAttempt = attempt.coerceIn(0, 16)
        val raw = baseMs * (1L shl safeAttempt)
        return raw.coerceAtMost(capMs)
    }

    /** GET builder with the identity headers already applied. */
    fun get(url: String): Request.Builder =
        Request.Builder().url(url).get()

    /** POST builder with the identity headers already applied. */
    fun post(url: String, body: RequestBody): Request.Builder =
        Request.Builder().url(url).post(body)

    private val IDENTITY = Interceptor { chain ->
        val builder = chain.request().newBuilder().header("User-Agent", USER_AGENT)
        val account = identityAccount
        if (account.isNotBlank()) builder.header("X-Sayvis-Account", account)
        chain.proceed(builder.build())
    }
}
