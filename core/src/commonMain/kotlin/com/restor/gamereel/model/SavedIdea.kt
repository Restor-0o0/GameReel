package com.restor.gamereel.model

import kotlinx.serialization.Serializable

@Serializable
data class IdeaConcept(
    val pitch: String = "",
    val coreLoop: String = "",
    val playerGoal: String = "",
    val hook: String = "",
)

@Serializable
data class SavedIdea(
    val id: String,
    val title: String,
    val reels: List<Reel>,
    val notes: String = "",
    val concept: IdeaConcept = IdeaConcept(),
    val project: String = "",
    val variantOfId: String? = null,
)
