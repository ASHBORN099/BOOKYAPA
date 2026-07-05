package com.bookyapa.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.data.repository.BookRepository
import com.bookyapa.app.data.repository.SourceRepository
import com.bookyapa.app.network.TurnstileBypassException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    data class UiState(
        val query: String = "",
        val selectedSourceId: Long? = null,
        val sources: List<SourceEntity> = emptyList(),
        val results: List<SearchResult> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val turnstileUrl: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            sourceRepository.getEnabledSources().collect { sources ->
                _state.update { it.copy(sources = sources) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isLoading = false, error = null, turnstileUrl = null) }
            return
        }
        val sourceId = _state.value.selectedSourceId
        if (sourceId == null) {
            _state.update { it.copy(error = null, turnstileUrl = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query, sourceId)
        }
    }

    fun selectSource(sourceId: Long) {
        _state.update { it.copy(selectedSourceId = sourceId, error = null, turnstileUrl = null) }
        val query = _state.value.query
        if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                delay(200)
                performSearch(query, sourceId)
            }
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = emptyList(), isLoading = false, error = null, turnstileUrl = null) }
    }

    fun clearTurnstileDialog() {
        _state.update { it.copy(turnstileUrl = null) }
    }

    fun retryAfterTurnstile() {
        val query = _state.value.query
        val sourceId = _state.value.selectedSourceId ?: return
        _state.update { it.copy(turnstileUrl = null) }
        if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(query, sourceId)
            }
        }
    }

    private suspend fun performSearch(query: String, sourceId: Long) {
        _state.update { it.copy(isLoading = true, error = null, turnstileUrl = null) }
        bookRepository.searchBooks(query, sourceId)
            .onSuccess { results ->
                _state.update { it.copy(results = results, isLoading = false) }
            }
            .onFailure { e ->
                val turnstile = findTurnstileException(e)
                if (turnstile != null) {
                    _state.update {
                        it.copy(
                            results = emptyList(),
                            isLoading = false,
                            error = "Cloudflare verification required",
                            turnstileUrl = turnstile.url,
                        )
                    }
                } else {
                    _state.update { it.copy(results = emptyList(), isLoading = false, error = e.message) }
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
