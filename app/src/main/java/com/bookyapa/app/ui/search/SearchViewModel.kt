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

    data class SourceResults(
        val sourceName: String,
        val results: List<SearchResult>,
        val error: String? = null,
        val isLoading: Boolean = true,
    )

    data class UiState(
        val query: String = "",
        val sources: List<SourceEntity> = emptyList(),
        val sourceResults: List<SourceResults> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val turnstileUrl: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val searchCache = HashMap<String, List<SourceResults>>()

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
            _state.update { it.copy(sourceResults = emptyList(), isLoading = false, error = null, turnstileUrl = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(query)
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", sourceResults = emptyList(), isLoading = false, error = null, turnstileUrl = null) }
    }

    fun clearTurnstileDialog() {
        _state.update { it.copy(turnstileUrl = null) }
    }

    fun retryAfterTurnstile() {
        val query = _state.value.query
        _state.update { it.copy(turnstileUrl = null) }
        if (query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(query)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        val cached = searchCache[query.lowercase()]
        if (cached != null) {
            _state.update {
                it.copy(sourceResults = cached, isLoading = false, error = null)
            }
            return
        }

        _state.update { it.copy(isLoading = true, error = null, turnstileUrl = null, sourceResults = emptyList()) }

        val allResults = bookRepository.searchAllSources(query)

        val sourceResultsList = allResults.map { (sourceName, result) ->
            result.fold(
                onSuccess = { results ->
                    SourceResults(sourceName = sourceName, results = results, isLoading = false)
                },
                onFailure = { e ->
                    val turnstile = findTurnstileException(e)
                    if (turnstile != null) {
                        _state.update { it.copy(turnstileUrl = turnstile.url) }
                        SourceResults(sourceName = sourceName, results = emptyList(), error = "Cloudflare verification required", isLoading = false)
                    } else {
                        SourceResults(sourceName = sourceName, results = emptyList(), error = e.message, isLoading = false)
                    }
                }
            )
        }

        searchCache[query.lowercase()] = sourceResultsList

        _state.update {
            it.copy(sourceResults = sourceResultsList, isLoading = false)
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
