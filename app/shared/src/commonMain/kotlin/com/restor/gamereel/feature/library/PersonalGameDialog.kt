package com.restor.gamereel.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.data.personal.GameTagDraft
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.GameReference
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PersonalGameDialog(
    editing: GameReference?,
    onDismiss: () -> Unit,
    onSave: (String, List<GameTagDraft>) -> Unit,
) {
    var title by remember(editing?.id) { mutableStateOf(editing?.title.orEmpty()) }
    val tags = remember(editing?.id) {
        mutableStateListOf<GameTagDraft>().also { list ->
            list.addAll(editing?.tags?.map { GameTagDraft(it.name, it.category) }.orEmpty())
        }
    }
    val canSave = title.isNotBlank() && tags.any { it.name.isNotBlank() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(if (editing == null) Res.string.personal_game_create
                    else Res.string.personal_game_edit), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.personal_game_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, { title = it.take(160) },
                    label = { Text(stringResource(Res.string.personal_game_title)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(tags, key = { index, _ -> index }) { index, draft ->
                        var menuOpen by remember { mutableStateOf(false) }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(Res.string.personal_game_tag_number, index + 1),
                                        Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { tags.removeAt(index) }) { Text("✕") }
                                }
                                OutlinedTextField(draft.name,
                                    { tags[index] = draft.copy(name = it.take(120)) },
                                    label = { Text(stringResource(Res.string.personal_game_tag_name)) },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                Box {
                                    OutlinedButton(onClick = { menuOpen = true }) {
                                        Text(stringResource(draft.category.title))
                                    }
                                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                        Category.entries.forEach { category ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(category.title)) },
                                                onClick = {
                                                    tags[index] = draft.copy(category = category)
                                                    menuOpen = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { tags.add(GameTagDraft("", Category.CORE)) },
                            enabled = tags.size < 100) {
                            Text(stringResource(Res.string.personal_game_add_tag))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    Button(onClick = { onSave(title.trim(), tags.toList()) }, enabled = canSave) {
                        Text(stringResource(Res.string.personal_game_save))
                    }
                }
            }
        }
    }
}
