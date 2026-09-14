package com.example.sayvis.ui

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sayvis.ai.GoogleAuthManager
import com.example.sayvis.ai.GoogleLinkManager
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.SecretKey
import com.example.sayvis.settings.SettingsStore
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.theme.SayvisCyan
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Lifecycle of the automatic connect pipeline (file-private). */
private enum class Phase { EXCHANGING, OPENING_STUDIO, WAITING_COPY, CONNECTING, DONE, FAILED }

/**
 * The fully automatic post-authentication pipeline. The owner only signs in
 * with Google once; from there SAYVIS takes over:
 *
 *  1. OAuth code exchanged (PKCE) → profile saved, refresh token in the vault.
 *  2. Clipboard is scanned for a Gemini key — if one is there (the usual case:
 *     copied from AI Studio moments before), it is validated live and saved.
 *  3. Otherwise Google AI Studio opens automatically; the moment the owner
 *     copies the key and returns, the resume handler captures, verifies and
 *     links it — Gemini becomes the brain with zero typing.
 */
class GoogleSignInActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private var phase by mutableStateOf(Phase.EXCHANGING)
    private var statusLine by mutableStateOf("")
    private var browserOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val error = data?.getQueryParameter("error")
        val code = data?.getQueryParameter("code")
        val state = data?.getQueryParameter("state")

        val store = SettingsStore.get(applicationContext)
        val persian = store.current().isPersian(SayvisStrings.deviceIsPersian())
        val strings = SayvisStrings.of(persian)

        when {
            // Plain open (no OAuth payload): treat as the manual entry point.
            code == null && error == null -> runAutoConnect(store, persian, strings)

            error != null -> {
                phase = Phase.FAILED
                statusLine = when (error) {
                    "access_denied" -> strings.googleDenied
                    else -> strings.googleErrorGeneric
                }
            }

            code != null -> {
                val pending = GoogleAuthManager.pendingFlow(applicationContext)
                if (pending == null || pending.second != state) {
                    GoogleAuthManager.clearPendingFlow(applicationContext)
                    phase = Phase.FAILED
                    statusLine = strings.googleErrorGeneric
                } else {
                    phase = Phase.EXCHANGING
                    statusLine = strings.googleConnecting
                    val clientId = store.current().google.clientId.trim()
                    scope.launch {
                        val result = runCatching {
                            GoogleAuthManager.exchange(clientId, code, pending.first)
                        }.getOrNull()
                        GoogleAuthManager.clearPendingFlow(applicationContext)
                        if (result == null || result.accessToken.isBlank()) {
                            phase = Phase.FAILED
                            statusLine = strings.googleErrorGeneric
                        } else {
                            val profile = result.idToken?.let { GoogleAuthManager.parseIdToken(it) }
                            store.putSecret(SecretKey.GOOGLE_REFRESH_TOKEN, result.refreshToken.orEmpty())
                            GoogleAuthManager.cacheAccessToken(applicationContext, result)
                            store.update {
                                it.copy(
                                    google = it.google.copy(
                                        email = profile?.email.orEmpty(),
                                        displayName = profile?.name.orEmpty(),
                                        pictureUrl = profile?.picture.orEmpty(),
                                        signedInAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                            }
                            Toast.makeText(this@GoogleSignInActivity, strings.googleWelcome, Toast.LENGTH_LONG).show()
                            runAutoConnect(store, persian, strings)
                        }
                    }
                }
            }
        }

        setContent {
            SayvisTheme {
                AutoConnectScreen(
                    isPersian = persian,
                    phase = phase,
                    statusLine = statusLine,
                    onRecheck = {
                        phase = Phase.CONNECTING
                        attemptKeyLink(store, persian, strings, reopenStudio = false)
                    },
                    onOpenStudio = {
                        browserOpened = true
                        runCatching {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GoogleLinkManager.STUDIO_KEY_URL))
                            )
                        }
                    },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The owner just came back from AI Studio — capture automatically.
        if (browserOpened && (phase == Phase.OPENING_STUDIO || phase == Phase.WAITING_COPY)) {
            val store = SettingsStore.get(applicationContext)
            val persian = store.current().isPersian(SayvisStrings.deviceIsPersian())
            attemptKeyLink(store, persian, SayvisStrings.of(persian), reopenStudio = false)
        }
    }

    /** Signs the account in conceptually, then links Gemini without asking twice. */
    private fun runAutoConnect(store: SettingsStore, persian: Boolean, strings: SayvisStrings) {
        phase = Phase.CONNECTING
        statusLine = strings.googleAutoConnecting
        attemptKeyLink(store, persian, strings, reopenStudio = true)
    }

    private fun attemptKeyLink(
        store: SettingsStore,
        persian: Boolean,
        strings: SayvisStrings,
        reopenStudio: Boolean
    ) {
        val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
            ?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        val key = GoogleLinkManager.extractKeyFromText(clip)

        if (key == null) {
            if (reopenStudio && !browserOpened) {
                phase = Phase.OPENING_STUDIO
                statusLine = strings.googleOpeningStudio
                browserOpened = true
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GoogleLinkManager.STUDIO_KEY_URL)))
                }
                return
            }
            phase = Phase.WAITING_COPY
            statusLine = strings.googleWaitingCopy
            return
        }

        phase = Phase.CONNECTING
        statusLine = strings.googleKeyVerifying
        scope.launch {
            val check = GoogleLinkManager.validateKey(key)
            if (check.keyUsable) {
                store.update {
                    it.copy(
                        ai = it.ai.copy(geminiApiKey = key, provider = AiProviderKind.GEMINI),
                        google = it.google.copy(grantedScopes = "gemini-linked")
                    )
                }
                phase = Phase.DONE
                statusLine = strings.googleAutoDone
                Toast.makeText(this@GoogleSignInActivity, strings.googleAutoDone, Toast.LENGTH_LONG).show()
            } else {
                phase = Phase.WAITING_COPY
                statusLine = if (persian) check.messageFa else check.messageEn
            }
        }
    }
}

@Composable
private fun AutoConnectScreen(
    isPersian: Boolean,
    phase: Phase,
    statusLine: String,
    onRecheck: () -> Unit,
    onOpenStudio: () -> Unit,
    onClose: () -> Unit
) {
    val s = remember(isPersian) { SayvisStrings.of(isPersian) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20B1220))
            .testTag("google_signin_activity"),
        contentAlignment = Alignment.Center
    ) {
        SayvisCard(modifier = Modifier.padding(18.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (phase) {
                    Phase.DONE -> {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = SayvisGreenSuccess,
                            modifier = Modifier.size(46.dp).testTag("google_auto_done")
                        )
                        Text(
                            text = statusLine,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisGreenSuccess
                        )
                    }
                    Phase.FAILED -> {
                        Text(
                            text = "⚠️ $statusLine",
                            fontSize = 12.5.sp,
                            color = SayvisRedAlert,
                            modifier = Modifier.testTag("google_signin_failure")
                        )
                    }
                    Phase.WAITING_COPY -> {
                        Text(
                            text = statusLine,
                            fontSize = 12.5.sp,
                            color = SayvisCyan
                        )
                        Text(
                            text = s.googleCopyGuide,
                            fontSize = 11.sp,
                            color = SayvisSilverMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            SayvisButton(
                                label = s.googleRecheck,
                                onClick = onRecheck,
                                tone = ButtonTone.PRIMARY,
                                modifier = Modifier.weight(1f).testTag("google_recheck")
                            )
                            SayvisButton(
                                label = s.captureOpenStudio,
                                onClick = onOpenStudio,
                                tone = ButtonTone.NEUTRAL,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp).testTag("google_auto_busy"),
                            color = SayvisCyan,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = statusLine,
                            fontSize = 12.5.sp,
                            color = SayvisSilver
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                SayvisButton(
                    label = s.captureCancel,
                    onClick = onClose,
                    tone = ButtonTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth().testTag("google_signin_close")
                )
            }
        }
    }
}
