package com.bookyapa.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceConfig(
    val searchUrlPattern: String = "",
    val searchResultContainer: String = "",
    val searchResultTitle: String = "",
    val searchResultLink: String = "",
    val searchResultCover: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookCover: String = "",
    val bookDescription: String = "",
    val chapterListItem: String = "",
    val chapterTitle: String = "",
    val chapterLink: String = "",
    val chapterContent: String = "",
    val chapterNextPage: String = "",
    val exploreUrlPattern: String = "",
    val exploreUrlPatternFallback: String = "",
    val bookContentUrlPattern: String = "",
    val chapterListAjaxUrl: String = "",
    val chapterListAjaxBody: String = "",
)
