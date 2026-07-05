package com.bookyapa.app.data.model

data class BookDetail(
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val chapterItems: List<ChapterItem> = emptyList(),
)
