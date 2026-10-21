package com.restor.gamereel.data.storage

import com.restor.gamereel.data.storage.BrowserWorkspaceRepository
import web.storage.localStorage

fun createRepository() =
    BrowserWorkspaceRepository(
        read = { localStorage.getItem("gamereel.workspace.v1") },
        write = { localStorage.setItem("gamereel.workspace.v1", it) },
        readLibraries = { localStorage.getItem("gamereel.libraries.v1") },
        writeLibraries = { localStorage.setItem("gamereel.libraries.v1", it) },
    )
