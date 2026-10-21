package com.restor.gamereel.feature.generator

import com.restor.gamereel.model.*
import kotlin.test.*

class SpinTimelineTest {
    @Test
    fun leverPullDoesNotWaitForReelProgress() {
        for (speed in listOf(AnimationSpeed.FAST, AnimationSpeed.NORMAL)) {
            for (count in listOf(1, 8)) {
                val timeline = SpinTimeline(List(count) { "$it" }, speed.durationMs)
                var pulls = 0
                var lever = 0f
                val motion = SpinMotion({ 0f }, { lever }, { pulls++; true }, { false }, timeline)
                assertEquals(0f, motion.leverProgress)
                assertTrue(motion.pullLever())
                assertEquals(1, pulls)
                lever = 0.5f
                assertEquals(0.5f, motion.leverProgress, 0.0001f)
                assertTrue(motion.running)
            }
        }
    }

    @Test
    fun reelsStartTogetherAndStopInOrderEvery125Milliseconds() {
        for (speed in listOf(AnimationSpeed.FAST, AnimationSpeed.NORMAL)) {
            val timeline = SpinTimeline(listOf("a", "b", "c"), speed.durationMs + 250)
            for (id in timeline.ids) {
                assertEquals(0f, timeline.progress(id, 0f))
                assertTrue(timeline.progress(id, 1f / timeline.durationMs) > 0f)
            }
            timeline.ids.forEachIndexed { index, id ->
                val stopMs = speed.durationMs + index * 125
                assertTrue(timeline.progress(id, (stopMs - 1f) / timeline.durationMs) < 1f)
                assertEquals(1f, timeline.progress(id, stopMs.toFloat() / timeline.durationMs))
            }
        }
    }

    @Test
    fun singleRerollHasNoExtraDelayAndLockedReelsStaySettled() {
        val timeline = SpinTimeline(listOf("b"), 480)
        assertEquals(480, timeline.durationMs)
        val motion = SpinMotion({ 0.5f }, { 0f }, { true }, { false }, timeline)
        assertTrue(motion.running)
        assertTrue(motion.settled(Reel("a", Category.CORE)))
        assertTrue(motion.settled(Reel("b", Category.CORE, state = ReelState.FROZEN)))
        assertEquals(1f, motion.progressFor(Reel("b", Category.CORE, state = ReelState.PINNED)))
        assertFalse(motion.settled(Reel("b", Category.CORE)))
    }

    @Test
    fun resultsSettleIndependentlyWhileOtherReelsKeepRunning() {
        val timeline = SpinTimeline(listOf("a", "b"), 1225)
        val motion = SpinMotion({ 1100f / 1225 }, { 0f }, { true }, { false }, timeline)
        assertTrue(motion.settled(Reel("a", Category.CORE)))
        assertFalse(motion.settled(Reel("b", Category.CORE)))
        assertTrue(motion.running)
    }

    @Test
    fun soundIsBoundedAndHasOneStopPerReel() {
        val timeline = SpinTimeline(listOf("a", "b", "c"), 1350)
        val sounds = SpinSoundTracker(timeline)
        val stops = mutableListOf<Int>()
        var lastSound = -30
        for (time in 0..timeline.durationMs) {
            val event = sounds.advance(time.toFloat() / timeline.durationMs) ?: continue
            if (event) stops.add(time) else assertTrue(time - lastSound >= 30)
            lastSound = time
            assertNull(sounds.advance(time.toFloat() / timeline.durationMs))
        }
        assertEquals(listOf(1100, 1225, 1350), stops)
    }

    @Test
    fun skippedFramesDoNotQueueOldClicksAndNewSpinStartsFresh() {
        val timeline = SpinTimeline(listOf("a", "b"), 1225)
        val sounds = SpinSoundTracker(timeline)
        assertNull(sounds.advance(0f))
        assertEquals(true, sounds.advance(1f))
        assertNull(sounds.advance(1f))
        val restarted = SpinSoundTracker(timeline)
        assertEquals(false, restarted.advance(0.2f))
    }

    @Test
    fun zeroGapSettlesAllReelsTogetherAtTotalDuration() {
        val timeline = SpinTimeline(listOf("a", "b", "c"), 3000, 0)
        assertEquals(3000, timeline.durationMs)
        timeline.ids.forEach { id ->
            assertTrue(timeline.progress(id, 2999f / 3000f) < 1f)
            assertEquals(timeline.progress("a", 0.5f), timeline.progress(id, 0.5f))
            assertEquals(1f, timeline.progress(id, 1f))
        }
    }

    @Test
    fun longGapExtendsTotalRatherThanShorteningRequestedInterval() {
        val timeline = SpinTimeline(listOf("a", "b", "c"), 1000, 1000)
        assertEquals(2250, timeline.durationMs)
        assertEquals(1f, timeline.progress("a", 250f / 2250f))
        assertTrue(timeline.progress("b", 1249f / 2250f) < 1f)
        assertEquals(1f, timeline.progress("b", 1250f / 2250f))
    }
}
