package com.droid.pdfmaxing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlin.math.roundToInt

// Official Material 3 Expressive Connected Shapes for Horizontal Button Groups
// Outer corners use M3E 20.dp step; inner connecting corners use 4.dp
private val ConnectedLeadingShape = RoundedCornerShape(
    topStart = 20.dp,
    bottomStart = 20.dp,
    topEnd = 4.dp,
    bottomEnd = 4.dp
)

private val ConnectedMiddleShape = RoundedCornerShape(4.dp)

private val ConnectedTrailingShape = RoundedCornerShape(
    topStart = 4.dp,
    bottomStart = 4.dp,
    topEnd = 20.dp,
    bottomEnd = 20.dp
)

private val ConnectedSpaceBetween = 2.dp

/**
 * Material 3 Expressive Mobile Floating Toolbar with HorizontalFloatingToolbar,
 * filled connected button groups, and a springy floating page scrub pill.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MobileFloatingToolbar(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int,
    colorMode: PdfColorMode,
    layoutMode: PdfViewLayoutMode,
    isPageSliderPillOpen: Boolean,
    onTogglePageSliderPill: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageJump: (Int) -> Unit,
    onCycleLayoutMode: () -> Unit,
    onCycleColorMode: () -> Unit,
    onToggleSidebar: () -> Unit,
    isSearchOpen: Boolean = false,
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spatialOffsetSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val spatialScaleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // === Springy Expressive Scrub Slider Pill (Floats directly above toolbar) ===
        AnimatedVisibility(
            visible = isPageSliderPillOpen,
            enter = fadeIn(animationSpec = effectsSpec) +
                    slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spatialOffsetSpec
                    ) +
                    scaleIn(
                        initialScale = 0.85f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                        animationSpec = spatialScaleSpec
                    ),
            exit = fadeOut(animationSpec = effectsSpec) +
                   slideOutVertically(
                       targetOffsetY = { fullHeight -> fullHeight / 2 },
                       animationSpec = spatialOffsetSpec
                   ) +
                   scaleOut(
                       targetScale = 0.85f,
                       transformOrigin = TransformOrigin(0.5f, 1f),
                       animationSpec = spatialScaleSpec
                   )
        ) {
            MobilePageSliderPill(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageJump = onPageJump
            )
        }

        // === Official Material 3 Expressive Horizontal Floating Toolbar ===
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spatialOffsetSpec
            ) + scaleIn(
                initialScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spatialScaleSpec
            ) + fadeIn(animationSpec = effectsSpec),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spatialOffsetSpec
            ) + scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spatialScaleSpec
            ) + fadeOut(animationSpec = effectsSpec)
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Group 1: Page Indicator & Slider Toggle (prev/next hidden on compact mobile)
                FilledIconButton(
                    onClick = onTogglePageSliderPill,
                    shape = CircleShape,
                    colors = if (isPageSliderPillOpen) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        IconButtonDefaults.filledIconButtonColors()
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 64.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$currentPage",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "of $totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPageSliderPillOpen) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Centered Vertical Divider between groups
                VerticalDivider(
                    modifier = Modifier
                        .height(28.dp)
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                )

                // Group 2: View Controls Filled Connected Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    // Layout Mode
                    val layoutIcon = when (layoutMode) {
                        PdfViewLayoutMode.FIT_100 -> Icons.Rounded.FitScreen
                        PdfViewLayoutMode.FIT_50 -> Icons.Rounded.ZoomOut
                        PdfViewLayoutMode.TWO_PAGE -> Icons.Rounded.AutoStories
                    }
                    MobileConnectedButton(
                        icon = layoutIcon,
                        contentDescription = "Cycle Layout",
                        onClick = onCycleLayoutMode,
                        shape = ConnectedLeadingShape
                    )

                    // Color Mode
                    MobileConnectedButton(
                        icon = Icons.Rounded.InvertColors,
                        contentDescription = "Cycle Color Mode",
                        onClick = onCycleColorMode,
                        shape = ConnectedMiddleShape
                    )

                    // Sidebar / Thumbnails
                    MobileConnectedButton(
                        icon = Icons.AutoMirrored.Rounded.ViewSidebar,
                        contentDescription = "Toggle Sidebar",
                        onClick = onToggleSidebar,
                        shape = ConnectedMiddleShape
                    )

                    // Search Toggle
                    MobileConnectedButton(
                        icon = if (isSearchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (isSearchOpen) "Close Search" else "Search in PDF",
                        onClick = onToggleSearch,
                        shape = ConnectedTrailingShape
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileConnectedButton(
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
            .size(48.dp)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MobilePageSliderPill(
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
        ) {
            val selectedPage = sliderState.value.roundToInt().coerceIn(1, totalPages)
            Text(
                text = "$selectedPage / $totalPages",
                style = MaterialTheme.typography.labelLarge,
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
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}
