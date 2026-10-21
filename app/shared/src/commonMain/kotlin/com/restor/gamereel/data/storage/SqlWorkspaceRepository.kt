package com.restor.gamereel.data.storage

import app.cash.sqldelight.db.SqlDriver
import com.restor.gamereel.database.GameReelDatabase
import com.restor.gamereel.model.*
import com.restor.gamereel.repository.WorkspaceRepository
import kotlinx.serialization.encodeToString

class SqlWorkspaceRepository(private val driver: SqlDriver) : WorkspaceRepository {
    private val database = GameReelDatabase(driver)
    private var savedIdeas: List<SavedIdea>? = null

    override fun load(): Workspace? {
        val generator =
            database.userDataQueries.selectGenerator().executeAsOneOrNull() ?: return null
        val ideas = database.userDataQueries.selectIdeas().executeAsList().map {
            workspaceJson.decodeFromString<SavedIdea>(it)
        }
        savedIdeas = ideas
        return Workspace(
            workspaceJson.decodeFromString(generator.reels_json),
            ideas,
            generator.next_id,
            workspaceJson.decodeFromString(generator.preferences_json),
            workspaceJson.decodeFromString(generator.history_json),
        )
    }

    override fun save(workspace: Workspace) {
        val ideasChanged = savedIdeas !== workspace.ideas && savedIdeas != workspace.ideas
        database.transaction {
            afterCommit { savedIdeas = workspace.ideas }
            database.userDataQueries.writeGenerator(
                workspaceJson.encodeToString(workspace.reels),
                workspace.nextId,
                workspaceJson.encodeToString(workspace.preferences),
                workspaceJson.encodeToString(workspace.history),
            )
            if (ideasChanged) {
                database.userDataQueries.clearIdeas()
                workspace.ideas.forEachIndexed { index, idea ->
                    database.userDataQueries.writeIdea(
                        idea.id,
                        index.toLong(),
                        workspaceJson.encodeToString(idea),
                    )
                }
            }
        }
    }

    override fun saveWithLibraries(workspace: Workspace, replacements: List<LibraryPack>) {
        database.transaction {
            replacements.forEach(::importLibrary)
            save(workspace)
        }
    }

    override fun close() = driver.close()

    override fun loadLibraries(): List<LibraryPack> =
        database.libraryDataQueries.selectLibraries().executeAsList().map { library ->
            LibraryPack(
                workspaceJson.decodeFromString(library.metadata_json),
                database.libraryDataQueries.selectGames(library.id).executeAsList().map {
                    workspaceJson.decodeFromString<GameReference>(it)
                },
            )
        }

    override fun importLibrary(pack: LibraryPack) = database.transaction {
        database.libraryDataQueries.writeLibrary(
            pack.library.id,
            workspaceJson.encodeToString(pack.library),
        )
        database.libraryDataQueries.deleteLibraryGames(pack.library.id)
        pack.games.forEach { game ->
            database.libraryDataQueries.writeGame(
                game.id,
                pack.library.id,
                game.title,
                workspaceJson.encodeToString(game),
            )
        }
    }
}
