package com.droid.pdfmaxing.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droid.pdfmaxing.data.RecentDocumentRepository
import com.droid.pdfmaxing.data.model.RecentDocument
import com.droid.pdfmaxing.data.pdf.PdfDocumentManager
import com.droid.pdfmaxing.data.pdf.PdfPageCache
import com.droid.pdfmaxing.data.pdf.PdfSearchMatch
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface DocumentLoadState {
    data object Idle : DocumentLoadState
    data class Loading(val title: String) : DocumentLoadState
    data class Success(
        val title: String,
        val pageCount: Int,
        val fileSizeBytes: Long,
        val uriString: String,
        val isAsset: Boolean
    ) : DocumentLoadState
    data class Error(val message: String, val canRelocate: Boolean = false) : DocumentLoadState
}

enum class SidebarTab(val title: String) {
    THUMBNAILS("Thumbnails"),
    OUTLINE("Outline"),
    INFO("Info")
}

data class PdfReaderUiState(
    val tabs: List<PdfTab> = emptyList(),
    val activeTabId: String? = null,
    val loadState: DocumentLoadState = DocumentLoadState.Idle,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val zoomLevel: Float = 1.0f,
    val colorMode: PdfColorMode = PdfColorMode.NORMAL,
    val layoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100,
    val isImmersive: Boolean = false,
    val isSidebarOpen: Boolean = false,
    val activeSidebarTab: SidebarTab = SidebarTab.THUMBNAILS,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchMatches: List<PdfSearchMatch> = emptyList(),
    val currentMatchIndex: Int = 0,
    val isSearching: Boolean = false
) {
    val activeTab: PdfTab?
        get() = tabs.find { it.id == activeTabId }
}

class PdfReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val documentManagers = mutableMapOf<String, PdfDocumentManager>()
    private val pageCache = PdfPageCache()
    private val recentRepository = RecentDocumentRepository(application)
    private val zoomHelper = PdfZoomHelper()
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    fun loadDocument(uri: Uri, initialPage: Int = 1, asNewTab: Boolean = false) {
        viewModelScope.launch {
            val titleCandidate = uri.lastPathSegment ?: "Document.pdf"
            val tabId = UUID.randomUUID().toString()

            _uiState.update { current ->
                current.copy(
                    loadState = DocumentLoadState.Loading(titleCandidate),
                    currentPage = initialPage
                )
            }

            val docManager = PdfDocumentManager(getApplication())
            val success = docManager.openUri(uri)
            if (success) {
                documentManagers[tabId] = docManager
                val totalPages = docManager.pageCount
                val title = docManager.documentTitle
                val size = docManager.fileSizeBytes
                val clampedPage = PdfPageHelper.clampPage(initialPage, totalPages)

                val newTab = PdfTab(
                    id = tabId,
                    uri = uri,
                    isAsset = false,
                    title = title,
                    pageCount = totalPages,
                    currentPage = clampedPage,
                    zoomLevel = 1.0f,
                    colorMode = _uiState.value.colorMode,
                    fileSizeBytes = size
                )

                recentRepository.addOrUpdateDocument(
                    RecentDocument(
                        uriString = uri.toString(),
                        title = title,
                        pageCount = totalPages,
                        lastReadPage = clampedPage,
                        fileSizeBytes = size,
                        isAsset = false
                    )
                )

                _uiState.update { current ->
                    val updatedTabs = if (asNewTab || current.tabs.isEmpty()) {
                        current.tabs + newTab
                    } else {
                        // Replace current active tab or add
                        current.tabs + newTab
                    }
                    current.copy(
                        tabs = updatedTabs,
                        activeTabId = tabId,
                        loadState = DocumentLoadState.Success(
                            title = title,
                            pageCount = totalPages,
                            fileSizeBytes = size,
                            uriString = uri.toString(),
                            isAsset = false
                        ),
                        currentPage = clampedPage,
                        totalPages = totalPages,
                        zoomLevel = 1.0f
                    )
                }
            } else {
                docManager.close()
                _uiState.update {
                    it.copy(
                        loadState = DocumentLoadState.Error(
                            message = "Unable to open PDF. The file may be corrupt, moved, or permission was revoked.",
                            canRelocate = true
                        )
                    )
                }
            }
        }
    }

    fun loadSampleGuide(asNewTab: Boolean = false) {
        viewModelScope.launch {
            val tabId = UUID.randomUUID().toString()
            val docTitle = "PDFMaxing Quick Guide.pdf"

            _uiState.update {
                it.copy(
                    loadState = DocumentLoadState.Loading(docTitle),
                    currentPage = 1
                )
            }

            val docManager = PdfDocumentManager(getApplication())
            val success = docManager.openAsset("sample_guide.pdf", docTitle)
            if (success) {
                documentManagers[tabId] = docManager
                val totalPages = docManager.pageCount
                val title = docManager.documentTitle
                val size = docManager.fileSizeBytes

                val newTab = PdfTab(
                    id = tabId,
                    uri = null,
                    isAsset = true,
                    assetPath = "sample_guide.pdf",
                    title = title,
                    pageCount = totalPages,
                    currentPage = 1,
                    zoomLevel = 1.0f,
                    colorMode = _uiState.value.colorMode,
                    fileSizeBytes = size
                )

                recentRepository.addOrUpdateDocument(
                    RecentDocumentRepository.getSampleGuideDocument().copy(
                        pageCount = totalPages,
                        fileSizeBytes = size
                    )
                )

                _uiState.update { current ->
                    val updatedTabs = if (asNewTab || current.tabs.isEmpty()) {
                        current.tabs + newTab
                    } else {
                        current.tabs + newTab
                    }
                    current.copy(
                        tabs = updatedTabs,
                        activeTabId = tabId,
                        loadState = DocumentLoadState.Success(
                            title = title,
                            pageCount = totalPages,
                            fileSizeBytes = size,
                            uriString = RecentDocumentRepository.SAMPLE_GUIDE_URI,
                            isAsset = true
                        ),
                        currentPage = 1,
                        totalPages = totalPages,
                        zoomLevel = 1.0f
                    )
                }
            } else {
                docManager.close()
                _uiState.update {
                    it.copy(
                        loadState = DocumentLoadState.Error("Failed to load sample guide.")
                    )
                }
            }
        }
    }

    fun switchTab(tabId: String) {
        val target = _uiState.value.tabs.find { it.id == tabId } ?: return
        val docManager = documentManagers[tabId] ?: return

        _uiState.update { current ->
            current.copy(
                activeTabId = tabId,
                currentPage = target.currentPage,
                totalPages = target.pageCount,
                zoomLevel = target.zoomLevel,
                colorMode = target.colorMode,
                layoutMode = target.layoutMode,
                loadState = DocumentLoadState.Success(
                    title = target.title,
                    pageCount = target.pageCount,
                    fileSizeBytes = target.fileSizeBytes,
                    uriString = target.uri?.toString() ?: RecentDocumentRepository.SAMPLE_GUIDE_URI,
                    isAsset = target.isAsset
                )
            )
        }
    }

    /**
     * Closes the given tab. Returns false if no tabs remain open (should return to library).
     */
    fun closeTab(tabId: String): Boolean {
        documentManagers.remove(tabId)?.close()

        val currentTabs = _uiState.value.tabs
        val currentActive = _uiState.value.activeTabId
        val nextActiveId = PdfTabHelper.calculateNextActiveTabId(currentTabs, currentActive, tabId)
        val remainingTabs = currentTabs.filterNot { it.id == tabId }

        if (remainingTabs.isEmpty() || nextActiveId == null) {
            _uiState.update {
                it.copy(
                    tabs = emptyList(),
                    activeTabId = null,
                    loadState = DocumentLoadState.Idle
                )
            }
            return false
        }

        _uiState.update { it.copy(tabs = remainingTabs) }
        switchTab(nextActiveId)
        return true
    }

    suspend fun getPageBitmap(pageIndex: Int, targetWidth: Int, scaleFactor: Float): Bitmap? {
        val activeId = _uiState.value.activeTabId ?: return null
        val docManager = documentManagers[activeId] ?: return null

        // Quantize target width into buckets of 60px to prevent micro-layout jitter
        // from re-rendering, while ensuring every zoom step gets its own crisp render
        val widthBucket = ((targetWidth + 30) / 60) * 60
        val cached = pageCache.getPageBitmap(activeId, pageIndex, widthBucket)
        if (cached != null) return cached

        val bitmap = docManager.renderPage(pageIndex, widthBucket, scaleFactor)
        if (bitmap != null) {
            pageCache.putPageBitmap(activeId, pageIndex, widthBucket, bitmap)
        }
        return bitmap
    }

    suspend fun getThumbnailBitmap(pageIndex: Int): Bitmap? {
        val activeId = _uiState.value.activeTabId ?: return null
        val docManager = documentManagers[activeId] ?: return null

        val cached = pageCache.getThumbnail(activeId, pageIndex)
        if (cached != null) return cached

        val bitmap = docManager.renderThumbnail(pageIndex, thumbnailWidth = 240)
        if (bitmap != null) {
            pageCache.putThumbnail(activeId, pageIndex, bitmap)
        }
        return bitmap
    }

    fun setPage(page: Int) {
        val total = _uiState.value.totalPages
        if (total <= 0) return
        val clamped = PdfPageHelper.clampPage(page, total)

        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(currentPage = clamped) else tab
            }
            current.copy(currentPage = clamped, tabs = updatedTabs)
        }

        // Persist reading progress
        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            viewModelScope.launch {
                recentRepository.updateLastReadPage(state.uriString, clamped)
            }
        }
    }

    fun nextPage() {
        setPage(_uiState.value.currentPage + 1)
    }

    fun previousPage() {
        setPage(_uiState.value.currentPage - 1)
    }

    fun setZoom(zoom: Float) {
        val clamped = zoomHelper.clampZoom(zoom)
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(zoomLevel = clamped) else tab
            }
            current.copy(zoomLevel = clamped, tabs = updatedTabs)
        }
    }

    fun zoomIn() {
        setZoom(zoomHelper.calculateZoomIn(_uiState.value.zoomLevel))
    }

    fun zoomOut() {
        setZoom(zoomHelper.calculateZoomOut(_uiState.value.zoomLevel))
    }

    fun resetZoom() {
        setZoom(1.0f)
    }

    fun cycleLayoutMode() {
        val next = _uiState.value.layoutMode.next()
        setLayoutMode(next)
    }

    fun setLayoutMode(mode: PdfViewLayoutMode) {
        val targetZoom = when (mode) {
            PdfViewLayoutMode.FIT_100 -> 1.0f
            PdfViewLayoutMode.FIT_50 -> 0.5f
            PdfViewLayoutMode.TWO_PAGE -> 1.0f
        }
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(layoutMode = mode, zoomLevel = targetZoom) else tab
            }
            current.copy(layoutMode = mode, zoomLevel = targetZoom, tabs = updatedTabs)
        }
        viewModelScope.launch {
            _snackbarEvents.emit("View Layout: ${mode.displayName}")
        }
    }

    fun handleDoubleTapZoom() {
        val next = zoomHelper.nextDoubleTapZoom(_uiState.value.zoomLevel)
        setZoom(next)
    }

    fun cycleColorMode() {
        val next = _uiState.value.colorMode.next()
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(colorMode = next) else tab
            }
            current.copy(colorMode = next, tabs = updatedTabs)
        }
        viewModelScope.launch {
            _snackbarEvents.emit("PDF Page Mode: ${next.displayName}")
        }
    }

    fun setColorMode(mode: PdfColorMode) {
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(colorMode = mode) else tab
            }
            current.copy(colorMode = mode, tabs = updatedTabs)
        }
        viewModelScope.launch {
            _snackbarEvents.emit("PDF Page Mode: ${mode.displayName}")
        }
    }

    fun toggleImmersive() {
        _uiState.update { it.copy(isImmersive = !it.isImmersive) }
    }

    fun setSidebarOpen(open: Boolean) {
        _uiState.update { it.copy(isSidebarOpen = open) }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(isSidebarOpen = !it.isSidebarOpen) }
    }

    fun setSidebarTab(tab: SidebarTab) {
        _uiState.update { it.copy(activeSidebarTab = tab) }
    }

    fun toggleSearch() {
        val next = !_uiState.value.isSearchOpen
        setSearchOpen(next)
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { current ->
            current.copy(
                isSearchOpen = open,
                searchQuery = if (!open) "" else current.searchQuery,
                searchMatches = if (!open) emptyList() else current.searchMatches,
                currentMatchIndex = 0,
                isSearching = false
            )
        }
        if (!open) {
            searchJob?.cancel()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchMatches = emptyList(), currentMatchIndex = 0, isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(150) // Debounce typing

            val activeId = _uiState.value.activeTabId
            val docManager = activeId?.let { documentManagers[it] }
            val matches = docManager?.searchDocument(query) ?: emptyList()

            _uiState.update { current ->
                current.copy(
                    searchMatches = matches,
                    currentMatchIndex = 0,
                    isSearching = false
                )
            }

            if (matches.isNotEmpty()) {
                val firstMatch = matches[0]
                setPage(firstMatch.pageIndex + 1)
            }
        }
    }

    fun nextSearchMatch(): PdfSearchMatch? {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return null

        val nextIndex = (_uiState.value.currentMatchIndex + 1) % matches.size
        _uiState.update { it.copy(currentMatchIndex = nextIndex) }
        val match = matches[nextIndex]
        setPage(match.pageIndex + 1)
        return match
    }

    fun previousSearchMatch(): PdfSearchMatch? {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return null

        val prevIndex = if (_uiState.value.currentMatchIndex - 1 < 0) {
            matches.size - 1
        } else {
            _uiState.value.currentMatchIndex - 1
        }
        _uiState.update { it.copy(currentMatchIndex = prevIndex) }
        val match = matches[prevIndex]
        setPage(match.pageIndex + 1)
        return match
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchMatches = emptyList(),
                currentMatchIndex = 0,
                isSearching = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        documentManagers.values.forEach { it.close() }
        documentManagers.clear()
        pageCache.clear()
    }
}
