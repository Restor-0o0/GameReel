package com.restor.gamereel.data.stats

import com.restor.gamereel.model.Category
import gamereel.app.shared.generated.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Market(val key: String) {
    STEAM("steam"),
    PLAY("play"),
}

@Serializable
data class MarketTotals(@SerialName("g") val games: Int, @SerialName("a") val audience: Long)

@Serializable
data class MarketGame(
    @SerialName("t") val title: String,
    @SerialName("p") val market: String,
    @SerialName("a") val audience: Long,
    @SerialName("r") val rating: Double? = null,
)

@Serializable
data class TagMarketStat(
    @SerialName("g") val games: Int,
    val sum: Long,
    val max: Long,
    @SerialName("med") val median: Long,
    @SerialName("hit") val hits: Int,
    @SerialName("r") val rating: Double? = null,
    val top: List<Int>,
)

@Serializable
data class TagStats(
    @SerialName("n") val name: String,
    @SerialName("c") val categoryName: String,
    val steam: TagMarketStat? = null,
    val play: TagMarketStat? = null,
) {
    val category: Category?
        get() = Category.entries.firstOrNull { it.name == categoryName }

    fun on(market: Market) = if (market == Market.STEAM) steam else play
}

@Serializable
data class MarketStats(
    val totals: Map<String, MarketTotals>,
    val games: List<MarketGame>,
    val references: Map<String, Int>,
    val tags: Map<String, TagStats>,
) {
    fun totals(market: Market) = totals[market.key]

    fun reference(title: String) = references[title]?.let(games::getOrNull)

    fun tag(category: Category, name: String) = tags["${category.name}:${name.trim().lowercase()}"]
}

private val json = Json { ignoreUnknownKeys = true }

suspend fun loadMarketStats(): MarketStats? =
    runCatching {
            json.decodeFromString<MarketStats>(
                Res.readBytes("files/stats/tag_stats.json").decodeToString()
            )
        }
        .getOrNull()
