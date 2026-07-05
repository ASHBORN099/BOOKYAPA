package com.bookyapa.app.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.dao.BookmarkWithBookInfo
import com.bookyapa.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    data class UiState(
        val bookmarks: List<BookmarkWithBookInfo> = emptyList(),
        val isLoading: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepository.getAllBookmarks().collect { bookmarks ->
                _state.update { it.copy(bookmarks = bookmarks, isLoading = false) }
            }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            bookRepository.deleteBookmarkById(bookmarkId)
        }
    }
}
