package com.restor.gamereel.audio

import androidx.compose.runtime.*
import gamereel.app.shared.generated.resources.Res
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberPlatformSlotSound(): SlotSound {
    val sound = remember { DesktopSlotSound() }
    LaunchedEffect(sound) {
        withContext(Dispatchers.IO) {
            sound.load(
                Res.readBytes("files/audio/slot_tick.wav"),
                Res.readBytes("files/audio/slot_stop.wav"),
            )
        }
    }
    return sound
}

private class DesktopSlotSound : SlotSound {
    private var clips = emptyList<Clip>()
    private var closed = false

    fun load(tick: ByteArray, stop: ByteArray) {
        val opened = mutableListOf<Clip>()
        try {
            for (bytes in listOf(tick, stop)) {
                val clip = AudioSystem.getClip()
                opened.add(clip)
                AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { clip.open(it) }
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    (clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl).value = -7f
                }
            }
            // Opening a device can be slow: never hold the playback lock while loading.
            synchronized(this) {
                if (closed) opened.forEach { it.close() } else clips = opened
            }
        } catch (_: Exception) {
            // An unavailable audio device must not prevent generating ideas.
            opened.forEach { it.close() }
        }
    }

    @Synchronized
    override fun play(stopped: Boolean) {
        clips.getOrNull(if (stopped) 1 else 0)?.let {
            it.stop()
            it.framePosition = 0
            it.start()
        }
    }

    @Synchronized
    override fun close() {
        closed = true
        clips.forEach { it.close() }
        clips = emptyList()
    }
}
