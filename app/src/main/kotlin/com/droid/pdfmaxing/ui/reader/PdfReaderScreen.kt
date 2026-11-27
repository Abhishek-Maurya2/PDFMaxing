package com.droid.pdfmaxing.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed as isPointerCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droid.pdfmaxing.data.settings.AppSettings
import com.droid.pdfmaxing.ui.components.DesktopPdfToolbar
import com.droid.pdfmaxing.ui.components.ExpressiveTabBar
import com.droid.pdfmaxing.ui.components.ExpressiveVerticalFloatingToolbar
import com.droid.pdfmaxing.ui.components.MobileFloatingToolbar
import com.droid.pdfmaxing.ui.components.PdfPageItem
import com.droid.pdfmaxing.ui.components.SearchPill
import com.droid.pdfmaxing.ui.components.ThemeVibrantMenu
import com.droid.pdfmaxing.ui.components.ThumbnailSidebar
import com.droid.pdfmaxing.ui.theme.EmphasizedTypography
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PdfReaderScreen(
    documentUri: Uri?,
    isSampleGuide: Boolean = false,
    onBackToLibrary: () -> Unit,
    onOpenSettings: () -> Unit = {},
    appSettings: AppSettings = AppSettings(),
    viewModel: PdfReaderViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabListStates = remember { mutableMapOf<String, LazyListState>() }
    val currentTabId = uiState.activeTabId ?: ""
    val listState = tabListStates.getOrPut(currentTabId) {
        val targetPage = uiState.currentPage
        val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
            (targetPage - 1).coerceAtLeast(0) / 2
        } else {
            (targetPage - 1).coerceAtLeast(0)
        }
        LazyListState(
            firstVisibleItemIndex = targetItem,
            firstVisibleItemScrollOffset = uiState.scrollOffset
        )
    }
    val horizontalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var showPageSliderPill by remember { mutableStateOf(false) }

    // SAF File Explorer Picker for opening a new document / tab directly in device file manager
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: android.content.Context, input: Array<String>): Intent {
                return super.createIntent(context, input).apply {
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    putExtra("android.content.extra.SHOW_ADVANCED", true)
                }
            }
        }
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.loadDocument(uri, initialPage = 1, asNewTab = true)
        }
    }

    // SAF Picker for relocating a broken/moved document in the active tab
    val relocateFileLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: android.content.Context, input: Array<String>): Intent {
                return super.createIntent(context, input).apply {
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    putExtra("android.content.extra.SHOW_ADVANCED", true)
                }
            }
        }
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.relocateActiveDocument(uri)
        }
    }

    // Load initial document if not already in tabs, or switch to it if already open
    LaunchedEffect(documentUri, isSampleGuide) {
        if (isSampleGuide) {
            val existingTab = uiState.tabs.find { it.isAsset && it.assetPath == "sample_guide.pdf" }
            if (existingTab != null) {
                if (uiState.activeTabId != existingTab.id) {
                    viewModel.switchTab(existingTab.id)
                }
            } else {
                viewModel.loadSampleGuide(asNewTab = uiState.tabs.isNotEmpty())
            }
        } else if (documentUri != null) {
            val targetUriStr = documentUri.toString()
            val existingTab = uiState.tabs.find { it.uri?.toString() == targetUriStr }
            if (existingTab != null) {
                if (uiState.activeTabId != existingTab.id) {
                    viewModel.switchTab(existingTab.id)
                }
            } else {
                viewModel.loadDocument(documentUri, initialPage = 1, asNewTab = uiState.tabs.isNotEmpty())
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

    // Track whether initial scroll position for active tab has been restored
    var hasRestoredScrollForTab by remember(uiState.activeTabId) { mutableStateOf(false) }
    var lastRestoredLayoutMode by remember(uiState.activeTabId) { mutableStateOf<PdfViewLayoutMode?>(null) }

    LaunchedEffect(uiState.activeTabId, uiState.loadState) {
        if (uiState.loadState is DocumentLoadState.Success && !hasRestoredScrollForTab) {
            val targetPage = uiState.currentPage
            val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                (targetPage - 1).coerceAtLeast(0) / 2
            } else {
                (targetPage - 1).coerceAtLeast(0)
            }
            listState.scrollToItem(targetItem, uiState.scrollOffset)
            hasRestoredScrollForTab = true
            lastRestoredLayoutMode = uiState.layoutMode
        }
    }

    // Keep scroll position aligned when layout mode changes within the active tab
    LaunchedEffect(uiState.layoutMode) {
        if (uiState.totalPages > 0 && hasRestoredScrollForTab &&
            lastRestoredLayoutMode != null && lastRestoredLayoutMode != uiState.layoutMode
        ) {
            val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                (uiState.currentPage - 1).coerceAtLeast(0) / 2
            } else {
                (uiState.currentPage - 1).coerceAtLeast(0)
            }
            listState.scrollToItem(targetItem, uiState.scrollOffset)
            lastRestoredLayoutMode = uiState.layoutMode
        }
    }

    val scrollToPage: (Int, Boolean) -> Unit = { page, animate ->
        scope.launch {
            val clampedPage = PdfPageHelper.clampPage(page, uiState.totalPages)
            val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                (clampedPage - 1).coerceAtLeast(0) / 2
            } else {
                (clampedPage - 1).coerceAtLeast(0)
            }
            if (animate) {
                listState.animateScrollToItem(targetItem)
            } else {
                listState.scrollToItem(targetItem)
            }
        }
    }

    // Sync lazy list visible item and scroll offset to current tab during scroll
    LaunchedEffect(listState, uiState.layoutMode, uiState.activeTabId) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .distinctUntilChanged()
            .collect { (firstIndex, scrollOffset) ->
                if (!hasRestoredScrollForTab) return@collect
                if (uiState.totalPages > 0 && listState.isScrollInProgress) {
                    val computedPage = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                        firstIndex * 2 + 1
                    } else {
                        firstIndex + 1
                    }
                    viewModel.updateScrollPosition(
                        page = computedPage.coerceIn(1, uiState.totalPages),
                        scrollOffset = scrollOffset
                    )
                }
            }
    }

    // Automatically scroll to active search match page when match changes or search results update
    LaunchedEffect(uiState.currentMatchIndex, uiState.searchMatches) {
        val currentMatch = uiState.searchMatches.getOrNull(uiState.currentMatchIndex)
        if (currentMatch != null) {
            val targetItem = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) {
                currentMatch.pageIndex / 2
            } else {
                currentMatch.pageIndex
            }
            listState.animateScrollToItem(targetItem.coerceAtLeast(0))
        }
    }

    val handleBackToLibrary = {
        viewModel.saveCurrentState()
        onBackToLibrary()
    }

    val handleCloseTab = { tabId: String ->
        tabListStates.remove(tabId)
        val hasRemaining = viewModel.closeTab(tabId)
        if (!hasRemaining) handleBackToLibrary()
    }

    // Intercept hardware Back button
    BackHandler {
        if (uiState.isSearchOpen) {
            viewModel.setSearchOpen(false)
        } else if (showPageSliderPill) {
            showPageSliderPill = false
        } else {
            handleBackToLibrary()
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
                        // Ctrl + O: Open home page to open recent or new PDF
                        keyEvent.isCtrlPressed && keyEvent.key == Key.O -> {
                            handleBackToLibrary()
                            true
                        }
                        // Ctrl + W: Close current tab
                        keyEvent.isCtrlPressed && keyEvent.key == Key.W -> {
                            val activeId = uiState.activeTabId
                            if (activeId != null) {
                                handleCloseTab(activeId)
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
                        // Page Down, Space, Right arrow: Next page
                        keyEvent.key == Key.PageDown || (keyEvent.key == Key.Spacebar && !keyEvent.isShiftPressed) || keyEvent.key == Key.DirectionRight -> {
                            val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                            val targetPage = PdfPageHelper.clampPage(uiState.currentPage + step, uiState.totalPages)
                            viewModel.nextPage()
                            scrollToPage(targetPage, true)
                            true
                        }
                        // Page Up, Shift+Space, Left arrow: Previous page
                        keyEvent.key == Key.PageUp || (keyEvent.key == Key.Spacebar && keyEvent.isShiftPressed) || keyEvent.key == Key.DirectionLeft -> {
                            val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                            val targetPage = PdfPageHelper.clampPage(uiState.currentPage - step, uiState.totalPages)
                            viewModel.previousPage()
                            scrollToPage(targetPage, true)
                            true
                        }
                        // Escape: Dismiss search, or dismiss slider, or return to library
                        keyEvent.key == Key.Escape -> {
                            if (uiState.isSearchOpen) {
                                viewModel.setSearchOpen(false)
                            } else if (showPageSliderPill) {
                                showPageSliderPill = false
                            } else {
                                handleBackToLibrary()
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

        LaunchedEffect(isLargeScreen, appSettings.autoOpenSidebar) {
            if (isLargeScreen && appSettings.autoOpenSidebar && !uiState.isSidebarOpen) {
                viewModel.setSidebarOpen(true)
            }
        }

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
                        onCloseTab = handleCloseTab,
                        onNewTabClick = handleBackToLibrary,
                        onBackClick = handleBackToLibrary,
                        onCycleColorMode = { viewModel.cycleColorMode() },
                        onSelectColorMode = { mode -> viewModel.setColorMode(mode) },
                        onCycleLayoutMode = { viewModel.cycleLayoutMode() },
                        onSelectLayoutMode = { mode -> viewModel.setLayoutMode(mode) },
                        onToggleSidebar = { viewModel.toggleSidebar() },
                        onOpenSettings = onOpenSettings
                    )
                } else {
                    // Mobile / Compact Top Bar: App Title Bar on top, separated Tab Bar below
                    val spatialOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    AnimatedVisibility(
                        visible = !uiState.isImmersive || uiState.isSearchOpen,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = spatialOffsetSpec
                        ) + fadeIn(animationSpec = effectsSpec),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = spatialOffsetSpec
                        ) + fadeOut(animationSpec = effectsSpec)
                    ) {
                        Column {
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
                                    IconButton(onClick = handleBackToLibrary) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                actions = {
                                    ThemeVibrantMenu(
                                        activeColorMode = uiState.colorMode,
                                        onSelectColorMode = { mode -> viewModel.setColorMode(mode) },
                                        activeLayoutMode = uiState.layoutMode,
                                        onSelectLayoutMode = { mode -> viewModel.setLayoutMode(mode) }
                                    )
                                    IconButton(
                                        onClick = {
                                            showPageSliderPill = false
                                            viewModel.toggleSearch()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                                            contentDescription = if (uiState.isSearchOpen) "Close Search" else "Search in PDF"
                                        )
                                    }
                                    FilledTonalIconButton(
                                        onClick = { openFileLauncher.launch(arrayOf("application/pdf")) },
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Tab")
                                    }
                                    IconButton(onClick = onOpenSettings) {
                                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                            if (uiState.tabs.size > 1 || (appSettings.showTabBarOnSingleTab && uiState.tabs.isNotEmpty())) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                ExpressiveTabBar(
                                    tabs = uiState.tabs,
                                    activeTabId = uiState.activeTabId,
                                    onSelectTab = { tabId -> viewModel.switchTab(tabId) },
                                    onCloseTab = handleCloseTab,
                                    onNewTabClick = handleBackToLibrary
                                )
                            }
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
                            onPickFile = { relocateFileLauncher.launch(arrayOf("application/pdf")) },
                            onBackToLibrary = {
                                val failedTabId = uiState.activeTabId
                                if (failedTabId != null && uiState.tabs.size <= 1) {
                                    handleCloseTab(failedTabId)
                                } else {
                                    handleBackToLibrary()
                                }
                            }
                        )
                    }

                    is DocumentLoadState.Success -> {
                        val activeDocId = uiState.activeTabId ?: ""
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Laptop / Large Screen: Animated Collapsible Sidebar
                            AnimatedVisibility(
                                visible = isLargeScreen && uiState.isSidebarOpen,
                                enter = expandHorizontally(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = shrinkHorizontally(
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                ) + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
                            ) {
                                ThumbnailSidebar(
                                    documentId = activeDocId,
                                    activeTab = uiState.activeSidebarTab,
                                    totalPages = uiState.totalPages,
                                    currentPage = uiState.currentPage,
                                    documentTitle = loadState.title,
                                    fileSizeBytes = loadState.fileSizeBytes,
                                    colorMode = uiState.colorMode,
                                    loadThumbnail = { pageIndex -> viewModel.getThumbnailBitmap(activeDocId, pageIndex) },
                                    onSelectTab = { tab -> viewModel.setSidebarTab(tab) },
                                    onPageClick = { page ->
                                        viewModel.setPage(page)
                                        scrollToPage(page, true)
                                    },
                                    onCloseSidebar = { viewModel.setSidebarOpen(false) }
                                )
                            }

                            // Calculate available viewport width for the page canvas
                            val availableViewportWidth = screenMaxWidth - (if (isLargeScreen && uiState.isSidebarOpen) 320.dp else 0.dp)

                            // Main Viewport: Pinch-to-zoom + Horizontally scrollable container + LazyColumn of scaled pages
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .pointerInput(appSettings.pinchToZoom) {
                                        if (!appSettings.pinchToZoom) return@pointerInput
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            var pastTouchSlop = false
                                            var zoom = 1f
                                            val touchSlop = viewConfiguration.touchSlop

                                            do {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val pressedCount = event.changes.count { it.pressed }
                                                if (pressedCount >= 2) {
                                                    val zoomChange = event.calculateZoom()
                                                    val panChange = event.calculatePan()

                                                    if (!pastTouchSlop) {
                                                        zoom *= zoomChange
                                                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                                        val zoomMotion = kotlin.math.abs(1f - zoom) * centroidSize
                                                        if (zoomMotion > touchSlop) {
                                                            pastTouchSlop = true
                                                        }
                                                    }

                                                    if (pastTouchSlop) {
                                                        if (zoomChange != 1f) {
                                                            viewModel.zoomBy(zoomChange)
                                                        }
                                                        if (panChange.x != 0f) {
                                                            horizontalScrollState.dispatchRawDelta(-panChange.x)
                                                        }
                                                        if (panChange.y != 0f) {
                                                            listState.dispatchRawDelta(-panChange.y)
                                                        }
                                                    }
                                                    // Consume 2+ finger events in Initial pass so child scrollables/tap detectors don't steal the pinch
                                                    event.changes.forEach { it.consume() }
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isPointerCtrlPressed) {
                                                    val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                    if (scrollDelta < 0) {
                                                        viewModel.zoomIn()
                                                    } else if (scrollDelta > 0) {
                                                        viewModel.zoomOut()
                                                    }
                                                }
                                            }
                                        }
                                    },
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
                                                .fillMaxHeight(),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            items(
                                                count = pagePairs.size,
                                                key = { pairIdx -> "${uiState.activeTabId ?: ""}_pair_$pairIdx" }
                                            ) { pairIndex ->
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
                                                        documentId = activeDocId,
                                                        totalPages = loadState.pageCount,
                                                        pageWidth = dualEffectiveWidth,
                                                        scaleFactor = uiState.zoomLevel,
                                                        colorMode = uiState.colorMode,
                                                        loadBitmap = { idx, targetW, _ ->
                                                            viewModel.getPageBitmap(activeDocId, idx, targetW)
                                                        },
                                                        onTap = { if (appSettings.tapToToggleImmersive && !uiState.isSearchOpen) viewModel.toggleImmersive() },
                                                        onDoubleTap = { if (appSettings.doubleTapToZoom) viewModel.handleDoubleTapZoom() },
                                                        loadTextSegments = { idx -> viewModel.getPageTextSegments(activeDocId, idx) },
                                                        onSelectNativeContent = { idx, sx, sy, ex, ey ->
                                                            viewModel.selectContent(activeDocId, idx, sx, sy, ex, ey)
                                                        },
                                                        onSearchSelectedText = { query ->
                                                            viewModel.setSearchOpen(true)
                                                            viewModel.onSearchQueryChanged(query)
                                                        },
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
                                                            documentId = activeDocId,
                                                            totalPages = loadState.pageCount,
                                                            pageWidth = dualEffectiveWidth,
                                                            scaleFactor = uiState.zoomLevel,
                                                            colorMode = uiState.colorMode,
                                                            loadBitmap = { idx, targetW, _ ->
                                                                viewModel.getPageBitmap(activeDocId, idx, targetW)
                                                            },
                                                            onTap = { if (appSettings.tapToToggleImmersive && !uiState.isSearchOpen) viewModel.toggleImmersive() },
                                                            onDoubleTap = { if (appSettings.doubleTapToZoom) viewModel.handleDoubleTapZoom() },
                                                            loadTextSegments = { idx -> viewModel.getPageTextSegments(activeDocId, idx) },
                                                            onSelectNativeContent = { idx, sx, sy, ex, ey ->
                                                                viewModel.selectContent(activeDocId, idx, sx, sy, ex, ey)
                                                            },
                                                            onSearchSelectedText = { query ->
                                                                viewModel.setSearchOpen(true)
                                                                viewModel.onSearchQueryChanged(query)
                                                            },
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
                                                .fillMaxHeight(),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            items(
                                                count = loadState.pageCount,
                                                key = { pageIdx -> "${activeDocId}_page_$pageIdx" }
                                            ) { pageIndex ->
                                                val pageMatches = pageSearchMatches[pageIndex] ?: emptyList()
                                                val isCurrent = currentMatch != null && currentMatch.pageIndex == pageIndex
                                                val activeMatchIndex = if (isCurrent) currentMatch.matchIndexInPage else null

                                                PdfPageItem(
                                                    pageIndex = pageIndex,
                                                    documentId = activeDocId,
                                                    totalPages = loadState.pageCount,
                                                    pageWidth = effectivePageWidth,
                                                    scaleFactor = uiState.zoomLevel,
                                                    colorMode = uiState.colorMode,
                                                    loadBitmap = { idx, targetW, _ ->
                                                        viewModel.getPageBitmap(activeDocId, idx, targetW)
                                                    },
                                                    onTap = { if (appSettings.tapToToggleImmersive && !uiState.isSearchOpen) viewModel.toggleImmersive() },
                                                    onDoubleTap = { if (appSettings.doubleTapToZoom) viewModel.handleDoubleTapZoom() },
                                                    loadTextSegments = { idx -> viewModel.getPageTextSegments(activeDocId, idx) },
                                                    onSelectNativeContent = { idx, sx, sy, ex, ey ->
                                                        viewModel.selectContent(activeDocId, idx, sx, sy, ex, ey)
                                                    },
                                                    onSearchSelectedText = { query ->
                                                        viewModel.setSearchOpen(true)
                                                        viewModel.onSearchQueryChanged(query)
                                                    },
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
                                    val spatialOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                                    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Mobile Search Pop-up Pill when active
                                        AnimatedVisibility(
                                            visible = uiState.isSearchOpen,
                                            enter = fadeIn(animationSpec = effectsSpec) +
                                                    slideInVertically(
                                                        initialOffsetY = { fullHeight -> fullHeight / 2 },
                                                        animationSpec = spatialOffsetSpec
                                                    ),
                                            exit = fadeOut(animationSpec = effectsSpec) +
                                                   slideOutVertically(
                                                       targetOffsetY = { fullHeight -> fullHeight / 2 },
                                                       animationSpec = spatialOffsetSpec
                                                   )
                                        ) {
                                            SearchPill(
                                                searchQuery = uiState.searchQuery,
                                                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                                currentMatchIndex = uiState.currentMatchIndex,
                                                totalMatches = uiState.searchMatches.size,
                                                isSearching = uiState.isSearching,
                                                onPreviousMatch = {
                                                    val match = viewModel.previousSearchMatch()
                                                    if (match != null) {
                                                        scrollToPage(match.pageIndex + 1, true)
                                                    }
                                                },
                                                onNextMatch = {
                                                    val match = viewModel.nextSearchMatch()
                                                    if (match != null) {
                                                        scrollToPage(match.pageIndex + 1, true)
                                                    }
                                                },
                                                onCloseSearch = { viewModel.setSearchOpen(false) }
                                            )
                                        }

                                        MobileFloatingToolbar(
                                            isVisible = !uiState.isImmersive || uiState.isSearchOpen,
                                            currentPage = uiState.currentPage,
                                            totalPages = uiState.totalPages,
                                            colorMode = uiState.colorMode,
                                            layoutMode = uiState.layoutMode,
                                            isPageSliderPillOpen = showPageSliderPill,
                                            onTogglePageSliderPill = {
                                                if (!showPageSliderPill) viewModel.setSearchOpen(false)
                                                showPageSliderPill = !showPageSliderPill
                                            },
                                            onPreviousPage = {
                                                val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                                                val targetPage = PdfPageHelper.clampPage(uiState.currentPage - step, uiState.totalPages)
                                                viewModel.previousPage()
                                                scrollToPage(targetPage, true)
                                            },
                                            onNextPage = {
                                                val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                                                val targetPage = PdfPageHelper.clampPage(uiState.currentPage + step, uiState.totalPages)
                                                viewModel.nextPage()
                                                scrollToPage(targetPage, true)
                                            },
                                            onPageJump = { page ->
                                                viewModel.setPage(page)
                                                scrollToPage(page, false)
                                            },
                                            onCycleLayoutMode = { viewModel.cycleLayoutMode() },
                                            onCycleColorMode = { viewModel.cycleColorMode() },
                                            onToggleSidebar = { viewModel.toggleSidebar() },
                                            isSearchOpen = uiState.isSearchOpen,
                                            onToggleSearch = {
                                                showPageSliderPill = false
                                                viewModel.toggleSearch()
                                            }
                                        )
                                    }
                                }

                                // Desktop / Laptop Expressive Vertical Floating Toolbar for Zoom & Page Navigation (Bottom-Right)
                                if (isLargeScreen) {
                                    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !uiState.isImmersive || uiState.isSearchOpen,
                                        enter = fadeIn(animationSpec = effectsSpec),
                                        exit = fadeOut(animationSpec = effectsSpec),
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
                                                val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                                                val targetPage = PdfPageHelper.clampPage(uiState.currentPage - step, uiState.totalPages)
                                                viewModel.previousPage()
                                                scrollToPage(targetPage, true)
                                            },
                                            onNextPage = {
                                                val step = if (uiState.layoutMode == PdfViewLayoutMode.TWO_PAGE) 2 else 1
                                                val targetPage = PdfPageHelper.clampPage(uiState.currentPage + step, uiState.totalPages)
                                                viewModel.nextPage()
                                                scrollToPage(targetPage, true)
                                            },
                                            onPageJump = { page ->
                                                viewModel.setPage(page)
                                                scrollToPage(page, false)
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
                                                    scrollToPage(match.pageIndex + 1, true)
                                                }
                                            },
                                            onNextMatch = {
                                                val match = viewModel.nextSearchMatch()
                                                if (match != null) {
                                                    scrollToPage(match.pageIndex + 1, true)
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
                                    documentId = activeDocId,
                                    activeTab = uiState.activeSidebarTab,
                                    totalPages = uiState.totalPages,
                                    currentPage = uiState.currentPage,
                                    documentTitle = loadState.title,
                                    fileSizeBytes = loadState.fileSizeBytes,
                                    colorMode = uiState.colorMode,
                                    loadThumbnail = { pageIndex -> viewModel.getThumbnailBitmap(activeDocId, pageIndex) },
                                    onSelectTab = { tab -> viewModel.setSidebarTab(tab) },
                                    onPageClick = { page ->
                                        viewModel.setPage(page)
                                        scrollToPage(page, true)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingView(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ContainedLoadingIndicator(
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Opening Document",
            style = EmphasizedTypography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearWavyProgressIndicator(
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ErrorView(
    message: String,
    canRelocate: Boolean,
    onPickFile: () -> Unit,
    onBackToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.92f,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.85f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cannot Open Document",
                        style = EmphasizedTypography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (canRelocate) {
                            Button(onClick = onPickFile, shape = MaterialTheme.shapes.medium) {
                                Text("Locate File", style = EmphasizedTypography.labelLarge)
                            }
                        }
                        OutlinedButton(onClick = onBackToLibrary, shape = MaterialTheme.shapes.medium) {
                            Text("Library", style = EmphasizedTypography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
