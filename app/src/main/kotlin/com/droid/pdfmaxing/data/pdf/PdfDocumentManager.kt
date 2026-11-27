package com.droid.pdfmaxing.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class PdfDocumentManager(private val context: Context) {

    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val renderMutex = Mutex()
    private var cachedPdfBytes: ByteArray? = null
    private var assetCacheFile: File? = null
    private var currentUri: Uri? = null

    var pageCount: Int = 0
        private set

    var documentTitle: String = "Document.pdf"
        private set

    var fileSizeBytes: Long = 0L
        private set

    suspend fun openUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        close()
        documentTitle = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Document.pdf"
        fileSizeBytes = 0L
        try {
            // Resolve document name and size from content provider
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let {
                                documentTitle = it
                            }
                        }
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            fileSizeBytes = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } else if (uri.scheme == "file") {
                val file = uri.path?.let { File(it) }
                if (file != null && file.exists()) {
                    documentTitle = file.name
                    fileSizeBytes = file.length()
                }
            }

            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext false

            if (fileSizeBytes <= 0L) {
                val statSize = pfd.statSize
                if (statSize > 0L) fileSizeBytes = statSize
            }

            currentUri = uri
            parcelFileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    suspend fun openAsset(assetPath: String, title: String): Boolean = withContext(Dispatchers.IO) {
        close()
        try {
            documentTitle = title
            // Clean up any orphaned temporary asset files from previous killed sessions
            context.cacheDir.listFiles { file ->
                file.name.startsWith("temp_asset_") && file.name.endsWith(".pdf")
            }?.forEach { stale ->
                runCatching { stale.delete() }
            }
            // Copy asset to a temporary cache file to obtain a ParcelFileDescriptor
            val cacheFile = File(context.cacheDir, "temp_asset_${System.currentTimeMillis()}.pdf")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            fileSizeBytes = cacheFile.length()
            assetCacheFile = cacheFile

            val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount
            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex !in 0 until pageCount) return@withContext null

        renderMutex.withLock {
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                Pair(page.width, page.height)
            } catch (e: Exception) {
                null
            } finally {
                page?.close()
            }
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        targetWidth: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex !in 0 until pageCount) return@withContext null

        renderMutex.withLock {
            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val originalWidth = page.width
                val originalHeight = page.height
                if (originalWidth <= 0 || originalHeight <= 0) return@withLock null

                val aspectRatio = originalHeight.toFloat() / originalWidth.toFloat()

                // Calculate supersampled rendering resolution for vector sharpness.
                // Rendering at higher density removes the "blurry image" raster artifact
                // and renders pristine font glyphs and line vectors.
                val supersample = when {
                    targetWidth <= 600 -> 2.0f
                    targetWidth <= 1200 -> 1.75f
                    targetWidth <= 1800 -> 1.5f
                    targetWidth <= 2400 -> 1.25f
                    else -> 1.1f
                }
                val effectiveWidth = (targetWidth * supersample).roundToInt().coerceIn(300, 3400)
                val effectiveHeight = (effectiveWidth * aspectRatio).roundToInt().coerceIn(300, 4800)

                val bitmap = Bitmap.createBitmap(effectiveWidth, effectiveHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE) // Initialize with solid white background

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                page?.close()
            }
        }
    }

    suspend fun renderThumbnail(
        pageIndex: Int,
        thumbnailWidth: Int = 180
    ): Bitmap? = withContext(Dispatchers.IO) {
        renderPage(pageIndex, targetWidth = thumbnailWidth)
    }

    suspend fun extractPageText(pageIndex: Int): String = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext ""
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext ""
        PdfSearchEngine.extractPageText(renderer, pageIndex)
    }

    suspend fun extractPageTextSegments(pageIndex: Int): List<PdfTextSegment> = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext emptyList()
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext emptyList()
        PdfSearchEngine.extractPageTextSegments(renderer, pageIndex)
    }

    suspend fun selectContent(
        pageIndex: Int,
        startNormX: Float,
        startNormY: Float,
        stopNormX: Float,
        stopNormY: Float
    ): PageSelectionResult? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null
        PdfSearchEngine.selectContentNative(
            renderer,
            pageIndex,
            startNormX,
            startNormY,
            stopNormX,
            stopNormY
        )
    }

    suspend fun searchDocument(query: String): List<PdfSearchMatch> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        val renderer = pdfRenderer ?: return@withContext emptyList()

        // 1. Try native Android 15+ (API 35+) search first if available
        if (Build.VERSION.SDK_INT >= 35) {
            val nativeMatches = mutableListOf<PdfSearchMatch>()
            var nativeSupported = true

            for (p in 0 until pageCount) {
                // Lock renderMutex per-page so UI renders and page scrolls interleave smoothly without UI freeze
                val pageMatches = renderMutex.withLock {
                    PdfSearchEngine.searchWithNativeApi(renderer, p, cleanQuery)
                }
                if (pageMatches != null) {
                    nativeMatches.addAll(pageMatches)
                } else {
                    nativeSupported = false
                    break
                }
            }

            if (nativeSupported && nativeMatches.isNotEmpty()) {
                return@withContext nativeMatches
            }
        }

        // 2. Stream-based fallback search (works on all devices & API levels)
        val bytes = cachedPdfBytes ?: run {
            val loaded = when {
                assetCacheFile != null -> {
                    try { assetCacheFile?.readBytes() } catch (e: Exception) { null }
                }
                currentUri != null -> {
                    try {
                        context.contentResolver.openInputStream(currentUri!!)?.use { it.readBytes() }
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }
            if (loaded != null && loaded.size <= 25 * 1024 * 1024) {
                cachedPdfBytes = loaded
            }
            loaded
        } ?: return@withContext emptyList()

        PdfSearchEngine.searchInPdfBytes(bytes, cleanQuery)
    }

    fun close() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            parcelFileDescriptor?.close()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            assetCacheFile?.delete()
        } catch (e: Exception) {
            // Ignore
        }
        pdfRenderer = null
        parcelFileDescriptor = null
        cachedPdfBytes = null
        assetCacheFile = null
        currentUri = null
        pageCount = 0
    }
}
