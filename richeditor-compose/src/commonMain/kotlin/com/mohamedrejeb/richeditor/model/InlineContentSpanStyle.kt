package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * A Generic SpanStyle for mapping inline Composables into the RichText editor. 
 * This tag behaves identically to a `<br>` or `<img>` tag where it does not 
 * house any children text nodes and acts as an atomic visual block.
 */
@OptIn(ExperimentalRichTextApi::class)
public class InlineContentSpanStyle(
    public val id: String
) : RichSpanStyle {

    // Ensures that typing does not happen inside the inline boundary
    override val acceptNewTextInTheEdges: Boolean = false

    override val spanStyle: (RichTextConfig) -> SpanStyle = { SpanStyle() }

    override fun AnnotatedString.Builder.appendCustomContent(
        richTextState: RichTextState
    ): AnnotatedString.Builder {
        appendInlineContent(id = id, alternateText = "[inline]")
        return this
    }

    override fun DrawScope.drawCustomStyle(
        layoutResult: TextLayoutResult,
        textRange: TextRange,
        richTextConfig: RichTextConfig,
        topPadding: Float,
        startPadding: Float
    ): Unit = Unit
}
