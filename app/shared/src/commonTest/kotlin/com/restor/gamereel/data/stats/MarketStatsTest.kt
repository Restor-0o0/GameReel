package com.restor.gamereel.data.stats

import com.restor.gamereel.model.Category
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class MarketStatsTest {
    @Test
    fun bundledStatsCoverBothMarkets() = runTest {
        val stats = assertNotNull(loadMarketStats())
        Market.entries.forEach { market -> assertTrue(assertNotNull(stats.totals(market)).audience > 0) }
        val survival = assertNotNull(stats.tag(Category.CORE, "Survival"))
        Market.entries.forEach { market ->
            val stat = assertNotNull(survival.on(market))
            assertTrue(stat.max <= stat.sum && stat.median <= stat.max && stat.hits <= stat.games)
            assertTrue(stat.top.all { stats.games[it].market == market.key })
        }
        assertTrue(stats.references.values.all { it in stats.games.indices })
    }
}
