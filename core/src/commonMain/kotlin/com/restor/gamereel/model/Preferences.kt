package com.restor.gamereel.model

import kotlinx.serialization.Serializable

enum class AnimationSpeed(val durationMs: Int) {
    OFF(0),
    FAST(480),
    NORMAL(1100),
}

enum class GeneratorView {
    CARDS,
    MACHINE,
}

enum class AppLanguage {
    SYSTEM,
    RUSSIAN,
    ENGLISH,
    SPANISH,
    FRENCH,
    GERMAN,
    PORTUGUESE,
    ITALIAN,
    POLISH,
    TURKISH,
    UKRAINIAN,
    INDONESIAN,
    JAPANESE,
    KOREAN,
    CHINESE,
}

enum class IdeaMode { OPEN, SOLO, JAM, MOBILE, OFFLINE }

enum class UiAccent { GOLD, MINT, BLUE, CORAL, LAVENDER, PINK, LIME, ORANGE }

enum class UiBackground { GRAPHITE, NAVY, CHARCOAL, PLUM, FOREST, SLATE }

enum class UiText { NEUTRAL, WARM, COOL, PURE_WHITE, SAND, LAVENDER }

enum class UiColorRole { ACCENT, BACKGROUND, TEXT }

@Serializable
data class UiCustomColors(
    val accents: List<String> = emptyList(),
    val backgrounds: List<String> = emptyList(),
    val texts: List<String> = emptyList(),
    val selectedAccent: String? = null,
    val selectedBackground: String? = null,
    val selectedText: String? = null,
)

fun normalizeHexColor(value: String): String? {
    val hex = value.trim().removePrefix("#").uppercase()
    return hex.takeIf { it.length == 6 && it.all { char -> char in '0'..'9' || char in 'A'..'F' } }
        ?.let { "#$it" }
}

@Serializable
data class Preferences(
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val generatorView: GeneratorView = GeneratorView.MACHINE,
    val disabledLibraryIds: Set<String> = emptySet(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val soundEnabled: Boolean = true,
    val spinButtonPosition: SpinButtonPosition = SpinButtonPosition(),
    val presets: List<GeneratorPreset> = emptyList(),
    val activePresetId: String? = null,
    val activePresetHistory: List<String?> = emptyList(),
    val enabledLibraryIds: Set<String>? = null,
    val sourceHistory: List<LibrarySelection?> = emptyList(),
    val uiAccent: UiAccent = UiAccent.GOLD,
    val uiBackground: UiBackground = UiBackground.GRAPHITE,
    val uiText: UiText = UiText.NEUTRAL,
    val customColors: UiCustomColors = UiCustomColors(),
    val spinTotalDurationMs: Int? = null,
    val reelStopGapMs: Int = 125,
    val ideaMode: IdeaMode = IdeaMode.OPEN,
    val activeVariantParentId: String? = null,
    val activeVariantHistory: List<String?> = emptyList(),
    val welcomeComplete: Boolean = true,
    val customIdeaFilters: List<CustomIdeaFilter> = emptyList(),
    val activeCustomIdeaFilterId: String? = null,
)

fun Preferences.requestedSpinDurationMs(reelCount: Int): Int =
    spinTotalDurationMs
        ?: ((if (animationSpeed == AnimationSpeed.FAST) AnimationSpeed.FAST.durationMs
             else AnimationSpeed.NORMAL.durationMs) +
            (reelCount - 1).coerceAtLeast(0) * reelStopGapMs)
