package com.droid.pdfmaxing.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.AppColorPalette
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_app_settings")

class AppSettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val DEFAULT_LAYOUT_MODE = stringPreferencesKey("default_layout_mode")
        val DEFAULT_COLOR_MODE = stringPreferencesKey("default_color_mode")
        val REMEMBER_PER_DOC = booleanPreferencesKey("remember_per_doc_settings")
        val DOUBLE_TAP_ZOOM = booleanPreferencesKey("double_tap_to_zoom")
        val TAP_IMMERSIVE = booleanPreferencesKey("tap_to_toggle_immersive")
        val PINCH_TO_ZOOM = booleanPreferencesKey("pinch_to_zoom")
        val SHOW_TAB_BAR_SINGLE = booleanPreferencesKey("show_tab_bar_single_tab")
        val AUTO_OPEN_SIDEBAR = booleanPreferencesKey("auto_open_sidebar")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorPalette = prefs[Keys.COLOR_PALETTE]?.let { runCatching { AppColorPalette.valueOf(it) }.getOrNull() }
                ?: AppColorPalette.DYNAMIC,
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            defaultLayoutMode = prefs[Keys.DEFAULT_LAYOUT_MODE]?.let { runCatching { PdfViewLayoutMode.valueOf(it) }.getOrNull() }
                ?: PdfViewLayoutMode.FIT_100,
            defaultColorMode = prefs[Keys.DEFAULT_COLOR_MODE]?.let { runCatching { PdfColorMode.valueOf(it) }.getOrNull() }
                ?: PdfColorMode.NORMAL,
            rememberPerDocumentSettings = prefs[Keys.REMEMBER_PER_DOC] ?: true,
            doubleTapToZoom = prefs[Keys.DOUBLE_TAP_ZOOM] ?: true,
            tapToToggleImmersive = prefs[Keys.TAP_IMMERSIVE] ?: true,
            pinchToZoom = prefs[Keys.PINCH_TO_ZOOM] ?: true,
            showTabBarOnSingleTab = prefs[Keys.SHOW_TAB_BAR_SINGLE] ?: false,
            autoOpenSidebar = prefs[Keys.AUTO_OPEN_SIDEBAR] ?: false,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateColorPalette(palette: AppColorPalette) {
        context.settingsDataStore.edit {
            it[Keys.COLOR_PALETTE] = palette.name
            it[Keys.USE_DYNAMIC_COLOR] = (palette == AppColorPalette.DYNAMIC)
        }
    }

    suspend fun updateUseDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit {
            it[Keys.USE_DYNAMIC_COLOR] = enabled
            if (enabled) {
                it[Keys.COLOR_PALETTE] = AppColorPalette.DYNAMIC.name
            } else if (it[Keys.COLOR_PALETTE] == AppColorPalette.DYNAMIC.name || it[Keys.COLOR_PALETTE] == null) {
                it[Keys.COLOR_PALETTE] = AppColorPalette.CORAL.name
            }
        }
    }

    suspend fun updateDefaultLayoutMode(mode: PdfViewLayoutMode) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_LAYOUT_MODE] = mode.name }
    }

    suspend fun updateDefaultColorMode(mode: PdfColorMode) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_COLOR_MODE] = mode.name }
    }

    suspend fun updateRememberPerDocumentSettings(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REMEMBER_PER_DOC] = enabled }
    }

    suspend fun updateDoubleTapToZoom(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DOUBLE_TAP_ZOOM] = enabled }
    }

    suspend fun updateTapToToggleImmersive(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.TAP_IMMERSIVE] = enabled }
    }

    suspend fun updatePinchToZoom(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PINCH_TO_ZOOM] = enabled }
    }

    suspend fun updateShowTabBarOnSingleTab(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_TAB_BAR_SINGLE] = enabled }
    }

    suspend fun updateAutoOpenSidebar(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_OPEN_SIDEBAR] = enabled }
    }

    suspend fun updateReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun updateHighContrast(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }
}
