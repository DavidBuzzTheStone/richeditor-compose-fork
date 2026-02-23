package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.graphics.Color

/**
 * Creates an HTML span element with the specified text and color style.
 *
 * @param text The text content to be wrapped in the span.
 * @param color The color to apply to the text (ARGB).
 * @return An HTML string representing a span with the specified text and color.
 */
public fun createColoredHtmlSpan(text: String, color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    val alpha = (color.alpha * 255).toInt()

    val hexColor = buildString {
        append("#")
        append(red.toString(16).padStart(2, '0'))
        append(green.toString(16).padStart(2, '0'))
        append(blue.toString(16).padStart(2, '0'))
        if (alpha != 255) {
            append(alpha.toString(16).padStart(2, '0'))
        }
    }

    return "<span style=\"color: $hexColor;\">$text</span>"
}
