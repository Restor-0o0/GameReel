package com.restor.gamereel.feature.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SlotReel(
    reel: Reel,
    motion: SpinMotion,
    slotHeight: Dp,
    large: Boolean,
    onFreeze: (String) -> Unit,
    onPick: (Reel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = MaterialTheme.colorScheme.primary
    val freezeLabel = stringResource(Res.string.freeze_action)
    val unfreezeLabel = stringResource(Res.string.unfreeze_action)
    val pickLabel = stringResource(Res.string.choose_value)
    val canFreeze = reel.tag != null && reel.state != ReelState.PINNED

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    if (reel.state == ReelState.RANDOM) MaterialTheme.colorScheme.outline else gold,
                    RoundedCornerShape(15.dp),
                )
                .combinedClickable(
                    enabled = !motion.running,
                    role = Role.Button,
                    onClickLabel =
                        if (reel.state == ReelState.FROZEN) unfreezeLabel else freezeLabel,
                    onLongClickLabel = pickLabel,
                    onLongClick = { onPick(reel) },
                ) {
                    if (canFreeze) onFreeze(reel.id)
                }
        ) {
            RollingValue(
                reel,
                { motion.progressFor(reel) },
                true,
                Modifier.fillMaxWidth(),
                slotHeight,
            )

            if (reel.state != ReelState.RANDOM)
                Text(
                    if (reel.state == ReelState.PINNED) "◆" else "❄",
                    color = gold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                )
        }

        Text(
            reel.title(),
            fontSize = if (large) 11.sp else 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = gold.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp),
        )

        val settled by
            remember(motion, reel) {
                derivedStateOf { motion.progressFor(reel) >= 1f }
            }
        val result =
            if (!settled) "…" else reel.tag?.label() ?: stringResource(Res.string.no_result)
        Text(
            result,
            fontSize =
                when {
                    result.length > 38 -> 9.sp
                    result.length > 23 -> 10.sp
                    result.length > 16 -> 11.sp
                    large -> 14.sp
                    else -> 13.sp
                },
            lineHeight = if (result.length > 23) 12.sp else 16.sp,
            maxLines = 3,
            minLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}
