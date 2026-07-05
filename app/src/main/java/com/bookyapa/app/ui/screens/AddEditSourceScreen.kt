package com.bookyapa.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookyapa.app.ui.theme.AppColors
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bookyapa.app.data.model.SourceConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSourceScreen(
    navController: NavController,
    viewModel: EditSourceViewModel = hiltViewModel(),
) {
    val sourceId = navController.currentBackStackEntry?.arguments?.getLong("sourceId") ?: -1L
    val isEditing = sourceId != -1L

    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var config by remember { mutableStateOf(SourceConfig()) }

    if (isEditing) {
        LaunchedEffect(sourceId) {
            viewModel.loadSource(sourceId) { source, cfg ->
                name = source.name
                baseUrl = source.baseUrl
                enabled = source.enabled
                config = cfg
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Source" else "Add Source") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveSource(sourceId, name, baseUrl, enabled, config) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Basic Info")

            FormField(
                value = name,
                onValueChange = { name = it },
                label = "Source Name",
                placeholder = "e.g. My Novel Site",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = "Base URL",
                placeholder = "e.g. https://example.com",
                keyboardType = KeyboardType.Uri,
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Search Configuration")

            FormField(
                value = config.searchUrlPattern,
                onValueChange = { config = config.copy(searchUrlPattern = it) },
                label = "Search URL Pattern",
                placeholder = "e.g. /search?keyword={query}",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.searchResultContainer,
                onValueChange = { config = config.copy(searchResultContainer = it) },
                label = "Search Result Container",
                placeholder = "e.g. .book-item",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.searchResultTitle,
                onValueChange = { config = config.copy(searchResultTitle = it) },
                label = "Search Result Title",
                placeholder = "e.g. h3.title a",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.searchResultLink,
                onValueChange = { config = config.copy(searchResultLink = it) },
                label = "Search Result Link",
                placeholder = "e.g. a",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.searchResultCover,
                onValueChange = { config = config.copy(searchResultCover = it) },
                label = "Search Result Cover",
                placeholder = "e.g. img.cover",
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Book Detail")

            FormField(
                value = config.bookTitle,
                onValueChange = { config = config.copy(bookTitle = it) },
                label = "Book Title",
                placeholder = "e.g. h1.book-title",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.bookAuthor,
                onValueChange = { config = config.copy(bookAuthor = it) },
                label = "Book Author",
                placeholder = "e.g. .author-name",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.bookCover,
                onValueChange = { config = config.copy(bookCover = it) },
                label = "Book Cover",
                placeholder = "e.g. .book-cover img",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.bookDescription,
                onValueChange = { config = config.copy(bookDescription = it) },
                label = "Book Description",
                placeholder = "e.g. .description",
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Chapter List")

            FormField(
                value = config.chapterListItem,
                onValueChange = { config = config.copy(chapterListItem = it) },
                label = "Chapter List Item",
                placeholder = "e.g. li.chapter-item",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.chapterTitle,
                onValueChange = { config = config.copy(chapterTitle = it) },
                label = "Chapter Title",
                placeholder = "e.g. a",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.chapterLink,
                onValueChange = { config = config.copy(chapterLink = it) },
                label = "Chapter Link",
                placeholder = "e.g. a",
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Chapter Content")

            FormField(
                value = config.chapterContent,
                onValueChange = { config = config.copy(chapterContent = it) },
                label = "Chapter Content Container",
                placeholder = "e.g. .chapter-content",
            )
            Spacer(modifier = Modifier.height(8.dp))
            FormField(
                value = config.chapterNextPage,
                onValueChange = { config = config.copy(chapterNextPage = it) },
                label = "Next Page Link (if multi-page)",
                placeholder = "e.g. a.next-page",
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF008080),
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = AppColors.bodyText) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFFE0E0E0),
            unfocusedTextColor = Color(0xFFE0E0E0),
            cursorColor = Color(0xFF008080),
            focusedBorderColor = Color(0xFF008080),
            unfocusedBorderColor = Color(0xFF2D2D2D),
            focusedLabelColor = Color(0xFF008080),
            unfocusedLabelColor = AppColors.bodyText,
        ),
    )
}
