package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReelCard(
    reel: Reel,
    actions: GeneratorActions,
    gameNames: Map<String, String>,
    onSelect: (Reel) -> Unit,
    motion: SpinMotion,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
        border = BorderStroke(
            1.dp,
            if (reel.state == ReelState.PINNED)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reel.title().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (reel.state == ReelState.PINNED)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        stringResource(
                            when (reel.state) {
                                ReelState.RANDOM -> Res.string.state_random
                                ReelState.FROZEN -> Res.string.state_frozen
                                ReelState.PINNED -> Res.string.state_pinned
                            }
                        ),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reel.state == ReelState.PINNED)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BoxWithConstraints {
                val artWidth = (maxWidth * 0.22f).coerceIn(64.dp, 96.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagArt(
                        reel.tag?.takeIf { motion.settled(reel) },
                        reel.symbol,
                        26.sp,
                        Modifier.padding(end = 10.dp)
                            .size(artWidth, artWidth * ArtRatio)
                            .clip(RoundedCornerShape(12.dp)),
                    )

                    Column(Modifier.weight(1f)) {
                        RollingValue(
                            reel,
                            { motion.progressFor(reel) },
                            false,
                            Modifier.fillMaxWidth(),
                            align = Alignment.CenterStart,
                        )

                        if (motion.settled(reel))
                            reel.tag?.fullName?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                    }
                }
            }

            if (motion.settled(reel))
                reel.tag
                    ?.about()
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            reel.tag?.let { tag ->
                val unknownSource = stringResource(Res.string.saved_library_source)
                val games =
                    remember(tag, gameNames, unknownSource) {
                        val source =
                            tag.sourceGameId?.let { gameTitle(it, gameNames) ?: unknownSource }
                        (listOfNotNull(source) + tag.referenceGames)
                            .distinctBy { it.lowercase() }
                            .take(3)
                    }
                referenceLabel(games)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (reel.state != ReelState.PINNED) {
                    TextButton(
                        onClick = { actions.reroll(reel.id) },
                        enabled = reel.state == ReelState.RANDOM,
                    ) {
                        Text(stringResource(Res.string.reroll))
                    }

                    TextButton(onClick = { actions.freeze(reel.id) }, enabled = reel.tag != null) {
                        Text(
                            stringResource(
                                if (reel.state == ReelState.FROZEN) Res.string.unfreeze
                                else Res.string.freeze
                            )
                        )
                    }
                }

                TextButton(onClick = { actions.pin(reel.id) }, enabled = reel.tag != null) {
                    Text(
                        stringResource(
                            if (reel.state == ReelState.PINNED) Res.string.unpin else Res.string.pin
                        )
                    )
                }
                Box {
                    TextButton(onClick = { menu = true }) { Text("···") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.choose_value)) },
                            onClick = {
                                menu = false
                                onSelect(reel)
                            },
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.remove_reel)) },
                            onClick = {
                                menu = false
                                actions.remove(reel.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
