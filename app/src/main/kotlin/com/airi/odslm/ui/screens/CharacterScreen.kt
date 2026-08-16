package com.airi.odslm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airi.odslm.ui.theme.Background
import com.airi.odslm.ui.theme.TextOnSurface

@Composable
fun CharacterScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Character Stage (Dummy)",
            color = TextOnSurface,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
