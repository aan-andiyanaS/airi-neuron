package com.airi.odslm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.airi.odslm.ui.screens.MainApp
import com.airi.odslm.ui.theme.AIRITheme
import com.airi.odslm.util.ThemePreferences
import com.airi.odslm.viewmodel.ChatViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val themePrefs = ThemePreferences(applicationContext)

        setContent {
            val isDarkTheme by themePrefs.isDarkMode.collectAsState()

            AIRITheme(isDarkTheme = isDarkTheme) {
                MainApp(
                    viewModel = viewModel,
                    isDarkMode = isDarkTheme,
                    onToggleDarkMode = { themePrefs.setDarkMode(it) }
                )
            }
        }
    }
}
