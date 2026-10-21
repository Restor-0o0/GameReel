package com.restor.gamereel.feature.generator

import androidx.compose.animation.core.LinearOutSlowInEasing

internal class SpinTimeline(
    val ids: List<String>,
    requestedDurationMs: Int,
    stopGapMs: Int = DefaultStopGapMs,
) {
    private val gapMs = stopGapMs.coerceAtLeast(0)
    val durationMs =
        if (ids.isEmpty()) 0
        else actualDurationMs(requestedDurationMs, gapMs, ids.size)
    private val stops = ids.mapIndexed { index, id ->
        id to (durationMs - (ids.lastIndex - index) * gapMs)
    }.toMap()

    fun progress(id: String, fraction: Float): Float {
        val stop = stops[id] ?: return 1f
        if (stop <= 0 || fraction >= 1f) return 1f
        val elapsed = fraction * durationMs
        if (elapsed >= stop) return 1f
        return LinearOutSlowInEasing.transform((elapsed / stop).coerceIn(0f, 1f))
            .coerceAtMost(0.999f)
    }

    companion object {
        const val DefaultStopGapMs = 125
        private const val MinFirstStopMs = 250

        fun actualDurationMs(requestedDurationMs: Int, stopGapMs: Int, reelCount: Int): Int =
            if (reelCount <= 0) 0
            else requestedDurationMs.coerceAtLeast(
                MinFirstStopMs + (reelCount - 1) * stopGapMs.coerceAtLeast(0)
            )
    }
}

// Boolean identifies the heavier stop click. Missed frames never produce a burst of old ticks.
internal class SpinSoundTracker(private val timeline: SpinTimeline) {
    private val steps = IntArray(timeline.ids.size)
    private var lastTickMs = -30f

    fun advance(fraction: Float): Boolean? {
        var moved = false
        var stopped = false
        timeline.ids.forEachIndexed { index, id ->
            val step = (timeline.progress(id, fraction) * SpinSteps).toInt()
            if (step > steps[index]) {
                moved = true
                stopped = stopped || step == SpinSteps
                steps[index] = step
            }
        }
        val elapsed = fraction * timeline.durationMs
        if (!stopped && (!moved || elapsed - lastTickMs < 30f)) return null
        lastTickMs = elapsed
        return stopped
    }
}
