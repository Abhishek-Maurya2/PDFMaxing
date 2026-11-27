package com.droid.pdfmaxing.data.model

import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import java.util.Locale

/**
 * Model representing a recently opened PDF document with metadata and reading progress.
 */
data class RecentDocument(
    val uriString: String,
    val title: String,
    val pageCount: Int,
    val lastReadPage: Int = 1,
    val scrollOffset: Int = 0,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val isAsset: Boolean = false,
    val assetPath: String? = null,
    val zoomLevel: Float = 1.0f,
    val colorMode: PdfColorMode = PdfColorMode.NORMAL,
    val layoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100
) {
    /**
     * Reading progress percentage from 0 to 100.
     */
    val progressPercentage: Int
        get() {
            if (pageCount <= 0) return 0
            val pct = ((lastReadPage.toFloat() / pageCount.toFloat()) * 100f).toInt()
            return pct.coerceIn(0, 100)
        }

    /**
     * Human-readable file size format.
     */
    val formattedFileSize: String
        get() {
            if (fileSizeBytes <= 0L) return "Unknown size"
            val kb = fileSizeBytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$fileSizeBytes B"
            }
        }
}
