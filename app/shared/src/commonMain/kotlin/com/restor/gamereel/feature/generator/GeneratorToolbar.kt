package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
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
    val undoDescription = stringResource(Res.string.undo_description)
    val menuDescription = stringResource(Res.string.generator_menu)
    val presetsDescription = stringResource(Res.string.presets_title)

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.nav_generator),
                style = MaterialTheme.typography.titleLarge,
            )
        }

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
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onSave, enabled = reels.any { it.tag != null }, modifier = Modifier.weight(1.1f).heightIn(min = 46.dp),
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(stringResource(Res.string.save), maxLines = 1)
        }
        OutlinedButton(onClick = onPresets,
            modifier = Modifier.weight(1.2f).heightIn(min = 46.dp).semantics { contentDescription = presetsDescription },
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(stringResource(Res.string.presets_button), Modifier.fillMaxWidth(),
                maxLines = 1, textAlign = TextAlign.Center)
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f).heightIn(min = 46.dp),
            shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(stringResource(Res.string.add_reel_short), maxLines = 1)
        }
    }
    }
}
