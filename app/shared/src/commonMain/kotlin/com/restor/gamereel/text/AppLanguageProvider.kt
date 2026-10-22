// Compose Resources 1.12.1 exposes environment reads but keeps the provider internal.
// Keep this version-specific bridge here; do not change the process/system locale.
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.restor.gamereel.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.restor.gamereel.model.AppLanguage
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ScriptQualifier
import org.jetbrains.compose.resources.rememberResourceEnvironment

@Composable
internal fun AppLanguageProvider(language: AppLanguage, content: @Composable () -> Unit) {
    val system = rememberResourceEnvironment()
    val environment = remember(system, language) { localizedEnvironment(system, language) }
    val provider =
        remember(environment) {
            object : ComposeEnvironment {
                @Composable override fun rememberEnvironment(): ResourceEnvironment = environment
            }
        }

    CompositionLocalProvider(LocalComposeEnvironment provides provider, content = content)
}

internal fun localizedEnvironment(
    system: ResourceEnvironment,
    language: AppLanguage,
): ResourceEnvironment {
    if (language == AppLanguage.SYSTEM) return system

    return ResourceEnvironment(
        LanguageQualifier(when (language) {
            AppLanguage.RUSSIAN -> "ru"
            AppLanguage.SPANISH -> "es"
            AppLanguage.FRENCH -> "fr"
            AppLanguage.GERMAN -> "de"
            AppLanguage.PORTUGUESE -> "pt"
            AppLanguage.ITALIAN -> "it"
            AppLanguage.POLISH -> "pl"
            AppLanguage.TURKISH -> "tr"
            AppLanguage.UKRAINIAN -> "uk"
            AppLanguage.INDONESIAN -> "id"
            AppLanguage.JAPANESE -> "ja"
            AppLanguage.KOREAN -> "ko"
            AppLanguage.CHINESE -> "zh"
            AppLanguage.ENGLISH, AppLanguage.SYSTEM -> "en"
        }),
        ScriptQualifier(""),
        RegionQualifier(""),
        system.theme,
        system.density,
    )
}
