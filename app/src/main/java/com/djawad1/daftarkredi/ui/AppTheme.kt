package com.djawad1.daftarkredi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DaftarTheme(themeName: String, content: @Composable () -> Unit) {
    val scheme = when (themeName) {
        "dark" -> darkColorScheme(
            primary = Color(0xFF8AB4F8), secondary = Color(0xFF80CBC4), background = Color(0xFF101318),
            surface = Color(0xFF1A2028), onPrimary = Color(0xFF10213D), onSecondary = Color(0xFF062F2A),
            onBackground = Color(0xFFE8EAED), onSurface = Color(0xFFE8EAED), error = Color(0xFFFF8A80)
        )
        "mint" -> lightColorScheme(
            primary = Color(0xFF087F68), secondary = Color(0xFF4B8F83), background = Color(0xFFF0FAF6),
            surface = Color(0xFFFFFFFF), onPrimary = Color.White, onSecondary = Color.White
        )
        "orange" -> lightColorScheme(
            primary = Color(0xFFB95616), secondary = Color(0xFFD98245), background = Color(0xFFFFF7F0),
            surface = Color(0xFFFFFFFF), onPrimary = Color.White, onSecondary = Color.White
        )
        "navy" -> lightColorScheme(
            primary = Color(0xFF164A78), secondary = Color(0xFF4D7899), background = Color(0xFFF1F6FA),
            surface = Color(0xFFFFFFFF), onPrimary = Color.White, onSecondary = Color.White
        )
        else -> lightColorScheme(
            primary = Color(0xFF245B9A), secondary = Color(0xFF527D9F), background = Color(0xFFF5F7FA),
            surface = Color.White, onPrimary = Color.White, onSecondary = Color.White
        )
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}
