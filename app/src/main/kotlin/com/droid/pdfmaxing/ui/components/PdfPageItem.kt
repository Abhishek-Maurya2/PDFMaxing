package com.droid.pdfmaxing.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    searchMatches: List<PdfSearchMatch> = emptyList(),
    isCurrentMatchPage: Boolean = false,
    activeMatchIndexInPage: Int? = null
) {
    // Retain previous pageBitmap across zoom changes so zooming is visually seamless without flickering
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }
    val density = LocalDensity.current

    LaunchedEffect(pageIndex, scaleFactor, pageWidth) {
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

    // Default US Letter aspect ratio 612 / 792 = ~0.772
    val pageAspectRatio = remember(pageBitmap) {
        val b = pageBitmap
        if (b != null && b.height > 0) {
            b.width.toFloat() / b.height.toFloat()
        } else {
            0.772f
        }
    }

    Card(
        modifier = modifier
            .width(pageWidth)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .aspectRatio(pageAspectRatio)
            .semantics {
                contentDescription = "PDF Page ${pageIndex + 1} of $totalPages"
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerHoverIcon(PointerIcon.Default),
        shape = RoundedCornerShape(8.dp),
        border = if (isCurrentMatchPage) {
            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        } else if (searchMatches.isNotEmpty()) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentMatchPage) 6.dp else 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colorMode == PdfColorMode.NORMAL) Color.White else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (pageBitmap != null) {
                Image(
                    bitmap = pageBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    colorFilter = colorMode.toColorFilter(),
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )

                // Render match highlight boxes if bounding boxes are available
                if (searchMatches.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val b = pageBitmap ?: return@Canvas
                        if (b.width <= 0 || b.height <= 0) return@Canvas
                        val scaleX = size.width / b.width.toFloat()
                        val scaleY = size.height / b.height.toFloat()

                        for (match in searchMatches) {
                            val isCurrent = isCurrentMatchPage && activeMatchIndexInPage == match.matchIndexInPage
                            val fillColor = if (isCurrent) {
                                Color(0xFFFF9800).copy(alpha = 0.55f)
                            } else {
                                Color(0xFFFFEB3B).copy(alpha = 0.40f)
                            }

                            for (rect in match.bounds) {
                                drawRect(
                                    color = fillColor,
                                    topLeft = Offset(rect.left * scaleX, rect.top * scaleY),
                                    size = Size(
                                        (rect.right - rect.left).coerceAtLeast(10f) * scaleX,
                                        (rect.bottom - rect.top).coerceAtLeast(10f) * scaleY
                                    )
                                )
                            }
                        }
                    }

                    // Floating badge indicating search matches on this page
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentMatchPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${searchMatches.size} match${if (searchMatches.size > 1) "es" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurrentMatchPage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Page $pageNumber",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
