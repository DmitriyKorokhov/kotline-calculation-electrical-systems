package feature.projecteditor.ui.labels // или твой пакет

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.domain.CalloutNode
import feature.projecteditor.domain.ProjectNode
import feature.projecteditor.domain.TextNode
import kotlin.math.roundToInt

@Composable
fun AnnotationTextEditor(
    node: ProjectNode,
    screenPos: Offset,
    scale: Float,
    editingText: String,
    onEditingTextChanged: (String) -> Unit,
    onFinishEdit: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    // Достаем свойства в зависимости от типа узла
    val fontSize = (node as? TextNode)?.fontSize ?: (node as? CalloutNode)?.fontSize ?: 14f
    val colorArgb = (node as? TextNode)?.colorArgb ?: (node as? CalloutNode)?.colorArgb ?: 0xFFFFFFFF
    val isBold = (node as? TextNode)?.isBold ?: (node as? CalloutNode)?.isBold ?: false
    val isItalic = (node as? TextNode)?.isItalic ?: (node as? CalloutNode)?.isItalic ?: false
    val isUnderline = (node as? TextNode)?.isUnderline ?: (node as? CalloutNode)?.isUnderline ?: false
    val isStrikethrough = (node as? TextNode)?.isStrikethrough ?: (node as? CalloutNode)?.isStrikethrough ?: false
    val align = (node as? TextNode)?.align ?: 0 // У Callout всегда 0 (слева)
    val hasBackground = (node as? TextNode)?.hasBackground ?: (node as? CalloutNode)?.hasBackground ?: false
    val backgroundColorArgb = (node as? TextNode)?.backgroundColorArgb ?: (node as? CalloutNode)?.backgroundColorArgb ?: 0xFFFFFFFF

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = editingText, selection = TextRange(editingText.length)))
    }

    Layout(
        content = {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onEditingTextChanged(newValue.text)
                },
                textStyle = TextStyle(
                    color = Color(colorArgb),
                    fontSize = (fontSize * scale).sp,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = TextDecoration.combine(
                        listOfNotNull(
                            if (isUnderline) TextDecoration.Underline else null,
                            if (isStrikethrough) TextDecoration.LineThrough else null
                        )
                    ),
                    textAlign = when (align) {
                        1 -> TextAlign.Center
                        2 -> TextAlign.Right
                        else -> TextAlign.Left
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .background(
                        color = if (hasBackground) Color(backgroundColorArgb) else Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(1.dp, Color(0xFF9C27B0), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            if (event.key == Key.Enter) {
                                if (event.isShiftPressed) {
                                    // Вставляем перенос строки в позицию курсора
                                    val text = textFieldValue.text
                                    val selection = textFieldValue.selection
                                    val newText = text.substring(0, selection.start) + "\n" + text.substring(selection.end)
                                    val newSelection = TextRange(selection.start + 1)

                                    textFieldValue = TextFieldValue(text = newText, selection = newSelection)
                                    onEditingTextChanged(newText)
                                    return@onPreviewKeyEvent true
                                }

                                onFinishEdit()
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.Escape) {
                                onFinishEdit()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
            )
        }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                x = screenPos.x.roundToInt() - placeable.width / 2,
                y = screenPos.y.roundToInt() - placeable.height / 2
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}