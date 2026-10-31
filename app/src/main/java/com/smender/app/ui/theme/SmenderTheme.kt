package com.smender.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource
import com.smender.app.R

@Composable
fun SmenderTheme(content: @Composable () -> Unit) {
    val lightColors = lightColorScheme(
        primary = colorResource(R.color.blue_600),
        onPrimary = colorResource(R.color.white),
        background = colorResource(R.color.app_background),
        onBackground = colorResource(R.color.text_primary),
        surface = colorResource(R.color.surface_card),
        onSurface = colorResource(R.color.text_primary)
    )
    val darkColors = darkColorScheme(
        primary = colorResource(R.color.blue_500),
        onPrimary = colorResource(R.color.white),
        background = colorResource(R.color.app_background),
        onBackground = colorResource(R.color.text_primary),
        surface = colorResource(R.color.surface_card),
        onSurface = colorResource(R.color.text_primary)
    )
    val colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
