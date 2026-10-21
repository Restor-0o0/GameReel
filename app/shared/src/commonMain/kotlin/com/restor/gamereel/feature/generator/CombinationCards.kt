package com.restor.gamereel.feature.generator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.restor.gamereel.generator.*
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.label
import gamereel.app.shared.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun rememberCombinationIndex(
    libraries: List<LibraryPack>,
    preferences: Preferences,
): CombinationIndex? {
    val selected =
        remember(libraries, preferences.disabledLibraryIds, preferences.enabledLibraryIds, preferences.activePresetId, preferences.presets) {
            val active = preferences.activePresetId
                ?.let { id -> preferences.presets.firstOrNull { it.id == id } }
            if (active != null)
                listOf(LibraryPack(Library("preset", "Preset"),
                    if (active.includeGameTags) active.games else emptyList()))
            else libraries.filter { preferences.includesLibrary(it.library.id) }
        }
    val index by
        produceState<CombinationIndex?>(null, selected) {
            value = null
            value = withContext(Dispatchers.Default) { CombinationIndex(selected) }
        }
    return index
}

@Composable
internal fun CombinationCards(reels: List<Reel>, index: CombinationIndex?) {
    val tags = remember(reels) { reels.mapNotNull { it.tag } }
    val synergies = remember(tags) { findSynergies(tags) }
    val rare = remember(tags, index) { index?.rareCombinations(tags).orEmpty() }
    if (synergies.isEmpty() && rare.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.synergy_heading),
            style = MaterialTheme.typography.titleMedium,
        )
        synergies.forEach { combo ->
            val description =
                when (combo.rule) {
                    SynergyRule.EXPEDITION -> Res.string.synergy_expedition
                    SynergyRule.SURVIVAL -> Res.string.synergy_survival
                    SynergyRule.LOADOUT -> Res.string.synergy_loadout
                    SynergyRule.HEIST -> Res.string.synergy_heist
                    SynergyRule.PHYSICS_PUZZLE -> Res.string.synergy_physics
                    SynergyRule.RUN_DECK -> Res.string.synergy_deck
                    SynergyRule.TEAM_CLOCK -> Res.string.synergy_team
                    SynergyRule.FARM_CYCLE -> Res.string.synergy_farm
                    SynergyRule.DISCOVERY -> Res.string.synergy_discovery
                    SynergyRule.RHYTHM_JUMPS -> Res.string.synergy_rhythm
                }
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        combo.tags.map { it.label() }.joinToString(" + "),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        rare.forEach { combo ->
            OutlinedCard {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        stringResource(Res.string.rare_heading),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(combo.tags.map { it.label() }.joinToString(" + "))
                    Text(
                        stringResource(
                            Res.string.rare_evidence,
                            combo.occurrences,
                            combo.corpusSize,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Text(
            stringResource(Res.string.combination_caveat),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
