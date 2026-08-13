package feature.projecteditor.state

import androidx.compose.runtime.*
import feature.projecteditor.domain.*
import kotlin.math.floor
import androidx.compose.runtime.mutableStateListOf
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
                is GeneratorNode -> {
                    // Проверка попадания в круг генератора
                    (worldPos - node.position).getDistanceSquared() < node.radius * node.radius
                }
                is SystemNode -> (worldPos - node.position).getDistanceSquared() < node.radius * node.radius
                else -> {
                    // ВОССТАНОВЛЕНО: переменные width и height для правильного расчета клика
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

    fun addTransformerNode(worldPos: Point) {
        saveHistory()
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(TransformerNode(id = nextId++, name = "T", position = snappedPosition, radiusOuter = 40f, radiusInner = 30f))
        showCanvasContextMenu = false
    }

    fun startConnecting() {
        connectingFromNodeId = selectedNode?.id
        showNodeContextMenu = false
    }

    fun tryFinishConnecting(clickedNode: ProjectNode?) {
        if (connectingFromNodeId != null && clickedNode != null && clickedNode.id != connectingFromNodeId) {
            saveHistory()
            connections.add(Connection(connectingFromNodeId!!, clickedNode.id))
        }
        connectingFromNodeId = null
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
    }

    fun toggleOrAddSelection(nodeId: Int) {
        // По ТЗ: если кликнули на невыделенную модель, она добавляется к выделенным
        if (!selectedNodeIds.contains(nodeId)) {
            selectedNodeIds.add(nodeId)
        }
    }

    fun applySelectionBox() {
        val start = selectionStartScreen ?: return
        val end = selectionEndScreen ?: return

        val startWorld = screenToWorld(start)
        val endWorld = screenToWorld(end)

        val newSelection = getNodesInSelectionBox(nodes, startWorld, endWorld)
        selectedNodeIds.clear() // Сбрасываем старое выделение
        selectedNodeIds.addAll(newSelection) // Применяем то, что попало в рамку
    }

    // 1. Удалить выделенное
    fun deleteSelectedNodes() {
        saveHistory()
        nodes.removeAll { it.id in selectedNodeIds }
        connections.removeAll { it.fromId in selectedNodeIds || it.toId in selectedNodeIds }
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