package com.bookyapa.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    data class UiState(
        val books: List<BookEntity> = emptyList(),
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.getHistory().collect { books ->
                _state.update { it.copy(books = books, isLoading = false) }
            }
        }
    }

    fun clearHistory(bookId: Long) {
        viewModelScope.launch {
            bookRepository.clearHistoryEntry(bookId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            bookRepository.clearAllHistory()
        }
    }
}
