package com.musornibak.pocketvpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PvDarkColors = darkColorScheme(
    primary              = PvPrimary,
    onPrimary            = PvOnPrimary,
    primaryContainer     = PvPrimaryContainer,
    onPrimaryContainer   = PvOnPrimaryContainer,
    secondary            = PvSecondary,
    onSecondary          = PvOnSecondary,
    secondaryContainer   = PvSecondaryContainer,
    onSecondaryContainer = PvOnSecondaryContainer,
    tertiary             = PvTertiary,
    onTertiary           = PvOnTertiary,
    tertiaryContainer    = PvTertiaryContainer,
    onTertiaryContainer  = PvOnTertiaryContainer,
    background           = PvBackground,
    onBackground         = PvOnBackground,
    surface              = PvSurface,
    onSurface            = PvOnSurface,
    surfaceVariant       = PvSurfaceVariant,
    onSurfaceVariant     = PvOnSurfaceVariant,
    surfaceContainer     = PvSurfaceContainer,
    surfaceContainerHigh = PvSurfaceContainerHigh,
    outline              = PvOutline,
    error                = PvError,
    onError              = PvOnError
)

@Composable
fun PocketVpnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PvDarkColors,
        typography = Typography,
        content = content
    )
}
