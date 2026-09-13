package com.example.sayvis.core

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

/**
 * Real hardware telemetry snapshot for the AWARE engine and the AI system prompt.
 * Every read is defensive (runCatching) so the app also works on emulators,
 * Robolectric tests and devices with unusual hardware.
 */
data class DeviceTelemetry(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "UNKNOWN",
    val isOnline: Boolean = false,
    val storageFreeGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val ramTotalGb: Float? = null,
    val deviceModel: String = Build.MODEL ?: "unknown",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
)

object TelemetryProvider {

    fun snapshot(context: Context): DeviceTelemetry {
        val appContext = context.applicationContext

        val battery = runCatching {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        }.getOrDefault(-1)

        val charging = runCatching {
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.isCharging ?: false
        }.getOrDefault(false)

        val network = runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            when {
                caps == null -> "NONE" to false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI" to true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR" to true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET" to true
                else -> "UNKNOWN" to true
            }
        }.getOrDefault("UNKNOWN" to false)

        val storage = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
            val free = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
            free to total
        }.getOrDefault(0f to 0f)

        val ramTotal = runCatching {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val info = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(info)
            if (info.totalMem > 0) info.totalMem / (1024f * 1024f * 1024f) else null
        }.getOrNull()

        return DeviceTelemetry(
            batteryLevel = if (battery in 1..100) battery else 100,
            isCharging = charging,
            networkType = network.first,
            isOnline = network.second,
            storageFreeGb = storage.first,
            storageTotalGb = storage.second,
            ramTotalGb = ramTotal
        )
    }
}
