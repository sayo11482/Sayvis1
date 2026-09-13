package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.sayvis.ui.SayvisMainApp
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.theme.SayvisTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SayvisViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      // Runtime accent color the AI can change on its own (self-restyle protocol)
      val accent by viewModel.runtimeAccent.collectAsState()
      SayvisTheme(accentColor = accent?.let { Color(it) }) {
        SayvisMainApp(viewModel = viewModel)
      }
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
