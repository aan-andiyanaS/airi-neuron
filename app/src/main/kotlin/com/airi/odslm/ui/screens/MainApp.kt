package com.airi.odslm.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.airi.odslm.ui.components.AppDestination
import com.airi.odslm.ui.components.BottomNavBar
import com.airi.odslm.viewmodel.ChatViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun MainApp(
    viewModel: ChatViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it }
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AppDestination.CHAT -> ChatScreen(viewModel)
                AppDestination.MEMORY -> DummyMemoryScreen()
                AppDestination.SETTINGS -> SettingsScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}

@Composable
fun DummyMemoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Memory Space (Dummy)",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
    }
}
