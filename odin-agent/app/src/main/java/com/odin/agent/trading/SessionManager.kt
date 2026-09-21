package com.odin.agent.trading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * ODIN - Trading Session Manager - NY & London
 * تاریخ ساعت و شروع و پایان سشن‌های معاملاتی نیویورک و لندن
 */

data class TradingSession(
    val name: String,
    val nameFa: String,
    val startHourUTC: Int,
    val startMinuteUTC: Int,
    val endHourUTC: Int,
    val endMinuteUTC: Int,
    val isActive: Boolean = false,
    val timeUntilStart: String = "",
    val timeUntilEnd: String = "",
    val overlap: Boolean = false
)

data class SessionState(
    val currentTime: String = "",
    val currentDate: String = "",
    val currentTimeUTC: String = "",
    val tehranTime: String = "",
    val sessions: List<TradingSession> = emptyList(),
    val activeSession: String = "",
    val isLondonActive: Boolean = false,
    val isNewYorkActive: Boolean = false,
    val isOverlap: Boolean = false,
    val nextSession: String = "",
    val sessionProgress: Float = 0f
)

class SessionManager {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    private val tehranTZ = TimeZone.getTimeZone("Asia/Tehran")
    private val utcTZ = TimeZone.getTimeZone("UTC")
    private val nyTZ = TimeZone.getTimeZone("America/New_York")
    private val londonTZ = TimeZone.getTimeZone("Europe/London")

    fun updateSessions() {
        val now = Calendar.getInstance()
        val utcNow = Calendar.getInstance(utcTZ)
        val tehranNow = Calendar.getInstance(tehranTZ)
        val nyNow = Calendar.getInstance(nyTZ)
        val londonNow = Calendar.getInstance(londonTZ)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        val timeFormatShort = SimpleDateFormat("HH:mm", Locale.US)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        dateFormat.timeZone = tehranTZ
        timeFormat.timeZone = tehranTZ
        dateTimeFormat.timeZone = tehranTZ

        val tehranTimeStr = timeFormat.format(tehranNow.time)
        val tehranDateStr = dateFormat.format(tehranNow.time)
        val utcTimeStr = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = utcTZ }.format(utcNow.time)
        val currentDateTimeStr = dateTimeFormat.format(now.time)

        // London session: 08:00 - 16:30 UTC (08:00-16:30 GMT)
        // NY session: 13:00 - 22:00 UTC (08:00-17:00 ET)
        // Overlap: 13:00 - 16:30 UTC

        val londonStart = 8
        val londonEnd = 16
        val londonEndMin = 30
        val nyStart = 13
        val nyEnd = 22

        val utcHour = utcNow.get(Calendar.HOUR_OF_DAY)
        val utcMinute = utcNow.get(Calendar.MINUTE)

        val isLondonActive = isTimeInSession(utcHour, utcMinute, londonStart, 0, londonEnd, londonEndMin)
        val isNYActive = isTimeInSession(utcHour, utcMinute, nyStart, 0, nyEnd, 0)
        val isOverlap = isLondonActive && isNYActive

        val londonProgress = if (isLondonActive) calculateProgress(utcHour, utcMinute, londonStart, 0, londonEnd, londonEndMin) else 0f
        val nyProgress = if (isNYActive) calculateProgress(utcHour, utcMinute, nyStart, 0, nyEnd, 0) else 0f
        val activeProgress = when {
            isOverlap -> (londonProgress + nyProgress) / 2
            isLondonActive -> londonProgress
            isNYActive -> nyProgress
            else -> 0f
        }

        val sessions = listOf(
            TradingSession(
                name = "London",
                nameFa = "لندن",
                startHourUTC = londonStart,
                startMinuteUTC = 0,
                endHourUTC = londonEnd,
                endMinuteUTC = londonEndMin,
                isActive = isLondonActive,
                timeUntilStart = if (!isLondonActive) timeUntil(utcHour, utcMinute, londonStart, 0) else "",
                timeUntilEnd = if (isLondonActive) timeUntil(utcHour, utcMinute, londonEnd, londonEndMin) else "",
                overlap = false
            ),
            TradingSession(
                name = "New York",
                nameFa = "نیویورک",
                startHourUTC = nyStart,
                startMinuteUTC = 0,
                endHourUTC = nyEnd,
                endMinuteUTC = 0,
                isActive = isNYActive,
                timeUntilStart = if (!isNYActive) timeUntil(utcHour, utcMinute, nyStart, 0) else "",
                timeUntilEnd = if (isNYActive) timeUntil(utcHour, utcMinute, nyEnd, 0) else "",
                overlap = false
            ),
            TradingSession(
                name = "Overlap",
                nameFa = "همپوشانی",
                startHourUTC = nyStart,
                startMinuteUTC = 0,
                endHourUTC = londonEnd,
                endMinuteUTC = londonEndMin,
                isActive = isOverlap,
                timeUntilStart = "",
                timeUntilEnd = if (isOverlap) timeUntil(utcHour, utcMinute, londonEnd, londonEndMin) else "",
                overlap = true
            )
        )

        val activeSession = when {
            isOverlap -> "London + New York Overlap"
            isLondonActive -> "London"
            isNYActive -> "New York"
            else -> "No Active Session - Asian"
        }

        val nextSession = when {
            !isLondonActive && utcHour < londonStart -> "London in ${timeUntil(utcHour, utcMinute, londonStart, 0)}"
            !isNYActive && utcHour < nyStart -> "New York in ${timeUntil(utcHour, utcMinute, nyStart, 0)}"
            !isLondonActive && !isNYActive -> "London in ${timeUntil(utcHour, utcMinute, londonStart + 24, 0)}"
            else -> "Active"
        }

        _state.value = SessionState(
            currentTime = tehranTimeStr,
            currentDate = tehranDateStr,
            currentTimeUTC = utcTimeStr,
            tehranTime = "$tehranDateStr $tehranTimeStr (Tehran) | $utcTimeStr UTC",
            sessions = sessions,
            activeSession = activeSession,
            isLondonActive = isLondonActive,
            isNewYorkActive = isNYActive,
            isOverlap = isOverlap,
            nextSession = nextSession,
            sessionProgress = activeProgress
        )
    }

    private fun isTimeInSession(currentHour: Int, currentMinute: Int, startHour: Int, startMin: Int, endHour: Int, endMin: Int): Boolean {
        val currentTotal = currentHour * 60 + currentMinute
        val startTotal = startHour * 60 + startMin
        val endTotal = endHour * 60 + endMin
        return currentTotal in startTotal until endTotal
    }

    private fun calculateProgress(currentHour: Int, currentMinute: Int, startHour: Int, startMin: Int, endHour: Int, endMin: Int): Float {
        val currentTotal = currentHour * 60 + currentMinute
        val startTotal = startHour * 60 + startMin
        val endTotal = endHour * 60 + endMin
        val totalDuration = endTotal - startTotal
        val elapsed = currentTotal - startTotal
        return (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
    }

    private fun timeUntil(currentHour: Int, currentMinute: Int, targetHour: Int, targetMin: Int): String {
        var currentTotal = currentHour * 60 + currentMinute
        var targetTotal = targetHour * 60 + targetMin
        if (targetTotal <= currentTotal) targetTotal += 24 * 60
        val diff = targetTotal - currentTotal
        val hours = diff / 60
        val minutes = diff % 60
        return "${hours}h ${minutes}m"
    }
}
