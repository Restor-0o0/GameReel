package com.restor.gamereel.ui.presentation

import com.restor.gamereel.platform.httpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess

// packs may only point at these game asset servers
private val imageSources = listOf(
    "https://cdn.cloudflare.steamstatic.com/",
    "https://cdn.akamai.steamstatic.com/",
    "https://community.cloudflare.steamstatic.com/economy/image/",
    "https://community.akamai.steamstatic.com/economy/image/",
    "https://ddragon.leagueoflegends.com/cdn/",
    "https://media.valorant-api.com/",
    "https://raw.githubusercontent.com/pubg/api-assets/", "https://raw.githubusercontent.com/ByMykel/counter-strike-image-tracker/",
)

internal fun isRemoteImage(url: String) = url.length <= 600 && imageSources.any(url::startsWith)

internal suspend fun readRemoteImage(url: String): ByteArray? {
    val response = httpClient.get(url)
    return if (response.status.isSuccess()) response.readRawBytes() else null
}
