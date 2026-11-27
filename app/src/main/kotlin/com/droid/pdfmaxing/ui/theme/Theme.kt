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

import android.app.Activity
import android.os.Build
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.droid.pdfmaxing.data.settings.ThemeMode

/**
 * M3 Expressive 10-step corner scale mappings.
 * extraSmall = 4dp, small = 8dp, medium = 12dp, large = 16dp, extraLarge = 28dp
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalReduceMotion = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PDFMaxingTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppColorPalette = AppColorPalette.DYNAMIC,
    useDynamicColor: Boolean = true,
    reduceMotion: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val baseScheme = when {
        useDynamicColor &&
            palette == AppColorPalette.DYNAMIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> staticColorSchemeForPalette(palette, darkTheme)
    }

    val colorScheme = if (highContrast) {
        applyHighContrast(baseScheme, darkTheme)
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            if (Build.VERSION.SDK_INT >= 35) {
                val mask = WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS or
                    WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
                val appearance = if (!darkTheme) {
                    WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS or
                        WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
                } else {
                    WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
                }
                window.insetsController?.setSystemBarsAppearance(appearance, mask)
            }
        }
    }

    val motionScheme = if (reduceMotion) {
        MotionScheme.standard()
    } else {
        MotionScheme.expressive()
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            shapes = ExpressiveShapes,
            typography = Typography,
            content = content
        )
    }
}

private fun applyHighContrast(scheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            outline = Color(0xFFD0D0D0),
            outlineVariant = Color(0xFF909090)
        )
    } else {
        scheme.copy(
            background = Color.White,
            surface = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            outline = Color(0xFF303030),
            outlineVariant = Color(0xFF606060)
        )
    }
}
