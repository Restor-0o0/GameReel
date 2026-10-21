package com.restor.gamereel.data.storage

import com.restor.gamereel.data.bundled.bundledLibraryIds
import com.restor.gamereel.model.*
import com.restor.gamereel.repository.WorkspaceRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class BrowserSnapshot(
    val format: String = "gamereel-browser-state",
    val version: Int = 2,
    val workspace: Workspace? = null,
    val libraries: List<LibraryPack> = emptyList(),
)

// One localStorage entry makes workspace + library updates atomic, including quota failures.
// Bundled resources remain in memory. Old workspace/library keys are read until the first write.
class BrowserWorkspaceRepository(
    private val read: () -> String?,
    private val write: (String) -> Unit,
    private val readLibraries: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER") writeLibraries: (String) -> Unit = {},
    private val transientIds: Set<String> = bundledLibraryIds,
) : WorkspaceRepository {
    private val transient = mutableMapOf<String, LibraryPack>()

    private fun snapshot(): BrowserSnapshot {
        val raw = read()
        if (raw != null) {
            val element = workspaceJson.parseToJsonElement(raw)
            if (element.jsonObject["format"]?.jsonPrimitive?.content == "gamereel-browser-state") {
                return workspaceJson.decodeFromString<BrowserSnapshot>(raw).also {
                    require(it.version == 2)
                }
            }
        }
        return BrowserSnapshot(
            workspace = raw?.let { workspaceJson.decodeFromString<Workspace>(it) },
            libraries = readLibraries()?.let {
                workspaceJson.decodeFromString<List<LibraryPack>>(it)
            }.orEmpty().filterNot { it.library.id in transientIds },
        )
    }

    override fun load(): Workspace? = snapshot().workspace

    override fun save(workspace: Workspace) {
        write(workspaceJson.encodeToString(snapshot().copy(workspace = workspace)))
    }

    override fun loadLibraries(): List<LibraryPack> = transient.values.toList() + snapshot().libraries

    override fun importBundledLibrary(pack: LibraryPack) {
        require(pack.library.id in transientIds)
        transient[pack.library.id] = pack
    }

    override fun importLibrary(pack: LibraryPack) {
        val before = snapshot()
        write(workspaceJson.encodeToString(before.copy(libraries = merge(before.libraries, listOf(pack)))))
    }

    override fun saveWithLibraries(workspace: Workspace, replacements: List<LibraryPack>) {
        val before = snapshot()
        val after = before.copy(workspace = workspace, libraries = merge(before.libraries, replacements))
        write(workspaceJson.encodeToString(after))
    }

    private fun merge(current: List<LibraryPack>, replacements: List<LibraryPack>): List<LibraryPack> {
        require(replacements.none { it.library.id in transientIds || it.library.id == LibraryIds.FIXTURE })
        val ids = replacements.map { it.library.id }.toSet()
        return current.filterNot { it.library.id in ids } + replacements
    }
}
