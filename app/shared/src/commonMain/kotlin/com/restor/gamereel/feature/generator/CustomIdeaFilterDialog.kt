package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.CustomIdeaFilter
import com.restor.gamereel.model.IdeaMode
import com.restor.gamereel.model.LibraryIds
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.model.Workspace
import com.restor.gamereel.model.includesLibrary
import com.restor.gamereel.model.normalizeFilterTerms
import com.restor.gamereel.ui.presentation.displayName
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CustomIdeaFilterDialog(
    workspace: Workspace,
    libraries: List<LibraryPack>,
    editing: CustomIdeaFilter?,
    onDismiss: () -> Unit,
    onSave: (CustomIdeaFilter) -> Unit,
    onDelete: (String) -> Unit,
) {
    var name by remember(editing?.id) { mutableStateOf(editing?.name.orEmpty()) }
    var baseMode by remember(editing?.id) { mutableStateOf(editing?.baseMode ?: IdeaMode.OPEN) }
    var termsText by remember(editing?.id) {
        mutableStateOf(editing?.excludedTerms?.joinToString(", ").orEmpty())
    }
    var restrictSources by remember(editing?.id) { mutableStateOf(editing?.sourceIds != null) }
    var includeCreativeTwists by remember(editing?.id) {
        mutableStateOf(editing?.includeCreativeTwists == true)
    }
    val availableSources = (listOf(LibraryIds.FIXTURE) + libraries.map { it.library.id }).toSet()
    var selectedSources by remember(editing?.id) {
        mutableStateOf(editing?.sourceIds?.intersect(availableSources) ?:
            availableSources.filter { workspace.preferences.includesLibrary(it) }.toSet())
    }
    var deleteRequested by remember(editing?.id) { mutableStateOf(false) }
    val terms = normalizeFilterTerms(termsText.split(',', '\n'))
    val hasRules = baseMode != IdeaMode.OPEN || terms.isNotEmpty() || restrictSources ||
        includeCreativeTwists
    val canSave = name.isNotBlank() && hasRules && (!restrictSources || selectedSources.isNotEmpty())

    if (deleteRequested && editing != null) {
        AlertDialog(
            onDismissRequest = { deleteRequested = false },
            title = { Text(stringResource(Res.string.filter_delete_title)) },
            text = { Text(stringResource(Res.string.filter_delete_text, editing.name)) },
            confirmButton = { TextButton(onClick = {
                onDelete(editing.id)
                onDismiss()
            }) { Text(stringResource(Res.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleteRequested = false }) {
                Text(stringResource(Res.string.cancel))
            } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editing == null) Res.string.filter_create
            else Res.string.filter_edit)) },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(80) },
                    label = { Text(stringResource(Res.string.filter_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(stringResource(Res.string.filter_base_mode),
                    style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdeaMode.entries.forEach { mode ->
                        FilterChip(selected = baseMode == mode,
                            onClick = { baseMode = mode },
                            label = { Text(stringResource(when (mode) {
                                IdeaMode.OPEN -> Res.string.idea_mode_open
                                IdeaMode.SOLO -> Res.string.idea_mode_solo
                                IdeaMode.JAM -> Res.string.idea_mode_jam
                                IdeaMode.MOBILE -> Res.string.idea_mode_mobile
                                IdeaMode.OFFLINE -> Res.string.idea_mode_offline
                            })) })
                    }
                }
                OutlinedTextField(termsText, { termsText = it.take(800) },
                    label = { Text(stringResource(Res.string.filter_excluded_terms)) },
                    supportingText = { Text(stringResource(Res.string.filter_terms_hint)) },
                    minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.filter_creative_twists),
                        modifier = Modifier.weight(1f))
                    Switch(checked = includeCreativeTwists,
                        onCheckedChange = { includeCreativeTwists = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.filter_fixed_sources),
                        modifier = Modifier.weight(1f))
                    Switch(checked = restrictSources, onCheckedChange = { restrictSources = it })
                }
                if (restrictSources) {
                    Text(stringResource(Res.string.filter_sources_hint),
                        style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sources = listOf(LibraryIds.FIXTURE) + libraries.map { it.library.id }
                        sources.forEach { id ->
                            val label = if (id == LibraryIds.FIXTURE)
                                stringResource(Res.string.fixture_library_name)
                            else libraries.first { it.library.id == id }.displayName()
                            FilterChip(selected = id in selectedSources,
                                onClick = { selectedSources = if (id in selectedSources)
                                    selectedSources - id else selectedSources + id },
                                label = { Text(label) })
                        }
                    }
                }
                if (!hasRules) Text(stringResource(Res.string.filter_need_rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = {
            onSave(CustomIdeaFilter(
                id = editing?.id.orEmpty(),
                name = name,
                baseMode = baseMode,
                excludedTerms = terms,
                sourceIds = if (restrictSources) selectedSources else null,
                includeCreativeTwists = includeCreativeTwists,
            ))
            onDismiss()
        }, enabled = canSave) { Text(stringResource(Res.string.save)) } },
        dismissButton = {
            Row {
                if (editing != null) TextButton(onClick = { deleteRequested = true }) {
                    Text(stringResource(Res.string.delete))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
            }
        },
    )
}
