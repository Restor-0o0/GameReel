package com.restor.gamereel.data.storage

import com.restor.gamereel.data.bundled.bundledLibraryIds
import com.restor.gamereel.model.*
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceBackup(
    val format: String = "gamereel-backup",
    val version: Int = 1,
    val workspace: Workspace,
    val customLibraries: List<LibraryPack> = emptyList(),
) {
    fun validate() {
        require(format == "gamereel-backup" && version == 1)
        require(workspace.nextId in 1..(Long.MAX_VALUE - ContentLimits.MAX_REELS))
        require(workspace.reels.size <= ContentLimits.MAX_REELS && workspace.ideas.size <= 10_000)
        require(workspace.ideas.map { it.id }.distinct().size == workspace.ideas.size)
        val preferences = workspace.preferences
        require(preferences.presets.map { it.id }.distinct().size == preferences.presets.size)
        require(preferences.customIdeaFilters.map { it.id }.distinct().size == preferences.customIdeaFilters.size)
        val reelLists = listOf(workspace.reels) + workspace.history +
            workspace.ideas.map { it.reels } + preferences.presets.map { it.reels }
        require(reelLists.all { reels -> reels.size <= ContentLimits.MAX_REELS &&
            reels.map { it.id }.distinct().size == reels.size })
        require(preferences.presets.all { preset ->
            preset.games.size <= ContentLimits.MAX_GAMES && preset.tags.size <= ContentLimits.MAX_TAGS &&
                preset.games.all { it.tags.size <= ContentLimits.MAX_TAGS }
        })
        // IDs in history and saved snapshots may become active again after undo/load.
        val managedIds = reelLists.flatten().map { it.id } + workspace.ideas.map { it.id } +
            workspace.ideas.mapNotNull { it.variantOfId } + preferences.presets.map { it.id } +
            preferences.customIdeaFilters.map { it.id } + preferences.activePresetHistory.filterNotNull() +
            preferences.activeVariantHistory.filterNotNull() +
            listOfNotNull(preferences.activePresetId, preferences.activeVariantParentId,
                preferences.activeCustomIdeaFilterId)
        val prefixes = setOf("reel", "idea", "preset", "filter")
        managedIds.forEach { id ->
            if (id.substringBefore(':') in prefixes && ':' in id) {
                val number = id.substringAfter(':').toLongOrNull()
                require(number != null && number > 0 && number < workspace.nextId)
            }
        }
        require(customLibraries.size <= 50)
        require(customLibraries.map { it.library.id }.distinct().size == customLibraries.size)
        val gameIds = customLibraries.flatMap { pack -> pack.games.map { it.id } }
        require(gameIds.size == gameIds.distinct().size)
        require(customLibraries.all { pack ->
            pack.library.id.isNotBlank() && pack.library.id !in bundledLibraryIds &&
                pack.library.id != LibraryIds.FIXTURE &&
                pack.games.size <= ContentLimits.MAX_GAMES && pack.library.standaloneTags.size <= ContentLimits.MAX_TAGS &&
                pack.games.all { it.id.isNotBlank() && it.title.isNotBlank() &&
                    it.sourceLibraryId == pack.library.id && it.tags.size <= ContentLimits.MAX_TAGS }
        })
    }
}

fun makeWorkspaceBackup(workspace: Workspace, libraries: List<LibraryPack>) =
    WorkspaceBackup(workspace = workspace,
        customLibraries = libraries.filterNot { it.library.id in bundledLibraryIds })
