package com.restor.gamereel.share

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.test.*

class IdeaShareCodeTest {
    private val tags = BuiltinCatalog.tags

    @Test
    fun roundTripsKnownTags() {
        val reels = tags.take(3).mapIndexed { i, tag -> Reel("r$i", tag.category, tag, ReelState.PINNED) } +
            Reel("empty", Category.TWIST)
        val decoded = IdeaShareCode.decode(IdeaShareCode.link(reels, "Моя идея"), tags)!!
        assertEquals("Моя идея", decoded.title)
        assertEquals(reels.map { it.category }, decoded.reels.map { it.category })
        assertEquals(reels.map { it.tag }, decoded.reels.map { it.tag })
        assertTrue(decoded.reels.all { it.state == ReelState.RANDOM })
    }

    @Test
    fun resolvesUnknownTagsByNameOrKeepsText() {
        val known = tags.first()
        val foreign = listOf(
            Reel("a", known.category, known.copy(id = "other:id")),
            Reel("b", Category.SETTING, Tag("steam:x", "Tab\tName", Category.SETTING, "steam")),
        )
        val decoded = IdeaShareCode.decode(IdeaShareCode.encode(foreign), tags)!!
        assertEquals(known, decoded.reels[0].tag)
        assertEquals("Tab Name", decoded.reels[1].tag?.name)
        assertEquals(Category.SETTING, decoded.reels[1].tag?.category)
    }

    @Test
    fun extractsCodeFromSurroundingText() {
        val code = IdeaShareCode.encode(listOf(Reel("a", tags.first().category, tags.first())))
        assertNotNull(IdeaShareCode.decode("Смотри: ${IdeaShareCode.WEB_URL}#idea=$code !", tags))
        assertTrue(code.none { it == '=' || it == '+' || it == '/' })
    }

    @Test
    fun rejectsGarbage() {
        assertNull(IdeaShareCode.decode("hello", tags))
        assertNull(IdeaShareCode.decode("GR1.@@@", tags))
        assertNull(IdeaShareCode.decode(IdeaShareCode.encode(listOf(Reel("a", Category.CORE))), tags))
    }
}
