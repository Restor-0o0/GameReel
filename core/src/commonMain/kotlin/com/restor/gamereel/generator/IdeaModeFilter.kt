package com.restor.gamereel.generator

import com.restor.gamereel.model.IdeaMode
import com.restor.gamereel.model.Tag

/** Lightweight scope hints, not a guarantee that a concept is feasible on a platform. */
fun IdeaMode.allows(tag: Tag): Boolean {
    if (this == IdeaMode.OPEN) return true
    val name = " ${normalizeModeWords(tag.name)} "
    val excluded = when (this) {
        IdeaMode.OPEN -> emptyList()
        IdeaMode.SOLO -> listOf(
            "multiplayer", "co op", "coop", "cooperative", "mmo", "mmorpg", "online", "pvp",
            "team based", "two player", "2 player", "1 vs many", "hidden role communication",
            "proximity voice", "player competition", "players can split",
            "players see different versions", "one player has different information",
            "one player controls logistics while others",
        )
        IdeaMode.JAM -> listOf(
            "mmo", "mmorpg", "open world", "massively", "live service", "campaign",
            "persistent world", "persistent online world", "persistent multiplayer",
            "large scope", "large team", "aaa",
        )
        IdeaMode.MOBILE -> listOf(
            "vr", "virtual reality", "keyboard", "mouse only", "desktop only", "console only",
        )
        IdeaMode.OFFLINE -> listOf(
            "online", "mmo", "mmorpg", "massively", "live service", "cloud gaming",
            "multiplayer server", "persistent multiplayer", "server dependent",
        )
    }
    return excluded.none { " $it " in name }
}

// Normalize punctuation too: co-op, co‑op and co op must follow the same rule.
private fun normalizeModeWords(value: String): String =
    value.lowercase().map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        .split(' ').filter { it.isNotEmpty() }.joinToString(" ")
