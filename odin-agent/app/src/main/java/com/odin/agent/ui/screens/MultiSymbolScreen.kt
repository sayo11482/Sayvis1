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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.trading.*
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay

/**
 * ODIN v1.0.24 - MultiSymbol - VITTAVERSE ONLY - قیمت هر لحظه نوسان واقعی - واحد تومان/تتر مشخص - اسپرد
 */

@Composable
fun MultiSymbolScreen(
    isPersian: Boolean,
    monitor: MultiSymbolMonitor = remember { MultiSymbolMonitor() },
    realDataManager: RealMarketDataManager = remember { RealMarketDataManager() }
) {
    var marketState by remember { mutableStateOf(realDataManager.state.value) }
    var analyses by remember { mutableStateOf<List<SymbolAnalysis>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            realDataManager.startPolling(1000L)
            monitor.startMonitoring()
            while (isScanning) {
                try {
                    marketState = realDataManager.state.value
                    monitor.updateWithRealPrices(marketState.prices)
                    analyses = monitor.state.value.symbols
                } catch (e: Exception) {
                    analyses = monitor.state.value.symbols
                }
                delay(1000)
            }
        } else {
            realDataManager.stopPolling()
            monitor.stopMonitoring()
        }
    }

    LaunchedEffect(Unit) {
        try {
            realDataManager.fetchRealPrices()
            marketState = realDataManager.state.value
            monitor.updateWithRealPrices(marketState.prices)
        } catch (e: Exception) {}
        analyses = monitor.state.value.symbols
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (isPersian) "ODIN × ویتاورس - ${marketState.prices.size} نماد زنده نوسان" else "ODIN × Vittaverse - ${marketState.prices.size} Live Fluctuating", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = if (isPersian) "ویتاورس https://vittaverse.com/fa/ - واحد تومان/تتر - اسپرد واقعی - نوسان هر لحظه" else "Vittaverse https://vittaverse.com/fa/ - Unit Toman/USDT - Spread REAL - Fluctuating", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { isScanning = !isScanning }, colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) OdinRed else OdinGreen), shape = RoundedCornerShape(10.dp)) {
                    Icon(imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isScanning) if (isPersian) "توقف" else "Stop" else if (isPersian) "شروع نوسان" else "Start Fluct", fontSize = 10.sp)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCardV24(label = if (isPersian) "نماد زنده ویتاورس" else "Live Vittaverse", value = "${marketState.prices.size}", color = OdinGreen, modifier = Modifier.weight(1f))
                StatCardV24(label = if (isPersian) "به‌روزرسانی" else "Updates", value = "${marketState.updateCount}", color = OdinGold, modifier = Modifier.weight(1f))
                StatCardV24(label = if (isPersian) "داده منتقل" else "Data Transferred", value = "${marketState.dataTransferred / 1024} KB", color = OdinCyan, modifier = Modifier.weight(1f))
                StatCardV24(label = if (isPersian) "واحد مشخص" else "Unit Specified", value = if (isPersian) "تومان/تتر" else "Toman/USDT", color = OdinGoldLight, modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "v1.0.24-vittaverse-real - فقط ویتاورس - قیمت لحظه‌ای نوسان واقعی - واحد مشخص - اسپرد" else "v1.0.24-vittaverse-real - Vittaverse ONLY - Fluctuating REAL - Unit Specified - Spread", fontWeight = FontWeight.Bold, color = OdinGold, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "• فقط بروکر ویتاورس https://vittaverse.com/fa/ - اسپرد واقعی ویتاورس محاسبه می‌شود\n• قیمت‌ها هر لحظه در نوسان واقعی - نه فقط متصل - داده ${marketState.dataTransferred} بایت منتقل شد - ${marketState.updateCount} به‌روزرسانی\n• واحد پولی مشخص: تومان برای ریالی (USDT/IRR) + تتر برای کریپتو/فارکس - هر نماد واحد مشخص دارد\n• اسکنر: انتخاب نماد + نمایش استراتژی بررسی شده + هزینه اسپرد تومان/تتر + سرمایه قابل تنظیم نه 10$ ثابت"
                        else "• Vittaverse ONLY https://vittaverse.com/fa/ - Vittaverse REAL spread calculated\n• Prices fluctuating REAL every moment - Not just connected - Data ${marketState.dataTransferred} bytes transferred - ${marketState.updateCount} updates\n• Unit specified: Toman for IRR (USDT/IRR) + USDT for crypto/forex - Each symbol unit specified\n• Scanner: Symbol select + strategy checked shown + spread cost Toman/USDT + capital adjustable not fixed 10$",
                        fontSize = 9.sp, color = OdinSilver, lineHeight = 11.sp
                    )
                }
            }
        }

        // نمایش قیمت‌های زنده ویتاورس با نوسان واقعی
        items(marketState.prices.values.sortedBy { it.symbol }.toList()) { realPrice ->
            SymbolRealCardV24(realPrice = realPrice, isPersian = isPersian)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCardV24(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 7.sp, color = OdinSilverMuted, maxLines = 1)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun SymbolRealCardV24(realPrice: RealPrice, isPersian: Boolean) {
    val symInfo = SymbolManager.find(realPrice.symbol)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = when (symInfo?.category) { SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.08f); SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.08f); SymbolCategory.METALS -> OdinGold.copy(alpha = 0.08f); else -> Color(0xFF0A0A0A) }),
        border = BorderStroke(1.dp, when (symInfo?.category) { SymbolCategory.FOREX_IRR -> OdinRed.copy(alpha = 0.3f); SymbolCategory.CRYPTO -> OdinCyan.copy(alpha = 0.3f); SymbolCategory.METALS -> OdinGold.copy(alpha = 0.3f); else -> OdinBorder }),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = realPrice.symbol, fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (realPrice.unit == "Toman") OdinRed.copy(alpha = 0.2f) else OdinCyan.copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text(text = realPrice.unitFa, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = if (realPrice.unit == "Toman") OdinRed else OdinCyan)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(OdinGold.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text(text = "Vittaverse", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (realPrice.unit == "Toman") String.format("%,.0f تومان", realPrice.price) else if (realPrice.price > 100) String.format("%,.0f USDT", realPrice.price) else String.format("%.4f", realPrice.price), fontWeight = FontWeight.Black, color = OdinGoldLight, fontSize = 11.sp)
                    Row {
                        Text(text = "${if (realPrice.changePercent >= 0) "+" else ""}${String.format("%.2f", realPrice.changePercent)}%", fontSize = 8.sp, color = if (realPrice.changePercent >= 0) OdinGreen else OdinRed, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isPersian) "نوسان واقعی" else "REAL Fluct", fontSize = 6.sp, color = OdinGreen)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = if (isPersian) "خرید/فروش ویتاورس واقعی" else "Bid/Ask Vittaverse REAL", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = if (realPrice.unit == "Toman") "${String.format("%,.0f", realPrice.bid)}/${String.format("%,.0f", realPrice.ask)} تومان" else "${String.format("%.4f", realPrice.bid)}/${String.format("%.4f", realPrice.ask)}", fontSize = 8.sp, color = OdinSilver, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                Column {
                    Text(text = if (isPersian) "اسپرد ویتاورس واقعی" else "Spread Vittaverse REAL", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = "${realPrice.spread} | ${if (realPrice.unit == "Toman") "${realPrice.spreadCostToman.toInt()} تومان" else "${String.format("%.2f", realPrice.spreadCostUSDT)} تتر"}", fontSize = 8.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = if (isPersian) "قیمت تومان + تتر" else "Price Toman+USDT", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = "${String.format("%,.0f", realPrice.priceToman)} تومان | ${String.format("%.2f", realPrice.priceUSDT)} تتر", fontSize = 7.sp, color = OdinCyan)
                }
                Column {
                    Text(text = if (isPersian) "منبع ویتاورس" else "Vittaverse Source", fontSize = 7.sp, color = OdinSilverMuted)
                    Text(text = realPrice.source.take(14), fontSize = 7.sp, color = OdinGreen)
                }
            }
        }
    }
}
