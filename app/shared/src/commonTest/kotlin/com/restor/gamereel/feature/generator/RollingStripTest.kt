package com.restor.gamereel.feature.generator

import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RollingStripTest {
    @Test
    fun fixedSlotsCoverTheViewportWithoutChangingTheirContent() {
        for (count in listOf(3, 5, 6)) {
            for (step in 0..120) {
                val phase = step / 10f
                val first = floor(phase).toInt() - 1
                val indices = (0 until count).map { rollingSlotIndex(it, phase, count) }

                assertEquals((first until first + count).toList(), indices.sorted())
                indices.forEachIndexed { slot, index ->
                    assertEquals(slot, ((index % count) + count) % count)
                }
            }
        }
    }

    @Test
    fun recyclingHappensOutsideTheVisibleWindow() {
        for (boundary in 1..SpinSteps) {
            for (slot in 0 until 5) {
                val before = rollingSlotIndex(slot, boundary - 0.001f, 5)
                val after = rollingSlotIndex(slot, boundary.toFloat(), 5)
                if (before != after) {
                    assertTrue(before <= boundary - 2)
                    assertTrue(after >= boundary + 2)
                }
            }
        }
    }

    @Test
    fun finalResultHasNoPlaceholderOverIt() {
        val phase = SpinSteps.toFloat()
        for (slot in 0 until 5) {
            val index = rollingSlotIndex(slot, phase, 5)
            assertTrue(index < SpinSteps && index - phase <= -1f || index >= SpinSteps)
        }
        assertEquals(0f, SpinSteps - phase)
    }
}
