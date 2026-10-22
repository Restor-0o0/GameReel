package com.restor.gamereel.feature.market

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restor.gamereel.data.stats.Market
import com.restor.gamereel.data.stats.MarketStats
import com.restor.gamereel.data.stats.TagMarketStat
import com.restor.gamereel.model.Category
import com.restor.gamereel.model.Tag
import com.restor.gamereel.ui.components.StudioCard
import com.restor.gamereel.ui.presentation.*
import gamereel.app.shared.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource

internal enum class MarketSort(val title: StringResource, val value: (TagMarketStat) -> Double) {
    TOTAL(Res.string.market_sort_total, { it.sum.toDouble() }),
    MAX(Res.string.market_sort_max, { it.max.toDouble() }),
    MEDIAN(Res.string.market_sort_median, { it.median.toDouble() }),
    GAMES(Res.string.market_sort_games, { it.games.toDouble() }),
    HITS(Res.string.market_sort_hits, { it.hits.toDouble() }),
    RATING(Res.string.market_sort_rating, { it.rating ?: 0.0 }),
}

private class MarketRow(val key: String, val tag: Tag, val label: String, val stat: TagMarketStat)

@Composable
internal fun MarketContent() {
    val stats = LocalMarketStats.current ?: return
    var market by remember { mutableStateOf(Market.STEAM) }
    var sort by remember { mutableStateOf(MarketSort.TOTAL) }
    var category by remember { mutableStateOf<Category?>(null) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }

    val environment = rememberResourceEnvironment()
    val labelled by produceState(emptyList<Pair<String, Tag>>() to emptyMap<String, String>(), stats, environment) {
        val tags =
            stats.tags.mapNotNull { (key, info) ->
                info.category?.let { key to Tag("market:$key", info.name, it, sourceLibraryId = "market") }
            }
        val categories = Category.entries.associate { it.name to getString(environment, it.title) }
        value = tags to tags.associate { (key, tag) -> key to tag.loadLabel(environment) + "\n" + categories[tag.category.name] }
    }
    val rows by produceState(emptyList<MarketRow>(), labelled, market, sort, category, query) {
        value =
            withContext(Dispatchers.Default) {
                val needle = query.trim().lowercase()
                val (tags, search) = labelled
                tags
                    .mapNotNull { (key, tag) ->
                        val stat = stats.tags[key]?.on(market) ?: return@mapNotNull null
                        val text = search[key].orEmpty()
                        if (category != null && tag.category != category) return@mapNotNull null
                        if (needle.isNotEmpty() && needle !in text.lowercase() && needle !in tag.name) return@mapNotNull null
                        MarketRow(key, tag, text.substringBefore('\n'), stat)
                    }
                    .sortedWith(compareByDescending<MarketRow> { sort.value(it.stat) }.thenByDescending { it.stat.sum })
            }
    }
    val categories =
        remember(stats, market) {
            Category.entries.filter { c -> stats.tags.values.any { it.category == c && it.on(market) != null } }
        }
    val total = stats.totals(market)
    val best = rows.maxOfOrNull { sort.value(it.stat) }?.takeIf { it > 0 } ?: 1.0

    Column {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Market.entries.forEach {
                FilterChip(
                    selected = market == it,
                    onClick = { market = it; expanded = null },
                    label = { Text(marketName(it)) },
                )
            }
        }
        if (total != null)
            Text(
                stringResource(
                    Res.string.market_summary,
                    pluralStringResource(Res.plurals.games_count, total.games, total.games),
                    audienceLabel(total.audience),
                ) + " · " + stringResource(if (market == Market.STEAM) Res.string.market_hit_steam else Res.string.market_hit_play),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        OutlinedTextField(
            query,
            { query = it },
            label = { Text(stringResource(Res.string.market_search)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ChipRow {
            FilterChip(category == null, { category = null }, { Text(stringResource(Res.string.market_all)) })
            categories.forEach {
                FilterChip(category == it, { category = if (category == it) null else it }, { Text(stringResource(it.title)) })
            }
        }
        ChipRow {
            MarketSort.entries.forEach {
                FilterChip(sort == it, { sort = it }, { Text(stringResource(it.title)) })
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    stringResource(Res.string.market_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rows.isEmpty()) item { Text(stringResource(Res.string.market_empty)) }
            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                MarketTagCard(
                    index + 1,
                    row,
                    market,
                    stats,
                    (sort.value(row.stat) / best).toFloat(),
                    total?.audience ?: 0,
                    expanded == row.key,
                ) { expanded = if (expanded == row.key) null else row.key }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun marketName(market: Market) = if (market == Market.STEAM) "Steam" else "Google Play"

@Composable
private fun MarketTagCard(
    rank: Int,
    row: MarketRow,
    market: Market,
    stats: MarketStats,
    fraction: Float,
    total: Long,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val stat = row.stat
    StudioCard(onClick, Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(row.tag.category.title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(audienceLabel(stat.sum), style = MaterialTheme.typography.titleSmall)
            }
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                )
            }
            Text(
                listOfNotNull(
                        pluralStringResource(Res.plurals.games_count, stat.games, stat.games),
                        stringResource(Res.string.market_max, audienceLabel(stat.max)),
                        stringResource(Res.string.market_median, audienceLabel(stat.median)),
                        stringResource(Res.string.market_hits, stat.hits),
                        stringResource(Res.string.market_share, shareLabel(stat.sum, total)),
                        stat.rating?.let { "★ " + ratingLabel(market, it) },
                    )
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                Text(stringResource(Res.string.market_top), style = MaterialTheme.typography.labelLarge)
                stat.top.mapNotNull(stats.games::getOrNull).forEach { game ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            game.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            listOfNotNull(audienceLabel(game.audience), game.rating?.let { ratingLabel(market, it) })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
