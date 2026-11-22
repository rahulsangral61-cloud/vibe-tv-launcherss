@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.vibe.tvlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            onBackground = Color.White,
        ),
        content = content,
    )
}
