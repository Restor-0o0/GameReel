package com.restor.gamereel.data.stats

import com.restor.gamereel.model.Category
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class MarketStatsTest {
    @Test
    fun bundledStatsCoverBothMarkets() = runTest {
        val stats = assertNotNull(loadMarketStats())
        val survival = assertNotNull(stats.tag(Category.CORE, "Survival"))
        Market.entries.forEach { market ->
            assertTrue(assertNotNull(stats.totals(market)).audience > 0)
            val all = assertNotNull(stats.summarize(market)[survival])
            val main = assertNotNull(stats.summarize(market, 1)[survival])
            assertTrue(all.max <= all.sum && all.median <= all.max && all.hits <= all.games)
            assertTrue(main.games < all.games && main.sum <= all.sum)
            assertTrue(all.top.all { it.market == market.key })
        }
        assertTrue(stats.references.values.all { it in stats.games.indices })
    }
}
