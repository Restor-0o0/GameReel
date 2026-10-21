package com.restor.gamereel.feature.generator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.Reel
import com.restor.gamereel.ui.presentation.displayIcon
import com.restor.gamereel.ui.presentation.label
import com.restor.gamereel.ui.presentation.symbol
import com.restor.gamereel.ui.presentation.title
import com.restor.gamereel.ui.theme.ReelPalette
import gamereel.app.shared.generated.resources.*
import kotlin.math.floor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RollingArtwork(
    reel: Reel,
    progress: () -> Float,
    modifier: Modifier,
    height: Dp,
) {
    val pool =
        remember(reel.category) {
            BuiltinCatalog.tags.filter { it.category == reel.category }.map { it.displayIcon }
        }
    val finalIcon = reel.tag?.displayIcon ?: reel.category.symbol
    val measurer = rememberTextMeasurer()
    val style =
        LocalTextStyle.current.copy(
            color = LocalContentColor.current,
            fontSize = (height.value * 0.44f).coerceAtMost(60f).sp,
        )
    val previews =
        remember(pool, measurer, style) {
            pool.map { measurer.measure(AnnotatedString(it), style) }
        }
    val fallback =
        remember(finalIcon, measurer, style) {
            measurer.measure(AnnotatedString(finalIcon), style)
        }
    val shade = remember {
        Brush.verticalGradient(
            listOf(
                ReelPalette.slotShade,
                Color.Transparent,
                Color.Transparent,
                ReelPalette.slotShade,
            )
        )
    }
    val description =
        stringResource(
            Res.string.labeled_value,
            stringResource(reel.category.title),
            reel.tag?.label() ?: stringResource(Res.string.no_result),
        )

    // Only this draw callback reads frame progress. No Text/Image nodes are replaced,
    // measured or laid out as the preview symbols pass through the window.
    Canvas(
        modifier.height(height).clip(RoundedCornerShape(10.dp)).semantics {
            contentDescription = description
        }
    ) {
        val phase = progress().coerceIn(0f, 1f) * SpinSteps
        val current = floor(phase).toInt()
        clipRect {
            for (index in current - 1..current + 1) {
                val top = (index - phase) * size.height
                val final = index >= SpinSteps || previews.isEmpty()
                val text =
                    if (final) fallback
                    else previews[((index % previews.size) + previews.size) % previews.size]
                drawText(
                    text,
                    topLeft =
                        Offset(
                            (size.width - text.size.width) / 2,
                            top + (size.height - text.size.height) / 2,
                        ),
                )
            }
            if (phase < SpinSteps) drawRect(shade)
        }
    }
}
