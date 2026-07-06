package com.bookyapa.app.data.repository

import com.bookyapa.app.data.local.dao.SourceDao
import com.bookyapa.app.data.local.entity.SourceEntity
import com.bookyapa.app.data.model.SourceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepository @Inject constructor(
    private val sourceDao: SourceDao,
    private val json: Json,
) {

    fun getAllSources(): Flow<List<SourceEntity>> = sourceDao.getAllSources()

    fun getEnabledSources(): Flow<List<SourceEntity>> = sourceDao.getEnabledSources()

    suspend fun getEnabledSourcesOnce(): List<SourceEntity> = sourceDao.getEnabledSourcesOnce()

    suspend fun getAllSourcesOnce(): List<SourceEntity> = sourceDao.getAllSourcesList()

    suspend fun getSourceById(id: Long): SourceEntity? = sourceDao.getSourceById(id)

    suspend fun getSourceConfig(id: Long): SourceConfig? {
        val source = sourceDao.getSourceById(id) ?: return null
        return decodeConfig(source.configJson)
    }

    suspend fun saveSource(source: SourceEntity): Long = sourceDao.insertSource(source)

    suspend fun updateSource(source: SourceEntity) = sourceDao.updateSource(source)

    suspend fun deleteSource(source: SourceEntity) = sourceDao.deleteSource(source)

    suspend fun deleteSourceById(id: Long) = sourceDao.deleteSourceById(id)

    fun encodeConfig(config: SourceConfig): String = json.encodeToString(SourceConfig.serializer(), config)

    fun decodeConfig(configJson: String): SourceConfig {
        return try {
            json.decodeFromString(SourceConfig.serializer(), configJson)
        } catch (_: Exception) {
            SourceConfig()
        }
    }
}
