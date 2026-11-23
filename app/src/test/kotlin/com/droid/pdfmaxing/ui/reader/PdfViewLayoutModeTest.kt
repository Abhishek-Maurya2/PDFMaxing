package com.droid.pdfmaxing.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfViewLayoutModeTest {

    @Test
    fun testModeCycle() {
        assertEquals(PdfViewLayoutMode.FIT_50, PdfViewLayoutMode.FIT_100.next())
        assertEquals(PdfViewLayoutMode.TWO_PAGE, PdfViewLayoutMode.FIT_50.next())
        assertEquals(PdfViewLayoutMode.FIT_100, PdfViewLayoutMode.TWO_PAGE.next())
    }

    @Test
    fun testPairedPagesCalculation() {
        // Even total pages: 4 pages -> 2 pairs
        val pairs4 = PdfTwoPageHelper.createPagePairs(4)
        assertEquals(2, pairs4.size)
        assertEquals(Pair(0, 1), pairs4[0])
        assertEquals(Pair(2, 3), pairs4[1])

        // Odd total pages: 3 pages -> 2 pairs (last has no second page)
        val pairs3 = PdfTwoPageHelper.createPagePairs(3)
        assertEquals(2, pairs3.size)
        assertEquals(Pair(0, 1), pairs3[0])
        assertEquals(Pair(2, null), pairs3[1])

        // 1 page -> 1 pair
        val pairs1 = PdfTwoPageHelper.createPagePairs(1)
        assertEquals(1, pairs1.size)
        assertEquals(Pair(0, null), pairs1[0])

        // 0 pages -> empty
        val pairs0 = PdfTwoPageHelper.createPagePairs(0)
        assertEquals(0, pairs0.size)
    }
}
