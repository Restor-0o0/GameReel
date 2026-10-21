package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.generator.*
import com.restor.gamereel.model.*
import kotlin.test.*

class GeneratorEngineTest {
    @Test
    fun atomWeightDoesNotDependOnNumberOfGames() {
        val bounds = mutableListOf<Int>()
        val recording =
            object : RandomProvider {
                override fun nextInt(bound: Int): Int {
                    bounds += bound
                    return 0
                }
            }
        val common =
            (1..100).map { Tag("tag:$it", "Combat", Category.CORE, sourceGameId = "game:$it") }
        val rare = Tag("rare", "Fishing", Category.CORE)
        GeneratorEngine(common + rare, recording).spin(listOf(Reel("reel", Category.CORE)))
        assertEquals(listOf(2, 100), bounds)
    }

    private val first =
        object : RandomProvider {
            override fun nextInt(bound: Int) = 0
        }
    private val engine = GeneratorEngine(BuiltinCatalog.tags, first)

    private fun spun() = engine.spin(BuiltinCatalog.initialWorkspace().reels)

    @Test
    fun fixtureProvidesEightIndependentCategories() {
        assertEquals(40, BuiltinCatalog.tags.size)
        assertEquals(40, BuiltinCatalog.tags.map { it.id }.toSet().size)
        assertEquals(8, spun().size)
        assertTrue(spun().all { it.tag?.category == it.category })
    }

    @Test
    fun spinPreservesFrozenAndPinned() {
        val before =
            spun().mapIndexed { index, reel ->
                reel.copy(
                    state =
                        when (index) {
                            0 -> ReelState.FROZEN
                            1 -> ReelState.PINNED
                            else -> ReelState.RANDOM
                        }
                )
            }
        val after = engine.spin(before)
        assertEquals(before.take(2), after.take(2))
        assertTrue(before.drop(2).zip(after.drop(2)).all { (a, b) -> a.tag != b.tag })
    }

    @Test
    fun unlockingDoesNotRemovePins() {
        val before =
            spun().take(2).mapIndexed { i, reel ->
                reel.copy(state = if (i == 0) ReelState.PINNED else ReelState.FROZEN)
            }
        val after = engine.unlockFrozen(before)
        assertEquals(ReelState.PINNED, after[0].state)
        assertEquals(ReelState.RANDOM, after[1].state)
    }

    @Test
    fun rerollOnlyChangesTarget() {
        val before = spun()
        val after = engine.spin(before, onlyId = before[3].id)
        assertNotEquals(before[3].tag, after[3].tag)
        assertEquals(
            before.filterIndexed { i, _ -> i != 3 },
            after.filterIndexed { i, _ -> i != 3 },
        )
    }

    @Test
    fun rerollCannotChangeLockedTarget() {
        val before = spun().map { it.copy(state = ReelState.FROZEN) }
        assertEquals(before, engine.spin(before, onlyId = before.first().id))
    }

    @Test
    fun blacklistExcludesAutomaticTagsButPreservesPins() {
        val before =
            spun().mapIndexed { i, reel ->
                if (i == 0) reel.copy(state = ReelState.PINNED) else reel
            }
        val blacklist = before.mapNotNull { it.tag?.id }.toSet()
        val after = engine.spin(before, GeneratorConfig(blacklist = blacklist))
        assertEquals(before.first(), after.first())
        assertTrue(after.drop(1).all { it.tag?.id !in blacklist })
    }

    @Test
    fun sourceFiltersAndEmptyPoolsAreRespected() {
        val sourceTag = Tag("user:core", "Custom", Category.CORE, sourceLibraryId = "user")
        val filtered =
            GeneratorEngine(BuiltinCatalog.tags + sourceTag, first)
                .spin(spun(), GeneratorConfig(sourceIds = setOf("user")))
        assertEquals(sourceTag, filtered.first().tag)
        assertTrue(filtered.drop(1).all { it.tag == null })
    }

    @Test
    fun duplicatePinIsNotAddedAndRetainsSource() {
        val tag = BuiltinCatalog.tags.first()
        val once = engine.pin(spun(), tag, "pin:1")
        assertEquals(once, engine.pin(once, tag, "pin:2"))
        assertEquals("teardown", once.first().tag?.sourceGameId)
    }

    @Test
    fun sameRandomProviderProducesSameSequence() {
        val a = engine.spin(spun())
        val b = GeneratorEngine(BuiltinCatalog.tags, first).spin(spun())
        assertEquals(a, b)
    }

    @Test
    fun antiCopyPrefersDiversityButDoesNotLeaveReelsEmpty() {
        val tags = (0..3).map { Tag("$it", "Tag $it", Category.CORE, sourceGameId = "game") }
        val local = GeneratorEngine(tags, first)
        val reels =
            listOf(
                Reel("p", Category.CORE, tags[0], ReelState.PINNED),
                Reel("f", Category.CORE, tags[1], ReelState.FROZEN),
                Reel("a", Category.CORE),
                Reel("b", Category.CORE),
            )
        val result = local.spin(reels)
        assertNotNull(result[2].tag)
        assertNotNull(result[3].tag)
        assertEquals(reels.take(2), result.take(2))
    }

    @Test
    fun gameJamTwistIsNotLostToSameGameLimit() {
        val categories = listOf(Category.CORE, Category.SECONDARY_MECHANIC,
            Category.VISUAL, Category.TWIST)
        val tags = categories.map { category ->
            Tag("game:${category.name}", category.name, category, "games", "game:1")
        }
        val reels = categories.mapIndexed { index, category -> Reel("reel:$index", category) }
        val result = GeneratorEngine(tags, first).spin(reels,
            GeneratorConfig(sourceIds = setOf("games"), maxAutoAtomsFromSameGame = 2))
        assertTrue(result.all { it.tag != null })
        assertEquals(tags.last(), result.last().tag)
    }
}
