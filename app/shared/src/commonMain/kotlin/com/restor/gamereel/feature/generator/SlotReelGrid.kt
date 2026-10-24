package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SlotReelGrid(
    reels: List<Reel>,
    motion: SpinMotion,
    onFreeze: (String) -> Unit,
    onPick: (Reel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth < 420.dp) 3 else 4
        val columnWidth = (maxWidth - 8.dp * (columns - 1)) / columns
        val slotHeight = columnWidth.coerceIn(76.dp, 160.dp)
        val large = columnWidth > 105.dp
        val rows = remember(reels, columns) { reels.chunked(columns) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (reels.isEmpty())
                Text(stringResource(Res.string.slot_empty), Modifier.padding(20.dp))
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { reel ->
                        key(reel.id) {
                            SlotReel(
                                reel,
                                motion,
                                slotHeight,
                                large,
                                onFreeze,
                                onPick,
                                Modifier.weight(1f),
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
