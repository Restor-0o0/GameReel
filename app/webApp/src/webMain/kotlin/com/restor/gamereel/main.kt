package com.restor.gamereel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.restor.gamereel.data.storage.createRepository
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val link = window.location.hash.takeIf { it.contains("GR1.") }
    if (link != null) window.history.replaceState(null, "", window.location.pathname + window.location.search)
    ComposeViewport {
        App(::createRepository, link)
    }
}
