package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Font used to draw arrow symbols (see [isSymbolFontChar]).
 *
 * Text fonts that lack a glyph such as ↔ (U+2194) make the OS fall back to its colour emoji font, and
 * the text-presentation selector U+FE0E is ignored during that fallback (verified with Skia on macOS).
 * Compose also does not fall back between members of one [FontFamily]. So the app provides a small
 * font that has these glyphs here, and they are drawn with it at render time. The stored text is not
 * changed. `null` (the default) leaves the text untouched.
 */
public val LocalSymbolFontFamily: ProvidableCompositionLocal<FontFamily?> =
    staticCompositionLocalOf { null }

/**
 * The arrows that fall back to an emoji (U+2194–2199, U+21A9–21AA, U+27A1, U+2B05–2B07), plus the
 * rest of the arrows block so a run of mixed arrows keeps one look.
 */
internal fun isSymbolFontChar(char: Char): Boolean =
    char in '←'..'↪' ||
        char == '➡' ||
        char in '⬅'..'⬇'

/** Same text with [family] applied to every arrow character. Offsets are unchanged. */
internal fun AnnotatedString.withSymbolFont(family: FontFamily?): AnnotatedString {
    if (family == null || text.none(::isSymbolFontChar)) return this
    val style = SpanStyle(fontFamily = family)
    return AnnotatedString.Builder(this).apply {
        var runStart = -1
        text.forEachIndexed { index, char ->
            if (isSymbolFontChar(char)) {
                if (runStart < 0) runStart = index
            } else if (runStart >= 0) {
                addStyle(style, runStart, index)
                runStart = -1
            }
        }
        if (runStart >= 0) addStyle(style, runStart, text.length)
    }.toAnnotatedString()
}

internal fun VisualTransformation.withSymbolFont(family: FontFamily?): VisualTransformation =
    if (family == null) this
    else VisualTransformation { original ->
        val transformed = filter(original)
        TransformedText(transformed.text.withSymbolFont(family), transformed.offsetMapping)
    }
