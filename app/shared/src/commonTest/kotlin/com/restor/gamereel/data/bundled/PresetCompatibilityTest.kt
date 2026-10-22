package com.restor.gamereel.data.bundled

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.generator.*
import com.restor.gamereel.model.*
import gamereel.app.shared.generated.resources.Res
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*

class PresetCompatibilityTest {
    @Test
    fun builtinLayoutsHaveVarietyInEveryModeAcrossDefaultCatalogs() = runTest {
        val packs = loadBundledLibraries(emptyMap())
        val publicIds = setOf(LibraryIds.CURATED, LibraryIds.ITCH_SELECTED, LibraryIds.STEAM_SELECTED)
        val selections = (listOf(
            packs.map { it.library.id }.toSet() + LibraryIds.FIXTURE,
            publicIds,
            setOf(LibraryIds.CURATED),
        ) + packs.map { setOf(LibraryIds.CURATED, it.library.id) }).distinct()
        val layouts = PresetCatalog.templates + GeneratorPreset(
            "initial", "Initial", BuiltinCatalog.initialWorkspace().reels,
        )
        val tags = BuiltinCatalog.tags + packs.flatMap { it.allTags }
        val random = Random(20261015)
        val editor = WorkspaceEditor(GeneratorEngine(tags, object : RandomProvider {
            override fun nextInt(bound: Int) = random.nextInt(bound)
        }), packs)

        for (sources in selections) for (mode in IdeaMode.entries) for (layout in layouts) {
            val context = "${layout.id}, $mode, $sources"
            val initial = Workspace(layout.reels, preferences = Preferences(
                enabledLibraryIds = sources, ideaMode = mode,
            ))
            var workspace = if (layout.id == "initial") initial else editor.applyPreset(initial, layout.id)
            assertTrue(editor.missingReelCategories(workspace).isEmpty(), context)
            for (category in layout.reels.map { it.category }) {
                val available = tags.filter {
                    it.sourceLibraryId in sources && it.category == category && mode.allows(it)
                }.map { it.name.lowercase() }.toSet()
                assertTrue(available.size >= 2, "$context: no variety for $category")
            }
            repeat(5) {
                workspace = editor.spin(workspace)
                for (reel in workspace.reels) {
                    val tag = assertNotNull(reel.tag, "$context: ${reel.category}")
                    assertEquals(reel.category, tag.category, context)
                    assertTrue(tag.sourceLibraryId in sources, context)
                    assertTrue(mode.allows(tag), context)
                }
            }
        }
    }

    @Test
    fun everySourceSelectionFillsEveryBuiltinLayout() = runTest(timeout = 3.minutes) {
        val packs = loadBundledLibraries(emptyMap())
        val ids = listOf(LibraryIds.FIXTURE) + packs.map { it.library.id }
        val tags = BuiltinCatalog.tags + packs.flatMap { it.allTags }
        val random = Random(20261022)
        val editor = WorkspaceEditor(GeneratorEngine(tags, object : RandomProvider {
            override fun nextInt(bound: Int) = random.nextInt(bound)
        }), packs)
        val layouts = PresetCatalog.templates.map { it.id } + "initial"
        val selections = ids.map { setOf(it) } + setOf((ids - LibraryIds.CURATED).toSet()) + setOf(ids.toSet())
        for (sources in selections) {
            for (mode in IdeaMode.entries) for (layout in layouts) {
                val initial = Workspace(BuiltinCatalog.initialWorkspace().reels,
                    preferences = Preferences(enabledLibraryIds = sources, ideaMode = mode))
                val workspace = if (layout == "initial") editor.spin(initial) else editor.applyPreset(initial, layout)
                val context = "$layout, $mode, $sources"
                assertTrue(editor.missingReelCategories(workspace).isEmpty(), context)
                workspace.reels.forEach { assertNotNull(it.tag, "$context: ${it.category}") }
            }
        }
    }

    @Test
    fun editorialCatalogCoversEveryNamedCategoryWithoutDuplicateOrPlaceholderTags() = runTest {
        // Inspect raw data too: the importer deliberately merges duplicate identities.
        val root = Json.parseToJsonElement(Res.readBytes("files/libraries/curated.json")
            .decodeToString()).jsonObject
        val tags = root.getValue("library").jsonObject.getValue("standaloneTags").jsonArray
        val keys = tags.map {
            val tag = it.jsonObject
            assertTrue(tag.getValue("description").jsonPrimitive.content.isNotBlank())
            val name = tag.getValue("name").jsonPrimitive.content.trim().lowercase()
            assertTrue(name.isNotEmpty() && "infer from" !in name && !name.startsWith("unspecified"))
            tag.getValue("category").jsonPrimitive.content to name
        }
        assertEquals(keys.size, keys.toSet().size)
        // OTHER is intentionally reserved for unclassified source metadata.
        assertEquals(Category.entries.filter { it != Category.OTHER }.map { it.name }.toSet(),
            keys.map { it.first }.toSet())
    }
}
