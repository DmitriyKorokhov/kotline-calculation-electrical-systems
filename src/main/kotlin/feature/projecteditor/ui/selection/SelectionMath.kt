package feature.projecteditor.ui.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import feature.projecteditor.domain.*
import feature.projecteditor.state.getNodeHeight
import feature.projecteditor.domain.Point

private const val NODE_WIDTH = 120f

// 1. Получаем точные границы (Bounding Box) для любой модели
fun getBoundingBox(node: ProjectNode): Rect {
    val width = when (node) {
        is TransformerNode -> node.radiusOuter * 2f
        is GeneratorNode -> node.radius * 2f
        is SystemNode -> node.radius * 2f
        else -> NODE_WIDTH
    }
    val height = when (node) {
        is TransformerNode -> node.radiusOuter * 2f
        is GeneratorNode -> node.radius * 2f
        is ShieldNode -> getNodeHeight(node)
        is SystemNode -> node.radius * 2f
        else -> 80f // Базовая высота NODE_HEIGHT
    }

    val left = node.position.x - width / 2
    val top = node.position.y - height / 2
    return Rect(left, top, left + width, top + height)
}

// 2. Логика AutoCAD: Window (полностью внутри) vs Crossing (пересекает)
fun getNodesInSelectionBox(nodes: List<ProjectNode>, startWorld: Point, endWorld: Point): List<Int> {
    val isLeftToRight = startWorld.x < endWorld.x

    val selectBox = Rect(
        left = minOf(startWorld.x, endWorld.x),
        top = minOf(startWorld.y, endWorld.y),
        right = maxOf(startWorld.x, endWorld.x),
        bottom = maxOf(startWorld.y, endWorld.y)
    )

    return nodes.filter { node ->
        val nodeBox = getBoundingBox(node)
        if (isLeftToRight) {
            // Слева-направо (Окно): модель должна быть полностью внутри
            selectBox.contains(Offset(nodeBox.left, nodeBox.top)) &&
                    selectBox.contains(Offset(nodeBox.right, nodeBox.bottom))
        } else {
            // Справа-налево (Секущая): достаточно любого пересечения
            selectBox.overlaps(nodeBox)
        }
    }.map { it.id }
}

fun getConnectionsInSelectionBox(
    connections: List<Connection>,
    state: feature.projecteditor.state.ProjectCanvasState,
    startWorld: Point,
    endWorld: Point
): List<Connection> {
    val isLeftToRight = startWorld.x < endWorld.x
    val selectBox = Rect(
        left = minOf(startWorld.x, endWorld.x),
        top = minOf(startWorld.y, endWorld.y),
        right = maxOf(startWorld.x, endWorld.x),
        bottom = maxOf(startWorld.y, endWorld.y)
    )

    return connections.filter { conn ->
        val pts = state.calculateConnectionPoints(conn)
        if (pts.isEmpty()) return@filter false

        if (isLeftToRight) {
            // Окно (слева направо): все точки линии должны быть внутри рамки
            pts.all { pt -> selectBox.contains(Offset(pt.x, pt.y)) }
        } else {
            // Секущая (справа налево): достаточно пересечения любого отрезка
            // 1. Если любая точка внутри - пересекает
            if (pts.any { pt -> selectBox.contains(Offset(pt.x, pt.y)) }) return@filter true

            // 2. Проверка пересечения самих отрезков с рамкой
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val minX = minOf(p1.x, p2.x)
                val maxX = maxOf(p1.x, p2.x)
                val minY = minOf(p1.y, p2.y)
                val maxY = maxOf(p1.y, p2.y)

                // Так как линии у нас ортогональные (только под прямым углом),
                // достаточно проверки пересечения BoundingBox отрезка с BoundingBox рамки
                if (maxX >= selectBox.left && minX <= selectBox.right &&
                    maxY >= selectBox.top && minY <= selectBox.bottom
                ) {
                    return@filter true
                }
            }
            false
        }
    }
}