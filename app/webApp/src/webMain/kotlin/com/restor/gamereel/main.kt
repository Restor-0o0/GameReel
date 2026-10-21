package com.restor.gamereel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.restor.gamereel.data.storage.createRepository

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(::createRepository)
    }
}
