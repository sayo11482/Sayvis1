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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.trading.OdinTokenManager
import com.odin.agent.trading.OdinTier
import com.odin.agent.ui.theme.*

/**
 * ODIN v1.0.27 - Odin Token Screen - Odin.trade
 * توکن اودین ODN - کیف پول - استیک - تییرها - گیت اجباری استراتژی پرسود
 * کمیسیون 20% سود → 2$ از هر 10$ سود اتومات به صاحب نرم‌افزار (سوینکس)
 */

@Composable
fun OdinTokenScreen(isPersian: Boolean) {
    val manager = remember { OdinTokenManager.getInstance() }
    var state by remember { mutableStateOf(manager.state.value) }
    var actionMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        manager.dailyLoginBonus()
        manager.state.collect { state = it }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------- هدر ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Paid, contentDescription = null, tint = OdinGold, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "توکن اودین ODN - اقتصاد Odin.trade" else "Odin Token ODN - Odin.trade Economy", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "کمیسیون 20% فقط از سود - اجباری برای استراتژی‌های پرسود بالاتر" else "20% fee on profit only - mandatory for high-profit strategies", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${state.wallet.balance}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                            Text(if (isPersian) "ODN آزاد" else "ODN liquid", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                        Column {
                            Text("${state.wallet.staked}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                            Text(if (isPersian) "ODN استیک شده" else "ODN staked", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${state.tier.label(isPersian)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                            Text(if (isPersian) "تییر فعلی" else "Current tier", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (isPersian) "آدرس ولت: ${state.wallet.address}" else "Wallet: ${state.wallet.address}", fontSize = 8.sp, color = OdinSilverMuted)
                }
            }
        }

        // ---------- گیت اجباری استراتژی‌ها ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(if (isPersian) "🔒 گیت اجباری توکن - دسترسی استراتژی‌ها" else "🔒 Mandatory Token Gate - Strategy Access", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.gates.forEach { gate ->
                        val strategyLabel = gate.strategy.label(isPersian)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(if (gate.isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = if (gate.isLocked) Color(0xFFFF5252) else OdinGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strategyLabel, fontSize = 10.sp, color = if (gate.isLocked) OdinSilverMuted else Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                if (gate.requiredStake == 0.0) (if (isPersian) "رایگان" else "FREE")
                                else "${gate.requiredStake.toInt()} ODN" + if (gate.isLocked) (if (isPersian) " 🔒" else " 🔒") else " ✓",
                                fontSize = 10.sp, fontWeight = FontWeight.Black,
                                color = if (gate.requiredStake == 0.0) OdinGreen else if (gate.isLocked) Color(0xFFFF5252) else OdinGoldLight
                            )
                        }
                    }
                    Text(if (isPersian) "پس از استیک کافی، گیت اتومات باز می‌شود - تییرها: نقره 100 | طلا 500 | پلاتینیوم 2000" else "Stake enough → gate auto-unlocks - Tiers: Silver 100 | Gold 500 | Platinum 2000", fontSize = 8.sp, color = OdinSilverMuted)
                }
            }
        }

        // ---------- دکمه‌های استیک سریع ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(if (isPersian) "استیک سریع - باز کردن استراتژی پرسود" else "Quick Stake - Unlock Profitable Strategies", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100.0 to "SILVER", 500.0 to "GOLD", 2000.0 to "PLATINUM").forEach { (amt, name) ->
                            OutlinedButton(
                                onClick = {
                                    val need = amt - state.wallet.staked
                                    if (need > 0 && need > state.wallet.balance) {
                                        manager.topUp(kotlin.math.ceil(need / OdinTokenManager.TOKEN_PRICE_USD))
                                        state = manager.state.value
                                    }
                                    val stakedNow = if (need > 0) manager.stake(need) else true
                                    state = manager.state.value
                                    actionMsg = if (stakedNow && state.wallet.staked >= amt) "$name ✓ ${state.wallet.staked.toInt()} ODN" else "ERR"
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OdinGold),
                                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${amt.toInt()} ODN\n$name", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { manager.topUp(10.0); state = manager.state.value; actionMsg = if (isPersian) "شارژ 10$ → +100 ODN" else "Topped 10$" }, colors = ButtonDefaults.buttonColors(containerColor = OdinGold.copy(alpha = 0.25f)), shape = RoundedCornerShape(8.dp)) {
                            Text(if (isPersian) "شارژ 10$ = 100 ODN" else "Top-Up 10$ = 100 ODN", fontSize = 9.sp, color = OdinGoldLight, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { manager.unstake(state.wallet.staked); state = manager.state.value; actionMsg = if (isPersian) "خروج از استیک" else "Unstaked all" }, colors = ButtonDefaults.outlinedButtonColors(contentColor = OdinCyan), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                            Text(if (isPersian) "خروج از استیک" else "Unstake All", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (actionMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(actionMsg, fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- کمیسیون صاحب نرم‌افزار ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPersian) "خزانه کمیسیون - سهم صاحب نرم‌افزار (سوینکس)" else "Commission Treasury - Software Owner (Swinex)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${state.ownerTreasuryUsd}$", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGreen); Text(if (isPersian) "خزانه دلاری (50%)" else "USD treasury", fontSize = 8.sp, color = OdinSilverMuted) }
                        Column { Text("${state.ownerTreasuryOdnBurned}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252)); Text(if (isPersian) "ODN سوزانده (دیفلوشنری)" else "ODN burned", fontSize = 8.sp, color = OdinSilverMuted) }
                        Column(horizontalAlignment = Alignment.End) { Text("${state.totalFeesCollected}$", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGoldLight); Text(if (isPersian) "کل کمیسیون 20%" else "Total 20% fees", fontSize = 8.sp, color = OdinSilverMuted) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (isPersian) "قانون: هر ${"%.0f".format(10.0)}$ سود ربات → ${"%.0f".format(2.0)}$ اتومات به سوینکس + ${"%.0f".format(8.0)}$ کاربر | روی ضرر هیچ کمیسیونی نیست" else "Rule: every 10$ bot profit → 2$ auto to Swinex + 8$ user | NO fee on losses", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---------- تاریخچه کمیسیون‌ها ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinSilverDim.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(if (isPersian) "آخرین کمیسیون‌ها (${state.commissionPayments.size})" else "Recent Commissions (${state.commissionPayments.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (state.commissionPayments.isEmpty()) {
                        Text(if (isPersian) "هنوز کمیسیونی ثبت نشده - با اولین معامله سودده ثبت می‌شود" else "No commissions yet - first profitable trade records", fontSize = 9.sp, color = OdinSilverMuted)
                    } else {
                        state.commissionPayments.takeLast(6).reversed().forEach { p ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Trade ${p.tradeId.take(16)}", fontSize = 9.sp, color = OdinSilver)
                                    Text("-${p.feeUsd}$ → سوینکس", fontSize = 9.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("سود ${p.grossProfitUsd}$ | کاربر ${p.userNetUsd}$ | سوزانده ${p.odnBurned} ODN", fontSize = 8.sp, color = OdinSilverMuted)
                                    Text(p.jalali.take(15), fontSize = 8.sp, color = OdinCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- تراکنش‌های ولت ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinSilverDim.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPersian) "تراکنش‌های ولت (${state.transactions.size})" else "Wallet Transactions (${state.transactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.ledgerIntegrityValid) "SHA-256 ✓" else "TAMPERED!", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (state.ledgerIntegrityValid) OdinGreen else Color(0xFFFF5252))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    state.transactions.takeLast(8).reversed().forEach { tx ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tx.kind.labelFa, fontSize = 9.sp, color = OdinSilver)
                            Text("${if (tx.amount >= 0) "+" else ""}${tx.amount} ODN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (tx.amount >= 0) OdinGreen else Color(0xFFFF5252))
                            Text(tx.jalali.take(12), fontSize = 8.sp, color = OdinSilverMuted)
                        }
                    }
                }
            }
        }

        item {
            Text(if (isPersian) "Odin Token ODN - قیمت 0.1$ - پاداش روزانه 5 ODN - زیرمجموعه 100 ODN - 50% کمیسیون سوزانده می‌شود" else "ODN price 0.1$ - daily 5 ODN - referral 100 ODN - 50% of fee burned", fontSize = 8.sp, color = OdinSilverMuted, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}
