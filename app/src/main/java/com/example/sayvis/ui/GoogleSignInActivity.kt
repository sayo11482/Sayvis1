package com.example.sayvis.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.sayvis.identity.GoogleAccountHub
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

/** Lifecycle of the connect pipeline (file-private). */
private enum class Phase { PICKING, MANUAL, EXCHANGING, LINKING, OPENING_STUDIO, WAITING_COPY, CONNECTING, DONE, FAILED }

/**
 * The connect entry point (v4.0.0 rebuild).
 *
 * Manual open (no OAuth payload) now runs the ZERO-CONFIG flow:
 *
 *  1. The system "choose a Google account" sheet opens automatically —
 *     no client ID, no console setup, no SHA-1 (this fixes the broken
 *     sign-in: the old button stayed disabled without a pasted client ID).
 *  2. The chosen e-mail becomes the identity hub: settings + every outgoing
 *     request carries it as `X-Sayvis-Account` (SayvisNet).
 *  3. The original automatic pipeline then continues unchanged: clipboard
 *     Gemini-key capture → live validation → AI wiring; otherwise AI Studio
 *     opens and the key is captured on return. Zero typing.
 *
 * If the device offers no Google account (no GMS), a manual e-mail fallback
 * keeps the owner in control. The PKCE OAuth callback (?code=…) is preserved
 * for the optional Gmail/Calendar/Drive scopes.
 */
class GoogleSignInActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private var phase by mutableStateOf(Phase.PICKING)
    private var statusLine by mutableStateOf("")
    private var manualEmail by mutableStateOf("")
    private var browserOpened = false

    private val accountPicker =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val email = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (email.isNullOrBlank()) {
                phase = Phase.MANUAL
                statusLine = ""
            } else {
                linkChosenAccount(email)
            }
        }

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
            // Plain open (no OAuth payload): the zero-config account picker.
            code == null && error == null -> {
                phase = Phase.PICKING
                statusLine = strings.googleAutoConnecting
                setContent {
                    SayvisTheme {
                        ConnectFlowScreen(
                            isPersian = persian,
                            phase = phase,
                            statusLine = statusLine,
                            manualEmail = manualEmail,
                            onManualEmailChange = { manualEmail = it },
                            onManualLink = { linkChosenAccount(manualEmail) },
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
                val pick = GoogleAccountHub.chooseAccountSender(applicationContext)
                if (pick != null) {
                    runCatching {
                        accountPicker.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(pick).build()
                        )
                    }.onFailure { phase = Phase.MANUAL }
                } else {
                    phase = Phase.MANUAL
                }
            }

            error != null -> {
                phase = Phase.FAILED
                statusLine = when (error) {
                    "access_denied" -> strings.googleDenied
                    else -> strings.googleErrorGeneric
                }
                renderShell(persian, strings)
            }

            code != null -> {
                renderShell(persian, strings)
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
                            val linked = profile?.email.orEmpty()
                            if (linked.isNotBlank()) {
                                GoogleAccountHub.link(applicationContext, linked)
                                store.update {
                                    it.copy(
                                        google = it.google.copy(
                                            displayName = profile?.name ?: GoogleAccountHub.displayNameFor(linked),
                                            pictureUrl = profile?.picture.orEmpty()
                                        )
                                    )
                                }
                            }
                            Toast.makeText(this@GoogleSignInActivity, strings.googleWelcome, Toast.LENGTH_LONG).show()
                            runAutoConnect(store, persian, strings)
                        }
                    }
                }
            }
        }
    }

    /** Renders the shell for the OAuth-callback paths (UI-only). */
    private fun renderShell(persian: Boolean, strings: SayvisStrings) {
        val store = SettingsStore.get(applicationContext)
        setContent {
            SayvisTheme {
                ConnectFlowScreen(
                    isPersian = persian,
                    phase = phase,
                    statusLine = statusLine,
                    manualEmail = manualEmail,
                    onManualEmailChange = { manualEmail = it },
                    onManualLink = { linkChosenAccount(manualEmail) },
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

    /** Persists the chosen Google account, then continues the auto pipeline. */
    private fun linkChosenAccount(rawEmail: String) {
        val store = SettingsStore.get(applicationContext)
        val persian = store.current().isPersian(SayvisStrings.deviceIsPersian())
        val strings = SayvisStrings.of(persian)
        phase = Phase.LINKING
        statusLine = strings.googleConnecting
        when (val result = GoogleAccountHub.link(applicationContext, rawEmail)) {
            is GoogleAccountHub.LinkResult.Success -> {
                Toast.makeText(this, strings.googleWelcome + " — " + result.email, Toast.LENGTH_LONG).show()
                runAutoConnect(store, persian, strings)
            }
            is GoogleAccountHub.LinkResult.Invalid -> {
                phase = Phase.MANUAL
                statusLine = result.message(persian)
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
private fun ConnectFlowScreen(
    isPersian: Boolean,
    phase: Phase,
    statusLine: String,
    manualEmail: String,
    onManualEmailChange: (String) -> Unit,
    onManualLink: () -> Unit,
    onRecheck: () -> Unit,
    onOpenStudio: () -> Unit,
    onClose: () -> Unit
) {
    val s = remember(isPersian) { SayvisStrings.of(isPersian) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A0B0E))
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
                    Phase.MANUAL -> {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = SayvisCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = s.connectManualTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SayvisSilver
                        )
                        Text(
                            text = s.connectManualHint,
                            fontSize = 11.sp,
                            color = SayvisSilverMuted
                        )
                        OutlinedTextField(
                            value = manualEmail,
                            onValueChange = onManualEmailChange,
                            singleLine = true,
                            placeholder = { Text("name@gmail.com", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_manual_email")
                        )
                        if (statusLine.isNotBlank()) {
                            Text(text = statusLine, fontSize = 11.sp, color = SayvisRedAlert)
                        }
                        SayvisButton(
                            label = s.connectManualLink,
                            onClick = onManualLink,
                            enabled = manualEmail.contains("@"),
                            tone = ButtonTone.PRIMARY,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_manual_link")
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
