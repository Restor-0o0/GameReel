package com.restor.gamereel.model

import kotlinx.serialization.Serializable

object LibraryIds {
    const val FIXTURE = "fixture"
    const val STEAM_BROAD = "steam_broad"
    const val STEAM_ALLTIME = "steam_alltime_owners"
    const val GOOGLE_PLAY = "google_play_alltime_like"
    const val CURATED = "curated_v5"
    const val ITCH_SELECTED = "itch_selected"
    const val STEAM_SELECTED = "steam_selected"
    const val PERSONAL = "personal_games"
}

@Serializable
data class Library(
    val id: String,
    val name: String,
    val version: Int = 1,
    val source: String = "",
    val standaloneTags: List<Tag> = emptyList(),
    val slots: List<ReelSlot> = emptyList(),
    val reels: List<String> = emptyList(),
)

@Serializable
data class GameReference(
    val id: String,
    val externalId: String,
    val sourceLibraryId: String,
    val title: String,
    val tags: List<Tag>,
    val image: String? = null,
    val storeUrl: String? = null,
)

@Serializable data class LibraryPack(val library: Library, val games: List<GameReference>)

val LibraryPack.isGamePack: Boolean get() = library.reels.isNotEmpty()

val LibraryPack.allTags: List<Tag>
    get() = library.standaloneTags + games.flatMap { it.tags }

enum class ImportWarning {
    UNTAGGED_GAMES,
    LOCAL_IMAGES,
}

data class ImportResult(val pack: LibraryPack, val warnings: List<ImportWarning>)
