package com.restor.gamereel.data.bundled

import gamereel.app.shared.generated.resources.Res
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class PublicLibrariesTest {
    @Test
    fun resourcePolicySelectsTheCorrectCatalogAndSupportsSecondLaunch() = runTest {
        val publicBuild =
            Res.readBytes("files/art-policy.txt").decodeToString().trim() == "no-store-art"
        val packs = loadBundledLibraries(emptyMap())
        val expected = if (publicBuild) setOf("curated_v5", "itch_selected", "steam_selected") else bundledLibraryIds
        assertEquals(expected, packs.map { it.library.id }.toSet())
        if (publicBuild) {
            assertTrue(packs.flatMap { it.games }.all { it.image == null })
            assertEquals(48, packs.sumOf { it.games.size })
        }
        assertTrue(
            loadBundledLibraries(packs.associate { it.library.id to it.library.version }).isEmpty()
        )
    }
}
