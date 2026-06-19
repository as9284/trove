package com.astrove.di

import android.content.Context
import com.astrove.data.ScreenshotRepository
import com.astrove.data.db.TroveDatabase
import com.astrove.data.media.MediaStoreSource
import com.astrove.data.ocr.OcrEngine
import com.astrove.data.ocr.OnnxOcrEngine
import com.astrove.data.prefs.TrovePrefs
import com.astrove.work.OcrScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container, built once in [com.astrove.TroveApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = TroveDatabase.get(appContext)
    private val mediaStore = MediaStoreSource(appContext)
    private val prefs = TrovePrefs(appContext)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val ocrEngine: OcrEngine = OnnxOcrEngine(appContext)

    val repository = ScreenshotRepository(
        appContext = appContext,
        dao = database.screenshotDao(),
        mediaStore = mediaStore,
        prefs = prefs,
    )

    private val ocrScheduler = OcrScheduler(repository, ocrEngine, appScope)

    init {
        // One-time re-OCR of the library when the OCR pipeline has been upgraded.
        appScope.launch { repository.migrateOcrPipelineIfNeeded() }
    }

    /** Must be called on the main thread (registers a process-lifecycle observer). */
    fun startOcrScheduler() = ocrScheduler.start()
}
