package com.restor.gamereel.data.bundled

import com.restor.gamereel.data.bundled.loadBundledLibraries
import com.restor.gamereel.generator.GeneratorEngine
import com.restor.gamereel.generator.RandomProvider
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.GeneratorConfig
import com.restor.gamereel.model.Reel
import com.restor.gamereel.model.allTags
import com.restor.gamereel.ui.presentation.artPath
import gamereel.app.shared.generated.resources.Res
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class BundledLibrariesTest {
    @Test
    fun upgradeAddsGooglePackWithoutReplacingCurrentSteamPacks() = runTest {
        val additions =
            loadBundledLibraries(
                mapOf(
                    "steam_broad" to 7,
                    "steam_alltime_owners" to 7,
                    "curated_v5" to 10,
                    "itch_selected" to 4,
                    "steam_selected" to 1,
                )
            )
        if (Res.readBytes("files/art-policy.txt").decodeToString().trim() == "no-store-art") {
            assertTrue(additions.isEmpty(), "Public builds must not load historical Google data")
            return@runTest
        }
        val google = additions.single()
        assertEquals("google_play_alltime_like", google.library.id)
        assertEquals(1000, google.games.size)
        assertEquals(1000, google.games.map { it.externalId }.toSet().size)
        assertTrue(
            google.games.all { it.sourceLibraryId == google.library.id && it.tags.isNotEmpty() }
        )
        assertTrue(google.games.flatMap { it.tags }.any { it.category == Category.MONETIZATION })
        assertEquals(7, google.library.version)
        if (Res.readBytes("files/art-policy.txt").decodeToString().trim() != "no-store-art") {
            assertTrue(
                google.games
                    .filter { it.image != null }
                    .all { Res.readBytes(artPath(it)!!).isNotEmpty() }
            )
        }
        assertTrue(
            google.games.count {
                it.image?.startsWith("https://play-lh.googleusercontent.com/") == true
            } > 800
        )
        assertTrue(google.games.flatMap { it.tags }.any { it.category == Category.STRUCTURE })
        assertTrue(
            loadBundledLibraries(
                    mapOf(
                        "steam_broad" to 7,
                        "steam_alltime_owners" to 7,
                        google.library.id to 7,
                        "curated_v5" to 10,
                        "itch_selected" to 4,
                        "steam_selected" to 1,
                    )
                )
                .isEmpty()
        )
    }

    @Test
    fun v4LibrariesAreUpgradedAndCuratedPoolIsAdded() = runTest {
        val packs =
            loadBundledLibraries(
                mapOf(
                    "steam_broad" to 4,
                    "steam_alltime_owners" to 4,
                    "google_play_alltime_like" to 4,
                )
            )
        val publicBuild =
            Res.readBytes("files/art-policy.txt").decodeToString().trim() == "no-store-art"
        assertEquals(if (publicBuild) 3 else 6, packs.size)
        assertTrue(
            packs.all {
                it.library.version ==
                    when (it.library.id) {
                        "curated_v5" -> 10
                        "itch_selected" -> 4
                        "steam_selected" -> 1
                        else -> 7
                    }
            }
        )
        assertEquals(if (publicBuild) 48 else 3048, packs.sumOf { it.games.size })
        val curated = packs.single { it.library.id == "curated_v5" }
        assertTrue(curated.games.isEmpty())
        assertEquals(1461, curated.allTags.size)
        assertEquals(18, curated.allTags.map { it.category }.toSet().size)
        assertTrue(
            curated.allTags.all { it.sourceGameId == null && it.sourceLibraryId == "curated_v5" }
        )
        assertEquals(1461, curated.allTags.count { it.description.isNotBlank() })
        val categories =
            listOf(Category.WORLD, Category.LOCATION, Category.ENVIRONMENT, Category.TWIST)
        val engine =
            GeneratorEngine(
                curated.allTags,
                object : RandomProvider {
                    override fun nextInt(bound: Int) = 0
                },
            )
        val reels = categories.map { Reel(it.name, it) }
        assertTrue(
            engine.spin(reels, GeneratorConfig(sourceIds = setOf("curated_v5"))).all {
                it.tag != null
            }
        )
        assertTrue(
            engine.spin(reels, GeneratorConfig(sourceIds = setOf("fixture"))).all { it.tag == null }
        )
        val tags = packs.flatMap { it.allTags }
        assertEquals(tags.size, tags.map { it.id }.toSet().size)
        assertTrue(tags.any { it.category == Category.THEME })
        assertTrue(tags.any { it.category == Category.PRESSURE })
        assertTrue(
            tags.none {
                it.name.startsWith("unspecified", true) || it.name.contains("infer from", true)
            }
        )
    }

    @Test
    fun curatedUpdateReimportsOnlyCuratedLibrary() = runTest {
        val changes =
            loadBundledLibraries(
                mapOf(
                    "steam_broad" to 7,
                    "steam_alltime_owners" to 7,
                    "google_play_alltime_like" to 7,
                    "curated_v5" to 9,
                    "itch_selected" to 4,
                    "steam_selected" to 1,
                )
            )
        assertEquals("curated_v5", changes.single().library.id)
        assertEquals(10, changes.single().library.version)
    }

    @Test
    fun itchSelectionHasSourceLinksAndNoCopiedArtwork() = runTest {
        val pack = loadBundledLibraries(emptyMap()).single { it.library.id == "itch_selected" }
        assertEquals(38, pack.games.size)
        assertTrue(pack.allTags.size >= 180)
        assertTrue(
            pack.games.all { it.storeUrl?.contains(".itch.io/") == true && it.image == null }
        )
        assertTrue(pack.games.take(28).flatMap { it.tags }.all { it.description.isNotBlank() })
        assertEquals(38, pack.games.map { it.id }.toSet().size)
        assertTrue(pack.games.filter { game -> game.tags.any { it.name == "survival" } }.size >= 6)
        assertTrue(pack.games.all { it.tags.map { tag -> tag.category }.distinct().size >= 3 })
        assertTrue(pack.games.drop(28).all { it.tags.map { tag -> tag.category }.distinct().size >= 5 })
    }

    @Test
    fun itchV1UpgradesWithoutReimportingOtherCurrentLibraries() = runTest {
        val changes =
            loadBundledLibraries(
                mapOf(
                    "steam_broad" to 7,
                    "steam_alltime_owners" to 7,
                    "google_play_alltime_like" to 7,
                    "curated_v5" to 10,
                    "itch_selected" to 1,
                    "steam_selected" to 1,
                )
            )
        val pack = changes.single()
        assertEquals("itch_selected", pack.library.id)
        assertEquals(4, pack.library.version)
        assertEquals(38, pack.games.size)
        assertTrue(
            pack.games
                .map { it.externalId }
                .containsAll(
                    listOf(
                        "dicey-dungeons",
                        "celeste",
                        "a-short-hike",
                        "vvvvvv",
                        "baba-is-you-jam",
                        "one-night-stand",
                    )
                )
        )
    }

    @Test
    fun selectedSteamGamesAreSelfContainedAndHaveMultidimensionalTags() = runTest {
        val pack = loadBundledLibraries(emptyMap()).single { it.library.id == "steam_selected" }
        assertEquals(10, pack.games.size)
        assertEquals(10, pack.games.map { it.externalId }.toSet().size)
        assertTrue(pack.games.all { game ->
            game.image == null && game.storeUrl?.startsWith("https://store.steampowered.com/app/") == true &&
                game.tags.map { it.category }.distinct().size >= 6 &&
                game.tags.all { it.description.isNotBlank() }
        })
        assertTrue(pack.games.count { game -> game.tags.any { it.name == "survival" } } >= 4)
    }
}
