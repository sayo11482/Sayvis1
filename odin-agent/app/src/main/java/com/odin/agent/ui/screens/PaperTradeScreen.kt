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
import com.odin.agent.models.*
import com.odin.agent.ui.theme.*
import kotlin.random.Random

@Composable
fun PaperTradeScreen(
    isPersian: Boolean,
    riskStatus: QuantRiskStatus
) {
    var isRunning by remember { mutableStateOf(false) }
    var positions by remember { mutableStateOf(listOf<QuantPosition>()) }

    // Mock positions
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                kotlinx.coroutines.delay(3000)
                if (positions.size < 3 && Random.nextBoolean()) {
                    val symbol = listOf("BTC/USDT", "ETH/USDT", "EURUSD").random()
                    val side = if (Random.nextBoolean()) SignalSide.BUY else SignalSide.SELL
                    val price = when(symbol) {
                        "BTC/USDT" -> 65000.0 + Random.nextDouble()*1000
                        "ETH/USDT" -> 3500.0 + Random.nextDouble()*100
                        else -> 1.0850 + Random.nextDouble()*0.01
                    }
                    positions = positions + QuantPosition(
                        id = "pos_${System.currentTimeMillis()}",
                        symbol = symbol,
                        strategy = QuantStrategyType.values().random(),
                        side = side,
                        entryPrice = price,
                        currentPrice = price + (if (side==SignalSide.BUY) 1 else -1)*Random.nextDouble()*50,
                        slPrice = price - (if (side==SignalSide.BUY) 1 else -1)*100,
                        tpPrice = price + (if (side==SignalSide.BUY) 1 else -1)*150,
                        size = Random.nextDouble()*0.5 + 0.1,
                        pnl = Random.nextDouble()*200 - 50,
                        pnlPercent = Random.nextDouble()*4 - 1,
                        entryTime = System.currentTimeMillis() - Random.nextLong(3600000),
                        confidence = 0.6 + Random.nextDouble()*0.3
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPersian) "پیپر ترید زنده" else "Live Paper Trading",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPersian) "شبیه‌سازی لایو بدون پول واقعی" else "Simulated live without real money",
                        fontSize = 11.sp,
                        color = OdinSilverMuted
                    )
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) OdinRed else OdinGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) {
                            if (isPersian) "توقف" else "Stop"
                        } else {
                            if (isPersian) "شروع" else "Start"
                        },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = if (isPersian) "وضعیت" else "Status", fontSize = 10.sp, color = OdinSilverMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isRunning) OdinGreen else OdinSilverMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) {
                                    if (isPersian) "در حال اجرا" else "Running"
                                } else {
                                    if (isPersian) "متوقف" else "Stopped"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) OdinGreen else OdinSilverMuted
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Capital", fontSize = 10.sp, color = OdinSilverMuted)
                        Text(text = "$${riskStatus.currentCapital.toInt()}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${if (riskStatus.totalPnl>=0) "+" else ""}${riskStatus.totalPnlPercent.toInt()}%", fontSize = 11.sp, color = if (riskStatus.totalPnl>=0) OdinGreen else OdinRed)
                    }
                }
            }
        }

        if (positions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, OdinBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPersian) "پوزیشن بازی وجود ندارد" else "No open positions",
                                color = OdinSilverMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isPersian) "پیپر ترید رو شروع کن" else "Start paper trading",
                                color = OdinSilverMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(positions) { pos ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                    border = BorderStroke(1.dp, OdinBorder),
                    shape = RoundedCornerShape(12.dp)
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
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (pos.side==SignalSide.BUY) OdinGreen.copy(alpha=0.2f) else OdinRed.copy(alpha=0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = pos.side.label(isPersian),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pos.side==SignalSide.BUY) OdinGreen else OdinRed
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = pos.symbol, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = pos.strategy.id, fontSize = 9.sp, color = OdinSilverMuted)
                            }
                            Text(
                                text = "${if (pos.pnl>=0) "+" else ""}$${pos.pnl.toInt()}",
                                fontWeight = FontWeight.Bold,
                                color = if (pos.pnl>=0) OdinGreen else OdinRed,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Entry: ${pos.entryPrice}", fontSize = 10.sp, color = OdinSilverMuted)
                            Text(text = "Now: ${pos.currentPrice}", fontSize = 10.sp, color = OdinSilver)
                            Text(text = "Size: ${String.format("%.3f", pos.size)}", fontSize = 10.sp, color = OdinSilverMuted)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "SL: ${pos.slPrice}", fontSize = 9.sp, color = OdinRed.copy(alpha=0.8f))
                            Text(text = "TP: ${pos.tpPrice}", fontSize = 9.sp, color = OdinGreen.copy(alpha=0.8f))
                            Text(text = "${pos.pnlPercent.toInt()}%", fontSize = 9.sp, color = if (pos.pnlPercent>=0) OdinGreen else OdinRed)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = if (isPersian) "نکات پیپر ترید" else "Paper Trading Tips", fontWeight = FontWeight.Bold, color = OdinCyan, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "• حداقل 1 ماه پیپر ترید قبل از لایو\n• کمیسیون و اسلیپیج لحاظ شده\n• Kill-switch در 3% DD روزانه\n• لاگ کامل در backend"
                        else
                            "• At least 1 month paper before live\n• Commission & slippage included\n• Kill-switch at 3% daily DD\n• Full logs in backend",
                        fontSize = 11.sp,
                        color = OdinSilverMuted,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
