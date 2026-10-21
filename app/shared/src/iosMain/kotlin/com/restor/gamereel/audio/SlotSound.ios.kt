@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.restor.gamereel.audio

import androidx.compose.runtime.*
import gamereel.app.shared.generated.resources.Res
import kotlinx.cinterop.*
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@Composable
internal actual fun rememberPlatformSlotSound(): SlotSound {
    val sound = remember { AppleSlotSound() }
    LaunchedEffect(sound) {
        sound.load(
            Res.readBytes("files/audio/slot_tick.wav"),
            Res.readBytes("files/audio/slot_stop.wav"),
        )
    }
    return sound
}

private class AppleSlotSound : SlotSound {
    private var players = emptyList<AVAudioPlayer?>()

    fun load(tick: ByteArray, stop: ByteArray) {
        players =
            listOf(tick, stop).map { bytes ->
                bytes
                    .usePinned {
                        AVAudioPlayer(
                            NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()),
                            null,
                        )
                    }
                    .apply {
                        volume = 0.45f
                        prepareToPlay()
                    }
            }
    }

    override fun play(stopped: Boolean) {
        players.getOrNull(if (stopped) 1 else 0)?.let {
            it.currentTime = 0.0
            it.play()
        }
    }

    override fun close() {
        players.forEach { it?.stop() }
        players = emptyList()
    }
}
