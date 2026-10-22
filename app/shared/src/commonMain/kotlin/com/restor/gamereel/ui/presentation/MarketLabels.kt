package com.restor.gamereel.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.restor.gamereel.data.stats.Market
import com.restor.gamereel.data.stats.MarketStats
import gamereel.app.shared.generated.resources.*
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource

val LocalMarketStats = compositionLocalOf<MarketStats?> { null }

@Composable
fun audienceLabel(value: Long): String {
    val (scaled, unit) =
        when {
            value >= 1_000_000_000 -> value / 1e9 to Res.string.market_billions
            value >= 1_000_000 -> value / 1e6 to Res.string.market_millions
            value >= 1_000 -> value / 1e3 to Res.string.market_thousands
            else -> return value.toString()
        }
    return stringResource(unit, decimal(scaled, if (scaled >= 100) 0 else 1))
}

@Composable
fun ratingLabel(market: Market, rating: Double): String =
    if (market == Market.STEAM) "${rating.roundToLong()}%" else "${decimal(rating, 1)}★"

@Composable
fun shareLabel(part: Long, total: Long): String {
    val percent = if (total > 0) part * 100.0 / total else 0.0
    return "${decimal(percent, if (percent >= 10) 0 else 1)}%"
}

@Composable
private fun decimal(value: Double, digits: Int): String {
    if (digits == 0) return value.roundToLong().toString()
    val tenths = (value * 10).roundToLong()
    val text = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    return if (stringResource(Res.string.language_code) == "en") text else text.replace('.', ',')
}
