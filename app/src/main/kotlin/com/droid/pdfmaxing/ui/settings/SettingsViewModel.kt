package com.droid.pdfmaxing.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droid.pdfmaxing.data.settings.AppSettings
import com.droid.pdfmaxing.data.settings.AppSettingsRepository
import com.droid.pdfmaxing.data.settings.ThemeMode
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.AppColorPalette
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppSettingsRepository(application)

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun setColorPalette(palette: AppColorPalette) {
        viewModelScope.launch { repository.updateColorPalette(palette) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.updateUseDynamicColor(enabled) }
    }

    fun setDefaultLayoutMode(mode: PdfViewLayoutMode) {
        viewModelScope.launch { repository.updateDefaultLayoutMode(mode) }
    }

    fun setDefaultColorMode(mode: PdfColorMode) {
        viewModelScope.launch { repository.updateDefaultColorMode(mode) }
    }

    fun setRememberPerDocumentSettings(enabled: Boolean) {
        viewModelScope.launch { repository.updateRememberPerDocumentSettings(enabled) }
    }

    fun setDoubleTapToZoom(enabled: Boolean) {
        viewModelScope.launch { repository.updateDoubleTapToZoom(enabled) }
    }

    fun setTapToToggleImmersive(enabled: Boolean) {
        viewModelScope.launch { repository.updateTapToToggleImmersive(enabled) }
    }

    fun setPinchToZoom(enabled: Boolean) {
        viewModelScope.launch { repository.updatePinchToZoom(enabled) }
    }

    fun setShowTabBarOnSingleTab(enabled: Boolean) {
        viewModelScope.launch { repository.updateShowTabBarOnSingleTab(enabled) }
    }

    fun setAutoOpenSidebar(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoOpenSidebar(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { repository.updateReduceMotion(enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { repository.updateHighContrast(enabled) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }
}
