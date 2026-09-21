package com.example.sayvis.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.i18n.LocalStrings
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisDeepSpace
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted

/**
 * Launch-time Google sign-in gate (optional, off by default): when the owner
 * enables “require Google sign-in” in Settings and an OAuth client is
 * configured, the app body is hidden behind this gate until the Google
 * consent flow completes. A local-continue escape hatch is always visible so
 * the owner can never be locked out of their own device data.
 */
@Composable
fun GoogleSignInGate(
    signedInEmail: String,
    onSignIn: () -> Unit,
    onLocalContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("google_signin_gate"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            tint = SayvisCyan,
            modifier = Modifier.size(84.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = s.googleGateTitle,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = SayvisSilver
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (signedInEmail.isBlank()) s.googleGateHint else s.googleGateWelcome + " " + signedInEmail,
            fontSize = 12.5.sp,
            color = SayvisSilverMuted
        )
        Spacer(modifier = Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SayvisButton(
                label = s.googleSignIn,
                onClick = onSignIn,
                tone = ButtonTone.PRIMARY,
                modifier = Modifier.weight(1f).testTag("google_gate_signin")
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SayvisButton(
            label = s.googleGateLocal,
            onClick = onLocalContinue,
            tone = ButtonTone.NEUTRAL,
            modifier = Modifier.fillMaxWidth().testTag("google_gate_local")
        )
    }
}
