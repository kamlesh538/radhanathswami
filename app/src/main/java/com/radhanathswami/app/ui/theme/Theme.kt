package com.radhanathswami.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = SaffronPrimary,
    onPrimary            = Color.White,
    primaryContainer     = SaffronLight,
    onPrimaryContainer   = DeepBrown,
    secondary            = Turmeric,
    onSecondary          = Color.White,
    secondaryContainer   = PaleSurface,
    onSecondaryContainer = MediumBrown,
    background           = CreamBackground,
    onBackground         = DeepBrown,
    surface              = WarmSurface,
    onSurface            = DeepBrown,
    surfaceVariant       = PaleSurface,
    onSurfaceVariant     = MediumBrown,
    error                = Color(0xFFB71C1C),
    onError              = Color.White
)

@Composable
fun RadhanathSwamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
