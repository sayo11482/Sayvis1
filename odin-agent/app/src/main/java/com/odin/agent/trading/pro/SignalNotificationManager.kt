package com.odin.agent.trading.pro

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN PRO v1.0.28 - Signal Push & Sound Alert Notification Engine
 * اعلان زنده سیگنال‌های با کانفلوئنس بالای ۸۰٪ با هشدار صوتی و لرزش
 */

data class SignalAlert(
    val id: String,
    val symbol: String,
    val side: SignalSide,
    val price: Double,
    val confidence: Double,
    val strategy: QuantStrategyType,
    val titleFa: String,
    val bodyFa: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

class SignalNotificationManager {

    private val _notifications = MutableStateFlow<List<SignalAlert>>(emptyList())
    val notifications: StateFlow<List<SignalAlert>> = _notifications

    fun dispatchSignalAlert(
        symbol: String,
        side: SignalSide,
        price: Double,
        confidence: Double,
        strategy: QuantStrategyType,
        sl: Double,
        tp: Double
    ): SignalAlert? {
        // فقط سیگنال‌های با کانفلوئنس بالای ۷۵٪ اعلان داده می‌شوند
        if (confidence < 75.0) return null

        val dirFa = if (side == SignalSide.BUY) "خرید (BUY)" else "فروش (SELL)"
        val title = "🎯 سیگنال طلایی Odin.trade ($confidence٪)"
        val body = "فرصت $dirFa نماد $symbol در قیمت $price | استراتژی: $strategy | حد ضرر: $sl | حد سود: $tp"

        val alert = SignalAlert(
            id = "ALERT-${System.currentTimeMillis()}",
            symbol = symbol,
            side = side,
            price = price,
            confidence = confidence,
            strategy = strategy,
            titleFa = title,
            bodyFa = body
        )

        _notifications.value = (listOf(alert) + _notifications.value).take(50)
        return alert
    }
}
