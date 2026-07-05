package com.bookyapa.app.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.repository.BookRepository
import com.bookyapa.app.data.repository.EpubRepository
import com.bookyapa.app.domain.epub.EpubParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val epubParser: EpubParser,
    private val epubRepository: EpubRepository,
) : ViewModel() {

    data class UiState(
        val allBooks: List<BookEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isImporting: Boolean = false,
        val importMessage: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.getAllBooks().collect { books ->
                _state.update { it.copy(allBooks = books, isLoading = false) }
            }
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }

    fun importEpub(uri: Uri, context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, importMessage = "Opening file...") }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open file")

                val epubBook = epubParser.parse(inputStream).getOrThrow()
                epubRepository.importEpub(epubBook, context.filesDir) { msg ->
                    _state.update { it.copy(importMessage = msg) }
                }
                _state.update { it.copy(isImporting = false, importMessage = "Import complete!") }
                delay(2000)
                _state.update { it.copy(importMessage = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        importMessage = "Import failed: ${e.message}",
                    )
                }
                delay(3000)
                _state.update { it.copy(importMessage = null) }
            }
        }
    }

    fun clearImportMessage() {
        _state.update { it.copy(importMessage = null) }
    }
}
