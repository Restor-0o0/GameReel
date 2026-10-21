package com.restor.gamereel.model

data class GeneratorConfig(
    val sourceIds: Set<String> = emptySet(),
    val blacklist: Set<String> = emptySet(),
    val maxAutoAtomsFromSameGame: Int = 2,
    val tagPool: List<Tag>? = null,
)
