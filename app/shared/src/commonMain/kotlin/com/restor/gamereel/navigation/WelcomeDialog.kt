package com.restor.gamereel.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import gamereel.app.shared.generated.resources.Res
import gamereel.app.shared.generated.resources.welcome_done
import gamereel.app.shared.generated.resources.welcome_next
import gamereel.app.shared.generated.resources.welcome_skip
import gamereel.app.shared.generated.resources.welcome_step_1
import gamereel.app.shared.generated.resources.welcome_step_2
import gamereel.app.shared.generated.resources.welcome_step_3
import gamereel.app.shared.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WelcomeDialog(onClose: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val pages = listOf(Res.string.welcome_step_1, Res.string.welcome_step_2,
        Res.string.welcome_step_3)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.welcome_title)) },
        text = { Text(stringResource(pages[step])) },
        confirmButton = {
            TextButton(onClick = { if (step == pages.lastIndex) onClose() else step++ }) {
                Text(stringResource(if (step == pages.lastIndex)
                    Res.string.welcome_done else Res.string.welcome_next))
            }
        },
        dismissButton = { TextButton(onClick = onClose) {
            Text(stringResource(Res.string.welcome_skip))
        } },
    )
}
