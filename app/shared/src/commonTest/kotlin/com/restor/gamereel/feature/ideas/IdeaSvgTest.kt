package com.restor.gamereel.feature.ideas

import kotlin.test.*

class IdeaSvgTest {
    @Test
    fun cardEscapesUserText() {
        val svg = ideaSvg("A <game> & idea")
        assertTrue(svg.contains("A &lt;game&gt; &amp; idea"))
        assertFalse(svg.contains("<game>"))
        assertTrue(svg.endsWith("</svg>"))
    }
}
