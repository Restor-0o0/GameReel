package com.restor.gamereel.data.personal

import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.LibraryIds
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersonalGamesTest {
    @Test
    fun exportedGamesRoundTripWithTagGroupsAndStableIds() {
        val pack = PersonalGames.save(null, null, "  My Game  ", listOf(
            GameTagDraft("Survival", Category.CORE),
            GameTagDraft("Pixel art", Category.VISUAL),
        ))
        val game = pack.games.single()
        val imported = JsonLibraryImporter().parse(Json.encodeToString(pack)).pack
        assertEquals(LibraryIds.PERSONAL, imported.library.id)
        assertEquals(game.id, imported.games.single().id)
        assertEquals(game.title, imported.games.single().title)
        assertEquals(game.tags.map { it.name to it.category },
            imported.games.single().tags.map { it.name to it.category })
    }

    @Test
    fun editsPreserveIdentityAndMergeDoesNotEraseLocalOnlyGames() {
        val first = PersonalGames.save(null, null, "First", listOf(GameTagDraft("A", Category.CORE)))
        val original = first.games.single()
        val local = PersonalGames.save(first, null, "Local", listOf(GameTagDraft("B", Category.WORLD)))
        val editedElsewhere = PersonalGames.save(first, original.id, "First edited",
            listOf(GameTagDraft("C", Category.VISUAL)))
        assertEquals(original.id, editedElsewhere.games.single().id)
        val merged = PersonalGames.merge(local, editedElsewhere)
        assertEquals(2, merged.games.size)
        assertEquals("First edited", merged.games.first { it.id == original.id }.title)
        assertTrue(merged.games.any { it.title == "Local" })
        assertEquals(merged, PersonalGames.merge(merged, editedElsewhere))
        assertEquals(1, PersonalGames.delete(merged, original.id).games.size)
    }
}
