package com.droid.pdfmaxing.ui.reader

import com.droid.pdfmaxing.ui.theme.PdfColorMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfToolbarInteractionTest {

    @Test
    fun testDirectLayoutModeSelection() {
        var currentLayout = PdfViewLayoutMode.FIT_100
        val onSelectLayout: (PdfViewLayoutMode) -> Unit = { mode -> currentLayout = mode }

        onSelectLayout(PdfViewLayoutMode.TWO_PAGE)
        assertEquals(PdfViewLayoutMode.TWO_PAGE, currentLayout)

        onSelectLayout(PdfViewLayoutMode.FIT_50)
        assertEquals(PdfViewLayoutMode.FIT_50, currentLayout)

        onSelectLayout(PdfViewLayoutMode.FIT_100)
        assertEquals(PdfViewLayoutMode.FIT_100, currentLayout)
    }

    @Test
    fun testDirectColorModeSelection() {
        var currentColor = PdfColorMode.NORMAL
        val onSelectColor: (PdfColorMode) -> Unit = { mode -> currentColor = mode }

        onSelectColor(PdfColorMode.INVERTED)
        assertEquals(PdfColorMode.INVERTED, currentColor)

        onSelectColor(PdfColorMode.NIGHT_SLATE)
        assertEquals(PdfColorMode.NIGHT_SLATE, currentColor)

        onSelectColor(PdfColorMode.NORMAL)
        assertEquals(PdfColorMode.NORMAL, currentColor)
    }

    @Test
    fun testColorModeDisplayNames() {
        assertEquals("Standard", PdfColorMode.NORMAL.displayName)
        assertEquals("High-Contrast Dark", PdfColorMode.INVERTED.displayName)
        assertEquals("Soft Night Slate", PdfColorMode.NIGHT_SLATE.displayName)
    }

    @Test
    fun testViewLayoutModeDisplayNames() {
        assertEquals("Fit 100%", PdfViewLayoutMode.FIT_100.displayName)
        assertEquals("50% Overview", PdfViewLayoutMode.FIT_50.displayName)
        assertEquals("Two-Page Spread", PdfViewLayoutMode.TWO_PAGE.displayName)
    }
}
