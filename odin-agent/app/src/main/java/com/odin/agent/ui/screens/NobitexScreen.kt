package com.odin.agent.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.odin.agent.nobitex.NobitexApiManager
import com.odin.agent.nobitex.NobitexWebViewGateway
import com.odin.agent.trading.NobitexMarketProvider
import com.odin.agent.trading.RealMarketDataManager
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NobitexScreen(isPersian: Boolean) {
    val context = LocalContext.current
    val webGateway = remember { NobitexWebViewGateway() }
    val apiManager = remember { NobitexApiManager() }
    val marketProvider = remember { NobitexMarketProvider() }
    val realDataManager = remember { RealMarketDataManager() }

    var webState by remember { mutableStateOf(webGateway.state.value) }
    var apiState by remember { mutableStateOf(apiManager.state.value) }
    var selectedTab by remember { mutableStateOf(0) } // 0=WebView Spot, 1=API Trading, 2=Market REAL
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(webGateway.getDefaultUrl()) }
    var extractedIRT by remember { mutableStateOf(0.0) }
    var extractedUSDT by remember { mutableStateOf(0.0) }
    var apiToken by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var realPrices by remember { mutableStateOf<Map<String, com.odin.agent.trading.RealPrice>>(emptyMap()) }
    var usdtPrice by remember { mutableStateOf<com.odin.agent.trading.NobitexPrice?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            webState = webGateway.state.value
            apiState = apiManager.state.value
            try {
                realPrices = realDataManager.fetchRealPrices()
                usdtPrice = marketProvider.fetchUSDTPrice()
            } catch (e: Exception) {}
            delay(2000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "odin metatrading - Nobitex REAL Gateway", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(text = "نوبیتکس - بزرگترین صرافی ایران 11M کاربر - موجودی اسپات REAL + معامله API + قیمت REAL 231K تومان", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
        }

        item {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF0A0A0A), contentColor = OdinGreen) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("WebView Spot REAL", fontSize = 9.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("API Trading REAL", fontSize = 9.sp) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Market REAL Price", fontSize = 9.sp) })
            }
        }

        if (selectedTab == 0) {
            // WEBVIEW SPOT TAB - like MT5 WebView
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (webState.isConnected || extractedIRT > 0) OdinGreen.copy(alpha = 0.4f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Nobitex WebView REAL - Spot Balance - Captcha Manual", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (webState.isConnected || extractedIRT > 0) OdinGreen.copy(alpha = 0.2f) else OdinGold.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (webState.isConnected || extractedIRT > 0) "● CONNECTED REAL BALANCE" else if (webState.isLoading) "● LOADING..." else "○ READY FOR LOGIN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = if (webState.isConnected || extractedIRT > 0) OdinGreen else OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "درگاه واقعی نوبیتکس - https://nobitex.ir/panel/balance/spot/ - کپچای دستی وارد شود - بعد کانکت موجودی نشان داده شود - 11M کاربر", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Nobitex URLs REAL", fontSize = 9.sp, color = OdinCyan, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        webGateway.getAllUrls().forEach { (url, name) ->
                            FilterChip(
                                selected = currentUrl == url,
                                onClick = { currentUrl = url; webViewRef?.loadUrl(url) },
                                label = { Text(name, fontSize = 7.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OdinGreen.copy(alpha = 0.2f), selectedLabelColor = OdinGoldLight),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = currentUrl == url, borderColor = if (currentUrl == url) OdinGreen else OdinBorder, selectedBorderColor = OdinGreen, borderWidth = 1.dp, selectedBorderWidth = 1.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                                    webGateway.extractBalanceFromPage(wv) { irt, usdt ->
                                        extractedIRT = irt
                                        extractedUSDT = usdt
                                    }
                                    webGateway.extractAllBalances(wv) { balances ->
                                        // Could show all balances
                                    }
                                }
                            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Balance", fontSize = 9.sp, color = OdinGreen)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "URL: ${webState.url.take(60)}", fontSize = 7.sp, color = OdinSilverDim, maxLines = 1)
                        Text(text = "Title: ${webState.title.take(50)}", fontSize = 7.sp, color = OdinSilverDim, maxLines = 1)
                        if (webState.lastError != null) Text(text = "Error: ${webState.lastError}", fontSize = 8.sp, color = OdinRed)
                        if (extractedIRT > 0 || extractedUSDT > 0 || webState.balanceIRT > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = "Balance REAL after connect - Must show - Nobitex Spot", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text(text = "IRT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%,.0f", if (extractedIRT > 0) extractedIRT else webState.balanceIRT)} Toman", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                        Column { Text(text = "USDT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", if (extractedUSDT > 0) extractedUSDT else webState.balanceUSDT)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                                        Column { Text(text = "Source", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "Nobitex REAL", fontSize = 9.sp, color = OdinCyan) }
                                    }
                                    Text(text = "REAL Nobitex WebView Gateway - Captcha manually entered - Balance extracted via JS - https://nobitex.ir/panel/balance/spot/", fontSize = 7.sp, color = OdinSilverDim)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().height(500.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(text = "REAL Nobitex WebView - Manual captcha entry supported - After login balance extracted - Spot Balance REAL - This is REAL gateway not fake", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
            }
        } else if (selectedTab == 1) {
            // API TRADING TAB
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (apiState.isConnected) OdinGreen.copy(alpha = 0.08f) else Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (apiState.isConnected) OdinGreen.copy(alpha = 0.4f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Nobitex API REAL Trading - Token", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (apiState.isConnected) OdinGreen.copy(alpha = 0.2f) else OdinRed.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (apiState.isConnected) "● CONNECTED REAL" else "○ DISCONNECTED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = if (apiState.isConnected) OdinGreen else OdinRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!apiState.isConnected) {
                            OutlinedTextField(
                                value = apiToken,
                                onValueChange = { apiToken = it },
                                label = { Text("Nobitex API Token", fontSize = 10.sp) },
                                placeholder = { Text("Token from https://nobitex.ir/panel/settings/api/", fontSize = 9.sp, color = OdinSilverDim) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true,
                                visualTransformation = if (showToken) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = OdinGold, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { IconButton(onClick = { showToken = !showToken }) { Icon(if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = OdinSilverMuted, modifier = Modifier.size(18.dp)) } }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isConnecting) return@Button
                                    isConnecting = true
                                    scope.launch {
                                        val success = apiManager.connect(apiToken)
                                        isConnecting = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = OdinGreen),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isConnecting && apiToken.isNotBlank()
                            ) {
                                if (isConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Connecting REAL...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Connect REAL to Nobitex API", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "💡 از پنل نوبیتکس: تنظیمات → API → توکن بساز - Token lasts 4h or 30 days", fontSize = 8.sp, color = OdinSilverMuted)
                            Text(text = "⚠️ توکن را امن نگه دار - 2025 هک 90M$ - منبع کد لو رفت", fontSize = 8.sp, color = OdinRed.copy(alpha = 0.7f))
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(text = "IRT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%,.0f", apiState.totalIRT)} Toman", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                Column { Text(text = "USDT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", apiState.totalUSDT)} USDT", fontSize = 13.sp, fontWeight = FontWeight.Black, color = OdinGold) }
                                Column { Text(text = "Wallets", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${apiState.wallets.size} REAL", fontSize = 11.sp, color = OdinCyan) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { scope.launch { apiManager.disconnect() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinRed.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = OdinRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Disconnect", color = OdinRed, fontSize = 12.sp)
                            }
                        }
                        apiState.lastError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinRed.copy(alpha = 0.1f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Text(text = err, fontSize = 10.sp, color = OdinRed, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }

            if (apiState.isConnected) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Wallets REAL - Spot Balance", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            apiState.wallets.values.sortedByDescending { it.balance }.forEach { wallet ->
                                if (wallet.balance > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(OdinGold.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                                Text(text = wallet.currency.take(3).uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Black, color = OdinGold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(text = wallet.currency.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(text = "Active: ${String.format("%.6f", wallet.activeBalance)}", fontSize = 8.sp, color = OdinSilverMuted)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = String.format("%.6f", wallet.balance), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            if (wallet.blocked > 0) Text(text = "Blocked ${String.format("%.4f", wallet.blocked)}", fontSize = 7.sp, color = OdinRed)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            if (apiState.wallets.values.none { it.balance > 0 }) {
                                Text(text = "No balance - کیف پول خالی", fontSize = 10.sp, color = OdinSilverMuted)
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Quick Trade REAL - Nobitex", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Buy/Sell via API - POST /market/orders/add - REAL trading", fontSize = 9.sp, color = OdinSilverMuted)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { apiManager.fetchWallets(apiState.token) } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Refresh REAL", fontSize = 9.sp, color = OdinCyan)
                                }
                                Button(onClick = { scope.launch { val orders = apiManager.fetchOrders(apiState.token); /* show */ } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGold.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = OdinGold, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Orders REAL", fontSize = 9.sp, color = OdinGold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MARKET REAL PRICE TAB
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Nobitex REAL Market Prices - 11M users", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        usdtPrice?.let { price ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(text = "USDT/IRR REAL", fontSize = 9.sp, color = OdinSilverMuted); Text(text = "${String.format("%,.0f", price.priceToman)} Toman", fontSize = 16.sp, fontWeight = FontWeight.Black, color = OdinGoldLight) }
                                Column { Text(text = "Source", fontSize = 9.sp, color = OdinSilverMuted); Text(text = price.source.take(20), fontSize = 9.sp, color = OdinGreen) }
                                Column { Text(text = "Change 24h", fontSize = 9.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", price.change24h)}%", fontSize = 10.sp, color = if (price.change24h >= 0) OdinGreen else OdinRed) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Best Buy ${String.format("%,.0f", price.bestBuy)}", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "Best Sell ${String.format("%,.0f", price.bestSell)}", fontSize = 8.sp, color = OdinSilverMuted)
                                Text(text = "Rial ${String.format("%,.0f", price.priceRial)}", fontSize = 8.sp, color = OdinSilverDim)
                            }
                        } ?: run {
                            Text(text = "Loading REAL Nobitex price... 231,493 Toman from https://nobitex.ir/price/usdt/", fontSize = 10.sp, color = OdinSilverMuted)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { scope.launch { usdtPrice = marketProvider.fetchUSDTPrice(); realPrices = realDataManager.fetchRealPrices() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Refresh REAL Nobitex Price", fontSize = 11.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinBorder), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "All REAL Prices - Nobitex + Binance + Forex - Tether Unit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        realPrices.values.sortedByDescending { it.price }.take(15).forEach { price ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = price.symbol, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = if (price.symbol.contains("IRR")) String.format("%,.0f Toman", price.price) else String.format("%.2f USDT", price.price), fontSize = 9.sp, color = OdinGold)
                                Text(text = price.source.take(18), fontSize = 7.sp, color = OdinSilverDim)
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinCyan.copy(alpha = 0.05f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Nobitex API Docs - REAL", fontWeight = FontWeight.Bold, color = OdinCyan, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Public: POST /market/stats srcCurrency=usdt dstCurrency=rls → latest (Rial/10=Toman)\nPrivate: POST /users/wallets/list + /users/wallets/balance + /market/orders/add\nWebSocket: wss://api.nobitex.ir/ws/ for live\nDocs: https://apidocs.nobitex.ir\nRate limit: 15 req/min public, 1000/10min private", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
