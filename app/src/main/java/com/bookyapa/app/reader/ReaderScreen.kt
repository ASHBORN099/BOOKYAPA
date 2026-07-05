package com.bookyapa.app.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bookyapa.app.ui.theme.ThemeManager
import com.bookyapa.app.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    navController: NavController,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by ThemeManager.getThemeMode(context).collectAsState(ThemeMode.DARK)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val density = LocalDensity.current
    var barsVisible by remember { mutableStateOf(false) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var goToPageInput by remember { mutableStateOf("") }

    LaunchedEffect(barsVisible) {
        if (barsVisible) {
            delay(3000)
            barsVisible = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    itemsIndexed(state.chapters) { index, chapter ->
                        val isCurrent = index == state.currentChapterIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateToChapter(index)
                                    scope.launch { drawerState.close() }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (chapter.isRead) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Read",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            var pages by remember { mutableStateOf<List<String>>(emptyList()) }
            var pagerState by remember { mutableStateOf<androidx.compose.foundation.pager.PagerState?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // Layer 1: Reading content (always visible)
                Column(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.isLoadingChapters || state.isLoadingContent -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (state.isLoadingChapters) "Loading..." else "Downloading chapter...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        state.error != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.error ?: "Unknown error",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(onClick = viewModel::loadCurrentChapterContent) {
                                        Text("Retry", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        else -> {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                            ) {
                                val maxWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
                                val maxHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)

                                val horizontalPaddingPx = with(density) { 40.dp.toPx() }.toInt()
                                val verticalPaddingPx = with(density) { 32.dp.toPx() }.toInt()

                                val style = TextStyle(
                                    fontSize = state.fontSize.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )

                                val textMeasurer = rememberTextMeasurer()

                                val computedPages = remember(state.content, state.fontSize, maxWidthPx, maxHeightPx) {
                                    Paginator.computePages(
                                        textMeasurer = textMeasurer,
                                        text = state.content,
                                        style = style,
                                        constraints = Constraints(
                                            maxWidth = (maxWidthPx - horizontalPaddingPx).coerceAtLeast(1),
                                            maxHeight = (maxHeightPx - verticalPaddingPx).coerceAtLeast(1),
                                        ),
                                    )
                                }

                                LaunchedEffect(computedPages) {
                                    pages = computedPages
                                    viewModel.setPages(computedPages)
                                }

                                val ps = rememberPagerState(
                                    initialPage = state.currentPage,
                                    pageCount = { computedPages.size },
                                )

                                LaunchedEffect(ps) {
                                    pagerState = ps
                                }

                                LaunchedEffect(ps.currentPage) {
                                    if (ps.currentPage != state.currentPage) {
                                        viewModel.goToPage(ps.currentPage)
                                    }
                                }

                                LaunchedEffect(state.currentPage) {
                                    if (ps.currentPage != state.currentPage) {
                                        ps.scrollToPage(state.currentPage)
                                    }
                                }

                                if (computedPages.isNotEmpty()) {
                                    HorizontalPager(
                                        state = ps,
                                        modifier = Modifier.fillMaxSize(),
                                    ) { pageIndex ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                                .pointerInput(Unit) {
                                                    detectTapGestures(onDoubleTap = {
                                                        barsVisible = !barsVisible
                                                    })
                                                },
                                        ) {
                                            Text(
                                                text = computedPages.getOrElse(pageIndex) { "" },
                                                style = style,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Layer 2: Top bar overlay (fades in/out)
                AnimatedVisibility(
                    visible = barsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.currentChapterTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Chapter list",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.toggleBookmark() }) {
                                Icon(
                                    imageVector = if (state.isBookmarked) Icons.Default.Bookmark
                                                  else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (state.isBookmarked) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    ThemeManager.setThemeMode(context, ThemeManager.nextMode(themeMode))
                                }
                            }) {
                                val icon = when (themeMode) {
                                    ThemeMode.DARK -> Icons.Default.BrightnessLow
                                    ThemeMode.LIGHT -> Icons.Default.BrightnessHigh
                                    ThemeMode.SEPIA -> Icons.Default.Palette
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Theme",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.updateFontSize(-2) }) {
                                Icon(
                                    imageVector = Icons.Default.TextDecrease,
                                    contentDescription = "Decrease font",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.updateFontSize(2) }) {
                                Icon(
                                    imageVector = Icons.Default.TextIncrease,
                                    contentDescription = "Increase font",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }

                // Layer 3: Bottom controls overlay
                if (barsVisible) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        if (state.pages.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { (state.currentPage + 1).toFloat() / state.pages.size },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                        Text(
                            text = if (state.pages.isNotEmpty())
                                "Page ${state.currentPage + 1} / ${state.pages.size}"
                            else if (state.chapters.isNotEmpty())
                                "${state.currentChapterIndex + 1} / ${state.chapters.size}"
                            else
                                "- / -",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = state.pages.isNotEmpty()) {
                                    goToPageInput = ""
                                    showGoToPageDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (showGoToPageDialog) {
                    AlertDialog(
                        onDismissRequest = { showGoToPageDialog = false },
                        title = { Text("Go to Page") },
                        text = {
                            OutlinedTextField(
                                value = goToPageInput,
                                onValueChange = { goToPageInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Page number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val page = goToPageInput.toIntOrNull()
                                if (page != null && page in 1..state.pages.size) {
                                    viewModel.goToPage(page - 1)
                                }
                                showGoToPageDialog = false
                            }) { Text("Go", color = MaterialTheme.colorScheme.primary) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGoToPageDialog = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
