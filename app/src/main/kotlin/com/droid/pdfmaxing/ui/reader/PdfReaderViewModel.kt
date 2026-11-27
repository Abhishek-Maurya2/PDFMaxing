package com.droid.pdfmaxing.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.droid.pdfmaxing.data.RecentDocumentRepository
import com.droid.pdfmaxing.data.model.RecentDocument
import com.droid.pdfmaxing.data.pdf.PdfDocumentManager
import com.droid.pdfmaxing.data.pdf.PdfPageCache
import com.droid.pdfmaxing.data.pdf.PdfSearchMatch
import com.droid.pdfmaxing.data.pdf.PdfTextSegment
import com.droid.pdfmaxing.data.pdf.PageSelectionResult
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
    val scrollOffset: Int = 0,
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

class PdfReaderViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val documentManagers = mutableMapOf<String, PdfDocumentManager>()
    private val pageCache = PdfPageCache()
    private val recentRepository = RecentDocumentRepository(application)
    private val zoomHelper = PdfZoomHelper()
    private var searchJob: Job? = null
    private var scrollPersistJob: Job? = null

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        cleanupOrphanedAssetFiles()
        restoreTabsFromSavedState()
    }

    private fun cleanupOrphanedAssetFiles() {
        runCatching {
            getApplication<Application>().cacheDir.listFiles { file ->
                file.name.startsWith("temp_asset_") && file.name.endsWith(".pdf")
            }?.forEach { it.delete() }
        }
    }

    private fun restoreTabsFromSavedState() {
        val savedUris = savedStateHandle.get<ArrayList<String>>(KEY_OPEN_TAB_URIS) ?: return
        if (savedUris.isEmpty()) return
        val savedActiveUri = savedStateHandle.get<String>(KEY_ACTIVE_TAB_URI)

        viewModelScope.launch {
            for (uriStr in savedUris) {
                if (uriStr == RecentDocumentRepository.SAMPLE_GUIDE_URI) {
                    loadSampleGuide(asNewTab = true)
                } else {
                    loadDocument(Uri.parse(uriStr), initialPage = 1, asNewTab = true)
                }
            }
            if (savedActiveUri != null) {
                val target = _uiState.value.tabs.find {
                    val tabUri = it.uri?.toString() ?: if (it.isAsset) RecentDocumentRepository.SAMPLE_GUIDE_URI else null
                    tabUri == savedActiveUri
                }
                if (target != null && target.id != _uiState.value.activeTabId) {
                    switchTab(target.id)
                }
            }
        }
    }

    private fun persistOpenTabsToSavedState(tabs: List<PdfTab> = _uiState.value.tabs, activeId: String? = _uiState.value.activeTabId) {
        val uris = ArrayList<String>(
            tabs.mapNotNull { tab ->
                tab.uri?.toString() ?: if (tab.isAsset) RecentDocumentRepository.SAMPLE_GUIDE_URI else null
            }
        )
        val activeTab = tabs.find { it.id == activeId }
        val activeUri = activeTab?.uri?.toString()
            ?: if (activeTab?.isAsset == true) RecentDocumentRepository.SAMPLE_GUIDE_URI else null

        savedStateHandle[KEY_OPEN_TAB_URIS] = uris
        savedStateHandle[KEY_ACTIVE_TAB_URI] = activeUri
    }

    fun loadDocument(
        uri: Uri,
        initialPage: Int = 1,
        asNewTab: Boolean = false,
        replaceTabId: String? = null
    ) {
        val uriString = uri.toString()

        // If a tab for this document is already open (or currently loading), switch to it
        val existingTab = _uiState.value.tabs.find { it.uri?.toString() == uriString }
        if (existingTab != null && existingTab.id != replaceTabId) {
            switchTab(existingTab.id)
            return
        }

        // Persist previous active tab state before switching away
        _uiState.value.activeTab?.let { persistTabState(it, touchTimestamp = false) }
        searchJob?.cancel()
        scrollPersistJob?.cancel()

        val titleCandidate = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Document.pdf"
        val currentState = _uiState.value
        val tabIdToReplace = when {
            replaceTabId != null && currentState.tabs.any { it.id == replaceTabId } -> replaceTabId
            !asNewTab && currentState.activeTabId != null -> currentState.activeTabId
            else -> null
        }

        val tabId = tabIdToReplace ?: UUID.randomUUID().toString()
        if (tabIdToReplace != null) {
            documentManagers.remove(tabIdToReplace)?.close()
            pageCache.evictDocument(tabIdToReplace)
        }

        val placeholderTab = PdfTab(
            id = tabId,
            uri = uri,
            isAsset = false,
            title = titleCandidate,
            pageCount = 0,
            isLoading = true
        )

        // Insert loading tab synchronously so rapid/duplicate callers deduplicate against it
        _uiState.update { current ->
            val updatedTabs = if (tabIdToReplace != null) {
                current.tabs.map { if (it.id == tabIdToReplace) placeholderTab else it }
            } else {
                current.tabs + placeholderTab
            }
            current.copy(
                tabs = updatedTabs,
                activeTabId = tabId,
                loadState = DocumentLoadState.Loading(titleCandidate),
                totalPages = 0,
                searchQuery = "",
                searchMatches = emptyList(),
                currentMatchIndex = 0,
                isSearching = false
            )
        }
        persistOpenTabsToSavedState()

        viewModelScope.launch {
            val savedDoc = recentRepository.getDocument(uriString)
            val initialTargetPage = if (initialPage != 1) initialPage else (savedDoc?.lastReadPage ?: 1)
            val initialScrollOffset = savedDoc?.scrollOffset ?: 0
            val initialZoom = savedDoc?.zoomLevel ?: 1.0f
            val initialColorMode = savedDoc?.colorMode ?: PdfColorMode.NORMAL
            val initialLayoutMode = savedDoc?.layoutMode ?: PdfViewLayoutMode.FIT_100

            val docManager = PdfDocumentManager(getApplication())
            val success = docManager.openUri(uri)

            // Guard against the user closing this tab while openUri was in flight
            if (_uiState.value.tabs.none { it.id == tabId }) {
                docManager.close()
                return@launch
            }

            if (success) {
                documentManagers[tabId] = docManager
                val totalPages = docManager.pageCount
                val title = docManager.documentTitle
                val size = docManager.fileSizeBytes
                val clampedPage = PdfPageHelper.clampPage(initialTargetPage, totalPages)
                val clampedZoom = zoomHelper.clampZoom(initialZoom)

                val loadedTab = PdfTab(
                    id = tabId,
                    uri = uri,
                    isAsset = false,
                    title = title,
                    pageCount = totalPages,
                    currentPage = clampedPage,
                    scrollOffset = initialScrollOffset,
                    zoomLevel = clampedZoom,
                    colorMode = initialColorMode,
                    layoutMode = initialLayoutMode,
                    fileSizeBytes = size,
                    isLoading = false
                )

                recentRepository.addOrUpdateDocument(
                    RecentDocument(
                        uriString = uriString,
                        title = title,
                        pageCount = totalPages,
                        lastReadPage = clampedPage,
                        scrollOffset = initialScrollOffset,
                        zoomLevel = clampedZoom,
                        colorMode = initialColorMode,
                        layoutMode = initialLayoutMode,
                        fileSizeBytes = size,
                        isAsset = false
                    )
                )

                _uiState.update { current ->
                    val updatedTabs = current.tabs.map { if (it.id == tabId) loadedTab else it }
                    if (current.activeTabId == tabId) {
                        current.copy(
                            tabs = updatedTabs,
                            loadState = loadedTab.toDocumentLoadState(),
                            currentPage = clampedPage,
                            scrollOffset = initialScrollOffset,
                            totalPages = totalPages,
                            zoomLevel = clampedZoom,
                            colorMode = initialColorMode,
                            layoutMode = initialLayoutMode
                        )
                    } else {
                        current.copy(tabs = updatedTabs)
                    }
                }
            } else {
                docManager.close()
                val errorMsg = "Unable to open PDF. The file may be corrupt, moved, or permission was revoked."
                _uiState.update { current ->
                    val updatedTabs = current.tabs.map { tab ->
                        if (tab.id == tabId) {
                            tab.copy(isLoading = false, errorMessage = errorMsg, canRelocate = true)
                        } else tab
                    }
                    if (current.activeTabId == tabId) {
                        current.copy(
                            tabs = updatedTabs,
                            loadState = DocumentLoadState.Error(message = errorMsg, canRelocate = true)
                        )
                    } else {
                        current.copy(tabs = updatedTabs)
                    }
                }
            }
        }
    }

    fun relocateActiveDocument(uri: Uri) {
        val activeId = _uiState.value.activeTabId
        loadDocument(uri, initialPage = 1, asNewTab = false, replaceTabId = activeId)
    }

    fun loadSampleGuide(asNewTab: Boolean = false) {
        val sampleUri = RecentDocumentRepository.SAMPLE_GUIDE_URI
        val existingTab = _uiState.value.tabs.find { it.isAsset && it.assetPath == "sample_guide.pdf" }
        if (existingTab != null) {
            switchTab(existingTab.id)
            return
        }

        _uiState.value.activeTab?.let { persistTabState(it, touchTimestamp = false) }
        searchJob?.cancel()
        scrollPersistJob?.cancel()

        val docTitle = "PDFMaxing Quick Guide.pdf"
        val currentState = _uiState.value
        val tabIdToReplace = if (!asNewTab && currentState.activeTabId != null) currentState.activeTabId else null
        val tabId = tabIdToReplace ?: UUID.randomUUID().toString()

        if (tabIdToReplace != null) {
            documentManagers.remove(tabIdToReplace)?.close()
            pageCache.evictDocument(tabIdToReplace)
        }

        val placeholderTab = PdfTab(
            id = tabId,
            uri = null,
            isAsset = true,
            assetPath = "sample_guide.pdf",
            title = docTitle,
            pageCount = 0,
            isLoading = true
        )

        _uiState.update { current ->
            val updatedTabs = if (tabIdToReplace != null) {
                current.tabs.map { if (it.id == tabIdToReplace) placeholderTab else it }
            } else {
                current.tabs + placeholderTab
            }
            current.copy(
                tabs = updatedTabs,
                activeTabId = tabId,
                loadState = DocumentLoadState.Loading(docTitle),
                totalPages = 0,
                searchQuery = "",
                searchMatches = emptyList(),
                currentMatchIndex = 0,
                isSearching = false
            )
        }
        persistOpenTabsToSavedState()

        viewModelScope.launch {
            val savedDoc = recentRepository.getDocument(sampleUri)
            val initialTargetPage = savedDoc?.lastReadPage ?: 1
            val initialScrollOffset = savedDoc?.scrollOffset ?: 0
            val initialZoom = savedDoc?.zoomLevel ?: 1.0f
            val initialColorMode = savedDoc?.colorMode ?: PdfColorMode.NORMAL
            val initialLayoutMode = savedDoc?.layoutMode ?: PdfViewLayoutMode.FIT_100

            val docManager = PdfDocumentManager(getApplication())
            val success = docManager.openAsset("sample_guide.pdf", docTitle)

            if (_uiState.value.tabs.none { it.id == tabId }) {
                docManager.close()
                return@launch
            }

            if (success) {
                documentManagers[tabId] = docManager
                val totalPages = docManager.pageCount
                val title = docManager.documentTitle
                val size = docManager.fileSizeBytes
                val clampedPage = PdfPageHelper.clampPage(initialTargetPage, totalPages)
                val clampedZoom = zoomHelper.clampZoom(initialZoom)

                val loadedTab = PdfTab(
                    id = tabId,
                    uri = null,
                    isAsset = true,
                    assetPath = "sample_guide.pdf",
                    title = title,
                    pageCount = totalPages,
                    currentPage = clampedPage,
                    scrollOffset = initialScrollOffset,
                    zoomLevel = clampedZoom,
                    colorMode = initialColorMode,
                    layoutMode = initialLayoutMode,
                    fileSizeBytes = size,
                    isLoading = false
                )

                recentRepository.addOrUpdateDocument(
                    RecentDocumentRepository.getSampleGuideDocument().copy(
                        pageCount = totalPages,
                        lastReadPage = clampedPage,
                        scrollOffset = initialScrollOffset,
                        zoomLevel = clampedZoom,
                        colorMode = initialColorMode,
                        layoutMode = initialLayoutMode,
                        fileSizeBytes = size
                    )
                )

                _uiState.update { current ->
                    val updatedTabs = current.tabs.map { if (it.id == tabId) loadedTab else it }
                    if (current.activeTabId == tabId) {
                        current.copy(
                            tabs = updatedTabs,
                            loadState = loadedTab.toDocumentLoadState(),
                            currentPage = clampedPage,
                            scrollOffset = initialScrollOffset,
                            totalPages = totalPages,
                            zoomLevel = clampedZoom,
                            colorMode = initialColorMode,
                            layoutMode = initialLayoutMode
                        )
                    } else {
                        current.copy(tabs = updatedTabs)
                    }
                }
            } else {
                docManager.close()
                val errorMsg = "Failed to load sample guide."
                _uiState.update { current ->
                    val updatedTabs = current.tabs.map { tab ->
                        if (tab.id == tabId) {
                            tab.copy(isLoading = false, errorMessage = errorMsg, canRelocate = false)
                        } else tab
                    }
                    if (current.activeTabId == tabId) {
                        current.copy(
                            tabs = updatedTabs,
                            loadState = DocumentLoadState.Error(errorMsg)
                        )
                    } else {
                        current.copy(tabs = updatedTabs)
                    }
                }
            }
        }
    }

    fun switchTab(tabId: String) {
        val currentState = _uiState.value
        val target = currentState.tabs.find { it.id == tabId } ?: return
        if (currentState.activeTabId == tabId && currentState.loadState !is DocumentLoadState.Idle) return

        currentState.activeTab?.let { previous ->
            if (previous.id != tabId) {
                persistTabState(previous, touchTimestamp = false)
            }
        }

        searchJob?.cancel()
        scrollPersistJob?.cancel()

        _uiState.update { current ->
            current.copy(
                activeTabId = tabId,
                currentPage = target.currentPage,
                scrollOffset = target.scrollOffset,
                totalPages = target.pageCount,
                zoomLevel = target.zoomLevel,
                colorMode = target.colorMode,
                layoutMode = target.layoutMode,
                loadState = target.toDocumentLoadState(),
                searchQuery = target.searchQuery,
                searchMatches = target.searchMatches,
                currentMatchIndex = target.currentMatchIndex,
                isSearching = false
            )
        }
        persistOpenTabsToSavedState()

        if (!target.isLoading && target.errorMessage == null) {
            persistTabState(target, touchTimestamp = true)
        }
    }

    /**
     * Closes the given tab. Returns false if no tabs remain open (should return to library).
     */
    fun closeTab(tabId: String): Boolean {
        val currentTabs = _uiState.value.tabs
        val currentActive = _uiState.value.activeTabId
        val tabToClose = currentTabs.find { it.id == tabId }

        if (tabId == currentActive) {
            searchJob?.cancel()
            scrollPersistJob?.cancel()
        }

        if (tabToClose != null) {
            persistTabState(tabToClose, touchTimestamp = false)
        }

        documentManagers.remove(tabId)?.close()
        pageCache.evictDocument(tabId)

        val nextActiveId = PdfTabHelper.calculateNextActiveTabId(currentTabs, currentActive, tabId)
        val remainingTabs = currentTabs.filterNot { it.id == tabId }

        if (remainingTabs.isEmpty() || nextActiveId == null) {
            _uiState.update {
                it.copy(
                    tabs = emptyList(),
                    activeTabId = null,
                    loadState = DocumentLoadState.Idle,
                    totalPages = 0,
                    currentPage = 1,
                    scrollOffset = 0,
                    isSearchOpen = false,
                    searchQuery = "",
                    searchMatches = emptyList(),
                    currentMatchIndex = 0,
                    isSearching = false
                )
            }
            persistOpenTabsToSavedState(emptyList(), null)
            return false
        }

        val nextTarget = remainingTabs.find { it.id == nextActiveId } ?: remainingTabs.first()
        _uiState.update { current ->
            if (currentActive == tabId) {
                current.copy(
                    tabs = remainingTabs,
                    activeTabId = nextTarget.id,
                    currentPage = nextTarget.currentPage,
                    scrollOffset = nextTarget.scrollOffset,
                    totalPages = nextTarget.pageCount,
                    zoomLevel = nextTarget.zoomLevel,
                    colorMode = nextTarget.colorMode,
                    layoutMode = nextTarget.layoutMode,
                    loadState = nextTarget.toDocumentLoadState(),
                    searchQuery = nextTarget.searchQuery,
                    searchMatches = nextTarget.searchMatches,
                    currentMatchIndex = nextTarget.currentMatchIndex,
                    isSearching = false
                )
            } else {
                current.copy(tabs = remainingTabs)
            }
        }
        persistOpenTabsToSavedState()
        return true
    }

    suspend fun getPageBitmap(documentId: String, pageIndex: Int, targetWidth: Int): Bitmap? {
        val targetDocId = documentId.ifEmpty { _uiState.value.activeTabId ?: return null }
        val docManager = documentManagers[targetDocId] ?: return null

        // Quantize target width into buckets of 60px to prevent micro-layout jitter
        // from re-rendering, while ensuring every zoom step gets its own crisp render
        val widthBucket = ((targetWidth + 30) / 60) * 60
        val cached = pageCache.getPageBitmap(targetDocId, pageIndex, widthBucket)
        if (cached != null) return cached

        val bitmap = docManager.renderPage(pageIndex, widthBucket)
        if (bitmap != null) {
            pageCache.putPageBitmap(targetDocId, pageIndex, widthBucket, bitmap)
        }
        return bitmap
    }

    suspend fun getThumbnailBitmap(documentId: String, pageIndex: Int): Bitmap? {
        val targetDocId = documentId.ifEmpty { _uiState.value.activeTabId ?: return null }
        val docManager = documentManagers[targetDocId] ?: return null

        val cached = pageCache.getThumbnail(targetDocId, pageIndex)
        if (cached != null) return cached

        val bitmap = docManager.renderThumbnail(pageIndex, thumbnailWidth = 240)
        if (bitmap != null) {
            pageCache.putThumbnail(targetDocId, pageIndex, bitmap)
        }
        return bitmap
    }

    suspend fun extractPageText(documentId: String, pageIndex: Int): String {
        val targetDocId = documentId.ifEmpty { _uiState.value.activeTabId ?: return "" }
        val docManager = documentManagers[targetDocId] ?: return ""
        return docManager.extractPageText(pageIndex)
    }

    suspend fun getPageTextSegments(documentId: String, pageIndex: Int): List<PdfTextSegment> {
        val targetDocId = documentId.ifEmpty { _uiState.value.activeTabId ?: return emptyList() }
        val docManager = documentManagers[targetDocId] ?: return emptyList()
        return docManager.extractPageTextSegments(pageIndex)
    }

    suspend fun selectContent(
        documentId: String,
        pageIndex: Int,
        startX: Float,
        startY: Float,
        stopX: Float,
        stopY: Float
    ): PageSelectionResult? {
        val targetDocId = documentId.ifEmpty { _uiState.value.activeTabId ?: return null }
        val docManager = documentManagers[targetDocId] ?: return null
        return docManager.selectContent(pageIndex, startX, startY, stopX, stopY)
    }

    fun updateScrollPosition(page: Int, scrollOffset: Int) {
        val total = _uiState.value.totalPages
        if (total <= 0) return
        val clamped = PdfPageHelper.clampPage(page, total)

        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) {
                    tab.copy(currentPage = clamped, scrollOffset = scrollOffset)
                } else tab
            }
            current.copy(currentPage = clamped, scrollOffset = scrollOffset, tabs = updatedTabs)
        }

        // Debounce DataStore disk writes while scrolling so every scroll frame doesn't serialize JSON
        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            scrollPersistJob?.cancel()
            scrollPersistJob = viewModelScope.launch {
                delay(350)
                recentRepository.updateLastReadPage(state.uriString, clamped, scrollOffset)
            }
        }
    }

    fun setPage(page: Int) {
        val total = _uiState.value.totalPages
        if (total <= 0) return
        val clamped = PdfPageHelper.clampPage(page, total)

        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(currentPage = clamped, scrollOffset = 0) else tab
            }
            current.copy(currentPage = clamped, scrollOffset = 0, tabs = updatedTabs)
        }

        scrollPersistJob?.cancel()
        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            viewModelScope.launch {
                recentRepository.updateLastReadPage(state.uriString, clamped, 0)
            }
        }
    }

    fun nextPage() {
        val step = if (_uiState.value.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
        setPage(_uiState.value.currentPage + step)
    }

    fun previousPage() {
        val step = if (_uiState.value.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
        setPage(_uiState.value.currentPage - step)
    }

    fun setZoom(zoom: Float) {
        val clamped = zoomHelper.clampZoom(zoom)
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(zoomLevel = clamped) else tab
            }
            current.copy(zoomLevel = clamped, tabs = updatedTabs)
        }

        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            viewModelScope.launch {
                recentRepository.updateZoomLevel(state.uriString, clamped)
            }
        }
    }

    fun zoomIn() {
        setZoom(zoomHelper.calculateZoomIn(_uiState.value.zoomLevel))
    }

    fun zoomOut() {
        setZoom(zoomHelper.calculateZoomOut(_uiState.value.zoomLevel))
    }

    fun zoomBy(factor: Float) {
        val currentZoom = _uiState.value.zoomLevel
        val nextZoom = zoomHelper.clampZoom(currentZoom * factor)
        if (nextZoom != currentZoom) {
            setZoom(nextZoom)
        }
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
        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            viewModelScope.launch {
                recentRepository.updateLayoutMode(state.uriString, mode, targetZoom)
            }
        }
    }

    fun handleDoubleTapZoom() {
        val next = zoomHelper.nextDoubleTapZoom(_uiState.value.zoomLevel)
        setZoom(next)
    }

    fun cycleColorMode() {
        val next = _uiState.value.colorMode.next()
        setColorMode(next)
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
        val state = _uiState.value.loadState
        if (state is DocumentLoadState.Success) {
            viewModelScope.launch {
                recentRepository.updateColorMode(state.uriString, mode)
            }
        }
    }

    fun saveCurrentState() {
        scrollPersistJob?.cancel()
        val activeTab = _uiState.value.activeTab ?: return
        persistTabState(activeTab, touchTimestamp = true)
    }

    private fun persistTabState(tab: PdfTab, touchTimestamp: Boolean) {
        if (tab.isLoading || tab.errorMessage != null || tab.pageCount <= 0) return
        val uriString = tab.uri?.toString()
            ?: if (tab.isAsset) RecentDocumentRepository.SAMPLE_GUIDE_URI else return

        viewModelScope.launch {
            recentRepository.updateDocumentViewState(
                uriString = uriString,
                page = tab.currentPage,
                zoomLevel = tab.zoomLevel,
                colorMode = tab.colorMode,
                scrollOffset = tab.scrollOffset,
                layoutMode = tab.layoutMode,
                touchTimestamp = touchTimestamp
            )
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
            val updatedTabs = if (!open) {
                current.tabs.map { tab ->
                    if (tab.id == current.activeTabId) {
                        tab.copy(searchQuery = "", searchMatches = emptyList(), currentMatchIndex = 0, isSearching = false)
                    } else tab
                }
            } else current.tabs

            current.copy(
                tabs = updatedTabs,
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
        val targetTabId = _uiState.value.activeTabId
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == targetTabId) tab.copy(searchQuery = query) else tab
            }
            current.copy(tabs = updatedTabs, searchQuery = query)
        }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { current ->
                val updatedTabs = current.tabs.map { tab ->
                    if (tab.id == targetTabId) {
                        tab.copy(searchMatches = emptyList(), currentMatchIndex = 0, isSearching = false)
                    } else tab
                }
                current.copy(tabs = updatedTabs, searchMatches = emptyList(), currentMatchIndex = 0, isSearching = false)
            }
            return
        }

        if (targetTabId == null) return

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(150) // Debounce typing

            val docManager = documentManagers[targetTabId]
            val matches = docManager?.searchDocument(query) ?: emptyList()

            var shouldJumpToFirst = false
            _uiState.update { current ->
                val updatedTabs = current.tabs.map { tab ->
                    if (tab.id == targetTabId) {
                        tab.copy(searchMatches = matches, currentMatchIndex = 0, isSearching = false)
                    } else tab
                }
                if (current.activeTabId == targetTabId) {
                    shouldJumpToFirst = matches.isNotEmpty()
                    current.copy(
                        tabs = updatedTabs,
                        searchMatches = matches,
                        currentMatchIndex = 0,
                        isSearching = false
                    )
                } else {
                    current.copy(tabs = updatedTabs)
                }
            }

            if (shouldJumpToFirst && matches.isNotEmpty()) {
                setPage(matches[0].pageIndex + 1)
            }
        }
    }

    fun nextSearchMatch(): PdfSearchMatch? {
        val matches = _uiState.value.searchMatches
        if (matches.isEmpty()) return null

        val nextIndex = (_uiState.value.currentMatchIndex + 1) % matches.size
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(currentMatchIndex = nextIndex) else tab
            }
            current.copy(tabs = updatedTabs, currentMatchIndex = nextIndex)
        }
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
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) tab.copy(currentMatchIndex = prevIndex) else tab
            }
            current.copy(tabs = updatedTabs, currentMatchIndex = prevIndex)
        }
        val match = matches[prevIndex]
        setPage(match.pageIndex + 1)
        return match
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { current ->
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == current.activeTabId) {
                    tab.copy(searchQuery = "", searchMatches = emptyList(), currentMatchIndex = 0, isSearching = false)
                } else tab
            }
            current.copy(
                tabs = updatedTabs,
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
        scrollPersistJob?.cancel()
        documentManagers.values.forEach { it.close() }
        documentManagers.clear()
        pageCache.clear()
    }

    companion object {
        private const val KEY_OPEN_TAB_URIS = "open_tab_uris"
        private const val KEY_ACTIVE_TAB_URI = "active_tab_uri"
    }
}
