package com.droid.pdfmaxing.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PdfColorModeTest {

    @Test
    fun testNormalModeHasNullFilter() {
        val normalMode = PdfColorMode.NORMAL
        assertEquals("Standard", normalMode.displayName)
        assertNull(normalMode.toColorMatrix())
    }

    @Test
    fun testInvertedModeHasNegativeMatrix() {
        val invertedMode = PdfColorMode.INVERTED
        assertEquals("High-Contrast Dark", invertedMode.displayName)
        val matrix = invertedMode.toColorMatrix()
        assertNotNull(matrix)

        val array = matrix!!.values
        // Check red inversion: R' = -1 * R + 255
        assertEquals(-1f, array[0], 0.001f)
        assertEquals(255f, array[4], 0.001f)
        // Check green inversion: G' = -1 * G + 255
        assertEquals(-1f, array[6], 0.001f)
        assertEquals(255f, array[9], 0.001f)
        // Check blue inversion: B' = -1 * B + 255
        assertEquals(-1f, array[12], 0.001f)
        assertEquals(255f, array[14], 0.001f)
        // Alpha unchanged: A' = 1 * A + 0
        assertEquals(1f, array[18], 0.001f)
        assertEquals(0f, array[19], 0.001f)
    }

    @Test
    fun testNightSlateModeHasSoftenedMatrix() {
        val slateMode = PdfColorMode.NIGHT_SLATE
        assertEquals("Soft Night Slate", slateMode.displayName)
        val matrix = slateMode.toColorMatrix()
        assertNotNull(matrix)

        val array = matrix!!.values
        // Check softened negative values
        assertEquals(-0.85f, array[0], 0.001f)
        assertEquals(220f, array[4], 0.001f)
        assertEquals(-0.85f, array[6], 0.001f)
        assertEquals(220f, array[9], 0.001f)
        assertEquals(-0.85f, array[12], 0.001f)
        assertEquals(225f, array[14], 0.001f)
    }

    @Test
    fun testModeCycle() {
        assertEquals(PdfColorMode.INVERTED, PdfColorMode.NORMAL.next())
        assertEquals(PdfColorMode.NIGHT_SLATE, PdfColorMode.INVERTED.next())
        assertEquals(PdfColorMode.NORMAL, PdfColorMode.NIGHT_SLATE.next())
    }
}
