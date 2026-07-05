package com.bookyapa.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.local.entity.ChapterEntity
import com.bookyapa.app.data.model.BookDetail
import com.bookyapa.app.data.model.BookStatus
import com.bookyapa.app.data.repository.BookRepository
import com.bookyapa.app.network.TurnstileBypassException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DisplayChapter(
    val title: String,
    val order: Int,
    val isRead: Boolean = false,
    val url: String = "",
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookId: Long = savedStateHandle["bookId"] ?: -1L
    val sourceId: Long = savedStateHandle["sourceId"] ?: -1L
    val bookUrl: String = savedStateHandle["bookUrl"] ?: ""

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null,
        val title: String = "",
        val author: String? = null,
        val coverUrl: String? = null,
        val description: String? = null,
        val chapters: List<DisplayChapter> = emptyList(),
        val isLibraryMode: Boolean = false,
        val isAddingToLibrary: Boolean = false,
        val addedToLibrary: Boolean = false,
        val addedBookId: Long? = null,
        val bookStatus: com.bookyapa.app.data.model.BookStatus? = null,
        val lastChapterOrder: Int? = null,
        val chaptersReversed: Boolean = false,
        val isRemovedFromLibrary: Boolean = false,
        val turnstileUrl: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        if (bookId > 0) {
            loadBookFromLibrary()
        } else {
            loadBookFromWeb()
        }
    }

    private fun loadBookFromLibrary() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                _state.update { it.copy(isLoading = false, error = "Book not found") }
                return@launch
            }
            val chapters = bookRepository.getChaptersForBookOnce(bookId)
            _state.update {
                it.copy(
                    isLoading = false,
                    isLibraryMode = true,
                    title = book.title,
                    author = book.author,
                    coverUrl = book.coverUrl,
                    description = book.description,
                    chapters = chapters.map { ch -> DisplayChapter(ch.title, ch.order, ch.isRead, ch.url ?: "") },
                    bookStatus = book.status,
                    lastChapterOrder = book.lastChapterOrder,
                )
            }
        }
    }

    private fun loadBookFromWeb() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            bookRepository.fetchBookDetail(bookUrl, sourceId)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLibraryMode = false,
                            title = detail.title,
                            author = detail.author,
                            coverUrl = detail.coverUrl,
                            description = detail.description,
                            chapters = detail.chapterItems.mapIndexed { index, item ->
                                DisplayChapter(item.title, index, url = item.url)
                            },
                        )
                    }
                }
                .onFailure { e ->
                    val turnstile = findTurnstileException(e)
                    if (turnstile != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Cloudflare verification required",
                                turnstileUrl = turnstile.url,
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = e.message) }
                    }
                }
        }
    }

    fun loadBookDetail() {
        if (bookId > 0) loadBookFromLibrary() else loadBookFromWeb()
    }

    fun clearTurnstileDialog() {
        _state.update { it.copy(turnstileUrl = null) }
    }

    fun retryAfterTurnstile() {
        _state.update { it.copy(turnstileUrl = null) }
        loadBookFromWeb()
    }

    fun toggleChapterOrder() {
        _state.update {
            it.copy(
                chaptersReversed = !it.chaptersReversed,
                chapters = it.chapters.reversed(),
            )
        }
    }

    fun changeStatus(newStatus: BookStatus) {
        viewModelScope.launch {
            bookRepository.updateBookStatus(bookId, newStatus)
            _state.update { it.copy(bookStatus = newStatus) }
        }
    }

    fun removeFromLibrary() {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
            _state.update { it.copy(isRemovedFromLibrary = true) }
        }
    }

    fun addToLibrary() {
        viewModelScope.launch {
            val detail = BookDetail(
                title = _state.value.title,
                author = _state.value.author,
                coverUrl = _state.value.coverUrl,
                description = _state.value.description,
                chapterItems = _state.value.chapters.map { ch ->
                    com.bookyapa.app.data.model.ChapterItem(ch.title, ch.url, ch.order)
                },
            )
            _state.update { it.copy(isAddingToLibrary = true) }
            try {
                val id = bookRepository.addBookToLibrary(detail, sourceId, bookUrl)
                _state.update { it.copy(isAddingToLibrary = false, addedToLibrary = true, addedBookId = id) }
            } catch (e: Exception) {
                _state.update { it.copy(isAddingToLibrary = false, error = e.message) }
            }
        }
    }
}

private fun findTurnstileException(e: Throwable): TurnstileBypassException? {
    var current: Throwable? = e
    while (current != null) {
        if (current is TurnstileBypassException) return current
        current = current.cause
    }
    return null
}
