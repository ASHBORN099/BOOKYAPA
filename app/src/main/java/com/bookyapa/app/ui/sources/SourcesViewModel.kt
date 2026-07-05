package com.bookyapa.app.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    private val _sources = MutableStateFlow<List<SourceEntity>>(emptyList())
    val sources: StateFlow<List<SourceEntity>> = _sources.asStateFlow()

    init {
        viewModelScope.launch {
            sourceRepository.getAllSources().collect { _sources.value = it }
        }
    }

    fun toggleSource(source: SourceEntity, enabled: Boolean) {
        viewModelScope.launch {
            sourceRepository.updateSource(source.copy(enabled = enabled))
        }
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch {
            sourceRepository.deleteSource(source)
        }
    }
}
