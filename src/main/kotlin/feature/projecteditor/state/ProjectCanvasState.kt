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
    var hoveredPin by mutableStateOf<Pair<ProjectNode, AnchorSide>?>(null)
    var isDraggingLineEnd by mutableStateOf(false)
    var draggingEndpointNodeId by mutableStateOf<Int?>(null)

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

    // === НОВЫЙ МЕТОД: Умное авто-распределение по граням ===
    fun getSmartAttachmentPoint(node: ProjectNode, conn: Connection, isSource: Boolean): Point {
        // Оставляем кастомную логику для IT-ряда (привязка к лучам), чтобы не сломать твои наработки
        if (node is ItRackRowNode) {
            val conns = if (isSource) connections.filter { it.fromId == node.id } else connections.filter { it.toId == node.id }
            val idx = conns.indexOf(conn)
            val otherNodeId = if (isSource) conn.toId else conn.fromId
            val otherNodeX = nodes.find { it.id == otherNodeId }?.position?.x ?: node.position.x
            return getItRackRowAttachmentPoint(node, maxOf(0, idx), otherNodeX)
        }

        val side = if (isSource) conn.fromSide else conn.toSide

        // Находим все соединения, подключенные к ЭТОМУ узлу и к ЭТОЙ ЖЕ стороне
        val sideConnections = connections.filter {
            (it.fromId == node.id && it.fromSide == side) ||
                    (it.toId == node.id && it.toSide == side)
        }.sortedBy { it.hashCode() } // Сортируем для стабильного порядка

        val index = sideConnections.indexOf(conn)
        val total = sideConnections.size

        // Получаем точные границы модели из SelectionMath
        val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)

        // Магия расталкивания: делим грань на (total + 1) частей
        val fraction = (maxOf(0, index) + 1).toFloat() / (total + 1).toFloat()

        return when (side) {
            AnchorSide.TOP -> Point(bounds.left + bounds.width * fraction, bounds.top)
            AnchorSide.BOTTOM -> Point(bounds.left + bounds.width * fraction, bounds.bottom)
            AnchorSide.LEFT -> Point(bounds.left, bounds.top + bounds.height * fraction)
            AnchorSide.RIGHT -> Point(bounds.right, bounds.top + bounds.height * fraction)
        }
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

    fun getPinPosition(node: ProjectNode, side: AnchorSide): Point {
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
        val thresholdSq = (25f / scale) * (25f / scale) // Радиус "магнита"

        for (node in nodes) {
            // Игнорируем узлы, к которым нельзя привязать этот конец линии
            if (draggingEndpointNodeId != null && node.id != draggingEndpointNodeId) continue

            for (side in AnchorSide.values()) {
                val pin = getPinPosition(node, side)
                if ((worldPos - pin).getDistanceSquared() < thresholdSq) {
                    hoveredPin = node to side
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

        // Встроенная очистка. На лету удаляем дубликаты и выпрямляем участки,
        // чтобы при перетаскивании моделей не оставалось "висящих" и оторванных сегментов.
        val cleanResult = mutableListOf<Point>()
        for (p in result) {
            if (cleanResult.isEmpty() || (kotlin.math.abs(cleanResult.last().x - p.x) > 1f || kotlin.math.abs(cleanResult.last().y - p.y) > 1f)) {
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
                val sameX = kotlin.math.abs(p1.x - p2.x) < 1f && kotlin.math.abs(p2.x - p3.x) < 1f
                val sameY = kotlin.math.abs(p1.y - p2.y) < 1f && kotlin.math.abs(p2.y - p3.y) < 1f
                if (sameX || sameY) {
                    cleanResult.removeAt(i + 1)
                    changed = true
                    break
                }
            }
        }

        return cleanResult
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

        var currentWps = conn.waypoints.toMutableList()
        val thresholdSq = (3f / scale) * (3f / scale)
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