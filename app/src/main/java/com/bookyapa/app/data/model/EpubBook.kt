package com.bookyapa.app.data.model

data class EpubBook(
    val title: String,
    val author: String? = null,
    val coverBytes: ByteArray? = null,
    val chapters: List<EpubChapter> = emptyList(),
)

data class EpubChapter(
    val title: String,
    val contentHtml: String,
)
