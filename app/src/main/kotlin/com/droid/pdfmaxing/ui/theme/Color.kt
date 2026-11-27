/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.droid.pdfmaxing.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppColorPalette(
    val displayName: String,
    val seedColor: Color,
    val accentPreviewColor: Color
) {
    DYNAMIC("Material You", Color.Transparent, Color(0xFF6750A4)),
    CORAL("Vibrant Coral", Color(0xFFA03520), Color(0xFFA03520)),
    OCEAN("Cool Ocean", Color(0xFF00506E), Color(0xFF00506E)),
    FOREST("Deep Forest", Color(0xFF2D6A4F), Color(0xFF2D6A4F)),
    PURPLE("Rich Purple", Color(0xFF6B4FA2), Color(0xFF6B4FA2)),
    AMBER("Warm Amber", Color(0xFF8B5E00), Color(0xFF8B5E00))
}

// === 1. Vibrant Coral / Amber / Violet Expressive Palette (Default Static) ===
val primaryLight = Color(0xFFA03520)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFFFDAD3)
val onPrimaryContainerLight = Color(0xFF3B0800)
val secondaryLight = Color(0xFF8B5E00)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFFFDEA6)
val onSecondaryContainerLight = Color(0xFF2C1A00)
val tertiaryLight = Color(0xFF6B4FA2)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFECDCFF)
val onTertiaryContainerLight = Color(0xFF250F5A)
val backgroundLight = Color(0xFFFFF8F6)
val onBackgroundLight = Color(0xFF231917)
val surfaceLight = Color(0xFFFFF8F6)
val onSurfaceLight = Color(0xFF231917)
val surfaceVariantLight = Color(0xFFF5DDD8)
val onSurfaceVariantLight = Color(0xFF534340)
val outlineLight = Color(0xFF85736F)
val outlineVariantLight = Color(0xFFD8C2BD)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFFF0ED)
val surfaceContainerLight = Color(0xFFFCEAE6)
val surfaceContainerHighLight = Color(0xFFF6E4E0)
val surfaceContainerHighestLight = Color(0xFFF0DFDB)

val primaryDark = Color(0xFFFFB4A4)
val onPrimaryDark = Color(0xFF601405)
val primaryContainerDark = Color(0xFF7F2310)
val onPrimaryContainerDark = Color(0xFFFFDAD3)
val secondaryDark = Color(0xFFFBC156)
val onSecondaryDark = Color(0xFF4A3000)
val secondaryContainerDark = Color(0xFF6A4600)
val onSecondaryContainerDark = Color(0xFFFFDEA6)
val tertiaryDark = Color(0xFFD5BBFF)
val onTertiaryDark = Color(0xFF3B1F71)
val tertiaryContainerDark = Color(0xFF523789)
val onTertiaryContainerDark = Color(0xFFECDCFF)
val backgroundDark = Color(0xFF1A110F)
val onBackgroundDark = Color(0xFFF1DFDB)
val surfaceDark = Color(0xFF1A110F)
val onSurfaceDark = Color(0xFFF1DFDB)
val surfaceVariantDark = Color(0xFF534340)
val onSurfaceVariantDark = Color(0xFFD8C2BD)
val outlineDark = Color(0xFFA08C88)
val outlineVariantDark = Color(0xFF534340)
val surfaceContainerLowestDark = Color(0xFF140C0A)
val surfaceContainerLowDark = Color(0xFF231917)
val surfaceContainerDark = Color(0xFF271D1B)
val surfaceContainerHighDark = Color(0xFF322825)
val surfaceContainerHighestDark = Color(0xFF3D3230)

val CoralLightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

val CoralDarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

// === 2. Cool Ocean Palette ===
val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF00506E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5E7FF),
    onPrimaryContainer = Color(0xFF001E2D),
    secondary = Color(0xFF486271),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFEAFD),
    onSecondaryContainer = Color(0xFF081F2D),
    tertiary = Color(0xFF653E78),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF28083B),
    background = Color(0xFFF7F9FD),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFF7F9FD),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFDBE4EB),
    onSurfaceVariant = Color(0xFF40484E),
    outline = Color(0xFF70787F),
    outlineVariant = Color(0xFFC0C8CF),
    surfaceContainerLow = Color(0xFFF1F4F8),
    surfaceContainer = Color(0xFFECEEF1),
    surfaceContainerHigh = Color(0xFFE6E8EC),
    surfaceContainerHighest = Color(0xFFE0E3E7)
)

val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF87CFF9),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF004C6A),
    onPrimaryContainer = Color(0xFFC5E7FF),
    secondary = Color(0xFFB0CADC),
    onSecondary = Color(0xFF193342),
    secondaryContainer = Color(0xFF304A59),
    onSecondaryContainer = Color(0xFFCFEAFD),
    tertiary = Color(0xFFE6B5F8),
    onTertiary = Color(0xFF462058),
    tertiaryContainer = Color(0xFF5D3770),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF101416),
    onBackground = Color(0xFFE0E3E6),
    surface = Color(0xFF101416),
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF40484E),
    onSurfaceVariant = Color(0xFFBFC8CF),
    outline = Color(0xFF899299),
    outlineVariant = Color(0xFF40484E),
    surfaceContainerLow = Color(0xFF181C1F),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF272A2D),
    surfaceContainerHighest = Color(0xFF323538)
)

// === 3. Deep Forest Palette ===
val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0D1),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D9),
    onSecondaryContainer = Color(0xFF0B1F16),
    tertiary = Color(0xFF8D4D28),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBC9),
    onTertiaryContainer = Color(0xFF331100),
    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF5FBF6),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDCE5DD),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C1),
    surfaceContainerLow = Color(0xFFEFF5F0),
    surfaceContainer = Color(0xFFE9EFEA),
    surfaceContainerHigh = Color(0xFFE4EAE4),
    surfaceContainerHighest = Color(0xFFDEE4DF)
)

val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BD4B6),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF105138),
    onPrimaryContainer = Color(0xFFB7F0D1),
    secondary = Color(0xFFB4CCBD),
    onSecondary = Color(0xFF20352A),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFD0E8D9),
    tertiary = Color(0xFFFFB68F),
    onTertiary = Color(0xFF532200),
    tertiaryContainer = Color(0xFF703613),
    onTertiaryContainer = Color(0xFFFFDBC9),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF404943),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF252B27),
    surfaceContainerHighest = Color(0xFF303632)
)

// === 4. Rich Purple Palette ===
val PurpleLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B4FA2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECDCFF),
    onPrimaryContainer = Color(0xFF250F5A),
    secondary = Color(0xFF635B70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1F182B),
    tertiary = Color(0xFF9E3F5B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E1),
    onTertiaryContainer = Color(0xFF3F0017),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4CF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF1ECF4),
    surfaceContainerHigh = Color(0xFFEBE6EE),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

val PurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD5BBFF),
    onPrimary = Color(0xFF3B1F71),
    primaryContainer = Color(0xFF523789),
    onPrimaryContainer = Color(0xFFECDCFF),
    secondary = Color(0xFFCDC2DB),
    onSecondary = Color(0xFF342D41),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFFFFB1C6),
    onTertiary = Color(0xFF620E2E),
    tertiaryContainer = Color(0xFF7F2744),
    onTertiaryContainer = Color(0xFFFFD9E1),
    background = Color(0xFF151218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF151218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F24),
    surfaceContainerHigh = Color(0xFF2C292F),
    surfaceContainerHighest = Color(0xFF36343A)
)

// === 5. Warm Amber Palette ===
val AmberLightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF2C1A00),
    secondary = Color(0xFF715B3E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCDFB9),
    onSecondaryContainer = Color(0xFF281904),
    tertiary = Color(0xFF51643F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4EABB),
    onTertiaryContainer = Color(0xFF102004),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF211B13),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF211B13),
    surfaceVariant = Color(0xFFF0E0CE),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF817567),
    outlineVariant = Color(0xFFD3C4B4),
    surfaceContainerLow = Color(0xFFFDF2E7),
    surfaceContainer = Color(0xFFF7ECE1),
    surfaceContainerHigh = Color(0xFFF1E6DC),
    surfaceContainerHighest = Color(0xFFEBE0D6)
)

val AmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFBC156),
    onPrimary = Color(0xFF4A3000),
    primaryContainer = Color(0xFF6A4600),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Color(0xFFDFC39F),
    onSecondary = Color(0xFF3F2D15),
    secondaryContainer = Color(0xFF584329),
    onSecondaryContainer = Color(0xFFFCDFB9),
    tertiary = Color(0xFFB8CEA0),
    onTertiary = Color(0xFF243515),
    tertiaryContainer = Color(0xFF3A4C2A),
    onTertiaryContainer = Color(0xFFD4EABB),
    background = Color(0xFF18130C),
    onBackground = Color(0xFFEBE0D6),
    surface = Color(0xFF18130C),
    onSurface = Color(0xFFEBE0D6),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    outline = Color(0xFF9B8F80),
    outlineVariant = Color(0xFF4F4539),
    surfaceContainerLow = Color(0xFF211B13),
    surfaceContainer = Color(0xFF251E17),
    surfaceContainerHigh = Color(0xFF302921),
    surfaceContainerHighest = Color(0xFF3B332B)
)

fun staticColorSchemeForPalette(palette: AppColorPalette, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        AppColorPalette.CORAL, AppColorPalette.DYNAMIC ->
            if (darkTheme) CoralDarkColorScheme else CoralLightColorScheme
        AppColorPalette.OCEAN ->
            if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
        AppColorPalette.FOREST ->
            if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
        AppColorPalette.PURPLE ->
            if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        AppColorPalette.AMBER ->
            if (darkTheme) AmberDarkColorScheme else AmberLightColorScheme
    }
}
