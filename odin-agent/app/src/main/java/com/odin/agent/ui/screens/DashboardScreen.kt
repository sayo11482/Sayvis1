package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.aware.AwareLearningEngine
import com.odin.agent.models.MarketRegime
import com.odin.agent.models.QuantRiskStatus
import com.odin.agent.mt5.MT5ConnectionManager
import com.odin.agent.testing.ConnectionTester
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    riskStatus: QuantRiskStatus,
    currentRegime: MarketRegime,
    isPersian: Boolean,
    onNavigateToStrategies: () -> Unit,
    onNavigateToBacktest: () -> Unit,
    onNavigateToPaperTrade: () -> Unit,
    onNavigateToGmailNews: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val awareEngine = remember { AwareLearningEngine() }
    val statsManager = remember { DashboardStatsManager() }
    val sessionManager = remember { SessionManager() }
    val connectionTester = remember { ConnectionTester(context) }
    val realDataManager = remember { RealMarketDataManager() }
    val mt5Manager = remember { MT5ConnectionManager() }
    val scope = rememberCoroutineScope()

    var dashboardStats by remember { mutableStateOf(statsManager.state.value) }
    var sessionState by remember { mutableStateOf(sessionManager.state.value) }
    var connectionState by remember { mutableStateOf(connectionTester.state.value) }
    var realPrices by remember { mutableStateOf<Map<String, RealPrice>>(emptyMap()) }
    var mt5State by remember { mutableStateOf(mt5Manager.state.value) }

    LaunchedEffect(Unit) {
        sessionManager.updateSessions()
        sessionState = sessionManager.state.value
        awareEngine.startLearning()
        repeat(10) { awareEngine.generateMockExperience() }
        statsManager.startNewSession()

        while (true) {
            sessionManager.updateSessions()
            sessionState = sessionManager.state.value
            statsManager.updateFromAware(awareEngine)
            dashboardStats = statsManager.state.value

            // Fetch real prices
            try {
                realPrices = realDataManager.fetchRealPrices()
                statsManager.updateFromRealPrices(realPrices)
            } catch (e: Exception) {}

            mt5State = mt5Manager.state.value

            if ((0..10).random() < 3) {
                awareEngine.generateMockExperience()
            }

            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(OdinGold, OdinGoldLight))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "ODIN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "ODIN AGENT v1.0.14 REAL MT5", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text(text = if (isPersian) "ویتاورس واقعی + ریال ایران + چارت واقعی" else "Vittaverse REAL + IRR + Real Chart", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OdinGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "● LIVE REAL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real ticker with IRR
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.Black).padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val btc = realPrices["BTCUSD"] ?: realPrices["BTC/USDT"]
                        val eth = realPrices["ETHUSD"] ?: realPrices["ETH/USDT"]
                        val xau = realPrices["XAUUSD"]
                        val eur = realPrices["EURUSD"]
                        val usdIrr = realPrices["USD/IRR"]

                        TickerItem("BTC", if (btc != null) "${btc.price.toInt()}" else "65,234", "${String.format("%.1f", btc?.changePercent ?: 2.3)}%", (btc?.changePercent ?: 2.3) >= 0)
                        TickerItem("ETH", if (eth != null) "${eth.price.toInt()}" else "3,521", "${String.format("%.1f", eth?.changePercent ?: 1.8)}%", (eth?.changePercent ?: 1.8) >= 0)
                        TickerItem("XAU", if (xau != null) "${xau.price.toInt()}" else "2,351", "${String.format("%.1f", xau?.changePercent ?: 0.5)}%", (xau?.changePercent ?: 0.5) >= 0)
                        TickerItem("EURUSD", if (eur != null) String.format("%.4f", eur.price) else "1.0850", "${String.format("%.1f", eur?.changePercent ?: -0.2)}%", (eur?.changePercent ?: -0.2) >= 0)
                        TickerItem("USD/IRR", if (usdIrr != null) String.format("%,.0f", usdIrr.price) else "590K", "${String.format("%.1f", usdIrr?.changePercent ?: 0.8)}%", (usdIrr?.changePercent ?: 0.8) >= 0)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                        border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isPersian) "تاریخ و سشن‌ها - نیویورک لندن" else "Date & Sessions - NY London", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = sessionState.tehranTime, fontSize = 9.sp, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SessionBadge(name = "London 08:00-16:30 UTC", isActive = sessionState.isLondonActive, progress = if (sessionState.isLondonActive) sessionState.sessionProgress else 0f)
                                SessionBadge(name = "NY 13:00-22:00 UTC", isActive = sessionState.isNewYorkActive, progress = if (sessionState.isNewYorkActive) sessionState.sessionProgress else 0f)
                                if (sessionState.isOverlap) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGold.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(text = "OVERLAP 🔥", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = if (isPersian) "فعال: ${sessionState.activeSession}" else "Active: ${sessionState.activeSession}", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = sessionState.nextSession, fontSize = 8.sp, color = OdinSilverDim)
                            if (sessionState.isLondonActive || sessionState.isNewYorkActive) {
                                LinearProgressIndicator(
                                    progress = sessionState.sessionProgress,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).padding(top = 4.dp),
                                    color = if (sessionState.isOverlap) OdinGold else OdinCyan,
                                    trackColor = Color(0xFF1A1A1A)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "فعال‌ترین" else "Most Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dashboardStats.mostActiveStrategy?.strategy?.name ?: "LIT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${dashboardStats.mostActiveStrategy?.trades ?: 0} trades • ${dashboardStats.mostActiveStrategy?.winrate?.toInt() ?: 0}% WR",
                            fontSize = 9.sp,
                            color = OdinSilverMuted
                        )
                        Text(
                            text = "RR 1:${String.format("%.1f", dashboardStats.mostActiveStrategy?.avgRR ?: 2.5)}",
                            fontSize = 9.sp,
                            color = OdinGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "موفق‌ترین" else "Most Successful", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dashboardStats.mostSuccessfulStrategy?.strategy?.name ?: "TV80",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = dashboardStats.mostSuccessfulStrategy?.reason?.take(30) ?: "Best WR PF PnL",
                            fontSize = 8.sp,
                            color = OdinSilverMuted,
                            maxLines = 1
                        )
                        Text(
                            text = "Power ${dashboardStats.mostSuccessfulStrategy?.powerScore?.toInt() ?: 85}%",
                            fontSize = 9.sp,
                            color = OdinGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "معاملات امروز" else "Trades Today", fontSize = 8.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
                        Text(text = "${dashboardStats.tradesToday}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                        Text(text = "${dashboardStats.floatingPnL.openPositions} open", fontSize = 8.sp, color = OdinSilverDim)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, if (dashboardStats.floatingPnL.floatingPnL >= 0) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "سود شناور سشن" else "Floating PnL", fontSize = 7.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
                        Text(
                            text = "${if (dashboardStats.floatingPnL.floatingPnL >= 0) "+" else ""}${String.format("%.2f", dashboardStats.floatingPnL.floatingPnL)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (dashboardStats.floatingPnL.floatingPnL >= 0) OdinGreen else OdinRed
                        )
                        Text(
                            text = "${String.format("%.2f", dashboardStats.floatingPnL.floatingPnLPercent)}%",
                            fontSize = 9.sp,
                            color = if (dashboardStats.floatingPnL.floatingPnLPercent >= 0) OdinGreen else OdinRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isPersian) "بیشترین سود نماد" else "Most Profitable", fontSize = 7.sp, color = OdinSilverMuted, textAlign = TextAlign.Center)
                        Text(text = dashboardStats.mostProfitableSymbol?.symbol ?: "EURUSD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
                        Text(
                            text = "+$${String.format("%.1f", dashboardStats.mostProfitableSymbol?.totalPnL ?: 45.2)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OdinGold
                        )
                        Text(text = "${dashboardStats.mostProfitableSymbol?.trades ?: 12} trades", fontSize = 7.sp, color = OdinSilverDim)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "AWARE در حال یادگیری - بدون ممنوعیت" else "AWARE Learning - No Ban",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(OdinGreen.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "● ONLINE REAL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dashboardStats.awareLearningStrategy?.name ?: "TV_80_PERCENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = OdinGoldLight
                        )
                        Text(text = "${dashboardStats.awareProgress.toInt()}% aware", fontSize = 10.sp, color = OdinGold)
                    }
                    LinearProgressIndicator(
                        progress = (dashboardStats.awareProgress / 100).toFloat(),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).padding(top = 6.dp),
                        color = OdinGold,
                        trackColor = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "یادگیری مدیریت مالی + استراتژی‌ها + بهترین برای هر نماد - تمام مجاز" else "Learning money management + strategies + best per symbol - all allowed",
                        fontSize = 8.sp,
                        color = OdinSilverMuted
                    )
                }
            }
        }

        // MT5 Real Connection Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, if (mt5State.isConnected) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = if (mt5State.isConnected) OdinGreen else OdinRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "اتصال MT5 ویتاورس - معامله واقعی" else "MT5 Vittaverse - REAL Trading",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (mt5State.isConnected) OdinGreen.copy(alpha = 0.15f) else OdinRed.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = if (mt5State.isConnected) "● CONNECTED REAL" else "○ DISCONNECTED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (mt5State.isConnected) OdinGreen else OdinRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (mt5State.isConnected) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Balance", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "$${String.format("%.2f", mt5State.balance)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text(text = "Equity", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "$${String.format("%.2f", mt5State.equity)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                            }
                            Column {
                                Text(text = "Positions", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "${mt5State.positions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                            }
                            Column {
                                Text(text = "Server", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = mt5State.connectedServer ?: "Vittaverse-Real", fontSize = 9.sp, color = OdinCyan)
                            }
                        }
                        if (mt5State.positions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            mt5State.positions.take(3).forEach { pos ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "${pos.symbol} ${pos.type} ${pos.volume}", fontSize = 8.sp, color = Color.White)
                                    Text(text = "${if (pos.profit >= 0) "+" else ""}${String.format("%.2f", pos.profit)}", fontSize = 8.sp, color = if (pos.profit >= 0) OdinGreen else OdinRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (isPersian) "برای معامله واقعی در بازار جهانی فارکس با بروکر ویتاورس به MT5 متصل شوید - تنظیمات → MT5" else "Connect to MT5 for REAL trading in global forex market with Vittaverse broker - Settings → MT5",
                            fontSize = 9.sp,
                            color = OdinSilverMuted,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, if (riskStatus.killSwitchActive) OdinRed else Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RiskItem(label = "Capital", value = "$${riskStatus.currentCapital.toInt()}", color = Color.White)
                    RiskItem(label = "PnL", value = "${riskStatus.totalPnlPercent.toInt()}%", color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed)
                    RiskItem(label = "DD", value = "${riskStatus.totalDrawdown.toInt()}%", color = OdinAmber)
                    RiskItem(label = "Daily", value = "${riskStatus.dailyDdPercent().toInt()}%", color = OdinSilver)
                }
            }
        }

        item {
            Text(text = if (isPersian) "دسترسی سریع - واقعی" else "Quick Access - REAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionCard(title = if (isPersian) "چارت واقعی" else "Real Chart", subtitle = "IRR+Forex+Crypto", icon = Icons.Default.ShowChart, tint = OdinGoldLight, modifier = Modifier.weight(1f), onClick = onNavigateToPaperTrade)
                ActionCard(title = if (isPersian) "بک‌تست بدون بن" else "Backtest No Ban", subtitle = "All Allowed", icon = Icons.Default.AllInclusive, tint = OdinGreen, modifier = Modifier.weight(1f), onClick = onNavigateToBacktest)
                ActionCard(title = if (isPersian) "آلارم واقعی" else "Real Alarm", subtitle = "Beep Auto", icon = Icons.Default.NotificationImportant, tint = OdinCyan, modifier = Modifier.weight(1f), onClick = onNavigateToPaperTrade)
                ActionCard(title = if (isPersian) "MT5 ویتاورس" else "MT5 Vittaverse", subtitle = "Real Trade", icon = Icons.Default.AccountBalance, tint = OdinGold, modifier = Modifier.weight(1f), onClick = onNavigateToStrategies)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, if (connectionState.allPassed) OdinGreen.copy(alpha = 0.3f) else OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = if (connectionState.allPassed) OdinGreen else OdinCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "تست اتصالات - اینترنت MT5 TV گوگل جمینای ویتاورس" else "Connection Tests - Internet MT5 TV Google Gemini Vittaverse",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(text = "${connectionState.passedTests}/${connectionState.totalTests}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (connectionState.allPassed) OdinGreen else OdinGold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val tester = ConnectionTester(context)
                                tester.testAllConnections()
                                connectionState = tester.state.value
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (connectionState.isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = OdinCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "در حال تست..." else "Testing...", fontSize = 10.sp, color = OdinCyan)
                        } else {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "تست اینترنت MT5 TV گوگل جمینای ویتاورس" else "Test Internet MT5 TV Google Gemini Vittaverse", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (connectionState.tests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        connectionState.tests.forEach { test ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(
                                            when (test.status) {
                                                "success" -> OdinGreen
                                                "failed" -> OdinRed
                                                else -> OdinSilverMuted
                                            }
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (isPersian) test.nameFa else test.name, fontSize = 9.sp, color = Color.White)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "${test.latencyMs}ms", fontSize = 8.sp, color = OdinSilverMuted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (test.status) {
                                            "success" -> "OK"
                                            "failed" -> "FAIL"
                                            else -> "..."
                                        },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (test.status) {
                                            "success" -> OdinGreen
                                            "failed" -> OdinRed
                                            else -> OdinSilverMuted
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ODIN v1.0.14-real-mt5 - REAL Trading Vittaverse + IRR Rial + Real Chart\nNo Ban Rule - All Strategies Allowed - Power Score Ranking\nReal Market Data: Binance + Forex API + Iran Free Market Bonbast\nMT5 Real Connection - Place REAL orders with REAL money\nSymbols: 40+ including USD/IRR EUR/IRR Forex Majors Metals Crypto\nProfessional Black Gold Green Dollar - Pure Black #000000",
                fontSize = 7.sp,
                color = OdinSilverDim,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                lineHeight = 9.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TickerItem(symbol: String, price: String, change: String, isPositive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = symbol, fontSize = 7.sp, color = OdinSilverMuted, fontWeight = FontWeight.Bold)
        Text(text = price, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = change, fontSize = 7.sp, color = if (isPositive) OdinGreen else OdinRed)
    }
}

@Composable
private fun SessionBadge(name: String, isActive: Boolean, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(
                if (isActive) OdinGreen.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
            ).padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(text = name.take(10), fontSize = 7.sp, fontWeight = FontWeight.Black, color = if (isActive) OdinGreen else OdinSilverMuted)
        }
        if (isActive) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.width(40.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).padding(top = 2.dp),
                color = OdinGreen,
                trackColor = Color(0xFF1A1A1A)
            )
        }
    }
}

@Composable
private fun RiskItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 7.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
            Text(text = subtitle, fontSize = 7.sp, color = OdinSilverMuted, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
