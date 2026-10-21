package com.restor.gamereel.generator

import com.restor.gamereel.model.*
import kotlin.test.*

class IdeaModeFilterTest {
    private fun tag(name: String) = Tag(name, name, Category.CORE)

    @Test
    fun abbreviationsDoNotMatchInsideUnrelatedWords() {
        for (mode in IdeaMode.entries) {
            for (name in listOf("Ammo scarcity", "Summoning", "Summon placement", "Offline crafting")) {
                assertTrue(mode.allows(tag(name)), "$mode incorrectly excluded $name")
            }
        }
    }

    @Test
    fun multiplayerSpellingVariantsAndImplicitGroupInteractionsAreRecognized() {
        for (name in listOf("Co-op", "CO‑OP", "co op", "cooperative/AI opposition",
            "MMORPG", "team PvP", "two-player carry", "1-vs-many",
            "one player has different information from others")) {
            assertFalse(IdeaMode.SOLO.allows(tag(name)), name)
        }
        assertTrue(IdeaMode.SOLO.allows(tag("Solo with AI companions")))
        assertTrue(IdeaMode.SOLO.allows(tag("Players draw their own map")))
        assertTrue(IdeaMode.SOLO.allows(tag("Players decide when run is over")))
        assertTrue(IdeaMode.OFFLINE.allows(tag("Local co-op")))
        assertTrue(IdeaMode.OFFLINE.allows(tag("Hot-seat multiplayer")))
    }

    @Test
    fun scopeAndPlatformRestrictionsRecognizeSeparatedWords() {
        for (name in listOf("Open-world", "Persistent online world", "MMORPG", "AAA")) {
            assertFalse(IdeaMode.JAM.allows(tag(name)), name)
        }
        for (name in listOf("VR-only", "Virtual-reality", "Keyboard controls", "Mouse-only")) {
            assertFalse(IdeaMode.MOBILE.allows(tag(name)), name)
        }
        for (name in listOf("Online co-op", "MMORPG", "Persistent multiplayer", "Cloud gaming")) {
            assertFalse(IdeaMode.OFFLINE.allows(tag(name)), name)
        }
    }
}
