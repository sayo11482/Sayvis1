package com.odin.agent.nobitex

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ODIN v1.0.18 - Nobitex API Manager - Private API for balance & trading
 * مدیریت API خصوصی نوبیتکس - موجودی و معامله واقعی
 * Docs: https://apidocs.nobitex.ir
 */

data class NobitexWallet(
    val currency: String,
    val balance: Double,
    val blocked: Double = 0.0,
    val activeBalance: Double = 0.0,
    val rialBalance: Double = 0.0
)

data class NobitexOrder(
    val id: String,
    val type: String, // buy/sell
    val srcCurrency: String,
    val dstCurrency: String,
    val amount: Double,
    val price: Double,
    val status: String,
    val timestamp: Long
)

data class NobitexApiState(
    val isConnected: Boolean = false,
    val token: String = "",
    val wallets: Map<String, NobitexWallet> = emptyMap(),
    val orders: List<NobitexOrder> = emptyList(),
    val totalIRT: Double = 0.0,
    val totalUSDT: Double = 0.0,
    val lastError: String? = null,
    val isLoading: Boolean = false
)

class NobitexApiManager {

    private val _state = MutableStateFlow(NobitexApiState())
    val state: StateFlow<NobitexApiState> = _state

    companion object {
        const val BASE_URL = "https://api.nobitex.ir"
    }

    suspend fun connect(token: String): Boolean = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(isLoading = true, lastError = null)
        try {
            val wallets = fetchWallets(token)
            if (wallets.isNotEmpty()) {
                val totalIRT = wallets["rls"]?.balance ?: wallets["irt"]?.balance ?: 0.0
                val totalUSDT = wallets["usdt"]?.balance ?: 0.0
                _state.value = _state.value.copy(
                    isConnected = true,
                    token = token,
                    wallets = wallets,
                    totalIRT = totalIRT,
                    totalUSDT = totalUSDT,
                    isLoading = false
                )
                return@withContext true
            } else {
                _state.value = _state.value.copy(isLoading = false, lastError = "Failed to fetch wallets - check token")
                return@withContext false
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, lastError = e.message)
            return@withContext false
        }
    }

    fun disconnect() {
        _state.value = NobitexApiState()
    }

    suspend fun fetchWallets(token: String): Map<String, NobitexWallet> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, NobitexWallet>()
        try {
            val url = URL("$BASE_URL/users/wallets/list")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Token $token")
            conn.doOutput = true
            conn.outputStream.use { it.write("{}".toByteArray()) }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                if (json.has("wallets")) {
                    val walletsArray = json.getJSONArray("wallets")
                    for (i in 0 until walletsArray.length()) {
                        val w = walletsArray.getJSONObject(i)
                        val currency = w.optString("currency", "").lowercase()
                        val balance = w.optString("balance", "0").toDoubleOrNull() ?: 0.0
                        val blocked = w.optString("blockedBalance", "0").toDoubleOrNull() ?: 0.0
                        result[currency] = NobitexWallet(
                            currency = currency,
                            balance = balance,
                            blocked = blocked,
                            activeBalance = balance - blocked,
                            rialBalance = if (currency == "rls" || currency == "irt") balance else 0.0
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Try alternative endpoint /users/wallets/balance for each currency
            val currencies = listOf("rls", "btc", "eth", "usdt", "shib", "doge", "ltc", "xrp")
            for (curr in currencies) {
                try {
                    val bal = fetchSingleBalance(token, curr)
                    if (bal != null) result[curr] = bal
                } catch (e2: Exception) {}
            }
        }
        result
    }

    private fun fetchSingleBalance(token: String, currency: String): NobitexWallet? {
        return try {
            val url = URL("$BASE_URL/users/wallets/balance")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Token $token")
            conn.doOutput = true
            val body = """{"currency":"$currency"}"""
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val balance = json.optString("balance", "0").toDoubleOrNull() ?: 0.0
                NobitexWallet(currency = currency, balance = balance, activeBalance = balance)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun placeOrder(
        token: String,
        type: String, // buy/sell
        srcCurrency: String, // btc
        dstCurrency: String, // rls
        amount: Double,
        price: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/market/orders/add")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Token $token")
            conn.doOutput = true
            val body = JSONObject().apply {
                put("type", type)
                put("srcCurrency", srcCurrency)
                put("dstCurrency", dstCurrency)
                put("amount", amount.toString())
                put("price", price.toString())
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val text = if (code == 200) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
            return@withContext code == 200 && text.contains("ok")
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun fetchOrders(token: String): List<NobitexOrder> = withContext(Dispatchers.IO) {
        val orders = mutableListOf<NobitexOrder>()
        try {
            val url = URL("$BASE_URL/market/orders/list")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Token $token")
            conn.doOutput = true
            conn.outputStream.use { it.write("{}".toByteArray()) }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                if (json.has("orders")) {
                    val arr = json.getJSONArray("orders")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        orders.add(
                            NobitexOrder(
                                id = o.optString("id", ""),
                                type = o.optString("type", ""),
                                srcCurrency = o.optString("srcCurrency", ""),
                                dstCurrency = o.optString("dstCurrency", ""),
                                amount = o.optString("amount", "0").toDoubleOrNull() ?: 0.0,
                                price = o.optString("price", "0").toDoubleOrNull() ?: 0.0,
                                status = o.optString("status", ""),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {}
        orders
    }
}
