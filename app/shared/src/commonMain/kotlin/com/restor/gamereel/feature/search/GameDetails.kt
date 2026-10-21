package com.restor.gamereel.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameDetails(
    game: GameReference,
    pinnedIds: Set<String>,
    onPin: (Tag) -> Unit,
    onClose: () -> Unit,
) {
    val tagsByCategory = remember(game.tags) { game.tags.groupBy { it.category } }
    val uriHandler = LocalUriHandler.current
    var linkFailed by remember(game.id) { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)) {
                Text(game.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(Res.string.pick_aspects),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (linkFailed)
                    Text(
                        stringResource(Res.string.open_link_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    if (game.tags.isEmpty())
                        item { Text(stringResource(Res.string.game_no_mechanics)) }
                    tagsByCategory.forEach { (category, tags) ->
                        item {
                            Text(
                                "${category.symbol} ${stringResource(category.title)}",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }

                        items(tags, key = { it.id }) { tag ->
                            TagResult(tag, null, tag.id in pinnedIds, onPin)
                        }
                    }
                }
                Row {
                    game.storeUrl?.let { url ->
                        TextButton(
                            onClick = {
                                runCatching { uriHandler.openUri(url) }
                                    .onFailure { linkFailed = true }
                            }
                        ) {
                            Text(stringResource(Res.string.open_store))
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(stringResource(Res.string.done)) }
                }
            }
        }
    }
}
