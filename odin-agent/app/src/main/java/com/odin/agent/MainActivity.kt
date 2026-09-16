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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    STRATEGIES("Strategies", "استراتژی‌ها", Icons.Default.Psychology),
    BACKTEST("Backtest", "بک‌تست", Icons.Default.Analytics),
    PAPER_TRADE("Paper Trade", "پیپر ترید", Icons.Default.PlayArrow),
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
            QuantStrategyType.TREND_FOLLOWING,
            QuantStrategyType.MEAN_REVERSION,
            QuantStrategyType.MOMENTUM_BREAKOUT
        ))
    }

    var currentRegime by remember { mutableStateOf(MarketRegime.TRENDING) }

    // Simulate regime changes
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            currentRegime = MarketRegime.values().random()
            riskStatus = riskManager.getStatus()
        }
    }

    Scaffold(
        containerColor = OdinDeepSpace,
        bottomBar = {
            NavigationBar(
                containerColor = OdinSurface,
                contentColor = OdinSilver
            ) {
                OdinScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(text = if (isPersian) screen.titleFa else screen.titleEn) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OdinCyan,
                            selectedTextColor = OdinCyan,
                            indicatorColor = OdinCyan.copy(alpha = 0.2f),
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
                    Row {
                        Text(text = "ODIN", color = OdinCyan, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "اودین ایجنت" else "AGENT", color = OdinGold, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { isPersian = !isPersian }) {
                        Text(text = if (isPersian) "FA" else "EN", color = OdinGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OdinSurface,
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
                    onNavigateToStrategies = { currentScreen = OdinScreen.STRATEGIES },
                    onNavigateToBacktest = { currentScreen = OdinScreen.BACKTEST },
                    onNavigateToPaperTrade = { currentScreen = OdinScreen.PAPER_TRADE }
                )
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
                OdinScreen.BACKTEST -> BacktestScreen(isPersian = isPersian)
                OdinScreen.PAPER_TRADE -> PaperTradeScreen(isPersian = isPersian, riskStatus = riskStatus)
                OdinScreen.SETTINGS -> SettingsScreen(isPersian = isPersian)
            }
        }
    }
}
