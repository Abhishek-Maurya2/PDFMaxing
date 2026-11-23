package com.droid.pdfmaxing.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droid.pdfmaxing.ui.reader.SidebarTab
import com.droid.pdfmaxing.ui.theme.PdfColorMode
import java.util.Locale

@Composable
fun ThumbnailSidebar(
    activeTab: SidebarTab,
    totalPages: Int,
    currentPage: Int,
    documentTitle: String,
    fileSizeBytes: Long,
    colorMode: PdfColorMode,
    loadThumbnail: suspend (pageIndex: Int) -> Bitmap?,
    onSelectTab: (SidebarTab) -> Unit,
    onPageClick: (Int) -> Unit,
    onCloseSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalIconButton(
                    onClick = onCloseSidebar,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Sidebar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Material 3 Expressive Segmented Buttons for tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                SidebarTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SidebarTab.entries.size
                        ),
                        icon = {
                            val icon = when (tab) {
                                SidebarTab.THUMBNAILS -> Icons.Rounded.ViewModule
                                SidebarTab.OUTLINE -> Icons.AutoMirrored.Rounded.MenuBook
                                SidebarTab.INFO -> Icons.Rounded.Info
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    SidebarTab.THUMBNAILS -> {
                        ThumbnailGridView(
                            totalPages = totalPages,
                            currentPage = currentPage,
                            colorMode = colorMode,
                            loadThumbnail = loadThumbnail,
                            onPageClick = onPageClick
                        )
                    }
                    SidebarTab.OUTLINE -> {
                        OutlineEmptyStateView(
                            onSwitchToThumbnails = { onSelectTab(SidebarTab.THUMBNAILS) }
                        )
                    }
                    SidebarTab.INFO -> {
                        DocumentInfoView(
                            title = documentTitle,
                            totalPages = totalPages,
                            currentPage = currentPage,
                            fileSizeBytes = fileSizeBytes
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailGridView(
    totalPages: Int,
    currentPage: Int,
    colorMode: PdfColorMode,
    loadThumbnail: suspend (pageIndex: Int) -> Bitmap?,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(totalPages) { index ->
            val pageNumber = index + 1
            val isCurrent = pageNumber == currentPage

            ThumbnailItem(
                pageIndex = index,
                pageNumber = pageNumber,
                isCurrent = isCurrent,
                colorMode = colorMode,
                loadThumbnail = loadThumbnail,
                onClick = { onPageClick(pageNumber) }
            )
        }
    }
}

@Composable
fun ThumbnailItem(
    pageIndex: Int,
    pageNumber: Int,
    isCurrent: Boolean,
    colorMode: PdfColorMode,
    loadThumbnail: suspend (pageIndex: Int) -> Bitmap?,
    onClick: () -> Unit
) {
    var thumbnailBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }

    LaunchedEffect(pageIndex) {
        isLoading = true
        thumbnailBitmap = loadThumbnail(pageIndex)
        isLoading = false
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.772f)
                .then(
                    if (isCurrent) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (colorMode == PdfColorMode.NORMAL) Color.White else MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Thumbnail for Page $pageNumber",
                        contentScale = ContentScale.Fit,
                        colorFilter = colorMode.toColorFilter(),
                        filterQuality = FilterQuality.High,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Page number pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$pageNumber",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OutlineEmptyStateView(
    onSwitchToThumbnails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No Table of Contents",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This document does not contain an embedded outline. You can use page thumbnails to navigate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSwitchToThumbnails,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("View Thumbnails")
        }
    }
}

@Composable
fun DocumentInfoView(
    title: String,
    totalPages: Int,
    currentPage: Int,
    fileSizeBytes: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InfoRow(label = "File Name", value = title)
        InfoRow(label = "Total Pages", value = "$totalPages")
        InfoRow(label = "Current Page", value = "$currentPage")
        InfoRow(label = "Reading Progress", value = "${((currentPage.toFloat() / totalPages.toFloat().coerceAtLeast(1f)) * 100).toInt()}%")
        val sizeFormatted = formatBytes(fileSizeBytes)
        InfoRow(label = "File Size", value = sizeFormatted)
        InfoRow(label = "Security", value = "Storage Access Framework (Zero Permissions)")
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
