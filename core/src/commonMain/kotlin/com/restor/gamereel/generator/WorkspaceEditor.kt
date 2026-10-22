package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*

enum class EditorError {
    LAST_LIBRARY,
    ALREADY_PINNED,
    EMPTY_IDEA,
    MISSING_PRESET_LIBRARY,
    TOO_MANY_REELS,
}

class EditorException(val error: EditorError) : IllegalStateException(error.name)

private fun ensure(condition: Boolean, error: EditorError) {
    if (!condition) throw EditorException(error)
}

class WorkspaceEditor(
    private val engine: GeneratorEngine,
    private val libraries: List<LibraryPack> = emptyList(),
) {
    companion object {
        const val HISTORY_LIMIT = 100
    }

    private val slots = libraries.flatMap { it.library.slots }.associateBy { it.id }

    private fun config(workspace: Workspace): GeneratorConfig {
        val preset = workspace.preferences.activePresetId
            ?.let { id -> workspace.preferences.presets.firstOrNull { it.id == id && it.isTagSet } }
        val enabledSources = (listOf(LibraryIds.FIXTURE) + libraries.map { it.library.id })
            .filter { workspace.preferences.includesLibrary(it) }.toSet()
        val customFilter = workspace.preferences.activeCustomIdeaFilterId
            ?.let { id -> workspace.preferences.customIdeaFilters.firstOrNull { it.id == id } }
        val sourceIds = customFilter?.sourceIds?.let(enabledSources::intersect) ?: enabledSources
        val selectedPool = preset?.let { selected ->
            (selected.tags +
                if (selected.includeGameTags) selected.games.flatMap { game -> game.tags } else emptyList()
            ).distinctBy { it.id }
        }
        val mode = customFilter?.baseMode ?: workspace.preferences.ideaMode
        val excludedTerms = normalizeFilterTerms(customFilter?.excludedTerms.orEmpty())
        val sourcePool = (BuiltinCatalog.tags + libraries.flatMap { it.allTags })
            .filter { it.sourceLibraryId in sourceIds }
        // Editorial prompts are an explicit opt-in for custom source restrictions.
        val editorialTwists = (BuiltinCatalog.tags + libraries.filter {
            it.library.id == LibraryIds.CURATED
        }.flatMap { it.allTags }).filter { it.category == Category.TWIST }
        val creativeTwists = if (selectedPool == null && customFilter?.includeCreativeTwists == true)
            editorialTwists
        else emptyList()
        // Materialize even an empty selection: an empty sourceIds set means
        // unrestricted sources to GeneratorEngine's legacy API.
        val allowed = { tag: Tag ->
            tag.slot == null && mode.allows(tag) && excludedTerms.none { it in tag.name.lowercase() }
        }
        val filtered = (selectedPool ?: (sourcePool + creativeTwists).distinctBy { it.id })
                .filter { tag ->
                    allowed(tag) &&
                        (customFilter?.sourceIds == null || tag.sourceLibraryId in sourceIds ||
                            tag in creativeTwists)
                }
        // categories the chosen sources never cover borrow editorial tags instead of staying empty
        val covered = filtered.map { it.category }.toSet()
        val fallback = if (selectedPool != null || sourceIds.isEmpty()) emptyList()
        else (BuiltinCatalog.tags + libraries.filter { it.library.id == LibraryIds.CURATED }.flatMap { it.allTags })
            .filter { it.category !in covered && allowed(it) }
        // pack reels ignore idea filters and tag sets, they only need their library enabled
        val packTags = libraries.filter { it.library.slots.isNotEmpty() && it.library.id in enabledSources }
            .flatMap { it.allTags }.filter { it.slot != null }
        val pool = (filtered + fallback + packTags).distinctBy { it.id }
        return GeneratorConfig(
            sourceIds = sourceIds,
            maxAutoAtomsFromSameGame = if (preset == null) 2 else Int.MAX_VALUE,
            tagPool = pool,
        )
    }

    fun missingReelCategories(workspace: Workspace): List<Category> {
        val config = config(workspace)
        val pool = config.tagPool ?: (BuiltinCatalog.tags + libraries.flatMap { it.allTags })
            .filter { it.sourceLibraryId in config.sourceIds }
        val available = pool.map { it.category }.toSet()
        return workspace.reels.asSequence()
            .filter { it.state == ReelState.RANDOM && it.slot == null && it.category !in available }
            .map { it.category }.distinct().toList()
    }

    fun spin(workspace: Workspace, onlyId: String? = null): Workspace {
        val config = config(workspace)
        return checkpoint(workspace).copy(reels = engine.spin(workspace.reels, config, onlyId))
    }

    fun undo(workspace: Workspace): Workspace {
        val sources = workspace.preferences.sourceHistory.lastOrNull()
        return if (workspace.history.isEmpty()) workspace
        else
            workspace.copy(
                reels = workspace.history.last(),
                history = workspace.history.dropLast(1),
                preferences =
                    workspace.preferences.copy(
                        disabledLibraryIds =
                            sources?.disabled ?: workspace.preferences.disabledLibraryIds,
                        enabledLibraryIds =
                            if (sources != null) sources.enabled
                            else workspace.preferences.enabledLibraryIds,
                        sourceHistory = workspace.preferences.sourceHistory.dropLast(1),
                        activePresetId = workspace.preferences.activePresetHistory.lastOrNull(),
                        activePresetHistory = workspace.preferences.activePresetHistory.dropLast(1),
                        activeVariantParentId =
                            if (workspace.preferences.activeVariantHistory.isNotEmpty())
                                workspace.preferences.activeVariantHistory.last()
                            else workspace.preferences.activeVariantParentId,
                        activeVariantHistory = workspace.preferences.activeVariantHistory.dropLast(1),
                    ),
            )
    }

    fun resetReels(workspace: Workspace): Workspace =
        checkpoint(workspace)
            .let { before -> before.copy(reels = engine.spin(BuiltinCatalog.initialWorkspace().reels, config(before)),
                preferences = before.preferences.copy(activeVariantParentId = null)) }

    private fun checkpoint(workspace: Workspace): Workspace =
        workspace.copy(
            history = (workspace.history + listOf(workspace.reels)).takeLast(HISTORY_LIMIT),
            preferences =
                workspace.preferences.copy(
                    sourceHistory =
                        (List(workspace.history.size) {
                                workspace.preferences.sourceHistory.getOrNull(it)
                            } + workspace.preferences.librarySelection)
                            .takeLast(HISTORY_LIMIT),
                    activePresetHistory =
                        (List(workspace.history.size) {
                            workspace.preferences.activePresetHistory.getOrNull(it)
                        } + workspace.preferences.activePresetId).takeLast(HISTORY_LIMIT),
                    activeVariantHistory =
                        (List(workspace.history.size) {
                            workspace.preferences.activeVariantHistory.getOrNull(it)
                        } + workspace.preferences.activeVariantParentId).takeLast(HISTORY_LIMIT),
                ),
        )

    fun setSpinButtonPosition(workspace: Workspace, position: SpinButtonPosition): Workspace =
        workspace.copy(
            preferences =
                workspace.preferences.copy(
                    spinButtonPosition =
                        SpinButtonPosition(
                            position.x.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f,
                            position.y.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f,
                        )
                )
        )

    fun savePreset(workspace: Workspace, name: String): Workspace {
        if (name.isBlank() || workspace.reels.isEmpty()) return workspace
        val reels = workspace.reels.mapIndexed { index, reel -> Reel("template:$index", reel.category, slot = reel.slot) }
        val preset = GeneratorPreset("preset:${workspace.nextId}", name.trim().take(80), reels)
        return workspace.copy(
            nextId = workspace.nextId + 1,
            preferences = workspace.preferences.copy(presets = workspace.preferences.presets + preset),
        )
    }

    fun installPack(workspace: Workspace, pack: LibraryPack): Workspace {
        val library = pack.library
        val byId = library.slots.associateBy { it.id }
        val reels = library.reels.mapNotNull(byId::get).take(ContentLimits.MAX_REELS)
        if (reels.isEmpty()) return workspace
        val presetId = "pack:${library.id}"
        val preset = GeneratorPreset(presetId, library.name.take(80),
            reels.mapIndexed { index, slot -> Reel("template:$index", Category.OTHER, slot = slot) })
        val enabled = workspace.copy(preferences = workspace.preferences.copy(
            disabledLibraryIds = workspace.preferences.disabledLibraryIds - library.id,
            enabledLibraryIds = workspace.preferences.enabledLibraryIds?.plus(library.id),
            presets = workspace.preferences.presets.filterNot { it.id == presetId } + preset,
        ))
        return applyPreset(enabled, presetId)
    }

    fun saveReelPreset(workspace: Workspace, name: String, categories: List<Category>): Workspace {
        if (name.isBlank() || categories.isEmpty()) return workspace
        ensure(categories.size <= ContentLimits.MAX_REELS, EditorError.TOO_MANY_REELS)
        val preset = GeneratorPreset(
            id = "preset:${workspace.nextId}",
            name = name.trim().take(80),
            reels = categories.mapIndexed { index, category -> Reel("template:$index", category) },
        )
        return workspace.copy(
            nextId = workspace.nextId + 1,
            preferences =
                workspace.preferences.copy(presets = workspace.preferences.presets + preset),
        )
    }

    fun deletePreset(workspace: Workspace, id: String): Workspace =
        workspace.copy(
            preferences =
                workspace.preferences.copy(
                    presets = workspace.preferences.presets.filterNot { it.id == id },
                    activePresetId = workspace.preferences.activePresetId.takeUnless { it == id },
                )
        )

    fun saveGamesPreset(workspace: Workspace, name: String, games: List<GameReference>): Workspace {
        return saveCustomPreset(workspace, name, games, emptyList(), true)
    }

    fun saveCustomPreset(
        workspace: Workspace,
        name: String,
        games: List<GameReference>,
        tags: List<Tag>,
        includeGameTags: Boolean,
    ): Workspace {
        if (name.isBlank()) return workspace
        val selected = games.distinctBy { it.id }
        require(selected.size <= ContentLimits.MAX_GAMES)
        require(selected.all { it.tags.size <= ContentLimits.MAX_TAGS })
        val selectedTags = tags.distinctBy { it.id }
        require(selectedTags.size <= ContentLimits.MAX_TAGS)
        val pool = selectedTags + if (includeGameTags) selected.flatMap { it.tags } else emptyList()
        if (pool.isEmpty()) return workspace
        val preset = GeneratorPreset(
            id = "preset:${workspace.nextId}",
            name = name.trim().take(80),
            games = selected,
            tags = selectedTags,
            includeGameTags = includeGameTags,
        )
        return workspace.copy(
            nextId = workspace.nextId + 1,
            preferences = workspace.preferences.copy(presets = workspace.preferences.presets + preset),
        )
    }

    fun importPreset(workspace: Workspace, imported: GeneratorPreset): Workspace {
        require(imported.name.isNotBlank() && imported.name.length <= 80)
        ensure(imported.reels.size <= ContentLimits.MAX_REELS, EditorError.TOO_MANY_REELS)
        require(imported.reels.isNotEmpty() || imported.tags.isNotEmpty() ||
            imported.includeGameTags && imported.games.any { it.tags.isNotEmpty() })
        require(imported.games.size <= ContentLimits.MAX_GAMES)
        require(imported.tags.size <= ContentLimits.MAX_TAGS)
        require(imported.tags.all { it.id.isNotBlank() && it.name.isNotBlank() })
        require(imported.games.isEmpty() && imported.tags.isEmpty() ||
            (imported.tags.isNotEmpty() || imported.includeGameTags && imported.games.any { it.tags.isNotEmpty() }))
        require(imported.games.all { game ->
            game.title.isNotBlank() && game.tags.size <= ContentLimits.MAX_TAGS &&
                game.tags.all { it.name.isNotBlank() && it.id.isNotBlank() }
        })
        val preset = imported.copy(id = "preset:${workspace.nextId}")
        return workspace.copy(
            nextId = workspace.nextId + 1,
            preferences = workspace.preferences.copy(presets = workspace.preferences.presets + preset),
        )
    }

    fun applyPreset(workspace: Workspace, id: String): Workspace {
        val preset =
            (PresetCatalog.templates + workspace.preferences.presets).firstOrNull { it.id == id }
                ?: return workspace
        if (!preset.isReelPreset) return workspace
        val before = checkpoint(workspace)
        val reels = preset.reels.mapIndexed { index, reel ->
            Reel(id = "reel:${workspace.nextId + index}", category = reel.category, slot = reel.slot)
        }
        return before.copy(
            reels = engine.spin(reels, config(before)),
            nextId = workspace.nextId + reels.size,
        )
    }

    fun applyTagSet(workspace: Workspace, id: String): Workspace {
        val set = workspace.preferences.presets.firstOrNull { it.id == id && it.isTagSet }
            ?: return workspace
        val before = checkpoint(workspace)
        val configured = before.copy(preferences = before.preferences.copy(activePresetId = set.id))
        return configured.copy(reels = engine.spin(configured.reels, config(configured)))
    }

    fun clearPreset(workspace: Workspace): Workspace {
        if (workspace.preferences.activePresetId == null) return workspace
        val before = checkpoint(workspace)
        val cleared = before.copy(preferences = before.preferences.copy(activePresetId = null))
        return cleared.copy(reels = engine.spin(cleared.reels, config(cleared)))
    }

    fun setAnimation(workspace: Workspace, speed: AnimationSpeed): Workspace {
        val preferences = workspace.preferences
        val duration =
            if (speed == AnimationSpeed.OFF && preferences.spinTotalDurationMs == null)
                preferences.requestedSpinDurationMs(workspace.reels.count { it.state == ReelState.RANDOM })
            else preferences.spinTotalDurationMs
        return workspace.copy(
            preferences = preferences.copy(animationSpeed = speed, spinTotalDurationMs = duration)
        )
    }

    fun setSpinTotalDuration(workspace: Workspace, durationMs: Int): Workspace =
        workspace.copy(
            preferences = workspace.preferences.copy(spinTotalDurationMs = durationMs.coerceIn(500, 30000))
        )

    fun setReelStopGap(workspace: Workspace, gapMs: Int): Workspace =
        workspace.copy(
            preferences = workspace.preferences.copy(
                spinTotalDurationMs = workspace.preferences.requestedSpinDurationMs(
                    workspace.reels.count { it.state == ReelState.RANDOM }
                ),
                reelStopGapMs = gapMs.coerceIn(0, 3000),
            )
        )

    fun setSpinTiming(workspace: Workspace, totalMs: Int, gapMs: Int): Workspace =
        workspace.copy(
            preferences = workspace.preferences.copy(
                spinTotalDurationMs = totalMs.coerceIn(500, 30000),
                reelStopGapMs = gapMs.coerceIn(0, 3000),
            )
        )

    fun setSound(workspace: Workspace, enabled: Boolean): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(soundEnabled = enabled))

    fun setLanguage(workspace: Workspace, language: AppLanguage): Workspace {
        return workspace.copy(preferences = workspace.preferences.copy(language = language))
    }

    fun setUiAccent(workspace: Workspace, accent: UiAccent): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(
            uiAccent = accent,
            customColors = workspace.preferences.customColors.copy(selectedAccent = null),
        ))

    fun setUiBackground(workspace: Workspace, background: UiBackground): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(
            uiBackground = background,
            customColors = workspace.preferences.customColors.copy(selectedBackground = null),
        ))

    fun setUiText(workspace: Workspace, text: UiText): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(
            uiText = text,
            customColors = workspace.preferences.customColors.copy(selectedText = null),
        ))

    fun saveUiColor(workspace: Workspace, role: UiColorRole, value: String): Workspace {
        val hex = normalizeHexColor(value) ?: return workspace
        val colors = workspace.preferences.customColors
        val updated = when (role) {
            UiColorRole.ACCENT -> colors.copy(
                accents = (colors.accents + hex).distinct().takeLast(24), selectedAccent = hex,
            )
            UiColorRole.BACKGROUND -> colors.copy(
                backgrounds = (colors.backgrounds + hex).distinct().takeLast(24), selectedBackground = hex,
            )
            UiColorRole.TEXT -> colors.copy(
                texts = (colors.texts + hex).distinct().takeLast(24), selectedText = hex,
            )
        }
        return workspace.copy(preferences = workspace.preferences.copy(customColors = updated))
    }

    fun removeUiColor(workspace: Workspace, role: UiColorRole, value: String): Workspace {
        val hex = normalizeHexColor(value) ?: return workspace
        val colors = workspace.preferences.customColors
        val updated = when (role) {
            UiColorRole.ACCENT -> colors.copy(
                accents = colors.accents - hex,
                selectedAccent = colors.selectedAccent.takeUnless { it == hex },
            )
            UiColorRole.BACKGROUND -> colors.copy(
                backgrounds = colors.backgrounds - hex,
                selectedBackground = colors.selectedBackground.takeUnless { it == hex },
            )
            UiColorRole.TEXT -> colors.copy(
                texts = colors.texts - hex,
                selectedText = colors.selectedText.takeUnless { it == hex },
            )
        }
        return workspace.copy(preferences = workspace.preferences.copy(customColors = updated))
    }

    fun setGeneratorView(workspace: Workspace, view: GeneratorView): Workspace {
        return workspace.copy(preferences = workspace.preferences.copy(generatorView = view))
    }

    fun setIdeaMode(workspace: Workspace, mode: IdeaMode): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(
            ideaMode = mode, activeCustomIdeaFilterId = null))

    fun saveCustomIdeaFilter(workspace: Workspace, filter: CustomIdeaFilter): Workspace {
        val name = filter.name.trim().take(80)
        val terms = normalizeFilterTerms(filter.excludedTerms)
        val sources = filter.sourceIds?.takeIf { it.isNotEmpty() }
        if (name.isBlank() ||
            (filter.baseMode == IdeaMode.OPEN && terms.isEmpty() && sources == null &&
                !filter.includeCreativeTwists)) return workspace
        val existing = workspace.preferences.customIdeaFilters.firstOrNull { it.id == filter.id }
        if (existing == null && workspace.preferences.customIdeaFilters.size >= 30) return workspace
        val id = existing?.id ?: "filter:${workspace.nextId}"
        val saved = filter.copy(id = id, name = name, excludedTerms = terms,
            sourceIds = sources)
        val filters = workspace.preferences.customIdeaFilters.filterNot { it.id == id } + saved
        return workspace.copy(
            nextId = workspace.nextId + if (existing == null) 1 else 0,
            preferences = workspace.preferences.copy(customIdeaFilters = filters,
                activeCustomIdeaFilterId = id),
        )
    }

    fun enableCreativeTwists(workspace: Workspace): Workspace {
        val activeId = workspace.preferences.activeCustomIdeaFilterId ?: return workspace
        val filters = workspace.preferences.customIdeaFilters.map { filter ->
            if (filter.id == activeId) filter.copy(includeCreativeTwists = true) else filter
        }
        if (filters == workspace.preferences.customIdeaFilters) return workspace
        return workspace.copy(preferences = workspace.preferences.copy(customIdeaFilters = filters))
    }

    fun applyCustomIdeaFilter(workspace: Workspace, id: String): Workspace =
        if (workspace.preferences.customIdeaFilters.none { it.id == id }) workspace
        else workspace.copy(preferences = workspace.preferences.copy(activeCustomIdeaFilterId = id))

    fun deleteCustomIdeaFilter(workspace: Workspace, id: String): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(
            customIdeaFilters = workspace.preferences.customIdeaFilters.filterNot { it.id == id },
            activeCustomIdeaFilterId = workspace.preferences.activeCustomIdeaFilterId
                .takeUnless { it == id },
        ))

    fun finishWelcome(workspace: Workspace): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(welcomeComplete = true))

    fun enableAllLibraries(workspace: Workspace): Workspace {
        return workspace.copy(
            preferences =
                workspace.preferences.copy(
                    disabledLibraryIds = emptySet(),
                    enabledLibraryIds = null,
                )
        )
    }

    fun toggleLibrary(workspace: Workspace, id: String): Workspace {
        val available = (listOf(LibraryIds.FIXTURE) + libraries.map { it.library.id }).toSet()
        if (id !in available) return workspace
        val selected = available.filter { workspace.preferences.includesLibrary(it) }.toSet()
        val updated = if (id in selected) selected - id else selected + id
        ensure(
            updated.isNotEmpty(),
            EditorError.LAST_LIBRARY,
        )

        return workspace.copy(
            preferences =
                workspace.preferences.copy(
                    disabledLibraryIds = available - updated,
                    enabledLibraryIds =
                        if (workspace.preferences.enabledLibraryIds == null) null else updated,
                )
        )
    }

    fun unlockFrozen(workspace: Workspace): Workspace {
        return workspace.copy(reels = engine.unlockFrozen(workspace.reels))
    }

    fun remove(workspace: Workspace, id: String): Workspace {
        return workspace.copy(reels = workspace.reels.filterNot { reel -> reel.id == id })
    }

    fun freeze(workspace: Workspace, id: String): Workspace {
        return workspace.copy(
            reels =
                workspace.reels.map {
                    if (it.id != id || it.tag == null || it.state == ReelState.PINNED) it
                    else
                        it.copy(
                            state =
                                if (it.state == ReelState.FROZEN) ReelState.RANDOM
                                else ReelState.FROZEN
                        )
                }
        )
    }

    fun pin(workspace: Workspace, id: String): Workspace {
        val reel = workspace.reels.firstOrNull { it.id == id } ?: return workspace
        val tag = reel.tag ?: return workspace
        ensure(
            workspace.reels.none {
                it.id != id && it.state == ReelState.PINNED && it.tag?.id == tag.id
            },
            EditorError.ALREADY_PINNED,
        )

        return workspace.copy(
            reels =
                workspace.reels.map {
                    if (it.id == id)
                        it.copy(
                            state =
                                if (it.state == ReelState.PINNED) ReelState.RANDOM
                                else ReelState.PINNED
                        )
                    else it
                }
        )
    }

    fun pinTag(workspace: Workspace, tag: Tag): Workspace {
        val reels = engine.pin(workspace.reels, tag, "reel:${workspace.nextId}", tag.slot?.let(slots::get))
        ensure(reels != workspace.reels, EditorError.ALREADY_PINNED)
        ensure(reels.size <= ContentLimits.MAX_REELS, EditorError.TOO_MANY_REELS)
        return workspace.copy(reels = reels, nextId = workspace.nextId + 1)
    }

    fun addReel(workspace: Workspace, category: Category, slot: ReelSlot? = null): Workspace {
        ensure(workspace.reels.size < ContentLimits.MAX_REELS, EditorError.TOO_MANY_REELS)
        val reel = Reel("reel:${workspace.nextId}", if (slot == null) category else Category.OTHER, slot = slot)
        return workspace.copy(
            reels = engine.spin(workspace.reels + reel, config(workspace), onlyId = reel.id),
            nextId = workspace.nextId + 1,
        )
    }

    fun selectTag(workspace: Workspace, id: String, tag: Tag): Workspace {
        ensure(
            workspace.reels.none {
                it.id != id && it.state == ReelState.PINNED && it.tag?.id == tag.id
            } || workspace.reels.firstOrNull { it.id == id }?.state != ReelState.PINNED,
            EditorError.ALREADY_PINNED,
        )

        val reels = workspace.reels.map {
            if (it.id != id) it
            else if (it.slot != null) it.copy(tag = tag)
            else it.copy(category = tag.category, tag = tag)
        }
        val dependents = engine.dependentsOf(reels, id)
        if (dependents.isEmpty()) return workspace.copy(reels = reels)
        val config = config(workspace)
        return workspace.copy(reels = dependents.fold(reels) { current, dependent ->
            engine.spin(current, config, dependent)
        })
    }

    fun saveIdea(workspace: Workspace, title: String, notes: String): Workspace =
        saveIdea(workspace, title, notes, IdeaConcept(), "")

    fun saveIdea(workspace: Workspace, title: String, notes: String,
                 concept: IdeaConcept, project: String): Workspace {
        ensure(workspace.reels.any { it.tag != null }, EditorError.EMPTY_IDEA)
        val parent = workspace.preferences.activeVariantParentId
        val inheritedProject = workspace.ideas.firstOrNull { it.id == parent }?.project.orEmpty()
        val idea =
            SavedIdea(
                "idea:${workspace.nextId}",
                title.trim().ifBlank { "#${workspace.ideas.size + 1}" },
                workspace.reels.toList(),
                notes,
                concept,
                project.trim().ifBlank { inheritedProject }.take(80),
                parent,
            )
        return workspace.copy(ideas = listOf(idea) + workspace.ideas, nextId = workspace.nextId + 1,
            preferences = workspace.preferences.copy(activeVariantParentId = null))
    }

    fun loadIdea(workspace: Workspace, id: String): Workspace {
        return workspace.copy(reels = workspace.ideas.first { it.id == id }.reels.toList(),
            preferences = workspace.preferences.copy(activeVariantParentId = null))
    }

    fun beginVariant(workspace: Workspace, id: String): Workspace {
        val source = workspace.ideas.first { it.id == id }
        return workspace.copy(reels = source.reels.toList(),
            preferences = workspace.preferences.copy(activeVariantParentId = source.id))
    }

    fun dailyIdea(workspace: Workspace, epochDay: Long): Workspace =
        openReels(workspace, DailyIdea.reels(epochDay, BuiltinCatalog.tags + libraries.flatMap { it.allTags }))

    fun openReels(workspace: Workspace, reels: List<Reel>): Workspace {
        ensure(reels.any { it.tag != null }, EditorError.EMPTY_IDEA)
        ensure(reels.size <= ContentLimits.MAX_REELS, EditorError.TOO_MANY_REELS)
        return checkpoint(workspace).let {
            it.copy(reels = reels, preferences = it.preferences.copy(activeVariantParentId = null))
        }
    }

    fun setIdeaOutcome(workspace: Workspace, id: String, outcome: IdeaOutcome): Workspace =
        workspace.copy(ideas = workspace.ideas.map {
            if (it.id == id) it.copy(outcome = outcome.copy(url = outcome.url.trim().take(500))) else it
        })

    fun cancelVariant(workspace: Workspace): Workspace =
        workspace.copy(preferences = workspace.preferences.copy(activeVariantParentId = null))

    fun deleteIdea(workspace: Workspace, id: String): Workspace {
        return workspace.copy(ideas = workspace.ideas.filterNot { idea -> idea.id == id })
    }

    fun editIdea(workspace: Workspace, id: String, title: String, notes: String): Workspace {
        val existing = workspace.ideas.firstOrNull { it.id == id } ?: return workspace
        return editIdea(workspace, id, title, notes, existing.concept, existing.project)
    }

    fun editIdea(workspace: Workspace, id: String, title: String, notes: String,
                 concept: IdeaConcept, project: String): Workspace {
        return workspace.copy(
            ideas =
                workspace.ideas.map {
                    if (it.id == id)
                        it.copy(title = title.trim().ifBlank { it.title }, notes = notes,
                            concept = concept, project = project.trim().take(80))
                    else it
                }
        )
    }
}
