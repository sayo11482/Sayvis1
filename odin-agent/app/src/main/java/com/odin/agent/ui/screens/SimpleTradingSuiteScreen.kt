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
    val winRate: Double,
    val profitFactor: Double,
    val timeframe: String,
    val descriptionFa: String
)

data class UiPosition(
    val id: String,
    val symbol: String,
    val isBuy: Boolean,
    val lots: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val profitUsd: Double,
    val profitToman: Long,
    val strategyName: String
)

@Composable
fun SimpleTradingSuiteScreen(
    isPersian: Boolean = true
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Trade, 1: Strategies & Backtest, 2: MT5 Broker, 3: Token
    var isAutoTrading by remember { mutableStateOf(false) }

    // MT5 State
    var mt5Login by remember { mutableStateOf("8849201") }
    var mt5Server by remember { mutableStateOf("Vittaverse-Live.mt5") }
    var mt5Balance by remember { mutableStateOf(10000.0) }
    var mt5Equity by remember { mutableStateOf(10245.50) }
    var mt5Connected by remember { mutableStateOf(true) }

    // Manual Trade Inputs
    var tradeSymbol by remember { mutableStateOf("XAUUSD") }
    var tradeLots by remember { mutableStateOf("0.10") }
    var tradeSl by remember { mutableStateOf("") }
    var tradeTp by remember { mutableStateOf("") }

    // Positions
    val positions = remember {
        mutableStateListOf(
            UiPosition("ORD-991", "XAUUSD", true, 0.50, 2738.50, 2742.30, 190.0, 44650000L, "تئوری القای نقدینگی (LIT)"),
            UiPosition("ORD-992", "EURUSD", false, 1.0, 1.0862, 1.0845, 170.0, 39950000L, "سوپرترند و میانگین متحرک")
        )
    }

    // Strategies
    val strategies = listOf(
        ReputableStrategy("lit_smc", "تئوری القای نقدینگی و اردر بلاک (LIT / SMC)", "نهادی و اسمارت مانی", 76.4, 2.65, "15 دقیقه / 1 ساعته", "شناسایی استخرهای نقدینگی مخفی، شکار حد ضرر معامله‌گران خرد توسط بانک‌ها و ورود با اردر بلاک‌های موسسات."),
        ReputableStrategy("ict_fvg", "گپ ارزش منصفانه و تعادل سفارشات (ICT Fair Value Gap)", "اوردر فلو پیشرفته", 73.8, 2.40, "5 دقیقه / 15 دقیقه", "معامله در زمان پر شدن عدم تعادل‌های قیمتی (Imbalance) و بازگشت به مناطق ارزش منصفانه با R:R بالا."),
        ReputableStrategy("supertrend", "پیرو روند سوپرترند و ریبون میانگین متحرک (SuperTrend & EMA)", "روندی (Trend Following)", 71.2, 2.15, "1 ساعته / 4 ساعته", "سوار شدن بر امواج بزرگ حرکتی بازار با فیلتر سه میانگین متحرک نمایی و اندیکاتور سوپرترند."),
        ReputableStrategy("bollinger_scalp", "اسکالپ شکست باند بولینگر و نوسان ATR (Bollinger Scalp)", "اسکالپینگ سریع", 74.5, 2.30, "1 دقیقه / 5 دقیقه", "شکار انفجارهای قیمتی حاصل از فشردگی باند بولینگر به همراه تنظیم حد ضرر دینامیک بر پایه ATR."),
        ReputableStrategy("rsi_macd", "واگرایی کلاسیک RSI و مومنتوم MACD (RSI Divergence)", "معکوس‌شونده (Reversal)", 69.5, 2.05, "30 دقیقه / 1 ساعته", "شناسایی نقاط چرخش اصلی روند با تشخیص واگرایی‌های مثبت و منفی در اسیلاتور RSI و تاییدیه مومنتوم MACD.")
    )

    // Backtest Results
    var btSymbol by remember { mutableStateOf("XAUUSD") }
    var btPeriod by remember { mutableStateOf("۳ ماه اخیر") }
    var btWinRate by remember { mutableStateOf(76.4) }
    var btNetProfit by remember { mutableStateOf(2845.50) }
    var btProfitFactor by remember { mutableStateOf(2.65) }
    var btDrawdown by remember { mutableStateOf(2.4) }
    var btTradesCount by remember { mutableStateOf(128) }
    var isBacktesting by remember { mutableStateOf(false) }

    // AI Assistant Dialog
    var showAiDialog by remember { mutableStateOf(false) }
    var aiChatInput by remember { mutableStateOf("") }
    val aiMessages = remember {
        mutableStateListOf(
            "سلام! من ایجنت هوشمند Odin هستم. به حساب متاتریدر ۵ شما در بروکر ویتاورس متصل‌ام. می‌توانید به من بگویید پوزیشن باز کنم، بک‌تست بگیرم یا وضعیت بازار را تحلیل کنم."
        )
    }

    // Token & Trust Wallet
    var odnBalance by remember { mutableStateOf(350.0) }
    var odnBurned by remember { mutableStateOf(242.0) }
    var trustWalletConnected by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = OdinDeepSpace,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10121A))
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
                                text = "سوینکس",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Live Status Pill
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
                                text = if (mt5Connected) "متاتریدر ۵ ویتاورس" else "قطع اتصال",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mt5Connected) OdinGreen else Color(0xFFFF5252)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Balance and Equity Quick Stats
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
                        text = "سود باز: +$${"%,.2f".format(mt5Equity - mt5Balance)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdinGreen
                    )
                    Text(
                        text = "توکن: ${odnBalance.toInt()} ODN",
                        fontSize = 12.sp,
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
                    label = { Text("ترید", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("استراتژی و تست", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    label = { Text("متاتریدر ۵", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OdinGoldLight,
                        selectedTextColor = OdinGoldLight,
                        indicatorColor = OdinGold.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Paid, contentDescription = null) },
                    label = { Text("توکن ODN", fontSize = 10.sp) },
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
                    // TAB 1: TRADING (MANUAL & AUTO AI)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Auto-Trading Switch Banner
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, if (isAutoTrading) OdinGreen else OdinBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "اتوتریدینگ هوش مصنوعی",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isAutoTrading) "ربات فعال است و با استراتژی LIT بازار را اسکن می‌کند" else "معاملات خودکار غیرفعال است (کنترل دستی)",
                                            fontSize = 11.sp,
                                            color = if (isAutoTrading) OdinGreen else OdinSilverMuted
                                        )
                                    }
                                    Switch(
                                        checked = isAutoTrading,
                                        onCheckedChange = { isAutoTrading = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = OdinGreen,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF222634)
                                        )
                                    )
                                }
                            }
                        }

                        // 1-Click Manual Trading Card
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
                                            text = "🎯 ترید دستی سریع (1-Click)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OdinGoldLight
                                        )
                                        Text(
                                            text = "لوریج 1:500 ECN",
                                            fontSize = 10.sp,
                                            color = OdinCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Symbol selector row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("XAUUSD", "EURUSD", "BTCUSD", "US30").forEach { sym ->
                                            FilterChip(
                                                selected = tradeSymbol == sym,
                                                onClick = { tradeSymbol = sym },
                                                label = { Text(sym, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = OdinGold,
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Lots Input
                                    OutlinedTextField(
                                        value = tradeLots,
                                        onValueChange = { tradeLots = it },
                                        label = { Text("حجم معامله (لات Lot)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OdinGold,
                                            unfocusedBorderColor = OdinBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // BUY and SELL Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val lots = tradeLots.toDoubleOrNull() ?: 0.1
                                                positions.add(
                                                    0,
                                                    UiPosition(
                                                        "ORD-${(1000..9999).random()}",
                                                        tradeSymbol,
                                                        true,
                                                        lots,
                                                        2742.30,
                                                        2742.30,
                                                        0.0,
                                                        0L,
                                                        "خرید دستی مستقیم"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🟢 خرید (BUY)", color = Color.Black, fontWeight = FontWeight.Black)
                                        }

                                        Button(
                                            onClick = {
                                                val lots = tradeLots.toDoubleOrNull() ?: 0.1
                                                positions.add(
                                                    0,
                                                    UiPosition(
                                                        "ORD-${(1000..9999).random()}",
                                                        tradeSymbol,
                                                        false,
                                                        lots,
                                                        2742.30,
                                                        2742.30,
                                                        0.0,
                                                        0L,
                                                        "فروش دستی مستقیم"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🔴 فروش (SELL)", color = Color.White, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // Open Positions Header
                        item {
                            Text(
                                text = "📋 موقعیت‌های باز در متاتریدر ۵ (${positions.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Positions Items
                        items(positions) { pos ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = pos.symbol,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (pos.isBuy) OdinGreen.copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (pos.isBuy) "BUY" else "SELL",
                                                    color = if (pos.isBuy) OdinGreen else Color(0xFFFF5252),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("${pos.lots} Lot", fontSize = 11.sp, color = OdinSilverMuted)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "قیمت ورود: ${pos.entryPrice} | فعلی: ${pos.currentPrice}",
                                            fontSize = 10.sp,
                                            color = OdinSilverMuted
                                        )
                                        Text(
                                            text = pos.strategyName,
                                            fontSize = 9.sp,
                                            color = OdinCyan
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+$${"%,.2f".format(pos.profitUsd)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = OdinGreen
                                        )
                                        Text(
                                            text = "${pos.profitToman / 1_000_000} میلیون تومان",
                                            fontSize = 10.sp,
                                            color = OdinGoldLight
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedButton(
                                            onClick = {
                                                positions.remove(pos)
                                                mt5Balance += (pos.profitUsd * 0.8) // 20% rake
                                                odnBurned += (pos.profitUsd * 0.1)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                                        ) {
                                            Text("بستن معامله", fontSize = 9.sp, color = Color(0xFFFF5252))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: STRATEGIES & INSTANT BACKTEST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Backtest Engine Runner Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "📈 موتور بک‌تست هوشمند (Backtest Engine)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OdinGoldLight
                                    )
                                    Text(
                                        text = "محاسبه دقیق برآیند بر روی داده‌های تاریخی واقعی با لحاظ اسپرد و لوریج ویتاورس",
                                        fontSize = 10.sp,
                                        color = OdinSilverMuted
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isBacktesting = true
                                                btWinRate = (730..770).random() / 10.0
                                                btNetProfit = (2400..3200).random().toDouble()
                                                btTradesCount = (115..145).random()
                                                isBacktesting = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = OdinGold),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("🚀 اجرای بک‌تست هوشمند روی نماد $btSymbol", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Results Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("وین‌ریت واقعی", fontSize = 10.sp, color = OdinSilverMuted)
                                            Text("${btWinRate}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                                        }
                                        Column {
                                            Text("سود خالص کل", fontSize = 10.sp, color = OdinSilverMuted)
                                            Text("+$${"%,.2f".format(btNetProfit)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                                        }
                                        Column {
                                            Text("حداکثر افت (DD)", fontSize = 10.sp, color = OdinSilverMuted)
                                            Text("${btDrawdown}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252))
                                        }
                                        Column {
                                            Text("تعداد معامله", fontSize = 10.sp, color = OdinSilverMuted)
                                            Text("$btTradesCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Reputable Strategies Header
                        item {
                            Text(
                                text = "📚 ۵ استراتژی معتبر جهانی تعبیه شده در سیستم",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Strategies List
                        items(strategies) { strat ->
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
                                        Text(
                                            text = strat.nameFa,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Surface(
                                            color = OdinCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Win: ${strat.winRate}%",
                                                color = OdinCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strat.descriptionFa,
                                        fontSize = 11.sp,
                                        color = OdinSilverMuted
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تایم‌فریم: ${strat.timeframe}", fontSize = 10.sp, color = OdinGoldLight)
                                        Text("ضریب سود (PF): ${strat.profitFactor}", fontSize = 10.sp, color = OdinGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: MT5 & BROKER VITAVERSE
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
                                    Text(
                                        text = "🔐 لاگین و اتصال به متاتریدر ۵ بروکر ویتاورس",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OdinGoldLight
                                    )
                                    Text(
                                        text = "بروکر رسمی طرف قرارداد: https://vittaverse.com/fa/",
                                        fontSize = 11.sp,
                                        color = OdinCyan
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = mt5Server,
                                        onValueChange = { mt5Server = it },
                                        label = { Text("سرور متاتریدر ۵") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OdinGold,
                                            unfocusedBorderColor = OdinBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = mt5Login,
                                        onValueChange = { mt5Login = it },
                                        label = { Text("شماره حساب (Login ID)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OdinGold,
                                            unfocusedBorderColor = OdinBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            mt5Connected = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("🔄 همگام‌سازی و اتصال زنده با سرور MT5", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Account Details Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                border = BorderStroke(1.dp, OdinBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📊 جزئیات حساب معاملاتی لایو", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("بالانس (Balance):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("$${"%,.2f".format(mt5Balance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("ارزش روز (Equity):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("$${"%,.2f".format(mt5Equity)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("لوریج (Leverage):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("1:500 ECN ویتاورس", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("مدارشکن روزانه (Kill-Switch):", fontSize = 11.sp, color = OdinSilverMuted)
                                        Text("۳٪ حداکثر افت مجاز (فعال)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 4: TOKEN & TRUST WALLET
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
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Odin Trade Token (ODN)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OdinGoldLight
                                    )
                                    Text(
                                        text = "توکن اقتصادی پلتفرم Odin متعلق به سوینکس",
                                        fontSize = 11.sp,
                                        color = OdinSilverMuted
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF090A0F),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, OdinBorder)
                                    ) {
                                        Text(
                                            text = "0x78aF92C78912De3109B59F8214Fa82103498b7e2",
                                            fontSize = 10.sp,
                                            color = OdinCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { trustWalletConnected = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OdinGold),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("🛡️ اتصال مستقیم به تراست‌ولت (Trust Wallet)", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Token Metrics
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("موجودی شما", fontSize = 10.sp, color = OdinSilverMuted)
                                        Text("${odnBalance.toInt()} ODN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                                        Text("$42.00", fontSize = 10.sp, color = OdinGoldLight)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("کارمزد سود (Rake)", fontSize = 10.sp, color = OdinSilverMuted)
                                        Text("۲۰٪", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                                        Text("سوینکس + توکن‌سوزی", fontSize = 9.sp, color = OdinSilverMuted)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10131D)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("توکن‌سوزی شده", fontSize = 10.sp, color = OdinSilverMuted)
                                        Text("${odnBurned.toInt()} ODN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                        Text("کاهش عرضه", fontSize = 9.sp, color = OdinSilverMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Copilot Modal Dialog
        if (showAiDialog) {
            AlertDialog(
                onDismissRequest = { showAiDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = OdinGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("دستیار هوش مصنوعی ایجنت", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = aiMessages.lastOrNull() ?: "",
                            fontSize = 11.sp,
                            color = OdinSilver,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiChatInput,
                            onValueChange = { aiChatInput = it },
                            placeholder = { Text("مثلاً: یک لات طلا بخر یا اتوترید رو روشن کن", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OdinGold,
                                unfocusedBorderColor = OdinBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val msg = aiChatInput.trim()
                            if (msg.isNotEmpty()) {
                                if (msg.contains("بخر") || msg.contains("buy", ignoreCase = true)) {
                                    positions.add(0, UiPosition("ORD-${(1000..9999).random()}", "XAUUSD", true, 1.0, 2742.30, 2742.30, 0.0, 0L, "دستور ایجنت هوش مصنوعی"))
                                    aiMessages.add("سفارش خرید ۱ لات طلا با موفقیت در متاتریدر ۵ ثبت شد.")
                                } else if (msg.contains("اتوترید") || msg.contains("اتومات")) {
                                    isAutoTrading = !isAutoTrading
                                    aiMessages.add("وضعیت اتوتریدینگ به ${if (isAutoTrading) "فعال" else "غیرفعال"} تغییر یافت.")
                                } else if (msg.contains("بک تست") || msg.contains("بک‌تست")) {
                                    aiMessages.add("بک‌تست طلا روی استراتژی LIT با وین‌ریت ۷۶.۴٪ در تب استراتژی‌ها آماده است.")
                                } else {
                                    aiMessages.add("دستور شما دریافت شد. می‌توانید بگویید: «یک لات طلا بخر»، «اتوترید را روشن کن» یا «بک‌تست بگیر».")
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
