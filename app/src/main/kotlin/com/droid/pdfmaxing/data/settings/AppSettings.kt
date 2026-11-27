package com.droid.pdfmaxing.data.settings

import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.AppColorPalette
import com.droid.pdfmaxing.ui.theme.PdfColorMode

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

data class AppSettings(
    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.DYNAMIC,
    val useDynamicColor: Boolean = true,

    // Reading Defaults
    val defaultLayoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100,
    val defaultColorMode: PdfColorMode = PdfColorMode.NORMAL,
    val rememberPerDocumentSettings: Boolean = true,

    // Gestures
    val doubleTapToZoom: Boolean = true,
    val tapToToggleImmersive: Boolean = true,
    val pinchToZoom: Boolean = true,

    // Display
    val showTabBarOnSingleTab: Boolean = false,
    val autoOpenSidebar: Boolean = false,

    // Accessibility
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false
)
