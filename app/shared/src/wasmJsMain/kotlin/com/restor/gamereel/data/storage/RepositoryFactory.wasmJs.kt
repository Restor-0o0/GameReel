@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.restor.gamereel.data.storage

import com.restor.gamereel.data.storage.BrowserWorkspaceRepository

@JsFun("() => localStorage.getItem('gamereel.workspace.v1')")
private external fun readWorkspace(): String?

@JsFun("(value) => { localStorage.setItem('gamereel.workspace.v1', value); }")
private external fun writeWorkspace(value: String)

@JsFun("() => localStorage.getItem('gamereel.libraries.v1')")
private external fun readLibraries(): String?

@JsFun("(value) => { localStorage.setItem('gamereel.libraries.v1', value); }")
private external fun writeLibraries(value: String)

fun createRepository() =
    BrowserWorkspaceRepository(::readWorkspace, ::writeWorkspace, ::readLibraries, ::writeLibraries)
