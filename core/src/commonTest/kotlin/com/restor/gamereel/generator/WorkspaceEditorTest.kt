package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.generator.*
import com.restor.gamereel.model.*
import kotlin.test.*

class WorkspaceEditorTest {
    private val random =
        object : RandomProvider {
            override fun nextInt(bound: Int) = 0
        }
    private val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags, random))

    @Test
    fun resetRestoresDefaultsAndCanBeUndoneWithoutLosingIdeasOrPreferences() {
        val pinned = Reel("custom:1", Category.CORE, BuiltinCatalog.tags.first(), ReelState.PINNED)
        val original =
            Workspace(
                reels = listOf(pinned),
                ideas = listOf(SavedIdea("idea:8", "Keep", listOf(pinned), "Notes")),
                nextId = 42,
                preferences = Preferences(AnimationSpeed.OFF, GeneratorView.CARDS),
            )
        val reset = editor.resetReels(original)
        assertEquals(BuiltinCatalog.defaultCategories, reset.reels.map { it.category })
        assertTrue(reset.reels.all { it.state == ReelState.RANDOM && it.tag != null })
        assertEquals(original.ideas, reset.ideas)
        assertEquals(original.preferences, reset.preferences.copy(sourceHistory = emptyList(),
            activePresetHistory = emptyList(), activeVariantHistory = emptyList()))
        assertEquals(original.nextId, reset.nextId)
        assertEquals(original, editor.undo(reset))
    }

    @Test
    fun resetRespectsDisabledLibrariesAndLimitsHistory() {
        val custom = Tag("custom:tag", "Custom", Category.CORE, "custom")
        val library =
            LibraryPack(
                Library("custom", "Custom"),
                listOf(GameReference("game", "1", "custom", "Game", listOf(custom))),
            )
        val local =
            WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags + custom, random), listOf(library))
        val original =
            Workspace(
                emptyList(),
                preferences = Preferences(disabledLibraryIds = setOf("fixture")),
                history = List(100) { emptyList() },
            )
        val reset = local.resetReels(original)
        assertEquals(100, reset.history.size)
        assertEquals(custom, reset.reels.first().tag)
        assertTrue(reset.reels.drop(1).all { it.tag == null })
    }

    @Test
    fun repeatedResetKeepsUniqueReelIds() {
        val reset = editor.resetReels(editor.resetReels(BuiltinCatalog.initialWorkspace()))
        val added = editor.addReel(reset, Category.CORE)
        assertEquals(9, added.reels.map { it.id }.toSet().size)
    }

    @Test
    fun variantKeepsParentAndProjectAfterReroll() {
        val starting = editor.spin(BuiltinCatalog.initialWorkspace())
        val parent = editor.saveIdea(starting, "Original", "",
            IdeaConcept(pitch = "A small game"), "Jam")
        val loaded = editor.beginVariant(parent, parent.ideas.single().id)
        assertEquals(parent.ideas.single().id, loaded.preferences.activeVariantParentId)
        val rerolled = editor.spin(loaded, loaded.reels.first().id)
        val saved = editor.saveIdea(rerolled, "New take", "",
            IdeaConcept(coreLoop = "Explore, gather, escape"), "")
        assertEquals(2, saved.ideas.size)
        assertEquals("Jam", saved.ideas.first().project)
        assertEquals(parent.ideas.single().id, saved.ideas.first().variantOfId)
        assertEquals("Explore, gather, escape", saved.ideas.first().concept.coreLoop)
        assertNull(saved.preferences.activeVariantParentId)
    }

    @Test
    fun soloModeExcludesOnlineTagsButKeepsOfflineTags() {
        val online = Tag("online", "Online multiplayer", Category.CORE, "mine")
        val offline = Tag("offline", "Offline crafting", Category.CORE, "mine")
        val pack = LibraryPack(Library("mine", "Mine"),
            listOf(GameReference("game", "1", "mine", "Test", listOf(online, offline))))
        val local = WorkspaceEditor(GeneratorEngine(listOf(online, offline), random), listOf(pack))
        val workspace = Workspace(listOf(Reel("r", Category.CORE)),
            preferences = Preferences(ideaMode = IdeaMode.SOLO,
                disabledLibraryIds = setOf(LibraryIds.FIXTURE)))
        assertEquals(offline, local.spin(workspace).reels.single().tag)
    }

    @Test
    fun customFilterCanBeSavedEditedAppliedAndDeleted() {
        val start = BuiltinCatalog.initialWorkspace()
        val created = editor.saveCustomIdeaFilter(start,
            CustomIdeaFilter("", "  No zombies  ", excludedTerms = listOf(" Zombie ", "zombie")))
        val filter = created.preferences.customIdeaFilters.single()
        assertEquals("No zombies", filter.name)
        assertEquals(listOf("zombie"), filter.excludedTerms)
        assertEquals(filter.id, created.preferences.activeCustomIdeaFilterId)
        assertEquals(start.nextId + 1, created.nextId)

        val updated = editor.saveCustomIdeaFilter(created,
            filter.copy(name = "No zombies or online", excludedTerms = listOf("online")))
        assertEquals(created.nextId, updated.nextId)
        assertEquals("No zombies or online", updated.preferences.customIdeaFilters.single().name)
        val builtInMode = editor.setIdeaMode(updated, IdeaMode.JAM)
        assertNull(builtInMode.preferences.activeCustomIdeaFilterId)
        val reapplied = editor.applyCustomIdeaFilter(builtInMode, filter.id)
        assertEquals(filter.id, reapplied.preferences.activeCustomIdeaFilterId)
        val deleted = editor.deleteCustomIdeaFilter(reapplied, filter.id)
        assertTrue(deleted.preferences.customIdeaFilters.isEmpty())
        assertNull(deleted.preferences.activeCustomIdeaFilterId)
    }

    @Test
    fun customFilterNarrowsExistingTagSetWithoutChangingIt() {
        val banned = Tag("set:survival", "Survival", Category.CORE, "snapshot")
        val allowed = Tag("set:puzzle", "Puzzle", Category.CORE, "snapshot")
        val set = GeneratorPreset("set", "My tags", tags = listOf(banned, allowed))
        val filter = CustomIdeaFilter("filter:1", "No survival",
            excludedTerms = listOf("survival"))
        val workspace = Workspace(listOf(Reel("r", Category.CORE)),
            preferences = Preferences(presets = listOf(set), activePresetId = set.id,
                customIdeaFilters = listOf(filter), activeCustomIdeaFilterId = filter.id))
        val result = editor.spin(workspace)
        assertEquals(allowed, result.reels.single().tag)
        assertEquals(set.id, result.preferences.activePresetId)
    }

    @Test
    fun customFilterCanLockTagSources() {
        val first = Tag("first", "First", Category.CORE, "first-pack")
        val second = Tag("second", "Second", Category.CORE, "second-pack")
        val packs = listOf(first, second).map { tag ->
            LibraryPack(Library(tag.sourceLibraryId, tag.sourceLibraryId,
                standaloneTags = listOf(tag)), emptyList())
        }
        val local = WorkspaceEditor(GeneratorEngine(listOf(first, second), random), packs)
        val filter = CustomIdeaFilter("filter:1", "Only second",
            sourceIds = setOf("second-pack"))
        val workspace = Workspace(listOf(Reel("r", Category.CORE)),
            preferences = Preferences(disabledLibraryIds = setOf(LibraryIds.FIXTURE),
                customIdeaFilters = listOf(filter), activeCustomIdeaFilterId = filter.id))
        assertEquals(second, local.spin(workspace).reels.single().tag)
    }
}
