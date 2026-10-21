package com.restor.gamereel.catalog

import com.restor.gamereel.model.*

object BuiltinCatalog {
    const val TEARDOWN = "teardown"
    val gameNames = mapOf(TEARDOWN to "Teardown")
    // ids stay fixed, names and order can change
    val tags =
        listOf(
                Category.CORE to
                    listOf(
                        "destruction|Physics destruction",
                        "farming|Farming",
                        "building|Building",
                        "deckbuilding|Deckbuilding",
                        "stealth|Stealth",
                    ),
                Category.SECONDARY_MECHANIC to
                    listOf(
                        "inventory|Inventory management",
                        "crafting|Crafting",
                        "logistics|Logistics",
                        "repair|Repair",
                        "dialogue|Dialogue choices",
                    ),
                Category.STRUCTURE to
                    listOf(
                        "runs|Short runs",
                        "sandbox|Sandbox",
                        "contracts|Contracts",
                        "campaign|Campaign",
                        "days|Day cycle",
                    ),
                Category.PRESSURE to
                    listOf(
                        "timer|Timer",
                        "debt|Debt",
                        "hunger|Hunger",
                        "pursuit|Pursuit",
                        "decay|Resource decay",
                    ),
                Category.CAMERA to
                    listOf(
                        "first|First-person",
                        "third|Third-person",
                        "top|Top-down",
                        "iso|Isometric",
                        "side|Side view",
                    ),
                Category.VISUAL to
                    listOf(
                        "ps1|PS1 low-poly",
                        "pixel|Pixel art",
                        "voxel|Voxels",
                        "painted|Hand-painted world",
                        "mono|Monochrome",
                    ),
                Category.SETTING to
                    listOf(
                        "factory|Abandoned factory",
                        "ocean|Underwater lab",
                        "space|Space station",
                        "village|Forest village",
                        "train|Endless train",
                    ),
                Category.TWIST to
                    listOf(
                        "break|Any machine can break",
                        "enemy|Player runs the enemy economy",
                        "night|The world changes every night",
                        "memory|Items remember their owners",
                        "cost|Every upgrade has a price",
                    ),
            )
            .flatMap { (category, values) ->
                values.map { value ->
                    val (key, name) = value.split('|', limit = 2)
                    Tag(
                        "${LibraryIds.FIXTURE}:${category.name}:$key",
                        name,
                        category,
                        sourceGameId =
                            if (key == "destruction" || key == "voxel") TEARDOWN else null,
                    )
                }
            }
    val defaultCategories =
        listOf(
            Category.CORE,
            Category.SECONDARY_MECHANIC,
            Category.STRUCTURE,
            Category.PRESSURE,
            Category.CAMERA,
            Category.VISUAL,
            Category.SETTING,
            Category.TWIST,
        )

    fun initialWorkspace() = Workspace(defaultCategories.map { Reel("default:${it.name}", it) })
}
