package com.mohamedrejeb.richeditor.math

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.mohamedrejeb.richeditor.gesture.detectTapGestures
import com.mohamedrejeb.richeditor.model.ImageLoader
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.drawRichSpanStyle


@Composable
public fun MathRichText(
    state: RichTextState,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onBackground,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    imageLoader: ImageLoader = LocalImageLoader.current,
) {
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val pointerIcon = remember {
        mutableStateOf(PointerIcon.Default)
    }

    // Merge it into the style for the Editor
    val mergedTextStyle = style.merge(TextStyle(color = color))

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val text = remember(
        state.visualTransformation,
        state.annotatedString,
    ) {
        state.visualTransformation.filter(state.annotatedString).text
    }

    CompositionLocalProvider(
        LocalImageLoader provides imageLoader
    ) {
        BasicText(
            text = text,
            modifier = modifier
                .drawRichSpanStyle(state)
                .pointerHoverIcon(pointerIcon.value)
                .pointerInput(state) {
                    try {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position
                            val isLink = state.isLink(position)

                            pointerIcon.value =
                                if (isLink)
                                    PointerIcon.Hand
                                else
                                    PointerIcon.Default
                        }
                    } catch (_: Exception) {

                    }
                }
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = { offset ->
                            state.getLinkByOffset(offset)?.let { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        consumeDown = { offset ->
                            state.isLink(offset)
                        },
                    )
                }
                .drawMathMarkers(
                    textLayoutResult = layoutResult,
                    annotatedString = state.annotatedString,
                    color = color
                )
                .onPreviewKeyEvent { event ->
                    // Disable Ctrl+Z (Windows/Linux) and Cmd+Z (Mac)
                    if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Z) {
                        return@onPreviewKeyEvent true // Consume the event (do nothing)
                    }
                    false
                },
            style = mergedTextStyle,
            onTextLayout = {
                state.onTextLayout(
                    textLayoutResult = it,
                    density = density,
                )
                onTextLayout(it)
                layoutResult = it
            },
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            inlineContent = remember(inlineContent, state.inlineContentMap.toMap()) {
                inlineContent + state.inlineContentMap
            }
        )
    }
}