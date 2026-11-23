package com.droid.pdfmaxing.data.model

import org.junit.Assert.assertEquals
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
}
