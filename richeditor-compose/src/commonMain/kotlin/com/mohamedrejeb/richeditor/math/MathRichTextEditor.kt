package com.matura.common_core.ui.utils.RichText

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.mohamedrejeb.richeditor.math.drawMathMarkers
import com.mohamedrejeb.richeditor.math.isSqrtMarker
import com.mohamedrejeb.richeditor.math.mergeRanges
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.parser.html.sqrtSpacer
import com.mohamedrejeb.richeditor.parser.utils.SqrtSpanStyle
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
public fun MathRichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
) {

    // 1. RESOLVE THE CORRECT THEME COLOR
    // 'LocalContentColor' matches your Theme (White in Dark Mode, Black in Light Mode).
    // We use it as the default if 'textStyle.color' is Unspecified.
    val defaultColor = if (textStyle.color == Color.Unspecified) {
        MaterialTheme.colorScheme.onBackground
    } else {
        textStyle.color
    }

    // Merge it into the style for the Editor
    val mergedTextStyle = textStyle.merge(TextStyle(color = defaultColor))

    // We can extract the effective color to pass to the math drawer if we want precise matching
    // (Or let the drawer use LocalContentColor as discussed previously)

    // 2. State for Layout & Watchdog
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { state.annotatedString }
            .collect { annotatedString ->
                MathGuardsProcessor.processMathGuards(
                    annotatedString = annotatedString,
                    onRemoveSpanStyle = { range ->
                        state.removeSpanStyle(SqrtSpanStyle, range)
                    },
                    onAddSpanStyle = { range ->
                        state.addSpanStyle(SqrtSpanStyle, range)
                    },
                    onUpdateSelection = { range ->
                        state.selection = range
                    },
                    onAddTextAtIndex = { index, text ->
                        state.addTextAtIndex(index, text)
                    }
                )
            }
    }



    // 3. The Composition
    BasicRichTextEditor(
        state = state,
        modifier = modifier
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight
            ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        onTextLayout = { layoutResult = it },
        cursorBrush = SolidColor(if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            // --- THE MAGIC NESTING ---

            // Layer A: The Material Decoration (Border, Label, Padding)
            OutlinedTextFieldDefaults.DecorationBox(
                value = state.annotatedString.text,
                innerTextField = {
                    // Layer B: The Math Drawer (Coordinate Space = Content Area)
                    // This Box sits *inside* the padding of the OutlinedTextField.
                    Box(
                        modifier = Modifier
                            //.fillMaxWidth().border(BorderStroke(2.dp, Color.Red))
                            .drawMathMarkers(
                                textLayoutResult = layoutResult,
                                annotatedString = state.annotatedString,
                                color = defaultColor
                            ),
                        propagateMinConstraints = true
                    ) {
                        // Layer C: The Actual Text
                        innerTextField()
                    }
                },
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isError,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                colors = colors,
                contentPadding = contentPadding,
                container = {
                    OutlinedTextFieldDefaults.ContainerBox(
                        enabled,
                        isError,
                        interactionSource,
                        colors,
                        shape
                    )
                }
            )
        }
    )
}

// Helper to access colors inside the composable (required for cursor brush above)
@Composable
private fun TextFieldColors.cursorColor(isError: Boolean): androidx.compose.runtime.State<Color> {
    return androidx.compose.runtime.rememberUpdatedState(
        if (isError) this.cursorColor else this.cursorColor
    )
}