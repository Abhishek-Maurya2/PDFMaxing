package com.droid.pdfmaxing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Official Material 3 Expressive Connected Shapes for Vertical Button Groups:
// Outer corners are fully rounded (22.dp) and inner connecting corners are 4.dp
private val ConnectedTopShape = RoundedCornerShape(
    topStart = 22.dp,
    topEnd = 22.dp,
    bottomStart = 4.dp,
    bottomEnd = 4.dp
)

private val ConnectedMiddleShape = RoundedCornerShape(4.dp)

private val ConnectedBottomShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomStart = 22.dp,
    bottomEnd = 22.dp
)

// Official M3 Expressive Connected Button Group space between adjacent buttons
private val ConnectedSpaceBetween = 2.dp

/**
 * Material 3 Expressive Vertical Floating Toolbar hosting filled connected button groups
 * for Zoom controls and Page Navigation controls, alongside an expressive springy slider pill
 * for live page scrubbing, and an attached FAB for real-time PDF search with an expressive popup pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveVerticalFloatingToolbar(
    currentPage: Int,
    totalPages: Int,
    zoomLevel: Float,
    isPageSliderPillOpen: Boolean,
    onTogglePageSliderPill: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageJump: (Int) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit = {},
    // Search controls
    isSearchOpen: Boolean = false,
    onToggleSearch: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    currentMatchIndex: Int = 0,
    totalMatches: Int = 0,
    isSearching: Boolean = false,
    onPreviousMatch: () -> Unit = {},
    onNextMatch: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === 1. Springy Expressive Search Pop-up Pill ===
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = TransformOrigin(1f, 0.5f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                   slideOutHorizontally(
                       targetOffsetX = { fullWidth -> fullWidth / 2 },
                       animationSpec = spring(
                           dampingRatio = Spring.DampingRatioNoBouncy,
                           stiffness = Spring.StiffnessMedium
                       )
                   ) +
                   scaleOut(
                       targetScale = 0.8f,
                       transformOrigin = TransformOrigin(1f, 0.5f),
                       animationSpec = spring(
                           dampingRatio = Spring.DampingRatioNoBouncy,
                           stiffness = Spring.StiffnessMedium
                       )
                   ),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            SearchPill(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                currentMatchIndex = currentMatchIndex,
                totalMatches = totalMatches,
                isSearching = isSearching,
                onPreviousMatch = onPreviousMatch,
                onNextMatch = onNextMatch,
                onCloseSearch = onCloseSearch
            )
        }

        // === 2. Springy Expressive Slider & Page Number Pill (when search is not active) ===
        AnimatedVisibility(
            visible = isPageSliderPillOpen && !isSearchOpen,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = TransformOrigin(1f, 0.5f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                   slideOutHorizontally(
                       targetOffsetX = { fullWidth -> fullWidth / 2 },
                       animationSpec = spring(
                           dampingRatio = Spring.DampingRatioNoBouncy,
                           stiffness = Spring.StiffnessMedium
                       )
                   ) +
                   scaleOut(
                       targetScale = 0.8f,
                       transformOrigin = TransformOrigin(1f, 0.5f),
                       animationSpec = spring(
                           dampingRatio = Spring.DampingRatioNoBouncy,
                           stiffness = Spring.StiffnessMedium
                       )
                   ),
            modifier = Modifier.padding(bottom = 44.dp)
        ) {
            PageSliderPill(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageJump = onPageJump
            )
        }

        // === 3. Vertical Floating Toolbar with Attached FAB ===
        VerticalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onToggleSearch,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (isSearchOpen) "Close Search" else "Search in PDF"
                    )
                }
            },
            floatingActionButtonPosition = FloatingToolbarVerticalFabPosition.Bottom,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        ) {
            // === Zoom Controls: Filled Connected Button Group (+ and -) ===
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ConnectedSpaceBetween),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                // Zoom In (Leading / Top Button)
                ConnectedIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Zoom In",
                    onClick = onZoomIn,
                    enabled = zoomLevel < 5.0f,
                    shape = ConnectedTopShape
                )

                // Zoom Out (Trailing / Bottom Button)
                ConnectedIconButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Zoom Out",
                    onClick = onZoomOut,
                    enabled = zoomLevel > 0.5f,
                    shape = ConnectedBottomShape
                )
            }

            // Divider between Zoom and Page Navigation controls
            HorizontalDivider(
                modifier = Modifier
                    .width(28.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
            )

            // === Page Navigation: Filled Connected Button Group ===
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ConnectedSpaceBetween),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                // Previous Page (Leading / Top Button)
                ConnectedIconButton(
                    icon = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Previous Page",
                    onClick = onPreviousPage,
                    enabled = currentPage > 1,
                    shape = ConnectedTopShape
                )

                // Page Indicator Button (Toggles the slider pill with springy motion!)
                FilledIconButton(
                    onClick = onTogglePageSliderPill,
                    shape = ConnectedMiddleShape,
                    colors = if (isPageSliderPillOpen) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        IconButtonDefaults.filledIconButtonColors()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$currentPage",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 12.sp,
                                fontSize = 12.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 10.sp,
                                fontSize = 9.sp
                            ),
                            color = if (isPageSliderPillOpen) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Next Page (Trailing / Bottom Button)
                ConnectedIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Next Page",
                    onClick = onNextPage,
                    enabled = currentPage < totalPages,
                    shape = ConnectedBottomShape
                )
            }
        }
    }
}

/**
 * Expressive Search Pop-up Pill featuring live search text input, real-time match counters,
 * previous/next match navigation, and clear/dismiss actions.
 */
@Composable
fun SearchPill(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentMatchIndex: Int,
    totalMatches: Int,
    isSearching: Boolean,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // Search Icon
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            // Text Input Field with keyboard actions for Enter and Shift+Enter
            Box(
                modifier = Modifier.width(180.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Find in document...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onNextMatch() }),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                if (event.isShiftPressed) {
                                    onPreviousMatch()
                                } else {
                                    onNextMatch()
                                }
                                true
                            } else if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                                onCloseSearch()
                                true
                            } else {
                                false
                            }
                        }
                )
            }

            // Clear Query Button (when search text is present)
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier
                        .size(28.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Real-time Match Count & Status Badge
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (searchQuery.isNotBlank()) {
                if (totalMatches > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${currentMatchIndex + 1} of $totalMatches",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "0 matches",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Match Navigation Buttons (Up & Down chevrons)
            if (totalMatches > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onPreviousMatch,
                        modifier = Modifier
                            .size(30.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Previous Match",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextMatch,
                        modifier = Modifier
                            .size(30.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Next Match",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Close Search Button
            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier
                    .size(30.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Expressive Pill Container in primaryContainer color featuring the page number and a smooth slider.
 */
@Composable
private fun PageSliderPill(
    currentPage: Int,
    totalPages: Int,
    onPageJump: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxPageFloat = totalPages.toFloat().coerceAtLeast(1f)
    val sliderState = remember(totalPages) {
        SliderState(
            currentPage.toFloat().coerceIn(1f, maxPageFloat),
            0,
            1f..maxPageFloat
        )
    }

    // Keep slider in sync with external page changes only when the user is not actively dragging
    LaunchedEffect(currentPage) {
        if (!sliderState.isDragging) {
            sliderState.value = currentPage.toFloat().coerceIn(1f, maxPageFloat)
        }
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
        ) {
            val selectedPage = sliderState.value.roundToInt().coerceIn(1, totalPages)
            Text(
                text = "Page $selectedPage of $totalPages",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (totalPages > 1) {
                Slider(
                    state = sliderState,
                    onValueChange = { newValue ->
                        sliderState.value = newValue
                    },
                    onValueChangeFinished = {
                        val target = sliderState.value.roundToInt().coerceIn(1, totalPages)
                        onPageJump(target)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.width(180.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    shape: Shape,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(),
        modifier = modifier
            .size(44.dp)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}
