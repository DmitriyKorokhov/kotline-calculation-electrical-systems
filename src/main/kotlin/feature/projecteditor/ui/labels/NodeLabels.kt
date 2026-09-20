package feature.projecteditor.ui.labels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.domain.AnchorSide

@Composable
fun NodeLabelText(
    name: String,
    nodePosScreen: Offset, // Координаты ЦЕНТРА модели на экране
    nodeWidthOnScreen: Float,
    nodeHeightOnScreen: Float,
    scale: Float,
    labelSide: AnchorSide, // Текущая сторона привязки
    isEditing: Boolean = false,
    editingText: String = "",
    onEditingTextChanged: (String) -> Unit = {},
    onStartEdit: () -> Unit = {},
    onFinishEdit: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: (Offset) -> Unit = {}
) {
    val density = LocalDensity.current

    // ИСПРАВЛЕНИЕ 2: Разные отступы для боков и верха/низа
    val horizontalGap = 15f * scale
    val verticalGap = 4f * scale

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val boxWidth = boxSize.width.toFloat()
    val boxHeight = boxSize.height.toFloat()

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragTouchLocalPos by remember { mutableStateOf(Offset.Zero) } // Сохраняем координату курсора

    val currentOnStartEdit by rememberUpdatedState(onStartEdit)

    val baseX = when (labelSide) {
        AnchorSide.LEFT -> nodePosScreen.x - nodeWidthOnScreen / 2f - horizontalGap - boxWidth
        AnchorSide.RIGHT -> nodePosScreen.x + nodeWidthOnScreen / 2f + horizontalGap
        AnchorSide.TOP -> nodePosScreen.x - boxWidth / 2f
        AnchorSide.BOTTOM -> nodePosScreen.x - boxWidth / 2f
    }

    val baseY = when (labelSide) {
        AnchorSide.LEFT -> nodePosScreen.y - boxHeight / 2f
        AnchorSide.RIGHT -> nodePosScreen.y - boxHeight / 2f
        AnchorSide.TOP -> nodePosScreen.y - nodeHeightOnScreen / 2f - verticalGap - boxHeight
        AnchorSide.BOTTOM -> nodePosScreen.y + nodeHeightOnScreen / 2f + verticalGap
    }

    val offsetX = with(density) { (baseX + dragOffset.x).toDp() }
    val offsetY = with(density) { (baseY + dragOffset.y).toDp() }
    val fontSize = (14f * scale).sp
    val minBoxHeight = (40f * scale).dp

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .defaultMinSize(minHeight = minBoxHeight)
            .widthIn(max = (300f * scale).dp)
            .onSizeChanged { boxSize = it }
            .graphicsLayer { alpha = if (isDragging) 0.5f else 1f }
            .pointerInput(name) {
                detectTapGestures(onDoubleTap = { currentOnStartEdit() })
            }
            .pointerInput(name) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragTouchLocalPos = offset // Запоминаем, за какую часть текста схватились
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        // ИСПРАВЛЕНИЕ 1: Высчитываем АБСОЛЮТНУЮ координату курсора мыши на экране при отпускании!
                        val pointerScreenX = baseX + dragOffset.x + dragTouchLocalPos.x
                        val pointerScreenY = baseY + dragOffset.y + dragTouchLocalPos.y
                        onDragEnd(Offset(pointerScreenX, pointerScreenY))
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        dragTouchLocalPos = change.position // Обновляем координату курсора
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isEditing) {
            val focusRequester = remember { FocusRequester() }
            val shape = RoundedCornerShape(4.dp)

            BasicTextField(
                value = editingText,
                onValueChange = onEditingTextChanged,
                textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = fontSize),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .background(Color.White.copy(alpha = 0.8f), shape)
                    .border(1.dp, MaterialTheme.colors.primary, shape)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.Escape)) {
                            onFinishEdit()
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            Text(
                text = name,
                color = MaterialTheme.colors.onSurface,
                fontSize = fontSize,
                lineHeight = (16f * scale).sp,
                textAlign = when (labelSide) {
                    AnchorSide.LEFT -> TextAlign.End
                    AnchorSide.RIGHT -> TextAlign.Start
                    else -> TextAlign.Center
                },
                softWrap = true,
                maxLines = 2
            )
        }
    }
}