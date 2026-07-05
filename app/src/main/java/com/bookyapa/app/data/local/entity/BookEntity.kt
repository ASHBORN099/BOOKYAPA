package com.bookyapa.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bookyapa.app.data.model.BookStatus

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val sourceId: Long? = null,
    val sourceBookUrl: String? = null,
    val status: BookStatus = BookStatus.PLAN_TO_READ,
    val rating: Float? = null,
    val lastReadAt: Long? = null,
    val lastChapterOrder: Int? = null,
    val fontSize: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
