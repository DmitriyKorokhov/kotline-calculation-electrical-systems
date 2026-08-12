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
