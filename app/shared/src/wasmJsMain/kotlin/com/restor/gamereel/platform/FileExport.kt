package com.restor.gamereel.platform

import androidx.compose.runtime.*
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

@Composable
internal actual fun rememberExportLauncher(
    dialogSettings: FileKitDialogSettings,
    onResult: (ExportTarget?) -> Unit,
): ExportLauncher {
    val latestResult by rememberUpdatedState(onResult)
    return remember {
        object : ExportLauncher {
            override fun launch(suggestedName: String, defaultExtension: String) {
                latestResult(ExportTarget { bytes ->
                    FileKit.download(bytes, "$suggestedName.$defaultExtension")
                })
            }
        }
    }
}
