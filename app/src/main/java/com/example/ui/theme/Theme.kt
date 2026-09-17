package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LimePrimaryDark,
    onPrimary = LimeOnPrimaryDark,
    primaryContainer = LimeContainerDark,
    onPrimaryContainer = LimeOnContainerDark,
    secondary = LimeAccent,
    onSecondary = LimeOnPrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    outline = DarkOutline,
    error = ErrorColor
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LimePrimaryLight,
    onPrimary = LimeOnPrimaryLight,
    primaryContainer = LimeContainerLight,
    onPrimaryContainer = LimeOnContainerLight,
    secondary = LimeAccent,
    onSecondary = LimeOnPrimaryLight,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    outline = LightOutline,
    error = ErrorColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve custom Lime Green identity
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
