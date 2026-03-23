package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import com.mohamedrejeb.ksoup.entities.KsoupEntities
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableListLevel
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.parser.RichTextStateParser
import com.mohamedrejeb.richeditor.parser.utils.BoldSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.DoubleUnderScoreSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H1SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H2SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H3SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H4SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H5SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.H6SpanStyle
import com.mohamedrejeb.richeditor.parser.utils.ItalicSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.MarkBackgroundColor
import com.mohamedrejeb.richeditor.parser.utils.OverlineSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.SmallSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.SqrtSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.StrikethroughSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.SubscriptSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.SuperscriptSpanStyle
import com.mohamedrejeb.richeditor.parser.utils.UnderlineSpanStyle
import com.mohamedrejeb.richeditor.utils.customMerge


public object MathSpacers {
    public const val Thin: String = "\u2009"       // Current (too thin)
//    const val Hair = "\u200A"       // Thinner
//    const val Quarter = "\u2005"    // 1/4 Em (Recommended)
//    const val Third = "\u2004"      // 1/3 Em (Wide)
//    const val Figure = "\u2007"     // Width of a number
//
//    // Config: Change this one line to swap them all
//    const val Default = Quarter
}
public const val sqrtSpacer: String = "\u2007"

internal object RichTextStateHtmlParser : RichTextStateParser<String> {

    @OptIn(ExperimentalRichTextApi::class)
    override fun encode(input: String): RichTextState {
        val openedTags = mutableListOf<Pair<String, Map<String, String>>>()
        val stringBuilder = StringBuilder()
        val richParagraphList = mutableListOf(RichParagraph())
        val lineBreakParagraphIndexSet = mutableSetOf<Int>()
        val toKeepEmptyParagraphIndexSet = mutableSetOf<Int>()
        var currentRichSpan: RichSpan? = null
        var currentListLevel = 0

        val handler = KsoupHtmlHandler
            .Builder()
            .onText {
                // In html text inside ul/ol tags is skipped
                val lastOpenedTag = openedTags.lastOrNull()?.first
                if (lastOpenedTag == "ul" || lastOpenedTag == "ol") return@onText

                if (lastOpenedTag in skippedHtmlElements) return@onText

                val rawText = it
                // 1. Hide the spacer by replacing it with a placeholder that won't be trimmed. This spacer has an important role in drawing the radical symbol.
                val placeholder = "@@MATH_GUARD@@"
                val protectedText = rawText.replace(sqrtSpacer, placeholder)

                val isInsidePre = openedTags.any { it.first == "pre" }

                val cleanedText = if (isInsidePre) {
                    protectedText
                } else {
                    removeHtmlTextExtraSpaces(
                        input = protectedText,
                        trimStart = stringBuilder.lastOrNull() == null || stringBuilder.lastOrNull()?.isWhitespace() == true || stringBuilder.lastOrNull() == '\n'
                    )
                }

                // 3. Restore the spacer and decode HTML entities
                var addedText = KsoupEntities.decodeHtml(
                    cleanedText.replace(placeholder, sqrtSpacer)
                )

                if (isInsidePre) {
                    addedText = addedText.replace("\n", "\u2028")
                }

                if (addedText.isEmpty()) return@onText

                stringBuilder.append(addedText)

                val currentRichParagraph = richParagraphList.last()
                val safeCurrentRichSpan = currentRichSpan ?: RichSpan(paragraph = currentRichParagraph)

                if (safeCurrentRichSpan.children.isEmpty()) {
                    safeCurrentRichSpan.text += addedText
                } else {
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.text = addedText
                    safeCurrentRichSpan.children.add(newRichSpan)
                }

                if (currentRichSpan == null) {
                    currentRichSpan = safeCurrentRichSpan
                    currentRichParagraph.children.add(safeCurrentRichSpan)
                }
            }
            .onOpenTag { name, attributes, _ ->
                val lastOpenedTag = openedTags.lastOrNull()?.first

                openedTags.add(name to attributes)

                if (name in skippedHtmlElements) {
                    return@onOpenTag
                }

                if (name == "ul" || name == "ol") {
                    // Todo: Apply ul/ol styling if exists
                    currentListLevel = currentListLevel + 1
                    return@onOpenTag
                }

                if (name == "body") {
                    stringBuilder.clear()
                    richParagraphList.clear()
                    richParagraphList.add(RichParagraph())
                    currentRichSpan = null
                }

                val cssStyleMap = attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                val cssSpanStyle = CssEncoder.parseCssStyleMapToSpanStyle(cssStyleMap)
                val tagSpanStyle = htmlElementsSpanStyleEncodeMap[name]

                val currentRichParagraph = richParagraphList.lastOrNull()
                val isCurrentRichParagraphBlank = currentRichParagraph?.isBlank() == true
                val isCurrentTagBlockElement = name in htmlBlockElements
                val isLastOpenedTagBlockElement = lastOpenedTag in htmlBlockElements

                // For <li> tags inside <ul> or <ol> tags
                if (
                    lastOpenedTag != null &&
                    isCurrentTagBlockElement &&
                    isLastOpenedTagBlockElement &&
                    name == "li" &&
                    currentRichParagraph != null &&
                    currentRichParagraph.type is DefaultParagraph &&
                    isCurrentRichParagraphBlank
                ) {
                    val paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag, currentListLevel)
                    currentRichParagraph.type = paragraphType

                    currentRichParagraph.paragraphStyle = resolveParagraphStyle(openedTags)
                }

                if (isCurrentTagBlockElement) {
                    val newRichParagraph =
                        if (isCurrentRichParagraphBlank)
                            currentRichParagraph
                        else
                            RichParagraph()

                    var paragraphType: ParagraphType = DefaultParagraph()
                    if (name == "li" && lastOpenedTag != null) {
                        paragraphType = encodeHtmlElementToRichParagraphType(lastOpenedTag, currentListLevel)
                    }

                    newRichParagraph.paragraphStyle = resolveParagraphStyle(openedTags)
                    newRichParagraph.type = paragraphType

                    if (!isCurrentRichParagraphBlank) {
                        stringBuilder.append(' ')

                        richParagraphList.add(newRichParagraph)
                    }

                    val newRichSpan = RichSpan(paragraph = newRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)

                    if (newRichSpan.spanStyle != SpanStyle()) {
                        currentRichSpan = newRichSpan
                        newRichParagraph.children.add(newRichSpan)
                    } else {
                        currentRichSpan = null
                    }
                } else if (name != BrElement) {
                    val richSpanStyle = encodeHtmlElementToRichSpanStyle(name, attributes)

                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph)
                    newRichSpan.spanStyle = cssSpanStyle.customMerge(tagSpanStyle)
                    newRichSpan.richSpanStyle = richSpanStyle

                    if (currentRichSpan != null) {
                        newRichSpan.parent = currentRichSpan
                        currentRichSpan?.children?.add(newRichSpan)
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                    }
                    currentRichSpan = newRichSpan
                } else {
                    // name == "br"
                    stringBuilder.append('\u2028')
                    
                    val currentRichParagraph = richParagraphList.last()
                    val newRichSpan = RichSpan(paragraph = currentRichParagraph, text = "\u2028")

                    if (currentRichSpan != null) {
                        newRichSpan.parent = currentRichSpan
                        currentRichSpan?.children?.add(newRichSpan)
                    } else {
                        currentRichParagraph.children.add(newRichSpan)
                    }
                    
                    if (currentRichParagraph.children.size == 1 && currentRichSpan == null) {
                        lineBreakParagraphIndexSet.add(richParagraphList.lastIndex)
                    }
                }
            }
            .onCloseTag { name, _ ->
                if (openedTags.isNotEmpty()) openedTags.removeAt(openedTags.lastIndex)

                val isCurrentTagBlockElement = name in htmlBlockElements && name != "li"

                if (isCurrentTagBlockElement) {
                    val currentPara = richParagraphList.lastOrNull()
                    if (currentPara != null && currentPara.children.isNotEmpty()) {
                        val lastSpan = currentPara.children.lastOrNull()
                        if (lastSpan != null && lastSpan.text == "\u2028" && lastSpan.children.isEmpty()) {
                            currentPara.children.removeAt(currentPara.children.lastIndex)
                        }
                    }
                }

                val isCurrentRichParagraphBlank = richParagraphList.lastOrNull()?.isBlank() == true

                if (isCurrentTagBlockElement) {
                    stringBuilder.append(' ')

                    val newParagraph = RichParagraph(paragraphStyle = resolveParagraphStyle(openedTags))

                    richParagraphList.add(newParagraph)

                    if (!isCurrentRichParagraphBlank) {
                        toKeepEmptyParagraphIndexSet.add(richParagraphList.lastIndex)
                    } else {
                        toKeepEmptyParagraphIndexSet.add(richParagraphList.lastIndex - 1)
                    }

                    currentRichSpan = null
                }

                if (name == "ul" || name == "ol") {
                    currentListLevel = (currentListLevel - 1).coerceAtLeast(0)
                    return@onCloseTag
                }

                if (name in skippedHtmlElements)
                    return@onCloseTag

                if (name != BrElement)
                    currentRichSpan = currentRichSpan?.parent
            }
            .build()

        val parser = KsoupHtmlParser(
            handler = handler
        )

        parser.write(input)
        parser.end()

        val initialLastIndex = richParagraphList.lastIndex
        for (i in initialLastIndex downTo 0) {
            // Keep empty paragraphs if they are line breaks <br> or by block html elements
            if (i in lineBreakParagraphIndexSet || (i != initialLastIndex && i in toKeepEmptyParagraphIndexSet))
                continue

            // Remove empty paragraphs
            if (richParagraphList[i].isBlank())
                richParagraphList.removeAt(i)
        }

        richParagraphList.forEach { richParagraph ->
            richParagraph.removeEmptyChildren()
        }

        return RichTextState(
            initialRichParagraphList = richParagraphList,
        )
    }

    private fun resolveParagraphStyle(openedTags: List<Pair<String, Map<String, String>>>): ParagraphStyle {
        var paragraphStyle = ParagraphStyle()
        val isList = openedTags.lastOrNull()?.first == "li"
        
        openedTags.fastForEach { (tagName, attributes) ->
            if (tagName in htmlBlockElements) {
                val cssStyleMap = attributes["style"]?.let { CssEncoder.parseCssStyle(it) } ?: emptyMap()
                val cssParagraphStyle = CssEncoder.parseCssStyleMapToParagraphStyle(cssStyleMap, attributes)
                paragraphStyle = paragraphStyle.merge(cssParagraphStyle)
            }
        }
        
        if (isList) {
            paragraphStyle = paragraphStyle.merge(ParagraphStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Start))
        }
        
        return paragraphStyle
    }

    override fun decode(richTextState: RichTextState): String {
        val builder = StringBuilder()

        val openedListTagNames = mutableListOf<String>()
        var lastParagraphGroupTagName: String? = null
        var lastParagraphGroupLevel = 0
        var isLastParagraphEmpty = false

        var currentListLevel = 0

        richTextState.richParagraphList.fastForEachIndexed { index, richParagraph ->
            val richParagraphType = richParagraph.type
            val isParagraphEmpty = richParagraph.isEmpty()
            val paragraphGroupTagName = decodeHtmlElementFromRichParagraphType(richParagraph.type)

            val paragraphLevel =
                if (richParagraphType is ConfigurableListLevel)
                    richParagraphType.level
                else
                    0

            val isParagraphList = paragraphGroupTagName in listOf("ol", "ul")
            val isLastParagraphList = lastParagraphGroupTagName in listOf("ol", "ul")

            fun isCloseParagraphGroup(): Boolean {
                if (!isLastParagraphList)
                    return false

                if (paragraphLevel > lastParagraphGroupLevel)
                    return false

                if (
                    lastParagraphGroupTagName == paragraphGroupTagName &&
                    paragraphLevel == lastParagraphGroupLevel
                )
                    return false

                return true
            }

            fun isCloseAllOpenedTags(): Boolean {
                if (isParagraphList)
                    return false

                if (!isLastParagraphList)
                    return false

                return true
            }

            fun isOpenParagraphGroup(): Boolean {
                if (!isParagraphList)
                    return false

                if (
                    isLastParagraphList &&
                    paragraphGroupTagName == openedListTagNames.lastOrNull() &&
                    paragraphLevel < lastParagraphGroupLevel
                )
                    return false

                if (
                    isLastParagraphList &&
                    paragraphLevel == lastParagraphGroupLevel &&
                    paragraphGroupTagName == lastParagraphGroupTagName
                )
                    return false

                return true
            }

            if (isCloseAllOpenedTags()) {
                openedListTagNames.fastForEachReversed {
                    builder.append("</$it>")
                }
                openedListTagNames.clear()
            } else if (isCloseParagraphGroup()) {
                // Close last paragraph group tag
                builder.append("</$lastParagraphGroupTagName>")
                if (openedListTagNames.isNotEmpty()) openedListTagNames.removeAt(openedListTagNames.lastIndex)

                // We can move from nested level: 3 to nested level: 1,
                // for this case we need to close more than one tag
                if (
                    isLastParagraphList &&
                    paragraphLevel < lastParagraphGroupLevel
                ) {
                    repeat(lastParagraphGroupLevel - paragraphLevel) {
                        if (openedListTagNames.isNotEmpty()){
                            openedListTagNames.removeAt(openedListTagNames.lastIndex)
                                .let { builder.append("</$it>") }
                        }
                    }
                }
            }

            if (isOpenParagraphGroup()) {
                builder.append("<$paragraphGroupTagName>")
                openedListTagNames.add(paragraphGroupTagName)
            }

            currentListLevel = paragraphLevel

            fun isLineBreak(): Boolean {
                if (!isParagraphEmpty)
                    return false

                if (isParagraphList && lastParagraphGroupTagName != paragraphGroupTagName)
                    return false

                return true
            }

            // Create paragraph tag name
            val paragraphTagName =
                if (paragraphGroupTagName == "ol" || paragraphGroupTagName == "ul") "li"
                else "p"

            // Add line break if the paragraph is empty
            if (isLineBreak()) {
                val skipAddingBr =
                    isLastParagraphEmpty && richParagraph.isEmpty() && index == richTextState.richParagraphList.lastIndex

                if (!skipAddingBr)
                    builder.append("<$paragraphTagName><$BrElement /></$paragraphTagName>")
            } else {
                // Create paragraph css
                val paragraphCssMap = CssDecoder.decodeParagraphStyleToCssStyleMap(richParagraph.paragraphStyle)
                val paragraphCss = CssDecoder.decodeCssStyleMap(paragraphCssMap)

                // Append paragraph opening tag
                builder.append("<$paragraphTagName")
                if (paragraphCss.isNotBlank()) builder.append(" style=\"$paragraphCss\"")
                builder.append(">")

                // Append paragraph children
                richParagraph.children.fastForEach { richSpan ->
                    if (richSpan.text == "\u2028") {
                        builder.append("<$BrElement />")
                    } else {
                        builder.append(decodeRichSpanToHtml(richSpan))
                    }
                }

                // Append paragraph closing tag
                builder.append("</$paragraphTagName>")
            }

            // Save last paragraph group tag name
            lastParagraphGroupTagName = paragraphGroupTagName
            lastParagraphGroupLevel = paragraphLevel

            isLastParagraphEmpty = isParagraphEmpty
        }

        // Close the remaining list tags
        openedListTagNames.fastForEachReversed {
            builder.append("</$it>")
        }
        openedListTagNames.clear()

        return builder.toString()
    }

    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeRichSpanToHtml(richSpan: RichSpan, parentFormattingTags: List<String> = emptyList()): String {
        val stringBuilder = StringBuilder()

        // Check if span is empty
        if (richSpan.isEmpty()) return ""

        // Get HTML element and attributes
        val spanHtml = decodeHtmlElementFromRichSpanStyle(richSpan.richSpanStyle)
        val tagName = spanHtml.first
        val tagAttributes = spanHtml.second

        // Convert attributes map to HTML string
        val tagAttributesStringBuilder = StringBuilder()
        tagAttributes.forEach { (key, value) ->
            tagAttributesStringBuilder.append(" $key=\"$value\"")
        }

        // Convert span style to CSS string
        val htmlStyleFormat = CssDecoder.decodeSpanStyleToHtmlStylingFormat(richSpan.spanStyle)
        val spanCss = CssDecoder.decodeCssStyleMap(htmlStyleFormat.cssStyleMap)
        val htmlTags = htmlStyleFormat.htmlTags.filter { it !in parentFormattingTags }

        val isRequireOpeningTag = tagName != "span" || tagAttributes.isNotEmpty() || spanCss.isNotEmpty()

        if (isRequireOpeningTag) {
            // Append HTML element with attributes and style
            stringBuilder.append("<$tagName$tagAttributesStringBuilder")
            if (spanCss.isNotEmpty()) stringBuilder.append(" style=\"$spanCss\"")
            stringBuilder.append(">")
        }

        htmlTags.forEach {
            stringBuilder.append("<$it>")
        }

        // Append text
        stringBuilder.append(
            richSpan.text.escapeHtmlEntities().replace("\u2028", "<br />")
        )

        // Append children
        richSpan.children.fastForEach { child ->
            stringBuilder.append(
                decodeRichSpanToHtml(
                    richSpan = child,
                    parentFormattingTags = parentFormattingTags + htmlTags,
                )
            )
        }

        htmlTags.reversed().forEach {
            stringBuilder.append("</$it>")
        }

        if (isRequireOpeningTag) {
            // Append closing HTML element
            stringBuilder.append("</$tagName>")
        }

        return stringBuilder.toString()
    }

    /**
     * Encodes HTML elements to [RichSpanStyle].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun encodeHtmlElementToRichSpanStyle(
        tagName: String,
        attributes: Map<String, String>,
    ): RichSpanStyle =
        when (tagName) {
            "a" ->
                RichSpanStyle.Link(url = attributes["href"].orEmpty())

            CodeSpanTagName, OldCodeSpanTagName ->
                RichSpanStyle.Code()

            "img" ->
                RichSpanStyle.Image(
                    model = attributes["src"].orEmpty(),
                    width = (attributes["width"]?.toIntOrNull() ?: 0).sp,
                    height = (attributes["height"]?.toIntOrNull() ?: 0).sp,
                    contentDescription = attributes["alt"] ?: ""
                )
                
            "inline" ->
                com.mohamedrejeb.richeditor.model.InlineContentSpanStyle(
                    id = attributes["id"].orEmpty()
                )

            "mark" -> {
                val style = attributes["style"]
                val color = if (style != null) {
                    val cssStyleMap = style.split(";")
                        .map { it.split(":") }
                        .filter { it.size == 2 }
                        .associate { it[0].trim() to it[1].trim() }
                    
                    val backgroundColor = cssStyleMap["background-color"] ?: cssStyleMap["background"]
                    backgroundColor?.let { parseCssColor(it) } ?: MarkBackgroundColor
                } else {
                    MarkBackgroundColor
                }
                RichSpanStyle.Mark(color = color)
            }

            else ->
                RichSpanStyle.Default
        }

    /**
     * Decodes HTML elements from [RichSpanStyle].
     */
    @OptIn(ExperimentalRichTextApi::class)
    private fun decodeHtmlElementFromRichSpanStyle(
        richSpanStyle: RichSpanStyle,
    ): Pair<String, Map<String, String>> =
        when (richSpanStyle) {
            is RichSpanStyle.Link ->
                "a" to mapOf(
                    "href" to richSpanStyle.url,
                    "target" to "_blank"
                )

            is RichSpanStyle.Code ->
                CodeSpanTagName to emptyMap()

            is RichSpanStyle.Image ->
                if (richSpanStyle.model is String)
                    "img" to mapOf(
                        "src" to richSpanStyle.model,
                        "width" to richSpanStyle.width.value.toString(),
                        "height" to richSpanStyle.height.value.toString(),
                    )
                else
                    "span" to emptyMap()
                    
            is com.mohamedrejeb.richeditor.model.InlineContentSpanStyle ->
                "inline" to mapOf(
                    "id" to richSpanStyle.id
                )

            is RichSpanStyle.Mark -> {
                val color = CssDecoder.decodeColorToCss(richSpanStyle.color)
                if (color.isNotBlank())
                    "mark" to mapOf("style" to "background-color: $color")
                else
                    "mark" to emptyMap()
            }

            else ->
                "span" to emptyMap()
        }
        
    private fun parseCssColor(colorString: String): Color {
        return try {
            if (colorString.startsWith("#")) {
                val hex = colorString.removePrefix("#")
                if (hex.length == 6) {
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    Color(r, g, b)
                } else if (hex.length == 8) {
                    val a = hex.substring(0, 2).toInt(16)
                    val r = hex.substring(2, 4).toInt(16)
                    val g = hex.substring(4, 6).toInt(16)
                    val b = hex.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                } else {
                    MarkBackgroundColor
                }
            } else if (colorString.startsWith("rgba")) {
                val parts = colorString.removePrefix("rgba(").removeSuffix(")").split(",")
                if (parts.size == 4) {
                    val r = parts[0].trim().toInt()
                    val g = parts[1].trim().toInt()
                    val b = parts[2].trim().toInt()
                    val a = parts[3].trim().toFloat()
                    Color(r, g, b, (a * 255).toInt())
                } else {
                    MarkBackgroundColor
                }
            } else if (colorString.startsWith("rgb")) {
                val parts = colorString.removePrefix("rgb(").removeSuffix(")").split(",")
                if (parts.size >= 3) {
                    val r = parts[0].trim().toInt()
                    val g = parts[1].trim().toInt()
                    val b = parts[2].trim().toInt()
                    Color(r, g, b)
                } else {
                    MarkBackgroundColor
                }
            } else {
                MarkBackgroundColor
            }
        } catch (e: Exception) {
            MarkBackgroundColor
        }
    }

    /**
     * Encodes HTML elements to [ParagraphType].
     */
    private fun encodeHtmlElementToRichParagraphType(
        tagName: String,
        listLevel: Int,
    ): ParagraphType {
        return when (tagName) {
            "ul" -> UnorderedList(initialLevel = listLevel)
            "ol" -> OrderedList(number = 1, initialLevel = listLevel)
            else -> DefaultParagraph()
        }
    }

    /**
     * Decodes HTML elements from [ParagraphType].
     */
    private fun decodeHtmlElementFromRichParagraphType(
        richParagraphType: ParagraphType,
    ): String {
        return when (richParagraphType) {
            is UnorderedList -> "ul"
            is OrderedList -> "ol"
            else -> "p"
        }
    }

}

/**
 * Encodes HTML elements to [SpanStyle].
 *
 * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
 */
internal val htmlElementsSpanStyleEncodeMap = mapOf(
    "b" to BoldSpanStyle,
    "strong" to BoldSpanStyle,
    "i" to ItalicSpanStyle,
    "em" to ItalicSpanStyle,
    "u" to UnderlineSpanStyle,
    "ins" to UnderlineSpanStyle,
    "s" to StrikethroughSpanStyle,
    "strike" to StrikethroughSpanStyle,
    "del" to StrikethroughSpanStyle,
    "sub" to SubscriptSpanStyle,
    "sup" to SuperscriptSpanStyle,
    "sup" to SuperscriptSpanStyle,
    // "mark" to MarkSpanStyle, // Handled by RichSpanStyle
    "small" to SmallSpanStyle,
    "h1" to H1SpanStyle,
    "h2" to H2SpanStyle,
    "h3" to H3SpanStyle,
    "h4" to H4SpanStyle,
    "h5" to H5SpanStyle,
    "h6" to H6SpanStyle,
    "sqrt" to SqrtSpanStyle,
    "overline" to OverlineSpanStyle,
    "double-u" to DoubleUnderScoreSpanStyle
)

/**
 * Decodes HTML elements from [SpanStyle].
 *
 * @see <a href="https://www.w3schools.com/html/html_formatting.asp">HTML formatting</a>
 */
internal val htmlElementsSpanStyleDecodeMap = mapOf(
    BoldSpanStyle to "b",
    ItalicSpanStyle to "i",
    UnderlineSpanStyle to "u",
    StrikethroughSpanStyle to "s",
    SubscriptSpanStyle to "sub",
    SuperscriptSpanStyle to "sup",
    SuperscriptSpanStyle to "sup",
    // MarkSpanStyle to "mark", // Handled by RichSpanStyle
    SmallSpanStyle to "small",
    H1SpanStyle to "h1",
    H2SpanStyle to "h2",
    H3SpanStyle to "h3",
    H4SpanStyle to "h4",
    H5SpanStyle to "h5",
    H6SpanStyle to "h6",
)

internal const val CodeSpanTagName = "code"
internal const val OldCodeSpanTagName = "code-span"

private fun String.escapeHtmlEntities(): String {
    return this.replace("&", "&amp;")
               .replace("<", "&lt;")
               .replace(">", "&gt;")
               .replace("\"", "&quot;")
               .replace("'", "&#39;")
}