package com.restor.gamereel.feature.generator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.restor.gamereel.audio.rememberSlotSound
import com.restor.gamereel.model.*

// Read progress lazily: subscribing the screen to the clock would recompose it every frame.
@Stable
class SpinMotion
internal constructor(
    private val value: () -> Float,
    private val leverValue: () -> Float,
    private val requestLeverPull: () -> Boolean,
    private val leverIsBusy: () -> Boolean,
    private val timeline: SpinTimeline,
) {
    val ids: Set<String> = timeline.ids.toSet()
    private val finished = derivedStateOf { value() >= 1f }
    private val settledReels =
        timeline.ids.associateWith { id ->
            derivedStateOf { timeline.progress(id, value()) >= 1f }
        }

    val progress: Float
        get() = value()

    val leverProgress: Float
        get() = leverValue()

    val leverBusy: Boolean
        get() = leverIsBusy()

    fun pullLever(): Boolean = requestLeverPull()

    val running: Boolean
        get() = ids.isNotEmpty() && !finished.value

    fun animates(reel: Reel) = reel.id in ids && reel.state == ReelState.RANDOM

    fun settled(reel: Reel) = !animates(reel) || settledReels.getValue(reel.id).value

    fun progressFor(reel: Reel) = if (animates(reel)) timeline.progress(reel.id, value()) else 1f
}

@Composable
fun rememberSpinMotion(
    spinRevision: Long,
    animatedReelIds: Set<String>,
    speed: AnimationSpeed,
    requestedDurationMs: Int,
    stopGapMs: Int,
    reels: List<Reel>,
    soundEnabled: Boolean,
): SpinMotion {
    val animation = remember { Animatable(1f) }
    val leverAnimation = remember { Animatable(0f) }
    var consumedRevision by remember { mutableLongStateOf(spinRevision) }
    var leverRequests by remember { mutableIntStateOf(0) }
    var leverBusy by remember { mutableStateOf(false) }
    val revision by rememberUpdatedState(spinRevision)
    val currentSpeed by rememberUpdatedState(speed)
    val playSound = rememberSlotSound(soundEnabled)
    val currentPlaySound by rememberUpdatedState(playSound)
    val timeline =
        remember(spinRevision, animatedReelIds, speed, requestedDurationMs, stopGapMs) {
            SpinTimeline(
                reels
                    .filter { it.id in animatedReelIds && it.state == ReelState.RANDOM }
                    .map { it.id },
                requestedDurationMs,
                stopGapMs,
            )
        }

    // A new spin cancels the old clock and its sound events together.
    LaunchedEffect(spinRevision, speed, animatedReelIds, requestedDurationMs, stopGapMs) {
        val newSpin = consumedRevision != spinRevision
        consumedRevision = spinRevision
        if (speed == AnimationSpeed.OFF || timeline.ids.isEmpty() || !newSpin) {
            animation.snapTo(1f)
        } else {
            val sounds = SpinSoundTracker(timeline)
            animation.snapTo(0f)
            animation.animateTo(1f, tween(timeline.durationMs, easing = LinearEasing)) {
                sounds.advance(value)?.let { currentPlaySound(it) }
            }
        }
    }

    // The lever responds to the gesture, not to the later repository commit.
    LaunchedEffect(leverRequests) {
        if (leverRequests > 0) {
            try {
                leverAnimation.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                leverAnimation.animateTo(0f, tween(220, easing = LinearOutSlowInEasing))
            } finally {
                leverBusy = false
            }
        }
    }

    return remember(timeline) {
        SpinMotion(
            {
                if (currentSpeed == AnimationSpeed.OFF) 1f
                else if (consumedRevision != revision) 0f else animation.value
            },
            { leverAnimation.value },
            {
                if (leverBusy) false
                else {
                    leverBusy = true
                    leverRequests++
                    true
                }
            },
            { leverBusy },
            timeline,
        )
    }
}
