package com.droid.pdfmaxing

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.droid.pdfmaxing.ui.library.LibraryScreen
import com.droid.pdfmaxing.ui.reader.PdfReaderScreen
import com.droid.pdfmaxing.ui.theme.PDFMaxingTheme

sealed interface AppScreen {
    data object Library : AppScreen
    data class Reader(val uri: Uri?, val isSampleGuide: Boolean = false) : AppScreen
}

class MainActivity : ComponentActivity() {

    private var currentScreenState by mutableStateOf<AppScreen>(AppScreen.Library)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 35) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
                WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
            )
        }

        handleIntent(intent)

        setContent {
            PDFMaxingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreenState) {
                        is AppScreen.Library -> {
                            LibraryScreen(
                                onOpenPdfUri = { uri ->
                                    takePersistablePermission(uri)
                                    currentScreenState = AppScreen.Reader(uri = uri, isSampleGuide = false)
                                },
                                onOpenSampleGuide = {
                                    currentScreenState = AppScreen.Reader(uri = null, isSampleGuide = true)
                                }
                            )
                        }
                        is AppScreen.Reader -> {
                            PdfReaderScreen(
                                documentUri = screen.uri,
                                isSampleGuide = screen.isSampleGuide,
                                onBackToLibrary = {
                                    currentScreenState = AppScreen.Library
                                }
                            )
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

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) &&
            (type == "application/pdf" || intent.data?.toString()?.endsWith(".pdf", ignoreCase = true) == true)
        ) {
            val uri = intent.data ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) {
                takePersistablePermission(uri)
                currentScreenState = AppScreen.Reader(uri = uri, isSampleGuide = false)
            }
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        if (uri.scheme == "content") {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Transient permission granted by external intent
            }
        }
    }
}
