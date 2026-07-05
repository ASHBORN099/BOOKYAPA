package com.bookyapa.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookyapa.app.data.CatalogSource

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF008080))
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bundled Sources",
                    color = Color(0xFF008080),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.bundled.isEmpty()) {
                item {
                    Text(
                        text = "No bundled sources available",
                        color = AppColors.bodyText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(state.bundled, key = { it.id }) { source ->
                    val baseUrl = source.baseUrl.trimEnd('/')
                    CatalogSourceItem(
                        catalog = source,
                        isInstalled = baseUrl in state.installedBaseUrls,
                        isOutdated = baseUrl in state.outdatedBaseUrls,
                        onInstall = { viewModel.installSource(source) },
                        onUpdate = { viewModel.updateSource(source) },
                        badge = "Bundled",
                    )
                }
            }

            if (state.remoteRepos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Remote Repositories",
                        color = Color(0xFF008080),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                state.remoteRepos.forEach { repo ->
                    item {
                        RepoHeader(repo.url, repo.name, repo.isLoading, repo.error)
                    }
                    if (repo.error != null) {
                        item {
                            Text(
                                text = repo.error,
                                color = Color(0xFFCF6679),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    items(repo.sources, key = { "${repo.url}:${it.id}" }) { source ->
                        val baseUrl = source.baseUrl.trimEnd('/')
                        CatalogSourceItem(
                            catalog = source,
                            isInstalled = baseUrl in state.installedBaseUrls,
                            isOutdated = baseUrl in state.outdatedBaseUrls,
                            onInstall = { viewModel.installSource(source) },
                            onUpdate = { viewModel.updateSource(source) },
                            badge = repo.name,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun RepoHeader(url: String, name: String, isLoading: Boolean, error: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            tint = Color(0xFF008080),
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = Color(0xFFE0E0E0),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        if (isLoading) {
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF008080),
            )
        }
        if (error != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color(0xFFCF6679),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun CatalogSourceItem(
    catalog: CatalogSource,
    isInstalled: Boolean,
    isOutdated: Boolean,
    onInstall: () -> Unit,
    onUpdate: () -> Unit,
    badge: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
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
                    text = catalog.name,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = catalog.baseUrl,
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (catalog.description.isNotEmpty()) {
                    Text(
                        text = catalog.description,
                        color = AppColors.bodyText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = badge,
                    color = Color(0xFF008080),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isInstalled && !isOutdated) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Installed",
                    tint = Color(0xFF008080),
                    modifier = Modifier.size(24.dp),
                )
            } else if (isInstalled && isOutdated) {
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB87333),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF008080),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
