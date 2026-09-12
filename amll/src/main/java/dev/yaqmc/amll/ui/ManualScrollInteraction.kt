package dev.yaqmc.amll.ui

import kotlin.math.abs

/** Mirrors upstream AMLL's scroll input distinction. */
internal enum class ManualScrollInputType {
    Touch,
    Wheel,
}

/** Upstream confirms touch scrolling only after either axis moves strictly more than 10 px. */
internal const val TOUCH_SCROLL_INTENT_THRESHOLD_PX = 10f

/** Upstream debounces wheel interaction end for 150 ms before the auto-align timer can start. */
internal const val WHEEL_IDLE_TIMEOUT_MS = 150L

/**
 * Pure touch-intent tracker used by the Compose pointer observer.
 *
 * It deliberately does not consume pointer events or perform scrolling. LazyColumn remains the owner
 * of drag/fling physics; this tracker only decides when AMLL's auto-follow should be suspended.
 */
internal class TouchScrollIntentTracker(
    private val thresholdPx: Float = TOUCH_SCROLL_INTENT_THRESHOLD_PX,
) {
    private var startX = 0f
    private var startY = 0f
    private var tracking = false

    var isConfirmed: Boolean = false
        private set

    fun onDown(x: Float, y: Float) {
        startX = x
        startY = y
        tracking = true
        isConfirmed = false
    }

    /** Re-anchor a multi-touch gesture without changing an already-confirmed interaction. */
    fun reanchor(x: Float, y: Float) {
        startX = x
        startY = y
        tracking = true
    }

    /** Returns true only for the transition from undecided -> confirmed. */
    fun onMove(x: Float, y: Float): Boolean {
        if (!tracking || isConfirmed) return false
        if (abs(x - startX) <= thresholdPx && abs(y - startY) <= thresholdPx) return false

        isConfirmed = true
        return true
    }

    fun onUpOrCancel() {
        tracking = false
        isConfirmed = false
    }
}
