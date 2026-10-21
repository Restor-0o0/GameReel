package com.restor.gamereel.presentation

import androidx.lifecycle.viewModelScope
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.personal.GameTagDraft
import com.restor.gamereel.data.personal.PersonalGames
import com.restor.gamereel.data.storage.*
import com.restor.gamereel.model.*
import com.restor.gamereel.presentation.GameReelViewModel
import com.restor.gamereel.repository.WorkspaceRepository
import com.restor.gamereel.text.UiText
import gamereel.app.shared.generated.resources.*
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class GameReelViewModelTest {
    @Test
    fun failedRestoreDoesNotPublishOrPersistPartialLibraries() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val original = LibraryPack(Library("mine", "Original"), emptyList())
        val repository = MemoryRepository().apply { libraries = listOf(original) }
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            val before = model.state.value.workspace!!
            val backup = WorkspaceBackup(workspace = before.copy(nextId = 42), customLibraries =
                listOf(original.copy(library = original.library.copy(name = "Replacement"))))
            repository.failWrites = true
            model.restoreBackup(backup)
            runCurrent()
            assertEquals(before, repository.stored)
            assertEquals(before, model.state.value.workspace)
            assertEquals(listOf(original), repository.libraries)
            assertEquals(listOf(original), model.state.value.libraries)
            assertNotNull(model.state.value.error)
            repository.failWrites = false
            model.restoreBackup(backup)
            runCurrent()
            assertEquals(backup.workspace, repository.stored)
            assertEquals(backup.customLibraries, model.state.value.libraries)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun userCannotImportABundledId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val bundled = LibraryPack(Library(LibraryIds.CURATED, "Bundled", standaloneTags =
            listOf(Tag("t", "Tag", Category.CORE, LibraryIds.CURATED))), emptyList())
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher, bundledLoader = { listOf(bundled) })
        try {
            runCurrent()
            model.importLibrary { workspaceJson.encodeToString(bundled) }
            runCurrent()
            assertNotNull(model.state.value.error)
            assertEquals(listOf(bundled), repository.libraries)
            assertEquals(listOf(bundled), model.state.value.libraries)
            assertFalse(model.state.value.importing)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoringBackupReplacesWorkspaceAndRestoresPersonalGames() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            val restored = BuiltinCatalog.initialWorkspace().copy(
                ideas = listOf(SavedIdea("idea:7", "Imported idea", emptyList(),
                    project = "Jam")), nextId = 8,
            )
            val tag = Tag("mine:tag", "Survival", Category.CORE, LibraryIds.PERSONAL)
            val personal = LibraryPack(Library(LibraryIds.PERSONAL, "My games"),
                listOf(GameReference("mine:game", "game", LibraryIds.PERSONAL,
                    "My game", listOf(tag))))
            model.restoreBackup(WorkspaceBackup(workspace = restored,
                customLibraries = listOf(personal)))
            runCurrent()
            assertEquals(restored, repository.stored)
            assertEquals("Imported idea", model.state.value.workspace!!.ideas.single().title)
            assertEquals(personal, repository.libraries.single())
            assertNull(model.state.value.error)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun personalGamesPersistMergeFromJsonAndFeedGenerator() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            model.savePersonalGame(null, "My survival game",
                listOf(GameTagDraft("My survival", Category.CORE)))
            runCurrent()
            val local = repository.libraries.single { it.library.id == LibraryIds.PERSONAL }
            assertEquals(1, local.games.size)
            assertEquals(Category.CORE, local.games.single().tags.single().category)

            val incoming = PersonalGames.save(null, null, "From tablet",
                listOf(GameTagDraft("Co-op", Category.MULTIPLAYER)))
            model.importPersonalGames { Json.encodeToString(incoming) }
            runCurrent()
            assertEquals(2, repository.libraries.single { it.library.id == LibraryIds.PERSONAL }.games.size)
            assertTrue(repository.stored!!.preferences.includesLibrary(LibraryIds.PERSONAL))

            model.toggleLibrary(LibraryIds.FIXTURE)
            model.spin()
            runCurrent()
            assertEquals("My survival", model.state.value.workspace!!.reels
                .first { it.category == Category.CORE }.tag?.name)

            model.deletePersonalGame(local.games.single().id)
            runCurrent()
            assertEquals(listOf("From tablet"), repository.libraries
                .single { it.library.id == LibraryIds.PERSONAL }.games.map { it.title })
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun bundledUpgradePreservesWorkspaceAndResetIsPersistedAndUndoable() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val oldTag = Tag("old-tag", "Original", Category.CORE, "steam_broad", "old-game")
        val oldReel = Reel("pinned", Category.CORE, oldTag, ReelState.PINNED)
        val original =
            Workspace(
                listOf(oldReel),
                listOf(SavedIdea("saved", "Keep", listOf(oldReel))),
                nextId = 12,
            )
        val oldPack = LibraryPack(Library("steam_broad", "Steam", 1), emptyList())
        val upgraded = oldPack.copy(library = oldPack.library.copy(version = 4))
        val repository =
            MemoryRepository().apply {
                stored = original
                libraries = listOf(oldPack)
            }
        val model =
            GameReelViewModel(
                { repository },
                dispatcher,
                bundledLoader = { versions ->
                    assertEquals(mapOf("steam_broad" to 1), versions)
                    listOf(upgraded)
                },
            )
        try {
            runCurrent()
            assertEquals(listOf(upgraded), model.state.value.libraries)
            assertEquals(original, repository.stored)
            model.resetReels()
            runCurrent()
            assertEquals(8, repository.stored!!.reels.size)
            assertEquals(original.ideas, repository.stored!!.ideas)
            model.undo()
            runCurrent()
            assertEquals(original, repository.stored)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun enableAllCombinesBuiltinAndCustomPoolsWithoutChangingIdeasOrLocks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val mobileTag =
            Tag(
                "mobile:1",
                "Mobile progression",
                Category.PROGRESSION,
                "google_play_alltime_like",
                "mobile-game",
            )
        val customTag =
            Tag("custom:1", "Custom interaction", Category.INTERACTION, "my_pack", "custom-game")
        val libraryTags = listOf(mobileTag, customTag)
        val locked =
            listOf(
                Reel("pinned", Category.CORE, BuiltinCatalog.tags.first(), ReelState.PINNED),
                Reel("frozen", Category.CORE, BuiltinCatalog.tags[1], ReelState.FROZEN),
            )
        val original =
            Workspace(
                locked +
                    listOf(
                        Reel("mobile", Category.PROGRESSION),
                        Reel("custom", Category.INTERACTION),
                    ),
                ideas = listOf(SavedIdea("idea", "Keep", locked)),
                preferences =
                    Preferences(disabledLibraryIds = setOf("google_play_alltime_like", "my_pack")),
            )
        val repository =
            MemoryRepository().apply {
                stored = original
                libraries = libraryTags.map { tag ->
                    LibraryPack(
                        Library(tag.sourceLibraryId, tag.sourceLibraryId),
                        listOf(
                            GameReference(
                                tag.sourceGameId!!,
                                "external",
                                tag.sourceLibraryId,
                                "Game",
                                listOf(tag),
                            )
                        ),
                    )
                }
            }
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            model.enableAllLibraries()
            runCurrent()
            assertEquals(
                original.copy(
                    preferences = original.preferences.copy(disabledLibraryIds = emptySet())
                ),
                repository.stored,
            )
            model.spin()
            runCurrent()
            val result = model.state.value.workspace!!
            assertEquals(locked, result.reels.take(2))
            assertEquals(mobileTag, result.reels[2].tag)
            assertEquals(customTag, result.reels[3].tag)
            assertEquals(original.ideas, result.ideas)
            assertTrue(repository.stored!!.preferences.disabledLibraryIds.isEmpty())
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun animationPreferencesUndoAndSpinSignalsAreIndependentOfPersistence() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            val initial = model.state.value.workspace!!.reels
            model.setAnimation(AnimationSpeed.OFF)
            model.setGeneratorView(GeneratorView.MACHINE)
            model.freeze(initial.first().id)
            runCurrent()
            val before = model.state.value.workspace!!.reels
            model.spin()
            model.spin()
            runCurrent()
            assertEquals(2L, model.state.value.spinRevision)
            assertFalse(initial.first().id in model.state.value.animatedReelIds)
            assertEquals(2, model.state.value.workspace!!.history.size)
            assertEquals(AnimationSpeed.OFF, repository.stored!!.preferences.animationSpeed)
            model.undo()
            model.undo()
            runCurrent()
            assertEquals(before, model.state.value.workspace!!.reels)
            assertTrue(model.state.value.animatedReelIds.isEmpty())
            assertTrue(model.state.value.workspace!!.history.isEmpty())
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun cannotDisableTheLastActiveLibrary() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val model = GameReelViewModel({ MemoryRepository() }, dispatcher)
        try {
            runCurrent()
            model.toggleLibrary("fixture")
            runCurrent()
            assertTrue(model.state.value.workspace!!.preferences.disabledLibraryIds.isEmpty())
            assertNotNull(model.state.value.error)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private class MemoryRepository : WorkspaceRepository {
        var libraries: List<LibraryPack> = emptyList()

        override fun loadLibraries() = libraries

        override fun importLibrary(pack: LibraryPack) {
            libraries = libraries.filterNot { it.library.id == pack.library.id } + pack
        }

        var stored: Workspace? = null
        var failWrites = false
        var failReads = false
        var closed = false

        override fun load(): Workspace? {
            check(!failReads) { "Read failed" }
            return stored
        }

        override fun save(workspace: Workspace) {
            check(!failWrites) { "Disk full" }
            stored = workspace
        }

        override fun saveWithLibraries(workspace: Workspace, replacements: List<LibraryPack>) {
            check(!failWrites) { "Disk full" }
            val ids = replacements.map { it.library.id }.toSet()
            libraries = libraries.filterNot { it.library.id in ids } + replacements
            stored = workspace
        }

        override fun close() {
            closed = true
        }
    }

    @Test
    fun freezePinSaveAndReloadFlow() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            val initial = model.state.value.workspace!!
            model.freeze(initial.reels[0].id)
            model.pin(initial.reels[1].id)
            model.spin()
            runCurrent()
            val spun = model.state.value.workspace!!
            assertEquals(initial.reels[0].tag, spun.reels[0].tag)
            assertEquals(initial.reels[1].tag, spun.reels[1].tag)
            model.unlockFrozen()
            model.saveIdea("My idea", "Notes")
            runCurrent()
            val saved = model.state.value.workspace!!.ideas.single()
            assertEquals(ReelState.PINNED, saved.reels[1].state)
            assertEquals(ReelState.RANDOM, saved.reels[0].state)
            model.spin()
            model.loadIdea(saved.id)
            runCurrent()
            assertEquals(saved.reels, model.state.value.workspace!!.reels)
            assertEquals(model.state.value.workspace, repository.stored)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
        assertTrue(repository.closed)
    }

    @Test
    fun failedSaveDoesNotPublishUnsavedState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository()
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            val before = model.state.value.workspace
            repository.failWrites = true
            val failed = model.saveIdea("Lost?", "Draft notes")
            assertFalse(failed.isCompleted)
            runCurrent()
            assertNotNull(failed.await())
            assertEquals(before, model.state.value.workspace)
            assertEquals(before, repository.stored)
            assertNotNull(model.state.value.error)
            repository.failWrites = false
            val saved = model.saveIdea("Persisted", "Draft notes")
            runCurrent()
            assertNull(saved.await())
            assertEquals("Persisted", model.state.value.workspace!!.ideas.single().title)
            assertNull(model.state.value.error)
            repository.failWrites = true
            val idea = model.state.value.workspace!!.ideas.single()
            val failedEdit = model.editIdea(idea.id, "Edited", "Edited notes")
            runCurrent()
            assertNotNull(failedEdit.await())
            assertEquals(idea, repository.stored!!.ideas.single())
            repository.failWrites = false
            val edited = model.editIdea(idea.id, "Edited", "Edited notes")
            runCurrent()
            assertNull(edited.await())
            assertEquals("Edited notes", repository.stored!!.ideas.single().notes)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun duplicatePinIsReportedWithoutDuplicatingReel() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val model = GameReelViewModel({ MemoryRepository() }, dispatcher)
        try {
            runCurrent()
            model.pinTag(BuiltinCatalog.tags.first())
            model.pinTag(BuiltinCatalog.tags.first())
            runCurrent()
            assertEquals(
                1,
                model.state.value.workspace!!.reels.count { it.state == ReelState.PINNED },
            )
            assertEquals(
                UiText.Str(
                    Res.string.error_not_saved,
                    listOf(UiText.Str(Res.string.error_already_pinned)),
                ),
                model.state.value.error,
            )
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedReadNeverReplacesExistingDataWithFixture() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = MemoryRepository().apply { failReads = true }
        val model = GameReelViewModel({ repository }, dispatcher)
        try {
            runCurrent()
            model.spin()
            runCurrent()
            assertNull(repository.stored)
            assertNull(model.state.value.workspace)
            assertNotNull(model.state.value.error)
        } finally {
            model.viewModelScope.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
