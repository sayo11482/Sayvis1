package com.example.sayvis.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.model.AwareOpportunity
import com.example.sayvis.model.OpportunityStatus
import com.example.sayvis.model.RiskLevel
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisBorder
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisSurfaceVariant

@Composable
fun ActionProposalCard(
    opportunity: AwareOpportunity,
    isPersian: Boolean,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (opportunity.riskLevel) {
        RiskLevel.LOW_RISK -> SayvisGreenSuccess
        RiskLevel.MEDIUM_RISK -> SayvisAmberWarning
        RiskLevel.HIGHER_RISK -> SayvisRedAlert
        RiskLevel.CRITICAL -> Color(0xFF991B1B)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_card_${opportunity.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SayvisSurfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SayvisText(
                    source = opportunity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    markTranslated = true
                )

                Box(
                    modifier = Modifier
                        .background(riskColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
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

            Spacer(modifier = Modifier.height(8.dp))

            SayvisText(
                source = opportunity.description,
                style = MaterialTheme.typography.bodyMedium,
                color = SayvisSilverMuted,
                markTranslated = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Details Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070A0F), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SayvisCyan,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = if (isPersian) "اقدام پیشنهادی: " else "Action Proposed: ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SayvisCyan
                    )
                    Text(
                        text = if (isPersian) opportunity.proposedAction.actionType.labelFa else opportunity.proposedAction.actionType.labelEn,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Text(
                        text = if (isPersian) "محتوا: " else "Target: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SayvisText(
                        source = opportunity.proposedAction.targetPayload,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        markTranslated = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${if (isPersian) "مجوز لازم: " else "Required Capability: "} ${opportunity.proposedAction.requiredPermission}",
                    fontSize = 11.sp,
                    color = SayvisSilverMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Decision Buttons
            if (opportunity.status == OpportunityStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SayvisBorder),
                        modifier = Modifier.testTag("dismiss_btn_${opportunity.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = SayvisSilverMuted)
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text(if (isPersian) "رد کردن" else "Deny", color = SayvisSilverMuted)
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (opportunity.riskLevel == RiskLevel.LOW_RISK) SayvisGreenSuccess else SayvisAmberWarning,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("approve_btn_${opportunity.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text(if (isPersian) "تأیید و اجرا" else "Approve & Execute", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (opportunity.status == OpportunityStatus.EXECUTED) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (opportunity.status == OpportunityStatus.EXECUTED) SayvisGreenSuccess else SayvisSilverMuted
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = if (isPersian) opportunity.status.labelFa else opportunity.status.labelEn,
                        fontSize = 12.sp,
                        color = if (opportunity.status == OpportunityStatus.EXECUTED) SayvisGreenSuccess else SayvisSilverMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
