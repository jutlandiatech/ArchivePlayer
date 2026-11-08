package com.jutlandiatech.archiveplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Nordic light / Nordic noir (UI_SHELL_SPEC.md → Theme, R63, R82). All app colours live here. */
object NordicColors {
    val Blue = Color(0xFF2769AD)
    val BlueLight = Color(0xFF8DB8E8)
    val NoirSurface = Color(0xFF15181C)
    val NoirCard = Color(0xFF22262C)
    val LightCard = Color(0xFFF2F4F7)
    val Warning = Color(0xFFB3261E)
}

private val LightScheme = lightColorScheme(
    primary = NordicColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8F5),
    onPrimaryContainer = Color(0xFF0D2C4D),
    secondary = NordicColors.Blue,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = NordicColors.LightCard,
    onSurfaceVariant = Color(0xFF44474E),
    surfaceContainer = NordicColors.LightCard,
    error = NordicColors.Warning,
)

private val NoirScheme = darkColorScheme(
    primary = NordicColors.BlueLight,
    onPrimary = Color(0xFF0D2C4D),
    primaryContainer = Color(0xFF1F4F82),
    onPrimaryContainer = Color(0xFFDCE8F5),
    secondary = NordicColors.BlueLight,
    background = NordicColors.NoirSurface,
    onBackground = Color(0xFFE6E8EB),
    surface = NordicColors.NoirSurface,
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = NordicColors.NoirCard,
    onSurfaceVariant = Color(0xFFC4C7CE),
    surfaceContainer = NordicColors.NoirCard,
    error = Color(0xFFF2B8B5),
)

private val NordicTypography = Typography().let { t ->
    t.copy(
        bodyLarge = t.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
        headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ArchivePlayerTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) NoirScheme else LightScheme, typography = NordicTypography, content = content)
}
