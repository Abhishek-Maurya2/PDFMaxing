package com.droid.pdfmaxing.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droid.pdfmaxing.data.settings.ThemeMode
import com.droid.pdfmaxing.ui.reader.PdfViewLayoutMode
import com.droid.pdfmaxing.ui.theme.AppColorPalette
import com.droid.pdfmaxing.ui.theme.EmphasizedTypography
import com.droid.pdfmaxing.ui.theme.PdfColorMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(onBack = onBack)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
            title = {
                Text(
                    text = "Reset Settings?",
                    style = EmphasizedTypography.titleLarge
                )
            },
            text = {
                Text(
                    text = "All appearance, reading, gesture, and accessibility preferences will be restored to their defaults.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = "Settings",
                        style = EmphasizedTypography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset to Defaults"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxContentWidth = when {
                maxWidth >= 1200.dp -> 960.dp
                maxWidth >= 840.dp -> 800.dp
                else -> maxWidth
            }
            val horizontalMargin = if (maxWidth >= 600.dp) 24.dp else 16.dp

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxSize()
                    .padding(horizontal = horizontalMargin),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // === 1. Appearance Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionHeader(
                            title = "Appearance",
                            icon = Icons.Rounded.Palette
                        )

                        // Theme Mode Selector Card
                        Card(
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
                                Text(
                                    text = "App Theme",
                                    style = EmphasizedTypography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                ThemeModeConnectedSelector(
                                    selectedMode = settings.themeMode,
                                    onSelectMode = { viewModel.setThemeMode(it) }
                                )
                            }
                        }

                        // Color Palette Swatches Card
                        Card(
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
                                Text(
                                    text = "Expressive Color Palette",
                                    style = EmphasizedTypography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                ColorPaletteSelectorRow(
                                    selectedPalette = settings.colorPalette,
                                    onSelectPalette = { viewModel.setColorPalette(it) }
                                )
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                            ) {
                                SwitchSettingItem(
                                    title = "Dynamic Color (Material You)",
                                    subtitle = "Adapt colors from your device wallpaper",
                                    icon = Icons.Rounded.AutoAwesome,
                                    checked = settings.useDynamicColor,
                                    onCheckedChange = { viewModel.setUseDynamicColor(it) },
                                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
                                )
                            }
                        }
                    }
                }

                // === 2. Reading Defaults Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionHeader(
                            title = "Reading Defaults",
                            icon = Icons.Rounded.AutoStories
                        )

                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Default Page Layout",
                                        style = EmphasizedTypography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    LayoutModeConnectedSelector(
                                        selectedMode = settings.defaultLayoutMode,
                                        onSelectMode = { viewModel.setDefaultLayoutMode(it) }
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Default Page Color Mode",
                                        style = EmphasizedTypography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    ColorModeSettingsSelector(
                                        selectedMode = settings.defaultColorMode,
                                        onSelectMode = { viewModel.setDefaultColorMode(it) }
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            SwitchSettingItem(
                                title = "Remember per-document settings",
                                subtitle = "Restore last page, zoom, layout, and color mode per PDF",
                                icon = Icons.Rounded.AutoStories,
                                checked = settings.rememberPerDocumentSettings,
                                onCheckedChange = { viewModel.setRememberPerDocumentSettings(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
                            )
                        }
                    }
                }

                // === 3. Gestures & Interaction Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            title = "Gestures & Interaction",
                            icon = Icons.Rounded.TouchApp
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            SwitchSettingItem(
                                title = "Double-tap to zoom",
                                subtitle = "Cycle between 100%, 200%, and 300% magnification",
                                icon = Icons.Rounded.ZoomIn,
                                checked = settings.doubleTapToZoom,
                                onCheckedChange = { viewModel.setDoubleTapToZoom(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3)
                            )
                            SwitchSettingItem(
                                title = "Tap to toggle immersive mode",
                                subtitle = "Hide top bar and floating toolbars while reading",
                                icon = Icons.Rounded.VisibilityOff,
                                checked = settings.tapToToggleImmersive,
                                onCheckedChange = { viewModel.setTapToToggleImmersive(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3)
                            )
                            SwitchSettingItem(
                                title = "Pinch-to-zoom",
                                subtitle = "Use two-finger pinch gesture on pages",
                                icon = Icons.Rounded.Pinch,
                                checked = settings.pinchToZoom,
                                onCheckedChange = { viewModel.setPinchToZoom(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3)
                            )
                        }
                    }
                }

                // === 4. Display & Workspace Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            title = "Display & Workspace",
                            icon = Icons.Rounded.Tab
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            SwitchSettingItem(
                                title = "Show tab bar with single tab",
                                subtitle = "Keep Chrome-style tab strip visible even with one PDF open",
                                icon = Icons.Rounded.Tab,
                                checked = settings.showTabBarOnSingleTab,
                                onCheckedChange = { viewModel.setShowTabBarOnSingleTab(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2)
                            )
                            SwitchSettingItem(
                                title = "Auto-open sidebar on large screens",
                                subtitle = "Show thumbnail navigation pane automatically on tablets & laptops",
                                icon = Icons.AutoMirrored.Rounded.ViewSidebar,
                                checked = settings.autoOpenSidebar,
                                onCheckedChange = { viewModel.setAutoOpenSidebar(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)
                            )
                        }
                    }
                }

                // === 5. Accessibility Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            title = "Accessibility",
                            icon = Icons.Rounded.AccessibilityNew
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            SwitchSettingItem(
                                title = "Reduce motion",
                                subtitle = "Use standard gentle transitions instead of expressive springs",
                                icon = Icons.Rounded.SlowMotionVideo,
                                checked = settings.reduceMotion,
                                onCheckedChange = { viewModel.setReduceMotion(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2)
                            )
                            SwitchSettingItem(
                                title = "High contrast",
                                subtitle = "Increase surface and outline contrast for readability",
                                icon = Icons.Rounded.Contrast,
                                checked = settings.highContrast,
                                onCheckedChange = { viewModel.setHighContrast(it) },
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)
                            )
                        }
                    }
                }

                // === 6. About Section ===
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            title = "About",
                            icon = Icons.Rounded.Info
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            InfoSettingItem(
                                title = "Version",
                                value = "1.0 • Material 3 Expressive",
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2)
                            )
                            InfoSettingItem(
                                title = "Architecture",
                                value = "Android SDK 37 • Compose BOM 2026.03.01 • Zero Storage Permissions",
                                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = EmphasizedTypography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeConnectedSelector(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit
) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Rounded.SettingsBrightness),
        Triple(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode)
    )

    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            val isChecked = selectedMode == mode
            customItem(
                buttonGroupContent = {
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = { onSelectMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        leadingIcon = { Icon(icon, contentDescription = null) },
                        onClick = {
                            onSelectMode(mode)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LayoutModeConnectedSelector(
    selectedMode: PdfViewLayoutMode,
    onSelectMode: (PdfViewLayoutMode) -> Unit
) {
    val options = listOf(
        Triple(PdfViewLayoutMode.FIT_100, "Fit 100%", Icons.Rounded.FitScreen),
        Triple(PdfViewLayoutMode.FIT_50, "50%", Icons.Rounded.ZoomOut),
        Triple(PdfViewLayoutMode.TWO_PAGE, "Spread", Icons.Rounded.AutoStories)
    )

    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            val isChecked = selectedMode == mode
            customItem(
                buttonGroupContent = {
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = { onSelectMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelectMode(mode)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorModeSettingsSelector(
    selectedMode: PdfColorMode,
    onSelectMode: (PdfColorMode) -> Unit
) {
    val options = listOf(
        Triple(PdfColorMode.NORMAL, "Standard", Icons.Rounded.LightMode),
        Triple(PdfColorMode.INVERTED, "Dark", Icons.Rounded.InvertColors),
        Triple(PdfColorMode.NIGHT_SLATE, "Slate", Icons.Rounded.Bedtime)
    )

    ButtonGroup(
        overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(menuState = it) },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            val isChecked = selectedMode == mode
            customItem(
                buttonGroupContent = {
                    val shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = { onSelectMode(mode) },
                        shapes = shapes,
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelectMode(mode)
                            menuState.dismiss()
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPaletteSelectorRow(
    selectedPalette: AppColorPalette,
    onSelectPalette: (AppColorPalette) -> Unit
) {
    val spatialDpSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val effectsColorSpec = MaterialTheme.motionScheme.defaultEffectsSpec<androidx.compose.ui.graphics.Color>()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppColorPalette.entries.forEach { palette ->
            if (palette == AppColorPalette.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return@forEach
            }
            val isSelected = selectedPalette == palette
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 14.dp else 24.dp,
                animationSpec = spatialDpSpec,
                label = "paletteShape"
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = effectsColorSpec,
                label = "paletteBg"
            )

            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .clickable { onSelectPalette(palette) }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.accentPreviewColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = palette.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ListItemShapes
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (checked) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InfoSettingItem(
    title: String,
    value: String,
    shapes: ListItemShapes
) {
    SegmentedListItem(
        onClick = {},
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        supportingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}
