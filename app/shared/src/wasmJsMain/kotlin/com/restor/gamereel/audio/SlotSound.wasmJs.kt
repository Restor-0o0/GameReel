@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.restor.gamereel.audio

import androidx.compose.runtime.*

@JsFun("() => globalThis.gameReelSlotAudio?.init()") private external fun initSound()

@JsFun("(stopped) => globalThis.gameReelSlotAudio?.play(stopped)")
private external fun playSound(stopped: Boolean)

@JsFun("() => globalThis.gameReelSlotAudio?.close()") private external fun closeSound()

@Composable
internal actual fun rememberPlatformSlotSound(): SlotSound = remember {
    initSound()
    object : SlotSound {
        override fun play(stopped: Boolean) = playSound(stopped)

        override fun close() = closeSound()
    }
}
