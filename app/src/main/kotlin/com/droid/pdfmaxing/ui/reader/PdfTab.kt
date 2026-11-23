package com.droid.pdfmaxing.ui.reader

import android.net.Uri
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import java.util.UUID

data class PdfTab(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri?,
    val isAsset: Boolean = false,
    val assetPath: String? = null,
    val title: String,
    val pageCount: Int,
    val currentPage: Int = 1,
    val zoomLevel: Float = 1.0f,
    val colorMode: PdfColorMode = PdfColorMode.NORMAL,
    val layoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100,
    val fileSizeBytes: Long = 0L
)

object PdfTabHelper {
    fun calculateNextActiveTabId(
        tabs: List<PdfTab>,
        currentActiveId: String?,
        closingId: String
    ): String? {
        if (tabs.size <= 1) return null
        if (currentActiveId != closingId) return currentActiveId

        val closingIndex = tabs.indexOfFirst { it.id == closingId }
        if (closingIndex == -1) return currentActiveId

        return if (closingIndex + 1 < tabs.size) {
            tabs[closingIndex + 1].id
        } else if (closingIndex - 1 >= 0) {
            tabs[closingIndex - 1].id
        } else {
            null
        }
    }
}
