package com.restor.gamereel.data.stats

import com.restor.gamereel.model.Category
import gamereel.app.shared.generated.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Market(val key: String, val hit: Long) {
    STEAM("steam", 10_000_000),
    PLAY("play", 100_000_000),
}

@Serializable
data class MarketTotals(@SerialName("g") val games: Int, @SerialName("a") val audience: Long)

@Serializable
data class MarketGame(
    @SerialName("t") val title: String,
    @SerialName("p") val market: String,
    @SerialName("a") val audience: Long,
    @SerialName("r") val rating: Double? = null,
    @SerialName("k") val ranks: List<Int> = emptyList(),
)

@Serializable
data class MarketTag(@SerialName("n") val name: String, @SerialName("c") val categoryName: String) {
    val key: String
        get() = "$categoryName:${name.lowercase()}"

    val category: Category?
        get() = Category.entries.firstOrNull { it.name == categoryName }
}

data class TagMarketStat(
    val games: Int,
    val sum: Long,
    val max: Long,
    val median: Long,
    val hits: Int,
    val rating: Double?,
    val top: List<MarketGame>,
)

@Serializable
data class MarketStats(
    val totals: Map<String, MarketTotals>,
    val games: List<MarketGame>,
    val references: Map<String, Int>,
    val tags: List<MarketTag>,
) {
    fun totals(market: Market) = totals[market.key]

    fun reference(title: String) = references[title]?.let(games::getOrNull)

    fun tag(category: Category, name: String) =
        tags.indexOfFirst { it.categoryName == category.name && it.name.equals(name.trim(), ignoreCase = true) }
            .takeIf { it >= 0 }

    // games are stored by descending audience, so buckets stay sorted
    fun summarize(market: Market, maxRank: Int? = null): Map<Int, TagMarketStat> {
        val buckets = HashMap<Int, MutableList<MarketGame>>()
        games.filter { it.market == market.key }.forEach { game ->
            for (i in 0 until game.ranks.size - 1 step 2)
                if (maxRank == null || game.ranks[i + 1] <= maxRank)
                    buckets.getOrPut(game.ranks[i]) { mutableListOf() }.add(game)
        }
        return buckets.mapValues { (_, members) ->
            val ratings = members.mapNotNull { it.rating }.sorted()
            TagMarketStat(
                games = members.size,
                sum = members.sumOf { it.audience },
                max = members.first().audience,
                median = members[members.size / 2].audience,
                hits = members.count { it.audience >= market.hit },
                rating = ratings.getOrNull(ratings.size / 2),
                top = members.take(5),
            )
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }

suspend fun loadMarketStats(): MarketStats? =
    runCatching {
            json.decodeFromString<MarketStats>(
                Res.readBytes("files/stats/tag_stats.json").decodeToString()
            )
        }
        .getOrNull()
