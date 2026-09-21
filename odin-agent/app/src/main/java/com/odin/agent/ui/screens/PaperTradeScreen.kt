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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.*
import com.odin.agent.mt5.MT5ConnectionManager
import com.odin.agent.ui.theme.*

@Composable
fun PaperTradeScreen(isPersian: Boolean, riskStatus: QuantRiskStatus) {
    val mt5Manager = remember { MT5ConnectionManager() }
    var mt5State by remember { mutableStateOf(mt5Manager.state.value) }

    LaunchedEffect(Unit) {
        while (true) {
            mt5State = mt5Manager.state.value
            kotlinx.coroutines.delay(2000)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(OdinDeepSpace).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (isPersian) "پیپر ترید زنده - ۱۰۰٪ واقعی" else "Live Paper Trading - 100% REAL", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = if (isPersian) "حساب واقعی MT5 ویتاورس - فقط معامله واقعی" else "REAL MT5 Vittaverse account - REAL trading only", fontSize = 11.sp, color = OdinGreen)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (mt5State.isConnected) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(text = if (mt5State.isConnected) if (isPersian) "● متصل واقعی" else "● CONNECTED REAL" else if (isPersian) "○ قطع" else "○ DISCONNECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (mt5State.isConnected) OdinGreen else OdinRed)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = if (isPersian) "وضعیت واقعی" else "Status REAL", fontSize = 10.sp, color = OdinSilverMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (mt5State.isConnected) OdinGreen else OdinSilverMuted))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (mt5State.isConnected) if (isPersian) "متصل واقعی MT5" else "Connected REAL MT5" else if (isPersian) "قطع - لاگین کنید" else "Disconnected - Login", fontWeight = FontWeight.Bold, color = if (mt5State.isConnected) OdinGreen else OdinSilverMuted, fontSize = 12.sp)
                        }
                        if (mt5State.isConnected) {
                            Text(text = "Balance REAL $${String.format("%.2f", mt5State.balance)}", fontSize = 10.sp, color = OdinGold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = if (isPersian) "سرمایه واقعی" else "Capital REAL", fontSize = 10.sp, color = OdinSilverMuted)
                        Text(text = "$${if (mt5State.isConnected) mt5State.equity.toInt() else riskStatus.currentCapital.toInt()}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${if (riskStatus.totalPnl >= 0) "+" else ""}${riskStatus.totalPnlPercent.toInt()}% REAL", fontSize = 11.sp, color = if (riskStatus.totalPnl >= 0) OdinGreen else OdinRed)
                    }
                }
            }
        }

        if (mt5State.positions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.5f)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = if (isPersian) "پوزیشن واقعی باز وجود ندارد - فقط واقعی" else "No REAL open positions - REAL only", color = OdinSilverMuted, fontSize = 12.sp)
                            Text(text = if (isPersian) "در مرکز معاملات به MT5 واقعی لاگین کنید" else "Login to REAL MT5 in Trading Hub", color = OdinSilverMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = if (isPersian) "هیچ شبیه‌سازی نمایش داده نمی‌شود" else "No simulation shown", color = OdinSilverDim, fontSize = 8.sp)
                        }
                    }
                }
            }
        } else {
            items(mt5State.positions.size) { idx ->
                val pos = mt5State.positions[idx]
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (pos.type.lowercase().contains("buy")) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(text = pos.type.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (pos.type.lowercase().contains("buy")) OdinGreen else OdinRed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = pos.symbol, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Text(text = "${if (pos.profit >= 0) "+" else ""}$${String.format("%.2f", pos.profit)} REAL", fontWeight = FontWeight.Bold, color = if (pos.profit >= 0) OdinGreen else OdinRed, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Entry REAL: ${pos.openPrice}", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = "Now REAL: ${pos.currentPrice}", fontSize = 10.sp, color = OdinSilver)
                            Text(text = "Vol REAL: ${pos.volume}", fontSize = 10.sp, color = OdinSilverMuted)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "نکات پیپر ترید واقعی" else "Paper Trading Tips REAL", fontWeight = FontWeight.Bold, color = OdinCyan, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian) "• حداقل ۱ ماه پیپر ترید واقعی قبل از لایو - واقعی\n• کمیسیون و اسلیپیج واقعی لحاظ شده - واقعی\n• Kill-switch در ۳% DD روزانه - واقعی\n• لاگ کامل واقعی در backend - بدون فیک"
                        else "• At least 1 month REAL paper before live - REAL\n• Commission & slippage REAL included - REAL\n• Kill-switch at 3% daily DD REAL\n• Full REAL logs in backend - No fake",
                        fontSize = 11.sp, color = OdinSilverMuted, lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
