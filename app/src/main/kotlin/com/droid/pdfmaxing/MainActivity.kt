package com.droid.pdfmaxing

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.IntentCompat
import com.droid.pdfmaxing.ui.library.LibraryScreen
import com.droid.pdfmaxing.ui.reader.PdfReaderScreen
import com.droid.pdfmaxing.ui.reader.PdfReaderViewModel
import com.droid.pdfmaxing.ui.settings.SettingsScreen
import com.droid.pdfmaxing.ui.settings.SettingsViewModel
import com.droid.pdfmaxing.ui.theme.PDFMaxingTheme

sealed interface AppScreen {
    data object Library : AppScreen
    data class Reader(val uri: Uri?, val isSampleGuide: Boolean = false) : AppScreen
    data object Settings : AppScreen
}

class MainActivity : ComponentActivity() {

    private val readerViewModel: PdfReaderViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var currentScreenState by mutableStateOf<AppScreen>(AppScreen.Library)
    private var previousScreenState by mutableStateOf<AppScreen>(AppScreen.Library)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState != null) {
            val screenType = savedInstanceState.getString("screen_type")
            when (screenType) {
                "reader" -> {
                    val uriStr = savedInstanceState.getString("reader_uri")
                    val isSample = savedInstanceState.getBoolean("is_sample_guide", false)
                    currentScreenState = AppScreen.Reader(
                        uri = uriStr?.let { Uri.parse(it) },
                        isSampleGuide = isSample
                    )
                }
                "settings" -> {
                    currentScreenState = AppScreen.Settings
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 35) {
            val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val mask = WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS or
                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            val appearance = if (!isDark) {
                WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS or
                    WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            } else {
                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            }
            window.insetsController?.setSystemBarsAppearance(appearance, mask)
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            val readerUiState by readerViewModel.uiState.collectAsState()
            val appSettings by settingsViewModel.settings.collectAsState()

            PDFMaxingTheme(
                themeMode = appSettings.themeMode,
                palette = appSettings.colorPalette,
                useDynamicColor = appSettings.useDynamicColor,
                reduceMotion = appSettings.reduceMotion,
                highContrast = appSettings.highContrast
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val transitionEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    val transitionSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    AnimatedContent(
                        targetState = currentScreenState,
                        transitionSpec = {
                            if (appSettings.reduceMotion) {
                                fadeIn(animationSpec = transitionEffectsSpec) togetherWith fadeOut(animationSpec = transitionEffectsSpec)
                            } else {
                                val forward = when {
                                    initialState is AppScreen.Library && targetState !is AppScreen.Library -> true
                                    initialState is AppScreen.Reader && targetState is AppScreen.Settings -> true
                                    else -> false
                                }
                                val dir = if (forward) 1 else -1
                                (slideInHorizontally(animationSpec = transitionSpatialSpec) { dir * (it / 4) } +
                                    fadeIn(animationSpec = transitionEffectsSpec)) togetherWith
                                    (slideOutHorizontally(animationSpec = transitionSpatialSpec) { -dir * (it / 4) } +
                                        fadeOut(animationSpec = transitionEffectsSpec))
                            }
                        },
                        label = "AppScreenTransition"
                    ) { screen ->
                        when (screen) {
                            is AppScreen.Library -> {
                                LibraryScreen(
                                    onOpenPdfUri = { uri ->
                                        takePersistablePermission(uri)
                                        val hasTabs = readerViewModel.uiState.value.tabs.isNotEmpty()
                                        readerViewModel.loadDocument(uri, initialPage = 1, asNewTab = hasTabs)
                                        previousScreenState = AppScreen.Library
                                        currentScreenState = AppScreen.Reader(uri = uri, isSampleGuide = false)
                                    },
                                    onOpenSampleGuide = {
                                        val hasTabs = readerViewModel.uiState.value.tabs.isNotEmpty()
                                        readerViewModel.loadSampleGuide(asNewTab = hasTabs)
                                        previousScreenState = AppScreen.Library
                                        currentScreenState = AppScreen.Reader(uri = null, isSampleGuide = true)
                                    },
                                    hasOpenTabs = readerUiState.tabs.isNotEmpty(),
                                    onReturnToReader = {
                                        val active = readerViewModel.uiState.value.activeTab
                                        previousScreenState = AppScreen.Library
                                        currentScreenState = AppScreen.Reader(
                                            uri = active?.uri,
                                            isSampleGuide = active?.isAsset == true
                                        )
                                    },
                                    onOpenSettings = {
                                        previousScreenState = AppScreen.Library
                                        currentScreenState = AppScreen.Settings
                                    }
                                )
                            }
                            is AppScreen.Reader -> {
                                PdfReaderScreen(
                                    documentUri = screen.uri,
                                    isSampleGuide = screen.isSampleGuide,
                                    onBackToLibrary = {
                                        currentScreenState = AppScreen.Library
                                    },
                                    onOpenSettings = {
                                        previousScreenState = screen
                                        currentScreenState = AppScreen.Settings
                                    },
                                    appSettings = appSettings,
                                    viewModel = readerViewModel
                                )
                            }
                            is AppScreen.Settings -> {
                                SettingsScreen(
                                    onBack = {
                                        currentScreenState = if (previousScreenState is AppScreen.Settings) {
                                            AppScreen.Library
                                        } else {
                                            previousScreenState
                                        }
                                    },
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        when (val screen = currentScreenState) {
            is AppScreen.Reader -> {
                outState.putString("screen_type", "reader")
                outState.putString("reader_uri", screen.uri?.toString())
                outState.putBoolean("is_sample_guide", screen.isSampleGuide)
            }
            is AppScreen.Library -> {
                outState.putString("screen_type", "library")
            }
            is AppScreen.Settings -> {
                outState.putString("screen_type", "settings")
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) {
            val uri = if (action == Intent.ACTION_SEND) {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: intent.data
            } else {
                intent.data ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            }
            val isPdfMime = type.equals("application/pdf", ignoreCase = true) ||
                type == null ||
                type == "*/*" ||
                type.equals("application/octet-stream", ignoreCase = true)
            val isPdfUri = uri?.toString()?.endsWith(".pdf", ignoreCase = true) == true ||
                uri?.scheme == "content"
            if (uri != null && (isPdfMime || isPdfUri)) {
                takePersistablePermission(uri)
                val hasTabs = readerViewModel.uiState.value.tabs.isNotEmpty()
                readerViewModel.loadDocument(uri, initialPage = 1, asNewTab = hasTabs)
                previousScreenState = AppScreen.Library
                currentScreenState = AppScreen.Reader(uri = uri, isSampleGuide = false)
            }
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        if (uri.scheme == "content") {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
                // Transient permission granted by external intent
            }
        }
    }
}
