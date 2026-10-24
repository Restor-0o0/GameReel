package com.restor.gamereel.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.feature.search.fuzzySearchMatch
import com.restor.gamereel.model.ContentLimits
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.model.isGamePack
import com.restor.gamereel.ui.presentation.displayName
import com.restor.gamereel.ui.presentation.isRemoteImage
import com.restor.gamereel.ui.presentation.rememberBitmap
import gamereel.app.shared.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PackEditorDialog(
    libraries: List<LibraryPack>,
    importing: Boolean,
    onInstall: (suspend () -> String) -> Unit,
    initial: PackDraft = PackDraft(),
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var selected by remember { mutableStateOf(initial.slots.first().key) }
    var newTags by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var startMenu by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val packs = remember(libraries) { libraries.filter { it.isGamePack } }
    val slot = draft.slots.firstOrNull { it.key == selected } ?: draft.slots.first()
    val parent = draft.slots.firstOrNull { it.key == slot.parent }
    val parentNames = remember(draft.tags, parent) { parent?.let { draft.tagsOf(it.key).map(TagDraft::name) }.orEmpty() }
    val slotTags = draft.tagsOf(slot.key).filter { filter.isBlank() || fuzzySearchMatch(it.name, filter) }
    val problem = draft.problem()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val optionsIndex = 6 + (if (packs.isNotEmpty()) 1 else 0) + draft.slots.size + (if (draft.slots.size < 50) 1 else 0)

    fun update(slotDraft: SlotDraft) {
        draft = draft.copy(slots = draft.slots.map { if (it.key == slotDraft.key) slotDraft else it })
    }
    fun update(tag: TagDraft) {
        draft = draft.copy(tags = draft.tags.map { if (it.key == tag.key) tag else it })
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.pack_editor_create), style = MaterialTheme.typography.headlineSmall)
                LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(stringResource(Res.string.pack_editor_hint), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        OutlinedTextField(draft.name, { draft = draft.copy(name = it.take(80)) },
                            label = { Text(stringResource(Res.string.pack_editor_name)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    if (packs.isNotEmpty()) item {
                        Box {
                            OutlinedButton(onClick = { startMenu = true }) {
                                Text(stringResource(Res.string.pack_editor_start_from))
                            }
                            DropdownMenu(expanded = startMenu, onDismissRequest = { startMenu = false }) {
                                packs.forEach { pack ->
                                    DropdownMenuItem(text = { Text(pack.displayName()) }, onClick = {
                                        draft = PackDraft.from(pack)
                                        selected = draft.slots.first().key
                                        expanded = null; filter = ""; startMenu = false
                                    })
                                }
                            }
                        }
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.pack_editor_slots), style = MaterialTheme.typography.titleMedium) }
                    item {
                        Text(stringResource(Res.string.pack_editor_slots_hint), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(draft.slots, key = { "slot${it.key}" }) { item ->
                        SlotCard(item, draft, item.key == slot.key,
                            onChange = ::update,
                            onSelect = {
                                selected = item.key; expanded = null; filter = ""
                                scope.launch { listState.animateScrollToItem(optionsIndex) }
                            },
                            onMoveUp = {
                                val index = draft.slots.indexOf(item)
                                draft = draft.copy(slots = draft.slots.toMutableList().apply { add(index - 1, removeAt(index)) })
                            },
                            onRemove = { draft = draft.withoutSlot(item.key) })
                    }
                    if (draft.slots.size < 50) item {
                        TextButton(onClick = {
                            val key = draft.nextKey
                            draft = draft.copy(slots = draft.slots + SlotDraft(key, ""))
                            selected = key
                        }) { Text(stringResource(Res.string.pack_editor_add_slot)) }
                    }
                    item { HorizontalDivider() }
                    item {
                        Text(stringResource(Res.string.pack_editor_options, slot.name.ifBlank { "…" }),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        OutlinedTextField(newTags, { newTags = it.take(20_000) },
                            label = { Text(stringResource(Res.string.pack_editor_options_input)) },
                            minLines = 2, maxLines = 6, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Button(onClick = {
                            draft = draft.withTags(slot.key, newTags.lines())
                            newTags = ""
                        }, enabled = newTags.isNotBlank() && draft.tags.size < ContentLimits.MAX_TAGS) {
                            Text(stringResource(Res.string.pack_editor_add))
                        }
                    }
                    if (draft.tagsOf(slot.key).size > 15) item {
                        OutlinedTextField(filter, { filter = it },
                            label = { Text(stringResource(Res.string.pack_editor_filter)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    items(slotTags, key = { "tag${it.key}" }) { tag ->
                        TagRow(tag, parentNames, tag.key == expanded,
                            onToggle = { expanded = if (expanded == tag.key) null else tag.key },
                            onChange = ::update,
                            onRemove = { draft = draft.copy(tags = draft.tags - tag) })
                    }
                }
                problem?.let { (text, arg) ->
                    Text(stringResource(text, arg), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                    if (SUBMIT_URL.isNotEmpty()) OutlinedButton(onClick = { submitting = true },
                        enabled = problem == null) { Text(stringResource(Res.string.pack_editor_submit)) }
                    Button(onClick = {
                        val json = draft.toJson()
                        onInstall { json }
                        onDismiss()
                    }, enabled = problem == null && !importing) { Text(stringResource(Res.string.pack_editor_save)) }
                }
            }
        }
    }
    if (submitting) SubmitPackDialog(draft) { submitting = false }
}

@Composable
private fun SlotCard(
    slot: SlotDraft,
    draft: PackDraft,
    selected: Boolean,
    onChange: (SlotDraft) -> Unit,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onRemove: () -> Unit,
) {
    var parentMenu by remember { mutableStateOf(false) }
    val parent = draft.slots.firstOrNull { it.key == slot.parent }
    // a slot can't hang under itself or under its own descendants
    val candidates = draft.slots.filter { other ->
        var current: SlotDraft? = other
        var steps = 0
        while (current != null && current.key != slot.key && steps++ < 60) {
            val up = current.parent
            current = draft.slots.firstOrNull { it.key == up }
        }
        current == null
    }
    OutlinedCard(Modifier.fillMaxWidth(), border = BorderStroke(if (selected) 2.dp else 1.dp,
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(slot.icon, { onChange(slot.copy(icon = it.take(8))) },
                    label = { Text(stringResource(Res.string.pack_editor_slot_icon)) },
                    singleLine = true, modifier = Modifier.width(110.dp))
                OutlinedTextField(slot.name, { onChange(slot.copy(name = it.take(60))) },
                    label = { Text(stringResource(Res.string.pack_editor_slot_name)) },
                    singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallButton("−", slot.count > 0) { onChange(slot.copy(count = slot.count - 1)) }
                Text(stringResource(Res.string.pack_editor_rolled, slot.count))
                SmallButton("＋", slot.count < 10 && draft.reelCount < ContentLimits.MAX_REELS) {
                    onChange(slot.copy(count = slot.count + 1))
                }
                Spacer(Modifier.weight(1f))
                if (draft.slots.first() != slot) SmallButton("↑", true, onMoveUp)
                if (draft.slots.size > 1) SmallButton("✕", true, onRemove)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    TextButton(onClick = { parentMenu = true }) {
                        Text(parent?.let { stringResource(Res.string.pack_editor_parent, it.name.ifBlank { "…" }) }
                            ?: stringResource(Res.string.pack_editor_no_parent))
                    }
                    DropdownMenu(expanded = parentMenu, onDismissRequest = { parentMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(Res.string.pack_editor_no_parent)) },
                            onClick = { onChange(slot.copy(parent = null)); parentMenu = false })
                        candidates.forEach { other ->
                            DropdownMenuItem(text = { Text("${other.icon} ${other.name}".trim()) },
                                onClick = { onChange(slot.copy(parent = other.key)); parentMenu = false })
                        }
                    }
                }
                TextButton(onClick = onSelect) {
                    Text(stringResource(Res.string.pack_editor_edit_options, draft.tagsOf(slot.key).size))
                }
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.defaultMinSize(minWidth = 40.dp)) { Text(text) }
}

@Composable
private fun TagRow(
    tag: TagDraft,
    parentNames: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (TagDraft) -> Unit,
    onRemove: () -> Unit,
) {
    val url = tag.image.trim()
    val picture = rememberBitmap(url.takeIf(::isRemoteImage))
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    if (picture != null) Image(picture, null, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
                }
                TextButton(onClick = onToggle, modifier = Modifier.weight(1f)) {
                    Text(listOf(tag.name).plus(tag.requires.sorted()).joinToString("  · "),
                        Modifier.fillMaxWidth(), maxLines = 2)
                }
                TextButton(onClick = onRemove) { Text("✕") }
            }
            if (expanded) {
                OutlinedTextField(tag.name, { if (it.isNotBlank()) onChange(tag.copy(name = it.take(120))) },
                    label = { Text(stringResource(Res.string.pack_editor_option_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                val bad = url.isNotEmpty() && !isRemoteImage(url)
                OutlinedTextField(tag.image, { onChange(tag.copy(image = it.take(600))) },
                    label = { Text(stringResource(Res.string.pack_editor_image)) },
                    supportingText = { Text(stringResource(Res.string.pack_editor_image_hint)) },
                    isError = bad, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (parentNames.isNotEmpty()) {
                    Text(stringResource(Res.string.pack_editor_requires), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(Res.string.pack_editor_requires_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        parentNames.forEach { name ->
                            val on = tag.requires.any { it.equals(name, ignoreCase = true) }
                            FilterChip(selected = on, label = { Text(name) }, onClick = {
                                onChange(tag.copy(requires = if (on) tag.requires.filterNot {
                                    it.equals(name, ignoreCase = true) }.toSet() else tag.requires + name))
                            })
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SubmitPackDialog(draft: PackDraft, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(stringResource(Res.string.pack_submit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (link == null) {
                    Text(stringResource(Res.string.pack_submit_text), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(note, { note = it.take(500) },
                        label = { Text(stringResource(Res.string.pack_submit_note)) },
                        enabled = !sending, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth())
                    if (sending) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let {
                        Text(stringResource(Res.string.pack_submit_error), color = MaterialTheme.colorScheme.error)
                        if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(stringResource(Res.string.pack_submit_done))
                    if (link.orEmpty().startsWith("https://github.com/"))
                        TextButton(onClick = { runCatching { uriHandler.openUri(link.orEmpty()) } }) {
                            Text(stringResource(Res.string.pack_submit_open))
                        }
                }
            }
        },
        confirmButton = {
            if (link == null) Button(onClick = {
                sending = true; error = null
                scope.launch {
                    try {
                        link = submitPack(draft.toJson(), note)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = (failure as? SubmissionException)?.message.orEmpty()
                    } finally {
                        sending = false
                    }
                }
            }, enabled = !sending) { Text(stringResource(Res.string.pack_submit_send)) }
            else TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        },
        dismissButton = {
            if (link == null) TextButton(onClick = onDismiss, enabled = !sending) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
