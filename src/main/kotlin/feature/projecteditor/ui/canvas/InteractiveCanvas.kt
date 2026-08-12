package feature.projecteditor.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectCanvasState
import feature.projecteditor.ui.drawGridHeaders
import feature.projecteditor.ui.toOffset
import feature.projecteditor.ui.toPoint
import feature.projecteditor.ui.drawProjectCanvas
import feature.projecteditor.ui.labels.RightSideNameText
import feature.shieldeditor.state.ShieldStorage

private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveCanvas(
    state: ProjectCanvasState,
    textMeasurer: TextMeasurer,
    onOpenShield: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragTarget by remember { mutableStateOf<Any?>(null) }
    var nodeDragStartOffset by remember { mutableStateOf(Point.Zero) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        if (node is ShieldNode) onOpenShield(node.id)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        if (event.type == PointerEventType.Scroll) {
                            state.onZoom(event.changes.first().scrollDelta.y, position.toPoint())
                            event.changes.first().consume()
                        }
                        if (event.type == PointerEventType.Press) {
                            val pressedNode = state.findNodeAtScreenPosition(position.toPoint())
                            if (event.buttons.isSecondaryPressed) {
                                state.contextMenuPosition = position.toPoint()
                                state.selectedNode = pressedNode
                                if (pressedNode != null) state.showNodeContextMenu = true else state.showCanvasContextMenu = true
                            } else if (event.buttons.isPrimaryPressed) {
                                if (state.connectingFromNodeId != null) state.tryFinishConnecting(pressedNode)
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val node = state.findNodeAtScreenPosition(position.toPoint())
                        if (node != null) {
                            state.saveHistory()
                            dragTarget = node.id
                            nodeDragStartOffset = node.position - state.screenToWorld(position.toPoint())
                        } else {
                            dragTarget = "Canvas"
                        }
                    },
                    onDragEnd = { (dragTarget as? Int)?.let { state.snapNodeToEndPosition(it) }; dragTarget = null },
                    onDragCancel = { dragTarget = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (val target = dragTarget) {
                            is Int -> state.updateNodePosition(target, state.screenToWorld(change.position.toPoint()) + nodeDragStartOffset)
                            is String -> state.onPan(dragAmount.toPoint())
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawProjectCanvas(textMeasurer, state)
            drawGridHeaders(textMeasurer, state)
        }

        // УНИФИЦИРОВАННАЯ ОТРИСОВКА ПОДПИСЕЙ (Всегда справа)
        state.nodes.forEach { node ->
            if (node.name.isNotBlank() || node is ShieldNode) {
                val screenPos = state.worldToScreen(node.position).toOffset()
                val scale = state.scale
                val nodeHeight = NODE_HEIGHT * scale

                // Вычисляем ширину узла для правильного отступа текста
                val nodeWidthForLabel = when (node) {
                    is TransformerNode -> node.radiusOuter * 2f
                    is GeneratorNode -> node.radius * 2f
                    is BatteryNode -> NODE_WIDTH * 0.5f
                    is UpsNode -> NODE_HEIGHT
                    is SolarPanelNode -> NODE_WIDTH * 0.8f
                    else -> NODE_WIDTH
                }

                val displayName = if (node is ShieldNode) ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name } else node.name

                RightSideNameText(displayName, screenPos, nodeWidthForLabel * scale, nodeHeight, scale)
            }
        }
    }
}