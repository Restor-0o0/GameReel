package com.restor.gamereel.model

import kotlinx.serialization.Serializable

@Serializable
data class GeneratorPreset(
    val id: String,
    val name: String,
    // Kept for older JSON presets. New presets select a tag pool, not a reel layout.
    val reels: List<Reel> = emptyList(),
    // Null means use current sources (built-in templates); saved presets always have an allowlist.
    val libraryIds: Set<String>? = null,
    // A self-contained snapshot: imported presets still work without the original library.
    val games: List<GameReference> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val includeGameTags: Boolean = true,
)

val GeneratorPreset.isTagSet: Boolean get() = tags.isNotEmpty() || games.isNotEmpty()
val GeneratorPreset.isReelPreset: Boolean get() = reels.isNotEmpty() && !isTagSet

@Serializable
data class LibrarySelection(
    val disabled: Set<String> = emptySet(),
    val enabled: Set<String>? = null,
)

@Serializable data class SpinButtonPosition(val x: Float = 1f, val y: Float = 1f)

val Preferences.librarySelection: LibrarySelection
    get() = LibrarySelection(disabledLibraryIds, enabledLibraryIds)

fun Preferences.includesLibrary(id: String): Boolean =
    enabledLibraryIds?.contains(id) ?: (id !in disabledLibraryIds)

object PresetCatalog {
    val templates =
        listOf(
            template(
                "jam",
                Category.CORE,
                Category.STRUCTURE,
                Category.VISUAL,
                Category.TWIST,
            ),
            template("prototype", Category.CORE, Category.INTERACTION, Category.PRESSURE, Category.SCOPE),
            template(
                "experiment",
                Category.CORE,
                Category.INTERACTION,
                Category.STRUCTURE,
                Category.WORLD,
                Category.TWIST,
            ),
            template("world", Category.WORLD, Category.LOCATION, Category.ENVIRONMENT, Category.THEME),
            template("gameplay", Category.CORE, Category.SECONDARY_MECHANIC, Category.STRUCTURE, Category.PRESSURE, Category.PROGRESSION),
            template("presentation", Category.CAMERA, Category.VISUAL, Category.THEME, Category.SCOPE),
            template("social", Category.CORE, Category.MULTIPLAYER, Category.INTERACTION, Category.STRUCTURE),
        )

    private fun template(id: String, vararg categories: Category) =
        GeneratorPreset(
            "builtin:$id",
            id,
            categories.mapIndexed { i, category -> Reel("template:$i", category) },
        )
}
