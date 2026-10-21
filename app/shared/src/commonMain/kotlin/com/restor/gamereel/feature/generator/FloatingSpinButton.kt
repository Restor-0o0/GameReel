package com.restor.gamereel.feature.generator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.restor.gamereel.model.SpinButtonPosition
import gamereel.app.shared.generated.resources.*
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FloatingSpinButton(
    position: SpinButtonPosition,
    enabled: Boolean,
    onSpin: () -> Unit,
    onMove: (SpinButtonPosition) -> Unit,
) {
    var local by remember(position) { mutableStateOf(position) }
    val save by rememberUpdatedState(onMove)
    val saved by rememberUpdatedState(position)
    val spin = stringResource(Res.string.spin)
    val hint = stringResource(Res.string.spin_button_hint)
    val left = stringResource(Res.string.spin_button_left)
    val right = stringResource(Res.string.spin_button_reset)

    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val density = LocalDensity.current
        val width = with(density) { (maxWidth - 56.dp).coerceAtLeast(0.dp).toPx() }
        val height = with(density) { (maxHeight - 56.dp).coerceAtLeast(0.dp).toPx() }
        FloatingActionButton(
            onClick = { if (enabled) onSpin() },
            shape = CircleShape,
            containerColor =
                if (enabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
                if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.size(56.dp)
                    .offset {
                        IntOffset(
                            (local.x.coerceIn(0f, 1f) * width).roundToInt(),
                            (local.y.coerceIn(0f, 1f) * height).roundToInt(),
                        )
                    }
                    .pointerInput(width, height) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = { save(local) },
                            onDragCancel = { local = saved },
                        ) { change, delta ->
                            change.consume()
                            local =
                                SpinButtonPosition(
                                    if (width > 0) (local.x + delta.x / width).coerceIn(0f, 1f)
                                    else 0f,
                                    if (height > 0) (local.y + delta.y / height).coerceIn(0f, 1f)
                                    else 0f,
                                )
                        }
                    }
                    .semantics {
                        contentDescription = spin
                        stateDescription = hint
                        if (!enabled) disabled()
                        customActions =
                            listOf(
                                CustomAccessibilityAction(left) {
                                    local = SpinButtonPosition(0f, 1f)
                                    save(local)
                                    true
                                },
                                CustomAccessibilityAction(right) {
                                    local = SpinButtonPosition()
                                    save(local)
                                    true
                                },
                            )
                    },
        ) {
            SpinIcon()
        }
    }
}

// Geometric icon: its alignment does not depend on the font's baseline or glyph metrics.
@Composable
private fun SpinIcon() {
    val color = LocalContentColor.current
    val arrow = remember {
        Path().apply {
            moveTo(17.65f, 6.35f)
            cubicTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
            cubicTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
            cubicTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
            cubicTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
            lineTo(17.65f, 14f)
            cubicTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
            cubicTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
            cubicTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
            cubicTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
            lineTo(13f, 11f)
            lineTo(20f, 11f)
            lineTo(20f, 4f)
            close()
        }
    }
    Canvas(Modifier.size(24.dp)) {
        scale(size.minDimension / 24f, pivot = Offset.Zero) { drawPath(arrow, color) }
    }
}
