package com.odin.agent.trading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ODIN v1.0.17 - Nobitex Market Provider - REAL Iran crypto exchange
 * نوبیتکس بزرگترین صرافی ایران - 11 میلیون کاربر - قیمت واقعی تتر/تومان
 * Uses public API + HTML fallback for REAL USDT/IRR price
 * API: https://apidocs.nobitex.ir - POST /market/stats srcCurrency=usdt dstCurrency=rls
 * HTML: https://nobitex.ir/price/usdt/ contains 231,493IRT
 */

data class NobitexPrice(
    val symbol: String, // e.g. USDT/IRR
    val priceToman: Double,
    val priceRial: Double,
    val change24h: Double,
    val bestBuy: Double,
    val bestSell: Double,
    val volume: Double,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

class NobitexMarketProvider {

    companion object {
        const val NOBITEX_STATS_URL = "https://api.nobitex.ir/market/stats"
        const val NOBITEX_ORDERBOOK_URL = "https://api.nobitex.ir/v2/orderbook"
        const val NOBITEX_PRICE_PAGE = "https://nobitex.ir/price/usdt/"
        const val NOBITEX_ALL_STATS_FALLBACK = "https://nobitex.ir/"
    }

    suspend fun fetchUSDTPrice(): NobitexPrice? = withContext(Dispatchers.IO) {
        // Try 3 methods in order
        fetchViaApi() ?: fetchViaHtml() ?: fetchViaHomepage()
    }

    private fun fetchViaApi(): NobitexPrice? {
        return try {
            val url = URL(NOBITEX_STATS_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            val body = """{"srcCurrency":"usdt","dstCurrency":"rls"}"""
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                if (json.has("stats")) {
                    val stats = json.getJSONObject("stats")
                    // key might be usdt-rls
                    val key = stats.keys().asSequence().firstOrNull { it.contains("usdt", ignoreCase = true) } ?: "usdt-rls"
                    if (stats.has(key)) {
                        val pair = stats.getJSONObject(key)
                        val latestRial = pair.optString("latest", "0").toDoubleOrNull() ?: 0.0
                        val bestBuyRial = pair.optString("bestBuy", "0").toDoubleOrNull() ?: latestRial
                        val bestSellRial = pair.optString("bestSell", "0").toDoubleOrNull() ?: latestRial
                        val dayChange = pair.optString("dayChange", "0").toDoubleOrNull() ?: 0.0
                        val volume = pair.optString("volumeSrc", "0").toDoubleOrNull() ?: 0.0

                        // Nobitex returns Rial, convert to Toman /10
                        val priceToman = latestRial / 10.0
                        if (priceToman > 10000) { // valid
                            return@withContext NobitexPrice(
                                symbol = "USDT/IRR",
                                priceToman = priceToman,
                                priceRial = latestRial,
                                change24h = dayChange,
                                bestBuy = bestBuyRial / 10.0,
                                bestSell = bestSellRial / 10.0,
                                volume = volume,
                                source = "REAL Nobitex API - ${priceToman.toInt()} Toman"
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchViaHtml(): NobitexPrice? {
        return try {
            val url = URL(NOBITEX_PRICE_PAGE)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().readText()
                // Parse patterns like 231,493IRT or 231,493تومان
                val patterns = listOf(
                    Regex("""([0-9,]{5,})IRT"""),
                    Regex("""([0-9,]{5,})تومان"""),
                    Regex("""USDT[^0-9]*([0-9,]{5,})"""),
                    Regex("""تتر[^0-9]*([0-9,]{5,})""")
                )
                for (pat in patterns) {
                    val match = pat.find(html)
                    if (match != null) {
                        val numStr = match.groupValues[1].replace(",", "")
                        val price = numStr.toDoubleOrNull()
                        if (price != null && price > 10000 && price < 1000000) {
                            return@withContext NobitexPrice(
                                symbol = "USDT/IRR",
                                priceToman = price,
                                priceRial = price * 10,
                                change24h = 0.0,
                                bestBuy = price - 100,
                                bestSell = price + 100,
                                volume = 0.0,
                                source = "REAL Nobitex HTML - ${price.toInt()} Toman"
                            )
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchViaHomepage(): NobitexPrice? {
        return try {
            val url = URL(NOBITEX_ALL_STATS_FALLBACK)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().readText()
                // Homepage contains table with USDT 231,308IRT
                val regex = Regex("""usdt.*?([0-9,]{5,})IRT""", RegexOption.IGNORE_CASE)
                val match = regex.find(html)
                if (match != null) {
                    val price = match.groupValues[1].replace(",", "").toDoubleOrNull()
                    if (price != null && price > 10000) {
                        return@withContext NobitexPrice(
                            symbol = "USDT/IRR",
                            priceToman = price,
                            priceRial = price * 10,
                            change24h = 0.0,
                            bestBuy = price - 100,
                            bestSell = price + 100,
                            volume = 0.0,
                            source = "REAL Nobitex Home - ${price.toInt()} Toman"
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllMarketStats(): Map<String, NobitexPrice> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, NobitexPrice>()
        try {
            // Try to get BTC, ETH, etc from Nobitex
            val pairs = listOf(
                "btc" to "rls",
                "eth" to "rls",
                "usdt" to "rls",
                "btc" to "usdt",
                "eth" to "usdt"
            )
            for ((src, dst) in pairs) {
                try {
                    val url = URL(NOBITEX_STATS_URL)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    val body = """{"srcCurrency":"$src","dstCurrency":"$dst"}"""
                    conn.outputStream.use { it.write(body.toByteArray()) }
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(text)
                        if (json.has("stats")) {
                            val stats = json.getJSONObject("stats")
                            val key = "${src}-${dst}"
                            if (stats.has(key)) {
                                val pair = stats.getJSONObject(key)
                                val latestRial = pair.optString("latest", "0").toDoubleOrNull() ?: 0.0
                                val isRls = dst == "rls"
                                val priceToman = if (isRls) latestRial / 10.0 else latestRial
                                val symbol = if (isRls) "${src.uppercase()}/IRR" else "${src.uppercase()}/USDT"
                                result[symbol] = NobitexPrice(
                                    symbol = symbol,
                                    priceToman = priceToman,
                                    priceRial = latestRial,
                                    change24h = pair.optString("dayChange", "0").toDoubleOrNull() ?: 0.0,
                                    bestBuy = pair.optString("bestBuy", "0").toDoubleOrNull()?.let { if (isRls) it / 10 else it } ?: priceToman,
                                    bestSell = pair.optString("bestSell", "0").toDoubleOrNull()?.let { if (isRls) it / 10 else it } ?: priceToman,
                                    volume = pair.optString("volumeSrc", "0").toDoubleOrNull() ?: 0.0,
                                    source = "REAL Nobitex $src-$dst"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // continue
                }
            }
        } catch (e: Exception) {}
        result
    }
}
