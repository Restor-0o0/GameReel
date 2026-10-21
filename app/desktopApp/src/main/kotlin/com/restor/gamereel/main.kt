package com.restor.gamereel

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.restor.gamereel.data.storage.createRepository

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GameReel",
    ) {
        App(::createRepository)
    }
}
