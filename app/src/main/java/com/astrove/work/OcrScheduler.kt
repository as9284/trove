package com.astrove.work

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.astrove.data.ScreenshotRepository
import com.astrove.data.ocr.OcrEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives OCR from the app's foreground lifecycle by default: it drains pending
 * work (newest-first) only while Trove is open, and stops the moment it's
 * backgrounded, so a large library never grinds silently in the
 * background. When the "only while charging" setting is on, it hands OCR to a
 * charging-constrained WorkManager job instead.
 */
class OcrScheduler(
    private val repo: ScreenshotRepository,
    private val engine: OcrEngine,
    private val scope: CoroutineScope,
) {
    @Volatile private var foreground = false
    private var drainJob: Job? = null

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> { foreground = true; ensureDrain() }
                    Lifecycle.Event.ON_STOP -> { foreground = false; drainJob?.cancel() }
                    else -> Unit
                }
            },
        )
        // New pending work appears (after indexing) → drain or defer to charging.
        scope.launch {
            repo.observeOcrPending().collect { pending ->
                if (pending <= 0) return@collect
                if (repo.chargingOnly.first()) repo.enqueueChargingOcr() else ensureDrain()
            }
        }
        // React to the setting flipping at runtime.
        scope.launch {
            repo.chargingOnly.collect { onlyCharging ->
                if (onlyCharging) {
                    drainJob?.cancel()
                    repo.enqueueChargingOcr()
                } else {
                    repo.cancelChargingOcr()
                    ensureDrain()
                }
            }
        }
    }

    private fun ensureDrain() {
        if (!foreground || drainJob?.isActive == true) return
        drainJob = scope.launch {
            if (repo.chargingOnly.first()) return@launch
            // Mid-batch cancellation propagates through the suspend OCR calls; the
            // predicate only needs to stop the loop once we're backgrounded.
            drainOcr(repo, engine) { foreground }
        }
    }
}
