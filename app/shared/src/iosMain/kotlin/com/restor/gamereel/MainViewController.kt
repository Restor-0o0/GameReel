package com.restor.gamereel

import androidx.compose.ui.window.ComposeUIViewController
import com.restor.gamereel.data.storage.createRepository

fun MainViewController() = ComposeUIViewController { App(::createRepository) }
