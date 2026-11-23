package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val titleUk: String,
    val subtitle: String,
    val previewBg: Color,
    val previewSurface: Color,
    val previewAccent: Color,
    val previewText: Color
) {
    LIGHT(
        titleUk = "Світла",
        subtitle = "Теплий папір",
        previewBg = LightPaperBg,
        previewSurface = LightPaperSurface,
        previewAccent = TerracottaPrimary,
        previewText = LightPaperOnSurface
    ),
    DARK(
        titleUk = "Темна",
        subtitle = "Нічний графіт",
        previewBg = DarkCharcoalBg,
        previewSurface = DarkCharcoalSurface,
        previewAccent = TerracottaDark,
        previewText = DarkCharcoalOnSurface
    ),
    SEPIA(
        titleUk = "Сепія",
        subtitle = "Пергамент",
        previewBg = SepiaBg,
        previewSurface = SepiaSurface,
        previewAccent = SepiaInkPrimary,
        previewText = SepiaOnSurface
    ),
    SAGE(
        titleUk = "Лісова",
        subtitle = "Шварцвальд",
        previewBg = SageBg,
        previewSurface = SageSurface,
        previewAccent = ForestPinePrimary,
        previewText = SageOnSurface
    ),
    SYSTEM(
        titleUk = "Системна",
        subtitle = "Автоматично",
        previewBg = LightPaperVariant,
        previewSurface = DarkCharcoalSurface,
        previewAccent = TerracottaPrimary,
        previewText = LightPaperOnSurface
    )
}

@Immutable
data class AppExtendedColors(
    val isDark: Boolean,
    val cardBorder: Color,
    val subtleFill: Color,
    val pixelStageBg: Color,
    val pixelStageBorder: Color,
    val accentWarm: Color,
    val heroBannerBg: Color,
    val heroBannerContent: Color,
    val genderDer: Color,
    val genderDerBg: Color,
    val genderDie: Color,
    val genderDieBg: Color,
    val genderDas: Color,
    val genderDasBg: Color,
    val warningYellow: Color,
    val warningYellowBg: Color
)

private val LightExtendedColors = AppExtendedColors(
    isDark = false,
    cardBorder = LightPaperOutline,
    subtleFill = Color(0xFFF1ECE3),
    pixelStageBg = Color(0xFFF3EFE6),
    pixelStageBorder = Color(0xFFDFD6C7),
    accentWarm = TerracottaPrimary,
    heroBannerBg = Color(0xFF2B2521),
    heroBannerContent = Color(0xFFF9F6F0),
    genderDer = GenderDerMasculine,
    genderDerBg = GenderDerContainer,
    genderDie = GenderDieFeminine,
    genderDieBg = GenderDieContainer,
    genderDas = GenderDasNeuter,
    genderDasBg = GenderDasContainer,
    warningYellow = WarningYellow,
    warningYellowBg = WarningYellowContainer
)

private val DarkExtendedColors = AppExtendedColors(
    isDark = true,
    cardBorder = DarkCharcoalOutline,
    subtleFill = Color(0xFF25221F),
    pixelStageBg = Color(0xFF171513),
    pixelStageBorder = Color(0xFF3B3630),
    accentWarm = TerracottaDark,
    heroBannerBg = Color(0xFF27221E),
    heroBannerContent = Color(0xFFF4EFE7),
    genderDer = GenderDerOnDark,
    genderDerBg = Color(0xFF1D2F42),
    genderDie = GenderDieOnDark,
    genderDieBg = Color(0xFF3F2229),
    genderDas = GenderDasOnDark,
    genderDasBg = Color(0xFF1A362D),
    warningYellow = WarningYellowOnDark,
    warningYellowBg = WarningYellowDarkContainer
)

private val SepiaExtendedColors = AppExtendedColors(
    isDark = false,
    cardBorder = SepiaOutline,
    subtleFill = Color(0xFFECE1C8),
    pixelStageBg = Color(0xFFEDE2CA),
    pixelStageBorder = Color(0xFFCBB894),
    accentWarm = SepiaInkPrimary,
    heroBannerBg = Color(0xFF3B2B1F),
    heroBannerContent = Color(0xFFFAF3E3),
    genderDer = Color(0xFF23527A),
    genderDerBg = Color(0xFFE0E8EE),
    genderDie = Color(0xFF9F2C40),
    genderDieBg = Color(0xFFF3DDD9),
    genderDas = Color(0xFF245F47),
    genderDasBg = Color(0xFFDDEADE),
    warningYellow = Color(0xFFA67200),
    warningYellowBg = Color(0xFFFDF0C2)
)

private val SageExtendedColors = AppExtendedColors(
    isDark = false,
    cardBorder = SageOutline,
    subtleFill = Color(0xFFE1EBE1),
    pixelStageBg = Color(0xFFE3ECE3),
    pixelStageBorder = Color(0xFFB5C9B6),
    accentWarm = ForestPinePrimary,
    heroBannerBg = Color(0xFF1B3A30),
    heroBannerContent = Color(0xFFF3F9F4),
    genderDer = GenderDerMasculine,
    genderDerBg = Color(0xFFE1ECEF),
    genderDie = GenderDieFeminine,
    genderDieBg = Color(0xFFF5E3E5),
    genderDas = ForestPinePrimary,
    genderDasBg = Color(0xFFD8EBE0),
    warningYellow = WarningYellow,
    warningYellowBg = WarningYellowContainer
)

val LocalAppExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightPaperColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E2D8),
    onPrimaryContainer = Color(0xFF642811),
    secondary = ForestPinePrimary,
    onSecondary = Color.White,
    tertiary = StreakAmber,
    onTertiary = Color.White,
    background = LightPaperBg,
    onBackground = LightPaperOnSurface,
    surface = LightPaperSurface,
    onSurface = LightPaperOnSurface,
    surfaceVariant = LightPaperVariant,
    onSurfaceVariant = LightPaperOnSurfaceVariant,
    outline = LightPaperOutline,
    outlineVariant = LightPaperOutline,
    error = ErrorRed
)

private val DarkCharcoalColorScheme = darkColorScheme(
    primary = TerracottaDark,
    onPrimary = Color(0xFF1F1009),
    primaryContainer = Color(0xFF4A2516),
    onPrimaryContainer = Color(0xFFFBDCCF),
    secondary = GenderDasOnDark,
    onSecondary = Color(0xFF0A241A),
    tertiary = Color(0xFFE5A443),
    onTertiary = Color.Black,
    background = DarkCharcoalBg,
    onBackground = DarkCharcoalOnSurface,
    surface = DarkCharcoalSurface,
    onSurface = DarkCharcoalOnSurface,
    surfaceVariant = DarkCharcoalVariant,
    onSurfaceVariant = DarkCharcoalOnSurfaceVariant,
    outline = DarkCharcoalOutline,
    outlineVariant = DarkCharcoalOutline,
    error = Color(0xFFEF6B6B)
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaInkPrimary,
    onPrimary = Color(0xFFFAF4E6),
    primaryContainer = Color(0xFFEED9C7),
    onPrimaryContainer = Color(0xFF52210E),
    secondary = Color(0xFF3E624E),
    onSecondary = Color(0xFFFAF4E6),
    tertiary = Color(0xFFB3741B),
    onTertiary = Color.White,
    background = SepiaBg,
    onBackground = SepiaOnSurface,
    surface = SepiaSurface,
    onSurface = SepiaOnSurface,
    surfaceVariant = SepiaVariant,
    onSurfaceVariant = SepiaOnSurfaceVariant,
    outline = SepiaOutline,
    outlineVariant = SepiaOutline,
    error = Color(0xFFAE3434)
)

private val SageColorScheme = lightColorScheme(
    primary = ForestPinePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E9DE),
    onPrimaryContainer = Color(0xFF123227),
    secondary = TerracottaPrimary,
    onSecondary = Color.White,
    tertiary = StreakAmber,
    onTertiary = Color.White,
    background = SageBg,
    onBackground = SageOnSurface,
    surface = SageSurface,
    onSurface = SageOnSurface,
    surfaceVariant = SageVariant,
    onSurfaceVariant = SageOnSurfaceVariant,
    outline = SageOutline,
    outlineVariant = SageOutline,
    error = ErrorRed
)

@Composable
fun DeutschLernTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        AppThemeMode.SYSTEM -> if (systemDark) AppThemeMode.DARK else AppThemeMode.LIGHT
        else -> themeMode
    }

    val colorScheme = when (effectiveMode) {
        AppThemeMode.LIGHT -> LightPaperColorScheme
        AppThemeMode.DARK -> DarkCharcoalColorScheme
        AppThemeMode.SEPIA -> SepiaColorScheme
        AppThemeMode.SAGE -> SageColorScheme
        AppThemeMode.SYSTEM -> LightPaperColorScheme
    }

    val extendedColors = when (effectiveMode) {
        AppThemeMode.LIGHT -> LightExtendedColors
        AppThemeMode.DARK -> DarkExtendedColors
        AppThemeMode.SEPIA -> SepiaExtendedColors
        AppThemeMode.SAGE -> SageExtendedColors
        AppThemeMode.SYSTEM -> LightExtendedColors
    }

    CompositionLocalProvider(LocalAppExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
