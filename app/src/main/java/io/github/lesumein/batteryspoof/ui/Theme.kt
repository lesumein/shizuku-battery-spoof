package io.github.lesumein.batteryspoof.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 로비 배경 이미지(네이비 + 시안/라임 네온)에 맞춘 고정 다크 팔레트. */
object LobbyColors {
    val Cyan = Color(0xFF3DF2E0)
    val Lime = Color(0xFFB6F25C)
    val Amber = Color(0xFFFFC857)
    val Danger = Color(0xFFFF5C7A)
    val Navy = Color(0xFF07131C)
    val Glass = Color(0xB30B1E2A)
    val GlassBorder = Color(0x553DF2E0)
    val TextDim = Color(0xFF9FB8C2)
}

private val LobbyScheme = darkColorScheme(
    primary = LobbyColors.Cyan,
    onPrimary = LobbyColors.Navy,
    secondary = LobbyColors.Lime,
    onSecondary = LobbyColors.Navy,
    tertiary = LobbyColors.Amber,
    error = LobbyColors.Danger,
    background = LobbyColors.Navy,
    onBackground = Color.White,
    surface = Color(0xFF0C1C26),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF13293A),
    onSurfaceVariant = LobbyColors.TextDim,
    surfaceContainerLow = Color(0xFF0E2130),
    surfaceContainer = Color(0xFF102536),
    surfaceContainerHigh = Color(0xFF132B3D),
    outline = LobbyColors.GlassBorder,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LobbyScheme, content = content)
}
