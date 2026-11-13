package com.musicplayer.promax.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme

@Composable
fun MusicPlayerProMaxTheme(
    appTheme: AppTheme = AppTheme.CLASSIC,
    appAccent: AppAccent = AppAccent.ELECTRIC_BLUE,
    content: @Composable () -> Unit
) {
    val accent = Color(appAccent.hex)
    val accentLight = accent.copy(red = (accent.red + 0.22f).coerceAtMost(1f), green = (accent.green + 0.22f).coerceAtMost(1f), blue = (accent.blue + 0.22f).coerceAtMost(1f))
    val colors = when (appTheme) {
        AppTheme.CLASSIC -> darkColorScheme(
            primary = accent,
            onPrimary = PrimaryText,
            secondary = accentLight,
            background = MidnightBlack,
            onBackground = PrimaryText,
            surface = SurfaceBlack,
            onSurface = PrimaryText,
            surfaceVariant = ElevatedBlack,
            onSurfaceVariant = SecondaryText,
            outline = SubtleBorder
        )
        AppTheme.CINEMATIC -> darkColorScheme(
            primary = accentLight,
            onPrimary = PrimaryText,
            secondary = accent,
            background = CinematicBackground,
            onBackground = PrimaryText,
            surface = CinematicSurface,
            onSurface = PrimaryText,
            surfaceVariant = CinematicElevated,
            onSurfaceVariant = SecondaryText,
            outline = SubtleBorder
        )
    }

    MaterialTheme(colorScheme = colors, content = content)
}
