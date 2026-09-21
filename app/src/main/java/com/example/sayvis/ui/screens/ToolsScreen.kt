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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.i18n.PersianFormat
import com.example.sayvis.ui.SayvisScreen
import com.example.sayvis.ui.components.SayvisSectionHeader
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGold
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

/**
 * One screen holding every capability, grouped into four practical categories.
 *
 * This replaces the old seven-item bottom bar: the bar now carries only Home /
 * Assistant / Tools / Settings, and the concrete features are discovered here where
 * there is room to explain what each one is for.
 */
data class ToolCounts(
    val missions: Int = 0,
    val opportunities: Int = 0,
    val uicAttributes: Int = 0,
    val devices: Int = 0,
    val scripts: Int = 0,
    val gatewayConnected: Boolean = false,
    val persianDigits: Boolean = true
)

private data class ToolEntry(
    val screen: SayvisScreen,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String
)

@Composable
fun ToolsScreen(
    counts: ToolCounts,
    isPersian: Boolean,
    onNavigate: (SayvisScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current

    val workTools = listOf(
        ToolEntry(SayvisScreen.MISSIONS, Icons.Default.Flag, SayvisGold, "tool_missions")
    )
    val mindTools = listOf(
        ToolEntry(SayvisScreen.UIC, Icons.Default.Psychology, SayvisCyan, "tool_uic"),
        ToolEntry(SayvisScreen.AWARE, Icons.Default.Radar, SayvisGreenSuccess, "tool_aware"),
        ToolEntry(SayvisScreen.SIMULATION, Icons.Default.AutoGraph, SayvisGold, "tool_simulation")
    )
    val moneyTools = listOf(
        ToolEntry(SayvisScreen.GATEWAY, Icons.Default.SwapHoriz, SayvisGreenSuccess, "tool_gateway"),
        ToolEntry(SayvisScreen.TRADING, Icons.Default.AutoGraph, SayvisCyan, "tool_trading")
    )
    val systemTools = listOf(
        ToolEntry(SayvisScreen.ARENA_AGENT, Icons.Default.AutoAwesome, SayvisGold, "tool_arena_agent"),
        ToolEntry(SayvisScreen.ROBOT, Icons.Default.SmartToy, SayvisCyan, "tool_robot"),
        ToolEntry(SayvisScreen.AGENT, Icons.Default.TravelExplore, SayvisGold, "tool_agent"),
        ToolEntry(SayvisScreen.MIRROR, Icons.Default.CameraFront, SayvisGreenSuccess, "tool_mirror"),
        ToolEntry(SayvisScreen.MARKETS, Icons.Default.CandlestickChart, SayvisGold, "tool_markets"),
        ToolEntry(SayvisScreen.AVATAR, Icons.Default.Mic, SayvisGreenSuccess, "tool_avatar_listen"),
        ToolEntry(SayvisScreen.SCRIPTS, Icons.Default.Code, SayvisGold, "tool_scripts"),
        ToolEntry(SayvisScreen.SECURITY, Icons.Default.Security, SayvisRedAlert, "tool_security")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("tools_screen"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = s.toolsTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SayvisSilver)
                Text(text = s.toolsSubtitle, fontSize = 11.sp, color = SayvisSilverMuted)
            }
        }

        groupHeader(s.toolsGroupWork)
        items(workTools, key = { it.testTag }) { entry ->
            ToolCard(entry = entry, counts = counts, isPersian = isPersian, onNavigate = onNavigate)
        }

        groupHeader(s.toolsGroupMind)
        items(mindTools, key = { it.testTag }) { entry ->
            ToolCard(entry = entry, counts = counts, isPersian = isPersian, onNavigate = onNavigate)
        }

        groupHeader(s.toolsGroupMoney)
        items(moneyTools, key = { it.testTag }) { entry ->
            ToolCard(entry = entry, counts = counts, isPersian = isPersian, onNavigate = onNavigate)
        }

        groupHeader(s.toolsGroupSystem)
        items(systemTools, key = { it.testTag }) { entry ->
            ToolCard(entry = entry, counts = counts, isPersian = isPersian, onNavigate = onNavigate)
        }

        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.groupHeader(title: String) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }, key = "header_$title") {
        SayvisSectionHeader(title = title)
    }
}

@Composable
private fun ToolCard(
    entry: ToolEntry,
    counts: ToolCounts,
    isPersian: Boolean,
    onNavigate: (SayvisScreen) -> Unit
) {
    val s = LocalStrings.current
    val title = toolTitle(entry.screen, s)
    val hint = toolHint(entry.screen, s)
    val badge = toolBadge(entry.screen, counts, isPersian)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onNavigate(entry.screen) }
            .testTag(entry.testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SayvisSurfaceVariant),
        border = BorderStroke(1.dp, SayvisBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(entry.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = entry.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(entry.accent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = entry.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = SayvisSilver,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hint,
                fontSize = 10.sp,
                color = SayvisSilverMuted,
                maxLines = 3,
                lineHeight = 13.sp
            )
        }
    }
}

private fun toolTitle(screen: SayvisScreen, s: com.example.sayvis.i18n.SayvisStrings): String = when (screen) {
    SayvisScreen.ARENA_AGENT -> if (s.fa) "ایجنت آرنا گرافیکی" else "Arena Graphical Agent"
    SayvisScreen.MISSIONS -> s.toolMissions
    SayvisScreen.UIC -> s.toolUic
    SayvisScreen.AWARE -> s.toolAware
    SayvisScreen.SIMULATION -> s.toolSimulation
    SayvisScreen.GATEWAY -> s.toolGateway
    SayvisScreen.TRADING -> s.toolTrading
    SayvisScreen.AVATAR -> s.toolAvatarListen
    SayvisScreen.ROBOT -> s.toolRobot
    SayvisScreen.SCRIPTS -> s.toolScripts
    SayvisScreen.SECURITY -> s.toolSecurity
    else -> screen.titleFa
}

private fun toolHint(screen: SayvisScreen, s: com.example.sayvis.i18n.SayvisStrings): String = when (screen) {
    SayvisScreen.ARENA_AGENT -> if (s.fa) "مثل Arena AI — محیط زنده: فایل، ترمینال، پیش‌نمایش گرافیکی" else "Like Arena AI — live: files, terminal & graphical preview"
    SayvisScreen.MISSIONS -> s.toolMissionsHint
    SayvisScreen.UIC -> s.toolUicHint
    SayvisScreen.AWARE -> s.toolAwareHint
    SayvisScreen.SIMULATION -> s.toolSimulationHint
    SayvisScreen.GATEWAY -> s.toolGatewayHint
    SayvisScreen.TRADING -> s.toolTradingHint
    SayvisScreen.AVATAR -> s.toolAvatarListenHint
    SayvisScreen.ROBOT -> s.toolRobotHint
    SayvisScreen.SCRIPTS -> s.toolScriptsHint
    SayvisScreen.SECURITY -> s.toolSecurityHint
    else -> ""
}

private fun toolBadge(screen: SayvisScreen, counts: ToolCounts, isPersian: Boolean): String? = when (screen) {
    SayvisScreen.MISSIONS -> counts.missions.takeIf { it > 0 }
        ?.let { PersianFormat.digits(it.toString(), counts.persianDigits) }
    SayvisScreen.AWARE -> counts.opportunities.takeIf { it > 0 }
        ?.let { PersianFormat.digits(it.toString(), counts.persianDigits) }
    SayvisScreen.UIC -> counts.uicAttributes.takeIf { it > 0 }
        ?.let { PersianFormat.digits(it.toString(), counts.persianDigits) }
    SayvisScreen.SECURITY -> counts.devices.takeIf { it > 0 }
        ?.let { PersianFormat.digits(it.toString(), counts.persianDigits) }
    SayvisScreen.SCRIPTS -> counts.scripts.takeIf { it > 0 }
        ?.let { PersianFormat.digits(it.toString(), counts.persianDigits) }
    SayvisScreen.GATEWAY -> if (counts.gatewayConnected) {
        if (isPersian) "متصل" else "Live"
    } else null
    else -> null
}
