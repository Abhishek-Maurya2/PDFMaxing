package com.droid.pdfmaxing.data.pdf

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfSearchEngineTest {

    private fun getSampleGuideBytes(): ByteArray {
        val file = File("src/main/assets/sample_guide.pdf")
        if (file.exists()) return file.readBytes()
        val altFile = File("app/src/main/assets/sample_guide.pdf")
        if (altFile.exists()) return altFile.readBytes()
        throw IllegalStateException("sample_guide.pdf not found")
    }

    @Test
    fun testSearchFindsMatchesOnPageOne() = runBlocking {
        val bytes = getSampleGuideBytes()
        val matches = PdfSearchEngine.searchInPdfBytes(bytes, "Welcome")
        assertFalse(matches.isEmpty())
        assertEquals(0, matches[0].pageIndex)
        assertTrue(matches[0].snippet.contains("Welcome", ignoreCase = true))
    }

    @Test
    fun testSearchFindsMatchesOnPageTwo() = runBlocking {
        val bytes = getSampleGuideBytes()
        val matches = PdfSearchEngine.searchInPdfBytes(bytes, "ColorMatrix")
        assertFalse(matches.isEmpty())
        assertEquals(1, matches[0].pageIndex)
        assertTrue(matches[0].snippet.contains("ColorMatrix", ignoreCase = true))
    }

    @Test
    fun testSearchFindsMatchesOnPageThree() = runBlocking {
        val bytes = getSampleGuideBytes()
        val matches = PdfSearchEngine.searchInPdfBytes(bytes, "Document search")
        assertFalse(matches.isEmpty())
        assertEquals(2, matches[0].pageIndex)
        assertTrue(matches[0].snippet.contains("Document search", ignoreCase = true))
    }

    @Test
    fun testSearchFindsMultiPageMatches() = runBlocking {
        val bytes = getSampleGuideBytes()
        val matches = PdfSearchEngine.searchInPdfBytes(bytes, "Dark Mode")
        // "Dark Mode" appears on Page 1 (index 0) and Page 2 (index 1)
        assertTrue(matches.size >= 2)
        val pagesWithMatch = matches.map { it.pageIndex }.distinct()
        assertTrue(pagesWithMatch.contains(0))
        assertTrue(pagesWithMatch.contains(1))
    }

    @Test
    fun testSearchCaseInsensitivity() = runBlocking {
        val bytes = getSampleGuideBytes()
        val lowerMatches = PdfSearchEngine.searchInPdfBytes(bytes, "pdfmaxing")
        val upperMatches = PdfSearchEngine.searchInPdfBytes(bytes, "PDFMAXING")
        assertEquals(lowerMatches.size, upperMatches.size)
        assertTrue(lowerMatches.isNotEmpty())
    }

    @Test
    fun testSearchNonExistentTermReturnsEmpty() = runBlocking {
        val bytes = getSampleGuideBytes()
        val matches = PdfSearchEngine.searchInPdfBytes(bytes, "nonexistentphrasexyz123")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchEmptyOrBlankQueryReturnsEmpty() = runBlocking {
        val bytes = getSampleGuideBytes()
        val emptyMatches = PdfSearchEngine.searchInPdfBytes(bytes, "")
        val blankMatches = PdfSearchEngine.searchInPdfBytes(bytes, "   ")
        assertTrue(emptyMatches.isEmpty())
        assertTrue(blankMatches.isEmpty())
    }

    @Test
    fun testMatchCyclingLogic() {
        val matches = listOf(
            PdfSearchMatch(pageIndex = 0, matchIndexInPage = 0),
            PdfSearchMatch(pageIndex = 1, matchIndexInPage = 0),
            PdfSearchMatch(pageIndex = 2, matchIndexInPage = 0)
        )

        var currentIndex = 0
        // Next match: 0 -> 1 -> 2 -> 0
        currentIndex = (currentIndex + 1) % matches.size
        assertEquals(1, currentIndex)
        currentIndex = (currentIndex + 1) % matches.size
        assertEquals(2, currentIndex)
        currentIndex = (currentIndex + 1) % matches.size
        assertEquals(0, currentIndex)

        // Prev match: 0 -> 2 -> 1 -> 0
        currentIndex = if (currentIndex - 1 < 0) matches.size - 1 else currentIndex - 1
        assertEquals(2, currentIndex)
        currentIndex = if (currentIndex - 1 < 0) matches.size - 1 else currentIndex - 1
        assertEquals(1, currentIndex)
        currentIndex = if (currentIndex - 1 < 0) matches.size - 1 else currentIndex - 1
        assertEquals(0, currentIndex)
    }
}
