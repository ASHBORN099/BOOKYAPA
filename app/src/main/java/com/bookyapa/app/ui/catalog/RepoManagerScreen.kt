package com.bookyapa.app.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bookyapa.app.ui.theme.RepoUrlManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoManagerScreen(
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    val urls by RepoUrlManager.getUrlsFlow().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Source Repositories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Repository")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color(0xFFE0E0E0),
                    navigationIconContentColor = Color(0xFFE0E0E0),
                    actionIconContentColor = Color(0xFF008080),
                ),
            )
        },
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        if (urls.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No remote repositories",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Tap + to add a source repository URL",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "The URL should return a JSON with 'name' and 'sources' fields",
                    color = AppColors.bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(urls, key = { it }) { url ->
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
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF008080),
                            )
                            Text(
                                text = url,
                                color = Color(0xFFE0E0E0),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = {
                                scope.launch { RepoUrlManager.removeUrl(url) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = AppColors.bodyText,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false; newUrl = "" },
                title = { Text("Add Repository") },
                text = {
                    Column {
                        Text(
                            text = "Enter the URL of a source repository JSON file.",
                            color = AppColors.bodyText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            label = { Text("Repository URL") },
                            placeholder = { Text("https://example.com/repo/index.json") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE0E0E0),
                                unfocusedTextColor = Color(0xFFE0E0E0),
                                focusedBorderColor = Color(0xFF008080),
                                unfocusedBorderColor = Color(0xFF2D2D2D),
                                focusedLabelColor = Color(0xFF008080),
                                unfocusedLabelColor = AppColors.bodyText,
                            ),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newUrl.isNotBlank()) {
                                scope.launch {
                                    RepoUrlManager.addUrl(newUrl.trim())
                                }
                                newUrl = ""
                                showDialog = false
                            }
                        },
                    ) {
                        Text("Add", color = Color(0xFF008080))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false; newUrl = "" }) {
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
