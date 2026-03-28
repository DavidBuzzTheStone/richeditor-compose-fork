package com.matura.common_core.ui.utils.RichText

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.parser.html.sqrtSpacer
import com.mohamedrejeb.richeditor.parser.utils.SqrtSpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class MathGuardsProcessorTest {

    @Test
    fun `Given SqrtSpanStyle block with missing prefix When processMathGuards is called Then style is removed`() {
        // Given
        val text = "some text"
        val spanStyle = AnnotatedString.Range(SqrtSpanStyle, 0, text.length)
        val annotatedString = AnnotatedString(text, spanStyles = listOf(spanStyle))
        
        var removedRange: TextRange? = null

        // When
        MathGuardsProcessor.processMathGuards(
            annotatedString = annotatedString,
            onRemoveSpanStyle = { range -> removedRange = range },
            onAddSpanStyle = { },
            onUpdateSelection = { },
            onAddTextAtIndex = { _, _ -> }
        )

        // Then
        assertEquals(TextRange(0, text.length), removedRange, "Expected the whole range to be removed due to missing start guard.")
    }

    @Test
    fun `Given SqrtSpanStyle block with swallowed guard When processMathGuards is called Then style is shifted`() {
        // Given
        val text = "$sqrtSpacer x^2 $sqrtSpacer"
        val spanStyle = AnnotatedString.Range(SqrtSpanStyle, 0, text.length)
        val annotatedString = AnnotatedString(text, spanStyles = listOf(spanStyle))
        
        var removedRange: TextRange? = null
        var addedRange: TextRange? = null

        // When
        MathGuardsProcessor.processMathGuards(
            annotatedString = annotatedString,
            onRemoveSpanStyle = { range -> removedRange = range },
            onAddSpanStyle = { range -> addedRange = range },
            onUpdateSelection = { },
            onAddTextAtIndex = { _, _ -> }
        )

        // Then
        assertEquals(TextRange(0, text.length), removedRange)
        assertEquals(TextRange(0, text.length - 1), addedRange, "Expected style to be re-applied excluding the swallowed suffix spacer.")
    }

    @Test
    fun `Given SqrtSpanStyle block missing end guard When processMathGuards is called Then spacer is added at end`() {
        // Given
        val text = "$sqrtSpacer x^2"
        val spanStyle = AnnotatedString.Range(SqrtSpanStyle, 0, text.length)
        val annotatedString = AnnotatedString(text, spanStyles = listOf(spanStyle))
        
        var addedIndex: Int? = null
        var addedText: String? = null

        // When
        MathGuardsProcessor.processMathGuards(
            annotatedString = annotatedString,
            onRemoveSpanStyle = { },
            onAddSpanStyle = { },
            onUpdateSelection = { },
            onAddTextAtIndex = { index, spacer ->
                addedIndex = index
                addedText = spacer
            }
        )

        // Then
        assertEquals(text.length, addedIndex)
        assertEquals(sqrtSpacer, addedText)
    }
}
