package com.restor.gamereel.feature.ideas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.share.IdeaShareCode
import com.restor.gamereel.ui.components.SectionLabel
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.ui.presentation.label
import com.restor.gamereel.ui.presentation.title
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import com.restor.gamereel.platform.rememberExportLauncher
import kotlinx.coroutines.launch
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SavedContent(
    ideas: List<SavedIdea>,
    onLoad: (String) -> Unit,
    onEdit: (SavedIdea) -> Unit,
    onVariant: (String) -> Unit,
    onDelete: (String) -> Unit,
    onGoToGenerator: () -> Unit,
    onLinkCopied: () -> Unit,
) {
    var deleting by remember { mutableStateOf<SavedIdea?>(null) }
    var comparing by remember { mutableStateOf<SavedIdea?>(null) }
    var exporting by remember { mutableStateOf<SavedIdea?>(null) }
    var exportAsImage by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf<String?>(null) }
    var selectedStage by remember { mutableStateOf<IdeaStage?>(null) }
    val uriHandler = LocalUriHandler.current
    var exportError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val pitchLabel = stringResource(Res.string.idea_pitch)
    val loopLabel = stringResource(Res.string.idea_core_loop)
    val goalLabel = stringResource(Res.string.idea_player_goal)
    val hookLabel = stringResource(Res.string.idea_hook)
    val notesLabel = stringResource(Res.string.idea_notes)
    val projectLabel = stringResource(Res.string.idea_project)
    val stageLabel = stringResource(Res.string.idea_stage)
    val resultLabel = stringResource(Res.string.idea_result_url)
    val stageLabels = IdeaStage.entries.associateWith { stringResource(it.title) }
    val categoryLabels = Category.entries.associateWith { stringResource(it.title) }
    val exportFailed = stringResource(Res.string.preset_export_failed)
    val textFor: (SavedIdea) -> String = { idea ->
        buildString {
            appendLine(idea.title)
            if (idea.project.isNotBlank()) appendLine("$projectLabel: ${idea.project}")
            if (idea.outcome.stage != IdeaStage.IDEA) appendLine("$stageLabel: ${stageLabels[idea.outcome.stage]}")
            if (idea.outcome.url.isNotBlank()) appendLine("$resultLabel: ${idea.outcome.url}")
            idea.reels.forEach { reel -> reel.tag?.let {
                appendLine("${reel.slot?.name ?: categoryLabels[reel.category]}: ${it.name}")
            } }
            if (idea.concept.pitch.isNotBlank()) appendLine("$pitchLabel: ${idea.concept.pitch}")
            if (idea.concept.coreLoop.isNotBlank()) appendLine("$loopLabel: ${idea.concept.coreLoop}")
            if (idea.concept.playerGoal.isNotBlank()) appendLine("$goalLabel: ${idea.concept.playerGoal}")
            if (idea.concept.hook.isNotBlank()) appendLine("$hookLabel: ${idea.concept.hook}")
            if (idea.notes.isNotBlank()) appendLine("$notesLabel: ${idea.notes}")
            appendLine(IdeaShareCode.link(idea.reels, idea.title))
        }
    }
    val saver = rememberExportLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { file ->
        val idea = exporting
        if (file != null && idea != null) scope.launch {
            runCatching {
                val content = textFor(idea)
                file.write((if (exportAsImage) ideaSvg(content) else content).encodeToByteArray())
            }
                .onFailure { exportError = it.message ?: exportFailed }
        }
        exporting = null
    }
    val projects = remember(ideas) { ideas.map { it.project }.filter { it.isNotBlank() }.distinct().sorted() }
    val activeProject = selectedProject.takeIf { it in projects }
    LaunchedEffect(activeProject) { selectedProject = activeProject }
    val stages = remember(ideas) { ideas.map { it.outcome.stage }.distinct().sorted() }
    val activeStage = selectedStage.takeIf { it in stages }
    val visibleIdeas = remember(ideas, activeProject, activeStage) {
        ideas.filter {
            (activeProject == null || it.project == activeProject) &&
                (activeStage == null || it.outcome.stage == activeStage)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionLabel(
                stringResource(Res.string.ideas_label),
                pluralStringResource(Res.plurals.ideas_count, visibleIdeas.size, visibleIdeas.size),
            )
        }
        if (projects.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(activeProject == null, onClick = { selectedProject = null },
                    label = { Text(stringResource(Res.string.ideas_all_projects)) })
                projects.forEach { project ->
                    FilterChip(activeProject == project, onClick = { selectedProject = project },
                        label = { Text(project) })
                }
            }
        }
        if (stages.size > 1) item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(activeStage == null, onClick = { selectedStage = null },
                    label = { Text(stringResource(Res.string.ideas_all_stages)) })
                stages.forEach { stage ->
                    FilterChip(activeStage == stage, onClick = { selectedStage = stage },
                        label = { Text(stringResource(stage.title)) })
                }
            }
        }
        exportError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }

        if (ideas.isEmpty())
            item {
                StudioCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("✦", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(Res.string.ideas_empty), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onGoToGenerator) {
                            Text(stringResource(Res.string.ideas_go_generator))
                        }
                    }
                }
            }
        items(visibleIdeas, key = { it.id }) { idea ->
            var menuOpen by remember(idea.id) { mutableStateOf(false) }
            StudioCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        idea.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (idea.project.isNotBlank() || idea.outcome.stage != IdeaStage.IDEA)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (idea.outcome.stage != IdeaStage.IDEA) Surface(shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(stringResource(idea.outcome.stage.title),
                                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            if (idea.project.isNotBlank()) Text(idea.project,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    idea.variantOfId?.let { parentId ->
                        ideas.firstOrNull { it.id == parentId }?.let { parent ->
                            Text(stringResource(Res.string.idea_variant_of, parent.title),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    val taggedReels = idea.reels.filter { it.tag != null }
                    if (taggedReels.isNotEmpty()) FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        taggedReels.take(4).forEach { reel ->
                            Surface(shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(stringResource(Res.string.labeled_value,
                                    reel.title(), reel.tag!!.label()),
                                    Modifier.widthIn(max = 240.dp)
                                        .padding(horizontal = 9.dp, vertical = 6.dp),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (taggedReels.size > 4) Text("+${taggedReels.size - 4}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val summary = idea.concept.pitch.ifBlank { idea.notes }
                    if (summary.isNotBlank()) Text(summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider()
                    FlowRow(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box {
                            TextButton(onClick = { menuOpen = true }) {
                                Text(stringResource(Res.string.more_actions))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.idea_make_variant)) },
                                    onClick = { menuOpen = false; onVariant(idea.id) })
                                if (idea.variantOfId != null && ideas.any { it.id == idea.variantOfId })
                                    DropdownMenuItem(text = { Text(stringResource(Res.string.idea_compare)) },
                                        onClick = { menuOpen = false; comparing = idea })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.share_link)) },
                                    onClick = {
                                        menuOpen = false
                                        clipboard.setText(AnnotatedString(IdeaShareCode.link(idea.reels, idea.title)))
                                        onLinkCopied()
                                    })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.idea_copy_text)) },
                                    onClick = {
                                        menuOpen = false
                                        clipboard.setText(AnnotatedString(textFor(idea)))
                                    })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.idea_export_text)) },
                                    onClick = {
                                        menuOpen = false
                                        exporting = idea
                                        exportAsImage = false
                                        saver.launch(suggestedName = "gamereel-idea", defaultExtension = "txt")
                                    })
                                DropdownMenuItem(text = { Text(stringResource(Res.string.idea_export_image)) },
                                    onClick = {
                                        menuOpen = false
                                        exporting = idea
                                        exportAsImage = true
                                        saver.launch(suggestedName = "gamereel-idea", defaultExtension = "svg")
                                    })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text(stringResource(Res.string.delete)) },
                                    onClick = { menuOpen = false; deleting = idea })
                            }
                        }
                        if (idea.outcome.url.startsWith("https://") || idea.outcome.url.startsWith("http://"))
                            TextButton(onClick = { runCatching { uriHandler.openUri(idea.outcome.url) } }) {
                                Text(stringResource(Res.string.idea_open_result))
                            }
                        TextButton(onClick = { onEdit(idea) }) {
                            Text(stringResource(Res.string.edit))
                        }
                        Button(onClick = { onLoad(idea.id) }) {
                            Text(stringResource(Res.string.load))
                        }
                    }
                }
            }
        }
    }
    deleting?.let { idea ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(Res.string.delete_idea_title, idea.title)) },
            text = { Text(stringResource(Res.string.delete_idea_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(idea.id)
                        deleting = null
                    }
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
    comparing?.let { idea ->
        val parent = ideas.firstOrNull { it.id == idea.variantOfId }
        if (parent != null) AlertDialog(
            onDismissRequest = { comparing = null },
            title = { Text(stringResource(Res.string.idea_compare)) },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(parent.title, style = MaterialTheme.typography.titleMedium)
                    Text(textFor(parent), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    Text(idea.title, style = MaterialTheme.typography.titleMedium)
                    Text(textFor(idea), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { comparing = null }) {
                Text(stringResource(Res.string.close))
            } },
        )
    }
}
