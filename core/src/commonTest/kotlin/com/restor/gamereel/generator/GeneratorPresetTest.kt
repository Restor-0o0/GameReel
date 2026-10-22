package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.test.*

class GeneratorPresetTest {
    private val engine =
        GeneratorEngine(
            BuiltinCatalog.tags,
            object : RandomProvider {
                override fun nextInt(bound: Int) = 0
            },
        )
    private val editor = WorkspaceEditor(engine)

    @Test
    fun gamesPresetKeepsEverySelectedGamesTagAndSurvivesImportWithoutLibrary() {
        val game = GameReference(
            "game:survival", "survival", "absent-library", "Survival Game",
            listOf(
                Tag("tag:survival", "survival", Category.CORE, "absent-library", "game:survival"),
                Tag("tag:forest", "forest", Category.SETTING, "absent-library", "game:survival"),
                Tag("tag:craft", "crafting", Category.INTERACTION, "absent-library", "game:survival"),
            ),
        )
        val initial = BuiltinCatalog.initialWorkspace()
        val saved = editor.saveGamesPreset(initial, "Survival", listOf(game, game))
        val preset = saved.preferences.presets.single()
        assertEquals(listOf(game), preset.games)
        assertTrue(preset.reels.isEmpty())
        val imported = editor.importPreset(initial, preset)
        val applied = editor.applyTagSet(imported, imported.preferences.presets.single().id)
        assertEquals(preset.games, imported.preferences.presets.single().games)
        assertEquals(preset.id, editor.applyTagSet(saved, preset.id).preferences.activePresetId)
        assertEquals(initial.reels.map { it.id to it.category }, applied.reels.map { it.id to it.category })
        assertTrue(applied.reels.mapNotNull { it.tag }.all { it in game.tags })
        assertTrue(editor.spin(applied).reels.mapNotNull { it.tag }.all { it in game.tags })
        assertNull(editor.undo(applied).preferences.activePresetId)
    }

    @Test
    fun customPresetCanMixManualTagsAndGamesAndRespectIncludeToggle() {
        val manual = Tag("manual", "manual", Category.CORE, "missing")
        val gameTag = Tag("game-tag", "game-only", Category.PRESSURE, "missing", "game")
        val game = GameReference("game", "1", "missing", "Game", listOf(gameTag))
        val initial = BuiltinCatalog.initialWorkspace()
        val without = editor.saveCustomPreset(initial, "Manual only", listOf(game), listOf(manual), false)
        val saved = without.preferences.presets.single()
        assertEquals(listOf(game), saved.games)
        assertEquals(listOf(manual), saved.tags)
        assertFalse(saved.includeGameTags)
        val applied = editor.applyTagSet(without, saved.id)
        assertTrue(applied.reels.mapNotNull { it.tag }.all { it == manual })
        assertEquals(initial.reels.map { it.category }, applied.reels.map { it.category })
        val with = editor.saveCustomPreset(initial, "All tags", listOf(game), listOf(manual), true)
        val appliedWith = editor.applyTagSet(with, with.preferences.presets.single().id)
        assertEquals(setOf(manual, gameTag), appliedWith.reels.mapNotNull { it.tag }.toSet())
    }

    @Test
    fun invalidImportedPresetIsRejected() {
        val original = BuiltinCatalog.initialWorkspace()
        assertFailsWith<IllegalArgumentException> {
            editor.importPreset(original, GeneratorPreset("x", "", listOf(Reel("x", Category.CORE))))
        }
    }

    @Test
    fun reelPresetChangesOnlyLayoutAndKeepsActiveTagSet() {
        val tag = Tag("manual", "manual", Category.CORE, "missing")
        val initial = BuiltinCatalog.initialWorkspace().copy(nextId = 20)
        val withSet = editor.saveCustomPreset(initial, "Core set", emptyList(), listOf(tag), false)
        val active = editor.applyTagSet(withSet, withSet.preferences.presets.single().id)
        val saved = editor.saveReelPreset(active, "Two cores", listOf(Category.CORE, Category.CORE))
        val preset = saved.preferences.presets.last()
        assertTrue(preset.isReelPreset)
        val applied = editor.applyPreset(saved, preset.id)
        assertEquals(listOf(Category.CORE, Category.CORE), applied.reels.map { it.category })
        assertEquals(2, applied.reels.map { it.id }.toSet().size)
        assertTrue(applied.reels.all { it.tag == tag })
        assertEquals(active.preferences.activePresetId, applied.preferences.activePresetId)
        assertEquals(saved.preferences.librarySelection, applied.preferences.librarySelection)
        assertEquals(saved.reels, editor.undo(applied).reels)
        val allTags = editor.clearPreset(applied)
        assertNull(allTags.preferences.activePresetId)
        assertEquals(applied.reels.map { it.id to it.category }, allTags.reels.map { it.id to it.category })
        assertEquals(applied, editor.undo(allTags))
    }

    @Test
    fun gameOnlySourcesBorrowEditorialTagsOnlyForUncoveredCategories() {
        val gameTag = Tag("game:core", "Puzzle", Category.CORE, "games", "game:1")
        val games = LibraryPack(Library("games", "Games"),
            listOf(GameReference("game:1", "1", "games", "Game", listOf(gameTag))))
        val gameEditor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags + games.allTags,
            object : RandomProvider { override fun nextInt(bound: Int) = 0 }), listOf(games))
        val workspace = BuiltinCatalog.initialWorkspace().copy(
            preferences = Preferences(enabledLibraryIds = setOf("games")))

        val jam = gameEditor.applyPreset(workspace, "builtin:jam")
        assertTrue(gameEditor.missingReelCategories(jam).isEmpty())
        assertEquals(gameTag, jam.reels.single { it.category == Category.CORE }.tag)
        assertTrue(jam.reels.filter { it.category != Category.CORE }.all { it.tag?.sourceLibraryId == LibraryIds.FIXTURE })

        val filtered = gameEditor.saveCustomIdeaFilter(gameEditor.enableAllLibraries(workspace),
            CustomIdeaFilter("", "Any game", sourceIds = setOf("games")))
        val filteredJam = gameEditor.applyPreset(filtered, "builtin:jam")
        assertTrue(gameEditor.missingReelCategories(filteredJam).isEmpty())
        assertEquals(gameTag, filteredJam.reels.single { it.category == Category.CORE }.tag)
        assertNotNull(filteredJam.reels.single { it.category == Category.TWIST }.tag)
        val withTwist = gameEditor.spin(gameEditor.enableCreativeTwists(filteredJam))
        assertTrue(withTwist.preferences.customIdeaFilters.single().includeCreativeTwists)
        assertEquals(gameTag, withTwist.reels.single { it.category == Category.CORE }.tag)

        val tagSet = gameEditor.saveCustomPreset(withTwist, "Core only", emptyList(),
            listOf(gameTag), false)
        val active = gameEditor.applyTagSet(tagSet, tagSet.preferences.presets.single().id)
        assertNull(gameEditor.applyPreset(active, "builtin:jam")
            .reels.single { it.category == Category.TWIST }.tag)
    }

    @Test
    fun oldMixedJsonStaysTagSetAndBuiltinLayoutsAreStillAvailable() {
        val tag = Tag("legacy", "legacy", Category.CORE, "missing")
        val legacy = GeneratorPreset("legacy", "Old", listOf(Reel("old", Category.WORLD)),
            tags = listOf(tag))
        assertTrue(legacy.isTagSet)
        val initial = BuiltinCatalog.initialWorkspace()
        val imported = editor.importPreset(initial, legacy)
        val appliedSet = editor.applyTagSet(imported, imported.preferences.presets.single().id)
        assertEquals(initial.reels.map { it.category }, appliedSet.reels.map { it.category })
        val appliedLayout = editor.applyPreset(appliedSet, "builtin:prototype")
        assertEquals(PresetCatalog.templates.first { it.id == "builtin:prototype" }.reels.map { it.category },
            appliedLayout.reels.map { it.category })
        assertEquals(appliedSet.preferences.activePresetId, appliedLayout.preferences.activePresetId)
    }

    @Test
    fun oldReelPresetRestoresOnlyTypesNotPinnedValues() {
        val tag = BuiltinCatalog.tags.first { it.category == Category.CORE }
        val old = GeneratorPreset("old", "Old layout",
            reels = listOf(Reel("legacy", Category.CORE, tag, ReelState.PINNED)))
        val imported = editor.importPreset(BuiltinCatalog.initialWorkspace(), old)
        val applied = editor.applyPreset(imported, imported.preferences.presets.single().id)
        assertEquals(listOf(Category.CORE), applied.reels.map { it.category })
        assertEquals(ReelState.RANDOM, applied.reels.single().state)
    }

    @Test
    fun positionIsClampedAndEmptyPresetsAreNotSaved() {
        val workspace = BuiltinCatalog.initialWorkspace()
        assertEquals(
            SpinButtonPosition(0f, 1f),
            editor
                .setSpinButtonPosition(workspace, SpinButtonPosition(-2f, 4f))
                .preferences
                .spinButtonPosition,
        )
        assertEquals(
            SpinButtonPosition(),
            editor
                .setSpinButtonPosition(
                    workspace,
                    SpinButtonPosition(Float.NaN, Float.POSITIVE_INFINITY),
                )
                .preferences
                .spinButtonPosition,
        )
        assertEquals(workspace, editor.savePreset(workspace, "  "))
    }

    @Test
    fun themeColorsCanBeChangedIndependently() {
        val original = BuiltinCatalog.initialWorkspace()
        val themed = editor.setUiText(
            editor.setUiBackground(
                editor.setUiAccent(original, UiAccent.MINT),
                UiBackground.NAVY,
            ),
            UiText.WARM,
        )
        assertEquals(UiAccent.MINT, themed.preferences.uiAccent)
        assertEquals(UiBackground.NAVY, themed.preferences.uiBackground)
        assertEquals(UiText.WARM, themed.preferences.uiText)
        assertEquals(original.reels, themed.reels)
    }

    @Test
    fun spinTimingIsBoundedAndLegacyDurationIsPreserved() {
        val workspace = BuiltinCatalog.initialWorkspace()
        val count = workspace.reels.count { it.state == ReelState.RANDOM }
        assertEquals(1100 + (count - 1) * 125, workspace.preferences.requestedSpinDurationMs(count))

        val timed = editor.setReelStopGap(editor.setSpinTotalDuration(workspace, 2000), 0)
        assertEquals(2000, timed.preferences.requestedSpinDurationMs(count))
        assertEquals(0, timed.preferences.reelStopGapMs)
        assertEquals(500, editor.setSpinTotalDuration(workspace, -1).preferences.spinTotalDurationMs)
        assertEquals(3000, editor.setReelStopGap(workspace, 9999).preferences.reelStopGapMs)
        val together = editor.setSpinTiming(workspace, 3000, 0)
        assertEquals(3000, together.preferences.spinTotalDurationMs)
        assertEquals(0, together.preferences.reelStopGapMs)
    }
}
