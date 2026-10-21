package com.restor.gamereel.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReferenceSearch(
    libraries: List<LibraryPack>,
    tags: List<Tag>,
    pinnedIds: Set<String>,
    onPin: (Tag) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var gamesTab by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<GameReference?>(null) }

    val gameIndex by produceState(emptyList<IndexedGame>(), libraries) {
        value = withContext(Dispatchers.Default) {
            indexSearchGames(prepareSearchGames(libraries))
        }
    }
    val games by produceState(emptyList<GameReference>(), gameIndex, query) {
        value = withContext(Dispatchers.Default) {
            filterIndexedSearchGamesCancellable(gameIndex, query)
        }
    }
    val gameNames =
        remember(libraries) { libraries.flatMap { it.games }.associate { it.id to it.title } }
    val environment = rememberResourceEnvironment()
    val searchIndex by
        produceState(emptyList<Pair<Tag, String>>(), tags, gameNames, environment) {
            val categories = Category.entries.associateWith { getString(environment, it.title) }
            val labels = tags.associate { it.id to it.loadLabel(environment) }
            value =
                withContext(Dispatchers.Default) {
                    tags.map { tag ->
                        tag to
                            listOfNotNull(
                                    tag.name,
                                    labels[tag.id],
                                    tag.category.name,
                                    categories[tag.category],
                                    gameTitle(tag.sourceGameId, gameNames),
                                    tag.referenceGames.joinToString(" "),
                                )
                                .joinToString("\n")
                                .searchNormalized()
                    }
                }
        }
    val filteredTags by produceState(emptyList<Tag>(), searchIndex, query) {
        value = withContext(Dispatchers.Default) {
            val needles = query.searchWords()
            val context = currentCoroutineContext()
            searchIndex.mapIndexedNotNull { index, (tag, text) ->
                if (index % 32 == 0) context.ensureActive()
                tag.takeIf { fuzzySearchMatchNormalized(text, needles) }
            }
        }
    }

    Column {
        OutlinedTextField(
            query,
            { query = it },
            label = { Text(stringResource(Res.string.search_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = gamesTab,
                onClick = { gamesTab = true },
                label = { Text(stringResource(Res.string.tab_games, games.size)) },
            )

            FilterChip(
                selected = !gamesTab,
                onClick = { gamesTab = false },
                label = { Text(stringResource(Res.string.tab_tags, filteredTags.size)) },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (gamesTab) {
                if (games.isEmpty()) item { Text(stringResource(Res.string.games_empty)) }
                items(games, key = { it.id }) { game ->
                    GameResult(game, onClick = { selected = game })
                }
            } else {
                if (filteredTags.isEmpty()) item { Text(stringResource(Res.string.tags_empty)) }
                items(filteredTags, key = { it.id }) { tag ->
                    TagResult(
                        tag,
                        gameTitle(tag.sourceGameId, gameNames),
                        tag.id in pinnedIds,
                        onPin,
                    )
                }
            }
        }
    }

    selected?.let { game -> GameDetails(game, pinnedIds, onPin, { selected = null }) }
}
