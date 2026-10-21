package com.restor.gamereel.data.storage

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import com.restor.gamereel.generator.*
import com.restor.gamereel.data.importer.JsonLibraryImporter
import kotlin.test.*
import kotlinx.serialization.encodeToString

class WorkspaceBackupTest {
    @Test
    fun invalidCounterCannotReuseIdsFromAnyRestorableContent() {
        val initial = BuiltinCatalog.initialWorkspace()
        val reel = Reel("reel:9", Category.CORE)
        val cases = listOf(
            initial.copy(ideas = listOf(SavedIdea("idea:9", "Keep", emptyList()))),
            initial.copy(reels = listOf(reel)),
            initial.copy(history = listOf(listOf(reel))),
            initial.copy(ideas = listOf(SavedIdea("saved", "Keep", listOf(reel)))),
            initial.copy(preferences = Preferences(presets = listOf(
                GeneratorPreset("preset:9", "Preset", reels = listOf(Reel("template:0", Category.CORE)))))),
            initial.copy(preferences = Preferences(customIdeaFilters = listOf(
                CustomIdeaFilter("filter:9", "Filter", baseMode = IdeaMode.SOLO)))),
        )
        cases.forEach { workspace ->
            assertFailsWith<IllegalArgumentException> { WorkspaceBackup(workspace = workspace.copy(nextId = 9)).validate() }
            WorkspaceBackup(workspace = workspace.copy(nextId = 10)).validate()
        }
        assertFails { WorkspaceBackup(workspace = initial.copy(nextId = Long.MAX_VALUE)).validate() }
    }

    @Test
    fun importedGameWithManyTagsRoundTripsThroughPresetAndBackup() {
        val tags = (1..101).map { Tag("t:$it", "Tag $it", Category.CORE, "custom", "game") }
        val pack = LibraryPack(Library("custom", "Custom"),
            listOf(GameReference("game", "game", "custom", "Game", tags)))
        val imported = JsonLibraryImporter().parse(workspaceJson.encodeToString(pack)).pack
        assertEquals(101, imported.games.single().tags.size)
        val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags,
            object : RandomProvider { override fun nextInt(bound: Int) = 0 }))
        val saved = editor.saveGamesPreset(BuiltinCatalog.initialWorkspace(), "Set", imported.games)
        val decoded = workspaceJson.decodeFromString<GeneratorPreset>(
            workspaceJson.encodeToString(saved.preferences.presets.single()))
        val restored = editor.importPreset(BuiltinCatalog.initialWorkspace(), decoded)
        assertEquals(101, restored.preferences.presets.single().games.single().tags.size)
        val backup = makeWorkspaceBackup(saved, listOf(imported))
        val restoredBackup = workspaceJson.decodeFromString<WorkspaceBackup>(workspaceJson.encodeToString(backup))
        restoredBackup.validate()
        assertEquals(backup, restoredBackup)
    }

    @Test
    fun oldSavedIdeaStillLoadsWithEmptyConcept() {
        val oldJson = """{"id":"idea:1","title":"Old idea","reels":[],"notes":"Keep me"}"""
        val idea = workspaceJson.decodeFromString<SavedIdea>(oldJson)
        assertEquals("Keep me", idea.notes)
        assertEquals(IdeaConcept(), idea.concept)
        assertEquals("", idea.project)
        assertNull(idea.variantOfId)
    }

    @Test
    fun backupRoundTripsIdeasAndCustomLibraries() {
        val workspace = BuiltinCatalog.initialWorkspace().copy(
            nextId = 3,
            ideas = listOf(SavedIdea("idea:1", "Example", emptyList(),
                concept = IdeaConcept(pitch = "Pitch"), project = "My game")),
            preferences = Preferences(customIdeaFilters = listOf(
                CustomIdeaFilter("filter:2", "No zombies", excludedTerms = listOf("zombie"),
                    includeCreativeTwists = true)),
                activeCustomIdeaFilterId = "filter:2"),
        )
        val custom = LibraryPack(Library("mine", "Mine"), emptyList())
        val bundled = LibraryPack(Library(LibraryIds.CURATED, "Curated"), emptyList())
        val backup = makeWorkspaceBackup(workspace, listOf(custom, bundled))
        val decoded = workspaceJson.decodeFromString<WorkspaceBackup>(workspaceJson.encodeToString(backup))
        decoded.validate()
        assertEquals(workspace, decoded.workspace)
        assertEquals(listOf(custom), decoded.customLibraries)
    }

    @Test
    fun backupRejectsBuiltInLibraryOverwrite() {
        val backup = WorkspaceBackup(workspace = BuiltinCatalog.initialWorkspace(),
            customLibraries = listOf(LibraryPack(Library(LibraryIds.CURATED, "Fake"), emptyList())))
        assertFails { backup.validate() }
    }
}
