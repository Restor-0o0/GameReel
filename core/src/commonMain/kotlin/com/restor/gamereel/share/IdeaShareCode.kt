package com.restor.gamereel.share

import com.restor.gamereel.model.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class SharedIdea(val title: String, val reels: List<Reel>)

@OptIn(ExperimentalEncodingApi::class)
object IdeaShareCode {
    const val PREFIX = "GR1."
    const val WEB_URL = "https://restor-0o0.github.io/GameReel/"
    const val LINK_KEY = "idea="
    private const val MAX_TEXT = 120
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun encode(reels: List<Reel>, title: String = ""): String {
        val lines = listOf(clean(title)) + reels.take(ContentLimits.MAX_REELS).map { reel ->
            listOfNotNull(reel.category.name, reel.tag?.id?.let(::clean), reel.tag?.name?.let(::clean))
                .joinToString("\t")
        }
        return PREFIX + base64.encode(lines.joinToString("\n").encodeToByteArray()).trimEnd('=')
    }

    fun link(reels: List<Reel>, title: String = ""): String = "$WEB_URL#$LINK_KEY${encode(reels, title)}"

    fun extract(input: String): String? {
        val start = input.indexOf(PREFIX).takeIf { it >= 0 } ?: return null
        return input.substring(start).takeWhile { it.isLetterOrDigit() || it in "._-" }
    }

    fun decode(input: String, known: List<Tag>, slots: List<ReelSlot> = emptyList()): SharedIdea? {
        val code = extract(input) ?: return null
        val text = runCatching { base64.decode(code.removePrefix(PREFIX)).decodeToString() }
            .getOrNull() ?: return null
        val lines = text.split('\n')
        val byId = known.associateBy { it.id }
        val reels = lines.drop(1).take(ContentLimits.MAX_REELS).mapIndexedNotNull { index, line ->
            val parts = line.split('\t')
            val category = Category.entries.firstOrNull { it.name == parts[0] } ?: return@mapIndexedNotNull null
            val tag = if (parts.size < 3 || parts[2].isBlank()) null
            else resolve(category, parts[1].take(MAX_TEXT), parts[2].take(MAX_TEXT), byId, known)
            Reel("shared:$index", category, tag, slot = tag?.slot?.let { id -> slots.firstOrNull { it.id == id } })
        }
        if (reels.none { it.tag != null }) return null
        return SharedIdea(lines.first().take(MAX_TEXT), reels)
    }

    private fun resolve(category: Category, id: String, name: String,
                        byId: Map<String, Tag>, known: List<Tag>): Tag =
        byId[id]?.takeIf { it.category == category }
            ?: known.firstOrNull { it.category == category && it.name.equals(name, ignoreCase = true) }
            ?: Tag("shared:${category.name}:${name.lowercase()}", name, category, LibraryIds.FIXTURE)

    private fun clean(value: String) = value.replace('\t', ' ').replace('\n', ' ').trim().take(MAX_TEXT)
}
