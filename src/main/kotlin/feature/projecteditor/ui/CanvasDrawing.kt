package feature.projecteditor.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectCanvasState
import feature.projecteditor.state.getNodeHeight
import feature.projecteditor.ui.drawing.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.clipRect

private const val NODE_WIDTH = 120f
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawProjectCanvas(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    withTransform({
        translate(left = state.offset.x, top = state.offset.y)
        scale(scale = state.scale, pivot = Offset.Zero)
    }) {
        // Конвертируем Offset Compose в наш Point для запроса к стейту
        val topLeftWorld = state.screenToWorld(Offset.Zero.toPoint())
        val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height).toPoint())

        // Для отрисовки переводим обратно в Offset
        drawGrid(topLeftWorld.toOffset(), bottomRightWorld.toOffset())
        drawLevels(state.levels, topLeftWorld.toOffset(), bottomRightWorld.toOffset(), state.scale)
        drawConnections(state.connections, state.nodes, state.scale)
        drawNodes(textMeasurer, state.nodes, state.connectingFromNodeId, state.scale)
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
            val startX = calculateConnectionX(fromNode, outgoingIndex, outgoingConnections.size)

            val incomingConnections = connections.filter { it.toId == toNode.id }
            val incomingIndex = incomingConnections.indexOf(conn)
            val endX = calculateConnectionX(toNode, incomingIndex, incomingConnections.size)

            val isFromNodeOnTop = fromNode.position.y < toNode.position.y

            val startOffset = when (fromNode) {
                is TransformerNode -> Offset(startX, if (isFromNodeOnTop) fromNode.position.y + 1.5f * fromNode.radiusOuter else fromNode.position.y - 1.5f * fromNode.radiusOuter)
                is GeneratorNode -> Offset(startX, if (isFromNodeOnTop) fromNode.position.y + fromNode.radius else fromNode.position.y - fromNode.radius)
                else -> Offset(startX, if (isFromNodeOnTop) fromNode.position.y + getNodeHeight(fromNode) / 2 else fromNode.position.y - getNodeHeight(fromNode) / 2)
            }

            val endOffset = when (toNode) {
                is TransformerNode -> Offset(endX, if (isFromNodeOnTop) toNode.position.y - 1.5f * toNode.radiusOuter else toNode.position.y + 1.5f * toNode.radiusOuter)
                is GeneratorNode -> Offset(endX, if (isFromNodeOnTop) toNode.position.y - toNode.radius else toNode.position.y + toNode.radius)
                else -> Offset(endX, if (isFromNodeOnTop) toNode.position.y - getNodeHeight(toNode) / 2 else toNode.position.y + getNodeHeight(toNode) / 2)
            }

            val midY = (startOffset.y + endOffset.y) / 2
            drawLine(color = Color.Gray, start = startOffset, end = Offset(startOffset.x, midY), strokeWidth = 2f / scale)
            drawLine(color = Color.Gray, start = Offset(startOffset.x, midY), end = Offset(endOffset.x, midY), strokeWidth = 2f / scale)
            drawLine(color = Color.Gray, start = Offset(endOffset.x, midY), end = endOffset, strokeWidth = 2f / scale)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawNodes(textMeasurer: TextMeasurer, nodes: List<ProjectNode>, connectingFromNodeId: Int?, scale: Float) {
    nodes.forEach { node ->
        val isSelected = node.id == connectingFromNodeId
        when (node) {
            is ShieldNode -> {
                val height = getNodeHeight(node)
                drawShieldShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is UpsNode -> {
                val height = getNodeHeight(node)
                drawUpsShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
            }
            is BatteryNode -> {
                val height = getNodeHeight(node)
                drawBatteryShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is SolarPanelNode -> {
                val height = getNodeHeight(node)
                drawSolarPanelShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is InverterNode -> {
                val height = getNodeHeight(node)
                drawInverterShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
            }
            is TransformerNode -> drawTransformerShape(node.position.toOffset(), node.radiusOuter, isSelected)
            is GeneratorNode -> drawGeneratorShape(textMeasurer, node.position.toOffset(), node.radius, isSelected)
        }
    }
}

private fun calculateConnectionX(node: ProjectNode, connectionIndex: Int, totalConnections: Int): Float {
    if (totalConnections <= 1) return node.position.x
    val span = when (node) {
        is TransformerNode -> node.radiusOuter * 1.5f
        is GeneratorNode -> node.radius * 1.5f
        else -> NODE_WIDTH * 0.8f
    }
    return (node.position.x - span / 2) + connectionIndex * (span / (totalConnections - 1))
}

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawGridHeaders(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    val headerHeight = 24.dp.toPx()
    val headerWidth = 40.dp.toPx()

    val bgColor = Color(0xFFF5F5F5)
    val lineColor = Color.Gray
    val textColor = Color.DarkGray

    // 1. Рисуем фоны панелей
    drawRect(color = bgColor, topLeft = Offset(0f, 0f), size = Size(size.width, headerHeight))
    drawRect(color = bgColor, topLeft = Offset(0f, 0f), size = Size(headerWidth, size.height))

    // Угловой квадрат (он всегда будет чистым)
    drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(0f, 0f), size = Size(headerWidth, headerHeight))

    // Линии-границы самих панелей
    drawLine(lineColor, Offset(0f, headerHeight), Offset(size.width, headerHeight))
    drawLine(lineColor, Offset(headerWidth, 0f), Offset(headerWidth, size.height))

    // 2. Вычисляем видимую зону
    val topLeftWorld = state.screenToWorld(Offset(0f, 0f).toPoint())
    val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height).toPoint())

    val startCol = kotlin.math.floor(topLeftWorld.x / GRID_WIDTH).toInt() - 1
    val endCol = kotlin.math.floor(bottomRightWorld.x / GRID_WIDTH).toInt() + 1

    val startRow = kotlin.math.floor(topLeftWorld.y / GRID_HEIGHT).toInt() - 1
    val endRow = kotlin.math.floor(bottomRightWorld.y / GRID_HEIGHT).toInt() + 1

    val textStyle = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

    // 3. Рисуем столбцы (Буквы) — используем clipRect для жесткой обрезки!
    clipRect(left = headerWidth, top = 0f, right = size.width, bottom = headerHeight) {
        for (col in startCol..endCol) {
            val worldX = col * GRID_WIDTH
            val screenX = (worldX * state.scale) + state.offset.x

            // Линия деления колонки
            drawLine(lineColor, Offset(screenX, 0f), Offset(screenX, headerHeight))

            // Отрисовка текста
            val text = getExcelColumnName(col)
            val layoutResult = textMeasurer.measure(text, textStyle)

            val cellWidthOnScreen = GRID_WIDTH * state.scale
            val textX = screenX + (cellWidthOnScreen - layoutResult.size.width) / 2
            val textY = (headerHeight - layoutResult.size.height) / 2

            drawText(layoutResult, topLeft = Offset(textX, textY))
        }
    }

    // 4. Рисуем строки (Цифры) — используем clipRect для жесткой обрезки!
    clipRect(left = 0f, top = headerHeight, right = headerWidth, bottom = size.height) {
        for (row in startRow..endRow) {
            val worldY = row * GRID_HEIGHT
            val screenY = (worldY * state.scale) + state.offset.y

            // Линия деления строки
            drawLine(lineColor, Offset(0f, screenY), Offset(headerWidth, screenY))

            // Отрисовка текста
            val text = row.toString()
            val layoutResult = textMeasurer.measure(text, textStyle)

            val cellHeightOnScreen = GRID_HEIGHT * state.scale
            val textX = (headerWidth - layoutResult.size.width) / 2
            val textY = screenY + (cellHeightOnScreen - layoutResult.size.height) / 2

            drawText(layoutResult, topLeft = Offset(textX, textY))
        }
    }
}

// Вспомогательная функция для генерации букв (A, B... Z, AA...).
// Поддерживает и отрицательные индексы (на случай если пользователь ушел влево: -A, -B)
private fun getExcelColumnName(index: Int): String {
    var num = kotlin.math.abs(index)
    var name = ""
    while (num >= 0) {
        name = ('A' + (num % 26)) + name
        num = (num / 26) - 1
        if (num < 0) break
    }
    return if (index < 0) "-$name" else name
}
