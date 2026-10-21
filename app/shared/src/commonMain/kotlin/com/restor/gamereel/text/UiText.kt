package com.restor.gamereel.text

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface UiText {
    data class Str(val id: StringResource, val args: List<Any> = emptyList()) : UiText

    data class Plural(val id: PluralStringResource, val count: Int) : UiText

    data class Raw(val value: String) : UiText

    data class Joined(val parts: List<UiText>) : UiText
}

fun uiText(id: StringResource, vararg args: Any): UiText = UiText.Str(id, args.toList())

class LocalizedException(val text: UiText) : IllegalArgumentException(text.toString())

fun fail(id: StringResource, vararg args: Any): Nothing =
    throw LocalizedException(UiText.Str(id, args.toList()))

@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Str ->
            stringResource(id, *args.map { if (it is UiText) it.resolve() else it }.toTypedArray())
        is UiText.Plural -> pluralStringResource(id, count, count)
        is UiText.Raw -> value
        is UiText.Joined -> parts.map { it.resolve() }.joinToString(" ")
    }
