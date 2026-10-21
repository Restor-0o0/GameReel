package com.restor.gamereel.audio

import androidx.compose.runtime.*

@Composable
internal actual fun rememberPlatformSlotSound(): SlotSound = remember {
    val audio = js("globalThis.gameReelSlotAudio")
    audio?.init()
    object : SlotSound {
        override fun play(stopped: Boolean) {
            audio?.play(stopped)
        }

        override fun close() {
            audio?.close()
        }
    }
}
