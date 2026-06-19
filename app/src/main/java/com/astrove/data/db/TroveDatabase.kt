package com.astrove.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ScreenshotEntity::class, ScreenshotTextFts::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TroveDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile private var instance: TroveDatabase? = null

        fun get(context: Context): TroveDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                TroveDatabase::class.java,
                "trove.db",
            ).build().also { instance = it }
        }
    }
}
