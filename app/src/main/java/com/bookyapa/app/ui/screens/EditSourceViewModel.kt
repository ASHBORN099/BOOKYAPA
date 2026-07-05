package com.bookyapa.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SourceConfig
import com.bookyapa.app.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditSourceViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
) : ViewModel() {

    fun loadSource(sourceId: Long, onLoaded: (SourceEntity, SourceConfig) -> Unit) {
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(sourceId) ?: return@launch
            val config = sourceRepository.getSourceConfig(sourceId) ?: SourceConfig()
            onLoaded(source, config)
        }
    }

    fun saveSource(
        sourceId: Long,
        name: String,
        baseUrl: String,
        enabled: Boolean,
        config: SourceConfig,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val configJson = sourceRepository.encodeConfig(config)
            if (sourceId == -1L) {
                sourceRepository.saveSource(
                    SourceEntity(name = name, baseUrl = baseUrl, enabled = enabled, configJson = configJson),
                )
            } else {
                val existing = sourceRepository.getSourceById(sourceId) ?: return@launch
                sourceRepository.updateSource(
                    existing.copy(name = name, baseUrl = baseUrl, enabled = enabled, configJson = configJson),
                )
            }
            onSaved()
        }
    }
}
