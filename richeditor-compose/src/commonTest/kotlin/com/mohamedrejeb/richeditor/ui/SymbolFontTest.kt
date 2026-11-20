package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SymbolFontTest {
    private val family = FontFamily.Monospace

    @Test
    fun arrowsGetTheSymbolFontAndTextIsUntouched() {
        val result = AnnotatedString("a ↔ b ↕↕ c").withSymbolFont(family)

        assertEquals("a ↔ b ↕↕ c", result.text)
        val ranges = result.spanStyles.map { it.start to it.end }
        assertEquals(listOf(2 to 3, 6 to 8), ranges)
        assertEquals(family, result.spanStyles.first().item.fontFamily)
    }

    @Test
    fun textWithoutArrowsOrWithoutFamilyIsReturnedAsIs() {
        val plain = AnnotatedString("no arrows here")
        assertSame(plain, plain.withSymbolFont(family))

        val arrow = AnnotatedString("a ↔ b")
        assertSame(arrow, arrow.withSymbolFont(null))
    }

    @Test
    fun arrowAtTheEndIsStyled() {
        val result = AnnotatedString("a →").withSymbolFont(family)
        assertEquals(listOf(2 to 3), result.spanStyles.map { it.start to it.end })
    }
}
