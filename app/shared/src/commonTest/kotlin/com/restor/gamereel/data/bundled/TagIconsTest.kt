package com.restor.gamereel.data.bundled

import com.restor.gamereel.catalog.BuiltinCatalog
import com.restor.gamereel.model.*
import com.restor.gamereel.ui.presentation.displayIcon
import com.restor.gamereel.ui.presentation.symbol
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class TagIconsTest {
    @Test
    fun everyBundledTagHasItsOwnIcon() = runTest {
        val tags = BuiltinCatalog.tags + loadBundledLibraries(emptyMap()).flatMap { it.allTags }
        val generic = tags.filter { it.displayIcon == it.category.symbol }
            .map { "${it.category}:${it.name.trim().lowercase()}" }.distinct()
        assertTrue(generic.isEmpty(), "add these to tools/tag_icons/emoji.txt: $generic")
    }
}
