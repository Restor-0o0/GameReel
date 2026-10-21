package com.restor.gamereel.generator

import com.restor.gamereel.model.*
import kotlin.test.*

class CombinationAnalyzerTest {
    private fun tag(name: String, category: Category = Category.CORE) = Tag(name, name, category)

    @Test
    fun exactAliasesIgnoreCaseWhitespaceAndUnrelatedLocations() {
        assertEquals(Mechanic.DECKBUILDING, mechanicFor(tag(" Deck   Building ")))
        assertNull(mechanicFor(tag("server farm", Category.LOCATION)))
        assertNull(mechanicFor(tag("farming", Category.LOCATION)))
        assertNull(mechanicFor(tag("crafting minigame")))
    }

    @Test
    fun strongerComboReplacesItsSubsetsAndDuplicatesDoNotStack() {
        val tags = listOf(tag("crafting"), tag("inventory management"), tag("resource scarcity"))
        assertEquals(listOf(SynergyRule.EXPEDITION), findSynergies(tags + tags).map { it.rule })
        assertTrue(findSynergies(listOf(tag("crafting"), tag("crafting"))).isEmpty())
    }

    @Test
    fun rarityUsesDeduplicatedGamesAndRequiresEvidenceForBothMechanics() {
        fun game(i: Int, names: List<String>) =
            GameReference("g$i", "$i", "a", "Game $i", names.map { tag(it) })
        val games =
            (0 until 20).map { i ->
                game(
                    i,
                    if (i < 10) listOf("crafting", "inventory management")
                    else listOf("stealth", "destruction"),
                )
            }
        val index =
            CombinationIndex(
                listOf(LibraryPack(Library("a", "A"), games), LibraryPack(Library("b", "B"), games))
            )
        assertEquals(20, index.corpusSize)
        val rare = index.rareCombinations(listOf(tag("crafting"), tag("stealth"))).single()
        assertEquals(0, rare.occurrences)
        assertTrue(index.rareCombinations(listOf(tag("crafting"), tag("rhythm play"))).isEmpty())
        assertTrue(
            index.rareCombinations(listOf(tag("crafting"), tag("inventory management"))).isEmpty()
        )
        assertTrue(
            CombinationIndex(emptyList())
                .rareCombinations(listOf(tag("crafting"), tag("stealth")))
                .isEmpty()
        )
    }
}
