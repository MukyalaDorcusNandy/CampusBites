package com.example.campusbite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CampusBiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Primary,
            secondary = Secondary,
            tertiary = PrimaryVariant,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            error = Error
        )
    } else {
        lightColorScheme(
            primary = Primary,
            secondary = Secondary,
            tertiary = PrimaryVariant,
            background = Background,
            surface = Surface,
            error = Error
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}