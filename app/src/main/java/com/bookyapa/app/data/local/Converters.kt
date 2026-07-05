package com.bookyapa.app.data.local

import androidx.room.TypeConverter
import com.bookyapa.app.data.model.BookStatus

class Converters {

    @TypeConverter
    fun fromBookStatus(status: BookStatus): String = status.name

    @TypeConverter
    fun toBookStatus(value: String): BookStatus = try {
        BookStatus.valueOf(value)
    } catch (_: Exception) {
        BookStatus.PLAN_TO_READ
    }
}
