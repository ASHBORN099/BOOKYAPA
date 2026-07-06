package com.bookyapa.app.data.model

data class SearchResult(
    val title: String,
    val url: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val sourceName: String = "",
)
