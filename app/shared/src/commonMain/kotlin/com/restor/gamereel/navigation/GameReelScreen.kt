package com.restor.gamereel.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.feature.generator.*
import com.restor.gamereel.feature.ideas.*
import com.restor.gamereel.feature.library.*
import com.restor.gamereel.feature.market.*
import com.restor.gamereel.feature.search.*
import com.restor.gamereel.feature.settings.*
import com.restor.gamereel.model.*
import com.restor.gamereel.presentation.*
import com.restor.gamereel.text.resolve
import com.restor.gamereel.ui.components.*
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val WideLayout = 840.dp

@Composable
internal fun GameReelScreen(state: GameReelState, model: GameReelViewModel) {
    var page by remember { mutableStateOf(Destination.GENERATOR) }
    var returnPage by remember { mutableStateOf(Destination.GENERATOR) }
    val openSettings = { returnPage = if (page == Destination.SETTINGS) returnPage else page; page = Destination.SETTINGS }
    var addReel by remember { mutableStateOf(false) }
    var saveIdea by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf<Reel?>(null) }
    var editing by remember { mutableStateOf<SavedIdea?>(null) }
    var welcomeOpen by remember { mutableStateOf(false) }
    var welcomeDismissed by remember { mutableStateOf(false) }
    val motionWorkspace = state.workspace
    val motionPreferences = motionWorkspace?.preferences
    val randomReelCount = motionWorkspace?.reels?.count { it.state == ReelState.RANDOM } ?: 0
    val motion =
        rememberSpinMotion(
            state.spinRevision,
            state.animatedReelIds,
            motionPreferences?.animationSpeed ?: AnimationSpeed.OFF,
            motionPreferences?.requestedSpinDurationMs(randomReelCount)
                ?: AnimationSpeed.NORMAL.durationMs,
            motionPreferences?.reelStopGapMs ?: SpinTimeline.DefaultStopGapMs,
            motionWorkspace?.reels.orEmpty(),
            motionPreferences?.soundEnabled == true && page == Destination.GENERATOR,
        )
    val gameNames =
        remember(state.libraries, state.workspace?.preferences?.activePresetId) {
            val activeGames = state.workspace?.preferences?.activePresetId
                ?.let { id -> state.workspace.preferences.presets.firstOrNull { it.id == id } }
                ?.games.orEmpty()
            (state.libraries.flatMap { it.games } + activeGames).associate { it.id to it.title }
        }
    val actions =
        remember(model) {
            GeneratorActions(
                spin = model::spin,
                undo = model::undo,
                resetReels = model::resetReels,
                unlockFrozen = model::unlockFrozen,
                reroll = model::reroll,
                freeze = model::freeze,
                pin = model::pin,
                remove = model::remove,
                setGeneratorView = model::setGeneratorView,
                setSpinButtonPosition = model::setSpinButtonPosition,
                savePreset = model::savePreset,
                saveReelPreset = model::saveReelPreset,
                applyPreset = model::applyPreset,
                deletePreset = model::deletePreset,
                importPreset = model::importPreset,
                saveCustomIdeaFilter = model::saveCustomIdeaFilter,
                applyCustomIdeaFilter = model::applyCustomIdeaFilter,
                deleteCustomIdeaFilter = model::deleteCustomIdeaFilter,
                clearTagSet = model::clearPreset,
                enableAllLibraries = model::enableAllLibrariesAndSpin,
                enableCreativeTwistsAndSpin = model::enableCreativeTwistsAndSpin,
                dailyIdea = model::dailyIdea,
                openShared = model::openSharedIdea,
                linkCopied = { model.showMessage(Res.string.msg_link_copied) },
                openSettings = openSettings,
            )
        }
    val allTags =
        remember(state.libraries) { BuiltinCatalog.tags + state.libraries.flatMap { it.allTags } }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
    ) {
        val wide = maxWidth >= WideLayout
        Row(Modifier.fillMaxSize()) {
            if (wide)
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    Destination.tabs.forEach { destination ->
                        NavigationRailItem(
                            selected = page == destination,
                            onClick = { page = destination },
                            icon = { DestinationIcon(destination) },
                            label = { Text(stringResource(destination.title)) },
                        )
                    }
                }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (page != Destination.GENERATOR)
                    ScreenHeader(stringResource(page.title)) {
                        if (page == Destination.SETTINGS)
                            LinkAction(stringResource(Res.string.close), { page = returnPage })
                        else SettingsButton(openSettings)
                    }

                if (state.loading || state.importing)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.loading)
                    Text(
                        stringResource(Res.string.loading_libraries),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                state.error?.let {
                    Text(
                        it.resolve(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.message?.let { message ->
                    Row(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            message.resolve(),
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        TextButton(onClick = model::dismissMessage) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                }

                val workspace = state.workspace
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (workspace != null)
                        when (page) {
                            Destination.GENERATOR ->
                                GeneratorContent(
                                    workspace,
                                    wide,
                                    actions,
                                    gameNames,
                                    motion,
                                    { addReel = true },
                                    { saveIdea = true },
                                    model::setIdeaMode,
                                    model::cancelVariant,
                                    { selecting = it },
                                    state.libraries,
                                    model.missingReelCategories(workspace),
                                )
                            Destination.SEARCH ->
                                ReferenceSearch(
                                    state.libraries,
                                    allTags,
                                    workspace.reels
                                        .filter { it.state == ReelState.PINNED }
                                        .mapNotNull { it.tag?.id }
                                        .toSet(),
                                    model::pinTag,
                                )
                            Destination.LIBRARIES ->
                                LibrariesContent(
                                    workspace,
                                    state.libraries,
                                    (listOf(LibraryIds.FIXTURE) +
                                            state.libraries.map { it.library.id })
                                        .filterNot { workspace.preferences.includesLibrary(it) }
                                        .toSet(),
                                    state.importing,
                                    model::toggleLibrary,
                                    model::enableAllLibraries,
                                    model::importLibrary,
                                    model::saveCustomPreset,
                                    model::applyTagSet,
                                    model::clearPreset,
                                    model::deletePreset,
                                    model::importPreset,
                                    model::savePersonalGame,
                                    model::deletePersonalGame,
                                    model::importPersonalGames,
                                )
                            Destination.IDEAS ->
                                SavedContent(
                                    workspace.ideas,
                                    {
                                        model.loadIdea(it)
                                        page = Destination.GENERATOR
                                    },
                                    { editing = it },
                                    {
                                        model.beginVariant(it)
                                        page = Destination.GENERATOR
                                    },
                                    model::deleteIdea,
                                    { page = Destination.GENERATOR },
                                    { model.showMessage(Res.string.msg_link_copied) },
                                )
                            Destination.MARKET -> MarketContent()
                            Destination.SETTINGS ->
                                PreferencesContent(
                                    workspace,
                                    state.libraries,
                                    workspace.reels.count { it.state == ReelState.RANDOM },
                                    model::setAnimation,
                                    model::setGeneratorView,
                                    model::setLanguage,
                                    model::setSound,
                                    model::setUiAccent,
                                    model::setUiBackground,
                                    model::setUiText,
                                    model::saveUiColor,
                                    model::removeUiColor,
                                    model::setSpinTiming,
                                    model::restoreBackup,
                                    { welcomeDismissed = false; welcomeOpen = true },
                                )
                        }
                }

                if (!wide)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        Destination.tabs.forEach { destination ->
                            NavigationBarItem(
                                selected = page == destination,
                                onClick = { page = destination },
                                icon = { DestinationIcon(destination) },
                                label = {
                                    Text(
                                        stringResource(destination.title),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
            }
        }
    }

    if (addReel)
        AlertDialog(
            onDismissRequest = { addReel = false },
            title = { Text(stringResource(Res.string.add_reel)) },
            text = {
                val packSlots = state.libraries
                    .filter { state.workspace?.preferences?.includesLibrary(it.library.id) == true }
                    .flatMap { it.library.slots }
                LazyColumn {
                    items(packSlots, key = { it.id }) { slot ->
                        TextButton(
                            onClick = {
                                model.addReel(slot)
                                addReel = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${slot.icon.ifBlank { "🎲" }} ${slot.name}")
                        }
                    }
                    items(Category.entries.toList()) { category ->
                        TextButton(
                            onClick = {
                                model.addReel(category)
                                addReel = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(category.title))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addReel = false }) { Text(stringResource(Res.string.close)) }
            },
        )
    if (saveIdea) {
        val fallback =
            stringResource(Res.string.idea_default_title, (state.workspace?.ideas?.size ?: 0) + 1)
        val defaultProject = state.workspace?.preferences?.activeVariantParentId
            ?.let { id -> state.workspace.ideas.firstOrNull { it.id == id }?.project }.orEmpty()
        IdeaDialog(null, defaultProject, { saveIdea = false }) { title, notes, concept, project, _ ->
            model.saveIdea(title.ifBlank { fallback }, notes, concept, project).await()
        }
    }
    editing?.let { idea ->
        IdeaDialog(idea, onDismiss = { editing = null }) { title, notes, concept, project, outcome ->
            model.editIdea(idea.id, title, notes, concept, project, outcome).await()
        }
    }
    if ((state.workspace?.preferences?.welcomeComplete == false && !welcomeDismissed) || welcomeOpen)
        WelcomeDialog(onClose = {
            welcomeOpen = false
            welcomeDismissed = true
            model.finishWelcome()
        })
    selecting?.let { reel ->
        val parentTag = reel.slot?.parent?.let { parent ->
            state.workspace?.reels?.firstOrNull { it.slot?.id == parent }?.tag
        }
        val options =
            remember(allTags, reel.kind, parentTag) {
                allTags
                    .filter { it.kind == reel.kind }
                    .filter { parentTag == null || it.requires.isEmpty() || parentTag.id in it.requires }
                    .distinctBy { it.name.lowercase() }
            }
        AlertDialog(
            onDismissRequest = { selecting = null },
            title = {
                Text(
                    stringResource(Res.string.pick_value_title, reel.title())
                )
            },
            text = {
                LazyColumn {
                    items(options, key = { it.id }) { tag ->
                        TextButton(
                            onClick = {
                                model.selectTag(reel.id, tag)
                                selecting = null
                            }
                        ) {
                            Text(tag.label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selecting = null }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }
}

@Composable
private fun DestinationIcon(destination: Destination) {
    Text(
        destination.symbol,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
    )
}
