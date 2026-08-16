package feature.projecteditor.state

import androidx.compose.runtime.*
import feature.projecteditor.domain.*
import kotlin.math.floor
import androidx.compose.runtime.mutableStateListOf
import feature.projecteditor.ui.selection.FEED_LINE_SPACING
import feature.projecteditor.ui.selection.getItRackRowSize
import feature.projecteditor.ui.selection.getNodesInSelectionBox

// Константы размеров объектов и сетки
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f

/**
 * Класс-хранитель состояния (State Holder).
 */
class ProjectCanvasState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Point.Zero)
    val nodes = mutableStateListOf<ProjectNode>()
    val connections = mutableStateListOf<Connection>()
    val levels = mutableStateListOf<LevelLine>()
    var nextId by mutableStateOf(1)
    var showNodeContextMenu by mutableStateOf(false)
    var showCanvasContextMenu by mutableStateOf(false)
    var contextMenuPosition by mutableStateOf(Point.Zero)
    var selectedNode by mutableStateOf<ProjectNode?>(null)
    var showRenameDialog by mutableStateOf(false)
    var connectingFromNodeId by mutableStateOf<Int?>(null)
    // === СОСТОЯНИЕ ВЫДЕЛЕНИЯ ===
    val selectedNodeIds = mutableStateListOf<Int>()
    var selectionStartScreen by mutableStateOf<Point?>(null)
    var selectionEndScreen by mutableStateOf<Point?>(null)
    // === МЕНЮ И БУФЕР ОБМЕНА ===
    var showMultiSelectMenu by mutableStateOf(false)
    var clipboardNodes by mutableStateOf<List<ProjectNode>>(emptyList())

    var showConnectionContextMenu by mutableStateOf(false)
    var clickedConnectionHit by mutableStateOf<ConnectionHit?>(null)

    val selectedConnections = mutableStateListOf<Connection>()
    var isCtrlPressed by mutableStateOf(false)
    var isShiftPressed by mutableStateOf(false)
    var showRackSettingsDialog by mutableStateOf(false)

    private val historyManager = core.utils.ProjectHistoryManager()

    fun saveHistory() {
        historyManager.pushState(this)
    }

    fun undo() {
        historyManager.undo(this)
    }

    fun redo() {
        historyManager.redo(this)
    }

    fun resetCameraAndId() {
        scale = 1f
        offset = Point.Zero
        nextId = 1
    }

    fun screenToWorld(screenPos: Point): Point {
        return (screenPos - offset) / scale
    }

    fun worldToScreen(worldPos: Point): Point {
        return worldPos * scale + offset
    }

    fun onPan(dragAmount: Point) {
        offset += dragAmount
    }

    fun onZoom(scrollDelta: Float, zoomCenter: Point) {
        val oldScale = scale
        val newScale = (scale * (1f - scrollDelta * 0.1f)).coerceIn(0.1f, 5f)
        scale = newScale
        offset = zoomCenter - ((zoomCenter - offset) / oldScale) * newScale
    }

    fun findNodeAtScreenPosition(screenPos: Point): ProjectNode? {
        val worldPos = screenToWorld(screenPos)
        return nodes.findLast { node ->
            when (node) {
                is TransformerNode -> {
                    val c1 = Point(node.position.x, node.position.y - node.radiusOuter / 2)
                    val c2 = Point(node.position.x, node.position.y + node.radiusOuter / 2)
                    (worldPos - c1).getDistanceSquared() < node.radiusOuter * node.radiusOuter ||
                            (worldPos - c2).getDistanceSquared() < node.radiusOuter * node.radiusOuter
                }
                is GeneratorNode -> (worldPos - node.position).getDistanceSquared() < node.radius * node.radius
                is SystemNode -> (worldPos - node.position).getDistanceSquared() < node.radius * node.radius
                is ItRackRowNode -> {
                    val (width, height) = feature.projecteditor.ui.selection.getItRackRowSize(node)
                    val nodeTopLeft = Point(node.position.x - width / 2, node.position.y - height / 2)
                    worldPos.x >= nodeTopLeft.x && worldPos.x <= nodeTopLeft.x + width &&
                            worldPos.y >= nodeTopLeft.y && worldPos.y <= nodeTopLeft.y + height
                }
                is ItRackRowNode -> {
                    val (width, height) = feature.projecteditor.ui.selection.getItRackRowSize(node)
                    val nodeTopLeft = Point(node.position.x - width / 2, node.position.y - height / 2)
                    worldPos.x >= nodeTopLeft.x && worldPos.x <= nodeTopLeft.x + width &&
                            worldPos.y >= nodeTopLeft.y && worldPos.y <= nodeTopLeft.y + height
                }
                else -> {
                    val width = NODE_WIDTH
                    val height = getNodeHeight(node)
                    val nodeTopLeft = Point(node.position.x - width / 2, node.position.y - height / 2)
                    worldPos.x >= nodeTopLeft.x && worldPos.x <= nodeTopLeft.x + width &&
                            worldPos.y >= nodeTopLeft.y && worldPos.y <= nodeTopLeft.y + height
                }
            }
        }
    }

    /**
     * Обновляет позицию узла по его ID.
     */
    fun updateNodePosition(nodeId: Int, newPosition: Point) {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index != -1) {
            val node = nodes[index]
            val updatedNode = when (node) {
                is ShieldNode -> node.copy(position = newPosition)
                is TransformerNode -> node.copy(position = newPosition)
                is GeneratorNode -> node.copy(position = newPosition)
                is UpsNode -> node.copy(position = newPosition)
                is BatteryNode -> node.copy(position = newPosition)
                is SolarPanelNode -> node.copy(position = newPosition)
                is InverterNode -> node.copy(position = newPosition)
                is SystemNode -> node.copy(position = newPosition)
                is ItRackRowNode -> node.copy(position = newPosition)
                is RectifierNode -> node.copy(position = newPosition)
                else -> node
            }
            nodes[index] = updatedNode
        }
    }

    /**
     * Привязывает узел к сетке по его ID.
     */
    fun snapNodeToEndPosition(nodeId: Int) {
        val node = nodes.find { it.id == nodeId }
        node?.let {
            val snappedPosition = snapToGrid(it.position)
            updateNodePosition(it.id, snappedPosition)
        }
    }

    fun addShieldNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(ShieldNode(id = nextId++, name = "Щит", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addItRackRowNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(ItRackRowNode(id = nextId++, position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addTransformerNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(TransformerNode(id = nextId++, name = "T", position = snappedPosition, radiusOuter = 40f, radiusInner = 30f))
        showCanvasContextMenu = false
    }

    fun addRectifierNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(RectifierNode(id = nextId++, name = "Выпрямитель", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun startConnecting() {
        connectingFromNodeId = selectedNode?.id
        showNodeContextMenu = false
    }

    fun deleteSelectedNode() {
        selectedNode?.let { nodeToDelete ->
            saveHistory()
            nodes.remove(nodeToDelete)
            connections.removeAll { it.fromId == nodeToDelete.id || it.toId == nodeToDelete.id }
        }
        showNodeContextMenu = false
    }

    fun updateSelectedNodeName(newName: String) {
        selectedNode?.let {
            saveHistory()
            val updatedNode = when (it) {
                is ShieldNode -> it.copy(name = newName)
                is TransformerNode -> it.copy(name = newName)
                is GeneratorNode -> it.copy(name = newName)
                is UpsNode -> it.copy(name = newName)
                is BatteryNode -> it.copy(name = newName)
                is SolarPanelNode -> it.copy(name = newName)
                is InverterNode -> it.copy(name = newName)
                is SystemNode -> it.copy(name = newName)
                is ItRackRowNode -> it.copy(name = newName)
                is RectifierNode -> it.copy(name = newName)
                else -> it
            }
            val index = nodes.indexOf(it)
            if (index != -1) nodes[index] = updatedNode
        }
        showRenameDialog = false
    }

    fun addUpsNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(UpsNode(id = nextId++, name = "ИБП", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addBatteryNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(BatteryNode(id = nextId++, name = "АКБ", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addSolarPanelNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(SolarPanelNode(id = nextId++, name = "СБ", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addInverterNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(InverterNode(id = nextId++, name = "Инвертор", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addSystemNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(SystemNode(id = nextId++, name = "Система", position = snappedPosition))
        showCanvasContextMenu = false
    }

    private fun snapToGrid(position: Point): Point {
        val cellX = floor(position.x / 200f)
        val cellY = floor(position.y / 140f)
        return Point(cellX * 200f + 100f, cellY * 140f + 70f)
    }

    fun addGeneratorNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(
            GeneratorNode(
                id = nextId++,
                name = "G",
                position = snappedPosition
            )
        )
        showCanvasContextMenu = false
    }

    fun clearSelection() {
        selectedNodeIds.clear()
        selectedConnections.clear()
    }

    fun applySelectionBox() {
        val start = selectionStartScreen ?: return
        val end = selectionEndScreen ?: return

        val startWorld = screenToWorld(start)
        val endWorld = screenToWorld(end)

        val newNodes = feature.projecteditor.ui.selection.getNodesInSelectionBox(nodes, startWorld, endWorld)
        val newConns = feature.projecteditor.ui.selection.getConnectionsInSelectionBox(connections, this, startWorld, endWorld)

        if (isCtrlPressed) {
            // Исключаем из выделения
            selectedNodeIds.removeAll(newNodes)
            selectedConnections.removeAll(newConns)
        } else if (isShiftPressed) {
            // Добавляем к выделению
            newNodes.forEach { if (it !in selectedNodeIds) selectedNodeIds.add(it) }
            newConns.forEach { if (it !in selectedConnections) selectedConnections.add(it) }
        } else {
            // Заменяем выделение
            selectedNodeIds.clear()
            selectedNodeIds.addAll(newNodes)
            selectedConnections.clear()
            selectedConnections.addAll(newConns)
        }
    }

    // 1. Удалить выделенное
    fun deleteSelectedNodes() {
        saveHistory()
        nodes.removeAll { it.id in selectedNodeIds }
        // Удаляем линии, привязанные к удаляемым узлам
        connections.removeAll { it.fromId in selectedNodeIds || it.toId in selectedNodeIds }
        // Удаляем явно выделенные линии
        connections.removeAll(selectedConnections)
        clearSelection()
    }

    // 2. Копировать выделенное
    fun copySelectedNodes() {
        clipboardNodes = nodes.filter { it.id in selectedNodeIds }
    }

    // 3. Вставить скопированное
    fun pasteNodes(screenPos: Point) {
        if (clipboardNodes.isEmpty()) return
        saveHistory()

        val worldPos = screenToWorld(screenPos)

        // Находим левый верхний угол скопированной группы, чтобы вставить ровно под курсор
        val minX = clipboardNodes.minOf { it.position.x }
        val minY = clipboardNodes.minOf { it.position.y }
        val deltaX = worldPos.x - minX
        val deltaY = worldPos.y - minY

        clearSelection()

        clipboardNodes.forEach { node ->
            val newPos = Point(node.position.x + deltaX, node.position.y + deltaY)

            // Копируем узел, присваиваем новый ID и новые координаты
            val newNode = when (node) {
                is ShieldNode -> node.copy(id = nextId, position = newPos)
                is TransformerNode -> node.copy(id = nextId, position = newPos)
                is GeneratorNode -> node.copy(id = nextId, position = newPos)
                is UpsNode -> node.copy(id = nextId, position = newPos)
                is BatteryNode -> node.copy(id = nextId, position = newPos)
                is SolarPanelNode -> node.copy(id = nextId, position = newPos)
                is InverterNode -> node.copy(id = nextId, position = newPos)
                is SystemNode -> node.copy(id = nextId, position = newPos)
                else -> node // Запасной вариант
            }

            nodes.add(newNode)
            selectedNodeIds.add(nextId) // Сразу выделяем вставленные элементы
            nextId++
        }
    }

    fun tryFinishConnecting(clickedNode: ProjectNode?) {
        if (connectingFromNodeId != null && clickedNode != null && clickedNode.id != connectingFromNodeId) {
            saveHistory()
            connections.add(Connection(connectingFromNodeId!!, clickedNode.id))
        }
        connectingFromNodeId = null
    }

    fun updateConnection(oldConn: Connection, newConn: Connection) {
        val index = connections.indexOf(oldConn)
        if (index != -1) {
            connections[index] = newConn
            if (selectedConnections.contains(oldConn)) {
                selectedConnections.remove(oldConn)
                selectedConnections.add(newConn)
            }
        }
    }

    // === МЕТОДЫ РАСЧЕТА СОЕДИНЕНИЙ (ВНУТРИ ProjectCanvasState) ===
    fun calculateConnectionX(node: ProjectNode, connectionIndex: Int, totalConnections: Int): Float {
        if (totalConnections <= 1) return node.position.x
        val span = when (node) {
            is TransformerNode -> node.radiusOuter * 1.5f
            is GeneratorNode -> node.radius * 1.5f
            is SystemNode -> node.radius * 1.5f
            else -> NODE_WIDTH * 0.8f
        }
        return (node.position.x - span / 2) + connectionIndex * (span / (totalConnections - 1))
    }

    // Метод с исправленными отступами для круглых моделей и расчет привязок так, чтобы линии "прилипали" к координатам горизонтальных шин питания.
    fun getAttachmentPoint(node: ProjectNode, targetX: Float, isTop: Boolean): Point {
        // Стандартная логика привязок для всех моделей, КРОМЕ IT-ряда
        val yOffset = when (node) {
            is TransformerNode -> node.radiusOuter * 0.8f
            is GeneratorNode -> node.radius * 0.85f
            is SystemNode -> node.radius * 0.85f
            else -> getNodeHeight(node) / 2
        }
        return Point(targetX, if (isTop) node.position.y + yOffset else node.position.y - yOffset)
    }

    fun getItRackRowAttachmentPoint(node: ItRackRowNode, connectionIndex: Int, otherNodeX: Float): Point {
        if (node.feeds.isEmpty()) return node.position

        // 1. Поочередно связываем соединение с конкретным лучом (1:1)
        val feedIndex = connectionIndex % node.feeds.size
        val feed = node.feeds[feedIndex]

        // 2. Рассчитываем габариты и положение ряда
        val (totalWidth, totalHeight) = feature.projecteditor.ui.selection.getItRackRowSize(node)
        val topLeftY = node.position.y - totalHeight / 2

        val racksWidth = (node.racks.size * feature.projecteditor.ui.selection.RACK_WIDTH) + ((node.racks.size - 1) * feature.projecteditor.ui.selection.RACK_GAP)
        val racksStartX = node.position.x - racksWidth / 2

        val assignments = feature.projecteditor.ui.selection.calculateFeedAssignments(node.feeds, node.racks)
        val assignment = assignments[feedIndex] ?: return node.position

        val topTracksCount = assignments.values.filter { it.isTop }.maxOfOrNull { it.trackIndex + 1 } ?: 0
        val racksTopY = topLeftY + (if (topTracksCount > 0) feature.projecteditor.ui.selection.FEED_MARGIN + (topTracksCount - 1) * feature.projecteditor.ui.selection.FEED_LINE_SPACING else 0f)

        // 3. Вычисляем Y-координату именно этого луча
        val feedY = if (assignment.isTop) {
            topLeftY + (assignment.trackIndex * feature.projecteditor.ui.selection.FEED_LINE_SPACING)
        } else {
            racksTopY + feature.projecteditor.ui.selection.RACK_HEIGHT + feature.projecteditor.ui.selection.FEED_MARGIN + (assignment.trackIndex * feature.projecteditor.ui.selection.FEED_LINE_SPACING)
        }

        // 4. Находим края луча по оси X (крайние левые и правые подключенные стойки)
        val connectedIndices = node.racks.mapIndexedNotNull { index, rack ->
            if (feed.connectedRacks.contains(rack.index)) index else null
        }
        val minIdx = connectedIndices.minOrNull() ?: 0
        val maxIdx = connectedIndices.maxOrNull() ?: 0

        val leftX = racksStartX + minIdx * (feature.projecteditor.ui.selection.RACK_WIDTH + feature.projecteditor.ui.selection.RACK_GAP) + feature.projecteditor.ui.selection.RACK_WIDTH / 2
        val rightX = racksStartX + maxIdx * (feature.projecteditor.ui.selection.RACK_WIDTH + feature.projecteditor.ui.selection.RACK_GAP) + feature.projecteditor.ui.selection.RACK_WIDTH / 2

        // 5. Привязываемся к левому краю, если другой узел левее, и к правому, если правее
        val attachX = if (otherNodeX < node.position.x) leftX else rightX

        return Point(attachX, feedY)
    }

    // МЕТОД расчета линий с внедрением логики IT-ряда
    fun calculateConnectionPoints(conn: Connection): List<Point> {
        val fromNode = nodes.find { it.id == conn.fromId } ?: return emptyList()
        val toNode = nodes.find { it.id == conn.toId } ?: return emptyList()

        val outgoingConnections = connections.filter { it.fromId == fromNode.id }
        val outgoingIndex = outgoingConnections.indexOf(conn)
        val startX = calculateConnectionX(fromNode, outgoingIndex, outgoingConnections.size)

        val incomingConnections = connections.filter { it.toId == toNode.id }
        val incomingIndex = incomingConnections.indexOf(conn)
        val endX = calculateConnectionX(toNode, incomingIndex, incomingConnections.size)

        val isFromNodeOnTop = fromNode.position.y < toNode.position.y

        // ИСПОЛЬЗУЕМ ТОЧНЫЕ ТОЧКИ ПРИВЯЗКИ, если узел - это ряд стоек
        val startOffset = if (fromNode is ItRackRowNode) {
            getItRackRowAttachmentPoint(fromNode, outgoingIndex, toNode.position.x)
        } else {
            getAttachmentPoint(fromNode, startX, isFromNodeOnTop)
        }

        val endOffset = if (toNode is ItRackRowNode) {
            getItRackRowAttachmentPoint(toNode, incomingIndex, fromNode.position.x)
        } else {
            getAttachmentPoint(toNode, endX, !isFromNodeOnTop)
        }

        val result = mutableListOf<Point>()
        result.add(startOffset)

        if (conn.waypoints.isEmpty()) {
            val midY = (startOffset.y + endOffset.y) / 2f
            result.add(Point(startOffset.x, midY))
            result.add(Point(endOffset.x, midY))
        } else {
            val wps = conn.waypoints

            if (wps.first().x != startOffset.x) {
                result.add(Point(startOffset.x, wps.first().y))
            }

            result.addAll(wps)

            if (wps.last().x != endOffset.x) {
                result.add(Point(endOffset.x, wps.last().y))
            }
        }

        result.add(endOffset)
        return result
    }

    fun hitTestConnections(screenPos: Point): ConnectionHit? {
        val worldPos = screenToWorld(screenPos)
        val thresholdSq = (15f / scale) * (15f / scale)

        for (conn in connections) {
            if (selectedConnections.contains(conn)) {
                val pts = calculateConnectionPoints(conn)
                for (i in 1 until pts.size - 1) {
                    if ((worldPos - pts[i]).getDistanceSquared() < thresholdSq) {
                        return ConnectionHit.Waypoint(conn, i - 1)
                    }
                }
            }
        }
        for (conn in connections) {
            if (selectedConnections.contains(conn)) {
                val pts = calculateConnectionPoints(conn)
                for (i in 0 until pts.size - 1) {
                    val mid = (pts[i] + pts[i+1]) / 2f
                    if ((worldPos - mid).getDistanceSquared() < thresholdSq) return ConnectionHit.Midpoint(conn, i)
                }
            }
        }
        for (conn in connections) {
            val pts = calculateConnectionPoints(conn)
            for (i in 0 until pts.size - 1) {
                if (pointToSegmentDistanceSquared(worldPos, pts[i], pts[i+1]) < thresholdSq) return ConnectionHit.Segment(conn, i)
            }
        }
        return null
    }

    fun cleanupConnection(conn: Connection): Connection {
        if (conn.waypoints.isEmpty()) return conn

        var currentWps = conn.waypoints.toMutableList()
        val thresholdSq = (20f / scale) * (20f / scale) // Радиус "прилипания" для объединения
        var changed = true

        // Шаг 1. Объединение точек (удаляем схлопнувшиеся П-образные колена)
        while (changed && currentWps.size >= 2) {
            changed = false
            for (i in 0 until currentWps.size - 1) {
                if ((currentWps[i] - currentWps[i+1]).getDistanceSquared() < thresholdSq) {
                    currentWps.removeAt(i + 1)
                    currentWps.removeAt(i) // Удаляем сразу две точки, распрямляя линию
                    changed = true
                    break
                }
            }
        }

        // Шаг 2. Удаление лишних точек, если они выстроились в одну прямую линию
        changed = true
        while (changed && currentWps.size >= 3) {
            changed = false
            for (i in 0 until currentWps.size - 2) {
                val p1 = currentWps[i]
                val p2 = currentWps[i+1]
                val p3 = currentWps[i+2]

                val sameX = kotlin.math.abs(p1.x - p2.x) < 2f && kotlin.math.abs(p2.x - p3.x) < 2f
                val sameY = kotlin.math.abs(p1.y - p2.y) < 2f && kotlin.math.abs(p2.y - p3.y) < 2f

                if (sameX || sameY) {
                    currentWps.removeAt(i + 1) // Удаляем точку посередине прямого участка
                    changed = true
                    break
                }
            }
        }

        return conn.copy(waypoints = currentWps)
    }
}

sealed class ConnectionHit {
    abstract val connection: Connection
    data class Waypoint(override val connection: Connection, val index: Int) : ConnectionHit()
    data class Midpoint(override val connection: Connection, val index: Int) : ConnectionHit()
    data class Segment(override val connection: Connection, val index: Int) : ConnectionHit()
    data class SegmentDrag(override val connection: Connection, val w1Index: Int, val w2Index: Int) : ConnectionHit()
}

fun pointToSegmentDistanceSquared(p: Point, v: Point, w: Point): Float {
    val l2 = (w - v).getDistanceSquared()
    if (l2 == 0f) return (p - v).getDistanceSquared()
    var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
    t = t.coerceIn(0f, 1f)
    val proj = Point(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
    return (p - proj).getDistanceSquared()
}

/**
 * Вспомогательная функция для получения высоты узла.
 */
fun getNodeHeight(node: ProjectNode): Float {
    return when (node) {
        is TransformerNode -> {
            2f * node.radiusOuter + node.radiusInner
        }
        else -> NODE_HEIGHT
    }
}