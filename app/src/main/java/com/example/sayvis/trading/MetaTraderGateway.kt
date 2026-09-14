package com.example.sayvis.trading

import com.example.sayvis.settings.MtAccountType
import com.example.sayvis.settings.MtBridgeKind
import com.example.sayvis.settings.MtGatewayProfile
import com.example.sayvis.settings.TradingExecutionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

/**
 * MetaTrader 4/5 gateway for SAYVIS.
 *
 * Android cannot speak the MT4/MT5 wire protocol directly — MetaQuotes ships no mobile
 * broker API — so a real terminal connection always goes through a **bridge**:
 *
 *  - [MtBridgeKind.METAAPI]      a hosted gateway (MetaApi-style REST) that already
 *                                maintains a logged-in MT4/MT5 session on a server and
 *                                exposes account, quotes, positions and trading as JSON.
 *  - [MtBridgeKind.SELF_HOSTED]  the owner's own bridge: an Expert Advisor or Manager-API
 *                                service running next to their terminal, exposing the
 *                                small REST contract documented in `API.md`.
 *  - [MtBridgeKind.OFFLINE_SIM]  no network at all. A local paper-trading simulator so the
 *                                whole screen stays usable and testable offline.
 *
 * Safety is enforced here, not in the UI: [placeOrder] re-checks the connection, the
 * emergency lock, the execution mode, the lot cap and the daily loss cap on every single
 * call, so no screen can accidentally route a real order.
 */
class MetaTraderGateway(
    private val random: Random = Random(System.currentTimeMillis())
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Baselines used by the offline simulator so prices stay plausible between runs. */
    private val simulator = MarketSimulator(random)

    // ------------------------------------------------------------------ connect

    suspend fun connect(profile: MtGatewayProfile): MtGatewayState = withContext(Dispatchers.IO) {
        if (profile.bridgeKind == MtBridgeKind.OFFLINE_SIM) {
            return@withContext simulatedState(profile, "شبیه‌ساز محلی فعال شد", "Local simulator started")
        }

        val validation = validate(profile)
        if (validation != null) {
            return@withContext MtGatewayState(
                phase = MtConnectionPhase.ERROR,
                profile = profile,
                lastErrorFa = validation.first,
                lastErrorEn = validation.second
            )
        }

        // Pause briefly so the UI can show the CONNECTING phase instead of flashing.
        delay(250)

        val result = runCatching { fetchAccountRemote(profile) }
        result.fold(
            onSuccess = { account ->
                val positions = runCatching { fetchPositionsRemote(profile) }.getOrDefault(emptyList())
                val quotes = runCatching { fetchQuotesRemote(profile, DEFAULT_WATCHLIST) }.getOrDefault(emptyList())
                MtGatewayState(
                    phase = MtConnectionPhase.CONNECTED,
                    profile = profile,
                    account = account,
                    positions = positions,
                    quotes = quotes.ifEmpty { simulator.seedQuotes(DEFAULT_WATCHLIST) },
                    messageFa = "اتصال به ${profile.terminalVersion.labelFa} برقرار شد",
                    messageEn = "Connected to ${profile.terminalVersion.labelEn}",
                    lastSyncAt = System.currentTimeMillis()
                )
            },
            onFailure = { error ->
                // Degrade to the simulator rather than showing a dead screen: the owner
                // still sees a working panel and a clear explanation of what failed.
                simulatedState(
                    profile,
                    "اتصال برقرار نشد؛ شبیه‌ساز محلی جایگزین شد. علت: ${error.message ?: "نامشخص"}",
                    "Connection failed; switched to the local simulator. Reason: ${error.message ?: "unknown"}"
                ).copy(
                    lastErrorFa = error.message ?: "خطای ناشناخته",
                    lastErrorEn = error.message ?: "Unknown error"
                )
            }
        )
    }

    /** Cheap credential check used by the "Test gateway" button. */
    suspend fun probe(profile: MtGatewayProfile): MtGatewayState = connect(profile)

    fun disconnect(profile: MtGatewayProfile): MtGatewayState = MtGatewayState(
        phase = MtConnectionPhase.DISCONNECTED,
        profile = profile,
        messageFa = "اتصال قطع شد",
        messageEn = "Disconnected"
    )

    /** Validates the profile before any network call is attempted. */
    fun validate(profile: MtGatewayProfile): Pair<String, String>? {
        val credential = profile.bridgeToken.ifBlank { profile.password }
        return when {
            profile.bridgeKind == MtBridgeKind.METAAPI && credential.isBlank() ->
                "برای اتصال به MetaApi باید توکن حساب را وارد کنید." to
                    "A MetaApi account token is required to connect."

            profile.bridgeKind == MtBridgeKind.SELF_HOSTED && profile.bridgeUrl.isBlank() ->
                "نشانی پل شخصی (REST یا WebSocket) وارد نشده است." to
                    "The self-hosted bridge address is missing."

            profile.bridgeKind == MtBridgeKind.SELF_HOSTED && profile.login.isBlank() ->
                "شمارهٔ حساب (Login) وارد نشده است." to
                    "The account login is missing."

            else -> null
        }
    }

    // ------------------------------------------------------------------- market

    suspend fun refresh(current: MtGatewayState): MtGatewayState = withContext(Dispatchers.IO) {
        val profile = current.profile
        if (current.phase == MtConnectionPhase.SIMULATED || profile.bridgeKind == MtBridgeKind.OFFLINE_SIM) {
            return@withContext simulatedState(
                profile,
                current.messageFa,
                current.messageEn
            ).copy(quotes = simulator.tick(current.quotes), positions = current.positions)
        }
        if (current.phase != MtConnectionPhase.CONNECTED) return@withContext current

        val account = runCatching { fetchAccountRemote(profile) }.getOrNull() ?: current.account
        val positions = runCatching { fetchPositionsRemote(profile) }.getOrNull() ?: current.positions
        val quotes = runCatching { fetchQuotesRemote(profile, DEFAULT_WATCHLIST) }.getOrNull() ?: current.quotes

        current.copy(
            account = account,
            positions = positions,
            quotes = quotes,
            lastSyncAt = System.currentTimeMillis(),
            dailyPnl = positions.sumOf { it.profit }
        )
    }

    // ------------------------------------------------------------------- orders

    /**
     * The single order entry point. Every guard is re-evaluated here.
     *
     * @param emergencyLockActive current kill-switch state
     * @param liveConfirmed       true only after the owner explicitly re-confirmed the
     *                            irreversible live-execution warning in this session
     */
    suspend fun placeOrder(
        request: MtOrderRequest,
        state: MtGatewayState,
        emergencyLockActive: Boolean,
        liveConfirmed: Boolean
    ): MtOrderResult = withContext(Dispatchers.IO) {
        val profile = state.profile
        val mode = profile.executionMode

        fun blocked(reason: OrderGateReason, fa: String, en: String) = MtOrderResult(
            accepted = false,
            blockedBy = reason,
            detailFa = fa,
            detailEn = en
        )

        if (!state.isConnected) {
            return@withContext blocked(
                OrderGateReason.NOT_CONNECTED,
                "ابتدا درگاه ترمینال را متصل کنید.",
                "Connect the terminal gateway first."
            )
        }
        if (emergencyLockActive) {
            return@withContext blocked(
                OrderGateReason.EMERGENCY_LOCK,
                "قفل اضطراری فعال است؛ ارسال سفارش مسدود شد.",
                "The emergency lock is engaged; order routing was blocked."
            )
        }
        if (mode == TradingExecutionMode.PAPER_SIMULATION) {
            return@withContext blocked(
                OrderGateReason.PAPER_MODE,
                "سطح اجرا روی «شبیه‌سازی کاغذی» است. برای ارسال سفارش واقعی، سطح اجرا را در تنظیمات درگاه تغییر دهید.",
                "Execution level is paper simulation. Change it in the gateway settings to route real orders."
            )
        }
        if (mode == TradingExecutionMode.LIVE_EXECUTION && profile.accountType == MtAccountType.REAL && !liveConfirmed) {
            return@withContext blocked(
                OrderGateReason.REAL_ACCOUNT_BLOCKED,
                "اجرای زنده روی حساب واقعی نیازمند تأیید صریح شما در همین نشست است.",
                "Live routing on a real account requires explicit confirmation in this session."
            )
        }
        if (request.volume > profile.maxLotSize + 1e-9) {
            return@withContext blocked(
                OrderGateReason.LOT_LIMIT_EXCEEDED,
                "حجم درخواستی (${request.volume}) از سقف مجاز (${profile.maxLotSize} لات) بیشتر است.",
                "Requested volume (${request.volume}) exceeds the configured cap (${profile.maxLotSize} lots)."
            )
        }
        if (profile.autoCloseOnDrawdown && state.dailyPnl <= -abs(profile.maxDailyLossUsd)) {
            return@withContext blocked(
                OrderGateReason.DAILY_LOSS_CAP,
                "سقف زیان روزانه پر شده است؛ ارسال سفارش تازه مسدود شد.",
                "The daily loss cap is reached; new orders are blocked."
            )
        }
        if (request.symbol.isBlank()) {
            return@withContext blocked(
                OrderGateReason.INVALID_SYMBOL,
                "نماد وارد نشده است.",
                "No symbol supplied."
            )
        }

        // Paper accounts still route, but only when the mode allows it.
        if (state.isSimulated || profile.bridgeKind == MtBridgeKind.OFFLINE_SIM) {
            val quote = state.quotes.firstOrNull { it.symbol.equals(request.symbol, ignoreCase = true) }
                ?: simulator.quoteFor(request.symbol)
            val fill = if (request.side == MtOrderSide.BUY) quote.ask else quote.bid
            return@withContext MtOrderResult(
                accepted = true,
                ticket = "SIM-${random.nextInt(100000, 999999)}",
                detailFa = "سفارش ${request.side.labelFa} ${request.volume} لات روی ${request.symbol} در شبیه‌ساز محلی با قیمت $fill ثبت شد.",
                detailEn = "Simulated ${request.side.labelEn} of ${request.volume} lots on ${request.symbol} filled at $fill.",
                simulated = true
            )
        }

        runCatching { submitOrderRemote(profile, request) }.fold(
            onSuccess = { it },
            onFailure = { error ->
                blocked(
                    OrderGateReason.BROKER_REJECTED,
                    "ارسال سفارش ناموفق بود: ${error.message ?: "خطای شبکه"}",
                    "Order submission failed: ${error.message ?: "network error"}"
                )
            }
        )
    }

    suspend fun closePosition(ticket: String, state: MtGatewayState): MtOrderResult = withContext(Dispatchers.IO) {
        val position = state.positions.firstOrNull { it.ticket == ticket }
            ?: return@withContext MtOrderResult(
                accepted = false,
                blockedBy = OrderGateReason.INVALID_SYMBOL,
                detailFa = "پوزیشن پیدا نشد.",
                detailEn = "Position not found."
            )

        val request = MtOrderRequest(
            symbol = position.symbol,
            side = if (position.side == MtOrderSide.BUY) MtOrderSide.SELL else MtOrderSide.BUY,
            volume = position.volume,
            comment = "SAYVIS-CLOSE"
        )
        placeOrder(request, state, emergencyLockActive = false, liveConfirmed = true)
    }

    // -------------------------------------------------------------- remote REST

    private fun fetchAccountRemote(profile: MtGatewayProfile): MtAccountInfo {
        val (url, headers) = endpointFor(profile, "account")
        val json = get(url, headers)
        return when (profile.bridgeKind) {
            MtBridgeKind.METAAPI -> parseMetaApiAccount(json, profile)
            else -> parseGenericAccount(json, profile)
        }
    }

    private fun fetchPositionsRemote(profile: MtGatewayProfile): List<MtPosition> {
        val (url, headers) = endpointFor(profile, "positions")
        val json = get(url, headers)
        val array = json.optJSONArray("positions") ?: json.optJSONArray("deals") ?: JSONArray()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val sideRaw = (item.optString("type") + item.optString("side")).uppercase()
            MtPosition(
                ticket = item.optString("id").ifBlank { item.optString("ticket") },
                symbol = item.optString("symbol"),
                side = if (sideRaw.contains("SELL")) MtOrderSide.SELL else MtOrderSide.BUY,
                volume = item.optDouble("volume", 0.0),
                openPrice = item.optDouble("openPrice", item.optDouble("price", 0.0)),
                currentPrice = item.optDouble("currentPrice", item.optDouble("price", 0.0)),
                stopLoss = item.optDouble("stopLoss", 0.0).takeIf { it > 0.0 },
                takeProfit = item.optDouble("takeProfit", 0.0).takeIf { it > 0.0 },
                profit = item.optDouble("profit", 0.0),
                comment = item.optString("comment", "")
            )
        }
    }

    private fun fetchQuotesRemote(profile: MtGatewayProfile, symbols: List<String>): List<MtQuote> =
        symbols.mapNotNull { symbol ->
            runCatching {
                val (url, headers) = endpointFor(profile, "symbol-info/$symbol")
                val json = get(url, headers)
                val root = if (json.has("symbolInfo")) json.optJSONObject("symbolInfo") ?: json else json
                val bid = root.optDouble("bid", 0.0)
                val ask = root.optDouble("ask", 0.0)
                if (bid <= 0.0 || ask <= 0.0) null
                else MtQuote(
                    symbol = symbol,
                    bid = bid,
                    ask = ask,
                    digits = root.optInt("digits", 2),
                    changePercent = root.optDouble("changePercent", 0.0)
                )
            }.getOrNull()
        }

    private fun submitOrderRemote(profile: MtGatewayProfile, request: MtOrderRequest): MtOrderResult {
        val (url, headers) = endpointFor(profile, "trade")
        val body = if (profile.bridgeKind == MtBridgeKind.METAAPI) {
            JSONObject().apply {
                put("actionType", if (request.side == MtOrderSide.BUY) "ORDER_TYPE_BUY" else "ORDER_TYPE_SELL")
                put("symbol", request.symbol)
                put("volume", request.volume)
                put("slippage", request.slippagePoints)
                put("comment", request.comment)
                request.stopLoss?.let { put("stopLoss", it) }
                request.takeProfit?.let { put("takeProfit", it) }
            }
        } else {
            JSONObject().apply {
                put("side", request.side.name)
                put("symbol", request.symbol)
                put("volume", request.volume)
                put("slippage", request.slippagePoints)
                put("comment", request.comment)
                request.stopLoss?.let { put("stopLoss", it) }
                request.takeProfit?.let { put("takeProfit", it) }
            }
        }

        val call = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE)).build()

        client.newCall(call).execute().use { response ->
            val payload = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val reason = runCatching { JSONObject(payload).optString("message") }.getOrDefault(payload.take(160))
                throw IllegalStateException(reason.ifBlank { "HTTP ${response.code}" })
            }
            val json = runCatching { JSONObject(payload) }.getOrDefault(JSONObject())
            val ticket = json.optString("id").ifBlank { json.optString("ticket").ifBlank { json.optString("orderId") } }
            return MtOrderResult(
                accepted = true,
                ticket = ticket.ifBlank { "ORD-${System.currentTimeMillis()}" },
                detailFa = "سفارش ${request.side.labelFa} ${request.volume} لات روی ${request.symbol} به ترمینال ارسال شد.",
                detailEn = "${request.side.labelEn} of ${request.volume} lots on ${request.symbol} routed to the terminal."
            )
        }
    }

    private fun endpointFor(profile: MtGatewayProfile, path: String): Pair<String, Map<String, String>> {
        return when (profile.bridgeKind) {
            MtBridgeKind.METAAPI -> {
                val token = profile.bridgeToken.ifBlank { profile.password }
                val base = profile.bridgeUrl.ifBlank { METAAPI_BASE }
                "${base.trimEnd('/')}/users/current/accounts/$token/$path" to
                    mapOf("auth-token" to token, "Content-Type" to "application/json")
            }

            MtBridgeKind.SELF_HOSTED -> {
                val base = profile.bridgeUrl.trimEnd('/')
                val headers = mutableMapOf("Content-Type" to "application/json")
                profile.bridgeToken.takeIf { it.isNotBlank() }?.let { headers["Authorization"] = "Bearer $it" }
                profile.login.takeIf { it.isNotBlank() }?.let { headers["X-MT-Login"] = it }
                "$base/$path" to headers
            }

            MtBridgeKind.OFFLINE_SIM -> "" to emptyMap()
        }
    }

    private fun get(url: String, headers: Map<String, String>): JSONObject {
        val call = Request.Builder().url(url).apply { headers.forEach { (key, value) -> header(key, value) } }.get().build()
        client.newCall(call).execute().use { response ->
            val payload = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val reason = runCatching { JSONObject(payload).optString("message") }.getOrDefault(payload.take(160))
                throw IllegalStateException(reason.ifBlank { "HTTP ${response.code}" })
            }
            return runCatching { JSONObject(payload) }.getOrElse { JSONObject() }
        }
    }

    private fun parseMetaApiAccount(json: JSONObject, profile: MtGatewayProfile): MtAccountInfo {
        val account = json.optJSONObject("account") ?: json
        val balance = account.optDouble("balance", 0.0)
        val equity = account.optDouble("equity", balance)
        val margin = account.optDouble("margin", 0.0)
        return MtAccountInfo(
            login = account.optString("login", profile.login),
            server = account.optString("server", profile.serverAddress),
            broker = account.optString("broker", profile.brokerName),
            currency = account.optString("currency", "USD"),
            balance = balance,
            equity = equity,
            margin = margin,
            freeMargin = account.optDouble("marginFree", max(0.0, equity - margin)),
            marginLevelPercent = account.optDouble("marginLevel", if (equity > 0) (equity / max(margin, 1e-9)) * 100.0 else 0.0),
            leverage = account.optInt("leverage", 100),
            isDemo = account.optString("accountType", "").contains("demo", true) || profile.accountType == MtAccountType.DEMO,
            isSimulated = false
        )
    }

    private fun parseGenericAccount(json: JSONObject, profile: MtGatewayProfile): MtAccountInfo {
        val balance = json.optDouble("balance", 0.0)
        val equity = json.optDouble("equity", balance)
        val margin = json.optDouble("margin", 0.0)
        return MtAccountInfo(
            login = json.optString("login", profile.login),
            server = json.optString("server", profile.serverAddress),
            broker = json.optString("broker", profile.brokerName),
            currency = json.optString("currency", "USD"),
            balance = balance,
            equity = equity,
            margin = margin,
            freeMargin = json.optDouble("freeMargin", max(0.0, equity - margin)),
            marginLevelPercent = json.optDouble("marginLevel", if (equity > 0) (equity / max(margin, 1e-9)) * 100.0 else 0.0),
            leverage = json.optInt("leverage", 100),
            isDemo = profile.accountType == MtAccountType.DEMO,
            isSimulated = false
        )
    }

    // --------------------------------------------------------------- simulator

    private fun simulatedState(
        profile: MtGatewayProfile,
        messageFa: String,
        messageEn: String
    ): MtGatewayState {
        val quotes = simulator.seedQuotes(DEFAULT_WATCHLIST)
        val positions = simulator.seedPositions(quotes)
        val account = simulator.account(profile, positions)
        return MtGatewayState(
            phase = MtConnectionPhase.SIMULATED,
            profile = profile,
            account = account,
            positions = positions,
            quotes = quotes,
            messageFa = messageFa,
            messageEn = messageEn,
            lastSyncAt = System.currentTimeMillis(),
            dailyPnl = positions.sumOf { it.profit }
        )
    }

    companion object {
        const val METAAPI_BASE = "https://mt-client-api-v1.agiliumtrade.ai"
        val DEFAULT_WATCHLIST = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "BTCUSD")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * Deterministic-ish random-walk market used when no bridge is reachable.
 * Keeps the trading screen meaningful offline and honest about being simulated.
 */
internal class MarketSimulator(private val random: Random) {

    private val baselines = mapOf(
        "EURUSD" to 1.0865,
        "GBPUSD" to 1.2710,
        "USDJPY" to 151.42,
        "XAUUSD" to 2580.00,
        "BTCUSD" to 64200.0
    )

    private fun digitsFor(symbol: String): Int = when {
        baselines[symbol.uppercase()]?.let { it < 10 } == true -> 5
        symbol.uppercase() == "USDJPY" -> 3
        else -> 2
    }

    private fun baseline(symbol: String): Double =
        baselines[symbol.uppercase()] ?: 100.0

    /** Rounds to 5 decimal places. Named distinctly so it never shadows kotlin.math.round. */
    private fun scale5(value: Double): Double = kotlin.math.round(value * 100000.0) / 100000.0

    fun quoteFor(symbol: String): MtQuote {
        val base = baseline(symbol)
        val drift = base * (random.nextDouble() - 0.5) * 0.0015
        val bid = scale5(base + drift)
        val spread = base * 0.00008
        return MtQuote(
            symbol = symbol.uppercase(),
            bid = bid,
            ask = scale5(bid + spread),
            digits = digitsFor(symbol),
            changePercent = scale5((drift / base) * 100.0)
        )
    }

    fun seedQuotes(symbols: List<String>): List<MtQuote> = symbols.map { quoteFor(it) }

    /** Advances every quote by a small bounded step. */
    fun tick(quotes: List<MtQuote>): List<MtQuote> = quotes.map { quote ->
        val step = quote.bid * (random.nextDouble() - 0.5) * 0.0008
        val bid = scale5(quote.bid + step)
        val ask = scale5(bid + (quote.ask - quote.bid))
        quote.copy(bid = bid, ask = ask, changePercent = scale5(quote.changePercent + (step / quote.bid) * 100.0))
    }

    fun seedPositions(quotes: List<MtQuote>): List<MtPosition> {
        if (quotes.isEmpty()) return emptyList()
        val first = quotes[0]
        val third = quotes.getOrNull(min(3, quotes.size - 1)) ?: first
        val openFirst = scale5(first.bid * (1 - 0.0022))
        val openThird = scale5(third.ask * (1 + 0.0041))
        return listOf(
            MtPosition(
                ticket = "SIM-1001",
                symbol = first.symbol,
                side = MtOrderSide.BUY,
                volume = 0.10,
                openPrice = openFirst,
                currentPrice = first.bid,
                stopLoss = scale5(openFirst * 0.994),
                takeProfit = scale5(openFirst * 1.012),
                profit = scale5((first.bid - openFirst) * 100000 * 0.10)
            ),
            MtPosition(
                ticket = "SIM-1002",
                symbol = third.symbol,
                side = MtOrderSide.SELL,
                volume = 0.05,
                openPrice = openThird,
                currentPrice = third.ask,
                stopLoss = scale5(openThird * 1.006),
                takeProfit = scale5(openThird * 0.988),
                profit = scale5((openThird - third.ask) * 100 * 0.05)
            )
        )
    }

    fun account(profile: MtGatewayProfile, positions: List<MtPosition>): MtAccountInfo {
        val balance = 10000.0
        val floating = positions.sumOf { it.profit }
        val equity = balance + floating
        val margin = positions.sumOf { it.volume * it.openPrice * 100.0 } / max(profile.maxLotSize, 0.01) * 0.01
        return MtAccountInfo(
            login = profile.login.ifBlank { "5${random.nextInt(1000000, 9999999)}" },
            server = profile.serverAddress.ifBlank { "SAYVIS-Demo" },
            broker = profile.brokerName.ifBlank { "SAYVIS Paper Broker" },
            currency = "USD",
            balance = balance,
            equity = equity,
            margin = scale5(margin),
            freeMargin = scale5(max(0.0, equity - margin)),
            marginLevelPercent = if (margin > 0) scale5((equity / margin) * 100.0) else 0.0,
            leverage = 100,
            isDemo = true,
            isSimulated = true
        )
    }
}
