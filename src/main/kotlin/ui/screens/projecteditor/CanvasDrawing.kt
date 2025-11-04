package ui.screens.projecteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import data.*

private const val NODE_WIDTH = 120f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH

private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawProjectCanvas(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    withTransform({
        translate(left = state.offset.x, top = state.offset.y)
        scale(scale = state.scale, pivot = Offset.Zero)
    }) {
        val topLeftWorld = state.screenToWorld(Offset.Zero)
        val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height))

        drawGrid(topLeftWorld, bottomRightWorld)
        drawLevels(state.levels, topLeftWorld, bottomRightWorld, state.scale)
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

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawNodes(textMeasurer: TextMeasurer, nodes: List<ProjectNode>, connectingFromNodeId: Int?, scale: Float) {
    nodes.forEach { node ->
        val isSelected = node.id == connectingFromNodeId
        when (node) {
            is PowerSourceNode -> {
                val width = POWER_SOURCE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                drawPowerSourceShape(topLeft, Size(width, height), isSelected)
            }
            is ShieldNode -> {
                val width = NODE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                drawShieldShape(topLeft, Size(width, height), isSelected)
            }
            is TransformerNode -> {
                drawTransformerShape(node.position, node.radiusOuter, isSelected)
            }
            is GeneratorNode -> {
                drawGeneratorShape(textMeasurer, node.position, node.radius, isSelected)
            }
            else -> {
                val width = NODE_WIDTH
                val height = getNodeHeight(node)
                val topLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                drawRect(Color.LightGray, topLeft, Size(width, height))
            }
        }
    }
}

private fun calculateConnectionX(node: ProjectNode, connectionIndex: Int, totalConnections: Int): Float {
    if (totalConnections <= 1) {
        return node.position.x
    }

    val span = when (node) {
        is TransformerNode -> node.radiusOuter * 1.5f
        is GeneratorNode -> node.radius * 1.5f
        else -> NODE_WIDTH * 0.8f
    }

    val step = span / (totalConnections - 1)
    val startX = node.position.x - span / 2
    return startX + connectionIndex * step
}
