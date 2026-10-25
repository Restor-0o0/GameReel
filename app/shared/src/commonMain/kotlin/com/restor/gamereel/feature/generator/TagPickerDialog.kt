package com.restor.gamereel.feature.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.model.Reel
import com.restor.gamereel.model.Tag
import com.restor.gamereel.ui.components.*
import com.restor.gamereel.ui.presentation.label
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TagPickerDialog(
    reel: Reel,
    options: List<Tag>,
    onPick: (Tag) -> Unit,
    onMove: (Int) -> Unit,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val earlierLabel = stringResource(Res.string.move_reel_earlier)
    val laterLabel = stringResource(Res.string.move_reel_later)
    var query by remember { mutableStateOf("") }
    val labels = options.map { it.label() }
    val visible = remember(options, labels, query) {
        val needle = query.trim().lowercase()
        options.indices
            .filter { needle.isEmpty() || needle in labels[it].lowercase() || needle in options[it].name.lowercase() }
            .sortedWith(compareBy({ options[it].id != reel.tag?.id }, { labels[it].lowercase() }))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(Space.m).widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(stringResource(Res.string.pick_value_title, reel.title())) {
                    LinkAction(stringResource(Res.string.close), onDismiss)
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = Space.l, end = Space.l, bottom = Space.m),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    TertiaryAction("←", { onMove(-1) },
                        Modifier.width(56.dp).semantics { contentDescription = earlierLabel }, canMoveEarlier)
                    TertiaryAction("→", { onMove(1) },
                        Modifier.width(56.dp).semantics { contentDescription = laterLabel }, canMoveLater)
                    SecondaryAction(stringResource(Res.string.remove_reel), onRemove, Modifier.weight(1f))
                }
                OutlinedTextField(
                    query, { query = it.take(80) },
                    label = { Text(stringResource(Res.string.pick_value_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.l),
                )
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = Space.s)) {
                    itemsIndexed(visible, key = { _, index -> options[index].id }) { _, index ->
                        val tag = options[index]
                        val current = tag.id == reel.tag?.id
                        val description = tag.description.removePrefix(labels[index]).removePrefix(":").trim()
                            .replaceFirstChar { it.uppercase() }
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(tag) }
                                .padding(horizontal = Space.l, vertical = Space.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(labels[index], style = MaterialTheme.typography.titleSmall,
                                    color = if (current) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface)
                                if (description.isNotEmpty())
                                    Text(description, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (current) Text("✓", Modifier.padding(start = Space.s),
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
