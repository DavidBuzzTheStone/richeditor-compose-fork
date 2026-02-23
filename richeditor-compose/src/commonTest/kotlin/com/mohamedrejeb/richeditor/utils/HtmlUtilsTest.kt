package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlUtilsTest {

    @Test
    fun testCreateColoredHtmlSpan() {
        val text = "Hello"
        val color = Color(0xFF00FF00) // Green (ARGB: FF full alpha, 00 Red, FF Green, 00 Blue)
        // Red: 00, Green: FF, Blue: 00 -> #00ff00
        val expected = "<span style=\"color: #00ff00;\">Hello</span>"
        val result = createColoredHtmlSpan(text, color)
        assertEquals(expected, result)
    }

    @Test
    fun testCreateColoredHtmlSpanWithAlpha() {
        val text = "World"
        val color = Color(0x80FF0000) // Red with ~50% alpha (ARGB: 80 Alpha, FF Red, 00 Green, 00 Blue)
        // Red: FF, Green: 00, Blue: 00, Alpha: 80 -> #ff000080
        val expected = "<span style=\"color: #ff000080;\">World</span>"
        val result = createColoredHtmlSpan(text, color)
        assertEquals(expected, result)
    }

    @Test
    fun testCreateColoredHtmlSpanBlack() {
        val text = "Text"
        val color = Color(0xFF000000) // Black
        val expected = "<span style=\"color: #000000;\">Text</span>"
        val result = createColoredHtmlSpan(text, color)
        assertEquals(expected, result)
    }
}
