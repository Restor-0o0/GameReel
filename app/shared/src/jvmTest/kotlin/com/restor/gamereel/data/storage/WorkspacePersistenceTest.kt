package com.restor.gamereel.data.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.storage.*
import com.restor.gamereel.database.GameReelDatabase
import com.restor.gamereel.model.*
import java.nio.file.Files
import java.util.Properties
import kotlin.test.*
import kotlinx.serialization.encodeToString

class WorkspacePersistenceTest {
    @Test
    fun failedRestoreRollsBackLibrariesWorkspaceAndIdeaCache() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameReelDatabase.Schema.create(driver)
        val repository = SqlWorkspaceRepository(driver)
        try {
            val before = BuiltinCatalog.initialWorkspace().copy(
                ideas = listOf(SavedIdea("old", "Original", emptyList())))
            val original = LibraryPack(Library("mine", "Original"), emptyList())
            repository.saveWithLibraries(before, listOf(original))
            val duplicate = SavedIdea("same-id", "Duplicate", emptyList())
            val replacement = original.copy(library = original.library.copy(name = "Replacement"))
            assertFails {
                repository.saveWithLibraries(before.copy(ideas = listOf(duplicate, duplicate)), listOf(replacement))
            }
            assertEquals(listOf(original), repository.loadLibraries())
            // Save without load(): a stale cache must not skip writing these ideas.
            repository.save(before)
            assertEquals(before, repository.load())
            val after = before.copy(ideas = listOf(duplicate))
            repository.saveWithLibraries(after, listOf(replacement))
            assertEquals(after, repository.load())
            assertEquals(listOf(replacement), repository.loadLibraries())
        } finally { repository.close() }
    }

    @Test
    fun standaloneLibraryTagsSurviveReopeningWithoutCreatingFakeGames() {
        val path = Files.createTempFile("gamereel-curated-", ".db")
        fun open() =
            SqlWorkspaceRepository(
                JdbcSqliteDriver("jdbc:sqlite:$path", Properties(), GameReelDatabase.Schema)
            )
        val tag = Tag("curated:location", "harbor", Category.LOCATION, "curated_v5")
        val pack =
            LibraryPack(
                Library("curated_v5", "Curated", 5, standaloneTags = listOf(tag)),
                emptyList(),
            )
        try {
            val repository = open()
            try {
                repository.importLibrary(pack)
            } finally {
                repository.close()
            }
            val reopened = open()
            try {
                assertEquals(pack, reopened.loadLibraries().single())
                assertNull(reopened.loadLibraries().single().allTags.single().sourceGameId)
            } finally {
                reopened.close()
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun migrationFromInstalledV1KeepsIdeasPinsAndAddsPreferences() {
        val path = Files.createTempFile("gamereel-v1-", ".db")
        val originalReels =
            listOf(Reel("p", Category.CORE, BuiltinCatalog.tags.first(), ReelState.PINNED))
        val originalIdea =
            SavedIdea("idea:2", "Моя старая идея", originalReels, "Сохранить заметки")
        val v1 = JdbcSqliteDriver("jdbc:sqlite:$path")
        v1.execute(
            null,
            "CREATE TABLE generator_state (id INTEGER NOT NULL PRIMARY KEY, reels_json TEXT NOT NULL, next_id INTEGER NOT NULL)",
            0,
        )
        v1.execute(
            null,
            "CREATE TABLE saved_ideas (id TEXT NOT NULL PRIMARY KEY, position INTEGER NOT NULL, snapshot_json TEXT NOT NULL)",
            0,
        )
        v1.execute(null, "INSERT INTO generator_state VALUES (1, ?, 3)", 1) {
            bindString(0, workspaceJson.encodeToString(originalReels))
        }
        v1.execute(null, "INSERT INTO saved_ideas VALUES ('idea:2', 0, ?)", 1) {
            bindString(0, workspaceJson.encodeToString(originalIdea))
        }
        v1.execute(null, "PRAGMA user_version = 1", 0)
        v1.close()
        fun open() =
            SqlWorkspaceRepository(
                JdbcSqliteDriver("jdbc:sqlite:$path", Properties(), GameReelDatabase.Schema)
            )
        try {
            val migrated = open()
            val restored = migrated.load()!!
            assertEquals(originalReels, restored.reels)
            assertEquals(originalIdea, restored.ideas.single())
            assertEquals(Preferences(), restored.preferences)
            val updated =
                restored.copy(
                    preferences =
                        Preferences(
                            animationSpeed = AnimationSpeed.OFF,
                            soundEnabled = false,
                            presets =
                                listOf(
                                    GeneratorPreset(
                                        "preset:4",
                                        "My setup",
                                        originalReels,
                                        setOf("fixture"),
                                    )
                                ),
                            enabledLibraryIds = setOf("fixture"),
                            spinButtonPosition = SpinButtonPosition(0.2f, 0.7f),
                        ),
                    history = listOf(originalReels),
                )
            migrated.save(updated)
            migrated.close()
            val reopened = open()
            try {
                assertEquals(updated, reopened.load())
            } finally {
                reopened.close()
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun libraryUpdateCannotDeleteUserSnapshots() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameReelDatabase.Schema.create(driver)
        val repository = SqlWorkspaceRepository(driver)
        try {
            val tag = Tag("tag", "Mechanic", Category.CORE, "pack", "game")
            val reels = listOf(Reel("p", Category.CORE, tag, ReelState.PINNED))
            val workspace =
                Workspace(
                    reels,
                    listOf(SavedIdea("idea", "Keep", reels, "Notes")),
                    7,
                    Preferences(animationSpeed = AnimationSpeed.OFF),
                )
            repository.save(workspace)
            val pack =
                LibraryPack(
                    Library("pack", "Test"),
                    listOf(GameReference("game", "external", "pack", "Old", listOf(tag))),
                )
            repository.importLibrary(pack)
            val updated =
                pack.copy(
                    library = pack.library.copy(version = 2),
                    games = pack.games.map { it.copy(title = "New", tags = emptyList()) },
                )
            repository.importLibrary(updated)
            assertEquals(updated, repository.loadLibraries().single())
            assertEquals(workspace, repository.load())
        } finally {
            repository.close()
        }
    }

    @Test
    fun closingAndReopeningDatabaseRestoresSnapshotAndCounter() {
        val path = Files.createTempFile("gamereel-test-", ".db")
        fun open() =
            SqlWorkspaceRepository(
                JdbcSqliteDriver("jdbc:sqlite:$path", Properties(), GameReelDatabase.Schema)
            )
        try {
            val tag = BuiltinCatalog.tags.first()
            val reels = listOf(Reel("pin:1", tag.category, tag, ReelState.PINNED))
            val expected =
                Workspace(
                    reels,
                    listOf(SavedIdea("idea:2", "Завод 🎮", reels, "Заметки\nВторая строка")),
                    nextId = 3,
                )
            val initial = open()
            assertNull(initial.load())
            initial.save(expected)
            initial.close()
            val reopened = open()
            try {
                assertEquals(expected, reopened.load())
                // editing reels must not touch the saved idea
                reopened.save(expected.copy(reels = emptyList()))
                assertEquals(reels, reopened.load()!!.ideas.single().reels)
            } finally {
                reopened.close()
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun failedTransactionPreservesPreviousWorkspace() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GameReelDatabase.Schema.create(driver)
        val repository = SqlWorkspaceRepository(driver)
        try {
            val before = BuiltinCatalog.initialWorkspace()
            repository.save(before)
            val duplicate = SavedIdea("same-id", "Test", before.reels)
            assertFails {
                repository.save(
                    before.copy(reels = emptyList(), ideas = listOf(duplicate, duplicate))
                )
            }
            assertEquals(before, repository.load())
        } finally {
            repository.close()
        }
    }
}
