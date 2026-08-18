package feature.projecteditor.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RightSideNameText(
    name: String,
    screenPos: Offset,
    nodeWidthOnScreen: Float,
    nodeHeight: Float,
    scale: Float,
    isEditing: Boolean = false,
    editingText: String = "",
    onEditingTextChanged: (String) -> Unit = {},
    onStartEdit: () -> Unit = {},
    onFinishEdit: () -> Unit = {}
) {
    val density = LocalDensity.current
    val gap = 15f * scale
    val offsetX = with(density) { (screenPos.x + nodeWidthOnScreen / 2f + gap).toDp() }

    val minBoxHeight = (40f * scale).dp
    val offsetY = with(density) { screenPos.y.toDp() } - (minBoxHeight / 2)

    val fontSize = (14f * scale).sp

    // 1. Фикс бага: всегда захватываем самую свежую функцию onStartEdit с актуальным именем
    val currentOnStartEdit by rememberUpdatedState(onStartEdit)

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .defaultMinSize(minHeight = minBoxHeight)
            .widthIn(max = (300f * scale).dp)
            // 2. pointerInput(name) пересоздает детектор, если имя модели изменилось
            .pointerInput(name) {
                detectTapGestures(onDoubleTap = { currentOnStartEdit() })
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (isEditing) {
            val focusRequester = remember { FocusRequester() }

            // 3. Форма с закругленными углами малого радиуса
            val shape = RoundedCornerShape(4.dp)

            BasicTextField(
                value = editingText,
                onValueChange = onEditingTextChanged,
                textStyle = TextStyle(
                    color = MaterialTheme.colors.onSurface,
                    fontSize = fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    // 4. Очень прозрачный белый фон (alpha = 0.2f означает прозрачность 80%)
                    .background(Color.White.copy(alpha = 0.2f), shape)
                    // 5. Синие края рамки с закруглениями
                    .border(1.dp, MaterialTheme.colors.primary, shape)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            if (event.key == Key.Enter) {
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

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        } else {
            Text(
                text = name,
                color = MaterialTheme.colors.onSurface,
                fontSize = fontSize,
                lineHeight = (16f * scale).sp,
                textAlign = TextAlign.Start,
                softWrap = true,
                maxLines = 2
            )
        }
    }
}