package com.droid.pdfmaxing.ui.reader

import kotlin.math.roundToInt

class PdfZoomHelper(
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 5.0f,
    val step: Float = 0.25f
) {
    fun clampZoom(zoom: Float): Float = zoom.coerceIn(minZoom, maxZoom)

    fun calculateZoomIn(current: Float): Float {
        val next = current + step
        return clampZoom((next * 100).roundToInt() / 100f)
    }

    fun calculateZoomOut(current: Float): Float {
        val next = current - step
        return clampZoom((next * 100).roundToInt() / 100f)
    }

    fun nextDoubleTapZoom(current: Float): Float {
        return when {
            current < 1.8f -> 2.0f
            current < 2.8f -> 3.0f
            else -> 1.0f
        }
    }
}

object PdfPageHelper {
    fun clampPage(page: Int, totalPages: Int): Int {
        if (totalPages <= 0) return 1
        return page.coerceIn(1, totalPages)
    }
}
