package com.odin.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.risk.RiskManager
import com.odin.agent.ui.screens.*
import com.odin.agent.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OdinTheme { OdinApp() } }
    }
}

enum class OdinScreen(val titleEn: String, val titleFa: String, val icon: ImageVector) {
    ARENA_AGENT("Arena AI", "ایجنت آرنا", Icons.Default.SmartToy),
    DASHBOARD("Dashboard", "داشبورد", Icons.Default.Dashboard),
    LIVE_CHART("Real Chart", "چارت واقعی", Icons.Default.ShowChart),
    SCANNER_ALARM("Scanner Vittaverse", "اسکنر ویتاورس", Icons.Default.NotificationImportant),
    TRADING_HUB("Vittaverse", "ویتاورس", Icons.Default.AccountBalance),
    BACKTEST("Backtest 10$", "بک‌تست 10$", Icons.Default.BarChart),
    OUTCOME("Outcome 10$", "برآیند 10$", Icons.Default.AccountBalanceWallet),
    PRO_ENGINE("Odin Pro", "اودین پرو", Icons.Default.Bolt),
    ODIN_TOKEN("ODIN Token", "توکن اودین", Icons.Default.Paid),
    DEMO_ACCOUNT("Demo Account", "حساب دمو", Icons.Default.CurrencyExchange),
    SYSTEM_AUDIT("System Audit", "تست کامل سیستم", Icons.Default.Science),
    SELF_UPGRADE("Self-Upgrade", "خود ارتقایی", Icons.Default.SystemUpdate),
    CAPABILITIES("Capabilities", "قابلیت‌ها", Icons.Default.Extension),
    AWARE("AWARE Security", "امنیت آگاه", Icons.Default.Security)
}

@Composable
fun OdinApp() {
    var currentScreen by remember { mutableStateOf(OdinScreen.ARENA_AGENT) }
    var isPersian by remember { mutableStateOf(true) }
    val riskManager = remember { RiskManager(initialCapital = 10000.0) }
    var riskStatus by remember { mutableStateOf(riskManager.getStatus()) }
    var currentRegime by remember { mutableStateOf(MarketRegime.TRENDING) }
    var regimeIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            val regimes = MarketRegime.values()
            regimeIndex = (regimeIndex + 1) % regimes.size
            currentRegime = regimes[regimeIndex % 2]
            riskStatus = riskManager.getStatus()
        }
    }
    var selectedEntrySignal by remember { mutableStateOf<com.odin.agent.trading.EntrySignal?>(null) }

    Scaffold(
        containerColor = OdinDeepSpace,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF050505), contentColor = OdinSilver) {
                val mainTabs = listOf(
                    OdinScreen.ARENA_AGENT,
                    OdinScreen.DASHBOARD,
                    OdinScreen.LIVE_CHART,
                    OdinScreen.SCANNER_ALARM,
                    OdinScreen.OUTCOME
                )
                mainTabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(text = if (isPersian) screen.titleFa else screen.titleEn, maxLines = 1, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OdinGoldLight,
                            selectedTextColor = OdinGoldLight,
                            indicatorColor = OdinGold.copy(alpha = 0.15f),
                            unselectedIconColor = OdinSilverMuted,
                            unselectedTextColor = OdinSilverMuted
                        )
                    )
                }
            }
        },
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "Odin.trade v1.0.28", color = OdinGoldLight, fontWeight = FontWeight.Black, fontSize = 13.sp) } },
                actions = {
                    IconButton(onClick = { isPersian = !isPersian }) { Text(text = if (isPersian) "FA | EN" else "EN | FA", color = OdinGold, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    IconButton(onClick = { currentScreen = OdinScreen.PRO_ENGINE }) { Icon(Icons.Default.Bolt, contentDescription = null, tint = if (currentScreen == OdinScreen.PRO_ENGINE) OdinGoldLight else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.ODIN_TOKEN }) { Icon(Icons.Default.Paid, contentDescription = null, tint = if (currentScreen == OdinScreen.ODIN_TOKEN) OdinGold else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.DEMO_ACCOUNT }) { Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = if (currentScreen == OdinScreen.DEMO_ACCOUNT) OdinCyan else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.SYSTEM_AUDIT }) { Icon(Icons.Default.Science, contentDescription = null, tint = if (currentScreen == OdinScreen.SYSTEM_AUDIT) OdinGreen else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.SELF_UPGRADE }) { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = if (currentScreen == OdinScreen.SELF_UPGRADE) OdinCyan else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.CAPABILITIES }) { Icon(Icons.Default.Extension, contentDescription = null, tint = if (currentScreen == OdinScreen.CAPABILITIES) OdinGold else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.TRADING_HUB }) { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = if (currentScreen == OdinScreen.TRADING_HUB) OdinGreen else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.AWARE }) { Icon(Icons.Default.Security, contentDescription = null, tint = if (currentScreen == OdinScreen.AWARE) OdinGreen else OdinSilverMuted, modifier = Modifier.size(16.dp)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505), titleContentColor = OdinSilver)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                OdinScreen.ARENA_AGENT -> ArenaAgentScreen(isPersian = isPersian)
                OdinScreen.DASHBOARD -> DashboardScreen(
                    riskStatus = riskStatus,
                    currentRegime = currentRegime,
                    isPersian = isPersian,
                    onNavigateToStrategies = { currentScreen = OdinScreen.TRADING_HUB },
                    onNavigateToBacktest = { currentScreen = OdinScreen.BACKTEST },
                    onNavigateToPaperTrade = { currentScreen = OdinScreen.LIVE_CHART },
                    onNavigateToGmailNews = { currentScreen = OdinScreen.AWARE }
                )
                OdinScreen.LIVE_CHART -> {
                    if (selectedEntrySignal != null) {
                        LiveChartScreen(isPersian = isPersian, initialSignal = selectedEntrySignal, initialPrice = selectedEntrySignal?.price)
                    } else {
                        LiveChartScreen(isPersian = isPersian)
                    }
                }
                OdinScreen.SCANNER_ALARM -> EntryScannerScreen(isPersian = isPersian, onSignalClick = { signal -> selectedEntrySignal = signal; currentScreen = OdinScreen.LIVE_CHART })
                OdinScreen.TRADING_HUB -> TradingHubScreen(isPersian = isPersian)
                OdinScreen.BACKTEST -> BacktestScreen(isPersian = isPersian)
                OdinScreen.OUTCOME -> InvestmentOutcomeScreen(isPersian = isPersian)
                OdinScreen.PRO_ENGINE -> ProTradingHubScreen(isPersian = isPersian)
                OdinScreen.ODIN_TOKEN -> OdinTokenScreen(isPersian = isPersian)
                OdinScreen.DEMO_ACCOUNT -> DemoAccountScreen(isPersian = isPersian)
                OdinScreen.SYSTEM_AUDIT -> SystemAuditScreen(isPersian = isPersian)
                OdinScreen.SELF_UPGRADE -> SelfUpgradeScreen(isPersian = isPersian)
                OdinScreen.CAPABILITIES -> CapabilitiesScreen(isPersian = isPersian, onNavigateToUpgrade = { currentScreen = OdinScreen.SELF_UPGRADE })
                OdinScreen.AWARE -> AwareScreen(isPersian = isPersian)
            }
        }
    }
}
