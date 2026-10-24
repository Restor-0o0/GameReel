package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.random.Random
import kotlin.test.*

class PackSlotsTest {
    private val hero = ReelSlot("dota:slot:hero", "Hero")
    private val talent = ReelSlot("dota:slot:talent", "Talent", parent = hero.id)
    private val item = ReelSlot("dota:slot:item", "Item")
    private val heroes = listOf("Axe", "Lina").map { Tag("h:$it", it, Category.OTHER, "dota", slot = hero.id) }
    private val talents = heroes.flatMap { h ->
        (1..3).map { Tag("t:${h.name}:$it", "${h.name} talent $it", Category.OTHER, "dota", slot = talent.id, requires = listOf(h.id)) }
    }
    private val items = (1..6).map { Tag("i:$it", "Item $it", Category.OTHER, "dota", slot = item.id) }
    private val tags = heroes + talents + items
    private val pack = LibraryPack(Library("dota", "Dota", slots = listOf(hero, talent, item),
        reels = listOf(hero.id, talent.id, item.id, item.id, item.id, item.id, item.id, item.id), standaloneTags = tags), emptyList())
    private fun random(seed: Int) = object : RandomProvider {
        val source = Random(seed)
        override fun nextInt(bound: Int) = source.nextInt(bound)
    }

    @Test
    fun talentsFollowHeroAndItemsNeverRepeat() {
        repeat(50) { seed ->
            val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags + tags, random(seed)), listOf(pack))
            val reels = editor.installPack(BuiltinCatalog.initialWorkspace(), pack).reels
            assertEquals(8, reels.size)
            val chosenHero = reels.single { it.slot == hero }.tag!!
            assertTrue(chosenHero.id in reels.single { it.slot == talent }.tag!!.requires)
            val picked = reels.filter { it.slot == item }.map { it.tag!!.name }
            assertEquals(6, picked.toSet().size)
        }
    }

    @Test
    fun selectingHeroRerollsItsTalentAndUndoRestoresIdeaReels() {
        val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags + tags, random(1)), listOf(pack))
        val start = BuiltinCatalog.initialWorkspace()
        val installed = editor.installPack(start, pack)
        assertTrue(installed.preferences.presets.any { it.id == "pack:dota" })
        val heroReel = installed.reels.single { it.slot == hero }
        val other = heroes.first { it != heroReel.tag }
        val changed = editor.selectTag(installed, heroReel.id, other)
        assertEquals(other.id, changed.reels.single { it.slot == hero }.tag!!.id)
        assertEquals(ReelState.FROZEN, changed.reels.single { it.slot == hero }.state)
        assertTrue(other.id in changed.reels.single { it.slot == talent }.tag!!.requires)
        assertEquals(start.reels.map { it.category }, editor.undo(installed).reels.map { it.category })
    }

    @Test
    fun dependentsAreTransitive() {
        val engine = GeneratorEngine(tags, random(0))
        val sub = ReelSlot("dota:slot:sub", "Sub", parent = talent.id)
        val reels = listOf(Reel("a", Category.OTHER, slot = hero), Reel("b", Category.OTHER, slot = talent),
            Reel("c", Category.OTHER, slot = sub), Reel("d", Category.OTHER, slot = item))
        assertEquals(setOf("b", "c"), engine.dependentsOf(reels, "a"))
    }
}
