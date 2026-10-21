package com.restor.gamereel.generator

import com.restor.gamereel.model.*

// Explicit aliases, never substring matching ("server farm" is not the farming mechanic).
enum class Mechanic(val aliases: Set<String>) {
    CRAFTING(setOf("crafting", "crafting recipes", "building and crafting")),
    INVENTORY(
        setOf(
            "inventory management",
            "inventory tetris",
            "grid inventory decisions",
            "inventory pressure",
        )
    ),
    SCARCITY(setOf("resource scarcity", "resource management", "resource decay", "scarcity")),
    STEALTH(setOf("stealth", "stealth detection", "stealth gadgets", "stealth takedowns")),
    DESTRUCTION(
        setOf(
            "destruction",
            "physics destruction",
            "destructible environment",
            "destructible objects",
        )
    ),
    PHYSICS(setOf("physics manipulation", "physics interaction")),
    DECKBUILDING(setOf("deckbuilding", "deck building", "deck growth", "run deck growth")),
    RUNS(setOf("short runs", "runs", "roguelike runs", "roguelite runs", "run-based", "run based")),
    COOP(
        setOf(
            "co-op",
            "online co-op",
            "local co-op",
            "co-op roles",
            "co-op with separate roles",
            "co-op with shared resources",
            "cooperative",
        )
    ),
    FARMING(setOf("farming", "farm growth")),
    DAYS(setOf("day cycle", "turn-based days", "day-night cycle")),
    TIMER(setOf("timer", "time pressure", "time limit")),
    PUZZLES(
        setOf(
            "puzzles",
            "puzzle solving",
            "word puzzles",
            "escape-room puzzles",
            "environmental puzzle interaction",
        )
    ),
    DIALOGUE(
        setOf("dialogue choices", "dialogue interaction", "branching dialogue", "dialogue wheel")
    ),
    EXPLORATION(setOf("exploration", "open exploration")),
    PLATFORMING(setOf("platforming", "precision platforming")),
    RHYTHM(setOf("rhythm play", "rhythm timing", "rhythm minigames")),
}

private val mechanicCategories =
    setOf(
        Category.CORE,
        Category.SECONDARY_MECHANIC,
        Category.STRUCTURE,
        Category.PRESSURE,
        Category.PROGRESSION,
        Category.INTERACTION,
        Category.MULTIPLAYER,
    )
private val whitespace = Regex("\\s+")
private val aliases = Mechanic.entries.flatMap { key -> key.aliases.map { it to key } }.toMap()

fun mechanicFor(tag: Tag): Mechanic? =
    if (tag.category in mechanicCategories)
        aliases[tag.name.trim().lowercase().replace(whitespace, " ")]
    else null

enum class SynergyRule(val mechanics: Set<Mechanic>) {
    EXPEDITION(setOf(Mechanic.CRAFTING, Mechanic.INVENTORY, Mechanic.SCARCITY)),
    SURVIVAL(setOf(Mechanic.CRAFTING, Mechanic.SCARCITY)),
    LOADOUT(setOf(Mechanic.CRAFTING, Mechanic.INVENTORY)),
    HEIST(setOf(Mechanic.STEALTH, Mechanic.DESTRUCTION)),
    PHYSICS_PUZZLE(setOf(Mechanic.PHYSICS, Mechanic.PUZZLES)),
    RUN_DECK(setOf(Mechanic.DECKBUILDING, Mechanic.RUNS)),
    TEAM_CLOCK(setOf(Mechanic.COOP, Mechanic.TIMER)),
    FARM_CYCLE(setOf(Mechanic.FARMING, Mechanic.DAYS)),
    DISCOVERY(setOf(Mechanic.DIALOGUE, Mechanic.EXPLORATION)),
    RHYTHM_JUMPS(setOf(Mechanic.RHYTHM, Mechanic.PLATFORMING)),
}

data class Synergy(val rule: SynergyRule, val tags: List<Tag>)

data class RareCombination(val tags: List<Tag>, val occurrences: Int, val corpusSize: Int)

fun findSynergies(tags: List<Tag>): List<Synergy> {
    val byMechanic = tags.mapNotNull { tag -> mechanicFor(tag)?.let { it to tag } }.toMap()
    val matches = mutableListOf<Synergy>()
    for (rule in SynergyRule.entries.sortedByDescending { it.mechanics.size }) {
        if (
            byMechanic.keys.containsAll(rule.mechanics) &&
                matches.none { it.rule.mechanics.containsAll(rule.mechanics) }
        ) {
            matches.add(Synergy(rule, rule.mechanics.map { byMechanic.getValue(it) }))
        }
    }
    return matches.take(3)
}

// Built once per selected corpus, off the UI thread. Missing annotations are not proof of novelty.
class CombinationIndex(libraries: List<LibraryPack>) {
    private val games =
        libraries
            .flatMap { it.games }
            .groupBy { it.title.trim().lowercase().replace(whitespace, " ") }
            .values
            .map { copies -> copies.flatMap { it.tags }.mapNotNull(::mechanicFor).toSet() }
            .filter { it.size >= 2 }
    val corpusSize: Int = games.size
    private val support = Mechanic.entries.associateWith { key -> games.count { key in it } }
    private val pairs =
        buildMap<Set<Mechanic>, Int> {
            for (game in games) {
                val keys = game.toList()
                for (i in keys.indices) for (j in i + 1 until keys.size) {
                    val pair = setOf(keys[i], keys[j])
                    put(pair, (get(pair) ?: 0) + 1)
                }
            }
        }

    fun rareCombinations(tags: List<Tag>): List<RareCombination> {
        if (corpusSize < 20) return emptyList()
        val unique =
            tags.distinctBy(::mechanicFor).mapNotNull { tag -> mechanicFor(tag)?.let { it to tag } }
        val result = mutableListOf<RareCombination>()
        for (i in unique.indices) for (j in i + 1 until unique.size) {
            val a = unique[i]
            val b = unique[j]
            if (support.getValue(a.first) < 3 || support.getValue(b.first) < 3) continue
            val count = pairs[setOf(a.first, b.first)] ?: 0
            if (count <= 2)
                result.add(RareCombination(listOf(a.second, b.second), count, corpusSize))
        }
        return result.sortedBy { it.occurrences }.take(2)
    }
}
