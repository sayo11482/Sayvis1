package com.example.sayvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.RiskLevel
import com.example.ui.theme.SayvisBorder
import com.example.ui.theme.SayvisMidnight
import com.example.ui.theme.SayvisRed
import com.example.ui.theme.SayvisSurface
import com.example.ui.theme.SayvisSurfaceVariant
import com.example.ui.theme.SayvisTextLight

@Composable
fun ActionConsentDialog(
    opportunity: AwareOpportunity,
    isPersian: Boolean,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    val riskColor = Color(opportunity.riskLevel.colorHex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SayvisMidnight,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, riskColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("action_consent_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header: Zero-Trust Guard
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Guard",
                            tint = riskColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "درخواست رضایت مالک" else "Zero-Trust Consent Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SayvisTextLight
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPersian) opportunity.riskLevel.labelFa else opportunity.riskLevel.labelEn,
                            color = riskColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = opportunity.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SayvisTextLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = opportunity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SayvisTextLight.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action payload preview
                Text(
                    text = if (isPersian) "پیش‌نمایش پارامتر اجرایی:" else "Target Payload & Permission:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SayvisSurfaceVariant)
                        .border(1.dp, SayvisBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row {
                            Text(
                                text = if (isPersian) "مجوز لازم: " else "Permission: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = opportunity.proposedAction.requiredPermission,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = opportunity.proposedAction.targetPayload,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = SayvisTextLight.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Principle Warning
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A1C15))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GppMaybe,
                        contentDescription = "Warning",
                        tint = SayvisRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian)
                            "بر اساس اصل صفر-اعتماد سایویس، هوش مصنوعی مجاز به اقدام مستقل در این سطح خطر نیست. نیاز به رضایت مستقیم شماست."
                        else
                            "SAYVIS Zero-Trust Rule: AI intelligence does NOT automatically grant authorization. Explicit owner approval is required.",
                        fontSize = 11.sp,
                        color = SayvisTextLight.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("consent_deny_button")
                    ) {
                        Text(if (isPersian) "رد / لغو" else "Deny")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = riskColor),
                        modifier = Modifier.testTag("consent_approve_button")
                    ) {
                        Text(
                            text = if (isPersian) "تأیید و اجرای اقدام" else "Authorize & Execute",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
