package com.example.sayvis.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.sayvis.i18n.SayvisStrings
import com.example.sayvis.settings.AiProviderKind
import com.example.sayvis.settings.SettingsStore
import com.example.sayvis.ui.components.ButtonTone
import com.example.sayvis.ui.components.SayvisButton
import com.example.sayvis.ui.components.SayvisCard
import com.example.sayvis.ui.theme.SayvisGreenSuccess
import com.example.sayvis.ui.theme.SayvisRedAlert
import com.example.sayvis.ui.theme.SayvisSilver
import com.example.sayvis.ui.theme.SayvisSilverMuted
import com.example.sayvis.ui.theme.SayvisTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Redirect target of the in-app Google sign-in flow
 * (`sayvis://oauth2?code=…&state=…`). Exchanges the authorization code via
 * PKCE, stores the refresh token in the Keystore vault, persists the profile
 * and closes itself. Also acts as a friendly status page when opened without
 * a code (e.g. the browser could not complete the flow).
 */
class GoogleSignInActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val error = data?.getQueryParameter("error")
        val code = data?.getQueryParameter("code")
        val state = data?.getQueryParameter("state")

        val store = SettingsStore.get(applicationContext)
        val persian = store.current().isPersian(SayvisStrings.deviceIsPersian())
        val strings = SayvisStrings.of(persian)

        var outcome by mutableStateOf<Pair<Boolean, String>?>(null)

        when {
            error != null -> outcome = false to when (error) {
                "access_denied" -> strings.googleDenied
                else -> strings.googleErrorGeneric
            }
            code != null -> {
                val pending = GoogleAuthManager.pendingFlow(applicationContext)
                if (pending == null || pending.second != state) {
                    GoogleAuthManager.clearPendingFlow(applicationContext)
                    outcome = false to strings.googleErrorGeneric
                } else {
                    val verifier = pending.first
                    val clientId = store.current().google.clientId.trim()
                    scope.launch {
                        val result = runCatching {
                            GoogleAuthManager.exchange(clientId, code, verifier)
                        }.getOrNull()
                        GoogleAuthManager.clearPendingFlow(applicationContext)
                        if (result == null || result.accessToken.isBlank()) {
                            outcome = false to strings.googleErrorGeneric
                        } else {
                            val profile = result.idToken?.let { GoogleAuthManager.parseIdToken(it) }
                            store.putSecret(
                                com.example.sayvis.settings.SecretKey.GOOGLE_REFRESH_TOKEN,
                                result.refreshToken.orEmpty()
                            )
                            GoogleAuthManager.cacheAccessToken(applicationContext, result)
                            store.update {
                                it.copy(
                                    google = it.google.copy(
                                        email = profile?.email.orEmpty(),
                                        displayName = profile?.name.orEmpty(),
                                        pictureUrl = profile?.picture.orEmpty(),
                                        signedInAtEpochMs = System.currentTimeMillis()
                                    ),
                                    ai = it.ai.copy(provider = AiProviderKind.GEMINI)
                                )
                            }
                            outcome = true to (profile?.email ?: strings.googleSignIn)
                            Toast.makeText(this@GoogleSignInActivity, strings.googleWelcome, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else -> outcome = null // plain open: show instructions
        }

        setContent {
            SayvisTheme {
                GoogleSignInStatusScreen(
                    isPersian = persian,
                    outcome = outcome,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInStatusScreen(
    isPersian: Boolean,
    outcome: Pair<Boolean, String>?,
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = s.googleLinkTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SayvisSilver
                )
                when {
                    outcome == null -> Text(
                        text = s.googleReturnHint,
                        fontSize = 12.sp,
                        color = SayvisSilverMuted
                    )
                    outcome.first -> Text(
                        text = "✅ " + s.googleConnectedAs + " " + outcome.second,
                        fontSize = 12.5.sp,
                        color = SayvisGreenSuccess,
                        modifier = Modifier.testTag("google_signin_success")
                    )
                    else -> Text(
                        text = "⚠️ " + outcome.second,
                        fontSize = 12.sp,
                        color = SayvisRedAlert,
                        modifier = Modifier.testTag("google_signin_failure")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
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
