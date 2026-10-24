package com.restor.gamereel.feature.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.platform.httpClient
import com.restor.gamereel.text.fail
import com.restor.gamereel.ui.presentation.isRemoteImage
import com.restor.gamereel.ui.presentation.rememberBitmap
import gamereel.app.shared.generated.resources.*
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val COMMUNITY_URL = "https://raw.githubusercontent.com/Restor-0o0/GameReel/main/community/"
private const val PACKS_PAGE = "https://github.com/Restor-0o0/GameReel/tree/main/community/packs"

@Serializable
private class CatalogIndex(val packs: List<CatalogPack> = emptyList())

@Serializable
private class CatalogPack(
    val file: String,
    val name: String,
    val id: String,
    val version: Int = 1,
    val tags: Int = 0,
    val cover: String? = null,
)

private val catalogJson = Json { ignoreUnknownKeys = true }

private suspend fun download(path: String): String {
    val response = httpClient.get(COMMUNITY_URL + path)
    if (!response.status.isSuccess()) fail(Res.string.packs_catalog_error)
    val text = response.bodyAsText()
    if (text.length > JsonLibraryImporter.MAX_CHARS)
        fail(Res.string.import_too_large, JsonLibraryImporter.MAX_MEGABYTES)
    return text
}

@Composable
internal fun PackCatalogDialog(
    libraries: List<LibraryPack>,
    importing: Boolean,
    onInstall: (suspend () -> String) -> Unit,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var attempt by remember { mutableStateOf(0) }
    var packs by remember { mutableStateOf<List<CatalogPack>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        failed = false
        try {
            packs = catalogJson.decodeFromString<CatalogIndex>(download("index.json")).packs
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        }
    }
    val installed = remember(libraries) { libraries.associate { it.library.id to it.library.version } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(12.dp).widthIn(max = 640.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.packs_catalog_title), Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(stringResource(Res.string.packs_catalog_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val list = packs
                    when {
                        failed && list == null -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(Res.string.packs_catalog_error),
                                    color = MaterialTheme.colorScheme.error)
                                OutlinedButton(onClick = { attempt++ }) {
                                    Text(stringResource(Res.string.packs_catalog_retry))
                                }
                            }
                        }
                        list == null -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> items(list, key = { it.id }) { pack ->
                            val current = installed[pack.id]
                            PackRow(pack, current, importing) {
                                onInstall { download(pack.file) }
                                onDismiss()
                            }
                        }
                    }
                    item {
                        Text(stringResource(Res.string.packs_catalog_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        TextButton(onClick = { runCatching { uriHandler.openUri(PACKS_PAGE) } }) {
                            Text(stringResource(Res.string.packs_catalog_github))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(pack: CatalogPack, installedVersion: Int?, importing: Boolean, onInstall: () -> Unit) {
    val cover = rememberBitmap(pack.cover?.takeIf(::isRemoteImage))
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 72.dp, height = 48.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (cover != null) Image(cover, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                else Text("🎮")
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(pack.name, style = MaterialTheme.typography.titleMedium)
                Text(pluralStringResource(Res.plurals.tags_count, pack.tags, pack.tags),
                    style = MaterialTheme.typography.bodySmall)
            }
            val upToDate = installedVersion != null && installedVersion >= pack.version
            if (upToDate) Text(stringResource(Res.string.packs_installed),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            else Button(onClick = onInstall, enabled = !importing) {
                Text(stringResource(if (installedVersion != null) Res.string.packs_update else Res.string.packs_install))
            }
        }
    }
}
