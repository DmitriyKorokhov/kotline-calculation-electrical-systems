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
    var connectingFromNodeId by mutableStateOf<Int?>(null)
    val selectedNodeIds = mutableStateListOf<Int>()
    var selectionStartScreen by mutableStateOf<Point?>(null)
    var selectionEndScreen by mutableStateOf<Point?>(null)
    var showMultiSelectMenu by mutableStateOf(false)
    var clipboardNodes by mutableStateOf<List<ProjectNode>>(emptyList())
    var showConnectionContextMenu by mutableStateOf(false)
    var clickedConnectionHit by mutableStateOf<ConnectionHit?>(null)
    val selectedConnections = mutableStateListOf<Connection>()
    var isCtrlPressed by mutableStateOf(false)
    var isShiftPressed by mutableStateOf(false)
    var showRackSettingsDialog by mutableStateOf(false)
    var hoveredPin by mutableStateOf<PinId?>(null)
    var isDraggingLineEnd by mutableStateOf(false)
    var draggingEndpointNodeId by mutableStateOf<Int?>(null)
    var clipboardConnections by mutableStateOf<List<Connection>>(emptyList())
    var inlineEditingNodeId by mutableStateOf<Int?>(null)
    var inlineEditingText by mutableStateOf("")

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
                else -> {
                    // Теперь зона клика идеально совпадает с визуальной моделью!
                    val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)
                    worldPos.x >= bounds.left && worldPos.x <= bounds.right &&
                            worldPos.y >= bounds.top && worldPos.y <= bounds.bottom
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

    fun getClosestSides(fromNode: ProjectNode, toNode: ProjectNode): Pair<AnchorSide, AnchorSide> {
        val fromBox = feature.projecteditor.ui.selection.getBoundingBox(fromNode)
        val toBox = feature.projecteditor.ui.selection.getBoundingBox(toNode)

        val dx = toBox.center.x - fromBox.center.x
        val dy = toBox.center.y - fromBox.center.y

        // Определяем, по какой оси модели разнесены сильнее
        return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            // Модели стоят слева-справа друг от друга
            if (dx > 0) Pair(AnchorSide.RIGHT, AnchorSide.LEFT)
            else Pair(AnchorSide.LEFT, AnchorSide.RIGHT)
        } else {
            // Модели стоят сверху-снизу друг от друга
            if (dy > 0) Pair(AnchorSide.BOTTOM, AnchorSide.TOP)
            else Pair(AnchorSide.TOP, AnchorSide.BOTTOM)
        }
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

        if (clipboardNodes.isNotEmpty()) {
            // Копируем линию ТОЛЬКО если оба её конца привязаны к скопированным моделям
            clipboardConnections = connections.filter { conn ->
                clipboardNodes.any { it.id == conn.fromId } && clipboardNodes.any { it.id == conn.toId }
            }
        } else {
            clipboardConnections = emptyList()
        }
    }

    // 3. Вставить скопированное
    fun pasteNodes(screenPos: Point? = null) {
        if (clipboardNodes.isEmpty()) return
        saveHistory()

        val minX = clipboardNodes.minOf { it.position.x }
        val minY = clipboardNodes.minOf { it.position.y }

        val deltaX: Float
        val deltaY: Float

        if (screenPos != null) {
            val worldPos = screenToWorld(screenPos)
            deltaX = worldPos.x - minX
            deltaY = worldPos.y - minY
        } else {
            // Сдвиг при вставке через клавиатуру
            deltaX = 50f
            deltaY = 50f
        }

        clearSelection()
        val idMapping = mutableMapOf<Int, Int>() // Маппинг старых ID на новые

        // Вставляем узлы
        clipboardNodes.forEach { node ->
            val newPos = Point(node.position.x + deltaX, node.position.y + deltaY)
            val newNodeId = nextId++
            idMapping[node.id] = newNodeId

            val newNode = when (node) {
                is ShieldNode -> node.copy(id = newNodeId, position = newPos)
                is TransformerNode -> node.copy(id = newNodeId, position = newPos)
                is GeneratorNode -> node.copy(id = newNodeId, position = newPos)
                is UpsNode -> node.copy(id = newNodeId, position = newPos)
                is BatteryNode -> node.copy(id = newNodeId, position = newPos)
                is SolarPanelNode -> node.copy(id = newNodeId, position = newPos)
                is InverterNode -> node.copy(id = newNodeId, position = newPos)
                is SystemNode -> node.copy(id = newNodeId, position = newPos)
                is ItRackRowNode -> node.copy(id = newNodeId, position = newPos)
                is RectifierNode -> node.copy(id = newNodeId, position = newPos)
            }

            nodes.add(newNode)
            selectedNodeIds.add(newNodeId)
        }

        // Вставляем линии
        clipboardConnections.forEach { conn ->
            val newFromId = idMapping[conn.fromId] ?: return@forEach
            val newToId = idMapping[conn.toId] ?: return@forEach

            // Сдвигаем все изломы
            val newWaypoints = conn.waypoints.map { Point(it.x + deltaX, it.y + deltaY) }

            val newConn = conn.copy(
                fromId = newFromId,
                toId = newToId,
                waypoints = newWaypoints
            )

            connections.add(newConn)
            selectedConnections.add(newConn)
        }
    }

    fun tryFinishConnecting(clickedNode: ProjectNode?) {
        if (connectingFromNodeId != null && clickedNode != null && clickedNode.id != connectingFromNodeId) {
            saveHistory()
            val fromNode = nodes.find { it.id == connectingFromNodeId }
            if (fromNode != null) {
                // Автоматически вычисляем ближайшие стороны
                val (fromSide, toSide) = getClosestSides(fromNode, clickedNode)

                connections.add(Connection(
                    fromId = connectingFromNodeId!!,
                    toId = clickedNode.id,
                    fromSide = fromSide,
                    toSide = toSide
                ))
            }
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

    fun getSmartAttachmentPoint(node: ProjectNode, conn: Connection, isSource: Boolean): Point {
        if (node is ItRackRowNode) {
            val subId = if (isSource) conn.fromSubId else conn.toSubId

            // Если мы явно привязали линию к лучу, берем его. Если нет - раскидываем автоматически по порядку
            val feedIndex = subId ?: run {
                val conns = if (isSource) connections.filter { it.fromId == node.id } else connections.filter { it.toId == node.id }
                val idx = conns.indexOf(conn).coerceAtLeast(0)
                if (node.feeds.isNotEmpty()) idx % node.feeds.size else 0
            }

            val otherNodeId = if (isSource) conn.toId else conn.fromId
            val otherNodeX = nodes.find { it.id == otherNodeId }?.position?.x ?: node.position.x

            return getItRackRowAttachmentPoint(node, feedIndex, otherNodeX)
        }

        val side = if (isSource) conn.fromSide else conn.toSide

        val sideConnections = connections.filter {
            (it.fromId == node.id && it.fromSide == side) ||
                    (it.toId == node.id && it.toSide == side)
        }.sortedBy { connections.indexOf(it) }

        val index = sideConnections.indexOf(conn)
        val total = sideConnections.size

        // Получаем точные границы модели из SelectionMath
        val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)

        if (node is TransformerNode || node is GeneratorNode || node is SystemNode) {
            return when (side) {
                AnchorSide.TOP -> Point(node.position.x, bounds.top)
                AnchorSide.BOTTOM -> Point(node.position.x, bounds.bottom)
                AnchorSide.LEFT -> Point(bounds.left, node.position.y)
                AnchorSide.RIGHT -> Point(bounds.right, node.position.y)
            }
        }

        // Магия расталкивания: делим грань на (total + 1) частей
        val fraction = (maxOf(0, index) + 1).toFloat() / (total + 1).toFloat()

        return when (side) {
            AnchorSide.TOP -> Point(bounds.left + bounds.width * fraction, bounds.top)
            AnchorSide.BOTTOM -> Point(bounds.left + bounds.width * fraction, bounds.bottom)
            AnchorSide.LEFT -> Point(bounds.left, bounds.top + bounds.height * fraction)
            AnchorSide.RIGHT -> Point(bounds.right, bounds.top + bounds.height * fraction)
        }
    }

    fun getPinPosition(node: ProjectNode, side: AnchorSide, subId: Int? = null): Point {
        if (node is ItRackRowNode && subId != null) {
            val coords = getFeedCoordinates(node, subId) ?: return node.position
            return if (side == AnchorSide.LEFT) Point(coords.leftX, coords.feedY) else Point(coords.rightX, coords.feedY)
        }

        val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)
        return when (side) {
            AnchorSide.TOP -> Point(bounds.left + bounds.width / 2f, bounds.top)
            AnchorSide.BOTTOM -> Point(bounds.left + bounds.width / 2f, bounds.bottom)
            AnchorSide.LEFT -> Point(bounds.left, bounds.top + bounds.height / 2f)
            AnchorSide.RIGHT -> Point(bounds.right, bounds.top + bounds.height / 2f)
        }
    }

    fun updateHoveredPin(screenPos: Point) {
        if (selectedConnections.size != 1 || selectedNodeIds.isNotEmpty() || !isDraggingLineEnd) {
            hoveredPin = null
            return
        }
        val worldPos = screenToWorld(screenPos)
        val thresholdSq = (25f / scale) * (25f / scale)
        for (node in nodes) {
            if (draggingEndpointNodeId != null && node.id != draggingEndpointNodeId) continue
            // Используем новую систему доступных пинов
            for (pinId in getAvailablePins(node)) {
                val pin = getPinPosition(pinId.node, pinId.side, pinId.subId)
                if ((worldPos - pin).getDistanceSquared() < thresholdSq) {
                    hoveredPin = pinId
                    return
                }
            }
        }
        hoveredPin = null
    }

    fun calculateConnectionPoints(conn: Connection): List<Point> {
        val fromNode = nodes.find { it.id == conn.fromId } ?: return emptyList()
        val toNode = nodes.find { it.id == conn.toId } ?: return emptyList()

        // 1. Получаем точные точки на гранях
        val startOffset = getSmartAttachmentPoint(fromNode, conn, isSource = true)
        val endOffset = getSmartAttachmentPoint(toNode, conn, isSource = false)

        val result = mutableListOf<Point>()
        result.add(startOffset)

        if (conn.waypoints.isEmpty()) {
            // 2. Автоматическая базовая маршрутизация для новых линий
            val isFromVertical = conn.fromSide == AnchorSide.TOP || conn.fromSide == AnchorSide.BOTTOM

            // Если выходим сверху/снизу, сначала идем по оси Y. Если слева/справа — по оси X.
            if (isFromVertical) {
                val midY = (startOffset.y + endOffset.y) / 2f
                result.add(Point(startOffset.x, midY))
                result.add(Point(endOffset.x, midY))
            } else {
                val midX = (startOffset.x + endOffset.x) / 2f
                result.add(Point(midX, startOffset.y))
                result.add(Point(midX, endOffset.y))
            }
        } else {
            // 3. Умное сохранение прямых углов при ручном редактировании
            val wps = conn.waypoints
            val firstWp = wps.first()

            // Выход из начальной модели строго перпендикулярно грани (используем порог 1f против багов округления)
            when (conn.fromSide) {
                AnchorSide.TOP, AnchorSide.BOTTOM -> if (kotlin.math.abs(firstWp.x - startOffset.x) > 1f) result.add(Point(startOffset.x, firstWp.y))
                AnchorSide.LEFT, AnchorSide.RIGHT -> if (kotlin.math.abs(firstWp.y - startOffset.y) > 1f) result.add(Point(firstWp.x, startOffset.y))
            }

            result.addAll(wps)

            val lastWp = wps.last()
            // Вход в конечную модель строго перпендикулярно грани
            when (conn.toSide) {
                AnchorSide.TOP, AnchorSide.BOTTOM -> if (kotlin.math.abs(lastWp.x - endOffset.x) > 1f) result.add(Point(endOffset.x, lastWp.y))
                AnchorSide.LEFT, AnchorSide.RIGHT -> if (kotlin.math.abs(lastWp.y - endOffset.y) > 1f) result.add(Point(lastWp.x, endOffset.y))
            }
        }

        result.add(endOffset)

        val cleanResult = mutableListOf<Point>()
        for (p in result) {
            // порог дубликатов уменьшен до 0.5f
            if (cleanResult.isEmpty() || (kotlin.math.abs(cleanResult.last().x - p.x) > 0.5f || kotlin.math.abs(cleanResult.last().y - p.y) > 0.5f)) {
                cleanResult.add(p)
            }
        }

        var changed = true
        while (changed && cleanResult.size >= 3) {
            changed = false
            for (i in 0 until cleanResult.size - 2) {
                val p1 = cleanResult[i]
                val p2 = cleanResult[i+1]
                val p3 = cleanResult[i+2]
                // порог схлопывания прямых участков уменьшен до 0.5f
                val sameX = kotlin.math.abs(p1.x - p2.x) < 0.5f && kotlin.math.abs(p2.x - p3.x) < 0.5f
                val sameY = kotlin.math.abs(p1.y - p2.y) < 0.5f && kotlin.math.abs(p2.y - p3.y) < 0.5f
                if (sameX || sameY) {
                    cleanResult.removeAt(i + 1)
                    changed = true
                    break
                }
            }
        }

        return cleanResult
    }

    data class FeedCoords(val feedY: Float, val leftX: Float, val rightX: Float)

    private fun getFeedCoordinates(node: ItRackRowNode, feedIndex: Int): FeedCoords? {
        if (node.feeds.isEmpty() || feedIndex !in node.feeds.indices) return null
        val feed = node.feeds[feedIndex]

        val (totalWidth, totalHeight) = feature.projecteditor.ui.selection.getItRackRowSize(node)
        val topLeftY = node.position.y - totalHeight / 2
        val racksWidth = (node.racks.size * feature.projecteditor.ui.selection.RACK_WIDTH) + ((node.racks.size - 1) * feature.projecteditor.ui.selection.RACK_GAP)
        val racksStartX = node.position.x - racksWidth / 2

        val assignments = feature.projecteditor.ui.selection.calculateFeedAssignments(node.feeds, node.racks)
        val assignment = assignments[feedIndex] ?: return null

        val topTracksCount = assignments.values.filter { it.isTop }.maxOfOrNull { it.trackIndex + 1 } ?: 0
        val racksTopY = topLeftY + (if (topTracksCount > 0) feature.projecteditor.ui.selection.FEED_MARGIN + (topTracksCount - 1) * feature.projecteditor.ui.selection.FEED_LINE_SPACING else 0f)

        val feedY = if (assignment.isTop) {
            topLeftY + (assignment.trackIndex * feature.projecteditor.ui.selection.FEED_LINE_SPACING)
        } else {
            racksTopY + feature.projecteditor.ui.selection.RACK_HEIGHT + feature.projecteditor.ui.selection.FEED_MARGIN + (assignment.trackIndex * feature.projecteditor.ui.selection.FEED_LINE_SPACING)
        }

        val connectedIndices = node.racks.mapIndexedNotNull { index, rack ->
            if (feed.connectedRacks.contains(rack.index)) index else null
        }
        val minIdx = connectedIndices.minOrNull() ?: 0
        val maxIdx = connectedIndices.maxOrNull() ?: 0

        val leftX = racksStartX + minIdx * (feature.projecteditor.ui.selection.RACK_WIDTH + feature.projecteditor.ui.selection.RACK_GAP) + feature.projecteditor.ui.selection.RACK_WIDTH / 2
        val rightX = racksStartX + maxIdx * (feature.projecteditor.ui.selection.RACK_WIDTH + feature.projecteditor.ui.selection.RACK_GAP) + feature.projecteditor.ui.selection.RACK_WIDTH / 2

        return FeedCoords(feedY, leftX, rightX)
    }

    // Полностью ЗАМЕНИТЕ старую getItRackRowAttachmentPoint на эту:
    fun getItRackRowAttachmentPoint(node: ItRackRowNode, feedIndex: Int, otherNodeX: Float): Point {
        val coords = getFeedCoordinates(node, feedIndex) ?: return node.position
        // Автоматически меняем лево/право в зависимости от положения второй модели!
        val attachX = if (otherNodeX < node.position.x) coords.leftX else coords.rightX
        return Point(attachX, coords.feedY)
    }

    fun getAvailablePins(node: ProjectNode): List<PinId> {
        if (node is ItRackRowNode) {
            val pins = mutableListOf<PinId>()
            node.feeds.indices.forEach { feedIndex ->
                pins.add(PinId(node, AnchorSide.LEFT, feedIndex))
                pins.add(PinId(node, AnchorSide.RIGHT, feedIndex))
            }
            return pins
        }
        // ИСПРАВЛЕНИЕ: Трансформатору оставляем только верх и низ
        if (node is TransformerNode) {
            return listOf(PinId(node, AnchorSide.TOP), PinId(node, AnchorSide.BOTTOM))
        }
        // Для остальных моделей - классические 4 стороны
        return listOf(
            PinId(node, AnchorSide.TOP), PinId(node, AnchorSide.BOTTOM),
            PinId(node, AnchorSide.LEFT), PinId(node, AnchorSide.RIGHT)
        )
    }

    fun hitTestConnections(screenPos: Point): ConnectionHit? {
        val worldPos = screenToWorld(screenPos)
        val thresholdSq = (15f / scale) * (15f / scale)

        for (conn in connections) {
            if (selectedConnections.contains(conn)) {
                val pts = calculateConnectionPoints(conn)
                if (pts.isNotEmpty()) {
                    if ((worldPos - pts.first()).getDistanceSquared() < thresholdSq) return ConnectionHit.Endpoint(conn, true)
                    if ((worldPos - pts.last()).getDistanceSquared() < thresholdSq) return ConnectionHit.Endpoint(conn, false)
                }
            }
        }

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

        // 1. Берем РЕАЛЬНУЮ отрисованную линию со всеми авто-коленами от моделей
        val pts = calculateConnectionPoints(conn).toMutableList()

        // Комфортный магнит (12 пикселей масштабируются при зуме)
        val snapThreshold = 12f / scale

        // 2. Двойной проход магнита (цепная реакция)

        // Проход вперед (примагничиваем к предыдущим сегментам)
        for (i in 1 until pts.size - 1) {
            val prev = pts[i - 1]
            var pt = pts[i]

            if (kotlin.math.abs(pt.x - prev.x) < snapThreshold) pt = Point(prev.x, pt.y)
            if (kotlin.math.abs(pt.y - prev.y) < snapThreshold) pt = Point(pt.x, prev.y)

            pts[i] = pt
        }

        // Проход назад (примагничиваем к последующим сегментам и финишу)
        for (i in pts.size - 2 downTo 1) {
            val next = pts[i + 1]
            var pt = pts[i]

            if (kotlin.math.abs(pt.x - next.x) < snapThreshold) pt = Point(next.x, pt.y)
            if (kotlin.math.abs(pt.y - next.y) < snapThreshold) pt = Point(pt.x, next.y)

            pts[i] = pt
        }

        // 3. Удаление слипшихся дубликатов (схлопнувшиеся колена)
        var changed = true
        while (changed && pts.size > 2) {
            changed = false
            for (i in 0 until pts.size - 1) {
                if ((pts[i] - pts[i+1]).getDistanceSquared() < 1f) {
                    pts.removeAt(i)
                    changed = true
                    break
                }
            }
        }

        // 4. Выпрямление длинных прямых участков (удаление промежуточных точек)
        changed = true
        while (changed && pts.size > 2) {
            changed = false
            for (i in 0 until pts.size - 2) {
                val p1 = pts[i]
                val p2 = pts[i+1]
                val p3 = pts[i+2]

                // Если три точки лежат на идеальной прямой
                val sameX = kotlin.math.abs(p1.x - p2.x) < 1f && kotlin.math.abs(p2.x - p3.x) < 1f
                val sameY = kotlin.math.abs(p1.y - p2.y) < 1f && kotlin.math.abs(p2.y - p3.y) < 1f

                if (sameX || sameY) {
                    pts.removeAt(i + 1) // Удаляем центральную точку
                    changed = true
                    break
                }
            }
        }

        // 5. Возвращаем только внутренние изломы (без старта и финиша моделей)
        val newWaypoints = if (pts.size <= 2) emptyList() else pts.subList(1, pts.size - 1)

        return conn.copy(waypoints = newWaypoints)
    }

    fun finishInlineEditing() {
        val nodeId = inlineEditingNodeId ?: return
        val index = nodes.indexOfFirst { it.id == nodeId }

        if (index != -1 && inlineEditingText.isNotBlank()) {
            saveHistory()
            val node = nodes[index]

            // 1. Сохранение имени щита в хранилище (если это щит)
            if (node is ShieldNode) {
                val data = feature.shieldeditor.state.ShieldStorage.loadOrCreate(node.id)
                data.shieldName = inlineEditingText
                feature.shieldeditor.state.ShieldStorage.save(node.id, data)
            }

            // 2. Обновление самой модели в графе напрямую по индексу
            val updatedNode = when (node) {
                is ShieldNode -> node.copy(name = inlineEditingText)
                is TransformerNode -> node.copy(name = inlineEditingText)
                is GeneratorNode -> node.copy(name = inlineEditingText)
                is UpsNode -> node.copy(name = inlineEditingText)
                is BatteryNode -> node.copy(name = inlineEditingText)
                is SolarPanelNode -> node.copy(name = inlineEditingText)
                is InverterNode -> node.copy(name = inlineEditingText)
                is SystemNode -> node.copy(name = inlineEditingText)
                is ItRackRowNode -> node.copy(name = inlineEditingText)
                is RectifierNode -> node.copy(name = inlineEditingText)
            }
            nodes[index] = updatedNode
        }

        inlineEditingNodeId = null
        inlineEditingText = ""
    }
}

sealed class ConnectionHit {
    abstract val connection: Connection
    data class Endpoint(override val connection: Connection, val isSource: Boolean) : ConnectionHit()
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