package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.test.*

class DailyIdeaTest {
    private val curated = Tag("curated:x", "Curated", Category.CORE, LibraryIds.CURATED)
    private val imported = Tag("imported:x", "Imported", Category.CORE, "user_lib")

    @Test
    fun sameDayGivesSameReelsRegardlessOfTagOrder() {
        val tags = BuiltinCatalog.tags + curated + imported
        assertEquals(DailyIdea.reels(20_000, tags), DailyIdea.reels(20_000, tags.reversed()))
        assertTrue(DailyIdea.reels(20_000, tags).all { it.tag != null })
    }

    @Test
    fun ignoresImportedLibraries() {
        assertEquals(DailyIdea.reels(20_001, BuiltinCatalog.tags + curated),
            DailyIdea.reels(20_001, BuiltinCatalog.tags + curated + imported))
    }

    @Test
    fun differentDaysDiffer() {
        val days = (0L..6L).map { DailyIdea.reels(20_000 + it, BuiltinCatalog.tags) }
        assertTrue(days.distinct().size > 1)
    }

    @Test
    fun convertsEpochDays() {
        assertEquals("1970-01-01", DailyIdea.isoDate(0))
        assertEquals("2026-10-21", DailyIdea.isoDate(20_747))
        assertEquals(20_747, DailyIdea.epochDay(1_792_540_800_000 + 1000))
        assertEquals(-1, DailyIdea.epochDay(-1))
    }

    @Test
    fun editorOpensDailyIdeaWithUndo() {
        val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags, KotlinRandomProvider()))
        val start = BuiltinCatalog.initialWorkspace()
        val daily = editor.dailyIdea(start, 20_000)
        assertEquals(DailyIdea.reels(20_000, BuiltinCatalog.tags), daily.reels)
        assertEquals(start.reels, editor.undo(daily).reels)
    }

    @Test
    fun outcomeIsStoredOnIdea() {
        val editor = WorkspaceEditor(GeneratorEngine(BuiltinCatalog.tags, KotlinRandomProvider()))
        val reel = Reel("r", Category.CORE, BuiltinCatalog.tags.first())
        val workspace = Workspace(listOf(reel), ideas = listOf(SavedIdea("idea:1", "A", listOf(reel))))
        val updated = editor.setIdeaOutcome(workspace, "idea:1",
            IdeaOutcome(IdeaStage.JAM, "  https://example.com/jam  "))
        assertEquals(IdeaOutcome(IdeaStage.JAM, "https://example.com/jam"), updated.ideas.single().outcome)
    }
}
