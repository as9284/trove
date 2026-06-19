package com.astrove.data

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.astrove.data.classify.Classifier
import com.astrove.data.db.CategoryCount
import com.astrove.data.db.ScreenshotDao
import com.astrove.data.db.ScreenshotEntity
import com.astrove.data.db.ScreenshotTextFts
import com.astrove.data.db.SearchRow
import com.astrove.data.media.ImageHasher
import com.astrove.data.media.MediaStoreSource
import com.astrove.data.prefs.ThemeMode
import com.astrove.data.prefs.TrovePrefs
import com.astrove.work.IndexWorker
import com.astrove.work.OcrWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

data class DupGroup(
    val items: List<ScreenshotEntity>,
    val keepId: Long,
    val reclaimBytes: Long,
)

private const val INDEX_CHUNK = 200

// Bumped when the OCR pipeline changes in a way that makes re-reading the whole
// library worthwhile (e.g. the confidence filter added in v1). On launch, a
// stored version below this triggers a one-time re-OCR of everything.
private const val OCR_PIPELINE_VERSION = 1

class ScreenshotRepository(
    private val appContext: Context,
    private val dao: ScreenshotDao,
    private val mediaStore: MediaStoreSource,
    private val prefs: TrovePrefs,
) {
    fun observeAll(): Flow<List<ScreenshotEntity>> = dao.observeAll()
    fun observe(id: Long): Flow<ScreenshotEntity?> = dao.observe(id)
    fun observeCount(): Flow<Int> = dao.observeCount()
    fun observeOcrPending(): Flow<Int> = dao.observeStatusCount(OcrStatus.PENDING)
    fun observeCategoryCounts(): Flow<List<CategoryCount>> = dao.observeCategoryCounts()
    fun observeByCategory(category: ShotCategory): Flow<List<ScreenshotEntity>> =
        dao.observeByCategory(category)
    fun observePinned(): Flow<List<ScreenshotEntity>> = dao.observePinned()
    val recentSearches: Flow<List<String>> = prefs.recentSearches

    suspend fun get(id: Long): ScreenshotEntity? = dao.get(id)
    suspend fun textFor(id: Long): String? = dao.textFor(id)
    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    fun mediaUri(id: Long): Uri = mediaStore.contentUri(id)

    suspend fun search(query: String): List<SearchRow> {
        val fts = toFtsQuery(query) ?: return emptyList()
        prefs.addRecentSearch(query)
        return runCatching { dao.search(fts, limit = 200) }.getOrDefault(emptyList())
    }

    /** Search without recording it as recent (live-as-you-type). */
    suspend fun searchPreview(query: String): List<SearchRow> {
        val fts = toFtsQuery(query) ?: return emptyList()
        return runCatching { dao.search(fts, limit = 200) }.getOrDefault(emptyList())
    }

    suspend fun recordSearch(query: String) = prefs.addRecentSearch(query)

    /**
     * Imports new screenshots in chunks so a huge first-run library becomes
     * browsable quickly and stays resumable: the watermark advances per chunk,
     * so a killed run picks up where it left off without re-hashing what's done.
     */
    suspend fun reconcile(): Int {
        val since = prefs.watermark.first()
        val items = mediaStore.querySince(since)
        if (items.isEmpty()) return 0
        val resolver = appContext.contentResolver
        var imported = 0
        for (chunk in items.chunked(INDEX_CHUNK)) {
            val entities = chunk.map { item ->
                ScreenshotEntity(
                    mediaId = item.mediaId,
                    uri = item.uri,
                    displayName = item.displayName,
                    dateAdded = item.dateAdded,
                    dateTaken = item.dateTaken,
                    width = item.width,
                    height = item.height,
                    mime = item.mime,
                    bucket = item.bucket,
                    sizeBytes = item.sizeBytes,
                    imageHash = ImageHasher.hash(resolver, Uri.parse(item.uri)),
                )
            }
            dao.upsert(entities)
            prefs.setWatermark(chunk.maxOf { it.dateAdded })
            imported += entities.size
        }
        return imported
    }

    suspend fun pendingForOcr(limit: Int): List<ScreenshotEntity> =
        dao.pending(OcrStatus.PENDING, limit)

    suspend fun saveOcrResult(id: Long, text: String) {
        dao.upsertText(ScreenshotTextFts(id, text))
        dao.setCategoryAndStatus(id, Classifier.classify(text), OcrStatus.DONE)
    }

    suspend fun setOcrStatus(id: Long, status: OcrStatus) = dao.setOcrStatus(id, status)

    /** Mark a shot for re-OCR and kick the workers. */
    suspend fun retryOcr(id: Long) {
        dao.setOcrStatus(id, OcrStatus.PENDING)
        requestIndexing()
    }

    /**
     * Clears the text index and marks every shot pending so the whole library is
     * re-read by the current OCR pipeline. Runs once after an OCR upgrade; the
     * scheduler then drains the pending work newest-first.
     */
    suspend fun migrateOcrPipelineIfNeeded() {
        if (prefs.ocrPipelineVersion.first() >= OCR_PIPELINE_VERSION) return
        dao.clearAllText()
        dao.resetAllOcrStatus(OcrStatus.PENDING)
        prefs.setOcrPipelineVersion(OCR_PIPELINE_VERSION)
    }

    suspend fun resurfacing(limit: Int = 12): List<ScreenshotEntity> {
        val cal = Calendar.getInstance()
        val monthDay = String.format(
            "%02d-%02d",
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
        val year = cal.get(Calendar.YEAR).toString()
        return dao.resurfacing(monthDay, year, limit)
    }

    suspend fun dedupGroups(): List<DupGroup> {
        val all = dao.allForDedup()
        val texts = dao.allTexts().associate { it.mediaId to it.text }
        val visited = BooleanArray(all.size)
        val groups = ArrayList<DupGroup>()
        for (i in all.indices) {
            if (visited[i]) continue
            visited[i] = true
            val cluster = ArrayList<ScreenshotEntity>().apply { add(all[i]) }
            for (j in i + 1 until all.size) {
                if (visited[j]) continue
                if (areDuplicates(all[i], all[j], texts)) {
                    cluster.add(all[j])
                    visited[j] = true
                }
            }
            if (cluster.size >= 2) {
                val keep = cluster.maxByOrNull { it.width.toLong() * it.height + it.sizeBytes }!!
                val reclaim = cluster.filter { it.mediaId != keep.mediaId }.sumOf { it.sizeBytes }
                groups.add(DupGroup(cluster, keep.mediaId, reclaim))
            }
        }
        return groups.sortedByDescending { it.reclaimBytes }
    }

    /**
     * Two shots are near-duplicates only when they share the same dimensions AND
     * their visual hashes are close. When OCR text is available for both, the text
     * must also be near-identical, which stops two sparse, mostly-white screenshots
     * (a chat and a receipt) from being mistaken for the same thing.
     */
    private fun areDuplicates(
        a: ScreenshotEntity,
        b: ScreenshotEntity,
        texts: Map<Long, String>,
    ): Boolean {
        if (a.width != b.width || a.height != b.height) return false
        val distance = ImageHasher.hammingDistance(a.imageHash, b.imageHash)
        if (distance > 10) return false
        val ta = texts[a.mediaId].orEmpty()
        val tb = texts[b.mediaId].orEmpty()
        return if (ta.isNotBlank() && tb.isNotBlank()) {
            textSimilarity(ta, tb) >= 0.6
        } else {
            // No text to disambiguate, so demand a very tight visual match and similar size.
            distance <= 2 && sizeRatio(a.sizeBytes, b.sizeBytes) >= 0.9
        }
    }

    private fun textSimilarity(a: String, b: String): Double {
        fun tokens(s: String) = s.lowercase().split(Regex("""[^\p{L}\p{N}]+"""))
            .filter { it.length >= 2 }.toSet()
        val sa = tokens(a)
        val sb = tokens(b)
        if (sa.isEmpty() && sb.isEmpty()) return 1.0
        val inter = sa.intersect(sb).size.toDouble()
        val union = sa.union(sb).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    private fun sizeRatio(a: Long, b: Long): Double {
        if (a == 0L || b == 0L) return 0.0
        return minOf(a, b).toDouble() / maxOf(a, b).toDouble()
    }

    suspend fun markTrashed(ids: List<Long>) = dao.markTrashed(ids)

    val chargingOnly: Flow<Boolean> = prefs.chargingOnly
    suspend fun setChargingOnly(enabled: Boolean) = prefs.setChargingOnly(enabled)

    val themeMode: Flow<ThemeMode> = prefs.themeMode
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    /** Quick metadata import. OCR is driven separately (foreground or charging). */
    fun requestIndexing() {
        val index = OneTimeWorkRequestBuilder<IndexWorker>()
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("trove-index", ExistingWorkPolicy.KEEP, index)
    }

    /** Background OCR that only runs while charging (used when the setting is on). */
    fun enqueueChargingOcr() {
        val ocr = OneTimeWorkRequestBuilder<OcrWorker>()
            .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("trove-ocr-charging", ExistingWorkPolicy.KEEP, ocr)
    }

    fun cancelChargingOcr() {
        WorkManager.getInstance(appContext).cancelUniqueWork("trove-ocr-charging")
    }

    private fun toFtsQuery(raw: String): String? {
        val tokens = raw.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "$it*" }
    }
}
