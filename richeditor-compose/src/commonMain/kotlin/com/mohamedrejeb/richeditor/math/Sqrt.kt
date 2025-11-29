package com.mohamedrejeb.richeditor.math

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.parser.html.sqrtSpacer
import com.mohamedrejeb.richeditor.parser.utils.SqrtSpanStyle


/**
 * Toggles the Square Root style on the current selection.
 */
public fun RichTextState.toggleSquareRoot() {
    // 1. Guard against toggle inside existing root (Optional: could handle nested roots here)
    if (this.currentSpanStyle.isSqrtMarker()) {
        // If we are already in a root, just toggle the style off for the pending cursor
        this.toggleSpanStyle(SqrtSpanStyle)
        return
    }

    val spacer = sqrtSpacer
    val selection = this.selection
    val start = selection.min

    // Calculate validity once
    val textLength = this.annotatedString.text.length
    if (start !in 0..textLength) return

    if (selection.collapsed) {
        // --- CASE A: Insert Empty Root ---

        // 1. Prepare the full insertion string
        val textToInsert = "$spacer$spacer"

        // 2. Insert text.
        // NOTE: This usually moves the selection to the END of the insertion automatically.
        this.addTextAtIndex(start, textToInsert)

        // 3. Apply Style to the FIRST spacer only
        this.addSpanStyle(
            spanStyle = SqrtSpanStyle,
            textRange = TextRange(start, start + spacer.length)
        )

        // 4. Force Cursor Placement
        // We place it strictly between the spacers.
        val cursorIndex = start + spacer.length
        this.selection = TextRange(cursorIndex) //TODO: the cursor doesn't actually get placed there. It completely disappears

    } else { //TODO: here the selection doesn't work at all. It just gets inserted after the selection as if it was collapsed (or maybe the if block is taken always, as it indeed thinks it is collapsed
        // --- CASE B: Wrap Selection ---
        val end = selection.max
        val selectedText = this.annotatedString.text.substring(start, end)

        // 1. Replace text with [Spacer]Content[Spacer]
        val newText = "$spacer$selectedText$spacer"
        this.replaceTextRange(selection, newText)

        // 2. Apply Style to [Spacer + Content]
        // Note: We do NOT style the trailing spacer.
        val styleEndIndex = start + spacer.length + selectedText.length

        this.addSpanStyle(
            spanStyle = SqrtSpanStyle,
            textRange = TextRange(start, styleEndIndex)
        )

        // 3. Restore Selection
        // Select the content inside (standard UX for wrapping)
        this.selection = TextRange(start + spacer.length, styleEndIndex)
    }
}

public fun Modifier.drawMathMarkers(
    textLayoutResult: TextLayoutResult?,
    annotatedString: AnnotatedString,
    color: Color = Color.Black
): Modifier = this.drawWithContent {
    drawContent()
    if (textLayoutResult == null) return@drawWithContent

    val strokeWidth = 1.5.dp.toPx()
    val radicalPath = Path()
    // =================================================
    // 1. DRAW OVERLINES
    // =================================================
    val overlineRanges = annotatedString.spanStyles.mergeRanges { it.isOverlineMarker() }

    overlineRanges.forEach { range ->
        try {
            val startOffset = range.start
            val endOffset = range.end
            if (startOffset >= endOffset) return@forEach

            // Scan for the highest character (Superscript Awareness)
            var minTop = Float.MAX_VALUE
            for (i in startOffset until endOffset) {
                val box = textLayoutResult.getBoundingBox(i)
                if (box.top < minTop) minTop = box.top
            }

            // Fallback if measurement failed
            if (minTop == Float.MAX_VALUE) {
                val lineIndex = textLayoutResult.getLineForOffset(startOffset)
                minTop = textLayoutResult.getLineTop(lineIndex)
            }

            // Geometry
            val startBox = textLayoutResult.getBoundingBox(startOffset)
            val endBox = textLayoutResult.getBoundingBox(endOffset - 1) // -1 because end is exclusive

            val lineStartX = startBox.left
            val lineEndX = endBox.right
            val lineY = minTop // Draw exactly at the top (or subtract padding: minTop - 2f)

            drawLine(
                color = color,
                start = Offset(lineStartX, lineY),
                end = Offset(lineEndX, lineY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

        } catch (e: Exception) { }
    }

    // =================================================
    // 2. DRAW SQUARE ROOTS (Your existing logic)
    // =================================================
    val sqrtRanges = annotatedString.spanStyles.mergeRanges { it.isSqrtMarker() }
    sqrtRanges.forEach { range ->
        try {
            val startOffset = range.start
            val endOffset = range.end
            if (startOffset >= endOffset) return@forEach

            // Standard Metrics Logic...
            val lineIndex = textLayoutResult.getLineForOffset(startOffset)
            val baseline = textLayoutResult.getLineBaseline(lineIndex)
            val lineTop = textLayoutResult.getLineTop(lineIndex)
            val lineBottom = textLayoutResult.getLineBottom(lineIndex)
            val lineHeight = lineBottom - lineTop

            // Scan for height (superscript support)
            var minTop = Float.MAX_VALUE
            var maxRight = -Float.MAX_VALUE

            for (i in startOffset until endOffset) {
                val box = textLayoutResult.getBoundingBox(i)
                if (box.top < minTop) minTop = box.top
                if (box.right > maxRight) maxRight = box.right
            }
            if (minTop == Float.MAX_VALUE) minTop = lineTop

            // Trailing Overhang Logic
            var roofEnd = maxRight
            if (endOffset < annotatedString.text.length) {
                val nextChar = annotatedString.text[endOffset]
                if (nextChar.toString() == sqrtSpacer) {
                    val spacerBox = textLayoutResult.getBoundingBox(endOffset)
                    roofEnd = spacerBox.center.x
                }
            }

            // Draw Geometry (Same as your working version)
            val startBox = textLayoutResult.getBoundingBox(startOffset)
            val hookStartX = startBox.left + (startBox.right - startBox.left) / 3
            val valleyX = startBox.right - 2 * strokeWidth
            val roofStartX = startBox.right - strokeWidth
            val roofY = minTop + (strokeWidth * 0.5f)
            val hookStartY = baseline - (lineHeight * 0.4f)
            val valleyY = baseline + (lineHeight * 0.05f)

            radicalPath.reset()
            radicalPath.moveTo(hookStartX, hookStartY)
            radicalPath.lineTo(valleyX, valleyY)
            radicalPath.lineTo(roofStartX, roofY)
            radicalPath.lineTo(roofEnd, roofY)
            radicalPath.lineTo(roofEnd, roofY + (lineHeight * 0.2f))

            drawPath(
                path = radicalPath,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

        } catch (e: Exception) { }
    }
}