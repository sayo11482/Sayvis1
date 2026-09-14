package com.example.sayvis.trading

import com.example.sayvis.settings.MtGatewayProfile

/** Transport-level connection lifecycle. */
enum class MtConnectionPhase {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    SIMULATED,
    ERROR
}

/** Side of an order or an open position. */
enum class MtOrderSide(val labelFa: String, val labelEn: String) {
    BUY("خرید", "Buy"),
    SELL("فروش", "Sell");

    fun label(isPersian: Boolean) = if (isPersian) labelFa else labelEn
}

/** Why an order was refused before it ever reached the broker. */
enum class OrderGateReason(val labelFa: String, val labelEn: String) {
    NOT_CONNECTED("درگاه متصل نیست", "Gateway is not connected"),
    EMERGENCY_LOCK("قفل اضطراری فعال است", "Emergency lock is engaged"),
    PAPER_MODE("سطح اجرا روی شبیه‌سازی کاغذی است", "Execution level is paper simulation"),
    REAL_ACCOUNT_BLOCKED("اجرای زنده روی حساب واقعی تأیید نشده است", "Live routing on a real account is not confirmed"),
    LOT_LIMIT_EXCEEDED("حجم سفارش از سقف مجاز بیشتر است", "Order volume exceeds the configured lot cap"),
    DAILY_LOSS_CAP("سقف زیان روزانه پر شده است", "Daily loss cap reached"),
    INVALID_SYMBOL("نماد نامعتبر است", "Invalid symbol"),
    BROKER_REJECTED("بروکر سفارش را رد کرد", "Broker rejected the order");

    fun label(isPersian: Boolean) = if (isPersian) labelFa else labelEn
}

/** Live snapshot of the connected trading account. */
data class MtAccountInfo(
    val login: String = "",
    val server: String = "",
    val broker: String = "",
    val currency: String = "USD",
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val margin: Double = 0.0,
    val freeMargin: Double = 0.0,
    val marginLevelPercent: Double = 0.0,
    val leverage: Int = 100,
    val isDemo: Boolean = true,
    val isSimulated: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val usedMarginPercent: Double
        get() = if (equity <= 0.0) 0.0 else (margin / equity) * 100.0

    val floatingPnl: Double get() = equity - balance
}

/** One instrument in the market watch list. */
data class MtQuote(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val digits: Int = 2,
    val changePercent: Double = 0.0,
    val time: Long = System.currentTimeMillis()
) {
    val spread: Double get() = ask - bid
}

/** An open position or a pending order. */
data class MtPosition(
    val ticket: String,
    val symbol: String,
    val side: MtOrderSide,
    val volume: Double,
    val openPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val profit: Double,
    val comment: String = "",
    val openedAt: Long = System.currentTimeMillis()
) {
    val isProfitable: Boolean get() = profit >= 0.0
}

/** An order request submitted by the owner. */
data class MtOrderRequest(
    val symbol: String,
    val side: MtOrderSide,
    val volume: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val comment: String = "SAYVIS",
    val slippagePoints: Int = 20
)

/** Result of an order attempt. */
data class MtOrderResult(
    val accepted: Boolean,
    val ticket: String? = null,
    val blockedBy: OrderGateReason? = null,
    val detailFa: String = "",
    val detailEn: String = "",
    val simulated: Boolean = false
) {
    fun detail(isPersian: Boolean) = if (isPersian) detailFa else detailEn
}

/** Everything the UI needs to render the gateway panel in one state object. */
data class MtGatewayState(
    val phase: MtConnectionPhase = MtConnectionPhase.DISCONNECTED,
    val profile: MtGatewayProfile = MtGatewayProfile(),
    val account: MtAccountInfo? = null,
    val positions: List<MtPosition> = emptyList(),
    val quotes: List<MtQuote> = emptyList(),
    val messageFa: String = "",
    val messageEn: String = "",
    val lastErrorFa: String = "",
    val lastErrorEn: String = "",
    val lastSyncAt: Long = 0L,
    val orderHistory: List<MtOrderResult> = emptyList(),
    val dailyPnl: Double = 0.0,
    val isBusy: Boolean = false
) {
    val isConnected: Boolean
        get() = phase == MtConnectionPhase.CONNECTED || phase == MtConnectionPhase.SIMULATED

    val isSimulated: Boolean get() = phase == MtConnectionPhase.SIMULATED

    fun message(isPersian: Boolean) = if (isPersian) messageFa else messageEn
    fun lastError(isPersian: Boolean) = if (isPersian) lastErrorFa else lastErrorEn

    fun phaseLabel(isPersian: Boolean): String = when (phase) {
        MtConnectionPhase.DISCONNECTED -> if (isPersian) "قطع" else "Disconnected"
        MtConnectionPhase.CONNECTING -> if (isPersian) "در حال اتصال" else "Connecting"
        MtConnectionPhase.AUTHENTICATING -> if (isPersian) "در حال احراز هویت" else "Authenticating"
        MtConnectionPhase.CONNECTED -> if (isPersian) "متصل" else "Connected"
        MtConnectionPhase.SIMULATED -> if (isPersian) "شبیه‌سازی محلی" else "Local simulation"
        MtConnectionPhase.ERROR -> if (isPersian) "خطا" else "Error"
    }
}
