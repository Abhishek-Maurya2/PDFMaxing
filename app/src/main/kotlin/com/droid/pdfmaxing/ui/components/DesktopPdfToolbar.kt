package com.droid.pdfmaxing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureAsPdf
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
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
    onOpenFileClick: (() -> Unit)? = null
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isFullscreen = statusBarTop > 0.dp
    val effectiveStartPadding = if (isFullscreen) 170.dp else startPadding

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Push toolbar content down below system status bar in fullscreen mode
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(start = effectiveStartPadding, end = endPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
            // Left Group: Sidebar Toggle (Back button removed; tab system handles navigation)
            FilledTonalIconToggleButton(
                checked = isSidebarOpen,
                onCheckedChange = { onToggleSidebar() },
                shape = RoundedCornerShape(12.dp),
                colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(width = 40.dp, height = 36.dp)
                    .systemGestureExclusion()
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ViewSidebar,
                    contentDescription = if (isSidebarOpen) "Hide Thumbnails" else "Show Thumbnails",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center Group: Browser-Style Tabs & "+" New Tab Button (Unified with Titlebar)
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    tabs.forEach { tab ->
                        val isActive = tab.id == activeTabId
                        PdfTabItem(
                            tab = tab,
                            isActive = isActive,
                            onClick = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) },
                            modifier = Modifier.systemGestureExclusion()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // New Tab "+" Button
                    FilledTonalIconButton(
                        onClick = onNewTabClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .systemGestureExclusion()
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Tab",
                            modifier = Modifier.size(18.dp)
                        )
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
                        tint = MaterialTheme.colorScheme.error,
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

            // Right Group: Material 3 Expressive Vibrant Theme Menu (Split button removed)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .systemGestureExclusion()
                    .padding(start = 6.dp)
            ) {
                // Official Material 3 Expressive Vibrant Menu for Theme Switcher (Opened via "..." Button)
                ThemeVibrantMenu(
                    activeColorMode = colorMode,
                    onSelectColorMode = onSelectColorMode
                )
            }
        }
    }
}
}

/**
 * Official Material 3 Expressive Split Button for View Toggle (Fit 100%, 50% Overview, Two-Page Spread).
 *
 * Built using official androidx.compose.material3.SplitButtonLayout with TonalLeadingButton and TonalTrailingButton.
 */
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

        // Dropdown Menu displaying all layout choices
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

/**
 * Official Material 3 Expressive Vibrant Menu for Theme / Colour Mode switching.
 * Triggered by the "..." (More options) button in the topbar.
 *
 * Implements M3 Expressive Vibrant Menu design tokens:
 * - Trigger button: ExpressiveIconButton with Icons.Rounded.MoreHoriz ("...")
 * - Menu Container: DropdownMenu with MenuDefaults.groupVibrantContainerColor and 20.dp rounded corners
 * - Menu Items: Styled with MenuDefaults.itemVibrantColors()
 * - Options:
 *     1. Standard (Original page colors, Icons.Rounded.LightMode)
 *     2. Dark (High-contrast negative page inversion, Icons.Rounded.InvertColors)
 *     3. Slate (Soft night dark mode for eye comfort, Icons.Rounded.Bedtime)
 * - Highlights active selection with Check icon, container tint, and bold typography.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeVibrantMenu(
    activeColorMode: PdfColorMode,
    onSelectColorMode: (PdfColorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val modes = listOf(
        Triple(PdfColorMode.NORMAL, "Standard", Icons.Rounded.LightMode),
        Triple(PdfColorMode.INVERTED, "Dark", Icons.Rounded.InvertColors),
        Triple(PdfColorMode.NIGHT_SLATE, "Slate", Icons.Rounded.Bedtime)
    )

    Box(modifier = modifier.systemGestureExclusion()) {
        FilledTonalIconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .systemGestureExclusion()
                .pointerHoverIcon(PointerIcon.Hand),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Theme Options",
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.groupVibrantContainerColor,
            shape = RoundedCornerShape(20.dp),
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
                                    shape = RoundedCornerShape(12.dp)
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

/**
 * Official Material 3 Expressive Filled Connected Button Group for Theme / Colour Inversion Modes:
 * - Standard (Original page colors)
 * - Dark (High-contrast negative page inversion)
 * - Night Slate (Soft night dark mode for eye comfort)
 *
 * Implements M3 Expressive filled connected button group:
 * - Spacing: ButtonGroupDefaults.ConnectedSpaceBetween (2.dp)
 * - Shapes: connectedLeadingButtonShapes, connectedMiddleButtonShapes, connectedTrailingButtonShapes
 * - Filled ToggleButtons with active primary container styling
 */
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

/**
 * Official Material 3 Expressive Icon Button using official IconButtonDefaults.shapes()
 * and IconButtonDefaults.toggleableShapes() for dynamic shape morphing on interaction.
 */
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
