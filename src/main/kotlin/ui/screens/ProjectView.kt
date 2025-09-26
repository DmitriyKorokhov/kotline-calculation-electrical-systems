package ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Connection
import data.LevelLine
import data.PowerSourceNode
import data.ProjectNode
import data.ShieldNode
import kotlin.math.floor

// --- ИЗМЕНЕНО: Размеры сетки и объектов теперь независимы ---
// Ячейки сетки (УВЕЛИЧЕНЫ)
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

// Объекты (ОСТАЛИСЬ ПРЕЖНИМИ, КОМПАКТНЫМИ)
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH
private const val POWER_SOURCE_HEIGHT = NODE_HEIGHT / 4f

/**
 * Вспомогательная функция привязывается к центру прямоугольной ячейки
 */
private fun snapToGrid(position: Offset): Offset {
    val cellX = floor(position.x / GRID_WIDTH)
    val cellY = floor(position.y / GRID_HEIGHT)
    val snappedX = cellX * GRID_WIDTH + (GRID_WIDTH / 2)
    val snappedY = cellY * GRID_HEIGHT + (GRID_HEIGHT / 2)
    return Offset(snappedX, snappedY)
}


@Composable
fun ProjectView(onOpenShield: (shieldId: Int) -> Unit) {
    // --- Состояния экрана ---
    val nodes = remember { mutableStateListOf<ProjectNode>() }
    val connections = remember { mutableStateListOf<Connection>() }
    val levels = remember { mutableStateListOf<LevelLine>() }

    var nextId by remember { mutableStateOf(1) }
    var showNodeContextMenu by remember { mutableStateOf(false) }
    var showCanvasContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var selectedNode by remember { mutableStateOf<ProjectNode?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var connectingFromNodeId by remember { mutableStateOf<Int?>(null) }
    var nodeToMoveId by remember { mutableStateOf<Int?>(null) }

    val surfaceColor = MaterialTheme.colors.surface
    val primaryColor = MaterialTheme.colors.primary

    Box(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val position = event.changes.first().position

                        if (event.type == PointerEventType.Press) {
                            val clickedNode = findNodeAtPosition(nodes, position)

                            if (event.buttons.isSecondaryPressed) {
                                // --- ЛОГИКА ПРАВОГО КЛИКА ---
                                contextMenuPosition = position
                                if (clickedNode != null) {
                                    selectedNode = clickedNode
                                    showNodeContextMenu = true
                                } else {
                                    showCanvasContextMenu = true
                                }
                                event.changes.forEach { it.consume() }

                            } else {
                                // --- ЛОГИКА ЛЕВОГО КЛИКА ---
                                if (nodeToMoveId != null) {
                                    // ЭТАП 2: ПЕРЕМЕЩЕНИЕ
                                    val nodeToUpdate = nodes.find { it.id == nodeToMoveId }
                                    nodeToUpdate?.let {
                                        val index = nodes.indexOf(it)
                                        if (index != -1) {
                                            val newPosition = snapToGrid(position)
                                            nodes[index] = when (it) {
                                                is ShieldNode -> it.copy(position = newPosition)
                                                is PowerSourceNode -> it.copy(position = newPosition)
                                            }
                                        }
                                    }
                                    nodeToMoveId = null

                                } else if (connectingFromNodeId != null) {
                                    // Логика соединения
                                    if (clickedNode != null && clickedNode.id != connectingFromNodeId) {
                                        connections.add(Connection(connectingFromNodeId!!, clickedNode.id))
                                    }
                                    connectingFromNodeId = null

                                } else if (clickedNode != null) {
                                    // ЭТАП 1: ВЫДЕЛЕНИЕ для перемещения
                                    nodeToMoveId = clickedNode.id
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Отрисовка сетки
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            for (i in 0..size.width.toInt() step GRID_WIDTH.toInt()) {
                drawLine(gridColor, start = Offset(i.toFloat(), 0f), end = Offset(i.toFloat(), size.height))
            }
            for (i in 0..size.height.toInt() step GRID_HEIGHT.toInt()) {
                drawLine(gridColor, start = Offset(0f, i.toFloat()), end = Offset(size.width, i.toFloat()))
            }

            // Отрисовка уровней
            levels.forEach { level ->
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, level.yPosition),
                    end = Offset(size.width, level.yPosition),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Отрисовка ортогональных соединений с распределением точек
            connections.forEach { conn ->
                val fromNode = nodes.find { it.id == conn.fromId }
                val toNode = nodes.find { it.id == conn.toId }
                if (fromNode != null && toNode != null) {
                    val outgoingConnections = connections.filter { it.fromId == fromNode.id }
                    val outgoingIndex = outgoingConnections.indexOf(conn)
                    val startX = calculateConnectionX(fromNode.position.x, outgoingIndex, outgoingConnections.size)

                    val incomingConnections = connections.filter { it.toId == toNode.id }
                    val incomingIndex = incomingConnections.indexOf(conn)
                    val endX = calculateConnectionX(toNode.position.x, incomingIndex, incomingConnections.size)

                    val isFromNodeOnTop = fromNode.position.y < toNode.position.y

                    val startOffset = Offset(
                        startX,
                        if (isFromNodeOnTop) fromNode.position.y + getNodeHeight(fromNode) / 2 else fromNode.position.y - getNodeHeight(fromNode) / 2
                    )
                    val endOffset = Offset(
                        endX,
                        if (isFromNodeOnTop) toNode.position.y - getNodeHeight(toNode) / 2 else toNode.position.y + getNodeHeight(toNode) / 2
                    )

                    val topY = if(isFromNodeOnTop) startOffset.y else endOffset.y
                    val bottomY = if(isFromNodeOnTop) endOffset.y else startOffset.y
                    val midY = topY + (bottomY - topY) / 2

                    val point1 = startOffset
                    val point2 = Offset(startOffset.x, midY)
                    val point3 = Offset(endOffset.x, midY)
                    val point4 = endOffset

                    drawLine(color = Color.Gray, start = point1, end = point2, strokeWidth = 2f)
                    drawLine(color = Color.Gray, start = point2, end = point3, strokeWidth = 2f)
                    drawLine(color = Color.Gray, start = point3, end = point4, strokeWidth = 2f)
                }
            }

            // Отрисовка узлов
            nodes.forEach { node ->
                val width = if (node is PowerSourceNode) POWER_SOURCE_WIDTH else NODE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                val nodeSize = Size(width, height)

                val fillColor = when (node) {
                    is PowerSourceNode -> Color.Black
                    is ShieldNode -> surfaceColor
                }
                val borderColor = if (node is PowerSourceNode) Color.Black else Color.Gray

                drawRect(color = fillColor, topLeft = topLeft, size = nodeSize)
                drawRect(color = borderColor, topLeft = topLeft, size = nodeSize, style = Stroke(1.5f))

                if (node.id == connectingFromNodeId || node.id == nodeToMoveId) {
                    drawRect(color = primaryColor, topLeft = topLeft, size = nodeSize, style = Stroke(3f))
                }
            }
        }

        // Отрисовка названий
        nodes.forEach { node ->
            if (node.name.isNotBlank()) {
                if (node is ShieldNode) {
                    val textColor = MaterialTheme.colors.onSurface
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                (node.position.x - NODE_WIDTH / 2).dp,
                                (node.position.y - NODE_HEIGHT / 2).dp
                            )
                            .size(width = NODE_WIDTH.dp, height = NODE_HEIGHT.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = node.name,
                            color = textColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (node is PowerSourceNode) {
                    Text(
                        text = node.name,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                (node.position.x - POWER_SOURCE_WIDTH / 2).dp,
                                (node.position.y - getNodeHeight(node) / 2 - 25).dp
                            ),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Контекстные меню
        DropdownMenu(
            expanded = showNodeContextMenu,
            onDismissRequest = { showNodeContextMenu = false },
            offset = DpOffset(contextMenuPosition.x.dp, contextMenuPosition.y.dp)
        ) {
            DropdownMenuItem(onClick = { showRenameDialog = true; showNodeContextMenu = false }) { Text("Изменить название") }
            if (selectedNode is ShieldNode) {
                DropdownMenuItem(onClick = { selectedNode?.let { onOpenShield(it.id) }; showNodeContextMenu = false }) { Text("Открыть") }
            }
            DropdownMenuItem(onClick = { connectingFromNodeId = selectedNode?.id; showNodeContextMenu = false }) { Text("Соединить") }
            DropdownMenuItem(onClick = {
                selectedNode?.let { nodeToDelete ->
                    nodes.remove(nodeToDelete)
                    connections.removeAll { it.fromId == nodeToDelete.id || it.toId == nodeToDelete.id }
                }
                showNodeContextMenu = false
            }) { Text("Удалить") }
        }

        DropdownMenu(
            expanded = showCanvasContextMenu,
            onDismissRequest = { showCanvasContextMenu = false },
            offset = DpOffset(contextMenuPosition.x.dp, contextMenuPosition.y.dp)
        ) {
            DropdownMenuItem(onClick = {
                val snappedPosition = snapToGrid(contextMenuPosition)
                nodes.add(ShieldNode(id = nextId++, name = "Щит", position = snappedPosition))
                showCanvasContextMenu = false
            }) { Text("Добавить щит") }
            DropdownMenuItem(onClick = {
                val snappedPosition = snapToGrid(contextMenuPosition)
                nodes.add(PowerSourceNode(id = nextId++, name = "Источник", position = snappedPosition))
                showCanvasContextMenu = false
            }) { Text("Добавить ист. питания") }
            DropdownMenuItem(onClick = {
                levels.add(LevelLine(id = nextId++, yPosition = contextMenuPosition.y))
                showCanvasContextMenu = false
            }) { Text("Добавить уровень") }
        }

        val statusText = when {
            connectingFromNodeId != null -> "Выберите объект для соединения..."
            nodeToMoveId != null -> "Выберите ячейку для перемещения..."
            else -> null
        }
        if (statusText != null) {
            Text(statusText, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha = 0.5f)).padding(8.dp), color = Color.White)
        }
    }

    if (showRenameDialog && selectedNode != null) {
        var newName by remember(selectedNode) { mutableStateOf(selectedNode!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Изменить название") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Новое название") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    selectedNode?.let {
                        val updatedNode = when (it) {
                            is ShieldNode -> it.copy(name = newName)
                            is PowerSourceNode -> it.copy(name = newName)
                        }
                        val index = nodes.indexOf(it)
                        if (index != -1) nodes[index] = updatedNode
                    }
                    showRenameDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { Button(onClick = { showRenameDialog = false }) { Text("Отмена") } }
        )
    }
}

private fun getNodeHeight(node: ProjectNode): Float {
    return if (node is PowerSourceNode) POWER_SOURCE_HEIGHT else NODE_HEIGHT
}


private fun calculateConnectionX(nodeCenterX: Float, connectionIndex: Int, totalConnections: Int): Float {
    if (totalConnections <= 1) {
        return nodeCenterX
    }
    val connectionSpan = NODE_WIDTH * 0.8f
    val step = connectionSpan / (totalConnections - 1)
    val startX = nodeCenterX - connectionSpan / 2
    return startX + connectionIndex * step
}


private fun findNodeAtPosition(nodes: List<ProjectNode>, position: Offset): ProjectNode? {
    return nodes.findLast { node ->
        val width = if (node is PowerSourceNode) POWER_SOURCE_WIDTH else NODE_WIDTH
        val height = getNodeHeight(node)
        (position.x in (node.position.x - width / 2)..(node.position.x + width / 2)) &&
                (position.y in (node.position.y - height / 2)..(node.position.y + height / 2))
    }
}

