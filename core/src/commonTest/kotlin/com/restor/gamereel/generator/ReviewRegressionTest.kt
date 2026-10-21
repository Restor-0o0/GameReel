package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.test.*

class ReviewRegressionTest {
    private val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags,
        object : RandomProvider { override fun nextInt(bound: Int) = 0 }))

    @Test
    fun undoResetRestoresVariantAndSavingKeepsItsParent() {
        val saved = editor.saveIdea(editor.spin(BuiltinCatalog.initialWorkspace()), "Parent", "")
        val parent = saved.ideas.single().id
        val variant = editor.beginVariant(saved, parent)
        val undone = editor.undo(editor.resetReels(variant))
        assertEquals(variant, undone)
        assertEquals(parent, editor.saveIdea(undone, "Variant", "").ideas.first().variantOfId)
    }

    @Test
    fun variantHistoryIsBoundedAndUndoRestoresNullParent() {
        var workspace = editor.saveIdea(editor.spin(BuiltinCatalog.initialWorkspace()), "Parent", "")
        workspace = editor.beginVariant(workspace, workspace.ideas.single().id)
        workspace = editor.resetReels(workspace)
        workspace = editor.spin(workspace)
        assertNull(editor.undo(workspace).preferences.activeVariantParentId)
        repeat(110) { workspace = editor.spin(workspace) }
        assertEquals(WorkspaceEditor.HISTORY_LIMIT, workspace.preferences.activeVariantHistory.size)
        assertEquals(workspace.history.size, workspace.preferences.activeVariantHistory.size)
    }

    @Test
    fun twentyAndOneHundredReelPresetsCanBeSavedImportedAndApplied() {
        for (count in listOf(20, ContentLimits.MAX_REELS)) {
            var workspace = BuiltinCatalog.initialWorkspace()
            repeat(count - workspace.reels.size) { workspace = editor.addReel(workspace, Category.CORE) }
            val saved = editor.savePreset(workspace, "$count reels")
            val preset = saved.preferences.presets.single()
            assertEquals(count, preset.reels.size)
            val imported = editor.importPreset(BuiltinCatalog.initialWorkspace(), preset)
            val applied = editor.applyPreset(imported, imported.preferences.presets.single().id)
            assertEquals(count, applied.reels.size)
            assertEquals(count, applied.reels.map { it.id }.distinct().size)
        }
    }

    @Test
    fun tooManyReelsProduceAnExplicitError() {
        val categories = List(ContentLimits.MAX_REELS + 1) { Category.CORE }
        val error = assertFailsWith<EditorException> {
            editor.saveReelPreset(BuiltinCatalog.initialWorkspace(), "Too many", categories)
        }
        assertEquals(EditorError.TOO_MANY_REELS, error.error)
        val full = Workspace(categories.dropLast(1).mapIndexed { i, c -> Reel("r:$i", c) })
        assertFailsWith<EditorException> { editor.addReel(full, Category.CORE) }
        assertFailsWith<EditorException> { editor.pinTag(full, BuiltinCatalog.tags.first()) }
    }
}
