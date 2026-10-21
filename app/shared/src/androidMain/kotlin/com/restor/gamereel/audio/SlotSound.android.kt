package com.restor.gamereel.audio

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.restor.gamereel.app.shared.R

@Composable
internal actual fun rememberPlatformSlotSound(): SlotSound {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        object : SlotSound {
            private val pool =
                SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
            private val loaded = mutableSetOf<Int>()

            init {
                pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
            }

            private val tick = pool.load(context, R.raw.slot_tick, 1)
            private val stop = pool.load(context, R.raw.slot_stop, 1)

            override fun play(stopped: Boolean) {
                val id = if (stopped) stop else tick
                if (id in loaded) pool.play(id, 0.45f, 0.45f, 1, 0, 1f)
            }

            override fun close() {
                pool.release()
            }
        }
    }
}
