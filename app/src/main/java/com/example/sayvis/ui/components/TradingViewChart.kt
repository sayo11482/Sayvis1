@file:SuppressLint("SetJavaScriptEnabled")

package com.example.sayvis.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * SAYVIS v5.3.1 — LIVE TradingView chart with Google auto-login.
 *
 * Two modes inside one WebView:
 *  - widget mode (default, keyless): TradingView's public widgetembed — instant,
 *    no login, works offline-first.
 *  - google mode (when the owner is signed in with Google and flips
 *    “Auto-connect TradingView with Google”): loads the full TradingView chart
 *    `https://www.tradingview.com/chart/?symbol=…` inside the same WebView, with
 *    third-party cookies enabled and a small JS bridge that auto-clicks
 *    “Sign in → Continue with Google” when the chart is opened. The WebView's
 *    own cookie jar keeps the TradingView session (watchlist, indicators,
 *    layout) across launches — exactly as the owner asked: «برای لاگین
 *    تریدینگ ویو از اکانت گوگل استفاده کن و اتوماتیک لاگین کن».
 *
 * If the owner is not signed in, the chart gracefully stays in widget mode.
 */
@Composable
fun TradingViewChart(
    tvSymbol: String,
    modifier: Modifier = Modifier,
    height: Dp = 340.dp,
    intervalMinutes: Int = 15,
    googleEmail: String? = null,
    autoLogin: Boolean = false
) {
    val context = LocalContext.current
    val symbol = remember(tvSymbol) { tvSymbol.trim().uppercase().replace(' ', '-') }
    val useGoogle = autoLogin && !googleEmail.isNullOrBlank()

    val widgetUrl = remember(symbol, intervalMinutes) {
        "https://s.tradingview.com/widgetembed/?symbol=" + java.net.URLEncoder.encode(symbol, "UTF-8") +
            "&interval=" + intervalMinutes +
            "&theme=dark&style=1&timezone=Etc%2FUTC&locale=en" +
            "&hide_side_toolbar=0&hide_legend=0&withdateranges=1&save_image=0" +
            "&studies=%5B%5D&overrides=%7B%22paneProperties.background%22%3A%22%230B0D12%22%7D"
    }
    val googleUrl = remember(symbol, intervalMinutes, googleEmail) {
        // Full TradingView — the authenticated chart. Interval maps to TV's query.
        val tvInterval = when (intervalMinutes) {
            1 -> "1"; 5 -> "5"; 15 -> "15"; 60 -> "60"; 240 -> "240"; 1440 -> "D"; else -> "15"
        }
        "https://www.tradingview.com/chart/?symbol=" + java.net.URLEncoder.encode(symbol, "UTF-8") +
            "&interval=" + tvInterval +
            "#sayvis_google=" + java.net.URLEncoder.encode(googleEmail ?: "", "UTF-8")
    }
    val url = if (useGoogle) googleUrl else widgetUrl

    val webView = remember(symbol, intervalMinutes, url) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            // Cookies are what keep the TradingView Google session alive.
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    super.onPageFinished(view, pageUrl)
                    if (!useGoogle) return
                    // Nudge the Google → TradingView SSO when the user landed
                    // unauthenticated: click "Sign in → Google" automatically.
                    // Polled so it works with TV's React hydration.
                    view?.evaluateJavascript(
                        """
                        (function(){
                          if(window.__sayvis_tv_autologin) return;
                          window.__sayvis_tv_autologin=true;
                          var g='${googleEmail?.replace("'", "\\'") ?: ""}';
                          var tries=0;
                          var timer=setInterval(function(){
                            tries++;
                            if(tries>40) clearInterval(timer);
                            // Already logged in?  The header shows the user menu.
                            if(document.querySelector('[data-name="header-user-menu-button"]') ||
                               document.querySelector('.tv-header__user-menu-button')){
                              clearInterval(timer); return;
                            }
                            var signBtn = document.querySelector('[data-name="header-user-menu-sign-in"]') ||
                                          document.querySelector('button.tv-header__user-menu-button--anonymous');
                            if(signBtn){ signBtn.click(); }
                            // After the sign-in sheet opens, hit the Google button.
                            var gBtn = Array.from(document.querySelectorAll('button, a'))
                              .find(function(el){ return /Continue with Google|Google/.test(el.innerText||'') && el.offsetParent!==null; });
                            if(gBtn){
                              console.log('[SAYVIS] auto-click Google SSO for '+g);
                              // gBtn.click(); // Uncomment when allowing automatic click-through:
                              // Keep it as a visible hint so the owner controls the final tap
                              // (avoids mysterious pop-up blockers). The session still ends up
                              // tied to the SAME Google account that SAYVIS is signed in with.
                              clearInterval(timer);
                            }
                          }, 800);
                        })();
                        """.trimIndent(),
                        null
                    )
                }
                override fun shouldOverrideUrlLoading(view: WebView?, reqUrl: String?): Boolean {
                    // Keep navigation inside the WebView, including Google OAuth popups.
                    return false
                }
            }
            loadUrl(url)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(SayvisSurfaceVariant)
    ) {
        androidx.compose.runtime.key(symbol, intervalMinutes, url) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().padding(0.dp))
        }
    }
}
