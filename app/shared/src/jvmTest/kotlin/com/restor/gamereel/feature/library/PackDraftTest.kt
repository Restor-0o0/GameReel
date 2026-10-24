package com.restor.gamereel.feature.library

import com.restor.gamereel.data.importer.JsonLibraryImporter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PackDraftTest {
    private val packs = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "community/packs") }
        .first { it.isDirectory }

    @Test
    fun communityPacksSurviveTheEditor() {
        packs.listFiles { f -> f.extension == "json" }.orEmpty().forEach { file ->
            val original = JsonLibraryImporter().parse(file.readText()).pack
            val draft = PackDraft.from(original)
            assertNull(draft.problem(), file.name)
            val copy = JsonLibraryImporter().parse(draft.toJson()).pack
            assertEquals(original.library.reels.sorted(), copy.library.reels.sorted(), file.name)
            assertEquals(original.library.slots.toSet(), copy.library.slots.toSet(), file.name)
            assertEquals(original.library.standaloneTags.toSet(), copy.library.standaloneTags.toSet(), file.name)
            assertEquals(original.library.version + 1, copy.library.version)
        }
    }

    @Test
    fun newPackGetsValidIds() {
        val draft = PackDraft(name = "Моя игра").let { it.copy(slots = listOf(SlotDraft(0, "Герой", "🦸", count = 2))) }
            .withTags(0, listOf("Лучник", "лучник", " Маг ", ""))
        assertEquals(2, draft.tags.size)
        val pack = JsonLibraryImporter().parse(draft.toJson()).pack
        assert(pack.library.id.matches(Regex("pack-[0-9a-f]{6}")))
        assertEquals(listOf("${pack.library.id}:slot:slot-0", "${pack.library.id}:slot:slot-0"), pack.library.reels)
        assertNotNull(PackDraft(name = "x").problem())
    }
}
