package com.bookyapa.app.data.repository

import com.bookyapa.app.data.local.dao.BookDao
import com.bookyapa.app.data.local.dao.BookmarkDao
import com.bookyapa.app.data.local.dao.BookmarkWithBookInfo
import com.bookyapa.app.data.local.dao.ChapterDao
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.local.entity.BookmarkEntity
import com.bookyapa.app.data.local.entity.ChapterEntity
import com.bookyapa.app.data.model.BookDetail
import com.bookyapa.app.data.model.BookStatus
import com.bookyapa.app.data.model.ChapterItem
import com.bookyapa.app.data.model.SearchResult
import com.bookyapa.app.data.model.SourceConfig
import com.bookyapa.app.data.remote.RemoteDataSource
import com.bookyapa.app.domain.parser.HtmlParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val bookmarkDao: BookmarkDao,
    private val remoteDataSource: RemoteDataSource,
    private val htmlParser: HtmlParser,
    private val sourceRepository: SourceRepository,
) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getHistory(): Flow<List<BookEntity>> = bookDao.getHistory()

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    suspend fun searchBooks(query: String, sourceId: Long): Result<List<SearchResult>> {
        val source = sourceRepository.getSourceById(sourceId) ?: return Result.failure(Exception("Source not found"))
        val config = sourceRepository.getSourceConfig(sourceId) ?: SourceConfig()

        if (config.searchUrlPattern.isBlank()) {
            return Result.failure(Exception("Source has no search configuration"))
        }

        val searchUrl = config.searchUrlPattern.replace("{query}", query)
        val fullUrl = if (searchUrl.startsWith("http")) searchUrl else "${source.baseUrl.trimEnd('/')}$searchUrl"

        return remoteDataSource.fetchHtml(fullUrl).mapCatching { html ->
            val results = htmlParser.searchBooks(html, source.baseUrl, config, source.name)
            if (results.isEmpty()) {
                val hasSelectors = config.searchResultContainer.isNotBlank() ||
                    config.searchResultTitle.isNotBlank()
                if (hasSelectors) {
                    throw Exception("No results found. The source may have changed its layout.")
                }
            }
            results
        }
    }

    suspend fun searchAllSources(query: String): Map<String, Result<List<SearchResult>>> {
        val sources = sourceRepository.getEnabledSourcesOnce()
        return coroutineScope {
            sources.map { source ->
                async {
                    if (isSearchBlocked(source.id, source.name)) {
                        source.name to Result.failure(Exception("Search blocked by Cloudflare — use the Explore tab instead"))
                    } else {
                        val result = searchBooks(query, source.id)
                        source.name to result
                    }
                }
            }.awaitAll().toMap()
        }
    }

    companion object {
        private val SEARCH_BLOCKED_SOURCES = setOf("scribblehub")

        fun isSearchBlocked(sourceId: Long, sourceName: String): Boolean =
            SEARCH_BLOCKED_SOURCES.any { sourceName.lowercase().contains(it) }
    }

    suspend fun fetchExploreBooks(sourceId: Long): Result<List<SearchResult>> {
        val source = sourceRepository.getSourceById(sourceId) ?: return Result.failure(Exception("Source not found"))
        val config = sourceRepository.getSourceConfig(sourceId) ?: SourceConfig()

        val exploreUrl = if (config.exploreUrlPattern.isNotBlank()) config.exploreUrlPattern else source.baseUrl
        val fullUrl = if (exploreUrl.startsWith("http")) exploreUrl else "${source.baseUrl.trimEnd('/')}$exploreUrl"

        val primaryResult = remoteDataSource.fetchHtml(fullUrl).mapCatching { html ->
            val results = htmlParser.searchBooks(html, source.baseUrl, config)
            if (results.isEmpty()) {
                val hasSelectors = config.searchResultContainer.isNotBlank() ||
                    config.searchResultTitle.isNotBlank()
                if (hasSelectors) {
                    throw Exception("No results found. The source may have changed its layout.")
                }
            }
            results
        }

        if (primaryResult.isSuccess || config.exploreUrlPatternFallback.isBlank()) {
            return primaryResult
        }

        val fallbackUrl = config.exploreUrlPatternFallback
        val fullFallbackUrl = if (fallbackUrl.startsWith("http")) fallbackUrl else "${source.baseUrl.trimEnd('/')}$fallbackUrl"

        return remoteDataSource.fetchHtml(fullFallbackUrl).mapCatching { html ->
            val results = htmlParser.searchBooks(html, source.baseUrl, config)
            if (results.isEmpty()) {
                val hasSelectors = config.searchResultContainer.isNotBlank() ||
                    config.searchResultTitle.isNotBlank()
                if (hasSelectors) {
                    throw Exception("No results found. The source may have changed its layout.")
                }
            }
            results
        }
    }

    suspend fun fetchBookDetail(url: String, sourceId: Long): Result<BookDetail> {
        val source = sourceRepository.getSourceById(sourceId) ?: return Result.failure(Exception("Source not found"))
        val config = sourceRepository.getSourceConfig(sourceId) ?: SourceConfig()

        val htmlResult = remoteDataSource.fetchHtml(url)
        if (htmlResult.isFailure) return Result.failure(htmlResult.exceptionOrNull()!!)

        val html = htmlResult.getOrThrow()
        var detail = htmlParser.parseBookDetail(html, source.baseUrl, config)

        if (detail.chapterItems.isEmpty() && config.bookContentUrlPattern.isNotBlank()) {
            val bookId = url.trimEnd('/').substringAfterLast('/')
            val contentUrl = config.bookContentUrlPattern.replace("{id}", bookId)
            detail = detail.copy(chapterItems = listOf(ChapterItem(title = "Full Book", url = contentUrl, order = 0)))
        }

        if (detail.chapterItems.isNotEmpty() && config.chapterListAjaxUrl.isNotBlank()) {
            val seriesId = url.substringAfter("/series/").substringBefore("/").ifEmpty {
                url.trimEnd('/').substringAfterLast('/')
            }
            val postBody = config.chapterListAjaxBody.replace("{id}", seriesId)
            val ajaxResult = remoteDataSource.fetchHtmlPost(config.chapterListAjaxUrl, postBody)
            if (ajaxResult.isSuccess) {
                val allChapters = htmlParser.parseChapterList(ajaxResult.getOrThrow(), source.baseUrl, config)
                if (allChapters.isNotEmpty()) {
                    detail = detail.copy(chapterItems = allChapters)
                }
            }
        }

        return Result.success(detail)
    }

    suspend fun addBookToLibrary(bookDetail: BookDetail, sourceId: Long?, sourceUrl: String?): Long {
        val bookEntity = BookEntity(
            title = bookDetail.title,
            author = bookDetail.author,
            coverUrl = bookDetail.coverUrl,
            description = bookDetail.description,
            sourceId = sourceId,
            sourceBookUrl = sourceUrl,
            status = BookStatus.PLAN_TO_READ,
        )
        val bookId = bookDao.insertBook(bookEntity)

        val chapters = bookDetail.chapterItems.map { item ->
            ChapterEntity(
                bookId = bookId,
                title = item.title,
                url = item.url,
                order = item.order,
            )
        }
        if (chapters.isNotEmpty()) {
            chapterDao.insertChapters(chapters)
        }

        return bookId
    }

    suspend fun downloadChapterContent(chapter: ChapterEntity, sourceId: Long): Result<String> {
        val chapterUrl = chapter.url ?: return Result.failure(Exception("Chapter has no URL"))
        val source = sourceRepository.getSourceById(sourceId) ?: return Result.failure(Exception("Source not found"))
        val config = sourceRepository.getSourceConfig(sourceId) ?: SourceConfig()

        val htmlResult = remoteDataSource.fetchHtml(chapterUrl)
        return htmlResult.map { html ->
            htmlParser.parseChapterContent(html, config)
        }
    }

    suspend fun saveChapterContent(chapterId: Long, content: String) {
        val chapter = chapterDao.getChapterById(chapterId) ?: return
        chapterDao.updateChapter(chapter.copy(content = content))
    }

    suspend fun markChapterRead(chapterId: Long) {
        val chapter = chapterDao.getChapterById(chapterId) ?: return
        chapterDao.updateChapter(chapter.copy(isRead = true))
    }

    suspend fun saveChapterScrollPosition(chapterId: Long, scrollPosition: Int) {
        chapterDao.updateScrollPosition(chapterId, scrollPosition)
    }

    suspend fun getChapterById(chapterId: Long): ChapterEntity? =
        chapterDao.getChapterById(chapterId)

    fun getChaptersForBook(bookId: Long): Flow<List<ChapterEntity>> =
        chapterDao.getChaptersByBookId(bookId)

    suspend fun getChaptersForBookOnce(bookId: Long): List<ChapterEntity> =
        chapterDao.getChaptersByBookIdOnce(bookId)

    suspend fun updateBookStatus(bookId: Long, status: BookStatus) {
        val book = bookDao.getBookById(bookId) ?: return
        bookDao.updateBook(book.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(bookId: Long) = bookDao.deleteBookById(bookId)

    suspend fun clearHistoryEntry(bookId: Long) {
        bookDao.clearHistoryEntry(bookId)
        chapterDao.resetProgressByBookId(bookId)
    }

    suspend fun clearAllHistory() {
        bookDao.clearAllHistory()
        chapterDao.resetAllProgress()
    }

    suspend fun updateBookFontSize(bookId: Long, fontSize: Int) {
        bookDao.updateFontSize(bookId, fontSize)
    }

    suspend fun getChapterCount(bookId: Long): Int = chapterDao.getChapterCount(bookId)

    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksByBookId(bookId)

    fun getAllBookmarks(): Flow<List<BookmarkWithBookInfo>> =
        bookmarkDao.getAllBookmarksWithBookInfo()

    suspend fun deleteBookmarkById(id: Long) = bookmarkDao.deleteById(id)

    fun isBookmarked(bookId: Long, chapterOrder: Int): Flow<Boolean> =
        bookmarkDao.isBookmarked(bookId, chapterOrder)

    suspend fun toggleBookmark(
        bookId: Long,
        chapterOrder: Int,
        chapterTitle: String,
        pageIndex: Int,
        text: String,
    ): Boolean {
        val existing = bookmarkDao.getBookmark(bookId, chapterOrder)
        return if (existing != null) {
            bookmarkDao.deleteByChapter(bookId, chapterOrder)
            false
        } else {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = bookId,
                    chapterOrder = chapterOrder,
                    chapterTitle = chapterTitle,
                    pageIndex = pageIndex,
                    text = text,
                )
            )
            true
        }
    }
}
