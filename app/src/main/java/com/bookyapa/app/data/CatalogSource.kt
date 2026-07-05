package com.bookyapa.app.data

import com.bookyapa.app.data.model.SourceConfig
import kotlinx.serialization.Serializable

@Serializable
data class CatalogSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val lang: String = "en",
    val description: String = "",
    val config: SourceConfig = SourceConfig(),
)

@Serializable
data class SourceRepo(
    val name: String,
    val sources: List<CatalogSource>,
)
