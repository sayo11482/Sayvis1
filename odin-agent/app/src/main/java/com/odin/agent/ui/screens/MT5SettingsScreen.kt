package com.odin.agent.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.odin.agent.mt5.MT5ConnectionManager
import com.odin.agent.mt5.MT5WebViewGateway
import com.odin.agent.mt5.VittaverseBrokerConfig
import com.odin.agent.trading.SymbolManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MT5SettingsScreen(isPersian: Boolean) {
    val context = LocalContext.current
    val mt5Manager = remember { MT5ConnectionManager() }
    val webGateway = remember { MT5WebViewGateway() }
    var mt5State by remember { mutableStateOf(mt5Manager.state.value) }
    var webState by remember { mutableStateOf(webGateway.state.value) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("Vittaverse-Real") }
    var isDemo by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = WebView Gateway REAL, 1 = Native API
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(webGateway.getDefaultUrl()) }
    var extractedBalance by remember { mutableStateOf(0.0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            mt5State = mt5Manager.state.value
            webState = webGateway.state.value
            delay(1000)
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
            Text(text = "odin metatrading - MT5 REAL Gateway", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = "REAL MT5 Vittaverse WebView with manual captcha - After connect balance shown - REAL", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
        }

        item {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF0A0A0A), contentColor = OdinGold) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("WebView REAL Gateway", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Native API", fontSize = 10.sp) })
            }
        }

        if (selectedTab == 0) {
            // WEBVIEW GATEWAY REAL
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (webState.isConnected || extractedBalance > 0) OdinGreen.copy(alpha = 0.4f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "MT5 WebView Gateway REAL - Captcha Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (webState.isConnected || extractedBalance > 0) OdinGreen.copy(alpha = 0.2f) else OdinGold.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (webState.isConnected || extractedBalance > 0) "● CONNECTED REAL BALANCE" else if (webState.isLoading) "● LOADING..." else "○ READY FOR LOGIN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = if (webState.isConnected || extractedBalance > 0) OdinGreen else OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "درگاه اتصال واقعی خود متاتریدر - کپچای امنیت دستی وارد شود - بعد کانکت موجودی نشان داده شود", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        // URL selector
                        Text(text = "WebTrader URLs - REAL", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        webGateway.getAllUrls().forEach { (url, name) ->
                            FilterChip(
                                selected = currentUrl == url,
                                onClick = {
                                    currentUrl = url
                                    webViewRef?.loadUrl(url)
                                },
                                label = { Text("$name", fontSize = 8.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f), selectedLabelColor = OdinGoldLight),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = currentUrl == url, borderColor = if (currentUrl == url) OdinGold else OdinBorder, selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // WebView controls
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { webViewRef?.reload() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Reload", fontSize = 9.sp, color = OdinCyan)
                            }
                            Button(onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Back", fontSize = 9.sp, color = Color.White)
                            }
                            Button(onClick = {
                                webViewRef?.let { wv ->
                                    webGateway.extractBalanceFromPage(wv) { bal ->
                                        extractedBalance = bal
                                    }
                                }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Balance", fontSize = 9.sp, color = OdinGreen)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Current URL display
                        Text(text = "URL: ${webState.url.take(60)}", fontSize = 7.sp, color = OdinSilverDim, maxLines = 1)
                        Text(text = "Title: ${webState.title.take(50)}", fontSize = 7.sp, color = OdinSilverDim, maxLines = 1)
                        if (webState.lastError != null) {
                            Text(text = "Error: ${webState.lastError}", fontSize = 8.sp, color = OdinRed)
                        }
                        // Balance after connect - MUST show
                        if (extractedBalance > 0 || webState.balance > 0 || mt5State.isConnected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "Balance REAL after connect - Must show", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text(text = "Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", if (extractedBalance > 0) extractedBalance else if (webState.balance > 0) webState.balance else mt5State.balance)} USDT", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                        Column { Text(text = "Equity REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", mt5State.equity)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGreen) }
                                        Column { Text(text = "Positions", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${mt5State.positions.size} REAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                                    }
                                    Text(text = "REAL MT5 WebView Gateway - Captcha manually entered - Balance extracted via JS", fontSize = 7.sp, color = OdinSilverDim)
                                }
                            }
                        }
                    }
                }
            }

            item {
                // WEBVIEW ITSELF
                Card(modifier = Modifier.fillMaxWidth().height(500.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webGateway.configureWebView(this)
                                webViewClient = webGateway.createWebViewClient { newState -> webState = newState }
                                webChromeClient = webGateway.createWebChromeClient { newState -> webState = newState }
                                loadUrl(currentUrl)
                                webViewRef = this
                            }
                        },
                        update = { view ->
                            if (view.url != currentUrl && currentUrl.isNotBlank()) {
                                // Don't auto reload if same
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "REAL MT5 WebTrader WebView - Manual captcha entry supported - After login balance extracted - Vittaverse Real - This is REAL gateway not fake login", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
            }
        } else {
            // NATIVE API TAB
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (mt5State.isConnected) OdinGreen.copy(alpha = 0.08f) else OdinRed.copy(alpha = 0.08f)), border = BorderStroke(1.dp, if (mt5State.isConnected) OdinGreen.copy(alpha = 0.4f) else OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = if (mt5State.isConnected) OdinGreen else OdinRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "MT5 Native API - Vittaverse", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (mt5State.isConnected) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (mt5State.isConnected) "● CONNECTED REAL" else "○ DISCONNECTED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (mt5State.isConnected) OdinGreen else OdinRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (mt5State.isConnected) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(text = "Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", mt5State.balance)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                                Column { Text(text = "Equity REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", mt5State.equity)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGreen) }
                                Column { Text(text = "Positions REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${mt5State.positions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { scope.launch { mt5Manager.disconnect() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinRed.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = OdinRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Disconnect", color = OdinRed, fontSize = 12.sp)
                            }
                        }
                        mt5State.lastError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.1f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Text(text = err, fontSize = 10.sp, color = OdinRed, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }

            if (!mt5State.isConnected) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Login to Vittaverse MT5 Account - REAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Login / Account Number", fontSize = 10.sp) }, placeholder = { Text("12345678", fontSize = 10.sp, color = OdinSilverDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) })
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password", fontSize = 10.sp) }, placeholder = { Text("••••••••", fontSize = 10.sp, color = OdinSilverDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) }, trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(18.dp)) } })
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Server", fontSize = 10.sp, color = OdinSilverMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                mt5Manager.getServers().take(2).forEach { srv ->
                                    FilterChip(selected = server == srv.name, onClick = { server = srv.name }, label = { Text(srv.name, fontSize = 9.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGold.copy(alpha = 0.2f), selectedLabelColor = OdinGoldLight), border = FilterChipDefaults.filterChipBorder(borderColor = if (server == srv.name) OdinGold else OdinBorder, selectedBorderColor = OdinGold, borderWidth = 1.dp, selectedBorderWidth = 1.dp, enabled = true, selected = server == srv.name))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = isDemo, onCheckedChange = { isDemo = it }, colors = SwitchDefaults.colors(checkedThumbColor = OdinCyan, checkedTrackColor = OdinCyan.copy(alpha = 0.3f)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Demo Account", fontSize = 11.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { if (isConnecting) return@Button; isConnecting = true; scope.launch { val success = mt5Manager.connect(login, password, server, isDemo); isConnecting = false } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen), shape = RoundedCornerShape(10.dp), enabled = !isConnecting && login.isNotBlank() && password.isNotBlank()) {
                                if (isConnecting) { CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Connecting REAL...", fontSize = 12.sp) } else { Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(text = "Connect REAL to Vittaverse MT5", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "💡 REAL Vittaverse account - REAL connection to global forex market - Floating PnL from REAL trades", fontSize = 9.sp, color = OdinGreen)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Vittaverse Broker Info - REAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Broker", "${VittaverseBrokerConfig.BROKER_NAME} (${VittaverseBrokerConfig.FOUNDED})")
                    InfoRow("Website", VittaverseBrokerConfig.WEBSITE)
                    InfoRow("Min Deposit", "$${VittaverseBrokerConfig.MIN_DEPOSIT}")
                    InfoRow("Leverage", "Up to 1:${VittaverseBrokerConfig.MAX_LEVERAGE}")
                    InfoRow("Spread", "From ${VittaverseBrokerConfig.SPREAD_FROM} pips")
                    InfoRow("Platforms", VittaverseBrokerConfig.platforms.joinToString(", "))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Tradable Symbols - ${SymbolManager.allSymbols.size} - Tether Unit 235K Toman REAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SymbolManager.allSymbols.groupBy { it.category }.forEach { (cat, symbols) -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(text = cat.labelEn, fontSize = 9.sp, color = OdinSilverMuted); Text(text = "${symbols.size} symbols", fontSize = 9.sp, color = OdinGold) }; Spacer(modifier = Modifier.height(2.dp)) }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "IRR: ${SymbolManager.getIRRPairs().joinToString(", ") { it.symbol }} - USDT/IRR 235K Toman REAL Tether unit", fontSize = 9.sp, color = OdinRed.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 10.sp, color = OdinSilverMuted)
        Text(text = value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
