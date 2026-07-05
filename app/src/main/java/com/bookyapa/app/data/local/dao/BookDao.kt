package com.bookyapa.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookyapa.app.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE sourceBookUrl = :url")
    suspend fun getBookBySourceUrl(url: String): BookEntity?

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    fun getBooksByStatus(status: com.bookyapa.app.data.model.BookStatus): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC")
    fun getHistory(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("UPDATE books SET lastReadAt = NULL, lastChapterOrder = NULL WHERE id = :bookId")
    suspend fun clearHistoryEntry(bookId: Long)

    @Query("UPDATE books SET lastReadAt = NULL, lastChapterOrder = NULL")
    suspend fun clearAllHistory()

    @Query("UPDATE books SET fontSize = :fontSize WHERE id = :bookId")
    suspend fun updateFontSize(bookId: Long, fontSize: Int)
}
