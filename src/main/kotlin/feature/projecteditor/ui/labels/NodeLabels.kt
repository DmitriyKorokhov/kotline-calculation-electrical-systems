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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.domain.AnchorSide
import feature.projecteditor.state.ProjectCanvasState
import kotlin.math.roundToInt

@Composable
fun NodeLabelText(
    name: String,
    nodeBoundsWorld: Rect,
    state: ProjectCanvasState,
    labelSide: AnchorSide,
    isEditing: Boolean = false,
    editingText: String = "",
    onEditingTextChanged: (String) -> Unit = {},
    onStartEdit: () -> Unit = {},
    onFinishEdit: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDragEnd: (Offset) -> Unit = {}
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val currentOnStartEdit by rememberUpdatedState(onStartEdit)
    val fontSize = (14f * state.scale).sp

    Box(
        modifier = Modifier
            .offset {
                val scale = state.scale
                val canvasOffset = state.offset

                val leftScreen = nodeBoundsWorld.left * scale + canvasOffset.x
                val rightScreen = nodeBoundsWorld.right * scale + canvasOffset.x
                val topScreen = nodeBoundsWorld.top * scale + canvasOffset.y
                val bottomScreen = nodeBoundsWorld.bottom * scale + canvasOffset.y

                val centerScreenX = (leftScreen + rightScreen) / 2f
                val centerScreenY = (topScreen + bottomScreen) / 2f

                val hGap = 15f * scale
                val vGap = 6f * scale

                val bx = when (labelSide) {
                    AnchorSide.LEFT -> leftScreen - hGap - boxSize.width
                    AnchorSide.RIGHT -> rightScreen + hGap
                    AnchorSide.TOP -> centerScreenX - boxSize.width / 2f
                    AnchorSide.BOTTOM -> centerScreenX - boxSize.width / 2f
                }

                val by = when (labelSide) {
                    AnchorSide.LEFT -> centerScreenY - boxSize.height / 2f
                    AnchorSide.RIGHT -> centerScreenY - boxSize.height / 2f
                    AnchorSide.TOP -> topScreen - vGap - boxSize.height
                    AnchorSide.BOTTOM -> bottomScreen + vGap
                }

                IntOffset(
                    (bx + dragOffset.x).roundToInt(),
                    (by + dragOffset.y).roundToInt()
                )
            }
            .defaultMinSize(minHeight = (40f * state.scale).dp)
            .widthIn(max = (300f * state.scale).dp)
            .onSizeChanged { boxSize = it }
            .graphicsLayer { alpha = if (isDragging) 0.5f else 1f }
            .pointerInput(name) {
                detectTapGestures(onDoubleTap = { currentOnStartEdit() })
            }
            .pointerInput(name) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false

                        val scale = state.scale
                        val canvasOffset = state.offset
                        val leftScreen = nodeBoundsWorld.left * scale + canvasOffset.x
                        val rightScreen = nodeBoundsWorld.right * scale + canvasOffset.x
                        val topScreen = nodeBoundsWorld.top * scale + canvasOffset.y
                        val bottomScreen = nodeBoundsWorld.bottom * scale + canvasOffset.y

                        val centerScreenX = (leftScreen + rightScreen) / 2f
                        val centerScreenY = (topScreen + bottomScreen) / 2f

                        val hGap = 15f * scale
                        val vGap = 6f * scale

                        // 1. Узнаем исходную позицию левого верхнего угла ярлыка
                        val bx = when (labelSide) {
                            AnchorSide.LEFT -> leftScreen - hGap - boxSize.width
                            AnchorSide.RIGHT -> rightScreen + hGap
                            AnchorSide.TOP -> centerScreenX - boxSize.width / 2f
                            AnchorSide.BOTTOM -> centerScreenX - boxSize.width / 2f
                        }

                        val by = when (labelSide) {
                            AnchorSide.LEFT -> centerScreenY - boxSize.height / 2f
                            AnchorSide.RIGHT -> centerScreenY - boxSize.height / 2f
                            AnchorSide.TOP -> topScreen - vGap - boxSize.height
                            AnchorSide.BOTTOM -> bottomScreen + vGap
                        }

                        // 2. Вычисляем текущую позицию ЦЕНТРА перетаскиваемого ярлыка
                        val currentBx = bx + dragOffset.x
                        val currentBy = by + dragOffset.y
                        val textCenterX = currentBx + boxSize.width / 2f
                        val textCenterY = currentBy + boxSize.height / 2f

                        // 3. Отдаем эти координаты в InteractiveCanvas -> ProjectCanvasState
                        onDragEnd(Offset(textCenterX, textCenterY))
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
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
                lineHeight = (16f * state.scale).sp,
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