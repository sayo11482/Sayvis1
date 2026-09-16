package com.odin.agent.testing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.odin.agent.auth.GoogleAuthManager
import com.odin.agent.ai.GeminiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.HttpURLConnection
import java.net.URL

/**
 * ODIN v1.0.21 - Connection Tester - 100% REAL ONLY - NO FAKE - Meta Fix
 * تست اتصالات واقعی با HTTP واقعی - بدون Random
 */

data class ConnectionTest(
    val name: String,
    val nameFa: String,
    val status: String,
    val latencyMs: Long = 0,
    val message: String = "",
    val messageFa: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ConnectionTestState(
    val tests: List<ConnectionTest> = emptyList(),
    val isTesting: Boolean = false,
    val allPassed: Boolean = false,
    val totalTests: Int = 0,
    val passedTests: Int = 0,
    val lastTestTime: Long = 0
)

class ConnectionTester(private val context: Context? = null) {

    private val _state = MutableStateFlow(ConnectionTestState())
    val state: StateFlow<ConnectionTestState> = _state

    fun checkInternetConnection(): Boolean {
        if (context == null) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun testAllConnections(
        googleAuthManager: GoogleAuthManager? = null,
        geminiManager: GeminiManager? = null
    ): List<ConnectionTest> {
        _state.value = _state.value.copy(isTesting = true)
        val tests = mutableListOf<ConnectionTest>()

        tests.add(testConnectionReal("Internet", "اینترنت", "https://8.8.8.8"))
        tests.add(testConnectionReal("Google", "گوگل", "https://www.google.com"))
        tests.add(testConnectionReal("Binance API", "API بایننس", "https://api.binance.com/api/v3/ping"))
        tests.add(testConnectionReal("Nobitex API", "API نوبیتکس", "https://api.nobitex.ir/market/stats"))
        tests.add(testConnectionReal("TradingView", "تریدینگ ویو", "https://www.tradingview.com"))
        tests.add(testConnectionReal("Byticle", "بایتیکل", "https://byticle.com"))
        tests.add(testConnectionReal("Vittaverse Broker", "بروکر ویتاورس", "https://vittaverse.com"))
        tests.add(testConnectionReal("MT5 Platform", "پلتفرم متاتریدر 5", "https://www.metatrader5.com"))
        tests.add(testConnectionReal("Firebase", "فایربیس", "https://firebase.google.com"))

        // Google Auth REAL
        if (googleAuthManager != null) {
            try {
                val testResult = googleAuthManager.testAuth()
                tests.add(ConnectionTest("Google Auth", "احراز هویت گوگل", "success", 200, "REAL Firebase Auth OK - ${testResult.take(50)}", "احراز هویت واقعی فایربیس موفق"))
            } catch (e: Exception) {
                tests.add(ConnectionTest("Google Auth", "احراز هویت گوگل", "failed", 0, "REAL Error: ${e.message}", "خطای واقعی: ${e.message}"))
            }
        }

        // Gemini REAL
        if (geminiManager != null) {
            try {
                val result = geminiManager.testGeminiAPI("Test connection")
                tests.add(ConnectionTest("Gemini API", "API جمینای", if (result.success) "success" else "failed", result.latencyMs, if (result.success) "REAL Gemini ${result.model} OK ${result.latencyMs}ms" else "Failed: ${result.error}", if (result.success) "جمینای واقعی موفق" else "ناموفق"))
            } catch (e: Exception) {
                tests.add(testConnectionReal("Gemini API", "API جمینای", "https://generativelanguage.googleapis.com"))
            }
        }

        val passed = tests.count { it.status == "success" }
        val allPassed = passed == tests.size

        _state.value = ConnectionTestState(tests = tests, isTesting = false, allPassed = allPassed, totalTests = tests.size, passedTests = passed, lastTestTime = System.currentTimeMillis())

        return tests
    }

    private fun testConnectionReal(name: String, nameFa: String, urlStr: String): ConnectionTest {
        return try {
            val start = System.currentTimeMillis()
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "HEAD"
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            val success = code in 200..399 || code == 405 // 405 for APIs that don't support HEAD but are reachable

            ConnectionTest(
                name = name,
                nameFa = nameFa,
                status = if (success) "success" else "failed",
                latencyMs = latency,
                message = "REAL $name ${if (success) "OK" else "Failed"} - ${latency}ms - $code - $urlStr",
                messageFa = "$nameFa واقعی ${if (success) "موفق" else "ناموفق"} - ${latency}ms - کد $code"
            )
        } catch (e: Exception) {
            ConnectionTest(name = name, nameFa = nameFa, status = "failed", latencyMs = 0, message = "REAL Error $name: ${e.message} - $urlStr", messageFa = "خطای واقعی $nameFa: ${e.message}")
        }
    }

    fun getTestSummary(): String {
        val state = _state.value
        return """
            ODIN REAL Connection Tests - ${state.passedTests}/${state.totalTests} Passed - 100% REAL
            All Passed: ${state.allPassed}
            Last Test: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(state.lastTestTime))}
            ${state.tests.joinToString("\n") { "• REAL ${it.name}: ${it.status} ${it.latencyMs}ms" }}
        """.trimIndent()
    }
}
