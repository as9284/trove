package com.astrove.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.astrove.data.OcrStatus
import com.astrove.data.ShotCategory
import kotlinx.coroutines.flow.Flow

data class CategoryCount(val category: ShotCategory, val count: Int)

data class TextRow(val mediaId: Long, val text: String)

data class SearchRow(
    @Embedded val shot: ScreenshotEntity,
    val ocrText: String,
)

@Dao
interface ScreenshotDao {

    @Upsert
    suspend fun upsert(items: List<ScreenshotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertText(text: ScreenshotTextFts)

    @Query("UPDATE screenshots SET ocrStatus = :status WHERE mediaId = :id")
    suspend fun setOcrStatus(id: Long, status: OcrStatus)

    @Query("UPDATE screenshots SET ocrStatus = :status WHERE trashed = 0")
    suspend fun resetAllOcrStatus(status: OcrStatus)

    @Query("DELETE FROM screenshot_text")
    suspend fun clearAllText()

    @Query("UPDATE screenshots SET category = :category, ocrStatus = :status WHERE mediaId = :id")
    suspend fun setCategoryAndStatus(id: Long, category: ShotCategory, status: OcrStatus)

    @Query("UPDATE screenshots SET pinned = :pinned WHERE mediaId = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE screenshots SET trashed = 1 WHERE mediaId IN (:ids)")
    suspend fun markTrashed(ids: List<Long>)

    @Query("DELETE FROM screenshots WHERE mediaId IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT * FROM screenshots WHERE trashed = 0 ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE mediaId = :id")
    fun observe(id: Long): Flow<ScreenshotEntity?>

    @Query("SELECT * FROM screenshots WHERE mediaId = :id")
    suspend fun get(id: Long): ScreenshotEntity?

    @Query("SELECT text FROM screenshot_text WHERE rowid = :id")
    suspend fun textFor(id: Long): String?

    @Query("SELECT COUNT(*) FROM screenshots WHERE trashed = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE trashed = 0 AND ocrStatus = :status")
    fun observeStatusCount(status: OcrStatus): Flow<Int>

    @Query(
        "SELECT category, COUNT(*) AS count FROM screenshots " +
            "WHERE trashed = 0 GROUP BY category ORDER BY count DESC"
    )
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query(
        "SELECT * FROM screenshots WHERE trashed = 0 AND category = :category ORDER BY dateAdded DESC"
    )
    fun observeByCategory(category: ShotCategory): Flow<List<ScreenshotEntity>>

    @Query(
        "SELECT * FROM screenshots WHERE trashed = 0 AND ocrStatus = :status " +
            "ORDER BY dateAdded DESC LIMIT :limit"
    )
    suspend fun pending(status: OcrStatus, limit: Int): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE trashed = 0 AND pinned = 1 ORDER BY dateAdded DESC")
    fun observePinned(): Flow<List<ScreenshotEntity>>

    @Query(
        "SELECT * FROM screenshots WHERE trashed = 0 AND imageHash != 0 ORDER BY imageHash"
    )
    suspend fun allForDedup(): List<ScreenshotEntity>

    @Query("SELECT MAX(dateAdded) FROM screenshots")
    suspend fun maxDateAdded(): Long?

    @Query("SELECT rowid AS mediaId, text FROM screenshot_text")
    suspend fun allTexts(): List<TextRow>

    @Query(
        "SELECT * FROM screenshots WHERE trashed = 0 " +
            "AND strftime('%m-%d', dateAdded, 'unixepoch') = :monthDay " +
            "AND strftime('%Y', dateAdded, 'unixepoch') < :year " +
            "ORDER BY dateAdded DESC LIMIT :limit"
    )
    suspend fun resurfacing(monthDay: String, year: String, limit: Int): List<ScreenshotEntity>

    @Query(
        "SELECT s.*, t.text AS ocrText FROM screenshots s " +
            "JOIN screenshot_text t ON s.mediaId = t.rowid " +
            "WHERE t.text MATCH :ftsQuery AND s.trashed = 0 " +
            "ORDER BY s.dateAdded DESC LIMIT :limit"
    )
    suspend fun search(ftsQuery: String, limit: Int): List<SearchRow>
}
