package com.odin.agent.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.strategies.*
import com.odin.agent.ui.theme.*

@Composable
fun StrategiesScreen(
    isPersian: Boolean,
    enabledStrategies: List<QuantStrategyType>,
    onToggleStrategy: (QuantStrategyType, Boolean) -> Unit
) {
    val allStrategies = listOf(
        TrendFollowingStrategy(),
        MeanReversionStrategy(),
        MomentumBreakoutStrategy(),
        PairsTradingStrategy(),
        VolatilityRegimeStrategy()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "استراتژی‌های کوانت" else "Quant Strategies",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isPersian) "5 استراتژی اولویت‌دار - ماژولار و قابل فعال/غیرفعال" else "5 Prioritized Strategies - Modular & Toggleable",
                fontSize = 11.sp,
                color = OdinSilverMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(allStrategies) { strategy ->
            val isEnabled = enabledStrategies.contains(strategy.type)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) OdinSurfaceVariant else OdinSurfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isEnabled) OdinCyan.copy(alpha = 0.5f) else OdinBorder
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when(strategy.type.priority) {
                                            1 -> OdinCyan.copy(alpha = 0.2f)
                                            2 -> OdinGold.copy(alpha = 0.2f)
                                            3 -> OdinGreen.copy(alpha = 0.2f)
                                            else -> OdinSilverMuted.copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${strategy.type.priority}",
                                    fontWeight = FontWeight.Black,
                                    color = when(strategy.type.priority) {
                                        1 -> OdinCyan
                                        2 -> OdinGold
                                        3 -> OdinGreen
                                        else -> OdinSilverMuted
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strategy.type.label(isPersian),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Risk: ${strategy.getRiskLevel()}",
                                        fontSize = 10.sp,
                                        color = when(strategy.getRiskLevel()) {
                                            "LOW" -> OdinGreen
                                            "MEDIUM" -> OdinGold
                                            else -> OdinRed
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Best: ${strategy.getBestRegime()}",
                                        fontSize = 10.sp,
                                        color = OdinSilverMuted
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggleStrategy(strategy.type, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OdinCyan,
                                checkedTrackColor = OdinCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = strategy.getDescription(isPersian),
                        fontSize = 11.sp,
                        color = OdinSilver,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Entry/Exit
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OdinDeepSpace)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = OdinGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strategy.getEntryRules(isPersian), fontSize = 10.sp, color = OdinSilver)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = OdinRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strategy.getExitRules(isPersian), fontSize = 10.sp, color = OdinSilver)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Params
                    Text(
                        text = if (isPersian) "پارامترها: ${strategy.params}" else "Params: ${strategy.params}",
                        fontSize = 9.sp,
                        color = OdinSilverMuted
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OdinSurfaceVariant),
                border = BorderStroke(1.dp, OdinBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPersian) "➕ افزودن استراتژی جدید" else "➕ Add New Strategy",
                        fontWeight = FontWeight.Bold,
                        color = OdinCyan,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isPersian)
                            "1. فایل جدید در strategies/ بساز\n2. از BaseStrategy ارث ببر\n3. generateSignals() رو پیاده کن\n4. در config.yaml اضافه کن"
                        else
                            "1. Create new file in strategies/\n2. Inherit from BaseStrategy\n3. Implement generateSignals()\n4. Add to config.yaml",
                        fontSize = 11.sp,
                        color = OdinSilverMuted,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
