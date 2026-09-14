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
import com.example.sayvis.ui.components.PermissionsGate
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
        if (settings.onboardingCompleted) {
          SayvisMainApp(viewModel = viewModel)
        } else {
          PermissionsGate(
            onFinish = { viewModel.updateSettings { it.copy(onboardingCompleted = true) } }
          )
        }
      }
    }
  }
}

