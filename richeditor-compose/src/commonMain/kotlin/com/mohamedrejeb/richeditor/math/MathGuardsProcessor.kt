package com.matura.common_core.ui.utils.RichText

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.math.isSqrtMarker
import com.mohamedrejeb.richeditor.math.mergeRanges
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.parser.html.sqrtSpacer
import com.mohamedrejeb.richeditor.parser.utils.SqrtSpanStyle

public object MathGuardsProcessor {

    /**
     * Inspects the [AnnotatedString] for broken math spans (e.g. Sqrt) and performs corrective actions.
     * Returns an action to execute on the [RichTextState] or null if no action is needed.
     */
    public fun processMathGuards(
        annotatedString: AnnotatedString,
        onRemoveSpanStyle: (TextRange) -> Unit,
        onAddSpanStyle: (TextRange) -> Unit,
        onUpdateSelection: (TextRange) -> Unit,
        onAddTextAtIndex: (Int, String) -> Unit,
    ) {
        val spanStyles = annotatedString.spanStyles
        val spacer = sqrtSpacer
        val mergedSqrtRanges = spanStyles.mergeRanges { it.isSqrtMarker() }

        for (range in mergedSqrtRanges) {
            val start = range.start
            val end = range.end
            val textLen = annotatedString.text.length

            // --- 1. START GUARD CHECK ---
            if (start < textLen) {
                val startChars = annotatedString.text.substring(
                    start,
                    (start + spacer.length).coerceAtMost(textLen)
                )
                if (startChars != spacer) {
                    onRemoveSpanStyle(TextRange(start, end))
                    return
                }
            }

            // --- 2. SWALLOWED GUARD CHECK ---
            if (end - start > spacer.length) {
                val lastCharStartIndex = end - spacer.length
                if (lastCharStartIndex >= 0) {
                    val tailText = annotatedString.text.substring(lastCharStartIndex, end)

                    if (tailText == spacer) {
                        onRemoveSpanStyle(TextRange(start, end))
                        onAddSpanStyle(TextRange(start, end - 1))
                        onUpdateSelection(TextRange(end - 1))
                        return
                    }
                }
            }

            // --- 3. MISSING END GUARD CHECK ---
            if (end >= textLen) {
                onAddTextAtIndex(end, spacer)
                return
            } else {
                val nextCharEndIndex = (end + spacer.length).coerceAtMost(textLen)
                val nextChars = annotatedString.text.substring(end, nextCharEndIndex)

                if (nextChars != spacer) {
                    onAddTextAtIndex(end, spacer)
                    return
                }
            }
        }
    }
}
