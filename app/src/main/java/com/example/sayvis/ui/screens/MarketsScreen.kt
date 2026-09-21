package com.example.sayvis.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.trading.LitStrategyEngine
import com.example.sayvis.trading.MarketDataService
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.components.TradingViewChart
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * SAYVIS v5.3.1 — Live trading terminal with smart AI auto-trade.
 *
 * - Real TradingView chart with optional Google auto-login (سایویس Google account)
 * - LIT engine finds the trade with the HIGHEST yield (RR≥1:3) via backtests & MTF confluence
 * - One-tap smart execution: AI picks the best entry and routes it through the safety-gated gateway
 *   (paper / demo / LIVE on REAL — Kill Switch & daily cap stay armed)
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
    val mtfBusy by viewModel.mtfBusy.collectAsState()
    val mtfReports by viewModel.mtfReports.collectAsState()
    val googleEmail by viewModel.googleEmail.collectAsState()
    val tvAutoLogin by viewModel.tvAutoLoginEnabled.collectAsState()
    val googleSignedIn by viewModel.googleSignedIn.collectAsState()
    val gatewayState by viewModel.gatewayState.collectAsState()

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

        // ---- TradingView chart with Google auto-login toggle ----
        if (selected.tvSymbol != null) {
            SayvisCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = s.tvTitle + " — " + selected.tvSymbol!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilver,
                            modifier = Modifier.weight(1f)
                        )
                        if (gatewayState.profile.executionMode == com.example.sayvis.settings.TradingExecutionMode.LIVE_EXECUTION) {
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = SayvisRedAlert,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SayvisRedAlert.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    TradingViewChart(
                        tvSymbol = selected.tvSymbol!!,
                        googleEmail = googleEmail,
                        autoLogin = tvAutoLogin && googleSignedIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .testTag("market_tv_chart"),
                        intervalMinutes = 15
                    )
                    // Google auto-login row
                    SayvisToggleRow(
                        label = s.tvAutoLogin,
                        hint = if (!googleSignedIn) s.tvNeedGoogle else if (tvAutoLogin) s.tvAutoLoginOn else s.tvAutoLoginOff,
                        checked = tvAutoLogin,
                        onCheckedChange = { viewModel.setTradingViewAutoLogin(it) },
                        modifier = Modifier.testTag("tv_autologin_toggle")
                    )
                    Text(
                        text = s.tvNoLoginNote,
                        fontSize = 9.5.sp,
                        color = SayvisSilverMuted
                    )
                    if (tvAutoLogin && googleSignedIn) {
                        Text(
                            text = s.tvGoogleBanner + " (" + googleEmail + ")",
                            fontSize = 9.5.sp,
                            color = SayvisCyan
                        )
                    }
                }
            }
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

        // ---- SMART AI AUTO-TRADE: find best yield trade ----
        val bestPreview = viewModel.bestLitOpportunity()
        if (snapshot != null && (analyses.isNotEmpty() || mtfReports.isNotEmpty())) {
            SayvisCard(containerColor = SayvisCyan.copy(alpha = 0.06f), borderColor = SayvisCyan.copy(alpha = 0.35f)) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SayvisGold, modifier = Modifier.padding(end = 6.dp).let { it })
                        Text(
                            text = if (LocalStrings.current.fa) "🧠 معاملهٔ هوشمند — بیشترین بازدهی"
                            else "🧠 Smart Trade — Highest Yield",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (LocalStrings.current.fa)
                            "هوش سایویس از آموزش‌ها، بک‌تست‌ها و همگرایی تایم‌فریم‌ها بهترین نقطهٔ ورود (RR≥۱:۳) را پیدا کرده و با تأیید شما مستقیم وارد می‌شود — در حساب دمو یا واقعی (Kill Switch فعال)."
                        else "SAYVIS AI learns from trainings, backtests & timeframe confluence, finds the best entry (RR≥1:3) and enters on your tap — demo or REAL (Kill Switch armed).",
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )
                    if (bestPreview != null) {
                        val (sym, an) = bestPreview
                        Text(
                            text = (if (LocalStrings.current.fa) "پیشنهاد هوشمند: " else "Smart pick: ") +
                                sym.labelFa + " " + an.plan.side.name + "  RR=1:" + "%.1f".format(an.plan.rr) +
                                "  ورود " + fmt(an.plan.entry),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGreenSuccess,
                            modifier = Modifier.testTag("lit_best_pick")
                        )
                        Text(text = an.plan.reasonFa, fontSize = 10.sp, color = SayvisSilverMuted)
                    } else {
                        Text(
                            text = if (LocalStrings.current.fa) "هنوز سیگنال مطمئن با RR≥۱:۳ یافت نشد — «به‌روزرسانی» یا «اسکن نقطهٔ ورود» را بزنید."
                            else "No confident signal with RR≥1:3 yet — tap Refresh or scan entries.",
                            fontSize = 10.5.sp,
                            color = SayvisAmberWarning
                        )
                    }
                    SayvisButton(
                        label = s.litAutoFindBest,
                        onClick = { viewModel.autoExecuteBestTrade() },
                        enabled = autoTrade,
                        tone = ButtonTone.GOLD,
                        modifier = Modifier.fillMaxWidth().testTag("lit_auto_best")
                    )
                    if (!autoTrade) {
                        Text(
                            text = if (LocalStrings.current.fa) "برای اجرای خودکار، «ترید خودکار» را پایین روشن کنید."
                            else "Enable auto-trade below to allow one-tap smart execution.",
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ---- LIT analysis per selected symbol ----
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
                        text = if (gatewayState.profile.executionMode == com.example.sayvis.settings.TradingExecutionMode.PAPER_SIMULATION) s.litRiskNotePaper else s.litRiskNote,
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
        // ============================= MULTI-TIMEFRAME ENTRY HUNT
        Spacer(modifier = Modifier.height(14.dp))
        SayvisSectionHeader(title = s.mtfTitle, icon = Icons.Default.QueryStats)
        SayvisCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = s.mtfHint, fontSize = 10.5.sp, color = SayvisSilverMuted)
                SayvisButton(
                    label = if (mtfBusy) s.mtfScanning else s.mtfScan,
                    onClick = { viewModel.runMtfScan() },
                    busy = mtfBusy,
                    tone = ButtonTone.PRIMARY,
                    modifier = Modifier.fillMaxWidth().testTag("mtf_scan")
                )
                mtfReports.forEach { report ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = report.symbolLabel,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilver
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            report.verdicts.forEach { v ->
                                val bg = when (v.side) {
                                    LitStrategyEngine.Side.LONG -> SayvisGreenSuccess.copy(alpha = 0.22f)
                                    LitStrategyEngine.Side.SHORT -> SayvisRedAlert.copy(alpha = 0.22f)
                                    LitStrategyEngine.Side.WAIT -> SayvisSurfaceVariant
                                }
                                val fg = when (v.side) {
                                    LitStrategyEngine.Side.LONG -> SayvisGreenSuccess
                                    LitStrategyEngine.Side.SHORT -> SayvisRedAlert
                                    LitStrategyEngine.Side.WAIT -> SayvisSilverMuted
                                }
                                Text(
                                    text = v.tf.labelFa + " " + v.side.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fg,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("mtf_tf_" + v.tf.name)
                                )
                            }
                        }
                        val d = report.decision
                        Text(
                            text = d.headline(true),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (d.isEntry) SayvisGreenSuccess else SayvisSilverMuted,
                            modifier = Modifier.testTag("mtf_decision")
                        )
                        if (d.agreeing.isNotEmpty()) {
                            Text(
                                text = s.mtfAgree + " " + d.agreeing.joinToString("، ") { it.labelFa },
                                fontSize = 10.5.sp,
                                color = SayvisSilver
                            )
                        }
                        if (d.opposing.isNotEmpty()) {
                            Text(
                                text = s.mtfOppose + " " + d.opposing.joinToString("، ") { it.labelFa },
                                fontSize = 10.5.sp,
                                color = SayvisRedAlert
                            )
                        }
                        report.plan?.let { plan ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SayvisSurfaceVariant)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = s.mtfPlanTitle +
                                        (report.executionTf?.let { " — " + it.labelFa } ?: ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SayvisGold
                                )
                                Text(
                                    text = "ورود: " + fmt(plan.entry) + "   |   حد ضرر: " + fmt(plan.stop),
                                    fontSize = 10.5.sp,
                                    color = SayvisSilver
                                )
                                Text(
                                    text = plan.targets.mapIndexed { i, t ->
                                        "TP" + (i + 1) + " (RR " + t.rr + "): " + fmt(t.price)
                                    }.joinToString("\n"),
                                    fontSize = 10.5.sp,
                                    color = SayvisSilver
                                )
                                SayvisButton(
                                    label = if (LocalStrings.current.fa) "اجرای این نقطهٔ ورود" else "Execute this entry",
                                    onClick = {
                                        val sym = MarketDataService.Symbol.XAUUSD
                                        viewModel.executeLitPlan(sym, plan)
                                    },
                                    enabled = autoTrade,
                                    tone = ButtonTone.GOLD,
                                    modifier = Modifier.fillMaxWidth().testTag("mtf_execute")
                                )
                            }
                        }
                    }
                }
                if (mtfReports.isEmpty() && !mtfBusy) {
                    Text(text = s.mtfNoData, fontSize = 10.5.sp, color = SayvisSilverMuted)
                }
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

private fun fmt(value: Double): String = when {
    value >= 100000 -> "%.0f".format(value)
    value >= 1000 -> "%.1f".format(value)
    else -> "%.4f".format(value)
}

