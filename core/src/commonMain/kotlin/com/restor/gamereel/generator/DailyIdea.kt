package com.restor.gamereel.generator

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import kotlin.random.Random

class SeededRandomProvider(seed: Long) : RandomProvider {
    private val random = Random(seed)

    override fun nextInt(bound: Int) = random.nextInt(bound)
}

object DailyIdea {
    const val MILLIS_PER_DAY = 86_400_000L

    fun epochDay(epochMillis: Long): Long = epochMillis.floorDiv(MILLIS_PER_DAY)

    fun seed(epochDay: Long): Long = (epochDay + 0x47524C) * -0x61c8864680b583ebL

    /** Uses only tags shipped in every build, so all users get the same reels for a UTC day. */
    fun reels(epochDay: Long, tags: List<Tag>): List<Reel> {
        val pool = tags.filter { it.sourceLibraryId == LibraryIds.FIXTURE || it.sourceLibraryId == LibraryIds.CURATED }
            .distinctBy { it.id }.sortedBy { it.id }
        val engine = GeneratorEngine(pool, SeededRandomProvider(seed(epochDay)))
        return engine.spin(BuiltinCatalog.initialWorkspace().reels,
            GeneratorConfig(maxAutoAtomsFromSameGame = Int.MAX_VALUE, tagPool = pool))
    }

    fun isoDate(epochDay: Long): String {
        val z = epochDay + 719_468
        val era = z.floorDiv(146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
}
