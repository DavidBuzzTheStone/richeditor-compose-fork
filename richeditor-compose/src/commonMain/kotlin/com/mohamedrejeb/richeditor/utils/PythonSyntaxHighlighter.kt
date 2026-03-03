package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

public data class PythonSyntaxColors(
    val keyword: Color = Color(0xFFCC7832),
    val string: Color = Color(0xFF6A8759),
    val number: Color = Color(0xFF6897BB),
    val comment: Color = Color(0xFF808080),
    val function: Color = Color(0xFFFFC66D)
)

/**
 * A syntax highlighter that implements [VisualTransformation].
 * 
 * This can be passed directly to a Compose `TextField` or `RichText` component:
 * `RichText(state = state, visualTransformation = PythonSyntaxHighlighter())`
 */
public class PythonSyntaxHighlighter(
    private val colors: PythonSyntaxColors = PythonSyntaxColors()
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // If the developer passes in a RichTextState's annotatedString, it already has spans 
        // (like Bold or Underline). We build on top of it.
        val highlighted = buildAnnotatedString {
            append(text)

            val plainText = text.text

            // Track comment ranges so we don't apply keywords/strings inside them.
            val excludedRanges = mutableListOf<IntRange>()

            // 1. Comments
            // RichTextState internally scrubs '\n' to prevent Compose selection bugs and relies 
            // purely on ParagraphStyle boundaries. So we find the '#' and stop at the logical end.
            val paragraphRanges = text.paragraphStyles
            val commentStartRegex = Regex("#")

            commentStartRegex.findAll(plainText).forEach { matchResult ->
                val startIndex = matchResult.range.first

                // If this '#' is already inside a previously registered comment, skip it
                if (excludedRanges.any { it.first <= startIndex && it.last >= startIndex }) {
                    return@forEach
                }

                // Find where this comment should logically stop. 
                // First, look for an explicit paragraph boundary from RichTextState:
                val paragraphEnd = paragraphRanges.firstOrNull { it.start <= startIndex && it.end > startIndex }?.end
                
                // Alternatively, look for a standard newline character if we are inside a normal string:
                val newlineEnd = plainText.indexOf('\n', startIndex).takeIf { it != -1 }

                // The comment stops at whichever boundary occurs first (or end of file)
                val stopIndex = listOfNotNull(paragraphEnd, newlineEnd, plainText.length).min()

                // Create inclusive range for our overlap checks
                val commentRange = startIndex until stopIndex
                excludedRanges.add(commentRange)

                addStyle(
                    style = SpanStyle(color = colors.comment),
                    start = startIndex,
                    end = stopIndex
                )
            }

            // Helper to check if a range overlaps with any comments
            fun isInsideExcluded(start: Int, end: Int): Boolean {
                return excludedRanges.any { comment ->
                    val maxStart = if (start > comment.first) start else comment.first
                    val minEnd = if ((end - 1) < comment.last) (end - 1) else comment.last
                    maxStart <= minEnd
                }
            }

            // 2. Strings
            val stringRegex = Regex("([fF]?\"[^\"]*\")|([fF]?'[^']*')")
            stringRegex.findAll(plainText).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                if (!isInsideExcluded(start, end)) {
                    val value = matchResult.value
                    if (value.startsWith("f", ignoreCase = true)) {
                        val braceRegex = Regex("\\{[^}]*\\}")
                        var currentStringPartStart = start
                        
                        braceRegex.findAll(value).forEach { braceMatch ->
                            val braceStart = start + braceMatch.range.first
                            val braceEnd = start + braceMatch.range.last + 1
                            
                            if (currentStringPartStart < braceStart) {
                                excludedRanges.add(currentStringPartStart until braceStart)
                                addStyle(
                                    style = SpanStyle(color = colors.string),
                                    start = currentStringPartStart,
                                    end = braceStart
                                )
                            }
                            currentStringPartStart = braceEnd
                        }
                        
                        if (currentStringPartStart < end) {
                            excludedRanges.add(currentStringPartStart until end)
                            addStyle(
                                style = SpanStyle(color = colors.string),
                                start = currentStringPartStart,
                                end = end
                            )
                        }
                    } else {
                        excludedRanges.add(start until end)
                        addStyle(
                            style = SpanStyle(color = colors.string),
                            start = start,
                            end = end
                        )
                    }
                }
            }

            // 3. Keywords
            val keywords = listOf(
                "False", "None", "True", "and", "as", "assert", "async", "await", "break",
                "class", "continue", "def", "del", "elif", "else", "except", "finally", "for",
                "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not",
                "or", "pass", "raise", "return", "try", "while", "with", "yield"
            )
            val keywordRegex = Regex("\\b(${keywords.joinToString("|")})\\b")
            keywordRegex.findAll(plainText).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                if (!isInsideExcluded(start, end)) {
                    addStyle(
                        style = SpanStyle(color = colors.keyword),
                        start = start,
                        end = end
                    )
                }
            }

            // 4. Numbers
            val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
            numberRegex.findAll(plainText).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                if (!isInsideExcluded(start, end)) {
                    addStyle(
                        style = SpanStyle(color = colors.number),
                        start = start,
                        end = end
                    )
                }
            }

            // 5. Functions
            val functionRegex = Regex("\\bdef\\s+([a-zA-Z_]\\w*)")
            functionRegex.findAll(plainText).forEach { matchResult ->
                val defAndName = matchResult.value
                val name = defAndName.removePrefix("def").trimStart()
                
                val nameStartIndex = plainText.indexOf(name, matchResult.range.first)
                if (nameStartIndex >= 0) {
                    val nameEndIndex = nameStartIndex + name.length
                    if (!isInsideExcluded(nameStartIndex, nameEndIndex)) {
                        addStyle(
                            style = SpanStyle(color = colors.function),
                            start = nameStartIndex,
                            end = nameEndIndex
                        )
                    }
                }
            }
        }

        // Return the TransformedText.
        // Because we did not change the length of the string (we only added SpanStyles),
        // we can use OffsetMapping.Identity to map cursors exactly 1:1.
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
