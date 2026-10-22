package com.restor.gamereel.navigation

import gamereel.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource

internal enum class Destination(val title: StringResource, val symbol: String) {
    GENERATOR(Res.string.nav_generator, "◈"),
    SEARCH(Res.string.nav_search, "⌕"),
    LIBRARIES(Res.string.nav_libraries, "▦"),
    IDEAS(Res.string.nav_ideas, "▤"),
    MARKET(Res.string.nav_market, "▥"),
    SETTINGS(Res.string.nav_settings, "⚙"),
}
