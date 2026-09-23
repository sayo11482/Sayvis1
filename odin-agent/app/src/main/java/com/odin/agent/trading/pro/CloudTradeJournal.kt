package com.odin.agent.trading.pro

import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.JalaliCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN PRO v1.0.28 - Cloud Trade Journal & Export Engine
 * ژورنال معاملات ابری، تحلیل روانشناختی تریدر و خروجی اکسل/CSV
 */

data class JournalEntry(
    val tradeId: String,
    val symbol: String,
    val side: SignalSide,
    val entryPrice: Double,
    val exitPrice: Double,
    val netPnlUsd: Double,
    val returnR: Double,
    val strategy: QuantStrategyType,
    val emotionalState: String, // CALM, GREEDY, DISCIPLINED, FOMO
    val tags: List<String>,
    val jalaliDate: String,
    val gregorianDate: String
)

class CloudTradeJournal {

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries

    fun logTrade(
        tradeId: String,
        symbol: String,
        side: SignalSide,
        entryPrice: Double,
        exitPrice: Double,
        netPnlUsd: Double,
        returnR: Double,
        strategy: QuantStrategyType,
        emotion: String = "DISCIPLINED",
        tags: List<String> = listOf("PRO_ENGINE", "AUTO_EXEC")
    ): JournalEntry {
        val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
        val entry = JournalEntry(
            tradeId = tradeId,
            symbol = symbol,
            side = side,
            entryPrice = entryPrice,
            exitPrice = exitPrice,
            netPnlUsd = netPnlUsd,
            returnR = returnR,
            strategy = strategy,
            emotionalState = emotion,
            tags = tags,
            jalaliDate = j,
            gregorianDate = g
        )
        _entries.value = listOf(entry) + _entries.value
        return entry
    }

    /**
     * صدور خروجی استاندارد CSV جهت ایمپورت در اکسل یا ژورنال‌های معاملاتی
     */
    fun exportToCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("TradeId,Symbol,Side,Entry,Exit,NetPnlUSD,ReturnR,Strategy,Emotion,JalaliDate,GregorianDate")
        for (e in _entries.value) {
            sb.appendLine("${e.tradeId},${e.symbol},${e.side},${e.entryPrice},${e.exitPrice},${e.netPnlUsd},${e.returnR},${e.strategy},${e.emotionalState},\"${e.jalaliDate}\",\"${e.gregorianDate}\"")
        }
        return sb.toString()
    }
}
