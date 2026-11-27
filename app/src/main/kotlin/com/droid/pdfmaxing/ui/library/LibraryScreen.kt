package com.droid.pdfmaxing.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droid.pdfmaxing.data.model.RecentDocument
import com.droid.pdfmaxing.ui.theme.EmphasizedTypography

enum class DocumentFilter(val label: String) {
    ALL("All Files"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Finished")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    onOpenPdfUri: (Uri) -> Unit,
    onOpenSampleGuide: () -> Unit = {},
    hasOpenTabs: Boolean = false,
    onReturnToReader: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(DocumentFilter.ALL) }
    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    if (hasOpenTabs) {
        BackHandler(onBack = onReturnToReader)
    }

    // SAF OpenDocument launcher for selecting any PDF from system storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onOpenPdfUri(uri)
        }
    }

    val filteredDocs = remember(recentDocs, uiState.searchQuery, selectedFilter) {
        recentDocs
            .filter { doc ->
                if (uiState.searchQuery.isBlank()) true
                else doc.title.contains(uiState.searchQuery, ignoreCase = true)
            }
            .filter { doc ->
                when (selectedFilter) {
                    DocumentFilter.ALL -> true
                    DocumentFilter.IN_PROGRESS -> doc.progressPercentage in 1..99
                    DocumentFilter.COMPLETED -> doc.progressPercentage == 100
                }
            }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (hasOpenTabs) {
                        IconButton(onClick = onReturnToReader) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Return to Document"
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = "PDFMaxing",
                            style = EmphasizedTypography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                expanded = isFabExpanded,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = "Open PDF",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val screenMaxWidth = maxWidth
            val isCompactScreen = screenMaxWidth < 600.dp

            // Adaptive horizontal margin and max content width for large/wide laptop screens (reduced margins by 35%)
            val maxContentWidth = when {
                maxWidth >= 1400.dp -> 1300.dp
                maxWidth >= 1000.dp -> 1080.dp
                maxWidth >= 800.dp -> 920.dp
                else -> maxWidth
            }

            val horizontalMargin = when {
                maxWidth >= 1200.dp -> 26.dp
                maxWidth >= 840.dp -> 21.dp
                maxWidth >= 600.dp -> 16.dp
                else -> 16.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = maxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalMargin)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Material 3 Expressive Filter Chips in pure native design without custom border radius or color overrides
                if (recentDocs.isNotEmpty()) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            DocumentFilter.entries.forEach { filter ->
                                val isSelected = selectedFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(text = filter.label)
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // Section 1: Recent Documents Carousel (HorizontalUncontainedCarousel)
                if (filteredDocs.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Jump Back In",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${filteredDocs.size} files",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val carouselState = rememberCarouselState { filteredDocs.size }
                            HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                preferredItemWidth = 260.dp,
                                itemSpacing = 14.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(188.dp)
                            ) { pageIndex ->
                                val doc = filteredDocs.getOrNull(pageIndex)
                                if (doc != null) {
                                    RecentCarouselCard(
                                        document = doc,
                                        onClick = {
                                            if (doc.isAsset) {
                                                onOpenSampleGuide()
                                            } else {
                                                onOpenPdfUri(Uri.parse(doc.uriString))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Recent Files Header with Search Bar on the Right
                if (recentDocs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Files",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isCompactScreen) {
                                IconButton(
                                    onClick = { viewModel.toggleSearching() },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isSearching) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = if (uiState.isSearching) "Close Search" else "Search Documents",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                DockedSearchBar(
                                    inputField = {
                                        SearchBarDefaults.InputField(
                                            query = uiState.searchQuery,
                                            onQueryChange = { viewModel.setSearchQuery(it) },
                                            onSearch = {},
                                            expanded = false,
                                            onExpandedChange = {},
                                            placeholder = {
                                                Text(
                                                    text = "Search files...",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                                {
                                                    IconButton(
                                                        onClick = { viewModel.setSearchQuery("") },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            } else null
                                        )
                                    },
                                    expanded = false,
                                    onExpandedChange = {},
                                    modifier = Modifier.widthIn(min = 220.dp, max = 320.dp),
                                    content = {}
                                )
                            }
                        }
                    }

                    // On compact screen: show search box when search icon is clicked
                    if (isCompactScreen) {
                        item {
                            val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                            val spatialSizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
                            AnimatedVisibility(
                                visible = uiState.isSearching,
                                enter = fadeIn(animationSpec = effectsSpec) + expandVertically(animationSpec = spatialSizeSpec),
                                exit = fadeOut(animationSpec = effectsSpec) + shrinkVertically(animationSpec = spatialSizeSpec)
                            ) {
                                DockedSearchBar(
                                    inputField = {
                                        SearchBarDefaults.InputField(
                                            query = uiState.searchQuery,
                                            onQueryChange = { viewModel.setSearchQuery(it) },
                                            onSearch = {},
                                            expanded = false,
                                            onExpandedChange = {},
                                            placeholder = {
                                                Text(
                                                    text = "Search files...",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            },
                                            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                                {
                                                    IconButton(
                                                        onClick = { viewModel.setSearchQuery("") },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            } else null
                                        )
                                    },
                                    expanded = false,
                                    onExpandedChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    content = {}
                                )
                            }
                        }
                    }

                    if (filteredDocs.isNotEmpty()) {
                        if (isCompactScreen) {
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                                ) {
                                    filteredDocs.forEachIndexed { index, doc ->
                                        val shapes = ListItemDefaults.segmentedShapes(
                                            index = index,
                                            count = filteredDocs.size
                                        )
                                        RecentFileSegmentedListItem(
                                            document = doc,
                                            shapes = shapes,
                                            onClick = {
                                                if (doc.isAsset) {
                                                    onOpenSampleGuide()
                                                } else {
                                                    onOpenPdfUri(Uri.parse(doc.uriString))
                                                }
                                            },
                                            onDelete = { viewModel.removeDocument(doc.uriString) }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Medium (2 columns) & Expanded/Large (3 columns) adaptive grid
                            val columnCount = if (screenMaxWidth >= 840.dp) 3 else 2
                            val rows = filteredDocs.chunked(columnCount)
                            items(
                                count = rows.size,
                                key = { rowIdx -> "grid_row_${rowIdx}_${rows[rowIdx].first().uriString}" }
                            ) { rowIdx ->
                                val rowDocs = rows[rowIdx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                            fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                        ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowDocs.forEach { doc ->
                                        RecentFileGridCard(
                                            document = doc,
                                            onClick = {
                                                if (doc.isAsset) {
                                                    onOpenSampleGuide()
                                                } else {
                                                    onOpenPdfUri(Uri.parse(doc.uriString))
                                                }
                                            },
                                            onDelete = { viewModel.removeDocument(doc.uriString) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(columnCount - rowDocs.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        // Search or filter produced 0 results
                        item {
                            SearchEmptyStateView(query = uiState.searchQuery)
                        }
                    }
                } else {
                    // Empty state when user hasn't opened any files yet
                    item {
                        EmptyLibraryView(
                            onOpenFile = { filePickerLauncher.launch(arrayOf("application/pdf")) }
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CarouselItemScope.RecentCarouselCard(
    document: RecentDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .maskClip(MaterialTheme.shapes.extraLarge)
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${document.pageCount} pgs",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearWavyProgressIndicator(
                    progress = { document.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val zoomPercent = (document.zoomLevel * 100).toInt()
                Text(
                    text = "Page ${document.lastReadPage} / ${document.pageCount} (${document.progressPercentage}%) • $zoomPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Segmented Filled List Item for recent PDF files.
 * Uses SegmentedListItem with ListItemDefaults.segmentedShapes and ListItemDefaults.segmentedColors.
 * Reference: https://developer.android.com/reference/kotlin/androidx/compose/material3/SegmentedListItem.composable
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentFileSegmentedListItem(
    document: RecentDocument,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Remove from recents",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        supportingContent = {
            Text(
                text = "${document.pageCount} pages • ${document.formattedFileSize} • Page ${document.lastReadPage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentFileGridCard(
    document: RecentDocument,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Remove from recents",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = document.title,
                    style = EmphasizedTypography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.pageCount} pages • ${document.formattedFileSize} • Page ${document.lastReadPage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearWavyProgressIndicator(
                progress = { document.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyLibraryView(
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.92f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No Documents Yet",
                    style = EmphasizedTypography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select any PDF from your device storage to begin reading with continuous fluid scrolling, dark mode inversion, and high-performance zoom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                MediumExtendedFloatingActionButton(
                    onClick = onOpenFile,
                    expanded = true,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Select PDF File",
                            style = EmphasizedTypography.labelLarge
                        )
                    },
                    shape = FloatingActionButtonDefaults.mediumExtendedFabShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchEmptyStateView(query: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.9f,
            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
        ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec())
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (query.isBlank()) "No matching documents" else "No results for \"$query\"",
                style = EmphasizedTypography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try adjusting your search or filter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
