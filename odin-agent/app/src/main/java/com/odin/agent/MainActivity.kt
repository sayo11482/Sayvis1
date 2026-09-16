package com.odin.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

        setContent {
            OdinTheme {
                OdinApp()
            }
        }
    }
}

enum class OdinScreen(val titleEn: String, val titleFa: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", "داشبورد", Icons.Default.Dashboard),
    LIVE_CHART("Real Chart", "چارت واقعی", Icons.Default.ShowChart),
    BACKTEST_CONT("No Ban", "بدون ممنوعیت", Icons.Default.AllInclusive),
    SCANNER_ALARM("Real Scanner", "اسکنر واقعی", Icons.Default.NotificationImportant),
    AWARE("AWARE", "آگاه", Icons.Default.Psychology),
    MT5_REAL("MT5 Real", "MT5 واقعی", Icons.Default.AccountBalance),
    NOBITEX_REAL("Nobitex", "نوبیتکس", Icons.Default.CurrencyExchange),
    LIT_MONITOR("80% Monitor", "مانیتور 80%", Icons.Default.Radar),
    STRATEGIES("Strategies", "استراتژی‌ها", Icons.Default.AutoAwesome),
    GMAIL_NEWS("Gmail News", "اخبار جیمیل", Icons.Default.Email),
    SETTINGS("Settings", "تنظیمات", Icons.Default.Settings)
}

@Composable
fun OdinApp() {
    var currentScreen by remember { mutableStateOf(OdinScreen.DASHBOARD) }
    var isPersian by remember { mutableStateOf(true) }

    val riskManager = remember { RiskManager(initialCapital = 10000.0) }
    var riskStatus by remember { mutableStateOf(riskManager.getStatus()) }

    var enabledStrategies by remember {
        mutableStateOf(listOf(
            QuantStrategyType.TV_80_PERCENT,
            QuantStrategyType.LIT_LIQUIDITY_INVERSION,
            QuantStrategyType.TREND_FOLLOWING,
            QuantStrategyType.MEAN_REVERSION,
            QuantStrategyType.MOMENTUM_BREAKOUT
        ))
    }

    var currentRegime by remember { mutableStateOf(MarketRegime.TRENDING) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            currentRegime = MarketRegime.values().random()
            riskStatus = riskManager.getStatus()
        }
    }

    var selectedEntrySignal by remember { mutableStateOf<com.odin.agent.trading.EntrySignal?>(null) }

    Scaffold(
        containerColor = OdinDeepSpace,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF050505),
                contentColor = OdinSilver
            ) {
                // Show 6 main tabs: Dashboard, Real Chart, Scanner, MT5 Real, Nobitex, AWARE
                val mainTabs = listOf(
                    OdinScreen.DASHBOARD,
                    OdinScreen.LIVE_CHART,
                    OdinScreen.SCANNER_ALARM,
                    OdinScreen.MT5_REAL,
                    OdinScreen.NOBITEX_REAL,
                    OdinScreen.AWARE
                )
                mainTabs.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { 
                            Text(
                                text = if (isPersian) screen.titleFa else screen.titleEn,
                                maxLines = 1,
                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                            ) 
                        },
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
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(text = "odin metatrading", color = OdinGoldLight, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { isPersian = !isPersian }) {
                        Text(text = if (isPersian) "FA | EN" else "EN | FA", color = OdinGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = OdinSilver
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                OdinScreen.DASHBOARD -> DashboardScreen(
                    riskStatus = riskStatus,
                    currentRegime = currentRegime,
                    isPersian = isPersian,
                    onNavigateToStrategies = { currentScreen = OdinScreen.MT5_REAL },
                    onNavigateToBacktest = { currentScreen = OdinScreen.BACKTEST_CONT },
                    onNavigateToPaperTrade = { currentScreen = OdinScreen.LIVE_CHART },
                    onNavigateToGmailNews = { currentScreen = OdinScreen.GMAIL_NEWS }
                )
                OdinScreen.LIVE_CHART -> {
                    if (selectedEntrySignal != null) {
                        LiveChartScreen(
                            isPersian = isPersian,
                            initialSignal = selectedEntrySignal,
                            initialPrice = selectedEntrySignal?.price
                        )
                    } else {
                        LiveChartScreen(isPersian = isPersian)
                    }
                }
                OdinScreen.BACKTEST_CONT -> ContinuousBacktestScreen(isPersian = isPersian)
                OdinScreen.SCANNER_ALARM -> EntryScannerScreen(
                    isPersian = isPersian,
                    onSignalClick = { signal ->
                        selectedEntrySignal = signal
                        currentScreen = OdinScreen.LIVE_CHART
                    }
                )
                OdinScreen.MT5_REAL -> MT5SettingsScreen(isPersian = isPersian)
                OdinScreen.NOBITEX_REAL -> NobitexScreen(isPersian = isPersian)
                OdinScreen.AWARE -> AwareScreen(isPersian = isPersian)
                OdinScreen.LIT_MONITOR -> MultiSymbolScreen(isPersian = isPersian)
                OdinScreen.STRATEGIES -> StrategiesScreen(
                    isPersian = isPersian,
                    enabledStrategies = enabledStrategies,
                    onToggleStrategy = { type, enabled ->
                        enabledStrategies = if (enabled) {
                            enabledStrategies + type
                        } else {
                            enabledStrategies - type
                        }
                    }
                )
                OdinScreen.GMAIL_NEWS -> GmailNewsScreen(isPersian = isPersian)
                OdinScreen.SETTINGS -> SettingsScreen(isPersian = isPersian)
            }
        }
    }
}
