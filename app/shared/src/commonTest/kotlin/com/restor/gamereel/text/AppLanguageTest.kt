package com.restor.gamereel.text

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.storage.BrowserWorkspaceRepository
import com.restor.gamereel.data.storage.workspaceJson
import com.restor.gamereel.model.AppLanguage
import com.restor.gamereel.model.Preferences
import com.restor.gamereel.ui.presentation.loadLabel
import gamereel.app.shared.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

class AppLanguageTest {
    @Test
    fun explicitLanguagesResolveIndependentlyOfTheSystem() = runTest {
        val system = getSystemResourceEnvironment()
        val english = localizedEnvironment(system, AppLanguage.ENGLISH)
        val russian = localizedEnvironment(system, AppLanguage.RUSSIAN)
        val spanish = localizedEnvironment(system, AppLanguage.SPANISH)
        val french = localizedEnvironment(system, AppLanguage.FRENCH)
        val german = localizedEnvironment(system, AppLanguage.GERMAN)

        assertEquals("Settings", getString(english, Res.string.nav_settings))
        assertEquals("Опции", getString(russian, Res.string.nav_settings))
        assertEquals("Ajustes", getString(spanish, Res.string.nav_settings))
        assertEquals("Options", getString(french, Res.string.nav_settings))
        assertEquals("Optionen", getString(german, Res.string.nav_settings))
        assertEquals("es", getString(spanish, Res.string.language_code))
        assertEquals("fr", getString(french, Res.string.language_code))
        assertEquals("de", getString(german, Res.string.language_code))
        val added = mapOf(
            AppLanguage.PORTUGUESE to "pt",
            AppLanguage.ITALIAN to "it",
            AppLanguage.POLISH to "pl",
            AppLanguage.TURKISH to "tr",
            AppLanguage.UKRAINIAN to "uk",
            AppLanguage.INDONESIAN to "id",
            AppLanguage.JAPANESE to "ja",
            AppLanguage.KOREAN to "ko",
            AppLanguage.CHINESE to "zh",
        ).map { (language, code) ->
            val environment = localizedEnvironment(system, language)
            assertEquals(code, getString(environment, Res.string.language_code))
            assertNotEquals(
                getString(english, Res.string.nav_settings),
                getString(environment, Res.string.nav_settings),
            )
            environment
        }
        assertEquals("2 tags", getPluralString(english, Res.plurals.tags_count, 2, 2))
        assertEquals("2 тега", getPluralString(russian, Res.plurals.tags_count, 2, 2))
        assertEquals("2 etiquetas", getPluralString(spanish, Res.plurals.tags_count, 2, 2))
        assertEquals("2 tags", getPluralString(french, Res.plurals.tags_count, 2, 2))
        assertEquals("2 Tags", getPluralString(german, Res.plurals.tags_count, 2, 2))
        assertNotEquals(
            getString(english, Res.string.fixture_destruction_about),
            getString(russian, Res.string.fixture_destruction_about),
        )
        assertEquals(
            getString(english, Res.string.fixture_destruction),
            BuiltinCatalog.tags.first().loadLabel(english),
        )
        assertEquals(
            getString(russian, Res.string.fixture_destruction),
            BuiltinCatalog.tags.first().loadLabel(russian),
        )
        (listOf(spanish, french, german) + added).forEach { environment ->
            assertEquals(
                getString(environment, Res.string.fixture_destruction),
                BuiltinCatalog.tags.first().loadLabel(environment),
            )
        }
        assertSame(system, localizedEnvironment(system, AppLanguage.SYSTEM))
    }

    @Test
    fun olderPreferencesDefaultToTheSystemLanguage() {
        val preferences =
            workspaceJson.decodeFromString<Preferences>(
                """{"animationSpeed":"FAST","generatorView":"CARDS","disabledLibraryIds":["custom"]}"""
            )
        assertEquals(AppLanguage.SYSTEM, preferences.language)
        assertEquals(setOf("custom"), preferences.disabledLibraryIds)
    }

    @Test
    fun selectedLanguageSurvivesRepositoryRecreation() {
        var storage: String? = null
        val initial = BuiltinCatalog.initialWorkspace()
        listOf(AppLanguage.ENGLISH, AppLanguage.SPANISH, AppLanguage.FRENCH, AppLanguage.GERMAN)
            .forEach { language ->
                val workspace = initial.copy(preferences = initial.preferences.copy(language = language))
                BrowserWorkspaceRepository({ storage }, { storage = it }).save(workspace)
                val reopened = BrowserWorkspaceRepository({ storage }, { storage = it })
                assertEquals(workspace, reopened.load())
            }
    }
}
