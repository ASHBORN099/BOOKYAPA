package com.bookyapa.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bookyapa.app.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

data class BookmarkWithBookInfo(
    val id: Long,
    val bookId: Long,
    val chapterOrder: Int,
    val chapterTitle: String,
    val pageIndex: Int,
    val text: String,
    val createdAt: Long,
    val bookTitle: String,
    val bookAuthor: String?,
    val bookCoverUrl: String?,
)

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksByBookId(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND chapterOrder = :chapterOrder LIMIT 1")
    suspend fun getBookmark(bookId: Long, chapterOrder: Int): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE bookId = :bookId AND chapterOrder = :chapterOrder)")
    fun isBookmarked(bookId: Long, chapterOrder: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND chapterOrder = :chapterOrder")
    suspend fun deleteByChapter(bookId: Long, chapterOrder: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT b.id, b.bookId, b.chapterOrder, b.chapterTitle, b.pageIndex, b.text, b.createdAt,
               bk.title AS bookTitle, bk.author AS bookAuthor, bk.coverUrl AS bookCoverUrl
        FROM bookmarks b
        INNER JOIN books bk ON b.bookId = bk.id
        ORDER BY b.createdAt DESC
    """)
    fun getAllBookmarksWithBookInfo(): Flow<List<BookmarkWithBookInfo>>
}
