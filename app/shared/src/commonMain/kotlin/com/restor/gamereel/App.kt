package com.restor.gamereel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restor.gamereel.data.bundled.loadBundledLibraries
import com.restor.gamereel.data.stats.MarketStats
import com.restor.gamereel.data.stats.loadMarketStats
import com.restor.gamereel.model.AppLanguage
import com.restor.gamereel.navigation.GameReelScreen
import com.restor.gamereel.presentation.*
import com.restor.gamereel.repository.WorkspaceRepository
import com.restor.gamereel.text.AppLanguageProvider
import com.restor.gamereel.ui.presentation.LocalMarketStats
import com.restor.gamereel.ui.theme.GameReelTheme

@Composable
fun App(
    repositoryFactory: () -> WorkspaceRepository,
    incomingLink: String? = null,
    incomingFile: IncomingFile? = null,
) {
    val model = viewModel {
        GameReelViewModel(repositoryFactory, bundledLoader = ::loadBundledLibraries)
    }

    val state by model.state.collectAsState()
    val loaded = state.workspace != null && !state.loading
    LaunchedEffect(incomingLink, loaded) {
        if (loaded && incomingLink != null) model.openSharedIdea(incomingLink)
    }
    LaunchedEffect(incomingFile, loaded) {
        if (loaded && incomingFile != null) model.importLibrary(incomingFile.read)
    }
    val market by produceState<MarketStats?>(null) { value = loadMarketStats() }
    AppLanguageProvider(state.workspace?.preferences?.language ?: AppLanguage.SYSTEM) {
        GameReelTheme(state.workspace?.preferences) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CompositionLocalProvider(LocalMarketStats provides market) {
                    GameReelScreen(state, model)
                }
            }
        }
    }
}

class IncomingFile(val key: String, val read: suspend () -> String)
