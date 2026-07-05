package com.bookyapa.app.data.repository

import com.bookyapa.app.data.local.dao.BookDao
import com.bookyapa.app.data.local.dao.ChapterDao
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.local.entity.ChapterEntity
import com.bookyapa.app.data.model.BookStatus
import com.bookyapa.app.data.model.EpubBook
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
) {

    suspend fun importEpub(epubBook: EpubBook, filesDir: File, onProgress: (String) -> Unit = {}): Long {
        val coverUrl = epubBook.coverBytes?.let { bytes ->
            val coversDir = File(filesDir, "covers").also { it.mkdirs() }
            val tempId = System.currentTimeMillis()
            val coverFile = File(coversDir, "epub_$tempId.jpg")
            coverFile.writeBytes(bytes)
            coverFile.toURI().toString()
        }

        onProgress("Saving ${epubBook.title}...")

        val bookId = bookDao.insertBook(
            BookEntity(
                title = epubBook.title,
                author = epubBook.author,
                coverUrl = coverUrl,
                sourceId = null,
                status = BookStatus.PLAN_TO_READ,
            )
        )

        val chapters = epubBook.chapters.mapIndexed { index, chapter ->
            ChapterEntity(
                bookId = bookId,
                title = chapter.title,
                content = chapter.contentHtml,
                order = index,
                isRead = false,
            )
        }

        if (chapters.isNotEmpty()) {
            chapterDao.insertChapters(chapters)
        }

        onProgress("Imported ${epubBook.title} (${chapters.size} chapters)")

        return bookId
    }
}
