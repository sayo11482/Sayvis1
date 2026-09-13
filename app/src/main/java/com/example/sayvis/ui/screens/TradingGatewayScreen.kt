package com.example.sayvis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.settings.MtAccountType
import com.example.sayvis.settings.MtBridgeKind
import com.example.sayvis.settings.MtGatewayProfile
import com.example.sayvis.settings.MtTerminalVersion
import com.example.sayvis.settings.TradingExecutionMode
import com.example.sayvis.trading.MtConnectionPhase
import com.example.sayvis.trading.MtGatewayState
import com.example.sayvis.trading.MtOrderRequest
import com.example.sayvis.trading.MtOrderResult
import com.example.sayvis.trading.MtOrderSide
import com.example.sayvis.trading.MtPosition
import com.example.sayvis.trading.MtQuote
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.components.SayvisConfirmDialog
import com.example.sayvis.ui.components.SayvisDivider
import com.example.sayvis.ui.components.SayvisField
import com.example.sayvis.ui.components.SayvisLinkButton
import com.example.sayvis.ui.components.SayvisOptionRow
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.components.SayvisStatRow
import com.example.sayvis.ui.components.SayvisStatusPill
import com.example.sayvis.ui.components.SayvisToggleRow
import com.example.sayvis.ui.components.pnlColor
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * MetaTrader 4/5 connection panel.
 *
 * The owner configures a bridge profile, connects, then sees the real account, market
 * watch and open positions. Order routing is gated twice — by the execution mode (paper
 * by default) and by the emergency lock — and the live level can only be reached through
 * an explicit re-confirmation dialog that falls back to demo when dismissed.
 */
@Composable
fun TradingGatewayScreen(
    profile: MtGatewayProfile,
    state: MtGatewayState,
    lastOrder: MtOrderResult?,
    isBusy: Boolean,
    emergencyLockActive: Boolean,
    persianDigits: Boolean,
    isPersian: Boolean,
    onConnect: (MtGatewayProfile) -> Unit,
    onDisconnect: () -> Unit,
    onTest: (MtGatewayProfile) -> Unit,
    onSaveProfile: (MtGatewayProfile) -> Unit,
    onRefresh: () -> Unit,
    onExecutionModeChange: (TradingExecutionMode) -> Unit,
    onPlaceOrder: (MtOrderRequest) -> Unit,
    onClosePosition: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current

    // The form edits a local copy so typing never hammers the persistence layer.
    var form by remember(profile) { mutableStateOf(profile) }
    var revealSecret by remember { mutableStateOf(false) }
    var showLiveWarning by remember { mutableStateOf(false) }
    var showHowTo by remember { mutableStateOf(false) }

    // Order ticket
    var orderSymbol by remember { mutableStateOf(state.quotes.firstOrNull()?.symbol ?: "EURUSD") }
    var orderSide by remember { mutableStateOf(MtOrderSide.BUY) }
    var orderVolume by remember { mutableStateOf("0.10") }
    var orderStop by remember { mutableStateOf("") }
    var orderTarget by remember { mutableStateOf("") }

    val banner = statusBanner(state, isPersian)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("gateway_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SayvisGreenSuccess, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = s.gatewayTitle, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Text(text = s.gatewaySubtitle, fontSize = 10.5.sp, color = SayvisSilverMuted)
                }
                SayvisStatusPill(text = state.phaseLabel(isPersian), color = phaseColor(state.phase))
            }
            SayvisLinkButton(
                label = s.gatewayHowTo,
                onClick = { showHowTo = true },
                modifier = Modifier.testTag("mt_howto")
            )
        }

        item {
            SayvisCard(borderColor = banner.second.copy(alpha = 0.45f), containerColor = banner.second.copy(alpha = 0.08f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(banner.first, contentDescription = null, tint = banner.second, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = banner.third, fontSize = 11.5.sp, color = banner.second)
                }
            }
        }

        // ------------------------------------------------------------ profile
        item {
            SayvisSectionHeader(title = if (isPersian) "پروفایل اتصال" else "Connection profile")
            SayvisCard {
                SayvisOptionRow(
                    label = s.bridgeKind,
                    options = MtBridgeKind.entries.map { it.label(isPersian) },
                    selectedIndex = MtBridgeKind.entries.indexOf(form.bridgeKind),
                    onSelect = { form = form.copy(bridgeKind = MtBridgeKind.entries[it]) }
                )
                SayvisOptionRow(
                    label = s.terminalVersion,
                    options = MtTerminalVersion.entries.map { it.label(isPersian) },
                    selectedIndex = MtTerminalVersion.entries.indexOf(form.terminalVersion),
                    onSelect = { form = form.copy(terminalVersion = MtTerminalVersion.entries[it]) }
                )
                SayvisOptionRow(
                    label = s.accountType,
                    options = MtAccountType.entries.map { it.label(isPersian) },
                    selectedIndex = MtAccountType.entries.indexOf(form.accountType),
                    onSelect = { form = form.copy(accountType = MtAccountType.entries[it]) }
                )

                SayvisDivider()

                SayvisField(
                    label = s.broker,
                    value = form.brokerName,
                    onValueChange = { form = form.copy(brokerName = it) },
                    hint = if (isPersian) "مثلاً IC Markets" else "e.g. IC Markets"
                )
                SayvisField(
                    label = s.server,
                    value = form.serverAddress,
                    onValueChange = { form = form.copy(serverAddress = it.trim()) },
                    hint = "ICMarkets-Demo"
                )
                SayvisField(
                    label = s.login,
                    value = form.login,
                    onValueChange = { input -> form = form.copy(login = input.filter { it.isDigit() }) },
                    hint = "51234567",
                    keyboardType = KeyboardType.Number
                )
                SayvisField(
                    label = s.password,
                    value = form.password,
                    onValueChange = { form = form.copy(password = it) },
                    hint = if (isPersian) "رمز حساب یا سرمایه‌گذار" else "Account or investor password",
                    isSecret = true,
                    revealSecret = revealSecret,
                    trailing = {
                        Text(
                            text = if (revealSecret) s.hideKey else s.showKey,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGold,
                            modifier = Modifier
                                .clickable { revealSecret = !revealSecret }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("mt_reveal_password")
                        )
                    }
                )

                SayvisDivider()

                when (form.bridgeKind) {
                    MtBridgeKind.METAAPI -> {
                        SayvisField(
                            label = s.bridgeToken,
                            value = form.bridgeToken,
                            onValueChange = { form = form.copy(bridgeToken = it.trim()) },
                            hint = if (isPersian) "توکن حساب MetaApi" else "MetaApi account token",
                            monospace = true
                        )
                        SayvisField(
                            label = s.bridgeUrl,
                            value = form.bridgeUrl,
                            onValueChange = { form = form.copy(bridgeUrl = it.trim()) },
                            hint = com.example.sayvis.trading.MetaTraderGateway.METAAPI_BASE,
                            keyboardType = KeyboardType.Uri,
                            monospace = true
                        )
                    }

                    MtBridgeKind.SELF_HOSTED -> {
                        SayvisField(
                            label = s.bridgeUrl,
                            value = form.bridgeUrl,
                            onValueChange = { form = form.copy(bridgeUrl = it.trim()) },
                            hint = "https://192.168.1.20:8443/sayvis",
                            keyboardType = KeyboardType.Uri,
                            monospace = true
                        )
                        SayvisField(
                            label = s.bridgeToken,
                            value = form.bridgeToken,
                            onValueChange = { form = form.copy(bridgeToken = it.trim()) },
                            hint = if (isPersian) "کلید اشتراکی پل (اختیاری)" else "Shared bridge secret (optional)",
                            isSecret = true,
                            revealSecret = revealSecret,
                            monospace = true
                        )
                    }

                    MtBridgeKind.OFFLINE_SIM -> Text(
                        text = if (isPersian) {
                            "در حالت شبیه‌ساز محلی هیچ اتصال شبکه‌ای برقرار نمی‌شود؛ حساب، قیمت‌ها و پوزیشن‌ها ساختگی‌اند تا بتوانید بدون ریسک کار با درگاه را تمرین کنید."
                        } else {
                            "Local-simulator mode makes no network call. Account, prices and positions are synthetic so you can practise the gateway without risk."
                        },
                        fontSize = 11.sp,
                        color = SayvisSilverMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SayvisButton(
                        label = s.save,
                        onClick = { onSaveProfile(form) },
                        modifier = Modifier.weight(1f),
                        tone = ButtonTone.NEUTRAL
                    )
                    SayvisButton(
                        label = s.testGateway,
                        onClick = { onTest(form) },
                        modifier = Modifier.weight(1f).testTag("mt_test_button"),
                        busy = isBusy,
                        tone = ButtonTone.GOLD
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isConnected) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SayvisButton(
                            label = s.disconnect,
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            tone = ButtonTone.DANGER,
                            icon = Icons.Default.Close
                        )
                        SayvisButton(
                            label = s.refresh,
                            onClick = onRefresh,
                            modifier = Modifier.weight(1f),
                            busy = isBusy,
                            tone = ButtonTone.PRIMARY,
                            icon = Icons.Default.Refresh
                        )
                    }
                } else {
                    SayvisButton(
                        label = s.connect,
                        onClick = {
                            onSaveProfile(form)
                            onConnect(form)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("mt_connect_button"),
                        busy = isBusy,
                        tone = ButtonTone.SUCCESS,
                        icon = Icons.Default.SwapHoriz
                    )
                }
            }
        }

        // ----------------------------------------------------- execution mode
        item {
            SayvisSectionHeader(title = s.executionMode, subtitle = s.executionModeHint)
            SayvisCard(borderColor = executionColor(form.executionMode).copy(alpha = 0.55f)) {
                SayvisOptionRow(
                    label = s.executionMode,
                    options = TradingExecutionMode.entries.map { it.label(isPersian) },
                    selectedIndex = TradingExecutionMode.entries.indexOf(form.executionMode),
                    onSelect = { index ->
                        val chosen = TradingExecutionMode.entries[index]
                        form = form.copy(executionMode = chosen)
                        onSaveProfile(form)
                        if (chosen == TradingExecutionMode.LIVE_EXECUTION) showLiveWarning = true
                        else onExecutionModeChange(chosen)
                    }
                )
                if (form.executionMode == TradingExecutionMode.LIVE_EXECUTION) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SayvisRedAlert, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = s.liveWarningBody, fontSize = 10.5.sp, color = SayvisRedAlert)
                    }
                }
            }
        }

        // ------------------------------------------------------- risk limits
        item {
            SayvisSectionHeader(title = if (isPersian) "محدودیت‌های ریسک" else "Risk limits")
            SayvisCard {
                SayvisField(
                    label = s.maxDailyLoss,
                    value = form.maxDailyLossUsd.toString(),
                    onValueChange = { input -> input.toDoubleOrNull()?.let { form = form.copy(maxDailyLossUsd = it) } },
                    keyboardType = KeyboardType.Decimal
                )
                SayvisField(
                    label = s.maxLot,
                    value = form.maxLotSize.toString(),
                    onValueChange = { input -> input.toDoubleOrNull()?.let { form = form.copy(maxLotSize = it) } },
                    keyboardType = KeyboardType.Decimal
                )
                SayvisToggleRow(
                    label = s.autoCloseOnDrawdown,
                    hint = null,
                    checked = form.autoCloseOnDrawdown,
                    onCheckedChange = { form = form.copy(autoCloseOnDrawdown = it) }
                )
                Spacer(modifier = Modifier.height(6.dp))
                SayvisButton(
                    label = s.save,
                    onClick = { onSaveProfile(form) },
                    modifier = Modifier.fillMaxWidth(),
                    tone = ButtonTone.NEUTRAL
                )
            }
        }

        // ------------------------------------------------------ account info
        item {
            SayvisSectionHeader(title = s.accountInfo)
            val account = state.account
            if (account == null) {
                SayvisCard {
                    Text(
                        text = if (isPersian) "برای دیدن اطلاعات حساب، ابتدا متصل شوید." else "Connect to see account details.",
                        fontSize = 11.5.sp,
                        color = SayvisSilverMuted
                    )
                }
            } else {
                SayvisCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${account.broker.ifBlank { form.brokerName }} · ${account.server.ifBlank { form.serverAddress }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilver,
                            modifier = Modifier.weight(1f)
                        )
                        SayvisStatusPill(
                            text = when {
                                account.isSimulated -> if (isPersian) "شبیه‌سازی" else "Simulated"
                                account.isDemo -> if (isPersian) "دمو" else "Demo"
                                else -> if (isPersian) "واقعی" else "Real"
                            },
                            color = when {
                                account.isSimulated -> SayvisCyan
                                account.isDemo -> SayvisGreenSuccess
                                else -> SayvisRedAlert
                            }
                        )
                    }
                    SayvisDivider()
                    SayvisStatRow(
                        label = "${s.login}: ${PersianFormat.digits(account.login, persianDigits)}",
                        value = "${s.leverage} 1:${PersianFormat.digits(account.leverage.toString(), persianDigits)}"
                    )
                    SayvisStatRow(label = s.balance, value = PersianFormat.money(account.balance, account.currency, persianDigits))
                    SayvisStatRow(
                        label = s.equity,
                        value = PersianFormat.money(account.equity, account.currency, persianDigits),
                        valueColor = SayvisGold
                    )
                    SayvisStatRow(
                        label = if (isPersian) "سود/زیان شناور" else "Floating P/L",
                        value = PersianFormat.money(account.floatingPnl, account.currency, persianDigits),
                        valueColor = pnlColor(account.floatingPnl)
                    )
                    SayvisStatRow(label = s.margin, value = PersianFormat.money(account.margin, account.currency, persianDigits))
                    SayvisStatRow(label = s.freeMargin, value = PersianFormat.money(account.freeMargin, account.currency, persianDigits))
                }
            }
        }

        // ----------------------------------------------------- market watch
        item {
            SayvisSectionHeader(
                title = s.marketWatch,
                subtitle = if (isPersian) "برای انتخاب نماد، روی ردیف بزنید" else "Tap a row to select the symbol"
            )
            if (state.quotes.isEmpty()) {
                SayvisCard {
                    Text(
                        text = if (isPersian) "دادهٔ بازار در دسترس نیست." else "No market data.",
                        fontSize = 11.5.sp,
                        color = SayvisSilverMuted
                    )
                }
            }
        }
        items(state.quotes, key = { it.symbol }) { quote ->
            QuoteRow(
                quote = quote,
                persianDigits = persianDigits,
                isPersian = isPersian,
                selected = quote.symbol == orderSymbol,
                onPick = { orderSymbol = quote.symbol }
            )
        }

        // ------------------------------------------------------- positions
        item {
            SayvisSectionHeader(
                title = s.openPositions,
                subtitle = PersianFormat.digits(state.positions.size.toString(), persianDigits)
            )
            if (state.positions.isEmpty()) {
                SayvisCard {
                    Text(text = s.noPositions, fontSize = 11.5.sp, color = SayvisSilverMuted)
                }
            }
        }
        items(state.positions, key = { it.ticket }) { position ->
            PositionRow(
                position = position,
                persianDigits = persianDigits,
                isPersian = isPersian,
                canClose = state.isConnected && form.executionMode != TradingExecutionMode.PAPER_SIMULATION,
                onClose = { onClosePosition(position.ticket) }
            )
        }

        // ---------------------------------------------------- order ticket
        item {
            SayvisSectionHeader(title = s.openOrder)
            SayvisCard {
                SayvisField(
                    label = s.symbol,
                    value = orderSymbol,
                    onValueChange = { orderSymbol = it.trim().uppercase() },
                    hint = "EURUSD"
                )
                SayvisOptionRow(
                    label = s.orderType,
                    options = MtOrderSide.entries.map { it.label(isPersian) },
                    selectedIndex = MtOrderSide.entries.indexOf(orderSide),
                    onSelect = { orderSide = MtOrderSide.entries[it] }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SayvisField(
                        label = s.volume,
                        value = orderVolume,
                        onValueChange = { orderVolume = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    SayvisField(
                        label = if (isPersian) "حد ضرر" else "Stop loss",
                        value = orderStop,
                        onValueChange = { orderStop = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    SayvisField(
                        label = if (isPersian) "حد سود" else "Take profit",
                        value = orderTarget,
                        onValueChange = { orderTarget = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }

                when {
                    emergencyLockActive -> InlineWarning(icon = Icons.Default.Warning, color = SayvisRedAlert, text = s.orderBlockedLock)
                    form.executionMode == TradingExecutionMode.PAPER_SIMULATION ->
                        InlineWarning(icon = Icons.Default.Warning, color = SayvisAmberWarning, text = s.orderBlockedPaper)
                }

                lastOrder?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.detail(isPersian),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (result.accepted) SayvisGreenSuccess else SayvisRedAlert
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SayvisButton(
                    label = if (orderSide == MtOrderSide.BUY) s.buy else s.sell,
                    onClick = {
                        onPlaceOrder(
                            MtOrderRequest(
                                symbol = orderSymbol,
                                side = orderSide,
                                volume = orderVolume.toDoubleOrNull() ?: 0.0,
                                stopLoss = orderStop.toDoubleOrNull(),
                                takeProfit = orderTarget.toDoubleOrNull()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("mt_place_order"),
                    enabled = !emergencyLockActive && state.isConnected,
                    tone = if (orderSide == MtOrderSide.BUY) ButtonTone.SUCCESS else ButtonTone.DANGER
                )
            }
        }

        item {
            Text(
                text = if (isPersian) {
                    "یادداشت فنی: اندروید نمی‌تواند مستقیماً پروتکل متاتریدر را صحبت کند، بنابراین اتصال همیشه از راه یک پل انجام می‌شود — " +
                        "یا سرویس ابری MetaApi، یا پل شخصی شما با Expert Advisor روی یک VPS. نشانی و توکن پل را در بالا وارد کنید."
                } else {
                    "Technical note: Android cannot speak the MetaTrader wire protocol directly, so a connection always goes through a bridge — " +
                        "either the MetaApi cloud service or your own Expert Advisor bridge on a VPS. Enter the bridge address and token above."
                },
                fontSize = 10.sp,
                color = SayvisSilverMuted,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showLiveWarning) {
        SayvisConfirmDialog(
            title = s.liveWarningTitle,
            body = s.liveWarningBody,
            confirmLabel = s.iUnderstand,
            dismissLabel = s.cancel,
            danger = true,
            onConfirm = {
                onExecutionModeChange(TradingExecutionMode.LIVE_EXECUTION)
                showLiveWarning = false
            },
            onDismiss = {
                // Refuse to escalate silently: drop back to demo execution.
                form = form.copy(executionMode = TradingExecutionMode.DEMO_EXECUTION)
                onSaveProfile(form)
                onExecutionModeChange(TradingExecutionMode.DEMO_EXECUTION)
                showLiveWarning = false
            }
        )
    }

    if (showHowTo) {
        SayvisConfirmDialog(
            title = s.gatewayHowTo,
            body = if (isPersian) {
                "۱) نزد کارگزاری خود حساب باز کنید و نام سرور و شمارهٔ حساب را یادداشت کنید.\n" +
                    "۲) یک پل انتخاب کنید: سرویس ابری MetaApi (ساده‌ترین راه) یا پل شخصی با Expert Advisor روی VPS.\n" +
                    "۳) توکن یا نشانی پل را همراه با سرور، شمارهٔ حساب و رمز وارد کنید.\n" +
                    "۴) «آزمون درگاه» را بزنید؛ اگر اطلاعات حساب برگشت، اتصال برقرار است.\n" +
                    "۵) تا وقتی سطح اجرا روی «شبیه‌سازی کاغذی» است، هیچ سفارشی به بروکر ارسال نمی‌شود."
            } else {
                "1) Open an account with your broker and note the server name and login.\n" +
                    "2) Choose a bridge: the MetaApi cloud service (simplest) or your own Expert Advisor bridge on a VPS.\n" +
                    "3) Enter the bridge token or URL together with the server, login and password.\n" +
                    "4) Tap “Test gateway”; if the account comes back, the link works.\n" +
                    "5) While the execution level is paper simulation, no order is ever routed to the broker."
            },
            confirmLabel = s.ok,
            dismissLabel = s.close,
            onConfirm = { showHowTo = false },
            onDismiss = { showHowTo = false }
        )
    }
}

private fun phaseColor(phase: MtConnectionPhase): Color = when (phase) {
    MtConnectionPhase.CONNECTED -> SayvisGreenSuccess
    MtConnectionPhase.SIMULATED -> SayvisCyan
    MtConnectionPhase.CONNECTING, MtConnectionPhase.AUTHENTICATING -> SayvisAmberWarning
    MtConnectionPhase.ERROR -> SayvisRedAlert
    MtConnectionPhase.DISCONNECTED -> SayvisSilverMuted
}

private fun executionColor(mode: TradingExecutionMode): Color = when (mode) {
    TradingExecutionMode.PAPER_SIMULATION -> SayvisGreenSuccess
    TradingExecutionMode.DEMO_EXECUTION -> SayvisAmberWarning
    TradingExecutionMode.LIVE_EXECUTION -> SayvisRedAlert
}

private fun statusBanner(state: MtGatewayState, isPersian: Boolean): Triple<ImageVector, Color, String> = when {
    state.phase == MtConnectionPhase.ERROR ->
        Triple(Icons.Default.ErrorOutline, SayvisRedAlert, state.lastError(isPersian).ifBlank { if (isPersian) "خطای اتصال" else "Connection error" })
    state.phase == MtConnectionPhase.SIMULATED ->
        Triple(Icons.Default.Warning, SayvisAmberWarning, state.message(isPersian))
    state.isConnected ->
        Triple(Icons.Default.CheckCircle, SayvisGreenSuccess, state.message(isPersian))
    state.phase == MtConnectionPhase.CONNECTING || state.phase == MtConnectionPhase.AUTHENTICATING ->
        Triple(Icons.Default.Refresh, SayvisAmberWarning, if (isPersian) "در حال اتصال…" else "Connecting…")
    else ->
        Triple(Icons.Default.SwapHoriz, SayvisSilverMuted, if (isPersian) "هنوز به ترمینالی متصل نشده‌اید." else "Not connected to any terminal yet.")
}

@Composable
private fun InlineWarning(icon: ImageVector, color: Color, text: String) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 10.5.sp, color = color)
    }
}

@Composable
private fun QuoteRow(quote: MtQuote, persianDigits: Boolean, isPersian: Boolean, selected: Boolean, onPick: () -> Unit) {
    val s = LocalStrings.current
    val up = quote.changePercent >= 0
    SayvisCard(borderColor = if (selected) SayvisCyan.copy(alpha = 0.6f) else SayvisBorder) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick() }
                .testTag("quote_${quote.symbol}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = quote.symbol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = s.bid, fontSize = 9.sp, color = SayvisSilverMuted)
                    Text(
                        text = PersianFormat.number(quote.bid, quote.digits, persianDigits),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisRedAlert
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = s.ask, fontSize = 9.sp, color = SayvisSilverMuted)
                    Text(
                        text = PersianFormat.number(quote.ask, quote.digits, persianDigits),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisGreenSuccess
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (isPersian) "تغییر" else "Change", fontSize = 9.sp, color = SayvisSilverMuted)
                    Text(
                        text = PersianFormat.percent(quote.changePercent / 100.0, 2, persianDigits),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (up) SayvisGreenSuccess else SayvisRedAlert
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionRow(
    position: MtPosition,
    persianDigits: Boolean,
    isPersian: Boolean,
    canClose: Boolean,
    onClose: () -> Unit
) {
    val s = LocalStrings.current
    SayvisCard(borderColor = pnlColor(position.profit).copy(alpha = 0.4f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SayvisStatusPill(
                        text = position.side.label(isPersian),
                        color = if (position.side == MtOrderSide.BUY) SayvisGreenSuccess else SayvisRedAlert
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = position.symbol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${PersianFormat.number(position.volume, 2, persianDigits)} ${if (isPersian) "لات" else "lots"}",
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${if (isPersian) "گشایش" else "Open"} ${PersianFormat.number(position.openPrice, 5, persianDigits)} → " +
                        PersianFormat.number(position.currentPrice, 5, persianDigits),
                    fontSize = 10.5.sp,
                    color = SayvisSilverMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianFormat.money(position.profit, "USD", persianDigits),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor(position.profit)
                )
                if (canClose) {
                    SayvisButton(label = s.closePosition, onClick = onClose, tone = ButtonTone.NEUTRAL)
                }
            }
        }
    }
}
