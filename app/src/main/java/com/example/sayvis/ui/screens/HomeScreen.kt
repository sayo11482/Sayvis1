package com.example.sayvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.i18n.ContextLocalization
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.ui.components.SayvisText
import com.example.sayvis.model.ContextSnapshot
import com.example.sayvis.model.Mission
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.ui.AvatarState
import com.example.sayvis.ui.SayvisScreen
import com.example.sayvis.ui.components.ActionProposalCard
import com.example.sayvis.ui.components.SayvisAvatar
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurface
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

private fun fmtQuote(v: Double): String = when {
    v >= 100000 -> "%.0f".format(v)
    v >= 1000 -> "%.1f".format(v)
    else -> "%.4f".format(v)
}

@Composable
fun HomeScreen(
    avatarState: AvatarState,
    visualStyle: com.example.sayvis.settings.AiVisualStyle = com.example.sayvis.settings.AiVisualStyle.GEOMETRIC,
    listenLevel: Float = 0f,
    contextSnapshot: ContextSnapshot,
    activeMission: Mission?,
    pendingOpportunities: List<AwareOpportunity>,
    isPersian: Boolean,
    emergencyLockActive: Boolean,
    persianDigits: Boolean = isPersian,
    onNavigate: (SayvisScreen) -> Unit,
    onSendMessage: (String) -> Unit,
    onApproveOpportunity: (String) -> Unit,
    onDismissOpportunity: (String) -> Unit,
    onToggleEmergencyLock: () -> Unit,
    marketSnapshot: com.example.sayvis.trading.MarketDataService.Snapshot? = null,
    online: Boolean = true,
    onToggleOnline: () -> Unit = {},
    speedText: String = "—",
    statusChipText: String = "",
    marketAgeSeconds: Long = -1L,
    onRefreshSpeed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var quickInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Top Status Bar: Emergency Lock & Online status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online/Offline & Gateway Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (contextSnapshot.isOnline) SayvisGreenSuccess else SayvisAmberWarning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ContextLocalization.network(contextSnapshot, isPersian),
                        fontSize = 11.5.sp,
                        color = SayvisSilverMuted
                    )
                }

                // Emergency Lock Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (emergencyLockActive) SayvisRedAlert.copy(alpha = 0.2f) else SayvisSurfaceVariant)
                        .clickable { onToggleEmergencyLock() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("emergency_lock_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (emergencyLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Emergency Lock",
                            tint = if (emergencyLockActive) SayvisRedAlert else SayvisSilverMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (emergencyLockActive) {
                                if (isPersian) "قفل اضطراری فعال" else "Locked"
                            } else {
                                if (isPersian) "امنیت عادی" else "Safe"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (emergencyLockActive) SayvisRedAlert else SayvisSilverMuted
                        )
                    }
                }
            }
        }

        // Center Hero: SAYVIS Avatar and State
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SayvisAvatar(state = avatarState, size = 110.dp, style = visualStyle, level = listenLevel)

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPersian) "سایو · پلتفرم هوش مصنوعی شخصی" else "SAYO · Personal AI Platform",
                    fontSize = 11.sp,
                    color = SayvisGold,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPersian) "سایویس" else "SAYVIS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Text(
                    text = if (isPersian) "لایه سیستم‌عامل هوش مصنوعی شخصی" else "Personal AI Operating Layer",
                    style = MaterialTheme.typography.bodySmall,
                    color = SayvisCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (emergencyLockActive) {
                        if (isPersian) "قفل اضطراری فعال است. ابزارهای حساس مسدود می‌باشند." else "Emergency Lock Engaged. High-risk execution blocked."
                    } else {
                        if (isPersian) "«چگونه می‌توانم در پیشبرد اهداف راهبردی شما سهیم باشم؟»" else "\"How can I augment your strategic agency today?\""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (emergencyLockActive) SayvisRedAlert else SayvisSilverMuted,
                    fontSize = 13.sp
                )
            }
        }

        // ===== LINK CENTER (v5.3.0): online/offline switch + live speed =====
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SayvisSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The owner-pressed FULL internet switch (real gate in SayvisNet).
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (online) SayvisGreenSuccess.copy(alpha = 0.18f) else SayvisRedAlert.copy(alpha = 0.22f))
                            .clickable { onToggleOnline() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("net_switch")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (online) SayvisGreenSuccess else SayvisRedAlert)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = if (online) (if (isPersian) "آنلاین — متصل" else "ONLINE — connected")
                                else (if (isPersian) "آفلاین — کل اتصال قطع" else "OFFLINE — links cut"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (online) SayvisGreenSuccess else SayvisRedAlert
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    // Live measured speed.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SayvisSurfaceVariant)
                            .clickable { onRefreshSpeed() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("net_speed"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = (if (online) speedText else "—") + (if (isPersian) " مگابیت/ث" else " Mbps"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SayvisSilver
                        )
                    }
                }
                if (statusChipText.isNotBlank()) {
                    Text(
                        text = statusChipText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SayvisGold,
                        modifier = Modifier.testTag("status_code_chip")
                    )
                }
                if (marketAgeSeconds >= 0) {
                    Text(
                        text = if (isPersian) "آخرین دیتای بازار: $marketAgeSeconds ثانیه پیش — هر ۳۰ ثانیه خودکار تازه می‌شود"
                        else "Last market data: $marketAgeSeconds s ago — auto-refreshes every 30 s",
                        fontSize = 9.5.sp,
                        color = SayvisSilverMuted,
                        modifier = Modifier.testTag("market_freshness")
                    )
                }
            }
        }

        // ===== LIVE FOREX/MARKET STRIP (v5.1.0) — real symbols, real data ====
        item {
            val snap = marketSnapshot
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SayvisSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = SayvisGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "مارکت لایو — فارکس و فلزات" else "Live market — forex & metals",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SayvisSilver,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SayvisGreenSuccess)
                    )
                }
                val quotes = snap?.quotes?.values?.take(4).orEmpty()
                if (quotes.isEmpty()) {
                    Text(
                        text = if (isPersian) "در حال دریافت نمادهای زنده…" else "Fetching live symbols…",
                        fontSize = 10.5.sp,
                        color = SayvisSilverMuted
                    )
                } else {
                    quotes.forEach { q ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q.symbol.labelFa,
                                fontSize = 11.5.sp,
                                color = SayvisSilver,
                                modifier = Modifier.weight(1f)
                            )
                            val change = q.change24hPercent
                            val changeColor = when {
                                change == null -> SayvisSilverMuted
                                change >= 0 -> SayvisGreenSuccess
                                else -> SayvisRedAlert
                            }
                            Text(
                                text = fmtQuote(q.price),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = change?.let { (if (it >= 0) "+" else "") + "%.2f%%".format(it) }
                                    ?: if (isPersian) "—" else "-",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = changeColor
                            )
                        }
                    }
                    // Mini sparkline of the first available series (live chart pulse).
                    val series = snap?.series?.values?.firstOrNull { it.size >= 10 }
                    if (series != null) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("home_market_spark")
                        ) {
                            val values = series.takeLast(60).map { it.toFloat() }
                            val minV = values.min()
                            val maxV = values.max()
                            val span = (maxV - minV).takeIf { it > 0f } ?: 1f
                            val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                            val up = values.last() >= values.first()
                            val lineColor = if (up) SayvisGreenSuccess else SayvisRedAlert
                            val path = androidx.compose.ui.graphics.Path()
                            values.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = size.height - ((v - minV) / span) * size.height
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(lineColor.copy(alpha = 0.5f), lineColor)
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // Quick Input prompt
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    placeholder = {
                        Text(
                            if (isPersian) "فرمان یا سؤال از سایویس..." else "Directive or inquiry to SAYVIS...",
                            color = SayvisSilverMuted.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SayvisCyan,
                        unfocusedBorderColor = SayvisBorder,
                        focusedContainerColor = SayvisSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = SayvisSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (quickInput.isNotBlank()) {
                            onSendMessage(quickInput)
                            quickInput = ""
                            onNavigate(SayvisScreen.ASSISTANT)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SayvisCyan)
                        .testTag("home_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black
                    )
                }
            }
        }

        // Active Mission Progress Card
        item {
            if (activeMission != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigate(SayvisScreen.MISSIONS) }
                        .testTag("home_mission_card"),
                    colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
                    border = BorderStroke(1.dp, SayvisBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = SayvisGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPersian) "مأموریت فعال" else "Active Strategic Mission",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SayvisGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = PersianFormat.percent(activeMission.progressPercent, persianDigits),
                                fontWeight = FontWeight.Bold,
                                color = SayvisCyan,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        SayvisText(
                            source = activeMission.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            markTranslated = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { activeMission.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SayvisCyan,
                            trackColor = SayvisSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                Text(
                                    text = if (isPersian) "مهلت: " else "Deadline: ",
                                    fontSize = 11.sp,
                                    color = SayvisSilverMuted
                                )
                                SayvisText(
                                    source = activeMission.deadline,
                                    fontSize = 11.sp,
                                    color = SayvisSilverMuted,
                                    markTranslated = true
                                )
                            }
                            Text(
                                text = if (isPersian) "مشاهده جزئیات →" else "View Tasks →",
                                fontSize = 11.sp,
                                color = SayvisCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavTile(
                    title = if (isPersian) "مأموریت‌ها" else "Missions",
                    subtitle = if (isPersian) "کارها و پیشرفت" else "Tasks & progress",
                    icon = Icons.Default.Flag,
                    tint = SayvisCyan,
                    modifier = Modifier.weight(1f).testTag("quick_tile_missions")
                ) { onNavigate(SayvisScreen.MISSIONS) }

                QuickNavTile(
                    title = if (isPersian) "درگاه ترید" else "Trading",
                    subtitle = if (isPersian) "اتصال متاتریدر" else "MetaTrader link",
                    icon = Icons.Default.SwapHoriz,
                    tint = SayvisGreenSuccess,
                    modifier = Modifier.weight(1f).testTag("quick_tile_gateway")
                ) { onNavigate(SayvisScreen.GATEWAY) }

                QuickNavTile(
                    title = if (isPersian) "اسکریپت‌ها" else "Scripts",
                    subtitle = if (isPersian) "خودکارسازی" else "Automation",
                    icon = Icons.Default.Code,
                    tint = SayvisGold,
                    modifier = Modifier.weight(1f).testTag("quick_tile_scripts")
                ) { onNavigate(SayvisScreen.SCRIPTS) }
            }
        }

        // Suggested Proactive Interventions (AWARE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = SayvisAmberWarning, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "فرصت‌های پیشنهادی AWARE" else "AWARE Proactive Interventions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = if (isPersian) "مدیریت همه" else "View All",
                    fontSize = 12.sp,
                    color = SayvisCyan,
                    modifier = Modifier.clickable { onNavigate(SayvisScreen.AWARE) }
                )
            }
        }

        if (pendingOpportunities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, SayvisBorder)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isPersian) "هیچ مداخله فوری مورد نیاز نیست. سیستم در وضعیت بهینه قرار دارد." else "All systems optimal. No pending interventions required.",
                            color = SayvisSilverMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(pendingOpportunities.take(2)) { opp ->
                ActionProposalCard(
                    opportunity = opp,
                    isPersian = isPersian,
                    onApprove = { onApproveOpportunity(opp.id) },
                    onDismiss = { onDismissOpportunity(opp.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickNavTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, SayvisBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
            Text(text = subtitle, fontSize = 10.sp, color = SayvisSilverMuted, maxLines = 1)
        }
    }
}
