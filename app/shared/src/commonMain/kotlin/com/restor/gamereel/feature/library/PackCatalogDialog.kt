package com.restor.gamereel.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.LibraryPack
import com.restor.gamereel.platform.httpClient
import com.restor.gamereel.text.fail
import com.restor.gamereel.ui.components.*
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
                ScreenHeader(stringResource(Res.string.packs_catalog_title)) {
                    LinkAction(stringResource(Res.string.close), onDismiss)
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(Space.l),
                    verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    item {
                        Text(stringResource(Res.string.packs_catalog_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val list = packs
                    when {
                        failed && list == null -> item {
                            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                                Text(stringResource(Res.string.packs_catalog_error),
                                    color = MaterialTheme.colorScheme.error)
                                SecondaryAction(stringResource(Res.string.packs_catalog_retry), { attempt++ })
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
                        LinkAction(stringResource(Res.string.packs_catalog_github),
                            { runCatching { uriHandler.openUri(PACKS_PAGE) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(pack: CatalogPack, installedVersion: Int?, importing: Boolean, onInstall: () -> Unit) {
    StudioCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = Space.l, vertical = Space.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = Space.m), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(pack.name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(Res.plurals.tags_count, pack.tags, pack.tags),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val upToDate = installedVersion != null && installedVersion >= pack.version
            if (upToDate) Text("✓ " + stringResource(Res.string.packs_installed),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            else PrimaryAction(
                stringResource(if (installedVersion != null) Res.string.packs_update else Res.string.packs_install),
                onInstall, enabled = !importing)
        }
    }
}
