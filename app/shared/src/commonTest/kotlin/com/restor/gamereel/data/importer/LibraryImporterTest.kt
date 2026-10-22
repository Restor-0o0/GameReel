package com.restor.gamereel.data.importer

import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.*
import com.restor.gamereel.text.LocalizedException
import com.restor.gamereel.text.UiText
import com.restor.gamereel.ui.presentation.displayIcon
import gamereel.app.shared.generated.resources.*
import kotlin.test.*

class LibraryImporterTest {
    @Test
    fun standaloneAtomsHaveStableIdentityAndNoGameAttribution() {
        val importer = JsonLibraryImporter()
        val source =
            """{"library":{"id":"curated_v5","name":"Curated","version":5,"standaloneTags":[
            {"name":"Harbor","category":"LOCATION"},{"name":"harbor","category":"LOCATION"},
            {"name":"Orbit","category":"ENVIRONMENT"}]},"games":[]}"""
        val first = importer.parse(source).pack
        val second = importer.parse(source.replace("\"version\":5", "\"version\":6")).pack
        assertEquals(2, first.allTags.size)
        assertEquals(first.allTags, second.allTags)
        assertTrue(first.games.isEmpty())
        assertTrue(first.allTags.all { it.sourceGameId == null })
        assertFails { importer.parse("""{"library":{"id":"empty","name":"Empty"},"games":[]}""") }
    }

    @Test
    fun enrichedV4FieldsRetainCategoriesAndStableIds() {
        val importer = JsonLibraryImporter()
        val original =
            importer
                .parse(
                    """{"games":[{"library":"steam_broad","appid":1,"title":"Game","core_mechanics":["shooting"]}]}"""
                )
                .pack
        val enriched =
            importer
                .parse(
                    """{"annotation_version":4,"games":[{
            "library":"steam_broad","appid":1,"title":"Game","core_mechanics":["shooting"],
            "structure_tags":["runs"],"progression_tags":["upgrades"],"pressure_tags":["timer"],
            "interaction_tags":["physics"],"setting_tags":["space"],"theme_tags":["horror"],
            "visual_tags":["pixel art"],"camera_tags":["top-down"],"player_tags":["co-op"],
            "other_tags":["unclassified"],"steam_tags":["ignored legacy tag"]
        }]}"""
                )
                .pack
        assertEquals(4, enriched.library.version)
        assertEquals(original.games.single().id, enriched.games.single().id)
        assertEquals(
            original.games.single().tags.single().id,
            enriched.games.single().tags.first().id,
        )
        assertEquals(
            setOf(
                Category.CORE,
                Category.STRUCTURE,
                Category.PROGRESSION,
                Category.PRESSURE,
                Category.INTERACTION,
                Category.SETTING,
                Category.THEME,
                Category.VISUAL,
                Category.CAMERA,
                Category.MULTIPLAYER,
                Category.OTHER,
            ),
            enriched.games.single().tags.map { it.category }.toSet(),
        )
        assertTrue(enriched.games.single().tags.none { it.name == "ignored legacy tag" })
    }

    private val importer = JsonLibraryImporter()
    private val broad =
        """{"games":[{"appid":"1167630","source_rank":1,"title":"Teardown","core_mechanics":["destruction","physics"],"extra_mechanics":["sandbox"],"camera":"first-person","visual_style":"voxel 3D","player_mode":"singleplayer","steam_tags":["Physics"],"header_image_url":"https://example.com/header.jpg"}]}"""

    @Test
    fun adaptsSteamBroadAndPreservesIdsAcrossReorderingAndRename() {
        val first = importer.parse(broad).pack
        val changed =
            importer
                .parse(
                    broad
                        .replace("\"source_rank\":1", "\"source_rank\":500")
                        .replace("Teardown", "Teardown Renamed")
                )
                .pack
        assertEquals("steam_broad", first.library.id)
        assertEquals(first.games.single().id, changed.games.single().id)
        assertEquals(
            first.games.single().tags.map { it.id },
            changed.games.single().tags.map { it.id },
        )
        assertEquals("1167630", first.games.single().externalId)
        assertTrue(first.games.single().tags.any { it.category == Category.VISUAL })
    }

    @Test
    fun missingTagsDoNotBecomeInventedMechanics() {
        val result =
            importer.parse(
                """{"dataset":"All Time","games":[{"library":"steam_alltime_owners","appid":730,"title":"Counter-Strike","core_mechanics":[],"visual_style":"infer from reference image","camera":"unspecified / infer from game"}]}"""
            )
        assertTrue(result.pack.games.single().tags.isEmpty())
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun adaptsGooglePlayMonetizationAndExternalId() {
        val result =
            importer
                .parse(
                    """{"games":[{"library":"google_play_alltime_like","app_id":"org.example.game","title":"Game","core_mechanics":["puzzles"],"mobile_business_tags":["ads","premium"]}]}"""
                )
                .pack
        assertEquals("org.example.game", result.games.single().externalId)
        assertEquals(2, result.games.single().tags.count { it.category == Category.MONETIZATION })
    }

    @Test
    fun canonicalImportIncludesDescriptionsAndIcons() {
        val result =
            importer
                .parse(
                    """{"library":{"id":"my_pack","name":"My pack","version":2},"games":[{"id":"game","title":"Reference","tags":[{"name":"Overheat","category":"PRESSURE","description":"Machines get hot","icon":"🔥"}]}]}"""
                )
                .pack
        assertEquals(2, result.library.version)
        assertEquals("Machines get hot", result.games.single().tags.single().description)
        assertEquals("🔥", result.games.single().tags.single().displayIcon)
    }

    @Test
    fun malformedImportsGiveUsefulErrors() {
        assertFailsWith<IllegalArgumentException> { importer.parse("{") }
        assertFails { importer.parse("""{"games":[]}""") }
        val duplicate = """{"games":[{"appid":"1","title":"A"},{"appid":"1","title":"B"}]}"""
        assertEquals(Res.string.import_duplicate_id, errorOf { importer.parse(duplicate) })
        val category =
            """{"library":{"id":"test","name":"Test"},"games":[{"id":"1","title":"A","tags":[{"name":"X","category":"NOT_A_CATEGORY"}]}]}"""
        assertEquals(Res.string.import_game_unknown_category, errorOf { importer.parse(category) })
    }

    private fun errorOf(block: () -> Unit) =
        ((assertFailsWith<LocalizedException>(block = block).text) as UiText.Str).id

    @Test
    fun unsafeImageReferencesAreNotUsedAsPaths() {
        val result =
            importer.parse(
                """{"games":[{"appid":"1","title":"A","local_image":"../../secret.png"}]}"""
            )
        assertNull(result.pack.games.single().image)
        assertTrue(result.warnings.isNotEmpty())
    }
}

class PackImporterTest {
    private val importer = JsonLibraryImporter()

    @Test
    fun parsesSlotsReelsAndRequires() {
        val pack = importer.parse("""{"library":{"id":"dota2","name":"Dota 2","version":1,
            "slots":[{"id":"hero","name":"Hero","icon":"H"},{"id":"talent10","name":"Talent","parent":"hero"},{"id":"item","name":"Item","unique":false}],
            "reels":["hero","item","talent10"],
            "standaloneTags":[{"name":"Anti-Mage","slot":"hero"},{"name":"+20 Agility","slot":"talent10","requires":["anti-mage"]},
            {"name":"+20 Agility","slot":"talent10","requires":["Axe"]},{"name":"Orphan","slot":"talent10","requires":["Nobody"]},
            {"name":"Blink","slot":"item"}]}}""").pack
        assertTrue(pack.isGamePack)
        assertEquals(listOf("dota2:slot:hero", "dota2:slot:item", "dota2:slot:talent10"), pack.library.reels)
        assertEquals("dota2:slot:hero", pack.library.slots[1].parent)
        assertFalse(pack.library.slots[2].unique)
        val hero = pack.allTags.single { it.name == "Anti-Mage" }
        assertEquals("H", hero.icon)
        assertEquals(listOf(hero.id), pack.allTags.single { it.name == "+20 Agility" }.requires)
        assertTrue(pack.allTags.none { it.name == "Orphan" })
    }

    @Test
    fun rejectsBrokenSlots() {
        fun parse(slots: String, reels: String = "[]") = importer.parse(
            """{"library":{"id":"p","name":"P","slots":$slots,"reels":$reels,"standaloneTags":[{"name":"X","category":"CORE"}]}}""")
        assertFails { parse("""[{"id":"a","name":"A","parent":"b"}]""") }
        assertFails { parse("""[{"id":"a","name":"A","parent":"b"},{"id":"b","name":"B","parent":"a"}]""") }
        assertFails { parse("""[{"id":"a b","name":"A"}]""") }
        assertFails { parse("""[{"id":"a","name":"A"}]""", """["c"]""") }
    }
}
