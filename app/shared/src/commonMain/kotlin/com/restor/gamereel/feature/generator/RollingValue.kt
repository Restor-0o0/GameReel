package com.restor.gamereel.feature.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.*
import com.restor.gamereel.ui.theme.ReelPalette
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal const val SpinSteps = 12

@Composable
fun RollingValue(
    reel: Reel,
    progress: () -> Float,
    iconsOnly: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = if (iconsOnly) 78.dp else 76.dp,
    align: Alignment = Alignment.Center,
) {
    if (iconsOnly) {
        RollingArtwork(reel, progress, modifier, height)
        return
    }

    val finalTag = reel.tag
    val pool =
        remember(reel.category) { BuiltinCatalog.tags.filter { it.category == reel.category } }
    val strip =
        remember(pool, finalTag) {
            val tags = pool.ifEmpty { listOfNotNull(finalTag) }
            when {
                tags.isEmpty() -> listOf(null, null, null)
                tags.size < 3 -> List(tags.size * 3) { tags[it % tags.size] }
                else -> tags
            }
        }
    val pixels = with(LocalDensity.current) { height.toPx() }
    val latest by rememberUpdatedState(progress)
    val phase = { latest().coerceIn(0f, 1f) * SpinSteps }
    val spinning by remember { derivedStateOf { latest() < 1f } }
    val description =
        stringResource(
            Res.string.labeled_value,
            stringResource(reel.category.title),
            finalTag?.label() ?: stringResource(Res.string.no_result),
        )
    Box(
        modifier.height(height).clip(RoundedCornerShape(10.dp)).semantics {
            contentDescription = description
        },
        contentAlignment = Alignment.Center,
    ) {
        // Keep the strip's content composed. Only layer positions change while spinning;
        // replacing three changing Text nodes caused repeated text measurement and layout.
        strip.forEachIndexed { slot, tag ->
            Box(
                Modifier.fillMaxWidth().height(height).graphicsLayer {
                    val position = phase()
                    val index = rollingSlotIndex(slot, position, strip.size)
                    translationY = (index - position) * pixels
                    alpha = if (index < SpinSteps) 1f else 0f
                },
                contentAlignment = align,
            ) {
                val label = tag?.label() ?: stringResource(Res.string.no_matching_tags)
                Text(
                    label,
                    style = cardTagTitleStyle(label, MaterialTheme.typography.titleLarge),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Measure the final label before it scrolls into view.
        Box(
            Modifier.fillMaxWidth().height(height).graphicsLayer {
                translationY = (SpinSteps - phase()) * pixels
            },
            contentAlignment = align,
        ) {
            val label = finalTag?.label() ?: stringResource(Res.string.no_matching_tags)
            Text(
                label,
                style = cardTagTitleStyle(label, MaterialTheme.typography.titleLarge),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (spinning)
            Box(
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ReelPalette.slotShade,
                                Color.Transparent,
                                Color.Transparent,
                                ReelPalette.slotShade,
                            )
                        )
                    )
            )
    }
}

private fun cardTagTitleStyle(label: String, base: TextStyle): TextStyle =
    when {
        label.length <= 22 -> base.copy(fontSize = 20.sp, lineHeight = 24.sp)
        label.length <= 42 -> base.copy(fontSize = 18.sp, lineHeight = 22.sp)
        else -> base.copy(fontSize = 15.sp, lineHeight = 19.sp)
    }
