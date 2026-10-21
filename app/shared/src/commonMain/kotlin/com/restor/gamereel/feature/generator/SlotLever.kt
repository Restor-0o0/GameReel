package com.restor.gamereel.feature.generator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import com.restor.gamereel.ui.theme.ReelPalette
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SlotLever(motion: SpinMotion, unlocked: Boolean, onSpin: () -> Unit) {
    val leverDescription = stringResource(Res.string.lever_description)
    val spinLabel = stringResource(Res.string.spin)
    val accent = MaterialTheme.colorScheme.primary
    val currentSpin by rememberUpdatedState(onSpin)
    val available = unlocked && !motion.leverBusy && !motion.running
    Canvas(
        Modifier.width(38.dp)
            .height(158.dp)
            .semantics { contentDescription = leverDescription }
            .pointerInput(available) {
                var distance = 0f
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        distance = 0f
                        triggered = false
                    }
                ) { change, amount ->
                    if (available) {
                        change.consume()
                        distance += amount
                        if (!triggered && distance > 28.dp.toPx()) {
                            triggered = true
                            currentSpin()
                        }
                    }
                }
            }
            .clickable(
                enabled = available,
                role = Role.Button,
                onClickLabel = spinLabel,
                interactionSource = null,
                indication = null,
                onClick = onSpin,
            )
    ) {
        val movement = motion.leverProgress * size.height * 0.48f
        val pivot = Offset(size.width * 0.48f, size.height * 0.8f)
        val knob = Offset(size.width * 0.65f, size.height * 0.15f + movement)
        drawLine(
            ReelPalette.lever,
            pivot,
            knob,
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(ReelPalette.leverPivot, 10.dp.toPx(), pivot)
        drawCircle(
            if (unlocked) accent else ReelPalette.knobOff,
            12.dp.toPx(),
            knob,
        )
    }
}
