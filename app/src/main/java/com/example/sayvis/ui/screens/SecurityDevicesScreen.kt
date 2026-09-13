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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Devices & Lock, 1: Audit Log

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
                        text = "${if (isPersian) "سیاهه ممیزی" else "Audit Log"} (${auditEvents.size})",
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
