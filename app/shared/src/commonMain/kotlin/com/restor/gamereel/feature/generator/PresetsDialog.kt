package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import com.restor.gamereel.platform.rememberExportLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

private val presetJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

@Composable
internal fun PresetsDialog(
    workspace: Workspace,
    actions: GeneratorActions,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var savingCurrent by remember { mutableStateOf(false) }
    var currentName by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<GeneratorPreset?>(null) }
    var ioError by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf<GeneratorPreset?>(null) }
    val wrongJsonType = stringResource(Res.string.preset_wrong_json_type)
    val jsonTooLarge = stringResource(Res.string.preset_json_too_large)
    val invalidJson = stringResource(Res.string.preset_invalid_json)
    val exportFailed = stringResource(Res.string.preset_export_failed)
    val scope = rememberCoroutineScope()
    val saver = rememberExportLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file ->
            val preset = exporting
            if (file != null && preset != null) scope.launch {
                runCatching { file.write(presetJson.encodeToString(preset).encodeToByteArray()) }
                    .onFailure { ioError = it.message ?: exportFailed }
            }
            exporting = null
        },
    )
    val picker = rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("json"))) { file ->
        if (file != null) scope.launch {
            runCatching {
                val bytes = file.readBytes()
                require(bytes.size <= 30_000_000) { jsonTooLarge }
                presetJson.decodeFromString<GeneratorPreset>(bytes.decodeToString())
                    .also { require(it.isReelPreset) { wrongJsonType } }
            }.onSuccess { actions.importPreset(it) }
                .onFailure { ioError = it.message ?: invalidJson }
        }
    }

    if (creating) {
        ReelPresetCreatorDialog(onDismiss = { creating = false }) { name, categories ->
            actions.saveReelPreset(name, categories)
            creating = false
        }
        return
    }

    val saved = workspace.preferences.presets.filter { it.isReelPreset }
    if (deleting == null) Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.presets_title), Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(stringResource(Res.string.presets_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ioError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
                    item {
                        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.preset_create))
                        }
                    }
                    item {
                        OutlinedButton(onClick = { savingCurrent = !savingCurrent },
                            enabled = workspace.reels.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.preset_save))
                        }
                    }
                    if (savingCurrent) item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(currentName, { currentName = it.take(80) },
                                    label = { Text(stringResource(Res.string.preset_name)) },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    actions.savePreset(currentName.trim())
                                    currentName = ""
                                    savingCurrent = false
                                }, enabled = currentName.isNotBlank(),
                                    modifier = Modifier.align(Alignment.End)) {
                                    Text(stringResource(Res.string.preset_save_new))
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { picker.launch() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.preset_import_json))
                        }
                    }
                    if (saved.isNotEmpty()) {
                        item { HorizontalDivider() }
                        item { Text(stringResource(Res.string.preset_my_section),
                            style = MaterialTheme.typography.titleMedium) }
                        items(saved, key = { it.id }) { preset ->
                            PresetListItem(preset, preset.name,
                                onApply = { actions.applyPreset(preset.id); onDismiss() },
                                onExport = {
                                    exporting = preset
                                    saver.launch(suggestedName = "gamereel-preset", defaultExtension = "json")
                                }, onDelete = { deleting = preset })
                        }
                    }
                    item { HorizontalDivider() }
                    item { Text(stringResource(Res.string.preset_builtin_section),
                        style = MaterialTheme.typography.titleMedium) }
                    items(PresetCatalog.templates, key = { it.id }) { preset ->
                        val label = when (preset.id) {
                            "builtin:jam" -> stringResource(Res.string.preset_jam)
                            "builtin:prototype" -> stringResource(Res.string.preset_prototype)
                            "builtin:experiment" -> stringResource(Res.string.preset_experiment)
                            "builtin:world" -> stringResource(Res.string.preset_world)
                            "builtin:gameplay" -> stringResource(Res.string.preset_gameplay)
                            "builtin:presentation" -> stringResource(Res.string.preset_presentation)
                            "builtin:social" -> stringResource(Res.string.preset_social)
                            else -> preset.name
                        }
                        val description = when (preset.id) {
                            "builtin:jam" -> Res.string.preset_jam_description
                            "builtin:prototype" -> Res.string.preset_prototype_description
                            "builtin:experiment" -> Res.string.preset_experiment_description
                            "builtin:world" -> Res.string.preset_world_description
                            "builtin:gameplay" -> Res.string.preset_gameplay_description
                            "builtin:presentation" -> Res.string.preset_presentation_description
                            else -> Res.string.preset_social_description
                        }
                        PresetListItem(preset, label, description = stringResource(description),
                            onApply = { actions.applyPreset(preset.id); onDismiss() })
                    }
                }
            }
        }
    }
    deleting?.let { preset ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.preset_delete_confirm, preset.name)) },
            confirmButton = { TextButton(onClick = {
                actions.deletePreset(preset.id)
                deleting = null
            }) { Text(stringResource(Res.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) {
                Text(stringResource(Res.string.cancel))
            } })
    }
}

@Composable
private fun PresetListItem(
    preset: GeneratorPreset,
    label: String,
    onApply: () -> Unit,
    description: String? = null,
    onExport: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(preset.reels.map { it.title() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                if (onExport != null || onDelete != null) Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(stringResource(Res.string.more_actions))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        onExport?.let { action -> DropdownMenuItem(
                            text = { Text(stringResource(Res.string.preset_export_json)) },
                            onClick = { menuOpen = false; action() }) }
                        onDelete?.let { action -> DropdownMenuItem(
                            text = { Text(stringResource(Res.string.preset_delete)) },
                            onClick = { menuOpen = false; action() }) }
                    }
                }
                Button(onClick = onApply) { Text(stringResource(Res.string.preset_apply)) }
            }
        }
    }
}
