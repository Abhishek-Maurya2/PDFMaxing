package com.droid.pdfmaxing.data.model

import com.droid.pdfmaxing.data.RecentDocumentRepository
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentDocumentTest {

    @Test
    fun testProgressPercentageCalculation() {
        val doc1 = RecentDocument(
            uriString = "content://docs/1",
            title = "Report.pdf",
            pageCount = 100,
            lastReadPage = 25
        )
        assertEquals(25, doc1.progressPercentage)

        val doc2 = RecentDocument(
            uriString = "content://docs/2",
            title = "Book.pdf",
            pageCount = 0,
            lastReadPage = 0
        )
        assertEquals(0, doc2.progressPercentage)

        val doc3 = RecentDocument(
            uriString = "content://docs/3",
            title = "Manual.pdf",
            pageCount = 50,
            lastReadPage = 50
        )
        assertEquals(100, doc3.progressPercentage)
    }

    @Test
    fun testFormattedFileSize() {
        val docBytes = RecentDocument(
            uriString = "content://docs/1",
            title = "Test.pdf",
            pageCount = 10,
            fileSizeBytes = 500L
        )
        assertEquals("500 B", docBytes.formattedFileSize)

        val docKb = RecentDocument(
            uriString = "content://docs/2",
            title = "Test.pdf",
            pageCount = 10,
            fileSizeBytes = 2048L
        )
        assertEquals("2.0 KB", docKb.formattedFileSize)

        val docMb = RecentDocument(
            uriString = "content://docs/3",
            title = "Test.pdf",
            pageCount = 10,
            fileSizeBytes = 5 * 1024 * 1024L
        )
        assertEquals("5.0 MB", docMb.formattedFileSize)
    }

    @Test
    fun testDefaultZoomAndTheme() {
        val doc = RecentDocument(
            uriString = "content://docs/default",
            title = "Doc.pdf",
            pageCount = 20,
            lastReadPage = 1
        )
        assertEquals(1.0f, doc.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.NORMAL, doc.colorMode)
    }

    @Test
    fun testCustomZoomAndTheme() {
        val doc = RecentDocument(
            uriString = "content://docs/custom",
            title = "Doc.pdf",
            pageCount = 20,
            lastReadPage = 8,
            zoomLevel = 1.75f,
            colorMode = PdfColorMode.NIGHT_SLATE
        )
        assertEquals(8, doc.lastReadPage)
        assertEquals(1.75f, doc.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.NIGHT_SLATE, doc.colorMode)
    }

    @Test
    fun testCopyUpdatesZoomAndTheme() {
        val doc = RecentDocument(
            uriString = "content://docs/1",
            title = "Doc.pdf",
            pageCount = 10
        )
        assertEquals(1, doc.lastReadPage)
        assertEquals(1.0f, doc.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.NORMAL, doc.colorMode)

        val updated = doc.copy(
            lastReadPage = 5,
            zoomLevel = 2.5f,
            colorMode = PdfColorMode.INVERTED
        )
        assertEquals(5, updated.lastReadPage)
        assertEquals(2.5f, updated.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.INVERTED, updated.colorMode)
    }

    @Test
    fun testSerializeAndParseRoundTripPreservesLayoutModeAndViewState() {
        val original = listOf(
            RecentDocument(
                uriString = "content://docs/two_page",
                title = "SpreadBook.pdf",
                pageCount = 40,
                lastReadPage = 14,
                scrollOffset = 320,
                lastOpenedTimestamp = 1_700_000_000_000L,
                fileSizeBytes = 1024_000L,
                isAsset = false,
                assetPath = null,
                zoomLevel = 1.5f,
                colorMode = PdfColorMode.NIGHT_SLATE,
                layoutMode = PdfViewLayoutMode.TWO_PAGE
            )
        )

        val json = RecentDocumentRepository.serializeRecentDocuments(original)
        val parsed = RecentDocumentRepository.parseRecentDocuments(json)

        assertEquals(1, parsed.size)
        val doc = parsed.first()
        assertEquals("content://docs/two_page", doc.uriString)
        assertEquals(14, doc.lastReadPage)
        assertEquals(320, doc.scrollOffset)
        assertEquals(1.5f, doc.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.NIGHT_SLATE, doc.colorMode)
        assertEquals(PdfViewLayoutMode.TWO_PAGE, doc.layoutMode)
        assertNull(doc.assetPath)
    }
}
