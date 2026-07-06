package com.bookyapa.app.ui.search

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.bookyapa.app.ui.theme.AppColors

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

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF008080))
                    }
                }

                state.sources.isEmpty() && state.query.isNotBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No sources available", color = AppColors.bodyText, style = MaterialTheme.typography.bodyLarge)
                            Text("Add a source in the Sources tab first", color = AppColors.bodyText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                state.query.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Type to search", color = AppColors.bodyText, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                state.sourceResults.isEmpty() && !state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No results found", color = AppColors.bodyText, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        state.sourceResults.forEach { sourceResults ->
                            if (sourceResults.results.isNotEmpty()) {
                                item(key = "header_${sourceResults.sourceName}") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = sourceResults.sourceName,
                                            color = Color(0xFF008080),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "(${sourceResults.results.size})",
                                            color = AppColors.bodyText,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                item(key = "cards_${sourceResults.sourceName}") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(sourceResults.results, key = { it.url }) { result ->
                                            HorizontalBookCard(
                                                result = result,
                                                onClick = {
                                                    val source = state.sources.find { it.name == sourceResults.sourceName }
                                                    if (source != null) {
                                                        navController.navigate(
                                                            Screen.BookDetail.createRoute(source.id, result.url)
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                                item(key = "spacer_${sourceResults.sourceName}") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            if (sourceResults.isLoading) {
                                item(key = "loading_${sourceResults.sourceName}") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF008080),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading ${sourceResults.sourceName}...", color = AppColors.bodyText, fontSize = 13.sp)
                                    }
                                }
                            }
                            if (sourceResults.error != null && sourceResults.results.isEmpty()) {
                                item(key = "error_${sourceResults.sourceName}") {
                                    Text(
                                        text = "${sourceResults.sourceName}: ${sourceResults.error}",
                                        color = Color(0xFFCF6679),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                            }
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
private fun HorizontalBookCard(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(140.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            AsyncImage(
                model = result.coverUrl,
                contentDescription = result.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Fit,
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = result.title,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!result.author.isNullOrBlank()) {
                    Text(
                        text = result.author,
                        color = AppColors.bodyText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
