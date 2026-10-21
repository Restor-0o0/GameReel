package com.restor.gamereel.data.bundled

import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.LibraryIds
import com.restor.gamereel.model.LibraryPack
import gamereel.app.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val bundled =
    listOf(
        Triple(LibraryIds.STEAM_BROAD, "steam_broad.json", 7),
        Triple(LibraryIds.STEAM_ALLTIME, "steam_alltime.json", 7),
        Triple(LibraryIds.GOOGLE_PLAY, "google_play.json", 7),
        Triple(LibraryIds.CURATED, "curated.json", 10),
        Triple(LibraryIds.ITCH_SELECTED, "itch_selected.json", 4),
        Triple(LibraryIds.STEAM_SELECTED, "steam_selected.json", 1),
    )

val bundledLibraryIds: Set<String> = bundled.map { it.first }.toSet()

@OptIn(ExperimentalResourceApi::class)
suspend fun loadBundledLibraries(existingVersions: Map<String, Int>): List<LibraryPack> {
    val importer = JsonLibraryImporter()
    val editorialOnly =
        Res.readBytes("files/art-policy.txt").decodeToString().trim() == "no-store-art"
    return bundled
        .filter { (id, _, _) ->
            !editorialOnly || id == LibraryIds.CURATED || id == LibraryIds.ITCH_SELECTED ||
                id == LibraryIds.STEAM_SELECTED
        }
        .filter { (id, _, version) -> (existingVersions[id] ?: 0) < version }
        .map { (id, filename, version) ->
            importer.parse(Res.readBytes("files/libraries/$filename").decodeToString()).pack.also {
                pack ->
                check(pack.library.id == id && pack.library.version == version) {
                    "bundled library version mismatch: $filename"
                }
            }
        }
}
