package com.restor.gamereel.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

internal fun interface ExportTarget {
    suspend fun write(bytes: ByteArray)
}

internal interface ExportLauncher {
    fun launch(suggestedName: String, defaultExtension: String)
}

/** Native save dialog or browser download, with the same error handling at the call site. */
@Composable
internal expect fun rememberExportLauncher(
    dialogSettings: FileKitDialogSettings,
    onResult: (ExportTarget?) -> Unit,
): ExportLauncher
