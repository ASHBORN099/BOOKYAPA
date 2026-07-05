package com.bookyapa.app.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.model.BookStatus
import com.bookyapa.app.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContainerScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<BookEntity?>(null) }

    val epubPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importEpub(it, context) }
    }

    LaunchedEffect(state.importMessage) {
        state.importMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val tabs = listOf("All", "Reading", "Planned", "Done", "Dropped")
    val tabFilters = listOf(
        null,
        BookStatus.READING,
        BookStatus.PLAN_TO_READ,
        BookStatus.COMPLETED,
        BookStatus.DROPPED,
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    TextButton(
                        onClick = { epubPickerLauncher.launch(arrayOf("application/epub+zip")) },
                        enabled = !state.isImporting,
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF008080),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Import EPUB", color = Color(0xFF008080), fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color(0xFFE0E0E0),
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color(0xFFE0E0E0),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF008080),
                    )
                },
                divider = {},
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color(0xFF008080) else AppColors.bodyText,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                LibraryScreen(
                    selectedFilter = tabFilters[page],
                    allBooks = state.allBooks,
                    isLoading = state.isLoading,
                    onDelete = { showDeleteDialog = it },
                    onTap = { book ->
                        navController.navigate(
                            com.bookyapa.app.navigation.Screen.BookDetail.createRoute(book.id)
                        )
                    },
                )
            }
        }

        showDeleteDialog?.let { book ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Remove from Library") },
                text = { Text("Remove \"${book.title}\" from your library?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteBook(book.id)
                        showDeleteDialog = null
                    }) { Text("Remove", color = Color(0xFFCF6679)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel", color = AppColors.bodyText)
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color(0xFFE0E0E0),
                textContentColor = Color(0xFFE0E0E0),
            )
        }
    }
}
