package com.mohamedrejeb.richeditor.math

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.parser.utils.OverlineBackgroundColor
import com.mohamedrejeb.richeditor.parser.utils.OverlineSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.SqrtBackgroundColor

// In your Utils file

/**
 * Generic Merger: Combines adjacent spans that match a specific predicate.
 * This fixes "fragmentation" caused by nested styles (like superscripts).
 */
internal fun List<AnnotatedString.Range<SpanStyle>>.mergeRanges(
    predicate: (SpanStyle) -> Boolean
): List<TextRange> {
    val sortedRanges = this
        .filter { predicate(it.item) }
        .sortedBy { it.start }

    if (sortedRanges.isEmpty()) return emptyList()

    val merged = mutableListOf<TextRange>()
    var currentStart = sortedRanges[0].start
    var currentEnd = sortedRanges[0].end

    for (i in 1 until sortedRanges.size) {
        val next = sortedRanges[i]
        // Merge if overlapping or adjacent
        if (next.start <= currentEnd) {
            currentEnd = maxOf(currentEnd, next.end)
        } else {
            merged.add(TextRange(currentStart, currentEnd))
            currentStart = next.start
            currentEnd = next.end
        }
    }
    merged.add(TextRange(currentStart, currentEnd))
    return merged
}

public fun SpanStyle.isSqrtMarker(): Boolean {
    return this.background == SqrtBackgroundColor
}

public fun SpanStyle.isOverlineMarker(): Boolean {
    return this.background == OverlineBackgroundColor
}

public fun RichTextState.toggleOverline() { //TODO: This doesn't work on non-empty selections (it just toggles after the selection)
    this.toggleSpanStyle(OverlineSpanStyle)
}