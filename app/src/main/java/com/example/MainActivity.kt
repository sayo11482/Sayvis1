package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sayvis.ui.SayvisMainApp
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.components.GoogleSignInGate
import com.example.sayvis.ui.components.PermissionsGate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sayvis.ui.theme.SayvisTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SayvisViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SayvisTheme {
        // First-launch gate: the device explicitly asks the owner to approve every
        // capability before the app body is shown. "Enter SAYVIS" always works.
        val settings by viewModel.settings.collectAsState()
        // Optional hard sign-in: the owner can require Google sign-in at every
        // launch (Settings -> Google account). "Continue locally" stays visible
        // so the owner can never be locked out of the on-device data.
        var localBypass by remember { mutableStateOf(false) }
        val googleGateRequired = settings.onboardingCompleted &&
            settings.google.requireSignInAtLaunch &&
            settings.google.clientId.isNotBlank() &&
            !settings.google.signedIn &&
            !localBypass
        when {
          !settings.onboardingCompleted -> {
            PermissionsGate(
              onFinish = { viewModel.updateSettings { it.copy(onboardingCompleted = true) } }
            )
          }
          googleGateRequired -> {
            GoogleSignInGate(
              signedInEmail = settings.google.email,
              onSignIn = { viewModel.beginGoogleSignIn() },
              onLocalContinue = { localBypass = true }
            )
          }
          else -> SayvisMainApp(viewModel = viewModel)
        }
      }
    }
  }
}

