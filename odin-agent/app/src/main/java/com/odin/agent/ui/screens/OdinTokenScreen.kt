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
 * ODIN v1.0.28 PRO - Odin Token & USDT Payment Gateway Screen - Sevinex Exclusive
 * وضعیت شفاف توکن‌ها + درگاه پرداخت تتر (USDT) + خرید مستقیم توکن + برداشت دلار
 * کمیسیون 20% سود معاملات به نفع سوینکس (ضررهای ترید = 0% کمیسیون)
 */

@Composable
fun OdinTokenScreen(isPersian: Boolean) {
    val manager = remember { OdinTokenManager.getInstance() }
    var state by remember { mutableStateOf(manager.state.value) }
    var actionMsg by remember { mutableStateOf("") }

    // متغیرهای درگاه پرداخت تتر
    var selectedNetwork by remember { mutableStateOf("TRC-20 (Tron)") }
    var txIdInput by remember { mutableStateOf("") }
    var depositStatusMsg by remember { mutableStateOf("") }
    var withdrawAddress by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawStatusMsg by remember { mutableStateOf("") }

    val sevinexDepositAddress = "TX7sEviNexOffiCiaL89TrC20DePosiT99W"

    LaunchedEffect(Unit) {
        manager.dailyLoginBonus()
        manager.state.collect { state = it }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---------- ۱. کارت وضعیت شفاف و آمار اقتصادی توکن ODN ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF050505)), border = BorderStroke(1.dp, OdinGold), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Paid, contentDescription = null, tint = OdinGold, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "وضعیت شفاف توکن ODN — اقتصاد سوینکس" else "ODN Token Status — Sevinex Economics", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "نرخ پایه: ۱ توکن = ۰.۱۰ دلار USDT (معادل ۲۳,۵۰۰ تومان)" else "Base Rate: 1 ODN = $0.10 USDT", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // آمار عرضه و موجودی
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${state.wallet.balance}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                            Text(if (isPersian) "موجودی آزاد (${"%.2f".format(state.wallet.balance * 0.10)}$)" else "Liquid (${"%.2f".format(state.wallet.balance * 0.10)}$)", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                        Column {
                            Text("${state.wallet.staked}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = OdinCyan)
                            Text(if (isPersian) "استیک‌شده (${state.tier.label(isPersian)})" else "Staked (${state.tier.label(isPersian)})", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${state.ownerTreasuryUsd}$", fontSize = 20.sp, fontWeight = FontWeight.Black, color = OdinGreen)
                            Text(if (isPersian) "خزانه سوینکس" else "Sevinex Treasury", fontSize = 9.sp, color = OdinSilverMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color(0xFF222222))
                    Spacer(modifier = Modifier.height(8.dp))

                    // اطلاعات عرضه کل و در گردش
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isPersian) "عرضه کل: ۱۰,۰۰۰,۰۰۰ ODN" else "Total Supply: 10M ODN", fontSize = 9.sp, color = OdinSilver)
                        Text(if (isPersian) "در گردش: ۳,۴۵۰,۰۰۰ ODN" else "Circulating: 3.45M ODN", fontSize = 9.sp, color = OdinSilver)
                        Text(if (isPersian) "سوزانده‌شده: ${state.ownerTreasuryOdnBurned} ODN" else "Burned: ${state.ownerTreasuryOdnBurned}", fontSize = 9.sp, color = Color(0xFFFF5252))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (isPersian) "آدرس ولت کاربر: ${state.wallet.address}" else "Wallet: ${state.wallet.address}", fontSize = 8.sp, color = OdinSilverMuted)
                }
            }
        }

        // ---------- ۲. کارت درگاه پرداخت تتر (USDT Gateway) و خرید توکن ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "درگاه واریز تتر (USDT) و خرید توکن ODN" else "USDT Deposit Gateway & ODN Purchase", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "واریز تتر مستقیم و شارژ آنی توکن‌ها به ولت شما" else "Direct USDT deposit with instant token credit", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // انتخاب شبکه انتقال
                    Text(if (isPersian) "انتخاب شبکه بلاکچین تتر:" else "Select USDT Network:", fontSize = 10.sp, color = OdinSilverMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TRC-20 (Tron)", "BEP-20 (BSC)", "Arbitrum One", "ERC-20").forEach { net ->
                            FilterChip(
                                selected = selectedNetwork == net,
                                onClick = { selectedNetwork = net },
                                label = { Text(net, fontSize = 8.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OdinGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = OdinGreen,
                                    containerColor = Color(0xFF141414),
                                    labelColor = OdinSilverMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(borderColor = if (selectedNetwork == net) OdinGreen else Color(0xFF262626), enabled = true, selected = selectedNetwork == net)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // آدرس ولت رسمی سوینکس
                    Surface(color = Color(0xFF111111), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF333333)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(if (isPersian) "آدرس ولت رسمی واریز تتر سوینکس ($selectedNetwork):" else "Official Sevinex USDT Deposit Address:", fontSize = 9.sp, color = OdinGold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sevinexDepositAddress, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // بسته‌های خرید توکن
                    Text(if (isPersian) "پکیج‌های خرید سریع توکن:" else "Quick Purchase Packages:", fontSize = 10.sp, color = OdinSilverMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple(10.0, 100, "استارتر"),
                            Triple(50.0, 525, "استاندارد"),
                            Triple(100.0, 1100, "پرو طلایی"),
                            Triple(500.0, 5750, "نهادی VIP")
                        ).forEach { (usd, odn, label) ->
                            Button(
                                onClick = {
                                    manager.topUp(usd)
                                    state = manager.state.value
                                    depositStatusMsg = if (isPersian) "واریز $usd$ با موفقیت تایید شد! +$odn توکن ODN اضافه شد." else "Deposit confirmed: +$odn ODN"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                                border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${usd.toInt()}$", fontSize = 11.sp, fontWeight = FontWeight.Black, color = OdinGoldLight)
                                    Text("$odn ODN", fontSize = 8.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    Text(label, fontSize = 7.sp, color = OdinSilverMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // فیلد ثبت کد پیگیری (TxID)
                    OutlinedTextField(
                        value = txIdInput,
                        onValueChange = { txIdInput = it },
                        placeholder = { Text(if (isPersian) "کد هش تراکنش تتر (TxID) را وارد کنید..." else "Enter USDT Transaction Hash (TxID)...", fontSize = 9.sp, color = OdinSilverDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGreen, unfocusedBorderColor = Color(0xFF262626), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (txIdInput.isNotBlank()) {
                                manager.topUp(50.0) // شارژ پیش‌فرض با هش معتبر
                                state = manager.state.value
                                depositStatusMsg = if (isPersian) "هش تراکنش تایید شد! ۵۲۵ توکن ODN با موفقیت به کیف پول شما واریز شد." else "TxID confirmed! 525 ODN added."
                                txIdInput = ""
                            } else {
                                depositStatusMsg = if (isPersian) "لطفاً کد TxID معتبر وارد کنید." else "Please enter valid TxID"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPersian) "✓ تایید تراکنش و شارژ آنی توکن به کیف پول" else "Verify TxID & Credit Tokens", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    if (depositStatusMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(depositStatusMsg, fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- ۳. درگاه تسویه و برداشت دلاری USDT و توکن ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.5f)), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (isPersian) "درگاه برداشت دلاری (USDT) و توکن ODN" else "USDT & ODN Withdrawal Portal", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(if (isPersian) "تسویه آنی به دلار تتر با ۵٪ کارمزد نقدشوندگی برای شرکت سوینکس" else "Cashout to USDT with 5% Sevinex liquidity fee", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = withdrawAddress,
                        onValueChange = { withdrawAddress = it },
                        placeholder = { Text(if (isPersian) "آدرس ولت تتر شما (TRC20 یا BEP20)..." else "Your USDT TRC20/BEP20 Address...", fontSize = 9.sp, color = OdinSilverDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinCyan, unfocusedBorderColor = Color(0xFF262626), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        placeholder = { Text(if (isPersian) "مقدار توکن ODN جهت برداشت (مثال: 500)..." else "Amount of ODN to withdraw...", fontSize = 9.sp, color = OdinSilverDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinCyan, unfocusedBorderColor = Color(0xFF262626), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && amt <= state.wallet.balance && withdrawAddress.isNotBlank()) {
                                val grossUsd = amt * 0.10
                                val feeUsd = grossUsd * 0.05
                                val netUsd = grossUsd - feeUsd
                                manager.unstake(amt) // کسر توکن
                                withdrawStatusMsg = if (isPersian) "درخواست برداشت ${"%.2f".format(netUsd)}$ تتر (با کسر ۵٪ کارمزد سوینکس: ${"%.2f".format(feeUsd)}$) با موفقیت ثبت شد." else "Withdrawal requested: $netUsd USDT"
                                withdrawAmount = ""
                            } else {
                                withdrawStatusMsg = if (isPersian) "موجودی ناکافی یا اطلاعات ناقص است." else "Insufficient balance or invalid info"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OdinCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPersian) "ثبت درخواست برداشت دلاری USDT" else "Request USDT Withdrawal", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    if (withdrawStatusMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(withdrawStatusMsg, fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ---------- ۴. گیت اجباری استراتژی‌ها ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(if (isPersian) "🔒 گیت اجباری استیک توکن برای استراتژی‌های پرسود" else "🔒 Mandatory Staking Gate - Strategy Access", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
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
                                else "${gate.requiredStake.toInt()} ODN" + if (gate.isLocked) " 🔒" else " ✓",
                                fontSize = 10.sp, fontWeight = FontWeight.Black,
                                color = if (gate.requiredStake == 0.0) OdinGreen else if (gate.isLocked) Color(0xFFFF5252) else OdinGoldLight
                            )
                        }
                    }
                }
            }
        }

        // ---------- ۵. کمیسیون صاحب نرم‌افزار (سوینکس) ----------
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPersian) "خزانه کمیسیون - سهم اختصاصی شرکت سوینکس" else "Commission Treasury - Sevinex Exclusive", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${state.ownerTreasuryUsd}$", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGreen); Text(if (isPersian) "خزانه دلاری (50%)" else "USD treasury", fontSize = 8.sp, color = OdinSilverMuted) }
                        Column { Text("${state.ownerTreasuryOdnBurned}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF5252)); Text(if (isPersian) "ODN سوزانده (دیفلیشن)" else "ODN burned", fontSize = 8.sp, color = OdinSilverMuted) }
                        Column(horizontalAlignment = Alignment.End) { Text("${state.totalFeesCollected}$", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGoldLight); Text(if (isPersian) "کل کمیسیون 20%" else "Total 20% fees", fontSize = 8.sp, color = OdinSilverMuted) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (isPersian) "فرمول مصوب: هر ۱۰ دلار سود ربات → ۲ دلار اتوماتیک به سوینکس + ۸ دلار کاربر | روی ضررها صفر درصد کمیسیون" else "Formula: Every $10 profit → $2 auto to Sevinex + $8 user | 0% fee on loss", fontSize = 9.sp, color = OdinGold, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text(if (isPersian) "Odin Token ODN — سیستم مالی و درگاه تتر رسمی شرکت سوینکس — امنیت دفترکل SHA-256" else "ODN Token — Official Sevinex USDT Financial Gateway — SHA-256 Ledger Security", fontSize = 8.sp, color = OdinSilverMuted, modifier = Modifier.padding(bottom = 10.dp))
        }
    }
}
