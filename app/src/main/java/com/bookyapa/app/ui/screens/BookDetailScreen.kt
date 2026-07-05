package com.bookyapa.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bookyapa.app.data.model.BookStatus
import com.bookyapa.app.navigation.Screen
import com.bookyapa.app.network.CloudflareVerifyActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    navController: NavController,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showRemoveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val turnstileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (state.turnstileUrl != null) {
            viewModel.retryAfterTurnstile()
        }
    }

    LaunchedEffect(state.addedToLibrary) {
        if (state.addedToLibrary) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.isRemovedFromLibrary) {
        if (state.isRemovedFromLibrary) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (state.isLibraryMode) {
                        IconButton(onClick = { showRemoveDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove from Library",
                                tint = AppColors.bodyText,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color(0xFFE0E0E0),
                    navigationIconContentColor = Color(0xFFE0E0E0),
                ),
            )
        },
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color(0xFF008080))
                }
            }

            state.error != null && state.title.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = AppColors.bodyText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = viewModel::loadBookDetail) {
                            Text("Retry", color = Color(0xFF008080))
                        }
                    }
                }
            }

            else -> {
                var showFullDescription by remember { mutableStateOf(false) }
                val author = state.author
                val description = state.description

                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        AsyncImage(
                            model = state.coverUrl,
                            contentDescription = state.title,
                            modifier = Modifier
                                .width(160.dp)
                                .height(240.dp)
                                .background(Color(0xFF2A2A2A))
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = state.title,
                            color = Color(0xFFE0E0E0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )

                        if (!author.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = author,
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        }

                        if (!description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val maxLines = if (showFullDescription) Int.MAX_VALUE else 4
                            Text(
                                text = description,
                                color = Color(0xFFE0E0E0),
                                fontSize = 14.sp,
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!showFullDescription && description.length > 200) {
                                TextButton(
                                    onClick = { showFullDescription = true },
                                ) {
                                    Text("Show more", color = Color(0xFF008080), fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isLibraryMode) {
                            val statuses = listOf(
                                BookStatus.READING to "Reading",
                                BookStatus.PLAN_TO_READ to "Plan to Read",
                                BookStatus.COMPLETED to "Completed",
                                BookStatus.DROPPED to "Dropped",
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                statuses.forEach { (status, label) ->
                                    FilterChip(
                                        selected = state.bookStatus == status,
                                        onClick = { viewModel.changeStatus(status) },
                                        label = { Text(label, fontSize = 13.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF004040),
                                            selectedLabelColor = Color(0xFF008080),
                                            containerColor = Color(0xFF2D2D2D),
                                            labelColor = Color(0xFFAAAAAA),
                                        ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        HorizontalDivider(color = Color(0xFF2D2D2D))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Chapters (${state.chapters.size})",
                                color = Color(0xFFE0E0E0),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { viewModel.toggleChapterOrder() },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = if (state.chaptersReversed) "Sort ascending" else "Sort descending",
                                    tint = Color(0xFF008080),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        state.chapters.forEachIndexed { displayIndex, chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .then(
                                        if (state.isLibraryMode) {
                                            Modifier.clickable {
                                                navController.navigate(
                                                    Screen.Reader.createRoute(viewModel.bookId, chapter.order)
                                                )
                                            }
                                        } else Modifier
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.isLibraryMode) {
                                    Icon(
                                        imageVector = if (chapter.isRead)
                                            Icons.Default.CheckCircle
                                        else
                                            Icons.Default.RadioButtonUnchecked,
                                        contentDescription = if (chapter.isRead) "Read" else "Unread",
                                        tint = if (chapter.isRead) Color(0xFF008080) else AppColors.bodyText,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = "${displayIndex + 1}.",
                                    color = AppColors.bodyText,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(28.dp),
                                )
                                Text(
                                    text = chapter.title,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            if (state.isLibraryMode) {
                                navController.navigate(
                                    Screen.Reader.createRoute(viewModel.bookId, state.lastChapterOrder ?: 0)
                                )
                            } else {
                                viewModel.addToLibrary()
                            }
                        },
                        enabled = if (state.isLibraryMode) true else !state.isAddingToLibrary && !state.addedToLibrary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.addedToLibrary) Color(0xFF004040) else Color(0xFF008080),
                            contentColor = if (state.addedToLibrary) Color(0xFF008080) else Color.White,
                            disabledContainerColor = Color(0xFF004040),
                            disabledContentColor = Color(0xFF008080),
                        ),
                    ) {
                        if (!state.isLibraryMode && state.isAddingToLibrary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when {
                                state.isLibraryMode -> {
                                    if (state.lastChapterOrder != null) "Continue Reading"
                                    else "Start Reading"
                                }
                                state.isAddingToLibrary -> "Adding..."
                                state.addedToLibrary -> "Added to Library"
                                else -> "Add to Library"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (showRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text("Remove from Library") },
                text = { Text("Remove \"${state.title}\" from your library?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeFromLibrary()
                        showRemoveDialog = false
                    }) { Text("Remove", color = Color(0xFFCF6679)) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) {
                        Text("Cancel", color = AppColors.bodyText)
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color(0xFFE0E0E0),
                textContentColor = Color(0xFFE0E0E0),
            )
        }

        if (state.turnstileUrl != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearTurnstileDialog() },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color(0xFFE0E0E0),
                title = { Text("Cloudflare Verification") },
                text = {
                    Text(
                        text = "This site requires Cloudflare verification. Tap \"Solve in App\" to complete the challenge, then it will retry automatically.",
                        color = AppColors.bodyText,
                        fontSize = 14.sp,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val url = state.turnstileUrl ?: return@Button
                            val intent = Intent(context, CloudflareVerifyActivity::class.java).apply {
                                putExtra(CloudflareVerifyActivity.EXTRA_URL, url)
                            }
                            turnstileLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                    ) {
                        Text("Solve in App")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearTurnstileDialog() }) {
                        Text("Cancel", color = AppColors.bodyText)
                    }
                },
            )
        }
    }
}
