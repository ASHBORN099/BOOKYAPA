package com.bookyapa.app.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.ChapterEntity
import com.bookyapa.app.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bookId: Long = savedStateHandle["bookId"] ?: -1L
    val startChapterIndex: Int = savedStateHandle["chapterIndex"] ?: 0

    data class UiState(
        val chapters: List<ChapterEntity> = emptyList(),
        val currentChapterIndex: Int = 0,
        val content: String = "",
        val pages: List<String> = emptyList(),
        val currentPage: Int = 0,
        val isLoadingChapters: Boolean = true,
        val isLoadingContent: Boolean = false,
        val error: String? = null,
        val fontSize: Int = 16,
        val currentChapterTitle: String = "",
        val sourceId: Long? = null,
        val isBookmarked: Boolean = false,
        val pendingRestorePage: Int = -1,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var saveJob: Job? = null

    private fun scheduleSavePosition() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val s = _state.value
            val chapter = s.chapters.getOrNull(s.currentChapterIndex) ?: return@launch
            bookRepository.saveChapterScrollPosition(chapter.id, s.currentPage)
        }
    }

    init {
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book == null) {
                _state.update { it.copy(isLoadingChapters = false, error = "Book not found") }
                return@launch
            }
            val savedFontSize = if (book.fontSize > 0) book.fontSize else 16
            val chapters = bookRepository.getChaptersForBookOnce(bookId)
            val clampedIndex = startChapterIndex.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
            _state.update {
                it.copy(
                    isLoadingChapters = false,
                    chapters = chapters,
                    currentChapterIndex = clampedIndex,
                    sourceId = book.sourceId,
                    fontSize = savedFontSize,
                )
            }
            observeBookmarkState()
            loadCurrentChapterContent()
        }
    }

    fun loadCurrentChapterContent() {
        val stateValue = _state.value
        val index = stateValue.currentChapterIndex
        if (index < 0 || index >= stateValue.chapters.size) return

        val chapter = stateValue.chapters[index]
        _state.update {
            it.copy(
                currentChapterTitle = chapter.title,
                isLoadingContent = true,
                error = null,
                pendingRestorePage = -1,
            )
        }

        viewModelScope.launch {
            val freshChapter = bookRepository.getChapterById(chapter.id)
            val savedPage = freshChapter?.lastScrollPosition ?: 0

            if (chapter.content.isNotBlank()) {
                _state.update {
                    it.copy(content = chapter.content, isLoadingContent = false, pendingRestorePage = savedPage)
                }
                markChapterRead(chapter.id)
                return@launch
            }

            if (chapter.url.isNullOrBlank()) {
                _state.update {
                    it.copy(isLoadingContent = false, error = "No content available for this chapter")
                }
                return@launch
            }

            val sourceId = _state.value.sourceId
            if (sourceId == null || sourceId == -1L) {
                _state.update {
                    it.copy(isLoadingContent = false, error = "Source not available")
                }
                return@launch
            }

            bookRepository.downloadChapterContent(chapter, sourceId)
                .onSuccess { html ->
                    val text = stripHtml(html)
                    bookRepository.saveChapterContent(chapter.id, text)
                    _state.update {
                        it.copy(content = text, isLoadingContent = false, pendingRestorePage = savedPage)
                    }
                    markChapterRead(chapter.id)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoadingContent = false, error = e.message ?: "Failed to load chapter")
                    }
                }
        }
    }

    fun setPages(pages: List<String>) {
        val stateValue = _state.value
        val restorePage = if (stateValue.pendingRestorePage >= 0) {
            stateValue.pendingRestorePage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        } else stateValue.currentPage
        _state.update { it.copy(pages = pages, currentPage = restorePage, pendingRestorePage = -1) }
    }

    fun goToPage(index: Int) {
        val pageCount = _state.value.pages.size
        if (index < 0 || index >= pageCount) return
        _state.update { it.copy(currentPage = index) }
        scheduleSavePosition()
    }

    fun nextPage() {
        val stateValue = _state.value
        if (stateValue.currentPage < stateValue.pages.size - 1) {
            _state.update { it.copy(currentPage = stateValue.currentPage + 1) }
        } else {
            nextChapter()
        }
    }

    fun previousPage() {
        val stateValue = _state.value
        if (stateValue.currentPage > 0) {
            _state.update { it.copy(currentPage = stateValue.currentPage - 1) }
        } else {
            previousChapter()
        }
    }

    fun navigateToChapter(index: Int) {
        if (index < 0 || index >= _state.value.chapters.size) return
        val currentState = _state.value
        val currentChapter = currentState.chapters.getOrNull(currentState.currentChapterIndex)
        if (currentChapter != null) {
            viewModelScope.launch {
                bookRepository.saveChapterScrollPosition(currentChapter.id, currentState.currentPage)
            }
        }
        _state.update { it.copy(currentChapterIndex = index, currentPage = 0) }
        loadCurrentChapterContent()
    }

    fun nextChapter() {
        navigateToChapter(_state.value.currentChapterIndex + 1)
    }

    fun previousChapter() {
        navigateToChapter(_state.value.currentChapterIndex - 1)
    }

    fun updateFontSize(delta: Int) {
        val current = _state.value.fontSize
        val newSize = (current + delta).coerceIn(12, 28)
        _state.update { it.copy(fontSize = newSize) }
        viewModelScope.launch { bookRepository.updateBookFontSize(bookId, newSize) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val s = _state.value
            if (s.currentChapterIndex < 0 || s.currentChapterIndex >= s.chapters.size) return@launch
            val chapter = s.chapters[s.currentChapterIndex]
            val isNowBookmarked = bookRepository.toggleBookmark(
                bookId = bookId,
                chapterOrder = chapter.order,
                chapterTitle = chapter.title,
                pageIndex = s.currentPage,
                text = s.pages.getOrElse(s.currentPage) { chapter.title }.take(200),
            )
            _state.update { it.copy(isBookmarked = isNowBookmarked) }
        }
    }

    private fun observeBookmarkState() {
        viewModelScope.launch {
            val s = _state.value
            val chapter = s.chapters.getOrNull(s.currentChapterIndex) ?: return@launch
            bookRepository.isBookmarked(bookId, chapter.order).collect { bookmarked ->
                _state.update { it.copy(isBookmarked = bookmarked) }
            }
        }
    }

    private suspend fun markChapterRead(chapterId: Long) {
        bookRepository.markChapterRead(chapterId)
        val book = bookRepository.getBookById(bookId) ?: return
        bookRepository.updateBook(
            book.copy(
                lastReadAt = System.currentTimeMillis(),
                lastChapterOrder = _state.value.currentChapterIndex,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun onCleared() {
        saveJob?.cancel()
        val s = _state.value
        val chapter = s.chapters.getOrNull(s.currentChapterIndex)
        if (chapter != null) {
            runBlocking {
                bookRepository.saveChapterScrollPosition(chapter.id, s.currentPage)
            }
        }
        super.onCleared()
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
