package com.restor.gamereel.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.write

@Composable
internal actual fun rememberExportLauncher(
    dialogSettings: FileKitDialogSettings,
    onResult: (ExportTarget?) -> Unit,
): ExportLauncher {
    val saver = rememberFileSaverLauncher(dialogSettings) { file ->
        onResult(file?.let { ExportTarget { bytes -> it.write(bytes) } })
    }
    return remember(saver) {
        object : ExportLauncher {
            override fun launch(suggestedName: String, defaultExtension: String) {
                saver.launch(suggestedName = suggestedName, defaultExtension = defaultExtension)
            }
        }
    }
}
