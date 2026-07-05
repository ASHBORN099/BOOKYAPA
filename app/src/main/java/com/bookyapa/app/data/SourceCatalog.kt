package com.bookyapa.app.data

import android.content.Context
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SourceConfig
import com.bookyapa.app.data.remote.RemoteDataSource
import com.bookyapa.app.data.repository.SourceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceRepository: SourceRepository,
    private val remoteDataSource: RemoteDataSource,
    private val json: Json,
) {
    fun getBundledSources(): List<CatalogSource> {
        return try {
            val jsonText = context.assets.open("sources_catalog.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<List<CatalogSource>>(jsonText)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchRemoteSources(repoUrl: String): Result<SourceRepo> {
        return remoteDataSource.fetchHtml(repoUrl).mapCatching { text ->
            json.decodeFromString<SourceRepo>(text)
        }
    }

    suspend fun installSource(catalog: CatalogSource): Long {
        val entity = SourceEntity(
            name = catalog.name,
            baseUrl = catalog.baseUrl.trimEnd('/'),
            enabled = true,
            configJson = sourceRepository.encodeConfig(catalog.config),
        )
        return sourceRepository.saveSource(entity)
    }

    suspend fun getInstalledBaseUrls(): Set<String> {
        val all = sourceRepository.getAllSourcesOnce()
        return all.map { it.baseUrl.trimEnd('/') }.toSet()
    }

    suspend fun getInstalledSources(): List<SourceEntity> {
        return sourceRepository.getAllSourcesOnce()
    }

    suspend fun updateSourceConfig(catalog: CatalogSource) {
        val all = sourceRepository.getAllSourcesOnce()
        val installed = all.find { it.baseUrl == catalog.baseUrl.trimEnd('/') } ?: return
        val updated = installed.copy(
            name = catalog.name,
            configJson = sourceRepository.encodeConfig(catalog.config),
        )
        sourceRepository.updateSource(updated)
    }

    fun encodeConfig(config: SourceConfig): String {
        return sourceRepository.encodeConfig(config)
    }
}
