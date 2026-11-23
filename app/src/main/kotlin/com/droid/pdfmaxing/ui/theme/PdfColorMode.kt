package com.droid.pdfmaxing.ui.theme

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Color modes for PDF document page rendering.
 *
 * Normal: Original page colors.
 * Inverted: High-contrast dark mode where white turns black and black turns white.
 * Night Slate: Soft dark mode mapping white to dark slate and black to soft cream.
 */
enum class PdfColorMode(val displayName: String) {
    NORMAL("Standard") {
        override fun toColorMatrix(): ColorMatrix? = null
    },
    INVERTED("High-Contrast Dark") {
        override fun toColorMatrix(): ColorMatrix = ColorMatrix(
            floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                 0f, -1f,  0f, 0f, 255f,
                 0f,  0f, -1f, 0f, 255f,
                 0f,  0f,  0f, 1f,   0f
            )
        )
    },
    NIGHT_SLATE("Soft Night Slate") {
        override fun toColorMatrix(): ColorMatrix = ColorMatrix(
            floatArrayOf(
                -0.85f, 0f,     0f,     0f, 220f,
                 0f,    -0.85f, 0f,     0f, 220f,
                 0f,     0f,    -0.85f, 0f, 225f,
                 0f,     0f,     0f,    1f,   0f
            )
        )
    };

    abstract fun toColorMatrix(): ColorMatrix?

    fun toColorFilter(): ColorFilter? {
        val matrix = toColorMatrix() ?: return null
        return ColorFilter.colorMatrix(matrix)
    }

    fun next(): PdfColorMode {
        val values = entries
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}
