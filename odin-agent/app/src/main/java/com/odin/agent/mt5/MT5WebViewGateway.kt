package com.odin.agent.mt5

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ODIN v1.0.16 - MT5 WebView Gateway - REAL MT5 login with captcha support
 * درگاه اتصال واقعی خود متاتریدر با قابلیت ورود دستی کپچا
 */

data class MT5WebState(
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val title: String = "",
    val isConnected: Boolean = false,
    val balance: Double = 0.0,
    val lastError: String? = null
)

class MT5WebViewGateway {

    private val _state = MutableStateFlow(MT5WebState())
    val state: StateFlow<MT5WebState> = _state

    // Real MT5 WebTrader URLs
    val vittaverseWebTraderUrls = listOf(
        "https://trade.vittaverse.com" to "Vittaverse WebTrader REAL",
        "https://metatrader5.com/en/terminal/help/startworking/authorization" to "MT5 Official Help",
        "https://webtrader.vittaverse.com" to "Vittaverse Web Terminal",
        "https://trade.mql5.com/trade?servers=Vittaverse-Real&trade_server=Vittaverse-Real&startup_mode=open_demo&lang=en" to "MQL5 WebTerminal - Vittaverse Real"
    )

    // For direct MT5 connection via broker's Web API
    val mt5WebApiUrls = listOf(
        "https://mt5.vittaverse.com" to "Vittaverse MT5 Server",
        "https://web.vittaverse.com" to "Vittaverse Web Platform"
    )

    fun getDefaultUrl(): String = vittaverseWebTraderUrls[0].first

    fun getAllUrls(): List<Pair<String, String>> = vittaverseWebTraderUrls + mt5WebApiUrls

    fun createWebViewClient(onStateChange: (MT5WebState) -> Unit): WebViewClient {
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

                // Try to detect if login succeeded by checking page content
                // In real MT5 WebTrader, after login, balance appears
                view?.evaluateJavascript(
                    """
                    (function() {
                        // Try to find balance in MT5 WebTrader DOM
                        var balance = null;
                        // Common selectors for MT5 WebTrader balance
                        var selectors = ['.balance', '#balance', '[class*=\"balance\"]', '[class*=\"Balance\"]'];
                        for (var sel of selectors) {
                            var el = document.querySelector(sel);
                            if (el) { balance = el.textContent; break; }
                        }
                        return balance || document.title;
                    })();
                    """.trimIndent()
                ) { result ->
                    if (result.contains("Trade") || result.contains("Balance") || result.contains("Account")) {
                        _state.value = _state.value.copy(isConnected = true)
                        onStateChange(_state.value)
                    }
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

    fun createWebChromeClient(onStateChange: (MT5WebState) -> Unit): WebChromeClient {
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

    // Extract balance from MT5 WebTrader after login
    fun extractBalanceFromPage(webView: WebView, onBalance: (Double) -> Unit) {
        webView.evaluateJavascript(
            """
            (function() {
                // Try multiple ways to get balance from MT5 WebTrader
                var texts = document.body.innerText;
                var balanceMatch = texts.match(/Balance[:\s]*\$?([0-9,]+\.?[0-9]*)/i);
                if (balanceMatch) return balanceMatch[1].replace(/,/g, '');
                
                // Try to find in specific elements
                var balanceEl = document.querySelector('.account-balance, .balance-value, [data-balance]');
                if (balanceEl) {
                    var val = balanceEl.textContent.match(/([0-9,]+\.?[0-9]*)/);
                    if (val) return val[1].replace(/,/g, '');
                }
                return '0';
            })();
            """.trimIndent()
        ) { result ->
            try {
                val clean = result.replace("\"", "").replace(",", "")
                val balance = clean.toDoubleOrNull() ?: 0.0
                if (balance > 0) {
                    _state.value = _state.value.copy(balance = balance, isConnected = true)
                    onBalance(balance)
                }
            } catch (e: Exception) {}
        }
    }
}
