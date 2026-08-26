package com.qatra.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = QatraDarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = QatraDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4AB),
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFFB77C),
    background = QatraDarkBackground,
    onBackground = Color(0xFFEDE0DF),
    surface = QatraDarkSurface,
    onSurface = Color(0xFFEDE0DF),
    surfaceVariant = QatraDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A)
)

private val LightColorScheme = lightColorScheme(
    primary = QatraRedPrimary,
    onPrimary = Color.White,
    primaryContainer = QatraRedContainer,
    onPrimaryContainer = QatraRedDark,
    secondary = QatraRedDark,
    onSecondary = Color.White,
    secondaryContainer = QatraRedContainerDark,
    onSecondaryContainer = QatraRedDark,
    tertiary = QatraWarning,
    background = QatraOffWhite,
    onBackground = QatraGray900,
    surface = QatraWhite,
    onSurface = QatraGray900,
    surfaceVariant = QatraRedSurface,
    onSurfaceVariant = QatraGray800,
    outline = QatraGray400,
    outlineVariant = QatraGray200
)

@Composable
fun QatraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
