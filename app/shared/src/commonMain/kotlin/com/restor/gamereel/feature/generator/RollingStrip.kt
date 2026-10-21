package com.restor.gamereel.feature.generator

import kotlin.math.floor

// Reuse a fixed strip, keeping one item above the viewport and the rest below it.
// Its length must be at least three and a multiple of the source pool's length.
internal fun rollingSlotIndex(slot: Int, phase: Float, count: Int): Int {
    val first = floor(phase).toInt() - 1
    return first + ((slot - first) % count + count) % count
}
