package com.restor.gamereel.audio

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

internal interface SlotSound {
    fun play(stopped: Boolean)

    fun close()
}

@Composable internal expect fun rememberPlatformSlotSound(): SlotSound

@Composable
internal fun rememberSlotSound(enabled: Boolean): (Boolean) -> Unit {
    val sound = rememberPlatformSlotSound()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val audible by
        rememberUpdatedState(enabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED))
    DisposableEffect(sound) { onDispose { sound.close() } }
    return remember(sound) { { stopped -> if (audible) sound.play(stopped) } }
}
