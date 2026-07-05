package com.bookyapa.app.ui.search

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.navigation.Screen
import com.bookyapa.app.network.CloudflareVerifyActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val turnstileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (state.turnstileUrl != null) {
            viewModel.retryAfterTurnstile()
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search books...", color = AppColors.bodyText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AppColors.bodyText,
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearResults) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = AppColors.bodyText,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE0E0E0),
                    unfocusedTextColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF008080),
                    unfocusedBorderColor = Color(0xFF2D2D2D),
                    cursorColor = Color(0xFF008080),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                ),
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.sources, key = { it.id }) { source ->
                    FilterChip(
                        selected = state.selectedSourceId == source.id,
                        onClick = { viewModel.selectSource(source.id) },
                        label = {
                            Text(
                                text = source.name,
                                fontSize = 13.sp,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF004040),
                            selectedLabelColor = Color(0xFF008080),
                            containerColor = Color(0xFF2D2D2D),
                            labelColor = Color(0xFFAAAAAA),
                        ),
                    )
                }
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF008080))
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = Color(0xFFCF6679),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                state.sources.isEmpty() && state.query.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No sources available",
                                color = AppColors.bodyText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Add a source in the Sources tab first",
                                color = AppColors.bodyText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                state.query.isNotBlank() && state.selectedSourceId == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Select a source above to search",
                            color = AppColors.bodyText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                state.query.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Type to search",
                            color = AppColors.bodyText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                state.results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No results found",
                                color = AppColors.bodyText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "The source selectors may need updating",
                                color = AppColors.bodyText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.results, key = { it.url }) { result ->
                            SearchResultItem(
                                result = result,
                                onClick = {
                                    val sourceId = state.selectedSourceId ?: return@SearchResultItem
                                    navController.navigate(
                                        Screen.BookDetail.createRoute(sourceId, result.url)
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
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

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = result.coverUrl,
                contentDescription = result.title,
                modifier = Modifier
                    .size(width = 72.dp, height = 108.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = result.title,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
