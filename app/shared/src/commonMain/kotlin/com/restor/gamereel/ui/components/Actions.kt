package com.restor.gamereel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gamereel.app.shared.generated.resources.Res
import gamereel.app.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.stringResource

internal object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
}

internal val ActionShape = RoundedCornerShape(14.dp)
private val ActionHeight = 48.dp
private val ActionPadding = PaddingValues(horizontal = 12.dp)

@Composable
private fun ActionLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
}

// one per screen or card: the thing the user came for
@Composable
internal fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = ActionHeight), enabled, shape = ActionShape, contentPadding = ActionPadding) {
        ActionLabel(text)
    }
}

@Composable
internal fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilledTonalButton(onClick, modifier.heightIn(min = ActionHeight), enabled, shape = ActionShape,
        contentPadding = ActionPadding) {
        ActionLabel(text)
    }
}

@Composable
internal fun TertiaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.heightIn(min = ActionHeight), enabled, shape = ActionShape,
        contentPadding = ActionPadding) {
        ActionLabel(text)
    }
}

@Composable
internal fun LinkAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick, modifier, enabled) { Text(text) }
}

@Composable
internal fun ScreenHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
internal fun SettingsButton(onClick: () -> Unit) {
    val description = stringResource(Res.string.nav_settings)
    IconButton(onClick, Modifier.semantics { contentDescription = description }) {
        Text("⚙", style = MaterialTheme.typography.titleLarge)
    }
}
