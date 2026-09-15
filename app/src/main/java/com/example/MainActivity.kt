package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sayvis.screentranslate.ScreenTranslatorService
import com.example.sayvis.ui.SayvisMainApp
import com.example.sayvis.ui.SayvisScreen
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.theme.SayvisTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SayvisViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    openScreenTranslatorIfRequested(intent)
    setContent {
      SayvisTheme {
        SayvisMainApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    openScreenTranslatorIfRequested(intent)
  }

  /**
   * The translator's notification (and the post-reboot reminder) opens SAYVIS straight on the
   * live-translation screen, so re-granting screen access is one tap away.
   */
  private fun openScreenTranslatorIfRequested(intent: Intent?) {
    if (intent?.getBooleanExtra(ScreenTranslatorService.EXTRA_OPEN_SCREEN_TRANSLATOR, false) == true) {
      viewModel.navigateTo(SayvisScreen.SCREEN_TRANSLATOR)
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  SayvisTheme { Greeting("Android") }
}

