package com.restor.gamereel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.restor.gamereel.model.Preferences
import com.restor.gamereel.model.UiAccent
import com.restor.gamereel.model.UiBackground
import com.restor.gamereel.model.UiText
import com.restor.gamereel.model.normalizeHexColor

private val ReelColors =
    darkColorScheme(
        primary = Color(0xFFE8BE70),
        onPrimary = Color(0xFF2A2112),
        primaryContainer = Color(0xFF51432C),
        onPrimaryContainer = Color(0xFFFFE4AA),
        secondary = Color(0xFF91C9C0),
        onSecondary = Color(0xFF1A2A3A),
        secondaryContainer = Color(0xFF50432F),
        onSecondaryContainer = Color(0xFFFFE4AA),
        background = Color(0xFF101318),
        onBackground = Color(0xFFE8EAF0),
        surface = Color(0xFF191E25),
        onSurface = Color(0xFFE8EAF0),
        surfaceVariant = Color(0xFF252C35),
        onSurfaceVariant = Color(0xFFADB4C2),
        tertiary = Color(0xFFF0CA7A),
        tertiaryContainer = Color(0xFF50432F),
        onTertiaryContainer = Color(0xFFFFE4AA),
        outline = Color(0xFF49525E),
    )

private val ReelTypography = Typography().let { defaults ->
    Typography(
        headlineMedium = defaults.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
        headlineSmall = defaults.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = defaults.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleMedium = defaults.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = defaults.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = defaults.bodyMedium.copy(lineHeight = 22.sp),
        labelLarge = defaults.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = defaults.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

internal object ReelPalette {
    val pinnedCard = Color(0xFF2A2930)
    val gold = Color(0xFFF0CA7A)
    val onGold = Color(0xFF29211A)
    val machineTop = Color(0xFF303541)
    val machineBottom = Color(0xFF1B202A)
    val machineEdge = Color(0xFF0C0F15)
    val machineHighlight = Color(0xFF596171)
    val controlTop = Color(0xFF4A4858)
    val controlBottom = Color(0xFF292A34)
    val slotWindow = Color(0xFF10151C)
    val slotInset = Color(0xFF080B10)
    val slotBorder = Color(0xFF4D4C50)
    val slotShade = Color(0xDD14191E)
    val lever = Color(0xFF7C7D8B)
    val leverPivot = Color(0xFF3F424F)
    val knob = Color(0xFFFF727D)
    val knobOff = Color.Gray
}

internal fun UiAccent.swatch(): Color = when (this) {
    UiAccent.GOLD -> Color(0xFFF0CA7A)
    UiAccent.MINT -> Color(0xFF81D6B5)
    UiAccent.BLUE -> Color(0xFF9ABEF5)
    UiAccent.CORAL -> Color(0xFFFFA8A1)
    UiAccent.LAVENDER -> Color(0xFFC5AEFF)
    UiAccent.PINK -> Color(0xFFFF9CCB)
    UiAccent.LIME -> Color(0xFFC8E889)
    UiAccent.ORANGE -> Color(0xFFFFB471)
}

internal fun UiBackground.swatch(): Color = when (this) {
    UiBackground.GRAPHITE -> Color(0xFF0F1218)
    UiBackground.NAVY -> Color(0xFF0B1423)
    UiBackground.CHARCOAL -> Color(0xFF151515)
    UiBackground.PLUM -> Color(0xFF201723)
    UiBackground.FOREST -> Color(0xFF101C19)
    UiBackground.SLATE -> Color(0xFF202838)
}

internal fun UiText.swatch(): Color = when (this) {
    UiText.NEUTRAL -> Color(0xFFE8EAF0)
    UiText.WARM -> Color(0xFFFFF0D8)
    UiText.COOL -> Color(0xFFDDEEFF)
    UiText.PURE_WHITE -> Color(0xFFFFFFFF)
    UiText.SAND -> Color(0xFFE8D9B7)
    UiText.LAVENDER -> Color(0xFFE8DFFF)
}

internal fun colorFromHex(value: String?): Color? =
    value?.let(::normalizeHexColor)?.removePrefix("#")?.toLongOrNull(16)
        ?.let { Color(0xFF000000 or it) }

@Composable
internal fun GameReelTheme(preferences: Preferences?, content: @Composable () -> Unit) {
    val accent = colorFromHex(preferences?.customColors?.selectedAccent)
        ?: (preferences?.uiAccent ?: UiAccent.GOLD).swatch()
    val background = colorFromHex(preferences?.customColors?.selectedBackground)
        ?: (preferences?.uiBackground ?: UiBackground.GRAPHITE).swatch()
    val text = colorFromHex(preferences?.customColors?.selectedText)
        ?: (preferences?.uiText ?: UiText.NEUTRAL).swatch()
    val surface = lerp(background, Color.White, 0.045f)
    val surfaceVariant = lerp(background, Color.White, 0.105f)
    val accentContainer = lerp(background, accent, 0.19f)
    val scheme = ReelColors.copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.42f) Color(0xFF17202A) else Color.White,
        primaryContainer = accentContainer,
        onPrimaryContainer = accent,
        secondary = accent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = accent,
        tertiary = accent,
        tertiaryContainer = accentContainer,
        onTertiaryContainer = accent,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = text.copy(alpha = 0.72f),
        outline = lerp(background, text, 0.29f),
        outlineVariant = lerp(background, text, 0.15f),
    )
    MaterialTheme(colorScheme = scheme, typography = ReelTypography, content = content)
}
