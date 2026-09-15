package com.example.sayvis.trading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Live, keyless market data for the SAYVIS trading terminal. Every backend
 * was live-audited before wiring:
 *   • CoinGecko simple price  → PAXG (tokenised gold ≈ XAU) + USDT in USD;
 *   • Frankfurter (ECB)       → EUR/USD live series (also the candles);
 *   • open.er-api.com         → official USD→IRR;
 *   • Nobitex                 → USDT/IRT free-market try-first, graceful
 *                               fall back to the labelled official estimate.
 * Never throws to the caller — failures become honest source messages.
 */
class MarketDataService {

    enum class Symbol(val labelFa: String, val labelEn: String, val tvSymbol: String?) {
        XAUUSD("طلا (انس)", "Gold (XAU)", "OANDA:XAUUSD"),
        EURUSD("یورو/دلار", "EUR/USD", "OANDA:EURUSD"),
        USDTIRT("تتر/تومان", "USDT/IRT", null),
        USDIRR("دلار/ریال", "USD/IRR", "FX_IDC:USDIRR");

        companion object {
            fun from(tvSymbol: String): Symbol? = entries.firstOrNull { it.tvSymbol == tvSymbol }
        }
    }

    data class Quote(
        val symbol: Symbol,
        val price: Double,
        val change24hPercent: Double?,
        val source: String,
        val estimated: Boolean,
        val asOfEpochMs: Long
    )

    data class Snapshot(
        val quotes: Map<Symbol, Quote>,
        val series: Map<Symbol, List<Double>>,
        val failures: List<String>
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun refreshAll(): Snapshot = withContext(Dispatchers.IO) {
        coroutineScope {
            val gold = async { runCatching { goldQuote() }.getOrNull() }
            val eur = async { runCatching { eurusdSeries() }.getOrNull() }
            val irr = async { runCatching { usdIrrQuote() }.getOrNull() }
            val usdt = async { runCatching { usdtIrtQuote(irr.await()?.price) }.getOrNull() }
            val goldSeries = async { runCatching { goldSeries() }.getOrNull() }

            val quotes = HashMap<Symbol, Quote>()
            val series = HashMap<Symbol, List<Double>>()
            val failures = ArrayList<String>()

            val goldQuote = gold.await()
            if (goldQuote != null) quotes[goldQuote.symbol] = goldQuote else failures += "gold quote"

            val eurData = eur.await()
            if (eurData != null) {
                quotes[Symbol.EURUSD] = eurData.first
                if (eurData.second.size >= 30) series[Symbol.EURUSD] = eurData.second
            } else failures += "EURUSD series"

            val irrQuote = irr.await()
            if (irrQuote != null) quotes[irrQuote.symbol] = irrQuote else failures += "USD/IRR quote"

            val usdtQuote = usdt.await()
            if (usdtQuote != null) quotes[usdtQuote.symbol] = usdtQuote else failures += "USDT/IRT quote"

            val goldCandles = goldSeries.await()
            if (goldCandles != null && goldCandles.size >= 30) series[Symbol.XAUUSD] = goldCandles

            Snapshot(quotes, series, failures)
        }
    }

    // ---------------------------------------------------------------- quotes

    suspend fun goldQuote(): Quote = withContext(Dispatchers.IO) {
        val body = get("https://api.coingecko.com/api/v3/simple/price?ids=pax-gold&vs_currencies=usd&include_24hr_change=true")
        Quote(
            Symbol.XAUUSD,
            number(body, "usd") ?: error("no price"),
            number(body, "usd_24h_change"),
            "PAXG ≈ XAU (CoinGecko)",
            estimated = false,
            asOfEpochMs = now()
        )
    }

    suspend fun usdtUsd(): Pair<Double, Double?> = withContext(Dispatchers.IO) {
        val body = get("https://api.coingecko.com/api/v3/simple/price?ids=tether&vs_currencies=usd&include_24hr_change=true")
        (number(body, "usd") ?: error("no tether price")) to number(body, "usd_24h_change")
    }

    suspend fun usdIrrQuote(): Quote = withContext(Dispatchers.IO) {
        val body = get("https://open.er-api.com/v6/latest/USD")
        val irr = number(body, "IRR") ?: error("no IRR")
        Quote(
            Symbol.USDIRR, irr, null,
            "official (exchangerate-api)",
            estimated = false,
            asOfEpochMs = now()
        )
    }

    /** Free-market USDT/IRT try-first (Nobitex), else the official estimate. */
    suspend fun usdtIrtQuote(irrOfficial: Double?): Quote = withContext(Dispatchers.IO) {
        // 1) Iranian free market (reachable from Iran-side networks).
        runCatching {
            val body = get("https://api.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls")
            val rls = number(body, "latest") ?: error("nobitex missing latest")
            Quote(
                Symbol.USDTIRT, rls / 10.0, null, "Nobitex (بازار آزاد)",
                estimated = false, asOfEpochMs = now()
            )
        }.getOrElse {
            // 2) Labelled official estimate: (USDT in USD) × (USD→IRR) / 10.
            val (usdtUsd, change) = usdtUsd()
            if (irrOfficial != null) {
                Quote(
                    Symbol.USDTIRT, usdtUsd * irrOfficial / 10.0, change,
                    "برآورد رسمی (CoinGecko × نرخ رسمی)",
                    estimated = true, asOfEpochMs = now()
                )
            } else {
                Quote(
                    Symbol.USDTIRT, usdtUsd, change,
                    "USDT/USD (CoinGecko)",
                    estimated = false, asOfEpochMs = now()
                )
            }
        }
    }

    // ---------------------------------------------------------------- series

    /** EUR/USD daily closes (ECB via Frankfurter) — doubles as candles. */
    suspend fun eurusdSeries(): Pair<Quote, List<Double>> = withContext(Dispatchers.IO) {
        val body = get("https://api.frankfurter.dev/v1/2026-06-01..?from=EUR&to=USD")
        val closes = parseFrankfurterSeries(body)
        require(closes.isNotEmpty()) { "empty frankfurter series" }
        val quote = Quote(
            Symbol.EURUSD, closes.last(), null, "ECB (Frankfurter)",
            estimated = false, asOfEpochMs = now()
        )
        quote to closes
    }

    /** Gold hourly-ish OHLC closes (PAXG) for the strategy engine. */
    suspend fun goldSeries(): List<Double> = withContext(Dispatchers.IO) {
        val body = get("https://api.coingecko.com/api/v3/coins/pax-gold/ohlc?vs_currency=usd&days=30")
        parseCoinGeckoOhlc(body).map { it[3] }
    }

    // --------------------------------------------------------------- parsing

    /** Pure: Frankfurter {"rates":{"2026-..":{"USD":1.15…},…}} → sorted closes. */
    fun parseFrankfurterSeries(json: String): List<Double> {
        val rates = Regex("\\\"(\\d{4}-\\d{2}-\\d{2})\\\"\\s*:\\s*\\{\\s*\\\"USD\\\"\\s*:\\s*([0-9.]+)")
            .findAll(json)
            .map { it.groupValues[1] to it.groupValues[2].toDouble() }
            .toList()
        return rates.sortedBy { it.first }.map { it.second }
    }

    /** Pure: CoinGecko OHLC [[ts,o,h,l,c],…] → full rows (close = index 4). */
    fun parseCoinGeckoOhlc(json: String): List<List<Double>> {
        val inner = json.substringAfter('[', "").substringBeforeLast(']', "")
        if (inner.isBlank()) return emptyList()
        return inner.split(']').mapNotNull { chunk ->
            Regex("([0-9.]+)").findAll(chunk).toList()
                .takeIf { it.size >= 5 }
                ?.map { it.groupValues[1].toDouble() }
        }
    }

    /** Pure: {"rates":{…"IRR":1466919…}} → IRR (also Nobitex {"stats":{…}}). */
    fun number(body: String, field: String): Double? {
        Regex("\\\"$field\\\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").find(body)
            ?.let { return it.groupValues[1].toDoubleOrNull() }
        return null
    }

    // --------------------------------------------------------------- helpers

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 SayvisBot/1.0")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return body
        }
    }

    private fun now(): Long = System.currentTimeMillis()
}
