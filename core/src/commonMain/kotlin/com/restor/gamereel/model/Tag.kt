package com.restor.gamereel.model

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: Category,
    val sourceLibraryId: String = LibraryIds.FIXTURE,
    val sourceGameId: String? = null,
    val description: String = "",
    val icon: String = "",
)
