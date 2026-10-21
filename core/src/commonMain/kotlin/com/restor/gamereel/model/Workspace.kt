package com.restor.gamereel.model

import kotlinx.serialization.Serializable

@Serializable
data class Workspace(
    val reels: List<Reel>,
    val ideas: List<SavedIdea> = emptyList(),
    val nextId: Long = 1,
    val preferences: Preferences = Preferences(),
    val history: List<List<Reel>> = emptyList(),
)
