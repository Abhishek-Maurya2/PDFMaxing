package com.droid.pdfmaxing.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfReaderStateTest {

    @Test
    fun testZoomClamping() {
        val helper = PdfZoomHelper(minZoom = 0.5f, maxZoom = 5.0f)
        assertEquals(1.0f, helper.clampZoom(1.0f), 0.001f)
        assertEquals(0.5f, helper.clampZoom(0.2f), 0.001f)
        assertEquals(5.0f, helper.clampZoom(8.0f), 0.001f)
        assertEquals(2.5f, helper.clampZoom(2.5f), 0.001f)
    }

    @Test
    fun testZoomSteps() {
        val helper = PdfZoomHelper(minZoom = 0.5f, maxZoom = 5.0f)
        // Zooming in from 1.0f with standard step 0.25f
        assertEquals(1.25f, helper.calculateZoomIn(1.0f), 0.001f)
        // Zooming out from 1.0f
        assertEquals(0.75f, helper.calculateZoomOut(1.0f), 0.001f)
        // Max zoom clamp
        assertEquals(5.0f, helper.calculateZoomIn(4.9f), 0.001f)
        // Min zoom clamp
        assertEquals(0.5f, helper.calculateZoomOut(0.6f), 0.001f)
    }

    @Test
    fun testDoubleTapZoomCycling() {
        val helper = PdfZoomHelper(minZoom = 0.5f, maxZoom = 5.0f)
        // At 1.0f, double tap zooms to 2.0f
        assertEquals(2.0f, helper.nextDoubleTapZoom(1.0f), 0.001f)
        // At 2.0f, double tap zooms to 3.0f
        assertEquals(3.0f, helper.nextDoubleTapZoom(2.0f), 0.001f)
        // At 3.0f, double tap resets to 1.0f
        assertEquals(1.0f, helper.nextDoubleTapZoom(3.0f), 0.001f)
    }

    @Test
    fun testPageBounds() {
        val pageCount = 10
        assertEquals(1, PdfPageHelper.clampPage(0, pageCount))
        assertEquals(5, PdfPageHelper.clampPage(5, pageCount))
        assertEquals(10, PdfPageHelper.clampPage(15, pageCount))
    }
}
