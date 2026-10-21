package com.restor.gamereel.ui.presentation

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.bundled.loadBundledLibraries
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.LibraryIds
import com.restor.gamereel.model.Tag
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class TagPresentationTest {
    @Test
    fun bundledGeneratorTagsHaveSpecificDescriptions() = runTest {
        val tags =
            BuiltinCatalog.tags +
                loadBundledLibraries(emptyMap()).flatMap { it.games }.flatMap { it.tags }
        val missing =
            tags
                .filter {
                    it.sourceLibraryId != LibraryIds.FIXTURE &&
                        it.category != Category.OTHER &&
                        it.explanation.isBlank()
                }
                .map { "${it.category}: ${it.name}" }
                .distinct()
        assertTrue(missing.isEmpty(), "Missing descriptions: $missing")
        assertTrue(tags.none { it.explanation.contains("Реши, что игрок повторяет") })
    }

    @Test
    fun unknownTagsHaveNoInventedDescriptionAndImportedTextTakesPriority() {
        val unknown = Tag("custom", "Unknown mechanic", Category.CORE, "custom")
        assertEquals("", unknown.explanation)
        assertEquals(
            "Авторское описание",
            unknown.copy(name = "shooting", description = "Авторское описание").explanation,
        )
    }

    @Test
    fun curatedTagsUseArchiveDescriptions() = runTest {
        val curated =
            loadBundledLibraries(emptyMap())
                .single { it.library.id == "curated_v5" }
                .library
                .standaloneTags
        assertTrue(curated.all { it.explanation.isNotBlank() })
        assertTrue(curated.none { it.explanation.contains("Реши, что игрок повторяет") })
        val archaeology = curated.first { it.name == "archaeology" }
        assertEquals(archaeology.explanation, archaeology.copy(description = "").explanation)
        val automation = curated.first { it.name == "factory automation" }
        assertTrue(automation.description.contains("конвейеры"))
        assertEquals(automation.explanation, automation.copy(description = "").explanation)
    }

    @Test
    fun curatedTagsHaveReferenceGames() = runTest {
        val curated =
            loadBundledLibraries(emptyMap())
                .single { it.library.id == "curated_v5" }
                .library
                .standaloneTags
        // Production and business prompts describe choices, not claims about games.
        val planningCategories = setOf(Category.SCOPE, Category.PLATFORM_TRAIT, Category.MONETIZATION)
        assertTrue(curated.filter { it.category !in planningCategories }
            .all { it.referenceGames.isNotEmpty() })
        assertEquals(
            listOf("Factorio"),
            curated.first { it.name == "factory automation" }.referenceGames,
        )
        assertEquals(
            listOf("Factorio", "Satisfactory", "Schedule I"),
            Tag("x", "automation", Category.CORE, "steam_broad").referenceGames,
        )
        assertTrue(Tag("x", "automation", Category.CORE, "fixture").referenceGames.isEmpty())
    }

    @Test
    fun libraryTagsUseReviewedDescriptionsAndReferences() = runTest {
        val tags =
            loadBundledLibraries(emptyMap())
                .filter { it.library.id != "curated_v5" }
                .flatMap { it.games }
                .flatMap { it.tags }
        assertTrue(
            tags.none {
                it.category == Category.OTHER &&
                    it.name in setOf("gore", "nsfw", "classic", "utilities")
            }
        )
        assertTrue(
            tags.filter { it.category == Category.CAMERA }.all { it.explanation.isNotBlank() }
        )
        assertTrue(tags.any { it.referenceGames.size > 1 })
    }

    @Test
    fun fixtureTagsHaveLocalizedResources() {
        val keys = BuiltinCatalog.tags.map { it.id.substringAfterLast(':') }
        assertTrue(keys.all { fixtureName(it) != null && fixtureAbout(it) != null })
    }

    @Test
    fun distinctMechanicsDoNotShareCategoryPlaceholder() {
        val names =
            listOf(
                "match-3",
                "auto battler",
                "grand strategy",
                "endless running",
                "social deduction",
            )
        val descriptions = names.map { Tag(it, it, Category.CORE, "test").explanation }
        assertTrue(descriptions.all { it.isNotBlank() })
        assertEquals(names.size, descriptions.toSet().size)
    }
}
