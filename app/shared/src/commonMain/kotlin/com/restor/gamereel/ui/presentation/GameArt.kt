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

class Art(val title: String, val path: String)

class GameArt(libraries: List<LibraryPack>, private val allowStoreArt: Boolean = true) {
    private class Entry(val art: Art, val rank: Int)

    private val byId = HashMap<String, Entry>()
    private val byTitle = HashMap<String, Entry>()

    init {
        if (allowStoreArt) {
            libraries.forEach { pack ->
                pack.games.forEachIndexed { rank, game ->
                    val path = artPath(game)?.takeIf { game.image != null } ?: return@forEachIndexed
                    val entry = Entry(Art(game.title, path), rank)
                    byId[game.id] = entry
                    val key = titleKey(game.title)
                    if ((byTitle[key]?.rank ?: Int.MAX_VALUE) > rank) byTitle[key] = entry
                }
            }
        }
    }

    // own game first, then the most popular reference, then any reference art we shipped
    fun artFor(tag: Tag): Art? {
        if (!allowStoreArt) return null
        tag.sourceGameId?.let(byId::get)?.let {
            return it.art
        }

        val references = tag.referenceGames
        references
            .mapNotNull { byTitle[titleKey(it)] }
            .minByOrNull { it.rank }
            ?.let {
                return it.art
            }
        return references
            .firstOrNull { titleKey(it) in referenceArt }
            ?.let { Art(it, "files/art/ref_${titleKey(it)}.webp") }
    }
}

private val parenthesized = Regex("\\([^)]*\\)")

// keep in sync with title_key in tools/fetch_store_images.py
internal fun titleKey(title: String) =
    title.replace(parenthesized, "").lowercase().filter { it.isLetterOrDigit() }

// art ships with the app, see tools/fetch_store_images.py
internal fun artPath(game: GameReference): String? =
    when (game.sourceLibraryId) {
        LibraryIds.GOOGLE_PLAY -> "files/art/play_${game.externalId}.webp"
        LibraryIds.STEAM_BROAD,
        LibraryIds.STEAM_ALLTIME -> "files/art/steam_${game.externalId}.webp"
        else -> null
    }

private object ArtCache {
    // Cache access stays on the UI coroutine; only reading/decoding runs on the worker.
    private val bitmaps = LinkedHashMap<String, ImageBitmap?>()
    private val loading = Mutex()

    operator fun get(path: String): ImageBitmap? = bitmaps[path]

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(path: String): ImageBitmap? = loading.withLock {
        if (bitmaps.containsKey(path)) return@withLock bitmaps[path]

        val bitmap =
            try {
                withContext(Dispatchers.Default) {
                    Res.readBytes(path).decodeToImageBitmap().also { it.prepareToDraw() }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }

        // Also remember missing files, rather than retrying them on every appearance.
        bitmaps[path] = bitmap
        if (bitmaps.size > 96) bitmaps.remove(bitmaps.keys.first())
        bitmap
    }
}

// steam header proportions
const val ArtRatio = 150f / 320f

val LocalGameArt = staticCompositionLocalOf { GameArt(emptyList(), allowStoreArt = false) }

@Composable
private fun rememberArt(tag: Tag?): Art? {
    val art = LocalGameArt.current
    return remember(tag, art) { tag?.let(art::artFor) }
}

@Composable
fun TagArt(tag: Tag?, fallback: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val art = rememberArt(tag)
    val bitmap = rememberTagBitmap(tag)
    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) Text(tag?.displayIcon ?: fallback, fontSize = fontSize)
        else Image(image, art?.title, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun rememberTagBitmap(tag: Tag?): ImageBitmap? {
    val art = rememberArt(tag)
    val path = art?.path
    val bitmap by
        produceState(path?.let { ArtCache[it] }, path) {
            value = path?.let { ArtCache[it] }
            value = path?.let { ArtCache.load(it) }
        }
    return bitmap
}
