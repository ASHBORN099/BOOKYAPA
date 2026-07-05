package com.bookyapa.app.di

import android.content.Context
import androidx.room.Room
import com.bookyapa.app.data.local.BookyapaDatabase
import com.bookyapa.app.data.local.dao.BookDao
import com.bookyapa.app.data.local.dao.BookmarkDao
import com.bookyapa.app.data.local.dao.ChapterDao
import com.bookyapa.app.data.local.dao.SourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BookyapaDatabase {
        return Room.databaseBuilder(
            context,
            BookyapaDatabase::class.java,
            "bookyapa.db",
        )
            .addMigrations(BookyapaDatabase.MIGRATION_1_2, BookyapaDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBookDao(database: BookyapaDatabase): BookDao = database.bookDao()

    @Provides
    fun provideChapterDao(database: BookyapaDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideSourceDao(database: BookyapaDatabase): SourceDao = database.sourceDao()

    @Provides
    fun provideBookmarkDao(database: BookyapaDatabase): BookmarkDao = database.bookmarkDao()
}
