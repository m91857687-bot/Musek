# Keep Room entities/DAOs and Media3 playback components safe under R8 shrinking
-keep class com.musicplayer.promax.data.local.** { *; }
-keep class com.musicplayer.promax.data.stats.** { *; }
-keep class com.musicplayer.promax.playback.** { *; }
-keep class com.musicplayer.promax.widget.** { *; }
-dontwarn androidx.media3.**
