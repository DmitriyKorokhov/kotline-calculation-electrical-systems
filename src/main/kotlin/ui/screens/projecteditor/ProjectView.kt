package ui.screens.projecteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.PowerSourceNode
import data.ShieldNode

// Константы размеров для позиционирования текста
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH

/**
 * --- ИСПРАВЛЕНО ---
 * 1. Функция теперь принимает 'state' как параметр.
 * 2. Удален вызов несуществующей функции rememberProjectCanvasState().
 */
@Composable
fun ProjectView(
    state: ProjectCanvasState,
    onOpenShield: (shieldId: Int) -> Unit
) {
    var dragTarget by remember { mutableStateOf<Any?>(null) } // Может быть ID (Int) или "Canvas" (String)
    var nodeDragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position

                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.first().scrollDelta.y
                            state.onZoom(scrollDelta, position)
                            event.changes.first().consume()
                        }

                        if (event.type == PointerEventType.Press) {
                            val pressedNode = state.findNodeAtScreenPosition(position)
                            if (event.buttons.isSecondaryPressed) {
                                state.contextMenuPosition = position
                                state.selectedNode = pressedNode
                                if (pressedNode != null) {
                                    state.showNodeContextMenu = true
                                } else {
                                    state.showCanvasContextMenu = true
                                }
                            } else if (event.buttons.isPrimaryPressed) {
                                if (state.connectingFromNodeId != null) {
                                    state.tryFinishConnecting(pressedNode)
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val node = state.findNodeAtScreenPosition(position)
                        if (node != null) {
                            dragTarget = node.id
                            nodeDragStartOffset = node.position - state.screenToWorld(position)
                        } else {
                            dragTarget = "Canvas"
                        }
                    },
                    onDragEnd = {
                        val nodeId = dragTarget as? Int
                        nodeId?.let { state.snapNodeToEndPosition(it) }
                        dragTarget = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        when (val target = dragTarget) {
                            is Int -> {
                                val newWorldPos = state.screenToWorld(change.position) + nodeDragStartOffset
                                state.updateNodePosition(target, newWorldPos)
                            }
                            is String -> {
                                state.onPan(dragAmount)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawProjectCanvas(state)
        }

        state.nodes.forEach { node ->
            if (node.name.isNotBlank()) {
                val screenPos = state.worldToScreen(node.position)
                val scale = state.scale
                val nodeHeight = if (node is PowerSourceNode) getNodeHeight(node) * scale else NODE_HEIGHT * scale
                if (node is ShieldNode) {
                    NodeNameText(node.name, screenPos, NODE_WIDTH * scale, nodeHeight)
                } else if (node is PowerSourceNode) {
                    PowerSourceNameText(node.name, screenPos, POWER_SOURCE_WIDTH * scale, nodeHeight, scale)
                }
            }
        }

        NodeContextMenu(state, onOpenShield)
        CanvasContextMenu(state)
        RenameNodeDialog(state)

        val statusText = if (state.connectingFromNodeId != null) "Выберите объект для соединения..." else null
        if (statusText != null) {
            Text(
                text = statusText,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
private fun NodeNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float) {
    Box(
        modifier = Modifier
            .offset(
                (screenPos.x - nodeWidth / 2).dp,
                (screenPos.y - nodeHeight / 2).dp
            )
            .size(width = nodeWidth.dp, height = nodeHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = MaterialTheme.colors.onSurface,
            fontSize = (14 * LocalDensity.current.fontScale).sp,
            textAlign = TextAlign.Center,
            softWrap = true
        )
    }
}

@Composable
private fun PowerSourceNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float, scale: Float) {
    Text(
        text = name,
        modifier = Modifier
            .offset(
                (screenPos.x - nodeWidth / 2).dp,
                (screenPos.y - nodeHeight / 2 - (25 * scale)).dp
            ),
        color = MaterialTheme.colors.onSurface,
        fontSize = (14 * LocalDensity.current.fontScale).sp
    )
}

@Composable
private fun NodeContextMenu(state: ProjectCanvasState, onOpenShield: (shieldId: Int) -> Unit) {
    DropdownMenu(
        expanded = state.showNodeContextMenu,
        onDismissRequest = { state.showNodeContextMenu = false },
        offset = DpOffset(state.contextMenuPosition.x.dp, state.contextMenuPosition.y.dp)
    ) {
        DropdownMenuItem(onClick = { state.showRenameDialog = true; state.showNodeContextMenu = false }) { Text("Изменить название") }
        if (state.selectedNode is ShieldNode) {
            DropdownMenuItem(onClick = { state.selectedNode?.let { onOpenShield(it.id) }; state.showNodeContextMenu = false }) { Text("Открыть") }
        }
        DropdownMenuItem(onClick = { state.startConnecting() }) { Text("Соединить") }
        DropdownMenuItem(onClick = { state.deleteSelectedNode() }) { Text("Удалить") }
    }
}

@Composable
private fun CanvasContextMenu(state: ProjectCanvasState) {
    DropdownMenu(
        expanded = state.showCanvasContextMenu,
        onDismissRequest = { state.showCanvasContextMenu = false },
        offset = DpOffset(state.contextMenuPosition.x.dp, state.contextMenuPosition.y.dp)
    ) {
        val worldPos = state.screenToWorld(state.contextMenuPosition)
        DropdownMenuItem(onClick = { state.addShieldNode(worldPos) }) { Text("Добавить щит") }
        DropdownMenuItem(onClick = { state.addPowerSourceNode(worldPos) }) { Text("Добавить ист. питания") }
        DropdownMenuItem(onClick = { state.addLevelLine(worldPos) }) { Text("Добавить уровень") }
    }
}

@Composable
private fun RenameNodeDialog(state: ProjectCanvasState) {
    if (state.showRenameDialog && state.selectedNode != null) {
        var newName by remember(state.selectedNode) { mutableStateOf(state.selectedNode!!.name) }
        AlertDialog(
            onDismissRequest = { state.showRenameDialog = false },
            title = { Text("Изменить название") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Новое название") }, singleLine = true) },
            confirmButton = { Button(onClick = { state.updateSelectedNodeName(newName) }) { Text("Сохранить") } },
            dismissButton = { Button(onClick = { state.showRenameDialog = false }) { Text("Отмена") } }
        )
    }
}

