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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.odin.agent.mt5.MT5WebViewGateway
import com.odin.agent.nobitex.NobitexApiManager
import com.odin.agent.nobitex.NobitexWebViewGateway
import com.odin.agent.trading.NobitexMarketProvider
import com.odin.agent.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingHubScreen(isPersian: Boolean) {
    var selectedPlatform by remember { mutableStateOf(0) } // 0=MT5, 1=Nobitex
    val mt5Gateway = remember { MT5WebViewGateway() }
    val nobitexWebGateway = remember { NobitexWebViewGateway() }
    val nobitexApiManager = remember { NobitexApiManager() }
    val marketProvider = remember { NobitexMarketProvider() }

    var mt5State by remember { mutableStateOf(mt5Gateway.state.value) }
    var nobitexWebState by remember { mutableStateOf(nobitexWebGateway.state.value) }
    var nobitexApiState by remember { mutableStateOf(nobitexApiManager.state.value) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(mt5Gateway.getDefaultUrl()) }
    var extractedIRT by remember { mutableStateOf(0.0) }
    var extractedUSDT by remember { mutableStateOf(0.0) }
    var extractedMT5Balance by remember { mutableStateOf(0.0) }
    var apiToken by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var usdtPrice by remember { mutableStateOf<com.odin.agent.trading.NobitexPrice?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            mt5State = mt5Gateway.state.value
            nobitexWebState = nobitexWebGateway.state.value
            nobitexApiState = nobitexApiManager.state.value
            try { usdtPrice = marketProvider.fetchUSDTPrice() } catch (e: Exception) {}
            delay(2000)
        }
    }

    LaunchedEffect(selectedPlatform) {
        currentUrl = if (selectedPlatform == 0) mt5Gateway.getDefaultUrl() else nobitexWebGateway.getDefaultUrl()
        webViewRef?.loadUrl(currentUrl)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(text = "odin metatrading - ${if (isPersian) "مرکز معاملات واقعی" else "Trading Hub REAL"}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(
                text = if (isPersian) "معاملات در اودین انجام می‌شود اما در بستر نوبیتکس یا متاتریدر ۵ اجرا می‌شود - تحلیل TradingView / بایتیکل"
                else "Trades in ODIN but executed on Nobitex or MT5 - Analysis TradingView / Byticle fallback",
                fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold, lineHeight = 11.sp
            )
        }

        item {
            TabRow(selectedTabIndex = selectedPlatform, containerColor = Color(0xFF0A0A0A), contentColor = OdinGold) {
                Tab(selected = selectedPlatform == 0, onClick = { selectedPlatform = 0 }, text = { Text(if (isPersian) "متاتریدر ۵ واقعی" else "MT5 REAL", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = selectedPlatform == 1, onClick = { selectedPlatform = 1 }, text = { Text(if (isPersian) "نوبیتکس واقعی" else "Nobitex REAL", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, OdinGold.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OdinGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isPersian) "معماری معاملات اودین - واقعی" else "ODIN Trading Architecture - REAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian) "• تحلیل: TradingView (اگر فیلتر بود بایتیکل - ایرانی مثل TradingView) ✅\n• تصمیم: ODIN AI با ۵ استراتژی + AWARE یادگیری ✅\n• اجرا: در بستر ${if (selectedPlatform == 0) "متاتریدر ۵ ویتاورس" else "نوبیتکس"} - معاملات اودین در MT5/Nobitex ✅\n• قیمت تتر واقعی ${usdtPrice?.priceToman?.let { String.format("%,.0f تومان", it) } ?: "۲۳۱,۴۹۳ تومان"} - واحد تتر ✅"
                        else "• Analysis: TradingView (if filtered Byticle - Iranian like TradingView) ✅\n• Decision: ODIN AI 5 strategies + AWARE ✅\n• Execution: On ${if (selectedPlatform == 0) "MT5 Vittaverse" else "Nobitex"} - ODIN trades on platform ✅\n• Tether REAL ${usdtPrice?.priceToman?.let { String.format("%,.0f Toman", it) } ?: "231,493 Toman"} - Unit Tether ✅",
                        fontSize = 9.sp, color = OdinSilver, lineHeight = 12.sp
                    )
                }
            }
        }

        if (selectedPlatform == 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (mt5State.isConnected) OdinGreen.copy(alpha = 0.4f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isPersian) "درگاه WebView واقعی MT5 - کپچا دستی - معاملات اودین در MT5" else "MT5 WebView REAL Gateway - Manual Captcha - ODIN trades on MT5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (mt5State.isConnected) OdinGreen.copy(alpha = 0.2f) else OdinGold.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (mt5State.isConnected) if (isPersian) "● متصل موجودی واقعی" else "● CONNECTED REAL BALANCE" else if (isPersian) "○ آماده لاگین" else "○ READY FOR LOGIN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = if (mt5State.isConnected) OdinGreen else OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = if (isPersian) "لاگین به صفحه اصلی متاتریدر ۵ - بعد اتصال معاملات اودین در بستر MT5 اجرا می‌شود - موجودی واقعی نمایش داده شود" else "Login to MT5 main page - after connect ODIN trades executed on MT5 platform - REAL balance must show", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { webViewRef?.reload() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "بارگذاری مجدد" else "Reload", fontSize = 9.sp, color = OdinCyan)
                            }
                            Button(onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "بازگشت" else "Back", fontSize = 9.sp, color = Color.White)
                            }
                            Button(onClick = { webViewRef?.let { wv -> mt5Gateway.extractBalanceFromPage(wv) { bal -> extractedMT5Balance = bal } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "موجودی" else "Balance", fontSize = 9.sp, color = OdinGreen)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "URL: ${mt5State.url.take(60)}", fontSize = 7.sp, color = OdinSilverDim, maxLines = 1)
                        if (mt5State.lastError != null) Text(text = "Error: ${mt5State.lastError}", fontSize = 8.sp, color = OdinRed)
                        if (mt5State.balance > 0 || extractedMT5Balance > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = if (isPersian) "موجودی واقعی بعد اتصال - حتما نمایش - معاملات اودین در MT5" else "Balance REAL after connect - Must show - ODIN trades on MT5", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text(text = if (isPersian) "بالانس واقعی" else "Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", if (extractedMT5Balance > 0) extractedMT5Balance else mt5State.balance)} USDT", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                        Column { Text(text = if (isPersian) "واحد" else "Unit", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "Tether", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                                        Column { Text(text = if (isPersian) "منبع" else "Source", fontSize = 8.sp, color = OdinSilverMuted); Text(text = if (isPersian) "MT5 واقعی" else "MT5 REAL", fontSize = 9.sp, color = OdinCyan) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().height(480.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            mt5Gateway.configureWebView(this)
                            webViewClient = mt5Gateway.createWebViewClient { newState -> mt5State = newState }
                            webChromeClient = mt5Gateway.createWebChromeClient { newState -> mt5State = newState }
                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    }, modifier = Modifier.fillMaxSize())
                }
                Text(text = if (isPersian) "WebView واقعی MT5 - ورود کپچا دستی پشتیبانی می‌شود - بعد لاگین موجودی واقعی استخراج می‌شود - معاملات اودین در بستر MT5" else "REAL MT5 WebView - Manual captcha supported - After login balance extracted - ODIN trades on MT5", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)), border = BorderStroke(1.dp, if (nobitexWebState.isConnected || extractedIRT > 0) OdinGreen.copy(alpha = 0.4f) else OdinGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isPersian) "درگاه WebView واقعی نوبیتکس - کپچا دستی - معاملات اودین در نوبیتکس" else "Nobitex WebView REAL - Captcha Manual - ODIN trades on Nobitex", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (nobitexWebState.isConnected || extractedIRT > 0) OdinGreen.copy(alpha = 0.2f) else OdinGold.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(text = if (nobitexWebState.isConnected || extractedIRT > 0) if (isPersian) "● متصل موجودی واقعی" else "● CONNECTED REAL BALANCE" else if (isPersian) "○ آماده لاگین" else "○ READY FOR LOGIN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = if (nobitexWebState.isConnected || extractedIRT > 0) OdinGreen else OdinGold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = if (isPersian) "لاگین به صفحه اصلی نوبیتکس https://nobitex.ir/panel/balance/spot/ - بعد اتصال معاملات اودین در بستر نوبیتکس اجرا می‌شود" else "Login to Nobitex main page https://nobitex.ir/panel/balance/spot/ - after connect ODIN trades executed on Nobitex", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { webViewRef?.reload() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinCyan.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = OdinCyan, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "بارگذاری مجدد" else "Reload", fontSize = 9.sp, color = OdinCyan)
                            }
                            Button(onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "بازگشت" else "Back", fontSize = 9.sp, color = Color.White)
                            }
                            Button(onClick = { webViewRef?.let { wv -> nobitexWebGateway.extractBalanceFromPage(wv) { irt, usdt -> extractedIRT = irt; extractedUSDT = usdt } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = if (isPersian) "موجودی" else "Balance", fontSize = 9.sp, color = OdinGreen)
                            }
                        }
                        if (extractedIRT > 0 || extractedUSDT > 0 || nobitexWebState.balanceIRT > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinGreen.copy(alpha = 0.08f)), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.4f)), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = if (isPersian) "موجودی واقعی بعد اتصال - معاملات اودین در نوبیتکس" else "Balance REAL after connect - ODIN trades on Nobitex", fontSize = 9.sp, color = OdinGreen, fontWeight = FontWeight.Bold)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column { Text(text = if (isPersian) "موجودی تومانی واقعی" else "IRT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%,.0f", if (extractedIRT > 0) extractedIRT else nobitexWebState.balanceIRT)} ${if (isPersian) "تومان" else "Toman"}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                        Column { Text(text = if (isPersian) "موجودی تتر واقعی" else "USDT Balance REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", if (extractedUSDT > 0) extractedUSDT else nobitexWebState.balanceUSDT)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                                        Column { Text(text = if (isPersian) "منبع" else "Source", fontSize = 8.sp, color = OdinSilverMuted); Text(text = if (isPersian) "نوبیتکس واقعی" else "Nobitex REAL", fontSize = 9.sp, color = OdinCyan) }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = OdinBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isPersian) "اتصال API واقعی نوبیتکس - توکن (اختیاری برای معامله خودکار)" else "Nobitex API REAL - Token (optional for auto trade)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (!nobitexApiState.isConnected) {
                            OutlinedTextField(value = apiToken, onValueChange = { apiToken = it }, label = { Text(if (isPersian) "توکن API نوبیتکس" else "Nobitex API Token", fontSize = 9.sp) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdinGold, unfocusedBorderColor = OdinBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White), singleLine = true, visualTransformation = if (showToken) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation())
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { if (!isConnecting) { isConnecting = true; scope.launch { nobitexApiManager.connect(apiToken); isConnecting = false } } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinGreen), shape = RoundedCornerShape(8.dp), enabled = apiToken.isNotBlank()) {
                                if (isConnecting) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp) else Text(text = if (isPersian) "اتصال واقعی API" else "Connect REAL API", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(text = if (isPersian) "موجودی تومانی API واقعی" else "IRT API REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%,.0f", nobitexApiState.totalIRT)} ${if (isPersian) "تومان" else "Toman"}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White) }
                                Column { Text(text = if (isPersian) "موجودی تتر API واقعی" else "USDT API REAL", fontSize = 8.sp, color = OdinSilverMuted); Text(text = "${String.format("%.2f", nobitexApiState.totalUSDT)} USDT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OdinGold) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { scope.launch { nobitexApiManager.disconnect() } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = OdinRed.copy(alpha = 0.15f)), border = BorderStroke(1.dp, OdinRed.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                                Text(text = if (isPersian) "قطع اتصال API" else "Disconnect API", color = OdinRed, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().height(480.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, OdinGreen.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            nobitexWebGateway.configureWebView(this)
                            webViewClient = nobitexWebGateway.createWebViewClient { newState -> nobitexWebState = newState }
                            webChromeClient = nobitexWebGateway.createWebChromeClient { newState -> nobitexWebState = newState }
                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    }, modifier = Modifier.fillMaxSize())
                }
                Text(text = if (isPersian) "WebView واقعی نوبیتکس - کپچا دستی - بعد لاگین موجودی استخراج می‌شود - معاملات اودین در بستر نوبیتکس اجرا می‌شود" else "REAL Nobitex WebView - Manual captcha - After login balance extracted - ODIN trades executed on Nobitex", fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OdinCyan.copy(alpha = 0.05f)), border = BorderStroke(1.dp, OdinCyan.copy(alpha = 0.2f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = if (isPersian) "تحلیل TradingView + بایتیکل (جایگزین ایرانی)" else "Analysis TradingView + Byticle (Iranian alternative)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OdinCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) "اگر TradingView فیلتر بود، بایتیکل https://byticle.com مانند TradingView ایرانی استفاده می‌شود - چارت حرفه‌ای کندل + اندیکاتور\nواحد واقعی هر نماد در چارت مشخص است - تتر برای کریپتو، تومان برای ریال، دلار برای فارکس"
                        else "If TradingView filtered, Byticle https://byticle.com like Iranian TradingView used - pro candle chart + indicators\nReal unit per symbol shown in chart - Tether for crypto, Toman for IRR, Dollar for forex",
                        fontSize = 8.sp, color = OdinSilverMuted, lineHeight = 10.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
