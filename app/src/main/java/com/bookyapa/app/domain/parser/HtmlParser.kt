package com.bookyapa.app.domain.parser

import com.bookyapa.app.data.model.BookDetail
import com.bookyapa.app.data.model.ChapterItem
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.data.model.SourceConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlParser @Inject constructor() {

    fun searchBooks(html: String, baseUrl: String, config: SourceConfig): List<SearchResult> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        val containers = if (config.searchResultContainer.isNotBlank()) {
            doc.select(config.searchResultContainer)
        } else {
            Elements().apply { add(doc.body()) }
        }

        for (container in containers) {
            val titleEl = selectFirstScoped(container, config.searchResultTitle)
            val linkEl = selectFirstScoped(container, config.searchResultLink)
            val coverEl = if (config.searchResultCover.isNotBlank()) {
                container.selectFirst(config.searchResultCover)
            } else null

            val title = titleEl?.text()?.trim()
            if (title.isNullOrBlank()) continue
            val href = linkEl?.attr("href")
            if (href.isNullOrBlank()) continue
            val url = resolveUrl(href, baseUrl)
            val coverUrl = coverEl?.let { resolveUrl(it.attr("src"), baseUrl) }
                ?.replace(".cover.small.jpg", ".cover.medium.jpg")

            results.add(SearchResult(title = title, url = url, coverUrl = coverUrl))
        }

        return results
    }

    private fun selectFirstScoped(root: Element, cssQuery: String): Element? {
        if (cssQuery.isBlank()) return null
        val all = root.select(cssQuery)
        if (all.isEmpty()) return null
        val textMatch = all.firstOrNull { !it.text().isNullOrBlank() }
        return textMatch ?: all.first()
    }

    fun parseBookDetail(html: String, baseUrl: String, config: SourceConfig): BookDetail {
        val doc = Jsoup.parse(html)

        val title = if (config.bookTitle.isNotBlank()) {
            doc.selectFirst(config.bookTitle)?.text()?.trim() ?: ""
        } else {
            doc.title().trim()
        }

        val author = if (config.bookAuthor.isNotBlank()) {
            doc.selectFirst(config.bookAuthor)?.text()?.trim()
                ?.replace(Regex(",\\s*\\d{4}(-\\d{4})?\\s*$"), "")
        } else null

        val coverUrl = if (config.bookCover.isNotBlank()) {
            doc.selectFirst(config.bookCover)?.let {
                resolveUrl(it.attr("src"), baseUrl)
            }
        } else null

        val description = if (config.bookDescription.isNotBlank()) {
            doc.selectFirst(config.bookDescription)?.text()?.trim()
        } else null

        val chapterItems = parseChapterList(html, baseUrl, config)

        return BookDetail(
            title = title,
            author = author,
            coverUrl = coverUrl,
            description = description,
            chapterItems = chapterItems,
        )
    }

    fun parseChapterList(html: String, baseUrl: String, config: SourceConfig): List<ChapterItem> {
        val doc = Jsoup.parse(html)
        val chapters = mutableListOf<ChapterItem>()

        if (config.chapterListItem.isBlank()) return chapters

        val items = doc.select(config.chapterListItem)
        for ((index, item) in items.withIndex()) {
            val titleEl = if (config.chapterTitle.isNotBlank()) {
                item.selectFirst(config.chapterTitle)
            } else item
            val linkEl = if (config.chapterLink.isNotBlank()) {
                item.selectFirst(config.chapterLink)
            } else item.selectFirst("a") ?: item

            val title = titleEl?.text()?.trim() ?: continue
            val href = linkEl.attr("href")
            if (href.isBlank()) continue
            val url = resolveUrl(href, baseUrl)

            chapters.add(ChapterItem(title = title, url = url, order = index))
        }

        return chapters
    }

    fun parseChapterContent(html: String, config: SourceConfig): String {
        if (config.chapterContent.isBlank()) return ""
        val doc = Jsoup.parse(html)
        val contentEl = doc.selectFirst(config.chapterContent) ?: return ""
        return contentEl.html()
    }

    fun findNextPageUrl(html: String, config: SourceConfig): String? {
        if (config.chapterNextPage.isBlank()) return null
        val doc = Jsoup.parse(html)
        return doc.selectFirst(config.chapterNextPage)?.attr("href")?.takeIf { it.isNotBlank() }
    }

    fun extractTextFromHtml(html: String): String {
        return Jsoup.parse(html).text()
    }

    private fun resolveUrl(href: String, baseUrl: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        val base = baseUrl.trimEnd('/')
        return when {
            href.startsWith("/") -> "$base$href"
            else -> "$base/$href"
        }
    }
}
