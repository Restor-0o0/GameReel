package com.restor.gamereel

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restor.gamereel.data.bundled.loadBundledLibraries
import com.restor.gamereel.model.AppLanguage
import com.restor.gamereel.navigation.GameReelScreen
import com.restor.gamereel.presentation.*
import com.restor.gamereel.repository.WorkspaceRepository
import com.restor.gamereel.text.AppLanguageProvider
import com.restor.gamereel.ui.presentation.GameArt
import com.restor.gamereel.ui.presentation.LocalGameArt
import com.restor.gamereel.ui.theme.GameReelTheme
import gamereel.app.shared.generated.resources.Res

@Composable
fun App(repositoryFactory: () -> WorkspaceRepository) {
    val model = viewModel {
        GameReelViewModel(repositoryFactory, bundledLoader = ::loadBundledLibraries)
    }

    val state by model.state.collectAsState()
    val allowStoreArt by
        produceState(false) {
            value = Res.readBytes("files/art-policy.txt").decodeToString().trim() != "no-store-art"
        }
    val art = remember(state.libraries, allowStoreArt) { GameArt(state.libraries, allowStoreArt) }
    AppLanguageProvider(state.workspace?.preferences?.language ?: AppLanguage.SYSTEM) {
        GameReelTheme(state.workspace?.preferences) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CompositionLocalProvider(LocalGameArt provides art) { GameReelScreen(state, model) }
            }
        }
    }
}
