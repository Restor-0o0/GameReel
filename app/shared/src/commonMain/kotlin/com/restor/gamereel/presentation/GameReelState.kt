package com.restor.gamereel.presentation

import com.restor.gamereel.model.*
import com.restor.gamereel.text.UiText

data class GameReelState(
    val workspace: Workspace? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val message: UiText? = null,
    val libraries: List<LibraryPack> = emptyList(),
    val spinRevision: Long = 0,
    val animatedReelIds: Set<String> = emptySet(),
    val importing: Boolean = false,
)
