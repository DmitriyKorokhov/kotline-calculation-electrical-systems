package ui.screens.projecteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import data.Connection
import data.LevelLine
import data.PowerSourceNode
import data.ProjectNode
import data.ShieldNode
import data.TransformerNode
import kotlin.math.max

// Константы, необходимые для расчетов отрисовки
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

/**
 * Функция-расширение для DrawScope, инкапсулирующая всю логику отрисовки.
 */
fun DrawScope.drawProjectCanvas(state: ProjectCanvasState) {
    withTransform({
        translate(left = state.offset.x, top = state.offset.y)
        scale(scale = state.scale, pivot = Offset.Zero)
    }) {
        val topLeftWorld = state.screenToWorld(Offset.Zero)
        val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height))

        drawGrid(topLeftWorld, bottomRightWorld)
        drawLevels(state.levels, topLeftWorld, bottomRightWorld, state.scale)
        drawConnections(state.connections, state.nodes, state.scale)
        drawNodes(state.nodes, state.connectingFromNodeId, state.scale)
    }
}

private fun DrawScope.drawGrid(topLeft: Offset, bottomRight: Offset) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val left = (topLeft.x - GRID_WIDTH).toInt() - ((topLeft.x - GRID_WIDTH).toInt() % GRID_WIDTH.toInt())
    val top = (topLeft.y - GRID_HEIGHT).toInt() - ((topLeft.y - GRID_HEIGHT).toInt() % GRID_HEIGHT.toInt())
    val right = (bottomRight.x + GRID_WIDTH).toInt()
    val bottom = (bottomRight.y + GRID_HEIGHT).toInt()

    for (i in left..right step GRID_WIDTH.toInt()) {
        drawLine(gridColor, start = Offset(i.toFloat(), top.toFloat()), end = Offset(i.toFloat(), bottom.toFloat()))
    }
    for (i in top..bottom step GRID_HEIGHT.toInt()) {
        drawLine(gridColor, start = Offset(left.toFloat(), i.toFloat()), end = Offset(right.toFloat(), i.toFloat()))
    }
}

private fun DrawScope.drawLevels(levels: List<LevelLine>, topLeft: Offset, bottomRight: Offset, scale: Float) {
    levels.forEach { level ->
        drawLine(
            color = Color.Gray,
            start = Offset(topLeft.x, level.yPosition),
            end = Offset(bottomRight.x, level.yPosition),
            strokeWidth = 2f / scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

private fun DrawScope.drawConnections(connections: List<Connection>, nodes: List<ProjectNode>, scale: Float) {
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

            // --- Вычисляем стартовую точку в зависимости от типа узла ---
            val startOffset = when (fromNode) {
                is TransformerNode -> {
                    val c1 = fromNode.position
                    val r = fromNode.radiusOuter
                    val c2 = Offset(c1.x, c1.y + r)
                    // Если fromNode выше toNode (isFromNodeOnTop == true),
                    // то старт должен быть САМЫМ НИЖНИМ местом нижнего круга:
                    // y = c2.y + r
                    // Иначе (если fromNode ниже toNode) — присоединяем к самой верхней точке верхнего круга:
                    // y = c1.y - r
                    val y = if (isFromNodeOnTop) c2.y + r else c1.y - r
                    Offset(startX, y)
                }
                else -> Offset(
                    startX,
                    if (isFromNodeOnTop) fromNode.position.y + getNodeHeight(fromNode) / 2 else fromNode.position.y - getNodeHeight(fromNode) / 2
                )
            }

            val endOffset = when (toNode) {
                is TransformerNode -> {
                    val c1 = toNode.position
                    val r = toNode.radiusOuter
                    val c2 = Offset(c1.x, c1.y + r)
                    // Аналогично: если fromNode выше toNode, то toNode находится ниже -> подключаемся к верхней точке верхнего круга:
                    // y = c1.y - r
                    // Иначе подключаемся к нижней точке нижнего круга: y = c2.y + r
                    val y = if (isFromNodeOnTop) c1.y - r else c2.y + r
                    Offset(endX, y)
                }
                else -> Offset(
                    endX,
                    if (isFromNodeOnTop) toNode.position.y - getNodeHeight(toNode) / 2 else toNode.position.y + getNodeHeight(toNode) / 2
                )
            }

            val midY = (startOffset.y + endOffset.y) / 2

            val point1 = startOffset
            val point2 = Offset(startOffset.x, midY)
            val point3 = Offset(endOffset.x, midY)
            val point4 = endOffset

            val strokeWidth = 2f / scale
            drawLine(color = Color.Gray, start = point1, end = point2, strokeWidth = strokeWidth)
            drawLine(color = Color.Gray, start = point2, end = point3, strokeWidth = strokeWidth)
            drawLine(color = Color.Gray, start = point3, end = point4, strokeWidth = strokeWidth)
        }
    }
}

private fun DrawScope.drawNodes(nodes: List<ProjectNode>, connectingFromNodeId: Int?, scale: Float) {
    val primaryColor = Color(0xFF6200EE)
    val surfaceColor = Color.White

    nodes.forEach { node ->
        when (node) {
            is PowerSourceNode -> {
                val width = POWER_SOURCE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                val nodeSize = Size(width, height)
                drawRect(color = Color.Black, topLeft = topLeft, size = nodeSize)
                drawRect(color = Color.Black, topLeft = topLeft, size = nodeSize, style = Stroke(1.5f / scale))
                if (node.id == connectingFromNodeId) {
                    drawRect(color = primaryColor, topLeft = topLeft, size = nodeSize, style = Stroke(3f / scale))
                }
            }
            is ShieldNode -> {
                val width = NODE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                val nodeSize = Size(width, height)
                drawRect(color = surfaceColor, topLeft = topLeft, size = nodeSize)
                drawRect(color = Color.Gray, topLeft = topLeft, size = nodeSize, style = Stroke(1.5f / scale))
                if (node.id == connectingFromNodeId) {
                    drawRect(color = primaryColor, topLeft = topLeft, size = nodeSize, style = Stroke(3f / scale))
                }
            }
            is TransformerNode -> {
                // Нарисуем два круга одинакового радиуса: внешний и внутренний, центр внутреннего смещён вниз на r
                val c1 = node.position
                val r = node.radiusOuter // оба круга одного диаметра, как надо по ТЗ
                val c2 = Offset(c1.x, c1.y + r) // центр второго круга смещён вниз на r

                // Нарисуем внешний круг (fill + stroke)
                drawCircle(color = Color.White, radius = r, center = c1)
                drawCircle(color = Color.Gray, radius = r, center = c1, style = Stroke(1.5f / scale))

                // Нарисуем второй круг (fill + stroke) — тот же радиус
                drawCircle(color = Color.White, radius = r, center = c2)
                drawCircle(color = Color.Gray, radius = r, center = c2, style = Stroke(1.5f / scale))

                // Выделение, если соединение начато с этого узла
                if (node.id == connectingFromNodeId) {
                    drawCircle(color = primaryColor, radius = r + 4f / scale, center = c1, style = Stroke(3f / scale))
                }
            }

            else -> {
                // По умолчанию - прямоугольник
                val width = NODE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                val nodeSize = Size(width, height)
                drawRect(color = Color.LightGray, topLeft = topLeft, size = nodeSize)
                drawRect(color = Color.Gray, topLeft = topLeft, size = nodeSize, style = Stroke(1.5f / scale))
                if (node.id == connectingFromNodeId) {
                    drawRect(color = primaryColor, topLeft = topLeft, size = nodeSize, style = Stroke(3f / scale))
                }
            }
        }
    }
}

// --- Вспомогательная функция ---
private fun calculateConnectionX(nodeCenterX: Float, connectionIndex: Int, totalConnections: Int): Float {
    if (totalConnections <= 1) {
        return nodeCenterX
    }
    val connectionSpan = NODE_WIDTH * 0.8f
    val step = connectionSpan / (totalConnections - 1)
    val startX = nodeCenterX - connectionSpan / 2
    return startX + connectionIndex * step
}
