package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.sayvis.identity.DevicePairing
import com.example.sayvis.identity.OwnerAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.AuditEvent
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.ui.components.offlineTranslate
import com.example.sayvis.model.Device
import com.example.sayvis.model.DeviceType
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityDevicesScreen(
    devices: List<Device>,
    auditEvents: List<AuditEvent>,
    emergencyLockActive: Boolean,
    isPersian: Boolean,
    onToggleEmergencyLock: () -> Unit,
    onToggleDeviceTrust: (String, Boolean) -> Unit,
    onRevokeDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
    ownerAccount: OwnerAccount? = null,
    ownerSignedIn: Boolean = false,
    pairingOffer: DevicePairing.Offer? = null,
    accountMessage: String? = null,
    onClearAccountMessage: () -> Unit = {},
    onRegisterOwner: (String, String) -> Unit = { _, _ -> },
    onSignInOwner: (String, String) -> Unit = { _, _ -> },
    onSignOutOwner: () -> Unit = {},
    onChangePassword: (String, String) -> Unit = { _, _ -> },
    onStartPairing: () -> Unit = {},
    onCancelPairing: () -> Unit = {},
    onCompletePairing: (String, DeviceType, String, String) -> Unit = { _, _, _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Devices & Lock, 1: Account & Pairing, 2: Audit Log

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("security_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isPersian) "امنیت بدون اعتماد (Zero Trust) و دستگاه‌ها" else "Zero-Trust Security & Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isPersian) "قفل اضطراری، کنترل نشست‌های متصل و گزارش ممیزی" else "Emergency lock, mesh device pairing & cryptographic audit",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Emergency Lock Kill-Switch Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (emergencyLockActive) SayvisRedAlert.copy(alpha = 0.2f) else SayvisSurfaceVariant
            ),
            border = BorderStroke(1.dp, if (emergencyLockActive) SayvisRedAlert else SayvisBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (emergencyLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (emergencyLockActive) SayvisRedAlert else SayvisGreenSuccess
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (emergencyLockActive) {
                                if (isPersian) "قفل اضطراری فعال است" else "EMERGENCY LOCK ACTIVE"
                            } else {
                                if (isPersian) "وضعیت عملیاتی عادی (Zero Trust)" else "OPERATIONAL (Zero Trust)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = if (emergencyLockActive) SayvisRedAlert else SayvisGreenSuccess,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (emergencyLockActive) {
                            if (isPersian) "کلیه اقدامات خودکار، اتوماسیون‌ها و دسترسی‌های خارجی بلافاصله متوقف و مسدود شده‌اند." else "All agent executions, background automations and remote writes are suspended."
                        } else {
                            if (isPersian) "اقدامات پرخطر نیازمند تأیید صریح مالک هستند. در صورت خطر، قفل اضطراری را فعال کنید." else "High-risk actions require owner sign-off. Engage lock to freeze all agent operations."
                        },
                        fontSize = 11.sp,
                        color = SayvisSilverMuted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onToggleEmergencyLock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (emergencyLockActive) SayvisGreenSuccess else SayvisRedAlert,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("emergency_lock_toggle_btn")
                ) {
                    Text(
                        text = if (emergencyLockActive) (if (isPersian) "بازگشایی" else "Unlock") else (if (isPersian) "قفل اضطراری" else "Lockdown"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: Devices vs Audit Log
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SayvisSurface,
            contentColor = SayvisCyan,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "${if (isPersian) "دستگاه‌های متصل" else "Paired Devices"} (${devices.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isPersian) "حساب و جفت‌سازی" else "Account & Pairing",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = "${if (isPersian) "ممیزی" else "Audit"} (${auditEvents.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Devices List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        isPersian = isPersian,
                        onToggleTrust = { onToggleDeviceTrust(device.id, device.isTrusted) },
                        onRevoke = { onRevokeDevice(device.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        } else if (selectedTab == 1) {
            AccountPairingPane(
                isPersian = isPersian,
                account = ownerAccount,
                signedIn = ownerSignedIn,
                offer = pairingOffer,
                message = accountMessage,
                onClearMessage = onClearAccountMessage,
                onRegister = onRegisterOwner,
                onSignIn = onSignInOwner,
                onSignOut = onSignOutOwner,
                onChangePassword = onChangePassword,
                onStartPairing = onStartPairing,
                onCancelPairing = onCancelPairing,
                onCompletePairing = onCompletePairing,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            // Audit Log List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditEvents, key = { it.eventId }) { event ->
                    AuditEventCard(event = event, isPersian = isPersian)
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    isPersian: Boolean,
    onToggleTrust: () -> Unit,
    onRevoke: () -> Unit
) {
    val deviceIcon = when (device.type) {
        DeviceType.ANDROID_PHONE -> Icons.Default.PhoneAndroid
        DeviceType.WINDOWS_PC, DeviceType.SECURE_LAPTOP -> Icons.Default.Computer
        DeviceType.WEB_CLIENT, DeviceType.EDGE_GATEWAY -> Icons.Default.Public
        else -> Icons.Default.Computer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${device.id}"),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, if (device.isRevoked) SayvisRedAlert.copy(alpha = 0.5f) else SayvisBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(deviceIcon, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        SayvisText(
                            source = device.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp,
                            markTranslated = true
                        )
                        Text(
                            text = if (isPersian) device.type.labelFa else device.type.labelEn,
                            fontSize = 10.sp,
                            color = SayvisSilverMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            when {
                                device.isRevoked -> SayvisRedAlert.copy(alpha = 0.2f)
                                device.isTrusted -> SayvisGreenSuccess.copy(alpha = 0.2f)
                                else -> SayvisAmberWarning.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            device.isRevoked -> if (isPersian) "لغوشده" else "REVOKED"
                            device.isTrusted -> if (isPersian) "مورد اعتماد" else "TRUSTED"
                            else -> if (isPersian) "محدود" else "UNTRUSTED"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            device.isRevoked -> SayvisRedAlert
                            device.isTrusted -> SayvisGreenSuccess
                            else -> SayvisAmberWarning
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = if (isPersian) "کلید: " else "Key: ",
                    fontSize = 10.sp,
                    color = SayvisSilverMuted
                )
                Text(
                    text = device.publicKeyFingerprint,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SayvisSilverMuted
                )
            }

            if (device.capabilities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = if (isPersian) "قابلیت‌ها: " else "Capabilities: ",
                        fontSize = 10.sp,
                        color = SayvisCyan.copy(alpha = 0.8f)
                    )
                    device.capabilities.take(3).forEachIndexed { index, capability ->
                        if (index > 0) {
                            Text(
                                text = if (isPersian) "، " else ", ",
                                fontSize = 10.sp,
                                color = SayvisCyan.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = offlineTranslate(capability),
                            fontSize = 10.sp,
                            color = SayvisCyan.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!device.isRevoked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onToggleTrust,
                        border = BorderStroke(1.dp, SayvisBorder),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (device.isTrusted) (if (isPersian) "عدم اعتماد" else "Untrust") else (if (isPersian) "اعتماد" else "Trust"),
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onRevoke,
                        border = BorderStroke(1.dp, SayvisRedAlert.copy(alpha = 0.6f)),
                        modifier = Modifier.height(30.dp).testTag("revoke_dev_${device.id}")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = SayvisRedAlert, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "ابطال دسترسی" else "Revoke", fontSize = 10.sp, color = SayvisRedAlert)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditEventCard(event: AuditEvent, isPersian: Boolean) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.7f)),
        border = BorderStroke(0.5.dp, SayvisBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (event.result == "SUCCESS") SayvisGreenSuccess else SayvisRedAlert)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SayvisText(
                        source = event.action,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(text = dateStr, fontSize = 10.sp, color = SayvisSilverMuted)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row {
                    Text(text = offlineTranslate(event.actor), fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = " • ", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = offlineTranslate(event.authorization), fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(text = " • ", fontSize = 10.sp, color = SayvisSilverMuted)
                    Text(
                        text = event.payloadDigest,
                        fontSize = 10.sp,
                        color = SayvisSilverMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountPairingPane(
    isPersian: Boolean,
    account: OwnerAccount?,
    signedIn: Boolean,
    offer: DevicePairing.Offer?,
    message: String?,
    onClearMessage: () -> Unit,
    onRegister: (String, String) -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onStartPairing: () -> Unit,
    onCancelPairing: () -> Unit,
    onCompletePairing: (String, DeviceType, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf(account?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf(DeviceType.WINDOWS_PC) }
    var fingerprint by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var typeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(6000)
            onClearMessage()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SayvisCyan,
        unfocusedBorderColor = SayvisBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = SayvisCyan
    )

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (message != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SayvisCyan.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, SayvisCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(message, modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color.White)
            }
        }

        // ---- Owner account card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("owner_account_card"),
            colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
            border = BorderStroke(1.dp, SayvisBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "حساب مالک سایویس" else "SAYVIS Owner Account",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (isPersian)
                        "با همین ایمیل و رمز عبور روی رایانه و سایر دستگاه‌ها وارد می‌شوید. رمز عبور هرگز ذخیره نمی‌شود؛ فقط اثر PBKDF2 آن در Keystore نگهداری می‌شود."
                    else
                        "Use the same e-mail and password on your PC and other devices. The password itself is never stored; only a PBKDF2 verifier inside the Keystore.",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )

                if (account != null && signedIn) {
                    Text(
                        text = (if (isPersian) "وارد شده: " else "Signed in: ") + account.email,
                        fontSize = 12.sp,
                        color = SayvisGreenSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = (if (isPersian) "شناسهٔ حساب: " else "Account ID: ") + account.accountId,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SayvisSilverMuted
                    )
                    Text(
                        text = (if (isPersian) "اثر انگشت این گوشی: " else "This phone's fingerprint: ") + account.thisDeviceFingerprint,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SayvisSilverMuted
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isPersian) "رمز فعلی" else "Current password", fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(if (isPersian) "رمز جدید" else "New password", fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onChangePassword(password, newPassword); password = ""; newPassword = "" },
                            border = BorderStroke(1.dp, SayvisBorder)
                        ) { Text(if (isPersian) "تغییر رمز" else "Change password", fontSize = 11.sp, color = Color.White) }
                        OutlinedButton(
                            onClick = onSignOut,
                            border = BorderStroke(1.dp, SayvisAmberWarning.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("owner_sign_out_btn")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = SayvisAmberWarning, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPersian) "خروج" else "Sign out", fontSize = 11.sp, color = SayvisAmberWarning)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (isPersian) "ایمیل" else "E-mail", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("owner_email_field")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isPersian) "رمز عبور" else "Password", fontSize = 11.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("owner_password_field")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (account == null) {
                            Button(
                                onClick = { onRegister(email, password); password = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                                modifier = Modifier.testTag("owner_register_btn")
                            ) { Text(if (isPersian) "ساخت حساب مالک" else "Create owner account", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        } else {
                            Button(
                                onClick = { onSignIn(email, password); password = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                                modifier = Modifier.testTag("owner_sign_in_btn")
                            ) { Text(if (isPersian) "ورود" else "Sign in", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        // ---- Pairing card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("pairing_card"),
            colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
            border = BorderStroke(1.dp, SayvisBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = SayvisCyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) "جفت‌سازی رایانه / دستگاه جدید" else "Pair a PC / new device",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = if (isPersian)
                        "۱) روی گوشی «ایجاد کد جفت‌سازی» را بزنید. ۲) روی رایانه با همان ایمیل و رمز وارد شوید و کد را وارد کنید؛ رایانه یک «کد تأیید» می‌سازد. ۳) اثر انگشت و کد تأیید رایانه را این‌جا وارد کنید. دستگاه ابتدا «محدود» است تا شما اعتماد را اعطا کنید. کد ۵ دقیقه اعتبار دارد."
                    else
                        "1) Tap “Create pairing code” on the phone. 2) On the PC sign in with the same e-mail/password and enter the code; the PC produces a proof. 3) Enter the PC's fingerprint and proof here. The device starts UNTRUSTED until you grant trust. Codes expire after 5 minutes.",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )

                if (!signedIn) {
                    Text(
                        text = if (isPersian) "برای جفت‌سازی ابتدا وارد حساب شوید." else "Sign in first to pair devices.",
                        fontSize = 11.sp,
                        color = SayvisAmberWarning
                    )
                } else if (offer == null) {
                    Button(
                        onClick = onStartPairing,
                        colors = ButtonDefaults.buttonColors(containerColor = SayvisCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("pairing_start_btn")
                    ) { Text(if (isPersian) "ایجاد کد جفت‌سازی" else "Create pairing code", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SayvisSurface)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = offer.sharedPayload(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = SayvisGold
                            )
                            Text(
                                text = if (isPersian) "این کد را روی رایانه وارد کنید" else "Enter this code on the PC",
                                fontSize = 10.sp,
                                color = SayvisSilverMuted
                            )
                        }
                    }
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text(if (isPersian) "نام دستگاه" else "Device name", fontSize = 11.sp) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(onClick = { typeMenu = true }, border = BorderStroke(1.dp, SayvisBorder)) {
                            Text(
                                (if (isPersian) "نوع: " else "Type: ") + (if (isPersian) deviceType.labelFa else deviceType.labelEn),
                                fontSize = 11.sp, color = Color.White
                            )
                        }
                        DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                            DeviceType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(if (isPersian) t.labelFa else t.labelEn) },
                                    onClick = { deviceType = t; typeMenu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = fingerprint,
                        onValueChange = { fingerprint = it },
                        label = { Text(if (isPersian) "اثر انگشت دستگاه (SHA256)" else "Device fingerprint (SHA256)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("pairing_fingerprint_field")
                    )
                    OutlinedTextField(
                        value = proof,
                        onValueChange = { proof = it },
                        label = { Text(if (isPersian) "کد تأیید دستگاه" else "Device proof", fontSize = 11.sp) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("pairing_proof_field")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onCompletePairing(deviceName, deviceType, fingerprint, proof)
                                fingerprint = ""; proof = ""; deviceName = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SayvisGreenSuccess, contentColor = Color.Black),
                            modifier = Modifier.testTag("pairing_complete_btn")
                        ) { Text(if (isPersian) "تأیید جفت‌سازی" else "Confirm pairing", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        TextButton(onClick = onCancelPairing) {
                            Text(if (isPersian) "لغو" else "Cancel", fontSize = 12.sp, color = SayvisRedAlert)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
