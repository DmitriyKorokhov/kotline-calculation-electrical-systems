package feature.projecteditor.ui.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import feature.projecteditor.domain.*
import feature.projecteditor.state.getNodeHeight
import feature.projecteditor.domain.Point

private const val NODE_WIDTH = 120f
const val RACK_WIDTH = 40f
const val RACK_HEIGHT = 80f
const val RACK_GAP = 10f
const val FEED_LINE_SPACING = 15f // Расстояние между горизонтальными шинами
const val FEED_MARGIN = 20f // Отступ от стоек до первой шины

// 1. Получаем точные границы (Bounding Box) для любой модели
fun getBoundingBox(node: ProjectNode): Rect {
    val width = when (node) {
        is TransformerNode -> node.radiusOuter * 2f
        is GeneratorNode -> node.radius * 2f
        is SystemNode -> node.radius * 2f
        is ItRackRowNode -> getItRackRowSize(node).first
        else -> NODE_WIDTH
    }
    val height = when (node) {
        is TransformerNode -> node.radiusOuter * 2f
        is GeneratorNode -> node.radius * 2f
        is ShieldNode -> getNodeHeight(node)
        is SystemNode -> node.radius * 2f
        is ItRackRowNode -> getItRackRowSize(node).second
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

// Динамический расчет размеров
fun getItRackRowSize(node: ItRackRowNode): Pair<Float, Float> {
    val racksCount = maxOf(1, node.racks.size)
    val width = (racksCount * RACK_WIDTH) + ((racksCount - 1) * RACK_GAP) + 20f

    val assignments = calculateFeedAssignments(node.feeds, node.racks)

    val topTracksCount = assignments.values.filter { it.isTop }.maxOfOrNull { it.trackIndex + 1 } ?: 0
    val bottomTracksCount = assignments.values.filter { !it.isTop }.maxOfOrNull { it.trackIndex + 1 } ?: 0

    val height = RACK_HEIGHT +
            (if (topTracksCount > 0) FEED_MARGIN + (topTracksCount - 1) * FEED_LINE_SPACING else 0f) +
            (if (bottomTracksCount > 0) FEED_MARGIN + (bottomTracksCount - 1) * FEED_LINE_SPACING else 0f)

    return Pair(width, height)
}

fun getPackedTracksCount(feeds: List<RackFeed>, racks: List<Rack>, isTop: Boolean): Int {
    val tracks = mutableListOf<MutableList<IntRange>>()
    feeds.filter { it.isTop == isTop }.forEach { feed ->
        val connectedIndices = racks.mapIndexedNotNull { index, rack ->
            if (feed.connectedRacks.contains(rack.index)) index else null
        }
        if (connectedIndices.isEmpty()) return@forEach
        val interval = connectedIndices.minOrNull()!!..connectedIndices.maxOrNull()!!

        var assignedTrack = -1
        for (i in tracks.indices) {
            // Если нет пересечений с другими лучами на этой дорожке
            val hasOverlap = tracks[i].any { maxOf(it.first, interval.first) <= minOf(it.last, interval.last) }
            if (!hasOverlap) {
                tracks[i].add(interval)
                assignedTrack = i
                break
            }
        }
        if (assignedTrack == -1) tracks.add(mutableListOf(interval))
    }
    return tracks.size
}

data class FeedAssignment(val isTop: Boolean, val trackIndex: Int)

// Умный алгоритм автоматического распределения лучей без нахлестов
fun calculateFeedAssignments(feeds: List<RackFeed>, racks: List<Rack>): Map<Int, FeedAssignment> {
    val topTracks = mutableListOf<MutableList<IntRange>>()
    val bottomTracks = mutableListOf<MutableList<IntRange>>()
    val assignments = mutableMapOf<Int, FeedAssignment>()

    feeds.forEachIndexed { feedIndex, feed ->
        // Находим индексы подключенных стоек
        val connected = racks.mapIndexedNotNull { idx, rack ->
            if (feed.connectedRacks.contains(rack.index)) idx else null
        }
        if (connected.isNotEmpty()) {
            val interval = connected.minOrNull()!!..connected.maxOrNull()!!
            var assigned = false
            var level = 0

            while (!assigned) {
                // Пробуем положить на текущий уровень СВЕРХУ
                if (topTracks.size <= level) topTracks.add(mutableListOf())
                if (!topTracks[level].any { maxOf(it.first, interval.first) <= minOf(it.last, interval.last) }) {
                    topTracks[level].add(interval)
                    assignments[feedIndex] = FeedAssignment(isTop = true, trackIndex = level)
                    assigned = true
                    break
                }

                // Пробуем положить на текущий уровень СНИЗУ
                if (bottomTracks.size <= level) bottomTracks.add(mutableListOf())
                if (!bottomTracks[level].any { maxOf(it.first, interval.first) <= minOf(it.last, interval.last) }) {
                    bottomTracks[level].add(interval)
                    assignments[feedIndex] = FeedAssignment(isTop = false, trackIndex = level)
                    assigned = true
                    break
                }

                // Если оба заняты, переходим на уровень выше
                level++
            }
        }
    }
    return assignments
}