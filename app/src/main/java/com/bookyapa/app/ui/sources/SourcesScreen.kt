package com.bookyapa.app.ui.sources

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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    navController: NavController,
    viewModel: SourcesViewModel = hiltViewModel(),
) {
    val sources by viewModel.sources.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<SourceEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sources") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Browse Catalog") },
                            onClick = {
                                showMenu = false
                                navController.navigate(Screen.Catalog.createRoute())
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Book, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Source Repositories") },
                            onClick = {
                                showMenu = false
                                navController.navigate(Screen.RepoManager.createRoute())
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            onClick = {
                                showMenu = false
                                navController.navigate(Screen.Bookmarks.createRoute())
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Bookmark, contentDescription = null)
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color(0xFFE0E0E0),
                    actionIconContentColor = Color(0xFFE0E0E0),
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEditSource.createRoute(-1L)) },
                containerColor = Color(0xFF008080),
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Source")
            }
        },
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        if (sources.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No sources yet",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add a source or Browse Catalog",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { navController.navigate(Screen.Catalog.createRoute()) },
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse Catalog")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sources, key = { it.id }) { source ->
                    SourceItem(
                        source = source,
                        onToggle = { enabled -> viewModel.toggleSource(source, enabled) },
                        onDelete = { showDeleteDialog = source },
                        onClick = { navController.navigate(Screen.AddEditSource.createRoute(source.id)) },
                    )
                }
            }
        }

        showDeleteDialog?.let { source ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Source") },
                text = { Text("Delete \"${source.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSource(source)
                        showDeleteDialog = null
                    }) { Text("Delete", color = Color(0xFFCF6679)) }
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

@Composable
private fun SourceItem(
    source: SourceEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
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
                    text = source.name,
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodyLarge,
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
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete source",
                    tint = AppColors.bodyText,
                )
            }
        }
    }
}
