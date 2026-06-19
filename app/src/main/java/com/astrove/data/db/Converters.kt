package com.astrove.data.db

import androidx.room.TypeConverter
import com.astrove.data.OcrStatus
import com.astrove.data.ShotCategory

class Converters {
    @TypeConverter
    fun categoryToString(c: ShotCategory): String = c.name

    @TypeConverter
    fun stringToCategory(s: String): ShotCategory =
        runCatching { ShotCategory.valueOf(s) }.getOrDefault(ShotCategory.OTHER)

    @TypeConverter
    fun statusToString(s: OcrStatus): String = s.name

    @TypeConverter
    fun stringToStatus(s: String): OcrStatus =
        runCatching { OcrStatus.valueOf(s) }.getOrDefault(OcrStatus.PENDING)
}
