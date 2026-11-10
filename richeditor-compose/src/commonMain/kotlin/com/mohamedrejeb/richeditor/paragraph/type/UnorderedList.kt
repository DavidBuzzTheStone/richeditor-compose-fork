package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.DefaultListLineHeight
import com.mohamedrejeb.richeditor.model.DefaultListLineHeightStyle
import com.mohamedrejeb.richeditor.model.DefaultUnorderedListStyleType
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

internal class UnorderedList private constructor(
    initialIndent: Int = DefaultListIndent,
    startTextWidth: TextUnit = 0.sp,
    initialLevel: Int = 1,
    initialStyleType: UnorderedListStyleType = DefaultUnorderedListStyleType,
    initialLineHeight: TextUnit = DefaultListLineHeight,
    initialLineHeightStyle: LineHeightStyle? = DefaultListLineHeightStyle,
): ParagraphType, ConfigurableStartTextWidth, ConfigurableListLevel {

    constructor(
        initialLevel: Int = 1,
    ): this(
        initialIndent = DefaultListIndent,
        initialLevel = initialLevel,
        initialLineHeight = DefaultListLineHeight,
        initialLineHeightStyle = DefaultListLineHeightStyle,
    )

    constructor(
        config: RichTextConfig,
        initialLevel: Int = 1,
    ): this(
        initialIndent = config.unorderedListIndent,
        initialLevel = initialLevel,
        initialStyleType = config.unorderedListStyleType,
        initialLineHeight = config.unorderedListLineHeight,
        initialLineHeightStyle = config.unorderedListLineHeightStyle,
    )

    override var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var indent = initialIndent
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    override var level = initialLevel
        set(value) {
            field = value
            style = getNewParagraphStyle()
            startRichSpan = getNewStartRichSpan()
        }

    private var styleType = initialStyleType
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan()
        }

    private var lineHeight = initialLineHeight
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var lineHeightStyle = initialLineHeightStyle
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        var styleChanged = false
        if (config.unorderedListIndent != indent) {
            indent = config.unorderedListIndent
            styleChanged = true
        }

        if (config.unorderedListStyleType != styleType) {
            styleType = config.unorderedListStyleType
        }

        if (config.unorderedListLineHeight != lineHeight) {
            lineHeight = config.unorderedListLineHeight
            styleChanged = true
        }

        if (config.unorderedListLineHeightStyle != lineHeightStyle) {
            lineHeightStyle = config.unorderedListLineHeightStyle
            styleChanged = true
        }

        if (styleChanged) {
            style = getNewParagraphStyle()
        }

        return style
    }

    private fun getNewParagraphStyle(): ParagraphStyle =
        if (lineHeight.isSpecified) {
            ParagraphStyle(
                textIndent = TextIndent(
                    firstLine = ((indent * level) - 14).sp,
                    restLine = (indent * level).sp
                ),
                lineHeight = lineHeight,
                lineHeightStyle = lineHeightStyle,
            )
        } else {
            ParagraphStyle(
                textIndent = TextIndent(
                    firstLine = ((indent * level) - 14).sp,
                    restLine = (indent * level).sp
                )
            )
        }

    @OptIn(ExperimentalRichTextApi::class)
    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan(textRange: TextRange = TextRange(0)): RichSpan {
        val prefixIndex =
            (level - 1).coerceIn(styleType.prefixes.indices)

        val prefix = styleType.prefixes
            .getOrNull(prefixIndex)
            ?: "•"

        val text = "$prefix "

        return RichSpan(
            paragraph = RichParagraph(type = this),
            text = text,
            textRange = TextRange(
                textRange.min,
                textRange.min + text.length
            )
        )
    }

    override fun getNextParagraphType(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialLineHeight = lineHeight,
            initialLineHeightStyle = lineHeightStyle,
        )

    override fun copy(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialLineHeight = lineHeight,
            initialLineHeightStyle = lineHeightStyle,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnorderedList) return false

        if (indent != other.indent) return false
        if (startTextWidth != other.startTextWidth) return false
        if (level != other.level) return false
        if (styleType != other.styleType) return false
        if (lineHeight != other.lineHeight) return false
        if (lineHeightStyle != other.lineHeightStyle) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + level
        result = 31 * result + styleType.hashCode()
        result = 31 * result + lineHeight.hashCode()
        result = 31 * result + (lineHeightStyle?.hashCode() ?: 0)
        return result
    }
}
