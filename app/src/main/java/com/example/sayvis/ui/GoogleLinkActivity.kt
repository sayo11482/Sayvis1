package com.example.sayvis.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ai.GoogleLinkManager
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.SettingsStore
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.theme.SayvisAmberWarning
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisTheme
import kotlinx.coroutines.launch

/**
 * Transparent capture target of the Google-login link flow: Android offers
 * SAYVIS inside the text-selection menu (ACTION_PROCESS_TEXT) and the share
 * sheet (ACTION_SEND) — e.g. right after the owner creates a fresh Gemini key
 * in Google AI Studio while signed in with their Google account. The captured
 * key is verified live against Google and stored in the Keystore-backed vault
 * (Settings → AI & API → Google account link offers the same from clipboard).
 */
class GoogleLinkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = listOf(
            intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString(),
            intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        ).filterNotNull().joinToString("\n")
        val captured = GoogleLinkManager.extractKeyFromText(shared)

        val store = SettingsStore.get(applicationContext)
        setContent {
            SayvisTheme {
                GoogleLinkDialog(
                    capturedKey = captured,
                    initialIsPersian = store.current().isPersian(SayvisStrings.deviceIsPersian()),
                    onConnect = { key ->
                        store.update {
                            it.copy(
                                ai = it.ai.copy(
                                    geminiApiKey = key,
                                    // Linking the Google account means the owner
                                    // wants Gemini as the brain.
                                    provider = AiProviderKind.GEMINI
                                )
                            )
                        }
                        val persian = store.current().isPersian(SayvisStrings.deviceIsPersian())
                        Toast.makeText(
                            this,
                            SayvisStrings.of(persian).googleLinkSaved,
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    },
                    onDismiss = { finish() },
                    onOpenStudio = {
                        runCatching {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GoogleLinkManager.STUDIO_KEY_URL))
                            )
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun GoogleLinkDialog(
    capturedKey: String?,
    initialIsPersian: Boolean,
    onConnect: (key: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenStudio: () -> Unit
) {
    val s = remember(initialIsPersian) { SayvisStrings.of(initialIsPersian) }
    var busy by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<GoogleLinkManager.LinkCheck?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B1220))
            .testTag("google_link_activity"),
        contentAlignment = Alignment.Center
    ) {
        SayvisCard(modifier = Modifier.padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = s.googleLinkTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SayvisSilver
                )

                if (capturedKey == null) {
                    Text(
                        text = s.captureNone,
                        fontSize = 12.sp,
                        color = SayvisSilverMuted
                    )
                    SayvisButton(
                        label = s.captureOpenStudio,
                        onClick = onOpenStudio,
                        tone = ButtonTone.PRIMARY,
                        modifier = Modifier.fillMaxWidth().testTag("google_link_open_studio")
                    )
                    SayvisButton(
                        label = s.captureCancel,
                        onClick = onDismiss,
                        tone = ButtonTone.NEUTRAL,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = kindLabel(s, capturedKey) + "  •  " + GoogleLinkManager.redact(capturedKey),
                        fontSize = 12.sp,
                        color = SayvisCyan
                    )
                    check?.let { result ->
                        Text(
                            text = if (initialIsPersian) result.messageFa else result.messageEn,
                            fontSize = 11.5.sp,
                            color = when (result.status) {
                                GoogleLinkManager.CheckStatus.VALID -> SayvisGreenSuccess
                                GoogleLinkManager.CheckStatus.INVALID -> SayvisRedAlert
                                else -> SayvisAmberWarning
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SayvisButton(
                            label = if (busy) s.googleLinkChecking else s.captureConnect,
                            onClick = {
                                if (busy) return@SayvisButton
                                busy = true
                                scope.launch {
                                    val result = GoogleLinkManager.validateKey(capturedKey)
                                    if (result.keyUsable) {
                                        onConnect(capturedKey)
                                    } else {
                                        check = result
                                        busy = false
                                    }
                                }
                            },
                            busy = busy,
                            tone = ButtonTone.SUCCESS,
                            modifier = Modifier.weight(1f).testTag("google_link_connect")
                        )
                        SayvisButton(
                            label = s.captureCancel,
                            onClick = onDismiss,
                            enabled = !busy,
                            tone = ButtonTone.NEUTRAL,
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun kindLabel(s: SayvisStrings, key: String): String = when (GoogleLinkManager.classify(key)) {
    GoogleLinkManager.KeyKind.AUTH_AQ -> s.googleLinkKindAuth
    GoogleLinkManager.KeyKind.STANDARD_AIZA -> s.googleLinkKindStandard
    GoogleLinkManager.KeyKind.UNKNOWN -> s.googleLinkKindUnknown
}
