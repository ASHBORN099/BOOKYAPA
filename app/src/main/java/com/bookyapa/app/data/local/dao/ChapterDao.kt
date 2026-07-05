package com.bookyapa.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bookyapa.app.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` ASC")
    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `order` ASC")
    suspend fun getChaptersByBookIdOnce(bookId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `order` = :order")
    suspend fun getChapterByOrder(bookId: Long, order: Int): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCount(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Long)

    @Query("UPDATE chapters SET lastScrollPosition = :scrollPosition WHERE id = :id")
    suspend fun updateScrollPosition(id: Long, scrollPosition: Int)

    @Query("UPDATE chapters SET isRead = 0, lastScrollPosition = 0 WHERE bookId = :bookId")
    suspend fun resetProgressByBookId(bookId: Long)

    @Query("UPDATE chapters SET isRead = 0, lastScrollPosition = 0")
    suspend fun resetAllProgress()
}
