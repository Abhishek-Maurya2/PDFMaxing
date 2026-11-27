package com.droid.pdfmaxing.ui.reader

import android.net.Uri
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PdfTabManagerTest {

    @Test
    fun testAddAndSwitchTab() {
        var tabs = listOf<PdfTab>()
        val tab1 = PdfTab(id = "1", uri = null, isAsset = true, title = "Guide.pdf", pageCount = 3)
        val tab2 = PdfTab(id = "2", uri = null, isAsset = false, title = "Invoice.pdf", pageCount = 5)

        tabs = tabs + tab1
        assertEquals(1, tabs.size)
        assertEquals("Guide.pdf", tabs.first().title)

        tabs = tabs + tab2
        assertEquals(2, tabs.size)
        assertEquals("Invoice.pdf", tabs.last().title)
    }

    @Test
    fun testCloseActiveTabSelectsAdjacent() {
        val tab1 = PdfTab(id = "1", uri = null, title = "Doc1.pdf", pageCount = 2)
        val tab2 = PdfTab(id = "2", uri = null, title = "Doc2.pdf", pageCount = 4)
        val tab3 = PdfTab(id = "3", uri = null, title = "Doc3.pdf", pageCount = 6)

        var tabs = listOf(tab1, tab2, tab3)
        var activeTabId = "2"

        // Close tab2 (middle tab)
        val nextActiveId = PdfTabHelper.calculateNextActiveTabId(tabs, activeTabId, closingId = "2")
        tabs = tabs.filterNot { it.id == "2" }

        assertEquals(2, tabs.size)
        // Should select tab3 (or tab1)
        assertEquals("3", nextActiveId)
    }

    @Test
    fun testCloseLastTabReturnsNull() {
        val tab1 = PdfTab(id = "1", uri = null, title = "Doc1.pdf", pageCount = 2)
        val tabs = listOf(tab1)
        val nextActiveId = PdfTabHelper.calculateNextActiveTabId(tabs, "1", closingId = "1")
        assertNull(nextActiveId)
    }

    @Test
    fun testUpdateActiveTabState() {
        val tab1 = PdfTab(id = "1", uri = null, title = "Doc1.pdf", pageCount = 10, currentPage = 1, zoomLevel = 1.0f)
        val updated = tab1.copy(currentPage = 5, zoomLevel = 2.0f, colorMode = PdfColorMode.INVERTED)

        assertEquals(5, updated.currentPage)
        assertEquals(2.0f, updated.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.INVERTED, updated.colorMode)
    }

    @Test
    fun testTabCreationWithRememberedState() {
        val rememberedPage = 7
        val rememberedZoom = 1.75f
        val rememberedTheme = PdfColorMode.NIGHT_SLATE

        val tab = PdfTab(
            id = "tab-saved",
            uri = null,
            title = "SavedBook.pdf",
            pageCount = 20,
            currentPage = rememberedPage,
            zoomLevel = rememberedZoom,
            colorMode = rememberedTheme
        )

        assertEquals(7, tab.currentPage)
        assertEquals(1.75f, tab.zoomLevel, 0.001f)
        assertEquals(PdfColorMode.NIGHT_SLATE, tab.colorMode)
    }

    @Test
    fun testTabToDocumentLoadStateTransitions() {
        val loadingTab = PdfTab(
            id = "tab-loading",
            uri = null,
            title = "Loading.pdf",
            pageCount = 0,
            isLoading = true
        )
        assertEquals(DocumentLoadState.Loading("Loading.pdf"), loadingTab.toDocumentLoadState())

        val errorTab = loadingTab.copy(
            isLoading = false,
            errorMessage = "Corrupt PDF",
            canRelocate = true
        )
        assertEquals(
            DocumentLoadState.Error("Corrupt PDF", canRelocate = true),
            errorTab.toDocumentLoadState()
        )

        val successTab = PdfTab(
            id = "tab-ok",
            uri = null,
            isAsset = true,
            title = "Ready.pdf",
            pageCount = 12,
            fileSizeBytes = 2048L
        )
        val state = successTab.toDocumentLoadState() as DocumentLoadState.Success
        assertEquals("Ready.pdf", state.title)
        assertEquals(12, state.pageCount)
        assertEquals(2048L, state.fileSizeBytes)
    }
}
