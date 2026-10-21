package com.restor.gamereel.presentation

import com.restor.gamereel.generator.EditorError
import com.restor.gamereel.generator.EditorException
import com.restor.gamereel.model.ImportResult
import com.restor.gamereel.model.ImportWarning
import com.restor.gamereel.model.allTags
import com.restor.gamereel.text.LocalizedException
import com.restor.gamereel.text.UiText
import com.restor.gamereel.text.uiText
import gamereel.app.shared.generated.resources.*

internal fun importedText(imported: ImportResult): UiText =
    UiText.Joined(
        listOf(
            uiText(
                Res.string.msg_imported,
                UiText.Plural(Res.plurals.games_count, imported.pack.games.size),
                UiText.Plural(Res.plurals.tags_count, imported.pack.allTags.size),
            )
        ) +
            imported.warnings.map {
                uiText(
                    when (it) {
                        ImportWarning.UNTAGGED_GAMES -> Res.string.warning_untagged
                        ImportWarning.LOCAL_IMAGES -> Res.string.warning_local_images
                    }
                )
            }
    )

internal fun Throwable.toUiText(): UiText =
    when (this) {
        is LocalizedException -> text
        is EditorException ->
            uiText(
                when (error) {
                    EditorError.LAST_LIBRARY -> Res.string.error_keep_one_library
                    EditorError.ALREADY_PINNED -> Res.string.error_already_pinned
                    EditorError.EMPTY_IDEA -> Res.string.error_empty_idea
                    EditorError.MISSING_PRESET_LIBRARY -> Res.string.error_preset_library
                    EditorError.TOO_MANY_REELS -> Res.string.error_reel_limit
                }
            )
        else -> UiText.Raw(message ?: this::class.simpleName.orEmpty())
    }
