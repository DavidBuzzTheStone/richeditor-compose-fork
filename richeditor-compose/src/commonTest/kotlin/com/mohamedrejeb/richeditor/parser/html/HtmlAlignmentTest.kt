package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.style.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlAlignmentTest {
    @Test
    fun testCenteredDivWithList() {
        val html = """
            <div style="text-align: center;">
                <ol>
                    <li>Item 1</li>
                    <li>Item 2</li>
                </ol>
            </div>
        """.trimIndent()
        
        val state = RichTextStateHtmlParser.encode(html)
        
        // Item 1
        assertEquals(TextAlign.Start, state.richParagraphList[0].paragraphStyle.textAlign, "Item 1 should be left aligned")
        // Item 2
        assertEquals(TextAlign.Start, state.richParagraphList[1].paragraphStyle.textAlign, "Item 2 should be left aligned")
    }

    @Test
    fun testNestedAlignmentLeakage() {
        val html = """
            <div style="text-align: center;">
                <div style="text-align: left;">
                    Left
                </div>
                Center again
            </div>
        """.trimIndent()
        
        val state = RichTextStateHtmlParser.encode(html)
        
        // Paragraph 0: "Left" (inside nested div)
        assertEquals(TextAlign.Left, state.richParagraphList[0].paragraphStyle.textAlign, "First paragraph should be left aligned")
        
        // Paragraph 1: "Center again" (after nested div closed)
        assertEquals(TextAlign.Center, state.richParagraphList[1].paragraphStyle.textAlign, "Second paragraph should be centered aligned")
    }

}
