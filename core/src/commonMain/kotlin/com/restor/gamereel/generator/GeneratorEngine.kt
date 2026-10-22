package com.restor.gamereel.generator

import com.restor.gamereel.model.*

class GeneratorEngine(tags: List<Tag>, private val random: RandomProvider) {
    private val byKind = tags.groupBy { it.kind }

    fun spin(
        reels: List<Reel>,
        config: GeneratorConfig = GeneratorConfig(),
        onlyId: String? = null,
    ): List<Reel> {
        val targets = onlyId?.let { dependentsOf(reels, it) + it }
        val candidatesByKind = config.tagPool?.groupBy { it.kind } ?: byKind
        val counts = mutableMapOf<String, Int>()
        reels
            .filter {
                it.state != ReelState.PINNED &&
                    (it.state == ReelState.FROZEN || (targets != null && it.id !in targets))
            }
            .mapNotNull { it.tag?.sourceGameId }
            .forEach { counts[it] = (counts[it] ?: 0) + 1 }

        val result = reels.toMutableList()
        // parents roll before the reels that depend on them
        for (index in reels.indices.sortedBy { depth(reels, reels[it]) }) {
            val reel = reels[index]
            if (reel.state != ReelState.RANDOM || (targets != null && reel.id !in targets)) continue

            val parent = reel.slot?.parent?.let { id -> result.firstOrNull { it.slot?.id == id } }
            if (parent != null && parent.tag == null) {
                result[index] = reel.copy(tag = null)
                continue
            }
            val taken =
                if (reel.slot?.unique == true)
                    result.filterIndexed { i, other -> i != index && other.kind == reel.kind }
                        .mapNotNull { it.tag?.name?.trim()?.lowercase() }.toSet()
                else emptySet()
            val eligible =
                candidatesByKind[reel.kind].orEmpty().filter {
                    it.id !in config.blacklist &&
                        (config.tagPool != null || config.sourceIds.isEmpty() || it.sourceLibraryId in config.sourceIds) &&
                        (parent == null || it.requires.isEmpty() || parent.tag!!.id in it.requires) &&
                        it.name.trim().lowercase() !in taken
                }
            val diverse = eligible.filter {
                it.sourceGameId == null ||
                    (counts[it.sourceGameId] ?: 0) < config.maxAutoAtomsFromSameGame
            }
            // Source and blacklist restrictions stay strict; diversity is only a preference.
            val candidates = diverse.ifEmpty { eligible }

            val atoms = candidates.groupBy { it.name.trim().lowercase() }
            val alternatives =
                atoms
                    .filterKeys { it != reel.tag?.name?.trim()?.lowercase() }
                    .ifEmpty { atoms }
                    .values
                    .toList()

            // pick atom first, otherwise popular games win
            val selected =
                alternatives
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        val references = it[random.nextInt(it.size)]
                        references[random.nextInt(references.size)]
                    }

            selected?.sourceGameId?.let { counts[it] = (counts[it] ?: 0) + 1 }
            result[index] = reel.copy(tag = selected)
        }
        return result
    }

    /** Random reels whose slot hangs on the given reel, directly or through other slots. */
    fun dependentsOf(reels: List<Reel>, id: String): Set<String> {
        val found = mutableSetOf<String>()
        var parents = reels.filter { it.id == id }.mapNotNull { it.slot?.id }.toSet()
        while (parents.isNotEmpty()) {
            val next = reels.filter {
                it.state == ReelState.RANDOM && it.slot?.parent in parents && found.add(it.id)
            }
            parents = next.mapNotNull { it.slot?.id }.toSet()
        }
        return found
    }

    private fun depth(reels: List<Reel>, reel: Reel): Int {
        var level = 0
        var parent = reel.slot?.parent
        while (parent != null && level < 10) {
            level++
            parent = reels.firstOrNull { it.slot?.id == parent }?.slot?.parent
        }
        return level
    }

    fun unlockFrozen(reels: List<Reel>) = reels.map {
        if (it.state == ReelState.FROZEN) it.copy(state = ReelState.RANDOM) else it
    }

    fun pin(reels: List<Reel>, tag: Tag, id: String, slot: ReelSlot? = null): List<Reel> =
        if (reels.any { it.state == ReelState.PINNED && it.tag?.id == tag.id }) reels
        else listOf(Reel(id, tag.category, tag, ReelState.PINNED, slot)) + reels
}
