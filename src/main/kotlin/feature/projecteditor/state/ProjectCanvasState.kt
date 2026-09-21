package feature.projecteditor.state

import androidx.compose.runtime.*
import feature.projecteditor.domain.*
import kotlin.math.floor
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import feature.projecteditor.ui.selection.getConnectionsInSelectionBox
import feature.projecteditor.ui.selection.getItRackRowSize
import feature.projecteditor.ui.selection.getNodesInSelectionBox
import feature.projecteditor.ui.utils.toPoint

// Константы размеров объектов и сетки
private const val NODE_HEIGHT = 80f

enum class CanvasToolMode {
    SELECT,     // Обычный режим (выделение, драг-н-дроп)
    ADD_TEXT,   // Ожидание клика для вставки текста
    ADD_CALLOUT, // Ожидание клика для вставки выноски
    DRAW_POLYLINE, DRAW_CIRCLE, DRAW_RECTANGLE, ADD_LEVEL
}

enum class HandleHitType {
    NONE,
    POLYLINE_POINT,
    CIRCLE_RADIUS,
    RECTANGLE_TOP,
    RECTANGLE_BOTTOM,
    RECTANGLE_LEFT,
    RECTANGLE_RIGHT
}

/**
 * Класс-хранитель состояния (State Holder).
 */
class ProjectCanvasState {

    // --- ПРЯМАЯ ССЫЛКА НА ДАННЫЕ (для удобства Compose) ---
    val nodes get() = ProjectRepository.nodes
    val connections get() = ProjectRepository.connections
    val levels get() = ProjectRepository.levels
    var nextId: Int
        get() = ProjectRepository.nextId
        set(value) { ProjectRepository.nextId = value }

    // --- UI STATE (Навигация) ---
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Point.Zero)
    var canvasSize by mutableStateOf(Point(1000f, 1000f))

    // --- UI STATE (Окна и меню) ---
    var showNodeContextMenu by mutableStateOf(false)
    var showCanvasContextMenu by mutableStateOf(false)
    var contextMenuPosition by mutableStateOf(Point.Zero)
    var showMultiSelectMenu by mutableStateOf(false)
    var showConnectionContextMenu by mutableStateOf(false)
    var showRackSettingsDialog by mutableStateOf(false)

    // --- UI STATE (Выделение и взаимодействие) ---
    var connectingFromNodeId by mutableStateOf<Int?>(null)
    var selectionStartScreen by mutableStateOf<Point?>(null)
    var selectionEndScreen by mutableStateOf<Point?>(null)
    var clipboardNodes by mutableStateOf<List<ProjectNode>>(emptyList())
    var clickedConnectionHit by mutableStateOf<ConnectionHit?>(null)
    var isCtrlPressed by mutableStateOf(false)
    var isShiftPressed by mutableStateOf(false)
    var hoveredPin by mutableStateOf<PinId?>(null)
    var isDraggingLineEnd by mutableStateOf(false)
    var draggingEndpointNodeId by mutableStateOf<Int?>(null)
    var clipboardConnections by mutableStateOf<List<Connection>>(emptyList())
    var isDraggingNode by mutableStateOf(false)

    val selectedNodeIds = mutableStateListOf<Int>()
    val selectedConnections = mutableStateListOf<Connection>()
    var selectedNode by mutableStateOf<ProjectNode?>(null)

    // --- UI STATE (Инструменты и Текст) ---
    var inlineEditingNodeId by mutableStateOf<Int?>(null)
    var inlineEditingText by mutableStateOf("")
    var selectedTab by mutableStateOf(feature.projecteditor.ui.components.EditorTab.EQUIPMENT)
    var currentToolMode by mutableStateOf(CanvasToolMode.SELECT)
    var previousTab by mutableStateOf<feature.projecteditor.ui.components.EditorTab?>(null)

    // Настройки аннотаций по умолчанию
    var defaultFontSize by mutableStateOf(14f)
    var defaultColorArgb by mutableStateOf(0xFFFFFFFF)
    var defaultIsBold by mutableStateOf(false)
    var defaultIsItalic by mutableStateOf(false)
    var defaultIsUnderline by mutableStateOf(false)
    var defaultIsStrikethrough by mutableStateOf(false)
    var defaultAlign by mutableStateOf(0)
    var defaultHasBackground by mutableStateOf(false)
    var defaultBackgroundColorArgb by mutableStateOf(0xFFFFFFFF)
    var defaultCalloutStartStyle by mutableStateOf(0)

    // --- ПОИСК И НАВИГАЦИЯ ---
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<ProjectNode>>(emptyList())
    var currentSearchIndex by mutableStateOf(0)

    // --- СТИЛИ ИЗ TOOLS ПАЛИТРЫ ---
    var currentLineType by mutableStateOf(0)
    var currentLineWeight by mutableStateOf(1)
    var currentLineColor by mutableStateOf(0xFF000000)

    // --- ВРЕМЕННОЕ СОСТОЯНИЕ РИСОВАНИЯ ---
    val tempPoints = mutableStateListOf<Point>()
    var currentMousePos by mutableStateOf<Point?>(null)

    var activeHandleHit by mutableStateOf<HandleHitType>(HandleHitType.NONE)
    var activeHandleNodeId by mutableStateOf<Int?>(null)
    var activeHandleIndex by mutableStateOf<Int>(-1) // Для полилинии
    var draggingLabelNodeId by mutableStateOf<Int?>(null)

    // ==========================================
    // ДЕЛЕГИРОВАНИЕ ОПЕРАЦИЙ В РЕПОЗИТОРИЙ
    // ==========================================

    fun resetCamera() {
        scale = 1f
        offset = Point.Zero
    }

    fun saveHistory() {
        ProjectRepository.saveHistory()
    }

    fun undo() {
        ProjectRepository.undo()
    }

    fun redo() {
        ProjectRepository.redo()
    }

    fun addShieldNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        val newNode = ShieldNode(id = ProjectRepository.nextId++, name = "Щит", position = snappedPosition)
        ProjectRepository.addNode(newNode)
        showCanvasContextMenu = false
    }

    fun addItRackRowNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(ItRackRowNode(id = ProjectRepository.nextId++, position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addTransformerNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(TransformerNode(id = ProjectRepository.nextId++, name = "T", position = snappedPosition, radiusOuter = 40f, radiusInner = 30f))
        showCanvasContextMenu = false
    }

    fun addRectifierNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(RectifierNode(id = ProjectRepository.nextId++, name = "Выпрямитель", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addUpsNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(UpsNode(id = ProjectRepository.nextId++, name = "ИБП", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addBatteryNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(BatteryNode(id = ProjectRepository.nextId++, name = "АКБ", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addSolarPanelNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(SolarPanelNode(id = ProjectRepository.nextId++, name = "СБ", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addInverterNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(InverterNode(id = ProjectRepository.nextId++, name = "Инвертор", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addSystemNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(SystemNode(id = ProjectRepository.nextId++, name = "Система", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addGeneratorNode(worldPos: Point) {
        val snappedPosition = snapToGrid(worldPos)
        ProjectRepository.addNode(GeneratorNode(id = ProjectRepository.nextId++, name = "G", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun deleteSelectedNodes() {
        ProjectRepository.removeNodes(selectedNodeIds.toSet())
        ProjectRepository.removeConnections(selectedConnections)
        clearSelection()
    }

    fun deleteSelectedNode() {
        selectedNode?.let { nodeToDelete ->
            ProjectRepository.removeNodes(setOf(nodeToDelete.id))
            clearSelection()
        }
        showNodeContextMenu = false
    }

    fun tryFinishConnecting(clickedNode: ProjectNode?) {
        if (connectingFromNodeId != null && clickedNode != null && clickedNode.id != connectingFromNodeId) {
            val fromNode = nodes.find { it.id == connectingFromNodeId }
            if (fromNode != null) {
                val (fromSide, toSide) = getClosestSides(fromNode, clickedNode)
                val newConn = Connection(
                    fromId = connectingFromNodeId!!,
                    toId = clickedNode.id,
                    fromSide = fromSide,
                    toSide = toSide
                )
                ProjectRepository.addConnection(newConn)
            }
        }
        connectingFromNodeId = null
    }

    fun updateConnection(oldConn: Connection, newConn: Connection) {
        ProjectRepository.updateConnection(oldConn, newConn)
        if (selectedConnections.contains(oldConn)) {
            selectedConnections.remove(oldConn)
            selectedConnections.add(newConn)
        }
    }

    fun updateNodePosition(nodeId: Int, newPosition: Point) {
        val node = nodes.find { it.id == nodeId } ?: return
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
            is TextNode -> node.copy(position = newPosition)
            is CalloutNode -> {
                val deltaX = newPosition.x - node.position.x
                val deltaY = newPosition.y - node.position.y
                node.copy(
                    position = newPosition,
                    targetPoint = Point(node.targetPoint.x + deltaX, node.targetPoint.y + deltaY)
                )
            }
            is CircleNode -> node.copy(position = newPosition)
            is RectangleNode -> node.copy(position = newPosition)
            is PolylineNode -> {
                val dx = newPosition.x - node.position.x
                val dy = newPosition.y - node.position.y
                node.copy(
                    position = newPosition,
                    points = node.points.map { Point(it.x + dx, it.y + dy) }
                )
            }
        }
        ProjectRepository.updateNode(updatedNode)
    }

    // ==========================================
    // ЛОГИКА UI И ИНТЕРАКТИВНОСТИ
    // ==========================================

    fun startConnecting() {
        connectingFromNodeId = selectedNode?.id
        showNodeContextMenu = false
    }

    fun clearSelection() {
        selectedNodeIds.clear()
        selectedConnections.clear()
    }

    fun finishPolyline() {
        if (currentToolMode == CanvasToolMode.DRAW_POLYLINE) {
            if (tempPoints.size > 1) {
                saveHistory()
                feature.projecteditor.state.ProjectRepository.addNode(
                    PolylineNode(
                        id = nextId++,
                        name = "",
                        position = tempPoints[0],
                        points = tempPoints.toList(),
                        colorArgb = currentLineColor,
                        lineWeight = currentLineWeight,
                        lineType = currentLineType
                    )
                )
            }
            tempPoints.clear()
            currentToolMode = CanvasToolMode.SELECT
        }
    }

    fun cancelTool() {
        if (currentToolMode != CanvasToolMode.SELECT) {
            tempPoints.clear()
            currentToolMode = CanvasToolMode.SELECT
        } else {
            clearSelection()
        }
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

    fun updateSearch(query: String) {
        searchQuery = query
        searchResults = emptyList()
        currentSearchIndex = 0

        if (query.isBlank()) return
        val q = query.lowercase()
        searchResults = nodes.filter { node ->
            val name = if (node is ShieldNode) {
                feature.shieldeditor.state.ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name }
            } else {
                node.name
            }
            name.lowercase().contains(q)
        }

        if (searchResults.isNotEmpty()) {
            centerOn(searchResults[0].position)
        }
    }

    fun nextSearchResult() {
        if (searchResults.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
        centerOn(searchResults[currentSearchIndex].position)
    }

    fun prevSearchResult() {
        if (searchResults.isEmpty()) return
        currentSearchIndex = if (currentSearchIndex - 1 < 0) searchResults.size - 1 else currentSearchIndex - 1
        centerOn(searchResults[currentSearchIndex].position)
    }

    private fun centerOn(worldPos: Point) {
        val screenCenterX = canvasSize.x / 2f
        val screenCenterY = canvasSize.y / 2f
        offset = Point(screenCenterX - worldPos.x * scale, screenCenterY - worldPos.y * scale)
    }

    fun findNodeAtScreenPosition(screenPos: Point): ProjectNode? {
        val worldPos = screenToWorld(screenPos)
        val strokeTolerance = 15f / scale // Погрешность клика (чтобы было легко попадать по тонким линиям)
        val strokeToleranceSq = strokeTolerance * strokeTolerance

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

                // ИСПРАВЛЕНИЕ ЗАДАЧИ 2: Выделение круга только по контуру
                is CircleNode -> {
                    val distSq = (worldPos - node.position).getDistanceSquared()
                    val dist = kotlin.math.sqrt(distSq.toDouble()).toFloat()
                    kotlin.math.abs(dist - node.radius) <= strokeTolerance
                }
                // ИСПРАВЛЕНИЕ ЗАДАЧИ 2: Выделение прямоугольника только по рамке
                is RectangleNode -> {
                    val rad = Math.toRadians(-node.rotationDegrees.toDouble())
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    val dx = worldPos.x - node.position.x
                    val dy = worldPos.y - node.position.y

                    // Переводим клик в локальные координаты прямоугольника с учетом поворота
                    val localX = node.position.x + (dx * cos - dy * sin)
                    val localY = node.position.y + (dx * sin + dy * cos)

                    val localDx = kotlin.math.abs(localX - node.position.x)
                    val localDy = kotlin.math.abs(localY - node.position.y)
                    val halfW = node.width / 2
                    val halfH = node.height / 2

                    val onVerticalBorder = kotlin.math.abs(localDx - halfW) <= strokeTolerance && localDy <= halfH + strokeTolerance
                    val onHorizontalBorder = kotlin.math.abs(localDy - halfH) <= strokeTolerance && localDx <= halfW + strokeTolerance

                    onVerticalBorder || onHorizontalBorder
                }
                // ИСПРАВЛЕНИЕ ЗАДАЧИ 3: Выделение полилинии только при клике на саму линию
                is PolylineNode -> {
                    var hit = false
                    if (node.points.isNotEmpty()) {
                        if (node.points.size == 1) {
                            hit = (worldPos - node.points[0]).getDistanceSquared() <= strokeToleranceSq
                        } else {
                            for (i in 0 until node.points.size - 1) {
                                if (pointToSegmentDistanceSquared(worldPos, node.points[i], node.points[i+1]) <= strokeToleranceSq) {
                                    hit = true
                                    break
                                }
                            }
                        }
                    }
                    hit
                }
                // Остальное оборудование выделяется по всей площади
                else -> {
                    val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)
                    worldPos.x >= bounds.left && worldPos.x <= bounds.right &&
                            worldPos.y >= bounds.top && worldPos.y <= bounds.bottom
                }
            }
        }
    }

    fun snapNodeToEndPosition(nodeId: Int) {
        val node = nodes.find { it.id == nodeId }
        node?.let {
            // Исключаем текст, выноски и всю геометрию из привязки к сетке
            if (it is TextNode || it is CalloutNode || it is CircleNode || it is RectangleNode || it is PolylineNode) return

            val snappedPosition = snapToGrid(it.position)
            updateNodePosition(it.id, snappedPosition)
        }
    }

    private fun snapToGrid(position: Point): Point {
        val cellX = floor(position.x / 200f)
        val cellY = floor(position.y / 140f)
        return Point(cellX * 200f + 100f, cellY * 140f + 70f)
    }

    fun getClosestSides(fromNode: ProjectNode, toNode: ProjectNode): Pair<AnchorSide, AnchorSide> {
        val fromBox = feature.projecteditor.ui.selection.getBoundingBox(fromNode)
        val toBox = feature.projecteditor.ui.selection.getBoundingBox(toNode)

        val dx = toBox.center.x - fromBox.center.x
        val dy = toBox.center.y - fromBox.center.y

        return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            if (dx > 0) Pair(AnchorSide.RIGHT, AnchorSide.LEFT) else Pair(AnchorSide.LEFT, AnchorSide.RIGHT)
        } else {
            if (dy > 0) Pair(AnchorSide.BOTTOM, AnchorSide.TOP) else Pair(AnchorSide.TOP, AnchorSide.BOTTOM)
        }
    }

    fun applySelectionBox() {
        val start = selectionStartScreen ?: return
        val end = selectionEndScreen ?: return

        val startWorld = screenToWorld(start)
        val endWorld = screenToWorld(end)

        val newNodes = getNodesInSelectionBox(nodes, startWorld, endWorld)
        val newConns = getConnectionsInSelectionBox(connections, this, startWorld, endWorld)

        if (isCtrlPressed) {
            selectedNodeIds.removeAll(newNodes)
            selectedConnections.removeAll(newConns)
        } else if (isShiftPressed) {
            newNodes.forEach { if (it !in selectedNodeIds) selectedNodeIds.add(it) }
            newConns.forEach { if (it !in selectedConnections) selectedConnections.add(it) }
        } else {
            selectedNodeIds.clear()
            selectedNodeIds.addAll(newNodes)
            selectedConnections.clear()
            selectedConnections.addAll(newConns)
        }
    }

    fun copySelectedNodes() {
        clipboardNodes = nodes.filter { it.id in selectedNodeIds }

        if (clipboardNodes.isNotEmpty()) {
            clipboardConnections = connections.filter { conn ->
                clipboardNodes.any { it.id == conn.fromId } && clipboardNodes.any { it.id == conn.toId }
            }
        } else {
            clipboardConnections = emptyList()
        }
    }

    fun pasteNodes(screenPos: Point? = null) {
        if (clipboardNodes.isEmpty()) return
        saveHistory() // Здесь ручное сохранение истории оправдано, так как мы мутируем списки напрямую, чтобы объединить в один шаг истории

        val minX = clipboardNodes.minOf { it.position.x }
        val minY = clipboardNodes.minOf { it.position.y }

        val deltaX = if (screenPos != null) screenToWorld(screenPos).x - minX else 50f
        val deltaY = if (screenPos != null) screenToWorld(screenPos).y - minY else 50f

        clearSelection()
        val idMapping = mutableMapOf<Int, Int>()

        clipboardNodes.forEach { node ->
            val newPos = Point(node.position.x + deltaX, node.position.y + deltaY)
            val newNodeId = ProjectRepository.nextId++
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
                is TextNode -> node.copy(id = newNodeId, position = newPos)
                is CalloutNode -> {
                    val newTarget = Point(node.targetPoint.x + deltaX, node.targetPoint.y + deltaY)
                    node.copy(id = newNodeId, position = newPos, targetPoint = newTarget)
                }
                is CircleNode -> node.copy(id = newNodeId, position = newPos)
                is RectangleNode -> node.copy(id = newNodeId, position = newPos)
                is PolylineNode -> {
                    val dx = newPos.x - node.position.x
                    val dy = newPos.y - node.position.y
                    node.copy(
                        id = newNodeId,
                        position = newPos,
                        points = node.points.map { Point(it.x + dx, it.y + dy) }
                    )
                }
            }

            nodes.add(newNode) // Напрямую добавляем в списки (чтобы не спамить историю)
            selectedNodeIds.add(newNodeId)
        }

        clipboardConnections.forEach { conn ->
            val newFromId = idMapping[conn.fromId] ?: return@forEach
            val newToId = idMapping[conn.toId] ?: return@forEach

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

    fun getSmartAttachmentPoint(node: ProjectNode, conn: Connection, isSource: Boolean): Point {
        if (node is ItRackRowNode) {
            val subId = if (isSource) conn.fromSubId else conn.toSubId
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
            (it.fromId == node.id && it.fromSide == side) || (it.toId == node.id && it.toSide == side)
        }.sortedBy { connections.indexOf(it) }

        val index = sideConnections.indexOf(conn)
        val total = sideConnections.size
        val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)

        if (node is TransformerNode || node is GeneratorNode || node is SystemNode) {
            return when (side) {
                AnchorSide.TOP -> Point(node.position.x, bounds.top)
                AnchorSide.BOTTOM -> Point(node.position.x, bounds.bottom)
                AnchorSide.LEFT -> Point(bounds.left, node.position.y)
                AnchorSide.RIGHT -> Point(bounds.right, node.position.y)
            }
        }

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

        val startOffset = getSmartAttachmentPoint(fromNode, conn, isSource = true)
        val endOffset = getSmartAttachmentPoint(toNode, conn, isSource = false)
        val result = mutableListOf<Point>()
        result.add(startOffset)

        if (conn.waypoints.isEmpty()) {
            val isFromVertical = conn.fromSide == AnchorSide.TOP || conn.fromSide == AnchorSide.BOTTOM
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
            val wps = conn.waypoints
            val firstWp = wps.first()
            when (conn.fromSide) {
                AnchorSide.TOP, AnchorSide.BOTTOM -> if (kotlin.math.abs(firstWp.x - startOffset.x) > 1f) result.add(Point(startOffset.x, firstWp.y))
                AnchorSide.LEFT, AnchorSide.RIGHT -> if (kotlin.math.abs(firstWp.y - startOffset.y) > 1f) result.add(Point(firstWp.x, startOffset.y))
            }
            result.addAll(wps)
            val lastWp = wps.last()
            when (conn.toSide) {
                AnchorSide.TOP, AnchorSide.BOTTOM -> if (kotlin.math.abs(lastWp.x - endOffset.x) > 1f) result.add(Point(endOffset.x, lastWp.y))
                AnchorSide.LEFT, AnchorSide.RIGHT -> if (kotlin.math.abs(lastWp.y - endOffset.y) > 1f) result.add(Point(lastWp.x, endOffset.y))
            }
        }

        result.add(endOffset)
        val cleanResult = mutableListOf<Point>()
        for (p in result) {
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
        val (_, totalHeight) = getItRackRowSize(node)
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

    fun getItRackRowAttachmentPoint(node: ItRackRowNode, feedIndex: Int, otherNodeX: Float): Point {
        val coords = getFeedCoordinates(node, feedIndex) ?: return node.position
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
        if (node is TransformerNode) {
            return listOf(PinId(node, AnchorSide.TOP), PinId(node, AnchorSide.BOTTOM))
        }
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
        val pts = calculateConnectionPoints(conn).toMutableList()
        val snapThreshold = 12f / scale

        for (i in 1 until pts.size - 1) {
            val prev = pts[i - 1]
            var pt = pts[i]
            if (kotlin.math.abs(pt.x - prev.x) < snapThreshold) pt = Point(prev.x, pt.y)
            if (kotlin.math.abs(pt.y - prev.y) < snapThreshold) pt = Point(pt.x, prev.y)
            pts[i] = pt
        }

        for (i in pts.size - 2 downTo 1) {
            val next = pts[i + 1]
            var pt = pts[i]
            if (kotlin.math.abs(pt.x - next.x) < snapThreshold) pt = Point(next.x, pt.y)
            if (kotlin.math.abs(pt.y - next.y) < snapThreshold) pt = Point(pt.x, next.y)
            pts[i] = pt
        }

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

        changed = true
        while (changed && pts.size > 2) {
            changed = false
            for (i in 0 until pts.size - 2) {
                val p1 = pts[i]
                val p2 = pts[i+1]
                val p3 = pts[i+2]
                val sameX = kotlin.math.abs(p1.x - p2.x) < 1f && kotlin.math.abs(p2.x - p3.x) < 1f
                val sameY = kotlin.math.abs(p1.y - p2.y) < 1f && kotlin.math.abs(p2.y - p3.y) < 1f
                if (sameX || sameY) {
                    pts.removeAt(i + 1)
                    changed = true
                    break
                }
            }
        }

        val newWaypoints = if (pts.size <= 2) emptyList() else pts.subList(1, pts.size - 1)
        return conn.copy(waypoints = newWaypoints)
    }

    fun finishInlineEditing() {
        val nodeId = inlineEditingNodeId ?: return
        val node = nodes.find { it.id == nodeId }

        if (node != null) {
            val textToSave = inlineEditingText.trim()

            if (textToSave.isNotBlank()) {
                saveHistory()

                if (node is ShieldNode) {
                    val data = feature.shieldeditor.state.ShieldStorage.loadOrCreate(node.id)
                    data.shieldName = textToSave
                    feature.shieldeditor.state.ShieldStorage.save(node.id, data)
                }

                val updatedNode = when (node) {
                    is ShieldNode -> node.copy(name = textToSave)
                    is TransformerNode -> node.copy(name = textToSave)
                    is GeneratorNode -> node.copy(name = textToSave)
                    is UpsNode -> node.copy(name = textToSave)
                    is BatteryNode -> node.copy(name = textToSave)
                    is SolarPanelNode -> node.copy(name = textToSave)
                    is InverterNode -> node.copy(name = textToSave)
                    is SystemNode -> node.copy(name = textToSave)
                    is ItRackRowNode -> node.copy(name = textToSave)
                    is RectifierNode -> node.copy(name = textToSave)
                    is TextNode -> node.copy(name = textToSave)
                    is CalloutNode -> node.copy(name = textToSave)
                    is CircleNode -> node.copy(name = textToSave)
                    is RectangleNode -> node.copy(name = textToSave)
                    is PolylineNode -> node.copy(name = textToSave)
                }
                ProjectRepository.updateNode(updatedNode)
            } else {
                saveHistory()
                ProjectRepository.removeNodes(setOf(node.id))
                selectedNodeIds.remove(node.id)
            }
        }

        inlineEditingNodeId = null
        inlineEditingText = ""

        if (previousTab != null) {
            selectedTab = previousTab!!
            previousTab = null
        }
    }

    fun updateInlineEditingTextProperties(
        fontSize: Float? = null,
        colorArgb: Long? = null,
        isBold: Boolean? = null,
        isItalic: Boolean? = null,
        isUnderline: Boolean? = null,
        isStrikethrough: Boolean? = null,
        align: Int? = null,
        hasBackground: Boolean? = null,
        backgroundColorArgb: Long? = null,
        calloutStartStyle: Int? = null
    ) {
        val nodeId = inlineEditingNodeId ?: return
        saveHistory()
        val node = nodes.find { it.id == nodeId } ?: return
        val updated = when (node) {
            is TextNode -> node.copy(
                fontSize = fontSize ?: node.fontSize,
                colorArgb = colorArgb ?: node.colorArgb,
                isBold = isBold ?: node.isBold,
                isItalic = isItalic ?: node.isItalic,
                isUnderline = isUnderline ?: node.isUnderline,
                isStrikethrough = isStrikethrough ?: node.isStrikethrough,
                align = align ?: node.align,
                hasBackground = hasBackground ?: node.hasBackground,
                backgroundColorArgb = backgroundColorArgb ?: node.backgroundColorArgb
            )
            is CalloutNode -> node.copy(
                fontSize = fontSize ?: node.fontSize,
                colorArgb = colorArgb ?: node.colorArgb,
                isBold = isBold ?: node.isBold,
                isItalic = isItalic ?: node.isItalic,
                isUnderline = isUnderline ?: node.isUnderline,
                isStrikethrough = isStrikethrough ?: node.isStrikethrough,
                hasBackground = hasBackground ?: node.hasBackground,
                backgroundColorArgb = backgroundColorArgb ?: node.backgroundColorArgb,
                startStyle = calloutStartStyle ?: node.startStyle
            )
            else -> node
        }
        ProjectRepository.updateNode(updated)
    }

    fun duplicateSelected() {
        copySelectedNodes()
        pasteNodes()
    }

    fun rotateSelectedNodes() {
        saveHistory()
        selectedNodeIds.forEach { id ->
            val node = nodes.find { it.id == id }
            if (node is RectangleNode) {
                ProjectRepository.updateNode(node.copy(rotationDegrees = node.rotationDegrees + 90f))
            }
            // Можно добавить поворот и для других узлов по необходимости
        }
    }

    fun updateSelectedGeometryProperties(
        lineType: Int? = null,
        lineWeight: Int? = null,
        lineColor: Long? = null
    ) {
        if (selectedNodeIds.isEmpty()) return
        saveHistory()
        selectedNodeIds.forEach { id ->
            val node = nodes.find { it.id == id } ?: return@forEach
            val updated = when (node) {
                is CircleNode -> node.copy(
                    lineType = lineType ?: node.lineType,
                    lineWeight = lineWeight ?: node.lineWeight,
                    colorArgb = lineColor ?: node.colorArgb
                )
                is RectangleNode -> node.copy(
                    lineType = lineType ?: node.lineType,
                    lineWeight = lineWeight ?: node.lineWeight,
                    colorArgb = lineColor ?: node.colorArgb
                )
                is PolylineNode -> node.copy(
                    lineType = lineType ?: node.lineType,
                    lineWeight = lineWeight ?: node.lineWeight,
                    colorArgb = lineColor ?: node.colorArgb
                )
                else -> node
            }
            ProjectRepository.updateNode(updated)
        }
    }

    fun snapLabelToClosestSide(nodeId: Int, dropScreenPos: Offset) {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1) return
        val node = nodes[index]

        // 1. Переводим экранные координаты курсора мыши в мировые координаты чертежа
        val dropWorldPos = screenToWorld(dropScreenPos.toPoint())

        // 2. Получаем границы модели
        val bounds = feature.projecteditor.ui.selection.getBoundingBox(node)

        // 3. Вычисляем координаты 4-х пинов оборудования (в мировых координатах)
        val topPin = Point(bounds.left + bounds.width / 2f, bounds.top)
        val bottomPin = Point(bounds.left + bounds.width / 2f, bounds.bottom)
        val leftPin = Point(bounds.left, bounds.top + bounds.height / 2f)
        val rightPin = Point(bounds.right, bounds.top + bounds.height / 2f)

        // 4. Находим дистанцию от курсора мыши до каждого пина
        val distances = mapOf(
            AnchorSide.TOP to (dropWorldPos - topPin).getDistanceSquared(),
            AnchorSide.BOTTOM to (dropWorldPos - bottomPin).getDistanceSquared(),
            AnchorSide.LEFT to (dropWorldPos - leftPin).getDistanceSquared(),
            AnchorSide.RIGHT to (dropWorldPos - rightPin).getDistanceSquared()
        )

        // 5. Выбираем сторону с минимальной дистанцией
        val closestSide = distances.minByOrNull { it.value }?.key ?: AnchorSide.RIGHT

        saveHistory()
        val updatedNode = when (node) {
            is ShieldNode -> node.copy(labelSide = closestSide)
            is TransformerNode -> node.copy(labelSide = closestSide)
            is GeneratorNode -> node.copy(labelSide = closestSide)
            is UpsNode -> node.copy(labelSide = closestSide)
            is BatteryNode -> node.copy(labelSide = closestSide)
            is SolarPanelNode -> node.copy(labelSide = closestSide)
            is InverterNode -> node.copy(labelSide = closestSide)
            is SystemNode -> node.copy(labelSide = closestSide)
            is ItRackRowNode -> node.copy(labelSide = closestSide)
            is RectifierNode -> node.copy(labelSide = closestSide)
            else -> node
        }

        nodes[index] = updatedNode // Сохраняем в локальный UI State
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