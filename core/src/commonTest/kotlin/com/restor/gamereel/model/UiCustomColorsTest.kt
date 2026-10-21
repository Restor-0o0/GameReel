package com.restor.gamereel.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UiCustomColorsTest {
    @Test
    fun hexColorsAreNormalizedAndValidated() {
        assertEquals("#C5AEFF", normalizeHexColor(" c5aeff "))
        assertEquals("#101820", normalizeHexColor("#101820"))
        assertNull(normalizeHexColor("#xyz"))
        assertNull(normalizeHexColor("#12345678"))
    }

}
