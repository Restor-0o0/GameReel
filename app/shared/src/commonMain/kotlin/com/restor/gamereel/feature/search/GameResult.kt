package com.restor.gamereel.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameResult(game: GameReference, onClick: () -> Unit) {
    StudioCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TagArt(
                game.tags.firstOrNull(),
                "🎮",
                24.sp,
                Modifier.padding(end = 12.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    if (game.tags.isEmpty()) stringResource(Res.string.game_no_tags)
                    else
                        pluralStringResource(
                            Res.plurals.tags_count,
                            game.tags.size,
                            game.tags.size,
                        ) + " · " + stringResource(Res.string.open_reference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("›", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
