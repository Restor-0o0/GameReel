package com.restor.gamereel.feature.settings

import com.restor.gamereel.model.Preferences
import com.restor.gamereel.model.UiCustomColors
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class UiCustomColorsSerializationTest {
    @Test
    fun savedPaletteSurvivesSerialization() {
        val preferences = Preferences(customColors = UiCustomColors(
            accents = listOf("#C5AEFF"),
            backgrounds = listOf("#202838"),
            texts = listOf("#E8DFFF"),
            selectedAccent = "#C5AEFF",
        ))

        assertEquals(preferences.customColors,
            Json.decodeFromString<Preferences>(Json.encodeToString(preferences)).customColors)
    }
}
