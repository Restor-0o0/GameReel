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
    val slot: ReelSlot? = null,
)

/** A reel kind declared by a pack, e.g. a hero or an item slot for one game. */
@Serializable
data class ReelSlot(
    val id: String,
    val name: String,
    val icon: String = "",
    val parent: String? = null,
    val unique: Boolean = true,
)

val Reel.kind: String get() = slot?.id ?: category.name
val Tag.kind: String get() = slot ?: category.name
