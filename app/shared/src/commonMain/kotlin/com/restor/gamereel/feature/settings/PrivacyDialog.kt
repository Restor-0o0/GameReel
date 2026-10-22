package com.restor.gamereel.feature.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.restor.gamereel.share.IdeaShareCode
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PrivacyDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.privacy_title)) },
        text = {
            Text(
                stringResource(Res.string.privacy_body),
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
        },
        dismissButton = {
            TextButton(onClick = { runCatching { uriHandler.openUri(IdeaShareCode.WEB_URL + "privacy/") } }) {
                Text(stringResource(Res.string.privacy_full))
            }
        },
    )
}
