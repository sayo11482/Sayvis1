package com.example.sayvis.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.trading.LitStrategyEngine
import com.example.sayvis.trading.MarketDataService
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * The SAYVIS live trading terminal: real TradingView charts (embedded widget)
 * for the global symbols, honest labelled feeds for Tether/Toman, and the LIT
 * strategy engine turning live series into concrete plans with a hard
 * risk:reward floor of 1:3 and one-tap execution through the safety-gated
 * gateway (paper simulation by default).
 */
@Composable
fun MarketsScreen(
    viewModel: SayvisViewModel,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var selected by remember { mutableStateOf(MarketDataService.Symbol.XAUUSD) }
    val snapshot by viewModel.marketSnapshot.collectAsState()
    val analyses by viewModel.marketAnalyses.collectAsState()
    val busy by viewModel.marketBusy.collectAsState()
    val autoTrade by viewModel.tradeAutomationEnabled.collectAsState()
    val tradeNote by viewModel.tradeNote.collectAsState()
    val tuning by viewModel.tradeTuning.collectAsState()
    val tuningBusy by viewModel.tuningBusy.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("markets_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SAYVIS // MARKETS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = SayvisSilverMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (busy) s.marketsRefreshing else s.marketsRefresh,
                fontSize = 11.sp,
                color = SayvisCyan,
                modifier = Modifier
                    .clickable { viewModel.refreshMarkets() }
                    .padding(6.dp)
                    .testTag("markets_refresh")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---- symbol tabs
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MarketDataService.Symbol.entries.forEachIndexed { index, symbol ->
                val selectedNow = selected == symbol
                Text(
                    text = symbol.labelFa,
                    fontSize = 10.5.sp,
                    color = if (selectedNow) SayvisSilver else SayvisSilverMuted,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            BorderStroke(1.dp, if (selectedNow) SayvisGold.copy(alpha = 0.6f) else SayvisSilverMuted.copy(alpha = 0.25f)),
                            androidx.compose.foundation.shape.RoundedCornerShape(50)
                        )
                        .clickable { selected = symbol }
                        .padding(horizontal = 4.dp, vertical = 7.dp)
                        .testTag("market_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---- live quote row
        snapshot?.quotes?.get(selected)?.let { quote ->
            SayvisCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fmt(quote.price),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilver,
                            modifier = Modifier.testTag("market_price")
                        )
                        Text(
                            text = quote.source + (if (quote.estimated) " • " + s.marketEstimated else ""),
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                    quote.change24hPercent?.let { change ->
                        Text(
                            text = (if (change >= 0) "+" else "") + "%.2f%%".format(change),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (change >= 0) SayvisGreenSuccess else SayvisRedAlert,
                            modifier = Modifier.testTag("market_change")
                        )
                    }
                }
            }
        } ?: run {
            SayvisCard {
                Text(
                    text = s.marketsPullHint,
                    fontSize = 11.5.sp,
                    color = SayvisSilverMuted,
                    modifier = Modifier.testTag("markets_empty")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---- TradingView chart (global symbols) or native panel for USDT/IRT
        if (selected.tvSymbol != null) {
            TradingViewChart(
                tvSymbol = selected.tvSymbol!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .testTag("market_tv_chart")
            )
        } else {
            SayvisCard {
                Text(
                    text = s.marketUsdtPanel,
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- LIT analysis
        analyses[selected]?.let { analysis ->
            SayvisCard {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (analysis.plan.side) {
                                LitStrategyEngine.Side.LONG -> "▲ LONG (خرید)"
                                LitStrategyEngine.Side.SHORT -> "▼ SHORT (فروش)"
                                LitStrategyEngine.Side.WAIT -> "◆ WAIT (انتظار)"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (analysis.plan.side) {
                                LitStrategyEngine.Side.LONG -> SayvisGreenSuccess
                                LitStrategyEngine.Side.SHORT -> SayvisRedAlert
                                LitStrategyEngine.Side.WAIT -> SayvisSilverMuted
                            },
                            modifier = Modifier.weight(1f).testTag("lit_side")
                        )
                        Text(
                            text = "EMA20=%.2f  EMA50=%.2f  RSI=%.0f  ATR=%.2f".format(
                                analysis.view.ema20, analysis.view.ema50, analysis.view.rsi14, analysis.view.atr14
                            ),
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SayvisSilverMuted
                        )
                    }
                    if (analysis.plan.side != LitStrategyEngine.Side.WAIT) {
                        Text(
                            text = s.litPlan(
                                analysis.plan.entry, analysis.plan.stop,
                                analysis.plan.targets.getOrNull(0)?.price ?: 0.0,
                                analysis.plan.targets.getOrNull(1)?.price ?: 0.0,
                                analysis.plan.targets.getOrNull(2)?.price ?: 0.0
                            ),
                            fontSize = 11.5.sp,
                            color = SayvisSilver
                        )
                        Text(text = analysis.plan.reasonFa, fontSize = 10.5.sp, color = SayvisSilverMuted)
                    } else {
                        Text(text = analysis.plan.reasonFa, fontSize = 11.sp, color = SayvisSilverMuted)
                    }
                    analysis.view.scenarios.forEach { scenario ->
                        Text(
                            text = "• " + scenario.labelFa + "  [" + fmt(scenario.trigger) + "]",
                            fontSize = 10.5.sp,
                            color = SayvisCyan
                        )
                    }
                    Text(
                        text = s.litRiskNote,
                        fontSize = 9.5.sp,
                        color = SayvisGold.copy(alpha = 0.9f)
                    )

                    SayvisToggleRow(
                        label = s.litAutoToggle,
                        hint = null,
                        checked = autoTrade,
                        onCheckedChange = { viewModel.setTradeAutomation(it) },
                        modifier = Modifier.testTag("lit_auto_toggle")
                    )
                    SayvisButton(
                        label = s.litExecute,
                        onClick = { viewModel.executeLitPlan(selected, analysis.plan) },
                        enabled = analysis.plan.side != LitStrategyEngine.Side.WAIT && autoTrade,
                        tone = ButtonTone.GOLD,
                        modifier = Modifier.fillMaxWidth().testTag("lit_execute")
                    )
                    tradeNote?.let { note ->
                        Text(text = note, fontSize = 10.5.sp, color = SayvisCyan, modifier = Modifier.testTag("lit_trade_note"))
                    }

                    // GitHub self-evolution: live tuning with provenance.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = s.litTuningLabel(
                                tuning.atrFactor, tuning.rsiHigh, tuning.rsiLow,
                                tuning.targetMultiples
                            ),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SayvisSilverMuted,
                            modifier = Modifier.weight(1f).testTag("lit_tuning")
                        )
                    }
                    if (tuning.sourceRepos.isNotEmpty()) {
                        Text(
                            text = "⭐ " + tuning.sourceRepos.take(3).joinToString(" • "),
                            fontSize = 9.5.sp,
                            color = SayvisGreenSuccess
                        )
                    }
                    SayvisButton(
                        label = if (tuningBusy) s.litTuningScanning else s.litTuningScan,
                        onClick = { viewModel.applyTradingEvolution() },
                        busy = tuningBusy,
                        tone = ButtonTone.NEUTRAL,
                        modifier = Modifier.fillMaxWidth().testTag("lit_tuning_scan")
                    )
                }
            }
        } ?: run {
            SayvisCard {
                Text(
                    text = s.marketsAnalysing,
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TradingViewChart(tvSymbol: String, modifier: Modifier = Modifier) {
    val url = "https://s.tradingview.com/widgetembed/?frameElementId=sayvis_tv&symbol=" +
        android.net.Uri.encode(tvSymbol) +
        "&interval=60&hidesidetoolbar=1&symboledit=0&saveimage=0&toolbarbg=0B1220&theme=dark&style=1&timezone=Etc%2FUTC&locale=en"
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                setBackgroundColor(0xFF0B1220.toInt())
                loadUrl(url)
            }
        },
        update = { web -> web.loadUrl(url) },
        modifier = modifier
    )
}

private fun fmt(value: Double): String = when {
    value >= 100000 -> "%.0f".format(value)
    value >= 1000 -> "%.1f".format(value)
    else -> "%.4f".format(value)
}
