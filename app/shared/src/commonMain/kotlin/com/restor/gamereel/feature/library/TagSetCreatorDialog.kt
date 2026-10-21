package com.restor.gamereel.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.feature.search.fuzzySearchMatch
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.explanation
import com.restor.gamereel.ui.presentation.label
import com.restor.gamereel.ui.presentation.shortName
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TagSetCreatorDialog(
    libraries: List<LibraryPack>,
    onDismiss: () -> Unit,
    onSave: (String, List<GameReference>, List<Tag>, Boolean) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var tagQuery by remember { mutableStateOf("") }
    var gameQuery by remember { mutableStateOf("") }
    var groupQuery by remember { mutableStateOf("") }
    var groupTag by remember { mutableStateOf<Tag?>(null) }
    var includeGameTags by remember { mutableStateOf(true) }
    var categoryMenu by remember { mutableStateOf(false) }
    val selectedGames = remember { mutableStateListOf<GameReference>() }
    val selectedTags = remember { mutableStateListOf<Tag>() }
    val selectedCategories = remember { mutableStateListOf<Category>() }
    val games = remember(libraries) {
        libraries.flatMap { it.games }
            .sortedByDescending { it.tags.size }
            .distinctBy { if (it.sourceLibraryId.startsWith("steam_")) "steam:${it.externalId}" else it.id }
            .sortedBy { it.title }
    }
    val tags = remember(libraries) {
        (BuiltinCatalog.tags + libraries.flatMap { it.allTags })
            .filter { it.name.isNotBlank() }
            .sortedByDescending { it.description.length }
            .distinctBy { "${it.category}:${it.name.trim().lowercase()}" }
            .sortedBy { it.name }
    }
    val gameTags = remember(games) {
        games.flatMap { it.tags }.filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim().lowercase() }.sortedBy { it.name }
    }
    val visibleTags = remember(tags, tagQuery) {
        if (tagQuery.isBlank()) emptyList() else tags.filter { it.matchesPresetQuery(tagQuery) }.take(12)
    }
    val visibleGames = remember(games, gameQuery) {
        if (gameQuery.isBlank()) emptyList() else games.filter { fuzzySearchMatch(it.title, gameQuery) }.take(12)
    }
    val visibleGroups = remember(gameTags, groupQuery) {
        if (groupQuery.isBlank()) emptyList() else gameTags.filter { it.matchesPresetQuery(groupQuery) }.take(10)
    }
    val groupGames = remember(games, groupTag) {
        val tagName = groupTag?.name
        if (tagName == null) emptyList()
        else games.filter { game -> game.tags.any { it.name.equals(tagName, ignoreCase = true) } }
    }
    val categories = remember(tags) { Category.entries.filter { category -> tags.any { it.category == category } } }
    val categoryTags = tags.filter { it.category in selectedCategories }
    val pool = selectedTags.toList() + categoryTags +
        if (includeGameTags) selectedGames.flatMap { it.tags } else emptyList()
    val canSave = name.isNotBlank() && pool.isNotEmpty()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.tag_set_create), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.tag_set_creator_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            name, { name = it.take(80) },
                            label = { Text(stringResource(Res.string.tag_set_name)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(stringResource(Res.string.preset_selected_summary,
                            (selectedTags + categoryTags).distinctBy { it.id }.size, selectedGames.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.preset_add_area), style = MaterialTheme.typography.titleMedium) }
                    item {
                        Box {
                            OutlinedButton(onClick = { categoryMenu = true }) {
                                Text(stringResource(Res.string.preset_choose_area))
                            }
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                categories.filterNot { it in selectedCategories }.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(category.title)) },
                                        onClick = { selectedCategories.add(category); categoryMenu = false },
                                    )
                                }
                            }
                        }
                    }
                    items(selectedCategories, key = { it.name }) { category ->
                        TextButton(onClick = { selectedCategories.remove(category) }) {
                            Text("✕  ${stringResource(category.title)}")
                        }
                    }
                    if (selectedTags.isNotEmpty()) {
                        item { Text(stringResource(Res.string.preset_selected_tags), style = MaterialTheme.typography.titleSmall) }
                        items(selectedTags.take(8), key = { it.id }) { tag ->
                            TextButton(onClick = { selectedTags.remove(tag) }) {
                                Text("✕  ${tag.label()}", maxLines = 2)
                            }
                        }
                        if (selectedTags.size > 8) item {
                            Text(stringResource(Res.string.preset_more_selected, selectedTags.size - 8),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (selectedGames.isNotEmpty()) {
                        item { Text(stringResource(Res.string.preset_selected_games), style = MaterialTheme.typography.titleSmall) }
                        items(selectedGames.take(8), key = { it.id }) { game ->
                            TextButton(onClick = { selectedGames.remove(game) }) {
                                Text("✕  ${game.title}", maxLines = 2)
                            }
                        }
                        if (selectedGames.size > 8) item {
                            Text(stringResource(Res.string.preset_more_selected, selectedGames.size - 8),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        item {
                            TextButton(onClick = { selectedGames.clear() }) {
                                Text(stringResource(Res.string.preset_clear_games))
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeGameTags, onCheckedChange = { includeGameTags = it })
                                Text(stringResource(Res.string.preset_include_game_tags))
                            }
                        }
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.preset_add_tags), style = MaterialTheme.typography.titleMedium) }
                    item {
                        OutlinedTextField(
                            tagQuery, { tagQuery = it },
                            label = { Text(stringResource(Res.string.preset_tag_search)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(visibleTags, key = { "${it.category}:${it.name}" }) { tag ->
                        TextButton(
                            onClick = {
                                if (selectedTags.any { it.id == tag.id }) selectedTags.removeAll { it.id == tag.id }
                                else selectedTags.add(tag)
                                keyboardController?.hide()
                            },
                        ) { Text("${if (selectedTags.any { it.id == tag.id }) "✕" else "＋"}  ${tag.label()}", maxLines = 2) }
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.preset_add_games), style = MaterialTheme.typography.titleMedium) }
                    item {
                        OutlinedTextField(
                            gameQuery, { gameQuery = it },
                            label = { Text(stringResource(Res.string.preset_game_search)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(visibleGames, key = { it.id }) { game ->
                        TextButton(
                            onClick = {
                                if (selectedGames.any { it.id == game.id }) selectedGames.removeAll { it.id == game.id }
                                else selectedGames.add(game)
                                keyboardController?.hide()
                            },
                        ) { Text("${if (selectedGames.any { it.id == game.id }) "✕" else "＋"}  ${game.title} · ${game.tags.size}", maxLines = 2) }
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.preset_add_by_tag), style = MaterialTheme.typography.titleMedium) }
                    item {
                        OutlinedTextField(
                            groupQuery, { groupQuery = it; groupTag = null },
                            label = { Text(stringResource(Res.string.preset_tag_search)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (groupTag == null) items(visibleGroups, key = { it.name }) { tag ->
                        TextButton(onClick = {
                            groupTag = tag
                            groupQuery = tag.name
                            keyboardController?.hide()
                        }) {
                            Text(tag.label(), maxLines = 2)
                        }
                    }
                    if (groupTag != null) item {
                        Text(stringResource(Res.string.preset_games_found, groupGames.size))
                        Text(groupGames.take(6).joinToString(" · ") { it.title }, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                val ids = selectedGames.map { it.id }.toSet()
                                selectedGames.addAll(groupGames.filterNot { it.id in ids })
                                groupTag = null; groupQuery = ""
                            },
                            enabled = groupGames.any { game -> selectedGames.none { it.id == game.id } },
                        ) { Text(stringResource(Res.string.preset_add_all_games)) }
                    }
                }
                if (!canSave && selectedGames.isNotEmpty() && !includeGameTags &&
                    selectedTags.isEmpty() && selectedCategories.isEmpty())
                    Text(stringResource(Res.string.preset_need_tags), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                Text(stringResource(Res.string.preset_pool_summary, pool.distinctBy { it.id }.size,
                    pool.map { it.category }.distinct().size), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    Button(onClick = { onSave(name.trim(), selectedGames.toList(),
                        (selectedTags + categoryTags).distinctBy { it.id }, includeGameTags) },
                        enabled = canSave) { Text(stringResource(Res.string.tag_set_save)) }
                }
            }
        }
    }
}

private fun Tag.matchesPresetQuery(query: String): Boolean {
    val needle = query.trim().replace("выживание", "survival", ignoreCase = true)
    return fuzzySearchMatch("$name $shortName $description $explanation", needle)
}
