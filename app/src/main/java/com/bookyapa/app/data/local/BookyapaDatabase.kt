package com.bookyapa.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bookyapa.app.data.local.dao.BookDao
import com.bookyapa.app.data.local.dao.BookmarkDao
import com.bookyapa.app.data.local.dao.ChapterDao
import com.bookyapa.app.data.local.dao.SourceDao
import com.bookyapa.app.data.local.entity.BookEntity
import com.bookyapa.app.data.local.entity.BookmarkEntity
import com.bookyapa.app.data.local.entity.ChapterEntity
import com.bookyapa.app.data.local.entity.SourceEntity

@Database(
    entities = [BookEntity::class, ChapterEntity::class, SourceEntity::class, BookmarkEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BookyapaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun sourceDao(): SourceDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `chapterOrder` INTEGER NOT NULL,
                        `chapterTitle` TEXT NOT NULL DEFAULT '',
                        `pageIndex` INTEGER NOT NULL DEFAULT 0,
                        `text` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (`bookId`) REFERENCES `books`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `books` ADD COLUMN `fontSize` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
