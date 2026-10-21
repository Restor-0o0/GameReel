package com.restor.gamereel.model

import kotlinx.serialization.Serializable

enum class ReelState {
    RANDOM,
    FROZEN,
    PINNED,
}

@Serializable
data class Reel(
    val id: String,
    val category: Category,
    val tag: Tag? = null,
    val state: ReelState = ReelState.RANDOM,
)
