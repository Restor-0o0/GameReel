package com.restor.gamereel.data.community

import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.model.GeneratorPreset
import com.restor.gamereel.model.isReelPreset
import com.restor.gamereel.model.isTagSet
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class CommunityContentTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "community") }
        .first { it.isDirectory }

    private fun files(kind: String) = File(root, kind).listFiles { f -> f.extension == "json" }.orEmpty().toList()

    @Test
    fun presetsImportAsReelPresets() {
        assertTrue(files("presets").isNotEmpty())
        files("presets").forEach {
            assertTrue(json.decodeFromString<GeneratorPreset>(it.readText()).isReelPreset, it.name)
        }
    }

    @Test
    fun tagSetsImportAsTagSets() {
        files("tag-sets").forEach {
            assertTrue(json.decodeFromString<GeneratorPreset>(it.readText()).isTagSet, it.name)
        }
    }

    @Test
    fun librariesPassTheImporter() {
        files("libraries").forEach {
            val pack = JsonLibraryImporter().parse(it.readText()).pack
            assertTrue(pack.games.isNotEmpty() || pack.library.standaloneTags.isNotEmpty(), it.name)
        }
    }
}
