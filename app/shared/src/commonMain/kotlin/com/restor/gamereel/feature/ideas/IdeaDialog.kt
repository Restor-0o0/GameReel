package com.restor.gamereel.feature.ideas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.model.IdeaConcept
import com.restor.gamereel.model.IdeaOutcome
import com.restor.gamereel.model.IdeaStage
import com.restor.gamereel.model.SavedIdea
import com.restor.gamereel.text.UiText
import com.restor.gamereel.text.resolve
import com.restor.gamereel.ui.presentation.title
import kotlinx.coroutines.launch
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun IdeaDialog(
    idea: SavedIdea?,
    defaultProject: String = "",
    onDismiss: () -> Unit,
    onSave: suspend (String, String, IdeaConcept, String, IdeaOutcome) -> UiText?,
) {
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<UiText?>(null) }
    var title by rememberSaveable(idea?.id) { mutableStateOf(idea?.title.orEmpty()) }
    var notes by rememberSaveable(idea?.id) { mutableStateOf(idea?.notes.orEmpty()) }
    var project by rememberSaveable(idea?.id, defaultProject) { mutableStateOf(idea?.project ?: defaultProject) }
    var pitch by rememberSaveable(idea?.id) { mutableStateOf(idea?.concept?.pitch.orEmpty()) }
    var coreLoop by rememberSaveable(idea?.id) { mutableStateOf(idea?.concept?.coreLoop.orEmpty()) }
    var playerGoal by rememberSaveable(idea?.id) { mutableStateOf(idea?.concept?.playerGoal.orEmpty()) }
    var hook by rememberSaveable(idea?.id) { mutableStateOf(idea?.concept?.hook.orEmpty()) }
    var stage by rememberSaveable(idea?.id) { mutableStateOf(idea?.outcome?.stage ?: IdeaStage.IDEA) }
    var resultUrl by rememberSaveable(idea?.id) { mutableStateOf(idea?.outcome?.url.orEmpty()) }
    var detailsExpanded by rememberSaveable(idea?.id) {
        mutableStateOf(idea?.concept?.let {
            it.pitch.isNotBlank() || it.coreLoop.isNotBlank() ||
                it.playerGoal.isNotBlank() || it.hook.isNotBlank()
        } == true)
    }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(12.dp).widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize()) {
                Text(stringResource(if (idea == null) Res.string.save_idea else Res.string.edit_idea),
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(title, { title = it.take(120) },
                        enabled = !saving, label = { Text(stringResource(Res.string.idea_title_hint)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(project, { project = it.take(80) },
                        enabled = !saving, label = { Text(stringResource(Res.string.idea_project)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it },
                        enabled = !saving, label = { Text(stringResource(Res.string.idea_notes)) },
                        minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                    if (idea != null) {
                        Text(stringResource(Res.string.idea_stage), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IdeaStage.entries.forEach { option ->
                                FilterChip(stage == option, onClick = { stage = option }, enabled = !saving,
                                    label = { Text(stringResource(option.title)) })
                            }
                        }
                        OutlinedTextField(resultUrl, { resultUrl = it.take(500) },
                            enabled = !saving, label = { Text(stringResource(Res.string.idea_result_url)) },
                            placeholder = { Text("https://") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    HorizontalDivider()
                    OutlinedButton(onClick = { detailsExpanded = !detailsExpanded },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.idea_concept_heading))
                        Spacer(Modifier.weight(1f))
                        Text(if (detailsExpanded) "⌃" else "⌄")
                    }
                    if (detailsExpanded) {
                        OutlinedTextField(pitch, { pitch = it },
                            enabled = !saving, label = { Text(stringResource(Res.string.idea_pitch)) },
                            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(coreLoop, { coreLoop = it },
                            enabled = !saving, label = { Text(stringResource(Res.string.idea_core_loop)) },
                            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(playerGoal, { playerGoal = it },
                            enabled = !saving, label = { Text(stringResource(Res.string.idea_player_goal)) },
                            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(hook, { hook = it },
                            enabled = !saving, label = { Text(stringResource(Res.string.idea_hook)) },
                            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                    }
                }
                HorizontalDivider()
                saveError?.let { Text(it.resolve(), Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text(stringResource(Res.string.cancel)) }
                    Button(enabled = !saving, onClick = {
                        if (!saving) {
                            saving = true
                            saveError = null
                            scope.launch {
                                try {
                                    saveError = onSave(title.trim(), notes.trim(),
                                        IdeaConcept(pitch.trim(), coreLoop.trim(), playerGoal.trim(), hook.trim()),
                                        project.trim(), IdeaOutcome(stage, resultUrl.trim()))
                                    if (saveError == null) onDismiss()
                                } finally { saving = false }
                            }
                        }
                    }) { Text(stringResource(Res.string.save)) }
                }
            }
        }
    }
}
