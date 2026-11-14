package com.musicplayer.promax.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.musicplayer.promax.domain.model.AppAccent
import com.musicplayer.promax.domain.model.AppTheme

@Composable
fun MusicPlayerProMaxTheme(
    appTheme: AppTheme = AppTheme.CLASSIC,
    appAccent: AppAccent = AppAccent.ELECTRIC_BLUE,
    dynamicPrimaryOverride: Color? = null,
    dynamicSecondaryOverride: Color? = null,
    content: @Composable () -> Unit
) {
    val baseAccent = Color(appAccent.hex)
    val baseAccentLight = baseAccent.copy(
        red = (baseAccent.red + 0.22f).coerceAtMost(1f),
        green = (baseAccent.green + 0.22f).coerceAtMost(1f),
        blue = (baseAccent.blue + 0.22f).coerceAtMost(1f)
    )
    val targetPrimary = dynamicPrimaryOverride ?: if (appTheme == AppTheme.CINEMATIC) baseAccentLight else baseAccent
    val targetSecondary = dynamicSecondaryOverride ?: if (appTheme == AppTheme.CINEMATIC) baseAccent else baseAccentLight

    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 550),
        label = "dynamic_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 550),
        label = "dynamic_secondary"
    )

    val colors = when (appTheme) {
        AppTheme.CLASSIC -> darkColorScheme(
            primary = animatedPrimary,
            onPrimary = MidnightBlack,
            secondary = animatedSecondary,
            background = MidnightBlack,
            onBackground = PrimaryText,
            surface = SurfaceBlack,
            onSurface = PrimaryText,
            surfaceVariant = ElevatedBlack,
            onSurfaceVariant = SecondaryText,
            outline = SubtleBorder
        )
        AppTheme.CINEMATIC -> darkColorScheme(
            primary = animatedPrimary,
            onPrimary = CinematicBackground,
            secondary = animatedSecondary,
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
