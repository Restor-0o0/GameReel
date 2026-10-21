package com.restor.gamereel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.data.bundled.bundledLibraryIds
import com.restor.gamereel.data.importer.JsonLibraryImporter
import com.restor.gamereel.data.personal.GameTagDraft
import com.restor.gamereel.data.personal.PersonalGames
import com.restor.gamereel.data.storage.WorkspaceBackup
import com.restor.gamereel.generator.GeneratorEngine
import com.restor.gamereel.generator.KotlinRandomProvider
import com.restor.gamereel.generator.WorkspaceEditor
import com.restor.gamereel.model.*
import com.restor.gamereel.repository.WorkspaceRepository
import com.restor.gamereel.text.fail
import com.restor.gamereel.text.uiText
import gamereel.app.shared.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

// publish state only after save went through
class GameReelViewModel(
    private val repositoryFactory: () -> WorkspaceRepository,
    workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val bundledLoader: suspend (Map<String, Int>) -> List<LibraryPack> = { emptyList() },
) : ViewModel() {
    private var engine = GeneratorEngine(BuiltinCatalog.tags, KotlinRandomProvider())
    private var editor = WorkspaceEditor(engine)

    private val mutableState = MutableStateFlow(GameReelState())
    val state = mutableState.asStateFlow()

    private data class Mutation(
        val message: StringResource? = null,
        val animate: Boolean = false,
        val onlyId: String? = null,
        val importText: (suspend () -> String)? = null,
        val personalOnly: Boolean = false,
        val personalUpdate: ((LibraryPack?) -> LibraryPack)? = null,
        val restoreBackup: WorkspaceBackup? = null,
        val completion: CompletableDeferred<com.restor.gamereel.text.UiText?>? = null,
        val transform: (Workspace) -> Workspace = { it },
    )

    private val mutations = Channel<Mutation>(Channel.UNLIMITED)

    init {
        viewModelScope.launch(workerDispatcher) {
            var repository: WorkspaceRepository? = null

            try {
                repository = repositoryFactory()
                var libraries = repository.loadLibraries()
                val bundled =
                    bundledLoader(libraries.associate { it.library.id to it.library.version })
                bundled.forEach(repository::importBundledLibrary)
                libraries =
                    libraries.filterNot { existing ->
                        bundled.any { it.library.id == existing.library.id }
                    } + bundled

                engine =
                    GeneratorEngine(
                        BuiltinCatalog.tags + libraries.flatMap { it.allTags },
                        KotlinRandomProvider(),
                    )
                editor = WorkspaceEditor(engine, libraries)

                val initial =
                    repository.load()
                        ?: BuiltinCatalog.initialWorkspace().let {
                            it.copy(reels = engine.spin(it.reels),
                                preferences = it.preferences.copy(welcomeComplete = false))
                                .also(repository::save)
                        }
                mutableState.value =
                    GameReelState(workspace = initial, loading = false, libraries = libraries)

                for (mutation in mutations) {
                    val before = mutableState.value.workspace ?: continue

                    try {
                        if (mutation.restoreBackup != null) {
                            val backup = mutation.restoreBackup
                            backup.validate()
                            val restoredIds = backup.customLibraries.map { it.library.id }.toSet()
                            val retainedGameIds = libraries.filterNot {
                                it.library.id in restoredIds
                            }.flatMap { it.games }.map { it.id }.toSet()
                            require(backup.customLibraries.none { pack ->
                                pack.games.any { it.id in retainedGameIds }
                            })
                            repository.saveWithLibraries(backup.workspace, backup.customLibraries)
                            libraries = libraries.filterNot { it.library.id in restoredIds } + backup.customLibraries
                            engine = GeneratorEngine(
                                BuiltinCatalog.tags + libraries.flatMap { it.allTags },
                                KotlinRandomProvider(),
                            )
                            editor = WorkspaceEditor(engine, libraries)
                            mutableState.value = mutableState.value.copy(
                                workspace = backup.workspace, libraries = libraries,
                                importing = false, error = null,
                                message = uiText(Res.string.backup_restored),
                            )
                            continue
                        }
                        if (mutation.importText != null) {
                            mutableState.value =
                                mutableState.value.copy(
                                    importing = true,
                                    error = null,
                                    message = null,
                                )

                            val imported = JsonLibraryImporter().parse(mutation.importText.invoke())
                            if (imported.pack.library.id in bundledLibraryIds)
                                fail(Res.string.import_reserved_library)
                            if (mutation.personalOnly && imported.pack.library.id != LibraryIds.PERSONAL)
                                fail(Res.string.error_not_personal_games)
                            val existing =
                                mutableState.value.libraries.firstOrNull {
                                    it.library.id == imported.pack.library.id
                                }
                            if (
                                imported.pack.library.id != LibraryIds.PERSONAL &&
                                existing != null &&
                                    imported.pack.library.version < existing.library.version
                            ) {
                                fail(Res.string.error_older_version)
                            }

                            val afterWorkspace = if (imported.pack.library.id == LibraryIds.PERSONAL)
                                before.withPersonalLibraryEnabled() else before
                            val replacement = if (imported.pack.library.id == LibraryIds.PERSONAL)
                                    PersonalGames.merge(existing, imported.pack)
                                else imported.pack
                            repository.saveWithLibraries(afterWorkspace, listOf(replacement))
                            libraries = libraries.filterNot { it.library.id == replacement.library.id } + replacement
                            engine =
                                GeneratorEngine(
                                    BuiltinCatalog.tags + libraries.flatMap { it.allTags },
                                    KotlinRandomProvider(),
                                )
                            editor = WorkspaceEditor(engine, libraries)

                            mutableState.value =
                                mutableState.value.copy(
                                    workspace = afterWorkspace,
                                    libraries = libraries,
                                    importing = false,
                                    message = importedText(imported),
                                )

                            continue
                        }

                        if (mutation.personalUpdate != null) {
                            val pack = mutation.personalUpdate.invoke(
                                libraries.firstOrNull { it.library.id == LibraryIds.PERSONAL }
                            )
                            val afterWorkspace = before.withPersonalLibraryEnabled()
                            repository.saveWithLibraries(afterWorkspace, listOf(pack))
                            libraries = libraries.filterNot { it.library.id == pack.library.id } + pack
                            engine = GeneratorEngine(
                                BuiltinCatalog.tags + libraries.flatMap { it.allTags },
                                KotlinRandomProvider(),
                            )
                            editor = WorkspaceEditor(engine, libraries)
                            mutableState.value = mutableState.value.copy(
                                workspace = afterWorkspace,
                                libraries = libraries,
                                error = null,
                                message = mutation.message?.let { uiText(it) },
                            )
                            continue
                        }

                        val after = mutation.transform(before)
                        repository.save(after)

                        mutableState.value =
                            mutableState.value.copy(
                                workspace = after,
                                loading = false,
                                error = null,
                                message = mutation.message?.let { uiText(it) },
                                spinRevision =
                                    mutableState.value.spinRevision +
                                        if (mutation.animate) 1 else 0,
                                animatedReelIds =
                                    if (mutation.animate)
                                        before.reels
                                            .filter {
                                                it.state == ReelState.RANDOM &&
                                                    (mutation.onlyId == null ||
                                                        it.id == mutation.onlyId)
                                            }
                                            .map { it.id }
                                            .toSet()
                                    else emptySet(),
                            )
                        mutation.completion?.complete(null)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error

                        mutableState.value =
                            mutableState.value.copy(
                                importing = false,
                                error = uiText(Res.string.error_not_saved, error.toUiText()),
                            )
                        mutation.completion?.complete(mutableState.value.error)
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error

                mutableState.value =
                    GameReelState(
                        loading = false,
                        error = uiText(Res.string.error_open_failed, error.toUiText()),
                    )
            } finally {
                repository?.close()
            }
        }
    }

    private fun update(message: StringResource? = null, transform: (Workspace) -> Workspace) {
        if (state.value.workspace != null)
            mutations.trySend(Mutation(message = message, transform = transform))
    }

    private fun updateIdea(message: StringResource, transform: (Workspace) -> Workspace):
        CompletableDeferred<com.restor.gamereel.text.UiText?> {
        val completion = CompletableDeferred<com.restor.gamereel.text.UiText?>()
        if (state.value.workspace == null ||
            mutations.trySend(Mutation(message = message, transform = transform, completion = completion)).isFailure)
            completion.complete(uiText(Res.string.error_open_failed,
                com.restor.gamereel.text.UiText.Raw("Workspace unavailable")))
        return completion
    }

    fun dismissMessage() {
        mutableState.value = mutableState.value.copy(message = null, error = null)
    }

    private fun spinMutation(onlyId: String?) {
        if (state.value.workspace == null) return

        mutations.trySend(
            Mutation(
                animate = true,
                onlyId = onlyId,
                transform = { workspace ->
                    editor.spin(workspace, onlyId)
                },
            )
        )
    }

    fun spin() = spinMutation(null)

    fun reroll(id: String) = spinMutation(id)

    fun undo() = update(Res.string.msg_undo) { editor.undo(it) }

    fun resetReels() = update(Res.string.msg_reset) { editor.resetReels(it) }

    fun setAnimation(speed: AnimationSpeed) = update { editor.setAnimation(it, speed) }

    fun setSpinTotalDuration(durationMs: Int) = update { editor.setSpinTotalDuration(it, durationMs) }

    fun setReelStopGap(gapMs: Int) = update { editor.setReelStopGap(it, gapMs) }

    fun setSpinTiming(totalMs: Int, gapMs: Int) = update {
        editor.setSpinTiming(it, totalMs, gapMs)
    }

    fun setLanguage(language: AppLanguage) = update { editor.setLanguage(it, language) }

    fun setUiAccent(accent: UiAccent) = update { editor.setUiAccent(it, accent) }

    fun setUiBackground(background: UiBackground) = update { editor.setUiBackground(it, background) }

    fun setUiText(text: UiText) = update { editor.setUiText(it, text) }

    fun saveUiColor(role: UiColorRole, hex: String) = update { editor.saveUiColor(it, role, hex) }

    fun removeUiColor(role: UiColorRole, hex: String) = update { editor.removeUiColor(it, role, hex) }

    fun setSound(enabled: Boolean) = update { editor.setSound(it, enabled) }

    fun setSpinButtonPosition(position: SpinButtonPosition) = update {
        editor.setSpinButtonPosition(it, position)
    }

    fun savePreset(name: String) = update { editor.savePreset(it, name) }

    fun saveGamesPreset(name: String, games: List<GameReference>) = update {
        editor.saveGamesPreset(it, name, games)
    }

    fun saveCustomPreset(name: String, games: List<GameReference>, tags: List<Tag>, includeGameTags: Boolean) = update {
        editor.saveCustomPreset(it, name, games, tags, includeGameTags)
    }

    fun importPreset(preset: GeneratorPreset) = update { editor.importPreset(it, preset) }

    fun deletePreset(id: String) = update { editor.deletePreset(it, id) }

    fun applyPreset(id: String) = update { editor.applyPreset(it, id) }

    fun missingReelCategories(workspace: Workspace): List<Category> =
        editor.missingReelCategories(workspace)

    fun enableCreativeTwistsAndSpin() {
        if (state.value.workspace != null) mutations.trySend(Mutation(
            animate = true,
            transform = { editor.spin(editor.enableCreativeTwists(it)) },
        ))
    }

    fun applyTagSet(id: String) = update { editor.applyTagSet(it, id) }

    fun saveReelPreset(name: String, categories: List<Category>) = update {
        editor.saveReelPreset(it, name, categories)
    }

    fun clearPreset() = update { editor.clearPreset(it) }

    fun setGeneratorView(view: GeneratorView) = update { editor.setGeneratorView(it, view) }

    fun setIdeaMode(mode: IdeaMode) = update { editor.setIdeaMode(it, mode) }

    fun saveCustomIdeaFilter(filter: CustomIdeaFilter) = update {
        editor.saveCustomIdeaFilter(it, filter)
    }

    fun applyCustomIdeaFilter(id: String) = update { editor.applyCustomIdeaFilter(it, id) }

    fun deleteCustomIdeaFilter(id: String) = update { editor.deleteCustomIdeaFilter(it, id) }

    fun finishWelcome() = update { editor.finishWelcome(it) }

    fun restoreBackup(backup: WorkspaceBackup) {
        if (state.value.workspace != null) mutations.trySend(Mutation(restoreBackup = backup))
    }

    fun enableAllLibraries() =
        update(Res.string.msg_all_libraries) { editor.enableAllLibraries(it) }

    fun enableAllLibrariesAndSpin() {
        if (state.value.workspace != null) mutations.trySend(Mutation(
            message = Res.string.msg_all_libraries,
            animate = true,
            transform = { editor.spin(editor.enableAllLibraries(it)) },
        ))
    }

    fun toggleLibrary(id: String) = update { editor.toggleLibrary(it, id) }

    fun importLibrary(readText: suspend () -> String) {
        if (state.value.workspace != null) mutations.trySend(Mutation(importText = readText))
    }

    fun importPersonalGames(readText: suspend () -> String) {
        if (state.value.workspace != null)
            mutations.trySend(Mutation(importText = readText, personalOnly = true))
    }

    fun savePersonalGame(editingId: String?, title: String, tags: List<GameTagDraft>) {
        if (state.value.workspace != null)
            mutations.trySend(Mutation(message = Res.string.msg_personal_game_saved,
                personalUpdate = { PersonalGames.save(it, editingId, title, tags) }))
    }

    fun deletePersonalGame(id: String) {
        if (state.value.workspace != null)
            mutations.trySend(Mutation(message = Res.string.msg_personal_game_deleted,
                personalUpdate = { PersonalGames.delete(it, id) }))
    }

    fun unlockFrozen() = update { editor.unlockFrozen(it) }

    fun remove(id: String) = update { editor.remove(it, id) }

    fun freeze(id: String) = update { editor.freeze(it, id) }

    fun pin(id: String) = update { editor.pin(it, id) }

    fun pinTag(tag: Tag) = update(Res.string.msg_tag_pinned) { editor.pinTag(it, tag) }

    fun addReel(category: Category) = update { editor.addReel(it, category) }

    fun selectTag(id: String, tag: Tag) = update { editor.selectTag(it, id, tag) }

    fun saveIdea(title: String, notes: String) =
        updateIdea(Res.string.msg_idea_saved) { editor.saveIdea(it, title, notes) }

    fun saveIdea(title: String, notes: String, concept: IdeaConcept, project: String) =
        updateIdea(Res.string.msg_idea_saved) { editor.saveIdea(it, title, notes, concept, project) }

    fun loadIdea(id: String) = update(Res.string.msg_idea_loaded) { editor.loadIdea(it, id) }

    fun beginVariant(id: String) = update { editor.beginVariant(it, id) }

    fun cancelVariant() = update { editor.cancelVariant(it) }

    fun deleteIdea(id: String) = update { editor.deleteIdea(it, id) }

    fun editIdea(id: String, title: String, notes: String) =
        updateIdea(Res.string.msg_idea_updated) { editor.editIdea(it, id, title, notes) }

    fun editIdea(id: String, title: String, notes: String, concept: IdeaConcept, project: String) =
        updateIdea(Res.string.msg_idea_updated) { editor.editIdea(it, id, title, notes, concept, project) }
}

private fun Workspace.withPersonalLibraryEnabled(): Workspace = copy(
    preferences = preferences.copy(
        disabledLibraryIds = preferences.disabledLibraryIds - LibraryIds.PERSONAL,
        enabledLibraryIds = preferences.enabledLibraryIds?.plus(LibraryIds.PERSONAL),
    ),
)
