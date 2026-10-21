package com.restor.gamereel.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomIdeaFilter(
    val id: String,
    val name: String,
    val baseMode: IdeaMode = IdeaMode.OPEN,
    val excludedTerms: List<String> = emptyList(),
    /** Null follows the currently enabled libraries. A non-empty set locks the sources. */
    val sourceIds: Set<String>? = null,
    /** Supplement game-only sources with editorial twist prompts. */
    val includeCreativeTwists: Boolean = false,
)

fun normalizeFilterTerms(terms: List<String>): List<String> =
    terms.map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(20)
