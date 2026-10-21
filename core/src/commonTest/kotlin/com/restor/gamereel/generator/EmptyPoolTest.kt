package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.test.*

class EmptyPoolTest {
    private val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags,
        object : RandomProvider { override fun nextInt(bound: Int) = 0 }))

    @Test
    fun emptySourceSelectionDoesNotFallBackToAllSourcesAndCanBeUndone() {
        val before = editor.spin(BuiltinCatalog.initialWorkspace()).let {
            it.copy(preferences = it.preferences.copy(enabledLibraryIds = emptySet()))
        }
        val after = editor.spin(before)
        assertTrue(after.reels.all { it.tag == null })
        assertEquals(before, editor.undo(after))
        assertEquals(before.reels.map { it.category }, editor.missingReelCategories(after))
    }

    @Test
    fun fullyExcludedPoolClearsOnlyRequestedRandomReelAndPreservesLocks() {
        val tag = Tag("tag", "Online multiplayer", Category.CORE)
        val preset = GeneratorPreset("set", "Online", tags = listOf(tag))
        val before = Workspace(listOf(
            Reel("target", Category.CORE, tag),
            Reel("other", Category.CORE, tag),
            Reel("frozen", Category.CORE, tag, ReelState.FROZEN),
            Reel("pinned", Category.CORE, tag, ReelState.PINNED),
        ), preferences = Preferences(presets = listOf(preset), activePresetId = preset.id,
            ideaMode = IdeaMode.OFFLINE))
        val single = editor.spin(before, "target")
        assertNull(single.reels.first().tag)
        assertEquals(before.reels.drop(1), single.reels.drop(1))
        val all = editor.spin(before)
        assertTrue(all.reels.take(2).all { it.tag == null })
        assertEquals(before.reels.drop(2), all.reels.drop(2))
    }
}
