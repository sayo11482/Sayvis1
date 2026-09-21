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
    BACKTEST("Backtest Vittaverse", "بک‌تست ویتاورس", Icons.Default.BarChart),
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
                    OdinScreen.TRADING_HUB
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
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "ODIN × ویتاورس v1.0.25 Arena AI", color = OdinGoldLight, fontWeight = FontWeight.Black, fontSize = 12.sp) } },
                actions = {
                    IconButton(onClick = { isPersian = !isPersian }) { Text(text = if (isPersian) "FA | EN" else "EN | FA", color = OdinGold, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    IconButton(onClick = { currentScreen = OdinScreen.AWARE }) { Icon(Icons.Default.Security, contentDescription = null, tint = if (currentScreen == OdinScreen.AWARE) OdinGreen else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { currentScreen = OdinScreen.BACKTEST }) { Icon(Icons.Default.BarChart, contentDescription = null, tint = if (currentScreen == OdinScreen.BACKTEST) OdinGold else OdinSilverMuted, modifier = Modifier.size(18.dp)) }
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
                OdinScreen.AWARE -> AwareScreen(isPersian = isPersian)
            }
        }
    }
}
