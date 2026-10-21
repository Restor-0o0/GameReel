package com.restor.gamereel.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TagResult(tag: Tag, gameTitle: String?, pinned: Boolean, onPin: (Tag) -> Unit) {
    StudioCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TagArt(
                tag,
                tag.category.symbol,
                20.sp,
                Modifier.padding(end = 10.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(tag.category.title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                val label = tag.label()
                Text(
                    label,
                    style =
                        if (label.length > 42) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                tag.fullName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (gameTitle != null)
                    Text(
                        gameTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                else
                    referenceLabel(tag.referenceGames)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
            }

            TextButton(onClick = { onPin(tag) }, enabled = !pinned) {
                Text(stringResource(if (pinned) Res.string.pinned_tag else Res.string.pin_tag))
            }
        }
    }
}
