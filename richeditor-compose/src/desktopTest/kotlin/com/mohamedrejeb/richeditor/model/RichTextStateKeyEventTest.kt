package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class, InternalComposeUiApi::class,
    ExperimentalRichTextApi::class
)
class RichTextStateKeyEventTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testOnPreviewKeyEventWithTab() = runDesktopComposeUiTest {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        scene.setContent {
            state.selection = TextRange(11)
            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        waitForIdle()
        // Simulate pressing Tab key
        scene.sendKeyEvent(
            keyEvent = KeyEvent(
                type = KeyEventType.KeyDown,
                key = Key.Tab,
            )
        )
        waitForIdle()

        val secondParagraphType = state.richParagraphList[1].type as OrderedList
        assertEquals(1, secondParagraphType.number)
        assertEquals(2, secondParagraphType.level)
    }

    @Test
    fun testOnPreviewKeyEventWithShiftTab() = runDesktopComposeUiTest {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        scene.setContent {
            state.selection = TextRange(11)
            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        waitForIdle()

        // Simulate pressing Shift+Tab
        scene.sendKeyEvent(
            keyEvent = KeyEvent(
                type = KeyEventType.KeyDown,
                key = Key.Tab,
                isShiftPressed = true
            )
        )
        waitForIdle()

        val paragraphType = state.richParagraphList[1].type as OrderedList
        assertEquals(2, paragraphType.number)
        assertEquals(1, paragraphType.level)
    }

    @Test
    fun testOnPreviewKeyEventTabWithNoList() = runDesktopComposeUiTest {
        lateinit var state: RichTextState

        scene.setContent {
            state = remember { RichTextState() }

            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        scene.sendKeyEvent(
            keyEvent = KeyEvent(
                type = KeyEventType.KeyDown,
                key = Key.Tab
            )
        )
        waitForIdle()

        val paragraphType = state.richParagraphList[0].type
        assertFalse(paragraphType is OrderedList)
    }

    @Test
    fun testOnPreviewKeyEventEnterInList() = runDesktopComposeUiTest {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        var interceptedCount = 0
        state.onInterceptEnter = { cursorIndex ->
            val textIndex = if (cursorIndex > 0) cursorIndex - 1 else -1
            val isCurrentItemEmpty = state.isCurrentListItemEmpty(textIndex)
            if (state.isList && !isCurrentItemEmpty) {
                false
            } else {
                interceptedCount++
                true
            }
        }

        scene.setContent {
            state.selection = TextRange(state.annotatedString.text.length)
            val focusRequester = remember { FocusRequester() }

            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        waitForIdle()

        // Press Enter on non-empty list item -> Should NOT intercept, should create 2nd list item
        scene.sendKeyEvent(
            keyEvent = KeyEvent(
                type = KeyEventType.KeyDown,
                key = Key.Enter,
            )
        )
        waitForIdle()

        assertEquals(0, interceptedCount)
        assertEquals(2, state.richParagraphList.size)
        assertEquals(true, state.isList)
        assertEquals(true, state.isCurrentListItemEmpty())

        // Press Enter on the newly created empty list item -> Should intercept!
        scene.sendKeyEvent(
            keyEvent = KeyEvent(
                type = KeyEventType.KeyDown,
                key = Key.Enter,
            )
        )
        waitForIdle()

        assertEquals(1, interceptedCount)
        // Paragraph list still has 2 items because enter was intercepted (handled by block splitter)
        assertEquals(2, state.richParagraphList.size)
    }
}

