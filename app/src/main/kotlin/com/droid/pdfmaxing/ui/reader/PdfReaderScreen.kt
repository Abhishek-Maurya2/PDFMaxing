package com.droid.pdfmaxing.ui.reader

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed as isPointerCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droid.pdfmaxing.ui.components.DesktopPdfToolbar
import com.droid.pdfmaxing.ui.components.ExpressiveVerticalFloatingToolbar
import com.droid.pdfmaxing.ui.components.MobileFloatingToolbar
import com.droid.pdfmaxing.ui.components.PdfPageItem
import com.droid.pdfmaxing.ui.components.PdfTabBar
import com.droid.pdfmaxing.ui.components.ThemeVibrantMenu
import com.droid.pdfmaxing.ui.components.ThumbnailSidebar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    documentUri: Uri?,
    isSampleGuide: Boolean = false,
    onBackToLibrary: () -> Unit,
    viewModel: PdfReaderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var showPageSliderPill by remember { mutableStateOf(false) }

    // SAF File Picker for opening a new document / tab
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadDocument(uri, initialPage = 1, asNewTab = true)
        }
    }

    // Load initial document if tabs are currently empty
    LaunchedEffect(documentUri, isSampleGuide) {
        if (uiState.tabs.isEmpty()) {
            if (isSampleGuide) {
                viewModel.loadSampleGuide(asNewTab = false)
            } else if (documentUri != null) {
                viewModel.loadDocument(documentUri, initialPage = 1, asNewTab = false)
            }
        }
    }

    // Collect snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Request keyboard focus for laptop / desktop shortcuts
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Sync lazy list visible item to current page indicator
    LaunchedEffect(listState, uiState.layoutMode) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                if (uiState.totalPages > 0) {
                    val computedPage = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                        firstIndex * 2 + 1
                    } else {
                        firstIndex + 1
                    }
                    viewModel.setPage(computedPage.coerceIn(1, uiState.totalPages))
                }
            }
    }

    // Intercept hardware Back button
    BackHandler {
        if (uiState.isSearchOpen) {
            viewModel.setSearchOpen(false)
        } else if (showPageSliderPill) {
            showPageSliderPill = false
        } else {
            onBackToLibrary()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                // Desktop Keyboard Shortcuts
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when {
                        // Ctrl + F: Toggle Search
                        keyEvent.isCtrlPressed && keyEvent.key == Key.F -> {
                            showPageSliderPill = false
                            viewModel.toggleSearch()
                            true
                        }
                        // Ctrl + O: Open file in new tab
                        keyEvent.isCtrlPressed && keyEvent.key == Key.O -> {
                            openFileLauncher.launch(arrayOf("application/pdf"))
                            true
                        }
                        // Ctrl + W: Close current tab
                        keyEvent.isCtrlPressed && keyEvent.key == Key.W -> {
                            val activeId = uiState.activeTabId
                            if (activeId != null) {
                                val hasRemaining = viewModel.closeTab(activeId)
                                if (!hasRemaining) onBackToLibrary()
                            }
                            true
                        }
                        // Ctrl + D: Cycle Dark Mode Inversion
                        keyEvent.isCtrlPressed && keyEvent.key == Key.D -> {
                            viewModel.cycleColorMode()
                            true
                        }
                        // Ctrl + Plus or Ctrl + Equals: Zoom in
                        keyEvent.isCtrlPressed && (keyEvent.key == Key.Plus || keyEvent.key == Key.Equals) -> {
                            viewModel.zoomIn()
                            true
                        }
                        // Ctrl + Minus: Zoom out
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Minus -> {
                            viewModel.zoomOut()
                            true
                        }
                        // Ctrl + Zero: Reset zoom to 100%
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Zero -> {
                            viewModel.resetZoom()
                            true
                        }
                        // Page Down, Space, Down arrow: Next page
                        keyEvent.key == Key.PageDown || (keyEvent.key == Key.Spacebar && !keyEvent.isShiftPressed) || keyEvent.key == Key.DirectionRight -> {
                            viewModel.nextPage()
                            scope.launch {
                                val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                    (uiState.currentPage - 1) / 2
                                } else {
                                    uiState.currentPage - 1
                                }
                                listState.animateScrollToItem(targetItem.coerceAtLeast(0))
                            }
                            true
                        }
                        // Page Up, Shift+Space, Up arrow: Previous page
                        keyEvent.key == Key.PageUp || (keyEvent.key == Key.Spacebar && keyEvent.isShiftPressed) || keyEvent.key == Key.DirectionLeft -> {
                            viewModel.previousPage()
                            scope.launch {
                                val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                    (uiState.currentPage - 2).coerceAtLeast(0) / 2
                                } else {
                                    (uiState.currentPage - 2).coerceAtLeast(0)
                                }
                                listState.animateScrollToItem(targetItem.coerceAtLeast(0))
                            }
                            true
                        }
                        // Escape: Dismiss search, or dismiss slider, or return to library
                        keyEvent.key == Key.Escape -> {
                            if (uiState.isSearchOpen) {
                                viewModel.setSearchOpen(false)
                            } else if (showPageSliderPill) {
                                showPageSliderPill = false
                            } else {
                                onBackToLibrary()
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val screenMaxWidth = maxWidth
        val isLargeScreen = screenMaxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isLargeScreen) {
                    // Unified Desktop App Window Titlebar (Merged Tab Bar + Toolbar)
                    DesktopPdfToolbar(
                        title = when (val state = uiState.loadState) {
                            is DocumentLoadState.Success -> state.title
                            is DocumentLoadState.Loading -> state.title
                            else -> "PDFMaxing"
                        },
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        zoomLevel = uiState.zoomLevel,
                        colorMode = uiState.colorMode,
                        layoutMode = uiState.layoutMode,
                        isSidebarOpen = uiState.isSidebarOpen,
                        tabs = uiState.tabs,
                        activeTabId = uiState.activeTabId,
                        onSelectTab = { tabId -> viewModel.switchTab(tabId) },
                        onCloseTab = { tabId ->
                            val hasRemaining = viewModel.closeTab(tabId)
                            if (!hasRemaining) onBackToLibrary()
                        },
                        onNewTabClick = { openFileLauncher.launch(arrayOf("application/pdf")) },
                        onBackClick = onBackToLibrary,
                        onCycleColorMode = { viewModel.cycleColorMode() },
                        onSelectColorMode = { mode -> viewModel.setColorMode(mode) },
                        onCycleLayoutMode = { viewModel.cycleLayoutMode() },
                        onSelectLayoutMode = { mode -> viewModel.setLayoutMode(mode) },
                        onToggleSidebar = { viewModel.toggleSidebar() }
                    )
                } else {
                    // Mobile Top Bar
                    Column {
                        if (uiState.tabs.size > 1) {
                            PdfTabBar(
                                tabs = uiState.tabs,
                                activeTabId = uiState.activeTabId,
                                onSelectTab = { tabId -> viewModel.switchTab(tabId) },
                                onCloseTab = { tabId ->
                                    val hasRemaining = viewModel.closeTab(tabId)
                                    if (!hasRemaining) onBackToLibrary()
                                },
                                onNewTabClick = { openFileLauncher.launch(arrayOf("application/pdf")) }
                            )
                        }
                        AnimatedVisibility(
                            visible = !uiState.isImmersive,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut()
                        ) {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = when (val state = uiState.loadState) {
                                                is DocumentLoadState.Success -> state.title
                                                else -> "PDFMaxing"
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (uiState.totalPages > 0) {
                                            Text(
                                                text = "${uiState.totalPages} pages",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                navigationIcon = {
                                    FilledTonalIconButton(
                                        onClick = onBackToLibrary,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    ThemeVibrantMenu(
                                        activeColorMode = uiState.colorMode,
                                        onSelectColorMode = { mode -> viewModel.setColorMode(mode) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    FilledTonalIconButton(
                                        onClick = { openFileLauncher.launch(arrayOf("application/pdf")) },
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Tab")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val loadState = uiState.loadState) {
                    is DocumentLoadState.Loading -> {
                        LoadingView(title = loadState.title)
                    }

                    is DocumentLoadState.Error -> {
                        ErrorView(
                            message = loadState.message,
                            canRelocate = loadState.canRelocate,
                            onPickFile = { openFileLauncher.launch(arrayOf("application/pdf")) },
                            onBackToLibrary = onBackToLibrary
                        )
                    }

                    is DocumentLoadState.Success -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Laptop / Large Screen: Persistent or Collapsible Sidebar
                            if (isLargeScreen && uiState.isSidebarOpen) {
                                ThumbnailSidebar(
                                    activeTab = uiState.activeSidebarTab,
                                    totalPages = uiState.totalPages,
                                    currentPage = uiState.currentPage,
                                    documentTitle = loadState.title,
                                    fileSizeBytes = loadState.fileSizeBytes,
                                    colorMode = uiState.colorMode,
                                    loadThumbnail = { pageIndex -> viewModel.getThumbnailBitmap(pageIndex) },
                                    onSelectTab = { tab -> viewModel.setSidebarTab(tab) },
                                    onPageClick = { page ->
                                        viewModel.setPage(page)
                                        scope.launch {
                                            val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                (page - 1) / 2
                                            } else {
                                                page - 1
                                            }
                                            listState.animateScrollToItem(item.coerceAtLeast(0))
                                        }
                                    },
                                    onCloseSidebar = { viewModel.setSidebarOpen(false) }
                                )
                            }

                            // Calculate available viewport width for the page canvas
                            val availableViewportWidth = screenMaxWidth - (if (isLargeScreen && uiState.isSidebarOpen) 320.dp else 0.dp)

                            // Main Viewport: Horizontally scrollable container + LazyColumn of scaled pages
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(horizontalScrollState),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    val pageSearchMatches = remember(uiState.searchMatches) {
                                        uiState.searchMatches.groupBy { it.pageIndex }
                                    }
                                    val currentMatch = uiState.searchMatches.getOrNull(uiState.currentMatchIndex)

                                    if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                        // Two-Page Spread Layout (Two pages visible side by side!)
                                        val dualBaseWidth = ((availableViewportWidth - 48.dp) / 2).coerceAtLeast(180.dp)
                                        val dualEffectiveWidth = dualBaseWidth * uiState.zoomLevel
                                        val pagePairs = remember(loadState.pageCount) {
                                            PdfTwoPageHelper.createPagePairs(loadState.pageCount)
                                        }

                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier
                                                .width(dualEffectiveWidth * 2 + 24.dp)
                                                .fillMaxHeight()
                                                .pointerInput(Unit) {
                                                    detectTransformGestures { _, _, zoomChange, _ ->
                                                        if (zoomChange != 1.0f) {
                                                            viewModel.setZoom(uiState.zoomLevel * zoomChange)
                                                        }
                                                    }
                                                },
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            items(pagePairs.size) { pairIndex ->
                                                val (leftPageIndex, rightPageIndex) = pagePairs[pairIndex]
                                                val leftMatches = pageSearchMatches[leftPageIndex] ?: emptyList()
                                                val isLeftCurrent = currentMatch != null && currentMatch.pageIndex == leftPageIndex
                                                val leftActiveMatchIndex = if (isLeftCurrent) currentMatch.matchIndexInPage else null

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    PdfPageItem(
                                                        pageIndex = leftPageIndex,
                                                        totalPages = loadState.pageCount,
                                                        pageWidth = dualEffectiveWidth,
                                                        scaleFactor = uiState.zoomLevel,
                                                        colorMode = uiState.colorMode,
                                                        loadBitmap = { idx, targetW, scale ->
                                                            viewModel.getPageBitmap(idx, targetW, scale)
                                                        },
                                                        onTap = { viewModel.toggleImmersive() },
                                                        onDoubleTap = { viewModel.handleDoubleTapZoom() },
                                                        searchMatches = leftMatches,
                                                        isCurrentMatchPage = isLeftCurrent,
                                                        activeMatchIndexInPage = leftActiveMatchIndex
                                                    )

                                                    if (rightPageIndex != null) {
                                                        val rightMatches = pageSearchMatches[rightPageIndex] ?: emptyList()
                                                        val isRightCurrent = currentMatch != null && currentMatch.pageIndex == rightPageIndex
                                                        val rightActiveMatchIndex = if (isRightCurrent) currentMatch.matchIndexInPage else null

                                                        PdfPageItem(
                                                            pageIndex = rightPageIndex,
                                                            totalPages = loadState.pageCount,
                                                            pageWidth = dualEffectiveWidth,
                                                            scaleFactor = uiState.zoomLevel,
                                                            colorMode = uiState.colorMode,
                                                            loadBitmap = { idx, targetW, scale ->
                                                                viewModel.getPageBitmap(idx, targetW, scale)
                                                            },
                                                            onTap = { viewModel.toggleImmersive() },
                                                            onDoubleTap = { viewModel.handleDoubleTapZoom() },
                                                            searchMatches = rightMatches,
                                                            isCurrentMatchPage = isRightCurrent,
                                                            activeMatchIndexInPage = rightActiveMatchIndex
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.width(dualEffectiveWidth))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Single-Page Layout (Fit 100% or 50% Overview)
                                        val basePageWidth = (availableViewportWidth - (if (isLargeScreen) 48.dp else 16.dp)).coerceAtLeast(280.dp)
                                        val effectivePageWidth = basePageWidth * uiState.zoomLevel

                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier
                                                .width(effectivePageWidth)
                                                .fillMaxHeight()
                                                .pointerInput(Unit) {
                                                    detectTransformGestures { _, _, zoomChange, _ ->
                                                        if (zoomChange != 1.0f) {
                                                            viewModel.setZoom(uiState.zoomLevel * zoomChange)
                                                        }
                                                    }
                                                }
                                                .pointerInput(Unit) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            if (event.type == PointerEventType.Scroll) {
                                                                if (event.keyboardModifiers.isPointerCtrlPressed) {
                                                                    val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                                    if (scrollDelta < 0) {
                                                                         viewModel.zoomIn()
                                                                    } else if (scrollDelta > 0) {
                                                                        viewModel.zoomOut()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            items(loadState.pageCount) { pageIndex ->
                                                val pageMatches = pageSearchMatches[pageIndex] ?: emptyList()
                                                val isCurrent = currentMatch != null && currentMatch.pageIndex == pageIndex
                                                val activeMatchIndex = if (isCurrent) currentMatch.matchIndexInPage else null

                                                PdfPageItem(
                                                    pageIndex = pageIndex,
                                                    totalPages = loadState.pageCount,
                                                    pageWidth = effectivePageWidth,
                                                    scaleFactor = uiState.zoomLevel,
                                                    colorMode = uiState.colorMode,
                                                    loadBitmap = { idx, targetW, scale ->
                                                        viewModel.getPageBitmap(idx, targetW, scale)
                                                    },
                                                    onTap = { viewModel.toggleImmersive() },
                                                    onDoubleTap = { viewModel.handleDoubleTapZoom() },
                                                    searchMatches = pageMatches,
                                                    isCurrentMatchPage = isCurrent,
                                                    activeMatchIndexInPage = activeMatchIndex
                                                )
                                            }
                                        }
                                    }
                                }

                                // Phone Mobile Floating Toolbar at bottom
                                if (!isLargeScreen) {
                                    MobileFloatingToolbar(
                                        isVisible = !uiState.isImmersive,
                                        currentPage = uiState.currentPage,
                                        totalPages = uiState.totalPages,
                                        colorMode = uiState.colorMode,
                                        layoutMode = uiState.layoutMode,
                                        isPageSliderPillOpen = showPageSliderPill,
                                        onTogglePageSliderPill = { showPageSliderPill = !showPageSliderPill },
                                        onPreviousPage = {
                                            viewModel.previousPage()
                                            scope.launch { listState.animateScrollToItem((uiState.currentPage - 2).coerceAtLeast(0)) }
                                        },
                                        onNextPage = {
                                            viewModel.nextPage()
                                            scope.launch { listState.animateScrollToItem(uiState.currentPage.coerceAtMost(uiState.totalPages - 1)) }
                                        },
                                        onPageJump = { page ->
                                            viewModel.setPage(page)
                                            scope.launch { listState.scrollToItem((page - 1).coerceAtLeast(0)) }
                                        },
                                        onCycleLayoutMode = { viewModel.cycleLayoutMode() },
                                        onCycleColorMode = { viewModel.cycleColorMode() },
                                        onToggleSidebar = { viewModel.toggleSidebar() },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 24.dp)
                                    )
                                }

                                // Desktop / Laptop Expressive Vertical Floating Toolbar for Zoom & Page Navigation (Bottom-Right)
                                if (isLargeScreen) {
                                    // Scrim to dismiss page slider pill or search pill when clicking anywhere outside
                                    if (showPageSliderPill || uiState.isSearchOpen) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(Unit) {
                                                    detectTapGestures {
                                                        showPageSliderPill = false
                                                        viewModel.setSearchOpen(false)
                                                    }
                                                }
                                        )
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !uiState.isImmersive,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 24.dp, bottom = 24.dp)
                                    ) {
                                        ExpressiveVerticalFloatingToolbar(
                                            currentPage = uiState.currentPage,
                                            totalPages = uiState.totalPages,
                                            zoomLevel = uiState.zoomLevel,
                                            isPageSliderPillOpen = showPageSliderPill,
                                            onTogglePageSliderPill = {
                                                if (!showPageSliderPill) viewModel.setSearchOpen(false)
                                                showPageSliderPill = !showPageSliderPill
                                            },
                                            onPreviousPage = {
                                                viewModel.previousPage()
                                                scope.launch {
                                                    val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                        (uiState.currentPage - 2).coerceAtLeast(0) / 2
                                                    } else {
                                                        (uiState.currentPage - 2).coerceAtLeast(0)
                                                    }
                                                    listState.animateScrollToItem(item)
                                                }
                                            },
                                            onNextPage = {
                                                viewModel.nextPage()
                                                scope.launch {
                                                    val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                        uiState.currentPage / 2
                                                    } else {
                                                        uiState.currentPage
                                                    }
                                                    listState.animateScrollToItem(item.coerceAtMost(uiState.totalPages - 1))
                                                }
                                            },
                                            onPageJump = { page ->
                                                viewModel.setPage(page)
                                                scope.launch {
                                                    val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                        (page - 1) / 2
                                                    } else {
                                                        page - 1
                                                    }
                                                    listState.scrollToItem(item.coerceAtLeast(0))
                                                }
                                            },
                                            onZoomIn = { viewModel.zoomIn() },
                                            onZoomOut = { viewModel.zoomOut() },
                                            onResetZoom = { viewModel.resetZoom() },
                                            // Search integration
                                            isSearchOpen = uiState.isSearchOpen,
                                            onToggleSearch = {
                                                showPageSliderPill = false
                                                viewModel.toggleSearch()
                                            },
                                            searchQuery = uiState.searchQuery,
                                            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                            currentMatchIndex = uiState.currentMatchIndex,
                                            totalMatches = uiState.searchMatches.size,
                                            isSearching = uiState.isSearching,
                                            onPreviousMatch = {
                                                val match = viewModel.previousSearchMatch()
                                                if (match != null) {
                                                    scope.launch {
                                                        val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                            match.pageIndex / 2
                                                        } else {
                                                            match.pageIndex
                                                        }
                                                        listState.animateScrollToItem(item)
                                                    }
                                                }
                                            },
                                            onNextMatch = {
                                                val match = viewModel.nextSearchMatch()
                                                if (match != null) {
                                                    scope.launch {
                                                        val item = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                                                            match.pageIndex / 2
                                                        } else {
                                                            match.pageIndex
                                                        }
                                                        listState.animateScrollToItem(item)
                                                    }
                                                }
                                            },
                                            onCloseSearch = { viewModel.setSearchOpen(false) }
                                        )
                                    }
                                }
                            }
                        }

                        // Phone Modal Bottom Sheet for Sidebar
                        if (!isLargeScreen && uiState.isSidebarOpen) {
                            ModalBottomSheet(
                                onDismissRequest = { viewModel.setSidebarOpen(false) },
                                sheetState = rememberModalBottomSheetState()
                            ) {
                                ThumbnailSidebar(
                                    activeTab = uiState.activeSidebarTab,
                                    totalPages = uiState.totalPages,
                                    currentPage = uiState.currentPage,
                                    documentTitle = loadState.title,
                                    fileSizeBytes = loadState.fileSizeBytes,
                                    colorMode = uiState.colorMode,
                                    loadThumbnail = { pageIndex -> viewModel.getThumbnailBitmap(pageIndex) },
                                    onSelectTab = { tab -> viewModel.setSidebarTab(tab) },
                                    onPageClick = { page ->
                                        viewModel.setPage(page)
                                        scope.launch { listState.animateScrollToItem(page - 1) }
                                        viewModel.setSidebarOpen(false)
                                    },
                                    onCloseSidebar = { viewModel.setSidebarOpen(false) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    is DocumentLoadState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No document open", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = onBackToLibrary, shape = RoundedCornerShape(12.dp)) {
                                    Text("Open Library")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Opening Document...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    canRelocate: Boolean,
    onPickFile: () -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Cannot Open Document",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canRelocate) {
                        Button(onClick = onPickFile, shape = RoundedCornerShape(12.dp)) {
                            Text("Locate File")
                        }
                    }
                    OutlinedButton(onClick = onBackToLibrary, shape = RoundedCornerShape(12.dp)) {
                        Text("Library")
                    }
                }
            }
        }
    }
}
