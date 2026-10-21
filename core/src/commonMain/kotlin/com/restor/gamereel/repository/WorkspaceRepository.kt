package com.restor.gamereel.repository

import com.restor.gamereel.model.*

interface WorkspaceRepository {
    fun load(): Workspace?

    fun save(workspace: Workspace)

    fun loadLibraries(): List<LibraryPack> = emptyList()

    fun importLibrary(pack: LibraryPack) {
        error("import is not supported by this storage")
    }

    fun importBundledLibrary(pack: LibraryPack) = importLibrary(pack)

    /** Commit the workspace and library replacements together, or leave both unchanged. */
    fun saveWithLibraries(workspace: Workspace, replacements: List<LibraryPack>) {
        error("atomic library updates are not supported by this storage")
    }

    fun close() {}
}
