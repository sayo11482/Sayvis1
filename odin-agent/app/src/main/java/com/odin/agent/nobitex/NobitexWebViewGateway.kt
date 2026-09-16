package com.odin.agent.nobitex

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN v1.0.18 - Nobitex WebView Gateway - REAL Nobitex login with captcha support
 * درگاه واقعی نوبیتکس - کپچای دستی - بعد کانکت موجودی نمایش داده شود
 * 11M users - Iran largest exchange - https://nobitex.ir/panel/balance/spot/
 */

data class NobitexWebState(
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val title: String = "",
    val isConnected: Boolean = false,
    val balanceIRT: Double = 0.0,
    val balanceUSDT: Double = 0.0,
    val lastError: String? = null
)

class NobitexWebViewGateway {

    private val _state = MutableStateFlow(NobitexWebState())
    val state: StateFlow<NobitexWebState> = _state

    val nobitexUrls = listOf(
        "https://nobitex.ir/panel/balance/spot/" to "موجودی اسپات - Spot Balance REAL",
        "https://nobitex.ir/panel/exchange/usdt-irt/" to "معامله تتر/تومان - USDT/IRT REAL",
        "https://nobitex.ir/panel/exchange/btc-irt/" to "معامله بیت‌کوین - BTC/IRT REAL",
        "https://nobitex.ir/price/usdt/" to "قیمت تتر REAL - 231K Toman",
        "https://nobitex.ir/" to "نوبیتکس اصلی - 11M users",
        "https://nobitex.ir/panel/history/orders/" to "تاریخچه سفارشات - Orders REAL"
    )

    fun getDefaultUrl(): String = nobitexUrls[0].first
    fun getAllUrls(): List<Pair<String, String>> = nobitexUrls

    fun createWebViewClient(onStateChange: (NobitexWebState) -> Unit): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                _state.value = _state.value.copy(
                    url = url ?: "",
                    isLoading = true,
                    canGoBack = view?.canGoBack() ?: false
                )
                onStateChange(_state.value)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                _state.value = _state.value.copy(
                    url = url ?: "",
                    isLoading = false,
                    canGoBack = view?.canGoBack() ?: false,
                    title = view?.title ?: ""
                )
                onStateChange(_state.value)

                // Auto detect login success and extract balance
                view?.evaluateJavascript(
                    """
                    (function() {
                        var bodyText = document.body.innerText || '';
                        var isLoggedIn = bodyText.includes('موجودی') || bodyText.includes('کیف پول') || bodyText.includes('Balance') || document.querySelector('[class*=balance]') !== null;
                        var title = document.title;
                        return JSON.stringify({loggedIn: isLoggedIn, title: title, hasBalance: bodyText.includes('تومان') || bodyText.includes('IRT')});
                    })();
                    """.trimIndent()
                ) { result ->
                    try {
                        if (result.contains("true")) {
                            _state.value = _state.value.copy(isConnected = true)
                            onStateChange(_state.value)
                        }
                    } catch (e: Exception) {}
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                _state.value = _state.value.copy(
                    isLoading = false,
                    lastError = "Error $errorCode: $description"
                )
                onStateChange(_state.value)
            }
        }
    }

    fun createWebChromeClient(onStateChange: (NobitexWebState) -> Unit): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    _state.value = _state.value.copy(isLoading = false)
                    onStateChange(_state.value)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                _state.value = _state.value.copy(title = title ?: "")
                onStateChange(_state.value)
            }
        }
    }

    fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        }
    }

    fun extractBalanceFromPage(webView: WebView, onBalance: (Double, Double) -> Unit) {
        webView.evaluateJavascript(
            """
            (function() {
                var bodyText = document.body.innerText;
                // Try to find IRT balance and USDT balance
                var irtMatch = bodyText.match(/([0-9,]+)\s*(?:تومان|IRT|ریال)/);
                var usdtMatch = bodyText.match(/([0-9,.]+)\s*USDT/);
                var irt = 0, usdt = 0;
                if (irtMatch) irt = parseFloat(irtMatch[1].replace(/,/g, ''));
                if (usdtMatch) usdt = parseFloat(usdtMatch[1].replace(/,/g, ''));
                // Also try to find in specific elements
                var balanceEls = document.querySelectorAll('[class*=balance], [class*=Balance], [data-balance]');
                var balances = [];
                balanceEls.forEach(function(el) {
                    var txt = el.textContent;
                    var m = txt.match(/([0-9,]+\.?[0-9]*)/);
                    if (m) balances.push(m[1]);
                });
                return JSON.stringify({irt: irt, usdt: usdt, all: balances, text: bodyText.substring(0,500)});
            })();
            """.trimIndent()
        ) { result ->
            try {
                val clean = result.replace("\\\"", "\"").replace("\"", "")
                // Simple parse
                val irtRegex = Regex("""irt[^\d]*([0-9,.]+)""", RegexOption.IGNORE_CASE)
                val usdtRegex = Regex("""usdt[^\d]*([0-9,.]+)""", RegexOption.IGNORE_CASE)
                val irtMatch = irtRegex.find(clean)
                val usdtMatch = usdtRegex.find(clean)
                val irt = irtMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val usdt = usdtMatch?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                if (irt > 0 || usdt > 0) {
                    _state.value = _state.value.copy(balanceIRT = irt, balanceUSDT = usdt, isConnected = true)
                    onBalance(irt, usdt)
                }
            } catch (e: Exception) {}
        }
    }

    fun extractAllBalances(webView: WebView, onBalances: (Map<String, Double>) -> Unit) {
        webView.evaluateJavascript(
            """
            (function() {
                var result = {};
                // Find all wallet rows
                var rows = document.querySelectorAll('tr, [class*=wallet], [class*=asset]');
                rows.forEach(function(row) {
                    var text = row.innerText;
                    // Match like BTC 0.001 or USDT 100
                    var match = text.match(/(BTC|ETH|USDT|IRT|RLS|SHIB|DOGE)\s*([0-9,.]+)/i);
                    if (match) {
                        var symbol = match[1].toUpperCase();
                        var amount = parseFloat(match[2].replace(/,/g, ''));
                        if (amount > 0) result[symbol] = amount;
                    }
                });
                // Also try to get from page text
                var bodyText = document.body.innerText;
                var btcMatch = bodyText.match(/BTC[^\d]*([0-9,.]+)/i);
                var ethMatch = bodyText.match(/ETH[^\d]*([0-9,.]+)/i);
                var usdtMatch = bodyText.match(/USDT[^\d]*([0-9,.]+)/i);
                if (btcMatch) result['BTC'] = parseFloat(btcMatch[1].replace(/,/g, ''));
                if (ethMatch) result['ETH'] = parseFloat(ethMatch[1].replace(/,/g, ''));
                if (usdtMatch) result['USDT'] = parseFloat(usdtMatch[1].replace(/,/g, ''));
                return JSON.stringify(result);
            })();
            """.trimIndent()
        ) { result ->
            try {
                // Parse JSON result
                val balances = mutableMapOf<String, Double>()
                // Simple regex parse
                val regex = Regex("\"([A-Z]+)\"\\s*:\\s*([0-9.]+)")
                regex.findAll(result).forEach { match ->
                    val sym = match.groupValues[1]
                    val amt = match.groupValues[2].toDoubleOrNull() ?: 0.0
                    if (amt > 0) balances[sym] = amt
                }
                if (balances.isNotEmpty()) {
                    onBalances(balances)
                }
            } catch (e: Exception) {}
        }
    }
}
