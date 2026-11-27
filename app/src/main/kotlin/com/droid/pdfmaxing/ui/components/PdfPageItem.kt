package com.droid.pdfmaxing.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import com.droid.pdfmaxing.data.pdf.PageSelectionResult
import com.droid.pdfmaxing.data.pdf.PdfTextSegment
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droid.pdfmaxing.data.pdf.PdfSearchMatch
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PdfPageItem(
    pageIndex: Int,
    totalPages: Int,
    pageWidth: Dp,
    scaleFactor: Float,
    colorMode: PdfColorMode,
    loadBitmap: suspend (pageIndex: Int, targetWidth: Int, scaleFactor: Float) -> Bitmap?,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    documentId: String = "",
    searchMatches: List<PdfSearchMatch> = emptyList(),
    isCurrentMatchPage: Boolean = false,
    activeMatchIndexInPage: Int? = null,
    loadTextSegments: (suspend (pageIndex: Int) -> List<PdfTextSegment>)? = null,
    onSelectNativeContent: (suspend (pageIndex: Int, startX: Float, startY: Float, stopX: Float, stopY: Float) -> PageSelectionResult?)? = null,
    onSearchSelectedText: ((String) -> Unit)? = null
) {
    // Retain previous pageBitmap across zoom changes so zooming is visually seamless without flickering.
    // DocumentId key ensures bitmap resets when switching tabs.
    var pageBitmap by remember(documentId, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(documentId, pageIndex) { mutableStateOf(true) }
    val density = LocalDensity.current

    // Text selection & cursor states
    var textSegments by remember(documentId, pageIndex) { mutableStateOf<List<PdfTextSegment>>(emptyList()) }
    var isHoveringText by remember { mutableStateOf(false) }
    var selectionBounds by remember { mutableStateOf<List<RectF>>(emptyList()) }
    var selectedText by remember { mutableStateOf("") }
    var isDraggingSelection by remember { mutableStateOf(false) }
    var dragStartOffset by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentOffset by remember { mutableStateOf<Offset?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(documentId, pageIndex) {
        if (loadTextSegments != null) {
            textSegments = loadTextSegments(pageIndex)
        }
    }

    LaunchedEffect(documentId, pageIndex, scaleFactor, pageWidth) {
        if (pageBitmap == null) {
            isLoading = true
        } else {
            // Debounce zoom gesture updates slightly so we do not spam PDFium rendering on each micro-frame
            delay(120)
        }
        val targetWidthPx = with(density) {
            ((pageWidth - 16.dp).toPx()).roundToInt().coerceAtLeast(300)
        }
        val bitmap = loadBitmap(pageIndex, targetWidthPx, scaleFactor)
        if (bitmap != null) {
            pageBitmap = bitmap
        }
        isLoading = false
    }

    val updateSelectionForOffsets = { start: Offset, current: Offset, size: Size ->
        if (size.width > 0f && size.height > 0f) {
            val normStartX = (start.x / size.width).coerceIn(0f, 1f)
            val normStartY = (start.y / size.height).coerceIn(0f, 1f)
            val normCurrX = (current.x / size.width).coerceIn(0f, 1f)
            val normCurrY = (current.y / size.height).coerceIn(0f, 1f)

            val minX = minOf(normStartX, normCurrX)
            val maxX = maxOf(normStartX, normCurrX)
            val minY = minOf(normStartY, normCurrY)
            val maxY = maxOf(normStartY, normCurrY)

            if (onSelectNativeContent != null) {
                coroutineScope.launch {
                    val result = onSelectNativeContent(pageIndex, normStartX, normStartY, normCurrX, normCurrY)
                    if (result != null && result.text.isNotBlank()) {
                        selectedText = result.text
                        selectionBounds = result.normalizedBounds
                    } else {
                        // Fallback: segment hit-test
                        val hitSegments = textSegments.filter { seg ->
                            seg.bounds.any { r ->
                                r.intersects(minX, minY, maxX, maxY)
                            }
                        }
                        selectedText = hitSegments.joinToString(" ") { it.text }
                        selectionBounds = hitSegments.flatMap { it.bounds }
                    }
                }
            } else {
                val hitSegments = textSegments.filter { seg ->
                    seg.bounds.any { r ->
                        r.intersects(minX, minY, maxX, maxY)
                    }
                }
                selectedText = hitSegments.joinToString(" ") { it.text }
                selectionBounds = hitSegments.flatMap { it.bounds }
            }
        }
    }
    val currentBitmap = pageBitmap
    val pageAspectRatio = remember(currentBitmap) {
        if (currentBitmap != null && currentBitmap.height > 0) {
            currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
        } else {
            0.772f
        }
    }

    var componentSize by remember { mutableStateOf(Size.Zero) }

    Card(
        modifier = modifier
            .width(pageWidth)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .aspectRatio(pageAspectRatio)
            .semantics {
                contentDescription = "PDF Page ${pageIndex + 1} of $totalPages"
            }
            .pointerInput(textSegments) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pos = event.changes.firstOrNull()?.position ?: continue
                        if (componentSize.width > 0f && componentSize.height > 0f) {
                            val normX = pos.x / componentSize.width
                            val normY = pos.y / componentSize.height
                            val hitsText = textSegments.any { seg ->
                                seg.bounds.any { r ->
                                    normX >= r.left && normX <= r.right && normY >= r.top && normY <= r.bottom
                                }
                            }
                            isHoveringText = hitsText
                        }
                    }
                }
            }
            .pointerInput(textSegments, componentSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartOffset = offset
                        dragCurrentOffset = offset
                        isDraggingSelection = true
                        updateSelectionForOffsets(offset, offset, componentSize)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragCurrentOffset = change.position
                        val start = dragStartOffset
                        if (start != null) {
                            updateSelectionForOffsets(start, change.position, componentSize)
                        }
                    },
                    onDragEnd = {
                        isDraggingSelection = false
                    },
                    onDragCancel = {
                        isDraggingSelection = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (selectedText.isNotEmpty()) {
                            selectedText = ""
                            selectionBounds = emptyList()
                        } else {
                            onTap()
                        }
                    },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerHoverIcon(if (isHoveringText) PointerIcon.Text else PointerIcon.Default),
        shape = RoundedCornerShape(8.dp),
        border = if (isCurrentMatchPage) {
            BorderStroke(2.5.dp, MaterialTheme.colorScheme.tertiary)
        } else if (searchMatches.isNotEmpty()) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMatchPage) 6.dp else 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colorMode == PdfColorMode.NORMAL) Color.White else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    componentSize = Size(it.width.toFloat(), it.height.toFloat())
                },
            contentAlignment = Alignment.Center
        ) {
            val bitmapToDraw = pageBitmap
            if (bitmapToDraw != null) {
                Image(
                    bitmap = bitmapToDraw.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    colorFilter = colorMode.toColorFilter(),
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )

                // Render active drag/text selection highlights
                if (selectionBounds.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val selectionColor = Color(0xFF2196F3).copy(alpha = 0.38f)
                        val selectionBorderColor = Color(0xFF1976D2).copy(alpha = 0.75f)

                        for (rect in selectionBounds) {
                            val left = rect.left * size.width
                            val top = rect.top * size.height
                            val width = (rect.right - rect.left) * size.width
                            val height = (rect.bottom - rect.top) * size.height

                            if (width > 1f && height > 1f) {
                                drawRoundRect(
                                    color = selectionColor,
                                    topLeft = Offset(left, top),
                                    size = Size(width, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                                drawRoundRect(
                                    color = selectionBorderColor,
                                    topLeft = Offset(left, top),
                                    size = Size(width, height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // Floating "Copy / Find" flyout positioned near the selected text
                val flyoutTopPx = if (selectionBounds.isNotEmpty() && componentSize.height > 0f) {
                    val minTop = selectionBounds.minOf { it.top } * componentSize.height
                    val maxBottom = selectionBounds.maxOf { it.bottom } * componentSize.height
                    val preferredAbove = minTop - with(density) { 52.dp.toPx() }
                    if (preferredAbove >= 8f) {
                        preferredAbove.roundToInt()
                    } else {
                        (maxBottom + with(density) { 8.dp.toPx() }).coerceAtMost((componentSize.height - with(density) { 48.dp.toPx() }).coerceAtLeast(8f)).roundToInt()
                    }
                } else {
                    with(density) { 16.dp.toPx() }.roundToInt()
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedText.isNotBlank() && !isDraggingSelection,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, flyoutTopPx) }
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(selectedText))
                                    selectedText = ""
                                    selectionBounds = emptyList()
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                            if (onSearchSelectedText != null) {
                                FilledTonalButton(
                                    onClick = {
                                        val query = selectedText.trim()
                                        selectedText = ""
                                        selectionBounds = emptyList()
                                        if (query.isNotEmpty()) {
                                            onSearchSelectedText(query)
                                        }
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Find", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedText = ""
                                    selectionBounds = emptyList()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear selection",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Render match highlight boxes if bounding boxes are available
                if (searchMatches.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val b = bitmapToDraw
                        if (b.width <= 0 || b.height <= 0) return@Canvas

                        for (match in searchMatches) {
                            val isCurrent = isCurrentMatchPage && activeMatchIndexInPage == match.matchIndexInPage
                            val fillColor = if (isCurrent) {
                                Color(0xFFFF9800).copy(alpha = 0.60f)
                            } else {
                                Color(0xFFFFEB3B).copy(alpha = 0.45f)
                            }

                            for (rect in match.bounds) {
                                // Support normalized [0..1] coordinates, with fallback for un-normalized bitmap pixels
                                val (drawX, drawY, drawW, drawH) = if (rect.right <= 1.05f && rect.bottom <= 1.05f) {
                                    val left = rect.left * size.width
                                    val top = rect.top * size.height
                                    val width = (rect.right - rect.left) * size.width
                                    val height = (rect.bottom - rect.top) * size.height
                                    listOf(left, top, width, height)
                                } else {
                                    val scaleX = size.width / b.width.toFloat()
                                    val scaleY = size.height / b.height.toFloat()
                                    val left = rect.left * scaleX
                                    val top = rect.top * scaleY
                                    val width = (rect.right - rect.left) * scaleX
                                    val height = (rect.bottom - rect.top) * scaleY
                                    listOf(left, top, width, height)
                                }

                                // Avoid drawing zero-size or stray whitespace highlights
                                if (drawW > 1f && drawH > 1f) {
                                    val padX = 2.dp.toPx()
                                    val padY = 1.dp.toPx()
                                    drawRoundRect(
                                        color = fillColor,
                                        topLeft = Offset(
                                            (drawX - padX).coerceAtLeast(0f),
                                            (drawY - padY).coerceAtLeast(0f)
                                        ),
                                        size = Size(
                                            (drawW + padX * 2).coerceAtMost(size.width - drawX + padX),
                                            (drawH + padY * 2).coerceAtMost(size.height - drawY + padY)
                                        ),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // Floating badge indicating search matches on this page
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentMatchPage) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${searchMatches.size} match${if (searchMatches.size > 1) "es" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurrentMatchPage) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            } else if (isLoading) {
                // Shimmer Loading Placeholder
                ShimmerPagePlaceholder(
                    pageNumber = pageIndex + 1,
                    colorMode = colorMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShimmerPagePlaceholder(
    pageNumber: Int,
    colorMode: PdfColorMode,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = if (colorMode == PdfColorMode.NORMAL) {
        listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.7f),
            Color.LightGray.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            Color.DarkGray.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.7f),
            Color.DarkGray.copy(alpha = 0.3f)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "shimmerEntranceScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LoadingIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Loading Page $pageNumber...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
