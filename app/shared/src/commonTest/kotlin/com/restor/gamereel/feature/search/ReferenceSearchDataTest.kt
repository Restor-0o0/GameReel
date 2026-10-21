package com.restor.gamereel.feature.search

import com.restor.gamereel.model.Category
import com.restor.gamereel.model.GameReference
import com.restor.gamereel.model.Library
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.model.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReferenceSearchDataTest {
    @Test
    fun duplicateSteamGamesKeepTheRichestMatchingReference() {
        val original = game("steam_broad", "42", "Original title", tagCount = 1)
        val renamed = game("steam_alltime", "42", "Renamed edition", tagCount = 3)
        val games = prepare(original, renamed)

        assertEquals(listOf(renamed), filterSearchGames(games, ""))
        assertEquals(listOf(original), filterSearchGames(games, " ORIGINAL "))
        assertEquals(listOf(renamed), filterSearchGames(games, "edition"))
        assertEquals(emptyList(), filterSearchGames(games, "missing"))
    }

    @Test
    fun equalTagCountsPreserveLibraryPriority() {
        val first = game("steam_broad", "42", "Game", tagCount = 2)
        val second = game("steam_alltime", "42", "Game", tagCount = 2)

        assertEquals(listOf(first), filterSearchGames(prepare(first, second), "game"))
        assertEquals(listOf(second), filterSearchGames(prepare(second, first), "game"))
    }

    @Test
    fun otherSourcesStayDistinctAndResultsAreSortedByTitle() {
        val steam = game("steam_broad", "42", "Zulu", tagCount = 3)
        val mobile = game("google_play", "42", "Alpha", tagCount = 0)

        assertEquals(listOf(mobile, steam), filterSearchGames(prepare(steam, mobile), ""))
        assertEquals(emptyList(), filterSearchGames(prepareSearchGames(emptyList()), ""))
    }

    @Test
    fun searchAcceptsPrefixesTyposAndSeveralWords() {
        assertTrue(fuzzySearchMatch("Factory automation and resource management", "fact auto"))
        assertTrue(fuzzySearchMatch("Factory automation", "factroy"))
        assertTrue(fuzzySearchMatch("Исследование и строительство базы", "иследование"))
        assertFalse(fuzzySearchMatch("Factory automation", "stealth"))
    }

    @Test
    fun gamesCanBeFoundByTagAndLocalizedDescription() {
        val tagged =
            GameReference(
                id = "curated:1",
                externalId = "1",
                sourceLibraryId = "curated",
                title = "Unknown title",
                tags =
                    listOf(
                        Tag(
                            "tag:stealth",
                            "stealth",
                            Category.CORE,
                            "curated",
                            description = "Скрытность и обход охраны.",
                        )
                    ),
            )
        val games = prepare(tagged)

        assertEquals(listOf(tagged), filterSearchGames(games, "steal"))
        assertEquals(listOf(tagged), filterSearchGames(games, "скрыт"))
    }

    @Test
    fun indexedSearchFindsPartialArcQueryAndKeepsTheSameResults() {
        val matching = game("steam_broad", "1", "Arcade Adventure", tagCount = 2)
        val other = game("steam_broad", "2", "Strategy Game", tagCount = 1)
        val games = prepare(matching, other)

        assertEquals(listOf(matching), filterIndexedSearchGames(indexSearchGames(games), "arc"))
        assertEquals(filterSearchGames(games, "arc"), filterIndexedSearchGames(indexSearchGames(games), "arc"))
    }

    private fun prepare(vararg games: GameReference): List<GameReference> =
        prepareSearchGames(
            games.map { game ->
                LibraryPack(Library(game.sourceLibraryId, game.sourceLibraryId), listOf(game))
            }
        )

    private fun game(source: String, externalId: String, title: String, tagCount: Int) =
        GameReference(
            id = "$source:$externalId",
            externalId = externalId,
            sourceLibraryId = source,
            title = title,
            tags = List(tagCount) { Tag("tag:$it", "Tag $it", Category.CORE, source) },
        )
}
