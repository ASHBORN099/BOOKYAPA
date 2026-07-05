package com.bookyapa.app.ui.explore

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bookyapa.app.network.CloudflareVerifyActivity
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.navigation.Screen

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
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

    if (state.isSourceList) {
        ExploreSourcesView(
            sources = state.sources,
            onExplore = { source ->
                viewModel.exploreSource(source.id, source.name)
            },
            onToggle = { source, enabled ->
                viewModel.toggleSource(source, enabled)
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        ExploreResultsView(
            results = state.results,
            isLoading = state.isLoading,
            error = state.error,
            selectedSourceId = state.selectedSourceId,
            onRetry = {
                val id = state.selectedSourceId ?: return@ExploreResultsView
                viewModel.exploreSource(id, state.selectedSourceName)
            },
            onTapResult = { result ->
                val sourceId = state.selectedSourceId ?: return@ExploreResultsView
                navController.navigate(Screen.BookDetail.createRoute(sourceId, result.url))
            },
            onBack = viewModel::backToSources,
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun ExploreSourcesView(
    sources: List<SourceEntity>,
    onExplore: (SourceEntity) -> Unit,
    onToggle: (SourceEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sources.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No sources available",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add sources from the Catalog tab",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sources, key = { it.id }) { source ->
                SourceExploreCard(
                    source = source,
                    onExplore = { onExplore(source) },
                    onToggle = { enabled -> onToggle(source, enabled) },
                )
            }
        }
    }
}

@Composable
private fun SourceExploreCard(
    source: SourceEntity,
    onExplore: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color(0xFF008080),
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = source.baseUrl,
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = source.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF008080),
                    checkedTrackColor = Color(0xFF004040),
                    uncheckedThumbColor = AppColors.bodyText,
                    uncheckedTrackColor = Color(0xFF2D2D2D),
                ),
            )
            if (source.enabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExplore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF008080),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Browse", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ExploreResultsView(
    results: List<SearchResult>,
    isLoading: Boolean,
    error: String?,
    selectedSourceId: Long?,
    onRetry: () -> Unit,
    onTapResult: (SearchResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF008080))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading...", color = AppColors.bodyText)
                }
            }
        }

        error != null -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = error,
                        color = AppColors.bodyText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF008080)),
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        results.isEmpty() -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No content found",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier.padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.url }) { result ->
                    ExploreResultItem(
                        result = result,
                        onClick = { onTapResult(result) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreResultItem(
    result: SearchResult,
    onClick: () -> Unit,
) {
    Card(
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
                    .aspectRatio(0.67f)
                    .background(Color(0xFF2A2A2A))
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = result.title,
                color = Color(0xFFE0E0E0),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
