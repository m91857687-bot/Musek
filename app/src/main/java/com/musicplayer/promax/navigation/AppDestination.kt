package com.musicplayer.promax.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.musicplayer.promax.R

enum class AppDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    HOME(R.string.home, Icons.Filled.Home),
    LIBRARY(R.string.library, Icons.Filled.LibraryMusic),
    PLAYLISTS(R.string.playlists, Icons.Filled.QueueMusic),
    EQUALIZER(R.string.equalizer, Icons.Filled.GraphicEq),
    STATISTICS(R.string.statistics, Icons.Filled.BarChart),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}
