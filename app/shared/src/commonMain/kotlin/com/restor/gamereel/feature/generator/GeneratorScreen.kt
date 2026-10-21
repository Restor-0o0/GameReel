package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.components.SectionLabel
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GeneratorContent(
    workspace: Workspace,
    wide: Boolean,
    actions: GeneratorActions,
    gameNames: Map<String, String>,
    motion: SpinMotion,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onMode: (IdeaMode) -> Unit,
    onCancelVariant: () -> Unit,
    onSelect: (Reel) -> Unit,
    libraries: List<LibraryPack>,
    missingCategories: List<Category>,
) {
    val reels = workspace.reels
    var confirmingReset by remember { mutableStateOf(false) }
    var presetsOpen by remember { mutableStateOf(false) }
    var filterDialogOpen by remember { mutableStateOf(false) }
    var editingFilter by remember { mutableStateOf<CustomIdeaFilter?>(null) }
    val activeFilter = workspace.preferences.activeCustomIdeaFilterId?.let { id ->
        workspace.preferences.customIdeaFilters.firstOrNull { it.id == id }
    }
    val activeTagSet = workspace.preferences.activePresetId?.let { id ->
        workspace.preferences.presets.firstOrNull { it.id == id && it.isTagSet }
    }
    val filterSourcesUnavailable = activeFilter?.sourceIds?.none { id ->
        workspace.preferences.includesLibrary(id) &&
            (id == LibraryIds.FIXTURE || libraries.any { it.library.id == id })
    } == true
    val machine = workspace.preferences.generatorView == GeneratorView.MACHINE
    val (pinned, random) = remember(reels) { reels.partition { it.state == ReelState.PINNED } }
    val machineReels = remember(pinned, random) { pinned + random }
    val combinationIndex = rememberCombinationIndex(libraries, workspace.preferences)
    val spinNow = {
        if (!filterSourcesUnavailable && !motion.running && motion.pullLever()) actions.spin()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GeneratorToolbar(
                reels = reels,
                generatorView = workspace.preferences.generatorView,
                canUndo = workspace.history.isNotEmpty(),
                actions = actions,
                onAdd = onAdd,
                onSave = onSave,
                onReset = { confirmingReset = true },
                onPresets = { presetsOpen = true },
            )

            workspace.preferences.activeVariantParentId?.let { parentId ->
                val parent = workspace.ideas.firstOrNull { it.id == parentId }
                if (parent != null) Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(Res.string.idea_variant_of, parent.title),
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = onCancelVariant) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    enabled = workspace.preferences.customIdeaFilters.size < 30,
                    onClick = { editingFilter = null; filterDialogOpen = true },
                    label = { Text(stringResource(Res.string.filter_create_short)) },
                )
                IdeaMode.entries.forEach { mode ->
                    FilterChip(
                        selected = activeFilter == null && workspace.preferences.ideaMode == mode,
                        onClick = { onMode(mode) },
                        label = { Text(stringResource(when (mode) {
                            IdeaMode.OPEN -> Res.string.idea_mode_open
                            IdeaMode.SOLO -> Res.string.idea_mode_solo
                            IdeaMode.JAM -> Res.string.idea_mode_jam
                            IdeaMode.MOBILE -> Res.string.idea_mode_mobile
                            IdeaMode.OFFLINE -> Res.string.idea_mode_offline
                        })) },
                    )
                }
                workspace.preferences.customIdeaFilters.forEach { filter ->
                    FilterChip(
                        selected = activeFilter?.id == filter.id,
                        onClick = { actions.applyCustomIdeaFilter(filter.id) },
                        label = { Text(filter.name) },
                    )
                }
            }
            if (activeTagSet != null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(Res.string.generator_active_tag_set, activeTagSet.name),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = actions.clearTagSet) {
                            Text(stringResource(Res.string.preset_use_all_tags))
                        }
                    }
                }
            }
            if (missingCategories.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Text(stringResource(Res.string.generator_missing_tags,
                        missingCategories.map { stringResource(it.title) }.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    if (activeTagSet == null && activeFilter == null &&
                        (workspace.preferences.disabledLibraryIds.isNotEmpty() ||
                            workspace.preferences.enabledLibraryIds != null))
                        TextButton(onClick = actions.enableAllLibraries) {
                            Text(stringResource(Res.string.generator_expand_sources))
                        }
                    if (activeTagSet == null && activeFilter != null &&
                        !activeFilter.includeCreativeTwists && Category.TWIST in missingCategories)
                        TextButton(onClick = actions.enableCreativeTwistsAndSpin) {
                            Text(stringResource(Res.string.generator_add_creative_twists))
                        }
                }
            }
            if (activeFilter != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.filter_active_hint),
                        Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { editingFilter = activeFilter; filterDialogOpen = true }) {
                        Text(stringResource(Res.string.filter_edit))
                    }
                }
                if (filterSourcesUnavailable) Text(stringResource(Res.string.filter_no_sources),
                    Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            } else if (workspace.preferences.ideaMode != IdeaMode.OPEN)
                Text(stringResource(Res.string.idea_mode_hint),
                    Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!machine) {
                Button(
                    onClick = spinNow,
                    enabled = reels.any { it.state == ReelState.RANDOM } &&
                        !motion.leverBusy && !motion.running && !filterSourcesUnavailable,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.spin))
                }
            }

            Row(Modifier.weight(1f)) {
                if (wide && !machine)
                    LazyColumn(
                        Modifier.width(300.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            SectionLabel(
                                stringResource(Res.string.pinned_label),
                                stringResource(Res.string.pinned_wide_hint),
                            )
                        }

                        if (pinned.isEmpty()) item { Text(stringResource(Res.string.pinned_empty)) }
                        items(pinned, key = { it.id }) {
                            ReelCard(it, actions, gameNames, onSelect, motion)
                        }
                    }
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding =
                        PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (machine)
                        item {
                            SlotMachine(
                                reels = machineReels,
                                motion = motion,
                                canSpin = !filterSourcesUnavailable,
                                onSpin = spinNow,
                                onFreeze = actions.freeze,
                                onAdd = onAdd,
                                onUnfreezeAll = actions.unlockFrozen,
                                onReset = { confirmingReset = true },
                            )
                        }
                    if (!motion.running)
                        item(key = "combination-analysis") {
                            CombinationCards(reels, combinationIndex)
                        }
                    if (machine && motion.running)
                        item {
                            Text(
                                stringResource(Res.string.machine_waiting),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    if (
                        (!wide || machine) && pinned.isNotEmpty() && (!machine || !motion.running)
                    ) {
                        item {
                            SectionLabel(
                                stringResource(Res.string.pinned_label),
                                stringResource(Res.string.pinned_hint),
                            )
                        }

                        items(pinned, key = { it.id }) {
                            ReelCard(it, actions, gameNames, onSelect, motion)
                        }
                    }

                    if (!machine || !motion.running) {
                        item {
                            SectionLabel(
                                stringResource(
                                    if (machine) Res.string.combo_label else Res.string.reels_label
                                ),
                                pluralStringResource(
                                    Res.plurals.components_hint,
                                    random.size,
                                    random.size,
                                ),
                            )
                        }

                        if (random.isEmpty()) item { Text(stringResource(Res.string.reels_empty)) }
                        items(random, key = { it.id }) {
                            ReelCard(it, actions, gameNames, onSelect, motion)
                        }
                    }
                }
            }
        }

        FloatingSpinButton(
            workspace.preferences.spinButtonPosition,
            reels.any { it.state == ReelState.RANDOM } &&
                !motion.leverBusy && !motion.running && !filterSourcesUnavailable,
            spinNow,
            actions.setSpinButtonPosition,
        )
    }

    if (presetsOpen) PresetsDialog(workspace, actions) { presetsOpen = false }

    if (filterDialogOpen) CustomIdeaFilterDialog(
        workspace = workspace,
        libraries = libraries,
        editing = editingFilter,
        onDismiss = { filterDialogOpen = false },
        onSave = actions.saveCustomIdeaFilter,
        onDelete = actions.deleteCustomIdeaFilter,
    )

    if (confirmingReset)
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text(stringResource(Res.string.reset_title)) },
            text = { Text(stringResource(Res.string.reset_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.resetReels()
                        confirmingReset = false
                    }
                ) {
                    Text(stringResource(Res.string.reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
}
