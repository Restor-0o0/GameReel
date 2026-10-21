package com.restor.gamereel.feature.search

import com.restor.gamereel.model.GameReference
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.ui.presentation.explanation
import com.restor.gamereel.ui.presentation.shortName
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class IndexedGame(val game: GameReference, val searchText: String)

internal fun prepareSearchGames(libraries: List<LibraryPack>): List<GameReference> =
    libraries.flatMap { it.games }.sortedByDescending { it.tags.size }

internal fun indexSearchGames(games: List<GameReference>): List<IndexedGame> =
    games.map { game ->
        IndexedGame(
            game,
            buildString {
                appendLine(game.title)
                game.tags.forEach { tag ->
                    appendLine(tag.name)
                    appendLine(tag.shortName)
                    appendLine(tag.explanation)
                    appendLine(tag.category.name)
                }
            }.searchNormalized(),
        )
    }

internal fun filterSearchGames(games: List<GameReference>, query: String): List<GameReference> =
    filterIndexedSearchGames(indexSearchGames(games), query)

internal fun filterIndexedSearchGames(games: List<IndexedGame>, query: String): List<GameReference> {
    val needles = query.searchWords()
    // Filter before deduplicating: the same Steam game can have different titles
    // in different libraries. Keep the richest matching reference.
    return games
        .filter { fuzzySearchMatchNormalized(it.searchText, needles) }
        .map { it.game }
        .distinctBy {
            if (it.sourceLibraryId.startsWith("steam_")) "steam:${it.externalId}" else it.id
        }
        .sortedBy { it.title }
}

internal suspend fun filterIndexedSearchGamesCancellable(
    games: List<IndexedGame>,
    query: String,
): List<GameReference> {
    val needles = query.searchWords()
    val context = currentCoroutineContext()
    val seen = HashSet<String>()
    val matches = ArrayList<GameReference>()
    games.forEachIndexed { index, entry ->
        if (index % 32 == 0) context.ensureActive()
        val game = entry.game
        if (fuzzySearchMatchNormalized(entry.searchText, needles)) {
            val key = if (game.sourceLibraryId.startsWith("steam_")) "steam:${game.externalId}" else game.id
            if (seen.add(key)) matches.add(game)
        }
    }
    return matches.sortedBy { it.title }
}
