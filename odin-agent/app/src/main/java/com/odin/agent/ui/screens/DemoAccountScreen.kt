package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.DemoAccountManager
import com.odin.agent.trading.OdinTokenManager
import com.odin.agent.trading.RealMarketDataManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

/**
 * ODIN v1.0.27 - Demo Account Screen - Odin.trade
 * حساب دمو ویتاورس - باز کردن معامله واقعی با قیمت واقعی
 * SL/TP اتومات - کمیسیون 20% سود به صاحب نرم‌افزار
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoAccountScreen(isPersian: Boolean) {
    val tokenManager = remember { OdinTokenManager.getInstance() }
    val demo = remember { DemoAccountManager(tokenManager) }
    val market = remember { RealMarketDataManager() }
    var state by remember { mutableStateOf(demo.state.value) }
    var prices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var selectedSymbol by remember { mutableStateOf("EURUSD") }
    var selectedSide by remember { mutableStateOf(SignalSide.BUY) }
    var capital by remember { mutableStateOf(100f) }
    var selectedStrategy by remember { mutableStateOf(QuantStrategyType.MEAN_REVERSION) }
    var msg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        demo.state.collect { state = it }
    }
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val fetched = market.fetchRealPrices()
                val simple = fetched.mapValues { it.value.price }
                if (simple.isNotEmpty()) prices = simple
                demo.updatePrices(simple)
                demo.refresh(simple)
                state = demo.state.value
            } catch (_: Exception) { }
            delay(5000)
        }
    }

    val acc = state.account
    val strategyLocked = !tokenManager.isStrategyAllowed(selectedStrategy)

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------- حساب دمو ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinCyan), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "حساب دمو - معامله واقعی با پول مجازی" else "Demo Account - Real Trading Virtual Money", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "قیمت واقعی ویتاورس هر 5 ثانیه - اسپرد واقعی - SL/TP اتومات" else "Real Vittaverse prices every 5s - real spread - auto SL/TP", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (acc == null) {
                        Button(onClick = { demo.createDemoAccount(10000.0); state = demo.state.value }, colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.2f)), border = BorderStroke(1.dp, OdinCyan), shape = RoundedCornerShape(10.dp)) {
                            Text(if (isPersian) "ساخت حساب دمو 10,000 دلاری - سرور Vittaverse-Demo" else "Create 10,000$ Demo - Vittaverse-Demo", color = OdinCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("${"%,.2f".format(acc.balance)}$", fontSize = 17.sp, fontWeight = FontWeight.Black, color = OdinGreen); Text(if (isPersian) "بالانس" else "Balance", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${"%,.2f".format(acc.equity)}$", fontSize = 17.sp, fontWeight = FontWeight.Black, color = OdinGoldLight); Text(if (isPersian) "اکوییتی" else "Equity", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${"%,.0f".format(acc.marginUsed)}$", fontSize = 15.sp, fontWeight = FontWeight.Black, color = OdinCyan); Text(if (isPersian) "مارجین" else "Margin", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column(horizontalAlignment = Alignment.End) { Text("${"%,.2f".format(acc.freeMargin)}$", fontSize = 15.sp, fontWeight = FontWeight.Black, color = OdinSilver); Text(if (isPersian) "آزاد" else "Free", fontSize = 8.sp, color = OdinSilverMuted) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isPersian) "لاگین: ${acc.login} | سرور: ${acc.server} | اهرم 1:${acc.leverage} | ساخت: ${acc.jalali.take(12)}" else "Login: ${acc.login} | ${acc.server} | 1:${acc.leverage} | ${acc.gregorian.take(12)}", fontSize = 8.sp, color = OdinSilverMuted)
                    }
                }
            }
        }

        // ---------- فرم باز کردن معامله ----------
        if (acc != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isPersian) "باز کردن معامله" else "Open Position", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        // نماد
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("EURUSD", "BTCUSD", "XAUUSD", "GBPUSD").forEach { sym ->
                                FilterChip(selected = selectedSymbol == sym, onClick = { selectedSymbol = sym }, label = { Text(sym, fontSize = 9.sp, fontWeight = FontWeight.Bold) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF111111), selectedContainerColor = OdinGold.copy(alpha = 0.2f), selectedLabelColor = OdinGoldLight))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val livePx = prices[selectedSymbol]
                        Text(if (livePx != null) "$selectedSymbol = ${"%.5f".format(livePx)} (REAL ✓)" else "$selectedSymbol = ${if (isPersian) "دریافت قیمت..." else "fetching..."}", fontSize = 10.sp, color = if (livePx != null) OdinGreen else OdinSilverMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        // خرید/فروش
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = selectedSide == SignalSide.BUY, onClick = { selectedSide = SignalSide.BUY }, label = { Text(if (isPersian) "خرید BUY" else "BUY", fontSize = 10.sp, fontWeight = FontWeight.Black) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF111111), selectedContainerColor = OdinGreen.copy(alpha = 0.2f), selectedLabelColor = OdinGreen))
                            FilterChip(selected = selectedSide == SignalSide.SELL, onClick = { selectedSide = SignalSide.SELL }, label = { Text(if (isPersian) "فروش SELL" else "SELL", fontSize = 10.sp, fontWeight = FontWeight.Black) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF111111), selectedContainerColor = Color(0xFFFF5252).copy(alpha = 0.2f), selectedLabelColor = Color(0xFFFF5252)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // سرمایه
                        Text(if (isPersian) "سرمایه: ${capital.toInt()}$ (10 تا 1000)" else "Capital: ${capital.toInt()}$ (10-1000)", fontSize = 10.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                        Slider(value = capital, onValueChange = { capital = it }, valueRange = 10f..1000f, colors = SliderDefaults.colors(thumbColor = OdinGold, activeTrackColor = OdinGold))
                        // استراتژی با گیت
                        Text(if (isPersian) "استراتژی:" else "Strategy:", fontSize = 10.sp, color = OdinSilver, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(QuantStrategyType.MEAN_REVERSION, QuantStrategyType.TREND_FOLLOWING, QuantStrategyType.LIT_LIQUIDITY_INVERSION, QuantStrategyType.TV_80_PERCENT).forEach { st ->
                                val locked = !tokenManager.isStrategyAllowed(st)
                                FilterChip(selected = selectedStrategy == st, onClick = { selectedStrategy = st }, label = { Row(verticalAlignment = Alignment.CenterVertically) { if (locked) { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(9.dp)); Spacer(Modifier.width(2.dp)) }; Text(if (isPersian) st.labelFa.take(10) else st.labelEn.take(10), fontSize = 8.sp, fontWeight = FontWeight.Bold) } }, colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF111111), selectedContainerColor = OdinCyan.copy(alpha = 0.2f), selectedLabelColor = if (locked) Color(0xFFFF5252) else OdinCyan))
                            }
                        }
                        if (strategyLocked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isPersian) "🔒 قفل توکن: ${selectedStrategy.label(true)} نیاز به ${tokenManager.requiredStakeFor(selectedStrategy).toInt()} ODN استیک دارد - از صفحه توکن استیک کن" else "🔒 Token gate: ${selectedStrategy.labelEn} needs ${tokenManager.requiredStakeFor(selectedStrategy).toInt()} ODN staked", fontSize = 9.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val res = demo.openPosition(selectedSymbol, selectedSide, capital.toDouble(), selectedStrategy, prices[selectedSymbol])
                                msg = res.fold(
                                    onSuccess = { if (isPersian) "معامله باز شد: ${it.symbol} ${if (it.side == SignalSide.BUY) "خرید" else "فروش"} @${it.entryPrice}" else "Opened: ${it.symbol} ${it.side} @${it.entryPrice}" },
                                    onFailure = { (it.message ?: "error").split("|").last() }
                                )
                                state = demo.state.value
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (strategyLocked) Color(0xFF333333) else OdinGold.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, if (strategyLocked) Color(0xFF555555) else OdinGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isPersian) "باز کردن معامله $capital$ $selectedSymbol" else "Open $capital$ $selectedSymbol", color = if (strategyLocked) OdinSilverMuted else OdinGoldLight, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        if (msg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg, fontSize = 9.sp, color = OdinSilver, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---------- پوزیشن‌های باز ----------
            item {
                Text(if (isPersian) "پوزیشن‌های باز (${state.openPositions.size})" else "Open Positions (${state.openPositions.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            if (state.openPositions.isEmpty()) {
                item { Text(if (isPersian) "پوزیشن بازی نیست - معامله باز کن" else "No open positions", fontSize = 10.sp, color = OdinSilverMuted) }
            }
            items(state.openPositions) { pos ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (pos.floatingPnl >= 0) OdinGreen.copy(alpha = 0.5f) else Color(0xFFFF5252).copy(alpha = 0.5f)), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("${pos.symbol} ${if (pos.side == SignalSide.BUY) "▲ BUY" else "▼ SELL"} ${pos.lots} lots", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text(if (isPersian) "ورود ${"%.5f".format(pos.entryPrice)} ← الان ${"%.5f".format(pos.currentPrice)} | SL ${"%.5f".format(pos.slPrice)} | TP ${"%.5f".format(pos.tpPrice)}" else "Entry ${"%.5f".format(pos.entryPrice)} → ${"%.5f".format(pos.currentPrice)} | SL ${"%.5f".format(pos.slPrice)} | TP ${"%.5f".format(pos.tpPrice)}", fontSize = 8.sp, color = OdinSilverMuted)
                                Text("${pos.strategy.label(isPersian)} | ${if (isPersian) "باز:" else "Open:"} ${pos.openJalali.take(12)}", fontSize = 8.sp, color = OdinCyan)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${if (pos.floatingPnl >= 0) "+" else ""}${"%.2f".format(pos.floatingPnl)}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (pos.floatingPnl >= 0) OdinGreen else Color(0xFFFF5252))
                                TextButton(onClick = { demo.closePosition(pos.id, prices[pos.symbol]); state = demo.state.value }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB74D))) {
                                    Text(if (isPersian) "بستن" else "CLOSE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // ---------- آمار ----------
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinSilverDim.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isPersian) "آمار حساب دمو" else "Demo Stats", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("${state.stats.totalTrades}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinSilver); Text(if (isPersian) "ترید" else "Trades", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${"%.0f".format(state.stats.winrate)}%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinCyan); Text(if (isPersian) "وین‌ریت" else "Winrate", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column { Text("${if (state.stats.totalNetPnl >= 0) "+" else ""}${"%.2f".format(state.stats.totalNetPnl)}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (state.stats.totalNetPnl >= 0) OdinGreen else Color(0xFFFF5252)); Text(if (isPersian) "سود خالص" else "Net PnL", fontSize = 8.sp, color = OdinSilverMuted) }
                            Column(horizontalAlignment = Alignment.End) { Text("${"%.2f".format(state.stats.totalFeesToOwner)}$", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB74D)); Text(if (isPersian) "کمیسیون سوینکس 20%" else "Swinex 20% fees", fontSize = 8.sp, color = OdinSilverMuted) }
                        }
                    }
                }
            }

            // ---------- تاریخچه ----------
            item {
                Text(if (isPersian) "تاریخچه (${state.history.size})" else "History (${state.history.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            items(state.history.take(15)) { h ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${h.symbol} ${if (h.side == SignalSide.BUY) "BUY" else "SELL"} ${h.status}", fontSize = 9.sp, color = OdinSilver, fontWeight = FontWeight.Bold)
                        Text("${h.closeJalali.take(12)} | اسپرد ${"%.2f".format(h.spreadCostUsd)}$ | کمیسیون ${"%.2f".format(h.performanceFeeUsd)}$", fontSize = 8.sp, color = OdinSilverMuted)
                    }
                    Text("${if (h.netPnlUsd >= 0) "+" else ""}${"%.2f".format(h.netPnlUsd)}$", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (h.netPnlUsd >= 0) OdinGreen else Color(0xFFFF5252))
                }
            }

            item { Text(if (isPersian) "حساب دمو - پول مجازی - قیمت و اسپرد واقعی ویتاورس - کمیسیون 20% سود اتومات به صاحب نرم‌افزار" else "Demo - virtual money - real Vittaverse prices/spread - 20% profit fee auto to owner", fontSize = 8.sp, color = OdinSilverMuted, modifier = Modifier.padding(bottom = 10.dp)) }
        }
    }
}
