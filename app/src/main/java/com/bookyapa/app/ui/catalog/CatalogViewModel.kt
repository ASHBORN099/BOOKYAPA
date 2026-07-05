package com.bookyapa.app.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookyapa.app.data.CatalogSource
import com.bookyapa.app.data.SourceCatalog
import com.bookyapa.app.data.model.SourceConfig
import com.bookyapa.app.ui.theme.RepoUrlManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val bundled: List<CatalogSource> = emptyList(),
    val remoteRepos: List<RepoState> = emptyList(),
    val installedBaseUrls: Set<String> = emptySet(),
    val outdatedBaseUrls: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class RepoState(
    val url: String,
    val name: String,
    val sources: List<CatalogSource>,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val sourceCatalog: SourceCatalog,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val bundled = sourceCatalog.getBundledSources()
            val repoUrls = RepoUrlManager.getUrls()
            val installedSources = sourceCatalog.getInstalledSources()
            val installedBaseUrls = installedSources.map { it.baseUrl }.toSet()

            val allCatalog = listOfNotNull(bundled) + emptyList<CatalogSource>()
            val outdatedBaseUrls = mutableSetOf<String>()
            for (catalogSource in bundled) {
                val installed = installedSources.find { it.baseUrl == catalogSource.baseUrl.trimEnd('/') }
                if (installed != null) {
                    val catalogConfigJson = sourceCatalog.encodeConfig(catalogSource.config)
                    if (installed.configJson != catalogConfigJson) {
                        outdatedBaseUrls.add(installed.baseUrl)
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                bundled = bundled,
                installedBaseUrls = installedBaseUrls,
                outdatedBaseUrls = outdatedBaseUrls,
                isLoading = false,
            )

            repoUrls.forEach { url ->
                fetchRepo(url)
            }
        }
    }

    fun fetchRepo(url: String) {
        viewModelScope.launch {
            val current = _uiState.value.remoteRepos.toMutableList()
            current.add(RepoState(url = url, name = url, sources = emptyList(), isLoading = true))
            _uiState.value = _uiState.value.copy(remoteRepos = current)

            val result = sourceCatalog.fetchRemoteSources(url)
            result.onSuccess { repo ->
                val updated = _uiState.value.remoteRepos.map {
                    if (it.url == url) RepoState(url = url, name = repo.name, sources = repo.sources)
                    else it
                }
                _uiState.value = _uiState.value.copy(remoteRepos = updated)
            }.onFailure { e ->
                val updated = _uiState.value.remoteRepos.map {
                    if (it.url == url) it.copy(isLoading = false, error = e.message ?: "Failed to load")
                    else it
                }
                _uiState.value = _uiState.value.copy(remoteRepos = updated)
            }
        }
    }

    fun installSource(catalog: CatalogSource) {
        viewModelScope.launch {
            sourceCatalog.installSource(catalog)
            val baseUrl = catalog.baseUrl.trimEnd('/')
            _uiState.value = _uiState.value.copy(
                installedBaseUrls = _uiState.value.installedBaseUrls + baseUrl,
                outdatedBaseUrls = _uiState.value.outdatedBaseUrls - baseUrl,
            )
        }
    }

    fun updateSource(catalog: CatalogSource) {
        viewModelScope.launch {
            sourceCatalog.updateSourceConfig(catalog)
            val baseUrl = catalog.baseUrl.trimEnd('/')
            _uiState.value = _uiState.value.copy(
                outdatedBaseUrls = _uiState.value.outdatedBaseUrls - baseUrl,
            )
        }
    }

    fun refresh() {
        loadCatalog()
    }
}
