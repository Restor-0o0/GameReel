package com.restor.gamereel.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.data.personal.GameTagDraft
import com.restor.gamereel.feature.search.fuzzySearchMatch
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.components.SectionLabel
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.text.fail
import com.restor.gamereel.ui.presentation.displayName
import gamereel.app.shared.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import com.restor.gamereel.platform.rememberExportLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val tagSetJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

@Composable
internal fun LibrariesContent(
    workspace: Workspace,
    libraries: List<LibraryPack>,
    disabledIds: Set<String>,
    importing: Boolean,
    onToggle: (String) -> Unit,
    onEnableAll: () -> Unit,
    onImport: (suspend () -> String) -> Unit,
    onSaveTagSet: (String, List<GameReference>, List<Tag>, Boolean) -> Unit,
    onApplyTagSet: (String) -> Unit,
    onClearTagSet: () -> Unit,
    onDeleteTagSet: (String) -> Unit,
    onImportTagSet: (GeneratorPreset) -> Unit,
    onSavePersonalGame: (String?, String, List<GameTagDraft>) -> Unit,
    onDeletePersonalGame: (String) -> Unit,
    onImportPersonalGames: (suspend () -> String) -> Unit,
) {
    var creatingTagSet by remember { mutableStateOf(false) }
    var tagSetError by remember { mutableStateOf<String?>(null) }
    var exportingTagSet by remember { mutableStateOf<GeneratorPreset?>(null) }
    var personalEditorOpen by remember { mutableStateOf(false) }
    var editingPersonalGame by remember { mutableStateOf<GameReference?>(null) }
    var deletingPersonalGame by remember { mutableStateOf<GameReference?>(null) }
    var personalQuery by remember { mutableStateOf("") }
    var personalError by remember { mutableStateOf<String?>(null) }
    var exportingPersonal by remember { mutableStateOf<LibraryPack?>(null) }
    val personalPack = libraries.firstOrNull { it.library.id == LibraryIds.PERSONAL }
    val visiblePersonalGames = remember(personalPack, personalQuery) {
        personalPack?.games.orEmpty().filter {
            personalQuery.isBlank() || fuzzySearchMatch(it.title, personalQuery)
        }.sortedBy { it.title }
    }
    val wrongJsonType = stringResource(Res.string.tag_set_wrong_json_type)
    val jsonTooLarge = stringResource(Res.string.preset_json_too_large)
    val invalidJson = stringResource(Res.string.preset_invalid_json)
    val exportFailed = stringResource(Res.string.preset_export_failed)
    val scope = rememberCoroutineScope()
    val tagSetSaver = rememberExportLauncher(dialogSettings = FileKitDialogSettings.createDefault(), onResult = { file ->
        val set = exportingTagSet
        if (file != null && set != null) scope.launch {
            runCatching { file.write(tagSetJson.encodeToString(set).encodeToByteArray()) }
                .onFailure { tagSetError = it.message ?: exportFailed }
        }
        exportingTagSet = null
    })
    val tagSetPicker = rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("json"))) { file ->
        if (file != null) scope.launch {
            runCatching {
                val bytes = file.readBytes()
                require(bytes.size <= 30_000_000) { jsonTooLarge }
                tagSetJson.decodeFromString<GeneratorPreset>(bytes.decodeToString())
                    .also { require(it.isTagSet) { wrongJsonType } }
            }.onSuccess(onImportTagSet)
                .onFailure { tagSetError = it.message ?: invalidJson }
        }
    }
    val personalSaver = rememberExportLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file ->
            val pack = exportingPersonal
            if (file != null && pack != null) scope.launch {
                runCatching { file.write(tagSetJson.encodeToString(pack).encodeToByteArray()) }
                    .onFailure { personalError = it.message ?: exportFailed }
            }
            exportingPersonal = null
        },
    )
    val personalPicker = rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("json"))) { file ->
        if (file != null) onImportPersonalGames {
            val bytes = file.readBytes()
            require(bytes.size <= 30_000_000) { jsonTooLarge }
            bytes.decodeToString()
        }
    }
    val allEnabled =
        (listOf(LibraryIds.FIXTURE) + libraries.map { it.library.id }).none { it in disabledIds }
    val picker =
        rememberFilePickerLauncher(type = FileKitType.File(extensions = listOf("json"))) { file ->
            if (file != null)
                onImport {
                    val bytes = file.readBytes()
                    if (bytes.size > JsonLibraryImporter.MAX_CHARS)
                        fail(Res.string.import_too_large, JsonLibraryImporter.MAX_MEGABYTES)
                    bytes.decodeToString()
                }
        }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionLabel(stringResource(Res.string.tag_sets_title),
            stringResource(Res.string.tag_sets_intro)) }
        tagSetError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = { creatingTagSet = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.tag_set_create))
            }
        }
        item {
            OutlinedButton(onClick = { tagSetPicker.launch() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.tag_set_import))
            }
        }
        if (workspace.preferences.activePresetId != null) item {
            TextButton(onClick = onClearTagSet) { Text(stringResource(Res.string.preset_use_all_tags)) }
        }
        items(workspace.preferences.presets.filter { it.isTagSet }, key = { it.id }) { set ->
            val poolSize = remember(set) {
                (set.tags + if (set.includeGameTags) set.games.flatMap { it.tags } else emptyList())
                    .distinctBy { it.id }.size
            }
            StudioCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(set.name + if (workspace.preferences.activePresetId == set.id)
                        " · ${stringResource(Res.string.preset_active)}" else "",
                        style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.tag_set_summary, poolSize, set.games.size),
                        style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onApplyTagSet(set.id) }) {
                            Text(stringResource(Res.string.tag_set_apply))
                        }
                        TextButton(onClick = {
                            exportingTagSet = set
                            tagSetSaver.launch(suggestedName = "gamereel-tag-set", defaultExtension = "json")
                        }) { Text(stringResource(Res.string.preset_export_json)) }
                    }
                    TextButton(onClick = { onDeleteTagSet(set.id) }) {
                        Text(stringResource(Res.string.preset_delete))
                    }
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionLabel(stringResource(Res.string.personal_games_title),
            stringResource(Res.string.personal_games_intro)) }
        personalError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = { editingPersonalGame = null; personalEditorOpen = true },
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.personal_game_create))
            }
        }
        item {
            OutlinedButton(onClick = { personalPicker.launch() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.personal_games_import))
            }
        }
        item {
            OutlinedButton(onClick = {
                exportingPersonal = personalPack
                personalSaver.launch(suggestedName = "gamereel-my-games", defaultExtension = "json")
            }, enabled = personalPack?.games?.isNotEmpty() == true,
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.personal_games_export))
            }
        }
        if (personalPack != null) item {
            LibrarySwitch(LibraryIds.PERSONAL, stringResource(Res.string.personal_games_title),
                pluralStringResource(Res.plurals.games_count, personalPack.games.size,
                    personalPack.games.size), disabledIds, onToggle)
        }
        if (personalPack?.games?.isNotEmpty() == true) item {
            OutlinedTextField(personalQuery, { personalQuery = it },
                label = { Text(stringResource(Res.string.personal_games_search)) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        items(visiblePersonalGames, key = { it.id }) { game ->
            StudioCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(game.title, style = MaterialTheme.typography.titleMedium)
                    Text(game.tags.take(4).joinToString(" · ") { it.name } +
                        if (game.tags.size > 4) " · +${game.tags.size - 4}" else "",
                        style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { editingPersonalGame = game; personalEditorOpen = true }) {
                            Text(stringResource(Res.string.personal_game_edit))
                        }
                        TextButton(onClick = { deletingPersonalGame = game }) {
                            Text(stringResource(Res.string.preset_delete))
                        }
                    }
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionLabel(stringResource(Res.string.libraries_title),
            stringResource(Res.string.libraries_intro)) }
        if (!allEnabled)
            item {
                FilledTonalButton(
                    onClick = onEnableAll,
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.libraries_merge))
                }
            }

        item {
            Button(onClick = { picker.launch() }, enabled = !importing) {
                Text(
                    stringResource(if (importing) Res.string.importing else Res.string.import_json)
                )
            }
        }

        item {
            LibrarySwitch(
                LibraryIds.FIXTURE,
                stringResource(Res.string.fixture_library_name),
                pluralStringResource(
                    Res.plurals.described_components,
                    BuiltinCatalog.tags.size,
                    BuiltinCatalog.tags.size,
                ),
                disabledIds,
                onToggle,
            )
        }

        items(libraries.filterNot { it.library.id == LibraryIds.PERSONAL }, key = { it.library.id }) { pack ->
            val parts =
                listOfNotNull(
                    pack.games
                        .takeIf { it.isNotEmpty() }
                        ?.let { pluralStringResource(Res.plurals.games_count, it.size, it.size) },
                    pluralStringResource(
                        Res.plurals.tags_count,
                        pack.allTags.size,
                        pack.allTags.size,
                    ),
                    stringResource(Res.string.library_version, pack.library.version),
                )
            LibrarySwitch(
                pack.library.id,
                pack.displayName(),
                parts.joinToString(" · "),
                disabledIds,
                onToggle,
            )

            val info =
                when (pack.library.id) {
                    LibraryIds.CURATED -> Res.string.library_curated_info
                    LibraryIds.GOOGLE_PLAY -> Res.string.library_google_info
                    else -> null
                }
            info?.let {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        item {
            Text(
                stringResource(Res.string.libraries_pool),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                stringResource(Res.string.import_formats_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        item {
            Text(
                stringResource(Res.string.import_zip_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (creatingTagSet) TagSetCreatorDialog(libraries, onDismiss = { creatingTagSet = false }) {
        name, games, tags, includeGameTags ->
        onSaveTagSet(name, games, tags, includeGameTags)
        creatingTagSet = false
    }
    if (personalEditorOpen) PersonalGameDialog(editingPersonalGame,
        onDismiss = { personalEditorOpen = false }) { title, tags ->
        onSavePersonalGame(editingPersonalGame?.id, title, tags)
        personalEditorOpen = false
    }
    deletingPersonalGame?.let { game ->
        AlertDialog(
            onDismissRequest = { deletingPersonalGame = null },
            title = { Text(stringResource(Res.string.personal_game_delete_title)) },
            text = { Text(stringResource(Res.string.personal_game_delete_text, game.title)) },
            confirmButton = {
                TextButton(onClick = { onDeletePersonalGame(game.id); deletingPersonalGame = null }) {
                    Text(stringResource(Res.string.preset_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPersonalGame = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LibrarySwitch(
    id: String,
    name: String,
    subtitle: String,
    disabledIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    StudioCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Switch(checked = id !in disabledIds, onCheckedChange = { onToggle(id) })
        }
    }
}
