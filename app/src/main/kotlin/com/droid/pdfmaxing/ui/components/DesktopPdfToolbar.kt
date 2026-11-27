package com.droid.pdfmaxing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.droid.pdfmaxing.ui.reader.PdfTab
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.PdfColorMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DesktopPdfToolbar(
    title: String,
    currentPage: Int = 1,
    totalPages: Int = 1,
    zoomLevel: Float = 1.0f,
    colorMode: PdfColorMode = PdfColorMode.NORMAL,
    layoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100,
    isSidebarOpen: Boolean = false,
    tabs: List<PdfTab> = emptyList(),
    activeTabId: String? = null,
    onSelectTab: (String) -> Unit = {},
    onCloseTab: (String) -> Unit = {},
    onNewTabClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onPageJump: (Int) -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onResetZoom: () -> Unit = {},
    onToggleSidebar: () -> Unit = {},
    modifier: Modifier = Modifier,
    startPadding: Dp = 80.dp,
    endPadding: Dp = 216.dp,
    onCycleColorMode: () -> Unit = {},
    onSelectColorMode: (PdfColorMode) -> Unit = {},
    onCycleLayoutMode: () -> Unit = {},
    onSelectLayoutMode: (PdfViewLayoutMode) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenFileClick: (() -> Unit)? = null
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isFullscreen = statusBarTop > 0.dp
    val effectiveStartPadding = if (isFullscreen) 170.dp else startPadding
    val tabListState = rememberLazyListState()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val spatialOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    LaunchedEffect(activeTabId, tabs.size) {
        val idx = tabs.indexOfFirst { it.id == activeTabId }
        if (idx >= 0) {
            tabListState.animateScrollToItem(idx)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(start = effectiveStartPadding, end = endPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Group: Sidebar Toggle
                IconToggleButton(
                    checked = isSidebarOpen,
                    onCheckedChange = { onToggleSidebar() },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(40.dp)
                        .systemGestureExclusion()
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewSidebar,
                        contentDescription = if (isSidebarOpen) "Hide Thumbnails" else "Show Thumbnails",
                        tint = if (isSidebarOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Group: Chrome-Style Expressive Tabs & "+" New Tab Button
                if (tabs.isNotEmpty()) {
                    LazyRow(
                        state = tabListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val isActive = tab.id == activeTabId
                            ExpressiveTabItem(
                                tab = tab,
                                isActive = isActive,
                                onClick = { onSelectTab(tab.id) },
                                onClose = { onCloseTab(tab.id) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = effectsSpec,
                                    fadeOutSpec = effectsSpec,
                                    placementSpec = spatialOffsetSpec
                                )
                            )
                        }

                        item(key = "desktop_new_tab") {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 2.dp, bottom = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NewTabButton(onClick = onNewTabClick)
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Group: Vibrant Options Menu (Theme + Layout submenus) & Settings
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .systemGestureExclusion()
                        .padding(start = 6.dp)
                ) {
                    ThemeVibrantMenu(
                        activeColorMode = colorMode,
                        onSelectColorMode = onSelectColorMode,
                        activeLayoutMode = layoutMode,
                        onSelectLayoutMode = onSelectLayoutMode
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .systemGestureExclusion()
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewToggleSplitButton(
    layoutMode: PdfViewLayoutMode,
    onCycleLayoutMode: () -> Unit,
    onSelectLayoutMode: (PdfViewLayoutMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (currentIcon, currentLabel) = when (layoutMode) {
        PdfViewLayoutMode.FIT_100 -> Pair(Icons.Rounded.FitScreen, "Fit 100%")
        PdfViewLayoutMode.FIT_50 -> Pair(Icons.Rounded.ZoomOut, "50% View")
        PdfViewLayoutMode.TWO_PAGE -> Pair(Icons.Rounded.AutoStories, "Two-Page")
    }

    val rotation: Float by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        label = "Trailing Chevron Rotation"
    )

    Box(modifier = modifier.systemGestureExclusion()) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = onCycleLayoutMode,
                    modifier = Modifier
                        .systemGestureExclusion()
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    modifier = Modifier
                        .systemGestureExclusion()
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Select Layout Mode",
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            PdfViewLayoutMode.entries.forEach { mode ->
                val (modeIcon, modeDesc) = when (mode) {
                    PdfViewLayoutMode.FIT_100 -> Pair(Icons.Rounded.FitScreen, "Fit 100% (Single Page)")
                    PdfViewLayoutMode.FIT_50 -> Pair(Icons.Rounded.ZoomOut, "50% Overview (Multi-page)")
                    PdfViewLayoutMode.TWO_PAGE -> Pair(Icons.Rounded.AutoStories, "Two-Page Spread (Book)")
                }
                val isSelected = mode == layoutMode
                DropdownMenuItem(
                    text = {
                        Text(
                            text = modeDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        onSelectLayoutMode(mode)
                        menuExpanded = false
                    },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeVibrantMenu(
    activeColorMode: PdfColorMode,
    onSelectColorMode: (PdfColorMode) -> Unit,
    activeLayoutMode: PdfViewLayoutMode = PdfViewLayoutMode.FIT_100,
    onSelectLayoutMode: (PdfViewLayoutMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var themeSubmenuExpanded by remember { mutableStateOf(false) }
    var layoutSubmenuExpanded by remember { mutableStateOf(false) }

    val modes = listOf(
        Triple(PdfColorMode.NORMAL, "Standard", Icons.Rounded.LightMode),
        Triple(PdfColorMode.INVERTED, "Dark", Icons.Rounded.InvertColors),
        Triple(PdfColorMode.NIGHT_SLATE, "Slate", Icons.Rounded.Bedtime)
    )

    val layouts = listOf(
        Triple(PdfViewLayoutMode.FIT_100, "Fit 100%", Icons.Rounded.FitScreen),
        Triple(PdfViewLayoutMode.FIT_50, "50% View", Icons.Rounded.ZoomOut),
        Triple(PdfViewLayoutMode.TWO_PAGE, "Two-Page Spread", Icons.Rounded.AutoStories)
    )

    Box(modifier = modifier.systemGestureExclusion()) {
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .systemGestureExclusion()
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Reader Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                themeSubmenuExpanded = false
                layoutSubmenuExpanded = false
            },
            containerColor = MenuDefaults.groupVibrantContainerColor,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            // Submenu 1: Page Theme
            Box {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Page Theme",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.InvertColors,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "Open Page Theme Submenu",
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = -90f }
                        )
                    },
                    onClick = {
                        layoutSubmenuExpanded = false
                        themeSubmenuExpanded = !themeSubmenuExpanded
                    },
                    colors = MenuDefaults.itemVibrantColors(),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )

                DropdownMenu(
                    expanded = themeSubmenuExpanded,
                    onDismissRequest = { themeSubmenuExpanded = false },
                    containerColor = MenuDefaults.groupVibrantContainerColor,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    modes.forEach { (mode, label, icon) ->
                        val isSelected = activeColorMode == mode
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onSelectColorMode(mode)
                                themeSubmenuExpanded = false
                                expanded = false
                            },
                            colors = MenuDefaults.itemVibrantColors(),
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }

            // Submenu 2: Page Layout
            Box {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Page Layout",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "Open Page Layout Submenu",
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = -90f }
                        )
                    },
                    onClick = {
                        themeSubmenuExpanded = false
                        layoutSubmenuExpanded = !layoutSubmenuExpanded
                    },
                    colors = MenuDefaults.itemVibrantColors(),
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                )

                DropdownMenu(
                    expanded = layoutSubmenuExpanded,
                    onDismissRequest = { layoutSubmenuExpanded = false },
                    containerColor = MenuDefaults.groupVibrantContainerColor,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    layouts.forEach { (layout, label, icon) ->
                        val isSelected = activeLayoutMode == layout
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onSelectLayoutMode(layout)
                                layoutSubmenuExpanded = false
                                expanded = false
                            },
                            colors = MenuDefaults.itemVibrantColors(),
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorModeConnectedButtonGroup(
    activeColorMode: PdfColorMode,
    onSelectColorMode: (PdfColorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        Triple(PdfColorMode.NORMAL, "Standard", Icons.Rounded.LightMode),
        Triple(PdfColorMode.INVERTED, "Dark", Icons.Rounded.InvertColors),
        Triple(PdfColorMode.NIGHT_SLATE, "Slate", Icons.Rounded.Bedtime)
    )

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier
    ) {
        modes.forEachIndexed { index, (mode, label, icon) ->
            val isChecked = activeColorMode == mode
            customItem(
                buttonGroupContent = {
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = { onSelectColorMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = if (isChecked) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        onClick = {
                            onSelectColorMode(mode)
                            menuState.dismiss()
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String,
    tooltipText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    toggleableShapes: IconToggleButtonShapes = IconButtonDefaults.toggleableShapes()
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState()
    ) {
        if (isSelected) {
            FilledTonalIconToggleButton(
                checked = true,
                onCheckedChange = { onClick() },
                enabled = enabled,
                shapes = toggleableShapes,
                modifier = modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            FilledTonalIconButton(
                onClick = onClick,
                enabled = enabled,
                shapes = shapes,
                modifier = modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
