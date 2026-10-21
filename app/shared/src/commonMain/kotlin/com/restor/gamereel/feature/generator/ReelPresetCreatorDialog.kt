package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.model.Category
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReelPresetCreatorDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<Category>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val categories = remember { mutableStateListOf<Category>() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.reel_preset_create), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.reel_preset_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    name, { name = it.take(80) },
                    label = { Text(stringResource(Res.string.preset_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(categories, key = { index, _ -> index }) { index, category ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${index + 1}. ${stringResource(category.title)}",
                                    style = MaterialTheme.typography.titleSmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val moveUp = stringResource(Res.string.reel_move_up)
                                    val moveDown = stringResource(Res.string.reel_move_down)
                                    val remove = stringResource(Res.string.reel_remove)
                                    TextButton(onClick = {
                                        if (index > 0) {
                                            val previous = categories[index - 1]
                                            categories[index - 1] = category
                                            categories[index] = previous
                                        }
                                    }, enabled = index > 0,
                                        modifier = Modifier.semantics { contentDescription = moveUp }) {
                                        Text("↑")
                                    }
                                    TextButton(onClick = {
                                        if (index < categories.lastIndex) {
                                            val next = categories[index + 1]
                                            categories[index + 1] = category
                                            categories[index] = next
                                        }
                                    }, enabled = index < categories.lastIndex,
                                        modifier = Modifier.semantics { contentDescription = moveDown }) {
                                        Text("↓")
                                    }
                                    TextButton(onClick = { categories.removeAt(index) },
                                        modifier = Modifier.semantics { contentDescription = remove }) {
                                        Text("✕")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Box {
                            OutlinedButton(onClick = { menuOpen = true }, enabled = categories.size < com.restor.gamereel.model.ContentLimits.MAX_REELS,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(Res.string.reel_preset_add_reel))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                Category.entries.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(category.title)) },
                                        onClick = { categories.add(category); menuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    Button(onClick = { onSave(name.trim(), categories.toList()) },
                        enabled = name.isNotBlank() && categories.isNotEmpty()) {
                        Text(stringResource(Res.string.preset_save_new))
                    }
                }
            }
        }
    }
}
