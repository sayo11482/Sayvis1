package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.sayvis.ui.SayvisMainApp
import com.example.sayvis.ui.SayvisViewModel
import com.example.sayvis.ui.theme.SayvisTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SayvisViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SayvisTheme {
        SayvisMainApp(viewModel = viewModel)
      }
    }
  }
}

