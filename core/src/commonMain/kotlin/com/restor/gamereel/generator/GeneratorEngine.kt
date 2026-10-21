package com.restor.gamereel.generator

import com.restor.gamereel.model.*

class GeneratorEngine(tags: List<Tag>, private val random: RandomProvider) {
    private val byCategory = tags.groupBy { it.category }

    fun spin(
        reels: List<Reel>,
        config: GeneratorConfig = GeneratorConfig(),
        onlyId: String? = null,
    ): List<Reel> {
        val candidatesByCategory = config.tagPool?.groupBy { it.category } ?: byCategory
        val counts = mutableMapOf<String, Int>()
        reels
            .filter {
                it.state != ReelState.PINNED &&
                    (it.state == ReelState.FROZEN || (onlyId != null && it.id != onlyId))
            }
            .mapNotNull { it.tag?.sourceGameId }
            .forEach { counts[it] = (counts[it] ?: 0) + 1 }

        return reels.map { reel ->
            if (reel.state != ReelState.RANDOM || (onlyId != null && reel.id != onlyId))
                return@map reel

            val eligible =
                candidatesByCategory[reel.category].orEmpty().filter {
                    it.id !in config.blacklist &&
                        (config.tagPool != null || config.sourceIds.isEmpty() || it.sourceLibraryId in config.sourceIds)
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
            reel.copy(tag = selected)
        }
    }

    fun unlockFrozen(reels: List<Reel>) = reels.map {
        if (it.state == ReelState.FROZEN) it.copy(state = ReelState.RANDOM) else it
    }

    fun pin(reels: List<Reel>, tag: Tag, id: String): List<Reel> =
        if (reels.any { it.state == ReelState.PINNED && it.tag?.id == tag.id }) reels
        else listOf(Reel(id, tag.category, tag, ReelState.PINNED)) + reels
}
