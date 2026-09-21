package com.example.sayvis.i18n

import com.example.sayvis.model.ContextSnapshot

/**
 * Renders a language-neutral [ContextSnapshot] into fully localised Persian or English
 * prose. Every label here is hand-written in both languages — none of it is machine
 * translated, because these strings appear on the home screen on every launch.
 */
object ContextLocalization {

    fun focusWindow(snapshot: ContextSnapshot, fa: Boolean, persianDigits: Boolean): String {
        val range = PersianFormat.timeRange("${snapshot.focusWindowStart} - ${snapshot.focusWindowEnd}", persianDigits)
        if (!snapshot.focusWindowActive) {
            return if (fa) "خارج از پنجرهٔ تمرکز" else "Outside focus window"
        }
        return if (fa) "پنجرهٔ کار عمیق ($range)" else "Deep work window ($range)"
    }

    fun activity(snapshot: ContextSnapshot, fa: Boolean): String =
        if (fa) snapshot.currentActivity.labelFa else snapshot.currentActivity.labelEn

    fun cognitiveLoad(snapshot: ContextSnapshot, fa: Boolean): String =
        if (fa) snapshot.cognitiveLoad.labelFa else snapshot.cognitiveLoad.labelEn

    fun network(snapshot: ContextSnapshot, fa: Boolean): String = when {
        !snapshot.isOnline && fa -> "همیشه متصل — هستهٔ حاکم فعال (حالت امن محلی)"
        !snapshot.isOnline -> "Always online — Sovereign Core active (sovereign local safe mode)"
        snapshot.gatewaySecure && fa -> "متصل — دروازهٔ امن سایویس"
        snapshot.gatewaySecure -> "Online — SAYVIS secure gateway"
        fa -> "متصل — بدون دروازهٔ امن"
        else -> "Online — no secure gateway"
    }

    fun emergencyLock(snapshot: ContextSnapshot, fa: Boolean): String = when {
        snapshot.emergencyLockActive && fa -> "قفل اضطراری فعال"
        snapshot.emergencyLockActive -> "Emergency lock engaged"
        fa -> "امنیت عادی"
        else -> "Normal security"
    }

    fun battery(snapshot: ContextSnapshot, fa: Boolean, persianDigits: Boolean): String {
        val value = PersianFormat.digits("${snapshot.batteryPercent}", persianDigits)
        val charging = if (fa) "در حال شارژ" else "charging"
        return if (snapshot.isCharging) "$value٪ ($charging)" else "$value٪"
    }

    /** Compact one-line summary fed to the AI provider as system context. */
    fun systemContextLine(snapshot: ContextSnapshot, fa: Boolean): String = if (fa) {
        "تمرکز: ${focusWindow(snapshot, true, true)}، فعالیت: ${activity(snapshot, true)}، " +
            "بار شناختی: ${cognitiveLoad(snapshot, true)}، وظایف مسدود: ${snapshot.blockedTasksCount}، " +
            "مأموریت فعال: ${snapshot.activeMissionsCount}، شبکه: ${network(snapshot, true)}، " +
            "قفل اضطراری: ${if (snapshot.emergencyLockActive) "فعال" else "غیرفعال"}"
    } else {
        "Focus: ${focusWindow(snapshot, false, false)}, Activity: ${activity(snapshot, false)}, " +
            "Load: ${cognitiveLoad(snapshot, false)}, Blocked: ${snapshot.blockedTasksCount}, " +
            "Active missions: ${snapshot.activeMissionsCount}, Network: ${network(snapshot, false)}, " +
            "Emergency lock: ${if (snapshot.emergencyLockActive) "ENGAGED" else "off"}"
    }
}
