package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.PyroShiftShell
import com.example.ui.theme.PyroShiftTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check before Compose UI, ViewModels, or Firebase AI calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      PyroShiftTheme {
        PyroShiftShell()
      }
    }
  }
}
