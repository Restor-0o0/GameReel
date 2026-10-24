package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.share.IdeaShareCode
import com.restor.gamereel.ui.components.*
import com.restor.gamereel.ui.presentation.title
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GeneratorToolbar(
    reels: List<Reel>,
    generatorView: GeneratorView,
    canUndo: Boolean,
    actions: GeneratorActions,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onPresets: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var openingShared by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val undoDescription = stringResource(Res.string.undo_description)
    val menuDescription = stringResource(Res.string.generator_menu)
    val presetsDescription = stringResource(Res.string.presets_title)

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
    ScreenHeader(stringResource(Res.string.nav_generator)) {
        IconButton(
            onClick = actions.undo,
            enabled = canUndo,
            modifier = Modifier.semantics { contentDescription = undoDescription },
        ) {
            Text("↶", style = MaterialTheme.typography.headlineSmall)
        }

        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.semantics { contentDescription = menuDescription },
            ) {
                Text("⋮", style = MaterialTheme.typography.headlineSmall)
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.daily_idea)) },
                    onClick = {
                        menuOpen = false
                        actions.dailyIdea()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.share_link)) },
                    enabled = reels.any { it.tag != null },
                    onClick = {
                        menuOpen = false
                        clipboard.setText(AnnotatedString(IdeaShareCode.link(reels)))
                        actions.linkCopied()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.open_shared)) },
                    onClick = {
                        menuOpen = false
                        openingShared = true
                    },
                )
                HorizontalDivider()
                GeneratorView.entries.forEach { view ->
                    DropdownMenuItem(
                        text = {
                            val title = stringResource(view.title)
                            Text(
                                if (generatorView == view)
                                    stringResource(Res.string.checked_option, title)
                                else title
                            )
                        },
                        onClick = {
                            menuOpen = false
                            actions.setGeneratorView(view)
                        },
                    )
                }

                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.spin_button_reset)) },
                    onClick = {
                        menuOpen = false
                        actions.setSpinButtonPosition(SpinButtonPosition())
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.add_reel)) },
                    onClick = {
                        menuOpen = false
                        onAdd()
                    },
                )

                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.unfreeze_all)) },
                    enabled = reels.any { it.state == ReelState.FROZEN },
                    onClick = {
                        menuOpen = false
                        actions.unlockFrozen()
                    },
                )

                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.reset_reels)) },
                    onClick = {
                        menuOpen = false
                        onReset()
                    },
                )
            }
        }
        SettingsButton(actions.openSettings)
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        PrimaryAction(stringResource(Res.string.save), onSave, Modifier.weight(1.1f),
            enabled = reels.any { it.tag != null })
        TertiaryAction(stringResource(Res.string.presets_button), onPresets,
            Modifier.weight(1.2f).semantics { contentDescription = presetsDescription })
        TertiaryAction(stringResource(Res.string.add_reel_short), onAdd, Modifier.weight(1f))
    }
    }
    if (openingShared) OpenSharedDialog(actions.openShared) { openingShared = false }
}

@Composable
private fun OpenSharedDialog(open: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.open_shared)) },
        text = {
            OutlinedTextField(text, { text = it.take(20_000); invalid = false },
                label = { Text(stringResource(Res.string.open_shared_hint)) },
                isError = invalid,
                supportingText = if (invalid) { { Text(stringResource(Res.string.open_shared_invalid)) } } else null,
                maxLines = 4, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = {
                if (open(text)) onDismiss() else invalid = true
            }) { Text(stringResource(Res.string.open)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
