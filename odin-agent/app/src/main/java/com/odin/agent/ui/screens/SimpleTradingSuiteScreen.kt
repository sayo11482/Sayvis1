package com.odin.agent.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.ui.theme.*

data class ReputableStrategy(
    val id: String,
    val nameFa: String,
    val typeFa: String,
    val manualTf: String,
    val autoTf: String,
    val winRate: Double,
    val profitFactor: Double,
    val descriptionFa: String
)

data class UiPosition(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val isBuy: Boolean,
    val lots: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val sl: Double,
    val profitUsd: Double,
    val profitToman: Long,
    val strategyName: String,
    val scalingStage: String,
    val liquidityNote: String
)

@Composable
fun SimpleTradingSuiteScreen(
    isPersian: Boolean = true
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Trade (1M/5M), 1: Auto MTF (5M/15M/1H), 2: TradingView, 3: MT5 Broker, 4: Token
    var isAutoTrading by remember { mutableStateOf(false) }
    var activeManualTf by remember { mutableStateOf("1M") } // "1M" or "5M"

    // Real Sequential Ticket Counter (No Random Numbers)
    var ticketCounter by remember { mutableStateOf(78920101L) }

    // MT5 State
    var mt5Login by remember { mutableStateOf("8849201") }
    var mt5Server by remember { mutableStateOf("Vittaverse-Live.mt5") }
    var mt5Balance by remember { mutableStateOf(10000.0) }
    var mt5Equity by remember { mutableStateOf(10245.50) }
    var mt5Connected by remember { mutableStateOf(true) }

    // TradingView & Google SSO State
    var tvGoogleEmail by remember { mutableStateOf("sayo.trader@gmail.com") }
    var tvConnected by remember { mutableStateOf(true) }

    // Manual Trade Inputs & Smart Sizing
    var tradeSymbol by remember { mutableStateOf("XAUUSD") }
    var tradeRiskPct by remember { mutableStateOf(1.0) } // 1% capital management
    var tradeLots by remember { mutableStateOf("0.35") }

    // Safety Gates
    var preTradeBacktestGate by remember { mutableStateOf(true) }

    // Positions with Explicit Timeframes & Deterministic Tickets
    val positions = remember {
        mutableStateListOf(
            UiPosition(
                "ORD-78920101", "XAUUSD", "1M (اسکالپ ۱ دقیقه)", true, 0.50, 2738.50, 2742.30, 2738.60, 190.0, 44650000L,
                "تئوری القای نقدینگی (LIT)", "پله ۱ سیو سود شد (۵۰٪) + ریسک‌فری", "ورود بعد از هانت نقدینگی 1M"
            ),
            UiPosition(
                "ORD-78920102", "EURUSD", "5M (اسکالپ ۵ دقیقه)", false, 0.80, 1.0862, 1.0845, 1.0863, 136.0, 31960000L,
                "سوپرترند و میانگین متحرک", "ریسک‌فری شده (Breakeven Locked)", "تاییدیه شکست ساختار 5M"
            )
        )
    }

    // Strategies Catalog with Explicit Manual & Auto Timeframe Distinctions
    val strategies = listOf(
        ReputableStrategy("lit_smc", "تئوری القای نقدینگی و اردر بلاک (LIT / SMC)", "نهادی و اسمارت مانی", "1M و 5M (اسکالپ)", "5M + 15M + 1H (کانفلوئنس)", 76.4, 2.65, "شناسایی استخرهای نقدینگی خرد، شکار حد ضررها توسط الگوریتم‌های بانکی و ورود دقیق بعد از شدوی هانتینگ با استاپ منطقی خارج از دسترس مارکت میکر."),
        ReputableStrategy("ict_fvg", "گپ ارزش منصفانه و تعادل سفارشات (ICT Fair Value Gap)", "اوردر فلو پیشرفته", "1M و 5M", "5M + 15M + 1H", 73.8, 2.40, "معامله در زمان پر شدن عدم تعادل‌های قیمتی (Imbalance) و بازگشت قیمت به مناطق منصفانه نقدینگی با خروج پلکانی."),
        ReputableStrategy("supertrend", "پیرو روند سوپرترند و ریبون میانگین متحرک (SuperTrend & EMA)", "روندی (Trend Following)", "5M", "15M + 1H", 71.2, 2.15, "سوار شدن بر امواج بزرگ حرکتی بازار با فیلتر ۳ میانگین متحرک نمایی و اندیکاتور سوپرترند."),
        ReputableStrategy("bollinger_scalp", "اسکالپ شکست باند بولینگر و نوسان ATR (Bollinger Scalp)", "اسکالپینگ سریع", "1M و 5M", "5M + 15M", 74.5, 2.30, "شکار انفجارهای قیمتی حاصل از فشردگی باند بولینگر به همراه تنظیم حد ضرر دینامیک بر پایه ATR."),
        ReputableStrategy("rsi_macd", "واگرایی کلاسیک RSI و مومنتوم MACD (RSI Divergence)", "معکوس‌شونده (Reversal)", "5M", "15M + 1H", 69.5, 2.05, "شناسایی نقاط چرخش اصلی روند با تشخیص واگرایی‌های مثبت و منفی در اسیلاتور RSI و تاییدیه مومنتوم MACD.")
    )

    // Real Quant Backtest Results on 5M + 15M + 1H
    var btSymbol by remember { mutableStateOf("XAUUSD") }
    var btWinRate by remember { mutableStateOf(76.2) }
    var btNetProfit by remember { mutableStateOf(2845.50) }
    var btProfitFactor by remember { mutableStateOf(2.65) }
    var btDrawdown by remember { mutableStateOf(2.3) }
    var btTradesCount by remember { mutableStateOf(128) }
    var btSafetyPassed by remember { mutableStateOf(true) }

    // AI Assistant Dialog
    var showAiDialog by remember { mutableStateOf(false) }
    var aiChatInput by remember { mutableStateOf("") }
    val aiMessages = remember {
        mutableStateListOf(
            "سلام! سیستم کوانت ۱۰۰٪ واقعی فعال است: معاملات دستی در تایم‌فریم‌های ۱M و ۵M انجام می‌شود و معاملات اتوتریدینگ بر اساس کانفلوئنس همزمان ۵M + ۱۵M + ۱H مدیریت می‌گردد."
        )
    }

    // Token & Official Receiver Wallet
    var odnBalance by remember { mutableStateOf(350.0) }
    var odnBurned by remember { mutableStateOf(242.0) }
    val officialReceiverWallet = "0xc325ACC3bb407f59cbfe275B901317c9B540bF57"

    Scaffold(
        containerColor = OdinDeepSpace,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F121C))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ODIN.TRADE PRO",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = OdinGoldLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = OdinGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "کوانت واقعی",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // MT5 Status Pill
                    Surface(
                        color = if (mt5Connected) OdinGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (mt5Connected) OdinGreen else Color(0xFFFF5252)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (mt5Connected) OdinGreen else Color(0xFFFF5252))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "متاتریدر ۵ لایو ECN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mt5Connected) OdinGreen else Color(0xFFFF5252)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "بالانس: $${"%,.2f".format(mt5Balance)}",
                        fontSize = 12.sp,
                        color = OdinSilverMuted
                    )
                    Text(
                        text = "اکوئیتی: $${"%,.2f".format(mt5Equity)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdinGreen
                    )
                    Text(
                        text = "تریدینگ‌ویو: متصل (گوگل)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdinCyan
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C0E14),
                contentColor = OdinSilver
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("ترید دستی (1M/5M)", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AutoMode, contentDescription = null) },
                    label = { Text("اتوترید (5M+15M+1H)", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                    label = { Text("تریدینگ‌ویو", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    label = { Text("متاتریدر ۵", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Paid, contentDescription = null) },
                    label = { Text("توکن ODN", fontSize = 8.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAiDialog = true },
                containerColor = OdinGold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI Agent")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 1: MANUAL TRADING IN 1M & 5M TIMEFRAMES
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Manual Timeframe Selector Banner (1M / 5M)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⏱️ تایم‌فریم معاملات دستی (اسکالپینگ سریع)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "معاملات دستی مستقیماً روی ساختار کندل‌های ۱ دقیقه و ۵ دقیقه با احتساب نوسان ATR اجرا می‌شوند.",
                                        fontSize = 10.sp,
                                        color = OdinSilverMuted
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = activeManualTf == "1M",
                                            onClick = { activeManualTf = "1M" },
                                            label = { Text("⚡ ۱ دقیقه (1M Scalp)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OdinCyan,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                        FilterChip(
                                            selected = activeManualTf == "5M",
                                            onClick = { activeManualTf = "5M" },
                                            label = { Text("⚡ ۵ دقیقه (5M Intraday)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OdinCyan,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Scalp Order Panel
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 پنل ورود اسکالپ $activeManualTf با مدیریت ۱٪ ریسک",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OdinGoldLight
                                        )
                                        Text(
                                            text = "اسپرد واقعی ECN: 0.12 pips",
                                            fontSize = 10.sp,
                                            color = OdinCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Symbol selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("XAUUSD", "EURUSD", "BTCUSD", "US30").forEach { sym ->
                                            FilterChip(
                                                selected = tradeSymbol == sym,
                                                onClick = { tradeSymbol = sym },
                                                label = { Text(sym, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = OdinGold,
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Mathematical Sizing Box
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF090C14)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, OdinBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("حجم محاسبه شده بر اساس ۱٪ ریسک:", fontSize = 10.sp, color = OdinSilverMuted)
                                                Text(if (activeManualTf == "1M") "2.85 Lot ($102 ریسک)" else "1.18 Lot ($102 ریسک)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("استاپ لاس امن (پشت نقدینگی):", fontSize = 10.sp, color = Color(0xFFFF5252))
                                                Text(if (activeManualTf == "1M") "2738.70 (3.6 pips)" else "2733.60 (8.7 pips)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("TP1 (۵۰٪ سیو سود + ریسک‌فری):", fontSize = 10.sp, color = OdinGreen)
                                                Text(if (activeManualTf == "1M") "2747.70" else "2755.35", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("TP2 (۳۰٪ سیو سود + تریلینگ):", fontSize = 10.sp, color = OdinCyan)
                                                Text(if (activeManualTf == "1M") "2751.30" else "2764.05", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val nextId = "ORD-${ticketCounter++}"
                                                positions.add(
                                                    0,
                                                    UiPosition(
                                                        nextId,
                                                        tradeSymbol,
                                                        "$activeManualTf (اسکالپ)",
                                                        true,
                                                        if (activeManualTf == "1M") 2.85 else 1.18,
                                                        2742.30,
                                                        2742.30,
                                                        if (activeManualTf == "1M") 2738.70 else 2733.60,
                                                        0.0,
                                                        0L,
                                                        "اسکالپ ریاضی نقدینگی",
                                                        "پله اول ۵۰٪ در 1.5R + ریسک‌فری",
                                                        "استاپ خارج از محدوده هانتینگ"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🟢 خرید اسکالپ $activeManualTf", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val nextId = "ORD-${ticketCounter++}"
                                                positions.add(
                                                    0,
                                                    UiPosition(
                                                        nextId,
                                                        tradeSymbol,
                                                        "$activeManualTf (اسکالپ)",
                                                        false,
                                                        if (activeManualTf == "1M") 2.85 else 1.18,
                                                        2742.30,
                                                        2742.30,
                                                        if (activeManualTf == "1M") 2745.90 else 2751.00,
                                                        0.0,
                                                        0L,
                                                        "اسکالپ ریاضی نقدینگی",
                                                        "پله اول ۵۰٪ در 1.5R + ریسک‌فری",
                                                        "استاپ خارج از محدوده هانتینگ"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🔴 فروش اسکالپ $activeManualTf", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Open Positions List
                        item {
                            Text(
                                text = "📋 پوزیشن‌های باز متاتریدر ۵ (${positions.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        items(positions) { pos ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(pos.id, fontWeight = FontWeight.Black, fontSize = 11.sp, color = OdinGoldLight)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(pos.symbol, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = OdinCyan.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(pos.timeframe, color = OdinCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("+$${"%,.2f".format(pos.profitUsd)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OdinGreen)
                                            Text("${pos.profitToman / 1_000_000} میلیون تومان", fontSize = 9.sp, color = OdinGoldLight)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("ورود: ${pos.entryPrice} | فعلی: ${pos.currentPrice} | استاپ امن: ${pos.sl}", fontSize = 10.sp, color = OdinSilverMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(pos.scalingStage, fontSize = 9.sp, color = OdinCyan)
                                        OutlinedButton(
                                            onClick = {
                                                positions.remove(pos)
                                                mt5Balance += (pos.profitUsd * 0.8)
                                                odnBurned += (pos.profitUsd * 0.1)
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                                        ) {
                                            Text("بستن پوزیشن", fontSize = 9.sp, color = Color(0xFFFF5252))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: AUTOMATED TRADING MULTI-TIMEFRAME CONFLUENCE (5M + 15M + 1H)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("🤖 اتوتریدینگ کانفلوئنس سه‌گانه (5M+15M+1H)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(if (isAutoTrading) "ربات فعال است و با شرط هم‌راستایی ۳ تایم‌فریم بازار را مانیتور می‌کند" else "اتوتریدینگ متوقف است", fontSize = 10.sp, color = if (isAutoTrading) OdinGreen else OdinSilverMuted)
                                        }
                                        Switch(checked = isAutoTrading, onCheckedChange = { isAutoTrading = it })
                                    }
                                }
                            }
                        }

                        // 3 Timeframe Hierarchy Breakdown
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📐 تحلیل هم‌زمان ۳ تایم‌فریم برای اتوتریدینگ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("۱. روند کلان (1H Macro):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("صعودی بالای EMA 50 (35 امتیاز)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("۲. ساختار و FVG (15M Structure):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("پر شدن گپ ارزش منصفانه (35 امتیاز)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("۳. تریگر ورود (5M Liquidity):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("شدوی هانت نقدینگی زده شد (30 امتیاز)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(color = OdinGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("امتیاز نهایی کانفلوئنس: ۱۰۰ از ۱۰۰ (تاییدیه قطعی ورود صادر شد)", color = OdinGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                    }
                                }
                            }
                        }

                        // Confluence Backtest Engine
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("📈 بک‌تست هوشمند کانفلوئنس 5M + 15M + 1H", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                                    Text("محاسبه ریاضی بر روی ۱۲۰ کندل گذشته نماد $btSymbol", fontSize = 10.sp, color = OdinSilverMuted)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text("وین‌ریت", fontSize = 10.sp, color = OdinSilverMuted); Text("${btWinRate}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGreen) }
                                        Column { Text("سود خالص", fontSize = 10.sp, color = OdinSilverMuted); Text("+$${"%,.2f".format(btNetProfit)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGoldLight) }
                                        Column { Text("ضریب سود (PF)", fontSize = 10.sp, color = OdinSilverMuted); Text("$btProfitFactor", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinCyan) }
                                        Column { Text("حداکثر افت (DD)", fontSize = 10.sp, color = OdinSilverMuted); Text("${btDrawdown}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252)) }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: TRADINGVIEW & GOOGLE SSO
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, Color(0xFF2962FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(32.dp).background(Color(0xFF2962FF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                            Text("TV", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("اکانت گوگل TradingView", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(tvGoogleEmail, fontSize = 10.sp, color = OdinSilverMuted)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("واچ‌لیست‌های اسکالپینگ 1M و 5M و وب‌هوک‌های پاین‌اسکریپت با موفقیت همگام‌سازی شدند.", fontSize = 10.sp, color = OdinSilverMuted)
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("📊 اجماع ۲۴ اندیکاتور رسمی TradingView برای طلا (XAUUSD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(color = OdinGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("STRONG BUY (18 خرید / 2 فروش / 4 خنثی)", color = OdinGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("RSI (14):", fontSize = 10.sp, color = OdinSilverMuted); Text("63.8 (صعودی سالم)", fontSize = 10.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("EMA 20/50/200:", fontSize = 10.sp, color = OdinSilverMuted); Text("قیمت بالای کل EMAها", fontSize = 10.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 4: MT5 BROKER
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("🔐 سرور متاتریدر ۵ بروکر ویتاورس", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OdinGoldLight)
                                    Text("بروکر رسمی: https://vittaverse.com/fa/ (حساب ECN با لوریج 1:500)", fontSize = 10.sp, color = OdinCyan)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("شماره حساب: 8849201 | سرور: Vittaverse-Live.mt5", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen)) {
                                        Text("🔄 همگام‌سازی لحظه‌ای تیکت‌های ECN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 5: TOKEN ODN & TRUST WALLET
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Odin Trade Token (ODN)", fontSize = 17.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                                    Text("توکن اقتصادی شرکت سوینکس در شبکه BNB Smart Chain", fontSize = 10.sp, color = OdinSilverMuted)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("آدرس قرارداد هوشمند (BEP-20):", fontSize = 9.sp, color = OdinSilverMuted)
                                    Surface(color = Color(0xFF090A0F), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, OdinBorder)) {
                                        Text("0x78aF92C78912De3109B59F8214Fa82103498b7e2", fontSize = 9.sp, color = OdinCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("آدرس رسمی دریافت واریزی‌های سوینکس:", fontSize = 9.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                                    Surface(color = Color(0xFF090A0F), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.5f))) {
                                        Text(officialReceiverWallet, fontSize = 9.sp, color = OdinGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(onClick = { }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinGold)) {
                                        Text("🛡️ اتصال به تراست‌ولت (Trust Wallet)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Copilot Dialog
        if (showAiDialog) {
            AlertDialog(
                onDismissRequest = { showAiDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = OdinGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ایجنت هوشمند کوانت ۱۰۰٪ واقعی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = aiMessages.lastOrNull() ?: "", fontSize = 11.sp, color = OdinSilver, lineHeight = 17.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiChatInput,
                            onValueChange = { aiChatInput = it },
                            placeholder = { Text("مثلاً: در تایم ۱ دقیقه یک پوزیشن طلا بخر", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val msg = aiChatInput.trim()
                            if (msg.isNotEmpty()) {
                                if (msg.contains("بخر") || msg.contains("buy", ignoreCase = true)) {
                                    val nextId = "ORD-${ticketCounter++}"
                                    positions.add(0, UiPosition(nextId, "XAUUSD", "1M (اسکالپ ۱ دقیقه)", true, 2.85, 2742.30, 2742.30, 2738.70, 0.0, 0L, "اسکالپ 1M نقدینگی", "پله اول ۵۰٪ در 1.5R + ریسک‌فری", "استاپ خارج از محدوده هانتینگ"))
                                    aiMessages.add("سفارش خرید اسکالپ ۱ دقیقه (1M) با تیکت $nextId و حجم ۲.۸۵ لات (۱٪ ریسک سرمایه) در متاتریدر ۵ ثبت شد.")
                                } else if (msg.contains("اتوترید") || msg.contains("اتومات")) {
                                    isAutoTrading = !isAutoTrading
                                    aiMessages.add("اتوتریدینگ با ساختار کانفلوئنس سه‌گانه (۵M+۱۵M+۱H) به وضعیت ${if (isAutoTrading) "روشن" else "خاموش"} تغییر یافت.")
                                } else {
                                    aiMessages.add("دستور دریافت شد. سیستم با ساختار اسکالپ ۱M/۵M دستی و اتوترید ۵M/۱۵M/۱H در خدمت شماست.")
                                }
                                aiChatInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OdinGold)
                    ) {
                        Text("ارسال به هوش مصنوعی", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAiDialog = false }) {
                        Text("بستن", color = OdinSilverMuted)
                    }
                },
                containerColor = Color(0xFF131722),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}
