package com.restor.gamereel.feature.generator

import com.restor.gamereel.model.GeneratorView
import com.restor.gamereel.model.SpinButtonPosition
import com.restor.gamereel.model.GeneratorPreset
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.CustomIdeaFilter

internal data class GeneratorActions(
    val spin: () -> Unit,
    val undo: () -> Unit,
    val resetReels: () -> Unit,
    val unlockFrozen: () -> Unit,
    val reroll: (String) -> Unit,
    val freeze: (String) -> Unit,
    val pin: (String) -> Unit,
    val remove: (String) -> Unit,
    val setGeneratorView: (GeneratorView) -> Unit,
    val setSpinButtonPosition: (SpinButtonPosition) -> Unit,
    val savePreset: (String) -> Unit,
    val saveReelPreset: (String, List<Category>) -> Unit,
    val applyPreset: (String) -> Unit,
    val deletePreset: (String) -> Unit,
    val importPreset: (GeneratorPreset) -> Unit,
    val saveCustomIdeaFilter: (CustomIdeaFilter) -> Unit,
    val applyCustomIdeaFilter: (String) -> Unit,
    val deleteCustomIdeaFilter: (String) -> Unit,
    val clearTagSet: () -> Unit,
    val enableAllLibraries: () -> Unit,
    val enableCreativeTwistsAndSpin: () -> Unit,
    val dailyIdea: () -> Unit,
    val openShared: (String) -> Boolean,
    val linkCopied: () -> Unit,
)
