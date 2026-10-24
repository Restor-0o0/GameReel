package com.restor.gamereel.ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import com.restor.gamereel.model.*
import gamereel.app.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

private val iconSlug = Regex("[^a-z0-9]+")

// keep in sync with slug in tools/tag_icons.py
internal fun iconPath(tag: Tag): String? {
    if (tag.slot != null || tag.icon.isNotBlank()) return null
    val key = tag.name.lowercase().replace(iconSlug, "_").trim('_')
    return if (key in drawnTagIcons) "files/tag_icons/$key.webp" else null
}

private object ArtCache {
    // Cache access stays on the UI coroutine; only reading/decoding runs on the worker.
    private val bitmaps = LinkedHashMap<String, ImageBitmap?>()
    private val loading = Mutex()

    operator fun get(path: String): ImageBitmap? = bitmaps[path]

    suspend fun load(path: String): ImageBitmap? =
        loading.withLock {
            if (bitmaps.containsKey(path)) return@withLock bitmaps[path]
            // Also remember missing files, rather than retrying them on every appearance.
            decode { Res.readBytes(path) }.also { remember(path, it) }
        }

    private fun remember(path: String, bitmap: ImageBitmap?) {
        bitmaps[path] = bitmap
        if (bitmaps.size > 160) bitmaps.remove(bitmaps.keys.first())
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun decode(read: suspend () -> ByteArray?): ImageBitmap? =
        try {
            read()?.let { bytes ->
                withContext(Dispatchers.Default) { bytes.decodeToImageBitmap().also { it.prepareToDraw() } }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
}

// steam header proportions
const val ArtRatio = 150f / 320f

@Composable
fun TagArt(tag: Tag?, fallback: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val bitmap = rememberTagBitmap(tag)
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) Text(tag?.displayIcon ?: fallback, fontSize = fontSize)
        else Image(image, tag?.name, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun rememberTagBitmap(tag: Tag?): ImageBitmap? =
    rememberBitmap(remember(tag) { tag?.let(::iconPath) })

@Composable
internal fun rememberBitmap(path: String?): ImageBitmap? {
    val bitmap by
        produceState(path?.let { ArtCache[it] }, path) {
            value = path?.let { ArtCache[it] }
            value = path?.let { ArtCache.load(it) }
        }
    return bitmap
}
