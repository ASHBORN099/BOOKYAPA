package com.bookyapa.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.data.repository.BookRepository
import com.bookyapa.app.data.repository.SourceRepository
import com.bookyapa.app.network.TurnstileBypassException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    data class UiState(
        val sources: List<SourceEntity> = emptyList(),
        val results: List<SearchResult> = emptyList(),
        val selectedSourceId: Long? = null,
        val selectedSourceName: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSourceList: Boolean = true,
        val turnstileUrl: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sourceRepository.getAllSources().collect { sources ->
                _state.update { it.copy(sources = sources) }
            }
        }
    }

    fun toggleSource(source: SourceEntity, enabled: Boolean) {
        viewModelScope.launch {
            sourceRepository.updateSource(source.copy(enabled = enabled))
        }
    }

    fun exploreSource(sourceId: Long, sourceName: String) {
        _state.update {
            it.copy(
                selectedSourceId = sourceId,
                selectedSourceName = sourceName,
                isLoading = true,
                error = null,
                turnstileUrl = null,
                isSourceList = false,
                results = emptyList(),
            )
        }
        viewModelScope.launch {
            bookRepository.fetchExploreBooks(sourceId)
                .onSuccess { results ->
                    _state.update { it.copy(results = results, isLoading = false) }
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

    fun clearTurnstileDialog() {
        _state.update { it.copy(turnstileUrl = null) }
    }

    fun retryAfterTurnstile() {
        val id = _state.value.selectedSourceId ?: return
        val name = _state.value.selectedSourceName
        _state.update { it.copy(turnstileUrl = null) }
        exploreSource(id, name)
    }

    fun backToSources() {
        _state.update {
            it.copy(
                selectedSourceId = null,
                selectedSourceName = "",
                results = emptyList(),
                error = null,
                turnstileUrl = null,
                isSourceList = true,
            )
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
