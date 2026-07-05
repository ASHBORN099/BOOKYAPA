package com.bookyapa.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val enabled: Boolean = true,
    val configJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
)
