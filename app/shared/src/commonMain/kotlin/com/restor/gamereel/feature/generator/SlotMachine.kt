package com.restor.gamereel.feature.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.model.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun SlotMachine(
    reels: List<Reel>,
    motion: SpinMotion,
    canSpin: Boolean,
    onSpin: () -> Unit,
    onFreeze: (String) -> Unit,
    onAdd: () -> Unit,
    onUnfreezeAll: () -> Unit,
    onReset: () -> Unit,
) {
    val unlocked = reels.any { it.state == ReelState.RANDOM }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(
                if (motion.running) Res.string.reels_spinning else Res.string.freeze_hint
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            SlotReelGrid(reels, motion, onFreeze, Modifier.weight(1f))

            SlotLever(motion, unlocked && canSpin, onSpin)
        }

        MachineActions(
            canUnfreeze = reels.any { it.state == ReelState.FROZEN },
            onAdd = onAdd,
            onUnfreezeAll = onUnfreezeAll,
            onReset = onReset,
        )
    }
}

@Composable
private fun MachineActions(
    canUnfreeze: Boolean,
    onAdd: () -> Unit,
    onUnfreezeAll: () -> Unit,
    onReset: () -> Unit,
) {
    val actions =
        listOf(
            Triple(stringResource(Res.string.machine_action_add), "+", onAdd),
            Triple(
                stringResource(Res.string.machine_action_unfreeze),
                "❄",
                onUnfreezeAll,
            ),
            Triple(stringResource(Res.string.machine_action_reset), "↶", onReset),
        )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        actions.forEachIndexed { index, (label, icon, action) ->
            val enabled = index != 1 || canUnfreeze
            MachineActionButton(label, icon, enabled, action, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MachineActionButton(
    label: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 9.dp),
        modifier = modifier.heightIn(min = 62.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(icon, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, lineHeight = 18.sp)
            Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
        }
    }
}
