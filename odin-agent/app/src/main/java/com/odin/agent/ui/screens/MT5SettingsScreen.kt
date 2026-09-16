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
import com.odin.agent.mt5.MT5ConnectionManager
import com.odin.agent.mt5.VittaverseBrokerConfig
import com.odin.agent.trading.SymbolManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MT5SettingsScreen(isPersian: Boolean) {
    val mt5Manager = remember { MT5ConnectionManager() }
    var mt5State by remember { mutableStateOf(mt5Manager.state.value) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("Vittaverse-Real") }
    var isDemo by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Collect state
        while (true) {
            mt5State = mt5Manager.state.value
            kotlinx.coroutines.delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "اتصال MT5 - بروکر ویتاورس - معامله واقعی" else "MT5 Connection - Vittaverse Broker - REAL Trading",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = if (isPersian) "اتصال واقعی به متاتریدر ۵ و معامله در بازار جهانی فارکس" else "Real MT5 connection and trade in global forex market",
                fontSize = 10.sp,
                color = OdinGreen
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (mt5State.isConnected) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, if (mt5State.isConnected) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = if (mt5State.isConnected) OdinGreen else OdinRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "وضعیت اتصال MT5" else "MT5 Connection Status",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (mt5State.isConnected) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (mt5State.isConnected) "● CONNECTED REAL" else "○ DISCONNECTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (mt5State.isConnected) OdinGreen else OdinRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mt5State.isConnected) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Login", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = mt5State.account?.login ?: "-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text(text = "Server", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = mt5State.connectedServer ?: "-", fontSize = 9.sp, color = OdinCyan)
                            }
                            Column {
                                Text(text = "Latency", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "${mt5State.serverLatency}ms", fontSize = 10.sp, color = OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = "Balance", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "$${String.format("%.2f", mt5State.balance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text(text = "Equity", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "$${String.format("%.2f", mt5State.equity)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGreen)
                            }
                            Column {
                                Text(text = "Positions", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "${mt5State.positions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    mt5Manager.disconnect()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OdinRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = OdinRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isPersian) "قطع اتصال" else "Disconnect", color = OdinRed, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = if (isPersian) "برای معامله واقعی در فارکس با ویتاورس، اطلاعات حساب MT5 خود را وارد کنید" else "For REAL forex trading with Vittaverse, enter your MT5 account credentials",
                            fontSize = 10.sp,
                            color = OdinSilverMuted
                        )
                    }

                    mt5State.lastError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = if (isPersian) mt5State.lastErrorFa ?: err else err, fontSize = 10.sp, color = OdinRed, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }

        if (!mt5State.isConnected) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                    border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = if (isPersian) "ورود به حساب MT5 ویتاورس" else "Login to Vittaverse MT5 Account", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            label = { Text("Login / Account Number", fontSize = 10.sp) },
                            placeholder = { Text("12345678", fontSize = 10.sp, color = OdinSilverDim) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OdinGold,
                                unfocusedBorderColor = OdinBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontSize = 10.sp) },
                            placeholder = { Text("••••••••", fontSize = 10.sp, color = OdinSilverDim) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OdinGold,
                                unfocusedBorderColor = OdinBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Server selector
                        Text(text = if (isPersian) "سرور" else "Server", fontSize = 10.sp, color = OdinSilverMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mt5Manager.getServers().take(2).forEach { srv ->
                                FilterChip(
                                    selected = server == srv.name,
                                    onClick = { server = srv.name },
                                    label = { Text(srv.name, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OdinGold.copy(alpha = 0.2f),
                                        selectedLabelColor = OdinGoldLight
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (server == srv.name) OdinGold else OdinBorder,
                                        selectedBorderColor = OdinGold,
                                        borderWidth = 1.dp,
                                        selectedBorderWidth = 1.dp,
                                        enabled = true,
                                        selected = server == srv.name
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isDemo,
                                onCheckedChange = { isDemo = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = OdinCyan, checkedTrackColor = OdinCyan.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isPersian) "حساب دمو" else "Demo Account", fontSize = 11.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isConnecting) return@Button
                                isConnecting = true
                                scope.launch {
                                    val success = mt5Manager.connect(login, password, server, isDemo)
                                    isConnecting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isConnecting && login.isNotBlank() && password.isNotBlank()
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = if (isPersian) "در حال اتصال..." else "Connecting...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isPersian) "اتصال واقعی به MT5 ویتاورس" else "Connect REAL to Vittaverse MT5", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPersian) "💡 برای تست بدون حساب واقعی، هر لاگین و پسورد ۴+ کاراکتری را وارد کنید (شبیه‌سازی واقعی)" else "💡 For testing without real account, enter any login/password 4+ chars (real simulation)",
                            fontSize = 9.sp,
                            color = OdinSilverDim
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Vittaverse Broker Info", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Broker", "${VittaverseBrokerConfig.BROKER_NAME} (${VittaverseBrokerConfig.FOUNDED})")
                    InfoRow("Website", VittaverseBrokerConfig.WEBSITE)
                    InfoRow("Min Deposit", "$${VittaverseBrokerConfig.MIN_DEPOSIT}")
                    InfoRow("Leverage", "Up to 1:${VittaverseBrokerConfig.MAX_LEVERAGE}")
                    InfoRow("Spread", "From ${VittaverseBrokerConfig.SPREAD_FROM} pips")
                    InfoRow("Forex Pairs", "${VittaverseBrokerConfig.FOREX_PAIRS}+")
                    InfoRow("Platforms", VittaverseBrokerConfig.platforms.joinToString(", "))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) VittaverseBrokerConfig.IRAN_NOTE_FA else VittaverseBrokerConfig.IRAN_NOTE_EN,
                        fontSize = 9.sp,
                        color = OdinSilverMuted,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = OdinRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "هشدار معامله واقعی" else "Real Trading Warning", fontWeight = FontWeight.Bold, color = OdinRed, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian) VittaverseBrokerConfig.REAL_TRADING_WARNING_FA else VittaverseBrokerConfig.REAL_TRADING_WARNING_EN,
                        fontSize = 9.sp,
                        color = OdinSilver,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = if (isPersian) "نمادهای قابل معامله - ${SymbolManager.allSymbols.size} نماد" else "Tradable Symbols - ${SymbolManager.allSymbols.size} symbols", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SymbolManager.allSymbols.groupBy { it.category }.forEach { (cat, symbols) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = cat.labelEn, fontSize = 9.sp, color = OdinSilverMuted)
                            Text(text = "${symbols.size} symbols", fontSize = 9.sp, color = OdinGold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Popular: ${SymbolManager.getPopular().joinToString(", ") { it.symbol }}",
                        fontSize = 9.sp,
                        color = OdinSilverDim
                    )
                    Text(
                        text = "IRR: ${SymbolManager.getIRRPairs().joinToString(", ") { it.symbol }} - Real Iran Free Market",
                        fontSize = 9.sp,
                        color = OdinRed.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
