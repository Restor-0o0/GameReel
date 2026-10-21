package com.restor.gamereel.data.storage

import kotlinx.serialization.json.Json

internal val workspaceJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
