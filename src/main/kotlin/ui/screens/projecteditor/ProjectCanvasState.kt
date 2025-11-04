package ui.screens.projecteditor

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import data.Connection
import data.GeneratorNode
import data.LevelLine
import data.PowerSourceNode
import data.ProjectNode
import data.ShieldNode
import data.TransformerNode
import kotlin.math.floor
import kotlin.math.max

// Константы размеров объектов и сетки
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH
private const val POWER_SOURCE_HEIGHT = NODE_HEIGHT / 4f
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f
/**
 * Класс-хранитель состояния (State Holder).
 */
class ProjectCanvasState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    val nodes = mutableStateListOf<ProjectNode>()
    val connections = mutableStateListOf<Connection>()
    val levels = mutableStateListOf<LevelLine>()
    private var nextId by mutableStateOf(1)

    var showNodeContextMenu by mutableStateOf(false)
    var showCanvasContextMenu by mutableStateOf(false)
    var contextMenuPosition by mutableStateOf(Offset.Zero)
    var selectedNode by mutableStateOf<ProjectNode?>(null)
    var showRenameDialog by mutableStateOf(false)
    var connectingFromNodeId by mutableStateOf<Int?>(null)

    fun resetCameraAndId() {
        scale = 1f
        offset = Offset.Zero
        nextId = 1
    }

    fun screenToWorld(screenPos: Offset): Offset {
        return (screenPos - offset) / scale
    }

    fun worldToScreen(worldPos: Offset): Offset {
        return worldPos * scale + offset
    }

    fun onPan(dragAmount: Offset) {
        offset += dragAmount
    }

    fun onZoom(scrollDelta: Float, zoomCenter: Offset) {
        val oldScale = scale
        val newScale = (scale * (1f - scrollDelta * 0.1f)).coerceIn(0.1f, 5f)
        scale = newScale
        offset = zoomCenter - ((zoomCenter - offset) / oldScale) * newScale
    }


    fun findNodeAtScreenPosition(screenPos: Offset): ProjectNode? {
        val worldPos = screenToWorld(screenPos)
        // Ищем в обратном порядке, чтобы верхние узлы проверялись первыми
        return nodes.findLast { node ->
            when (node) {
                is TransformerNode -> {
                    // Проверка попадания в одну из двух окружностей трансформатора
                    val c1 = Offset(node.position.x, node.position.y - node.radiusOuter / 2)
                    val c2 = Offset(node.position.x, node.position.y + node.radiusOuter / 2)
                    (worldPos - c1).getDistanceSquared() < node.radiusOuter * node.radiusOuter ||
                            (worldPos - c2).getDistanceSquared() < node.radiusOuter * node.radiusOuter
                }
                is GeneratorNode -> {
                    // Проверка попадания в круг генератора
                    (worldPos - node.position).getDistanceSquared() < node.radius * node.radius
                }
                else -> {
                    // Стандартная проверка для прямоугольных узлов
                    val width = if (node is PowerSourceNode) POWER_SOURCE_WIDTH else NODE_WIDTH
                    val height = getNodeHeight(node)
                    val nodeTopLeft = Offset(node.position.x - width / 2, node.position.y - height / 2)
                    worldPos.x >= nodeTopLeft.x && worldPos.x <= nodeTopLeft.x + width &&
                            worldPos.y >= nodeTopLeft.y && worldPos.y <= nodeTopLeft.y + height
                }
            }
        }
    }


    /**
     * Обновляет позицию узла по его ID.
     */
    fun updateNodePosition(nodeId: Int, newPosition: Offset) {
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index != -1) {
            val node = nodes[index]
            val updatedNode = when (node) {
                is ShieldNode -> node.copy(position = newPosition)
                is PowerSourceNode -> node.copy(position = newPosition)
                is TransformerNode -> node.copy(position = newPosition)
                is GeneratorNode -> node.copy(position = newPosition)
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

    fun addShieldNode(worldPos: Offset) {
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(ShieldNode(id = nextId++, name = "Щит", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addPowerSourceNode(worldPos: Offset) {
        val snappedPosition = snapToGrid(worldPos)
        nodes.add(PowerSourceNode(id = nextId++, name = "Источник", position = snappedPosition))
        showCanvasContextMenu = false
    }

    fun addTransformerNode(worldPos: Offset) {
        val snappedPosition = snapToGrid(worldPos)
        // default radii - можно настроить
        nodes.add(TransformerNode(id = nextId++, name = "T", position = snappedPosition, radiusOuter = 40f, radiusInner = 30f))
        showCanvasContextMenu = false
    }

    fun addLevelLine(worldPos: Offset) {
        levels.add(LevelLine(id = nextId++, yPosition = worldPos.y))
        showCanvasContextMenu = false
    }

    fun startConnecting() {
        connectingFromNodeId = selectedNode?.id
        showNodeContextMenu = false
    }

    fun tryFinishConnecting(clickedNode: ProjectNode?) {
        if (connectingFromNodeId != null && clickedNode != null && clickedNode.id != connectingFromNodeId) {
            connections.add(Connection(connectingFromNodeId!!, clickedNode.id))
        }
        connectingFromNodeId = null
    }

    fun deleteSelectedNode() {
        selectedNode?.let { nodeToDelete ->
            nodes.remove(nodeToDelete)
            connections.removeAll { it.fromId == nodeToDelete.id || it.toId == nodeToDelete.id }
        }
        showNodeContextMenu = false
    }

    fun updateSelectedNodeName(newName: String) {
        selectedNode?.let {
            val updatedNode = when (it) {
                is ShieldNode -> it.copy(name = newName)
                is PowerSourceNode -> it.copy(name = newName)
                is TransformerNode -> it.copy(name = newName)
                else -> it
            }
            val index = nodes.indexOf(it)
            if (index != -1) nodes[index] = updatedNode
        }
        showRenameDialog = false
    }

    private fun snapToGrid(position: Offset): Offset {
        val cellX = floor(position.x / GRID_WIDTH)
        val cellY = floor(position.y / GRID_HEIGHT)
        val snappedX = cellX * GRID_WIDTH + (GRID_WIDTH / 2)
        val snappedY = cellY * GRID_HEIGHT + (GRID_HEIGHT / 2)
        return Offset(snappedX, snappedY)
    }

    fun addGeneratorNode(worldPos: Offset) {
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
}

/**
 * Вспомогательная функция для получения высоты узла.
 */
fun getNodeHeight(node: ProjectNode): Float {
    return when (node) {
        is PowerSourceNode -> POWER_SOURCE_HEIGHT
        is TransformerNode -> {
            // вертикальный размер от верхней точки внешнего круга до нижней точки внутреннего круга
            2f * node.radiusOuter + node.radiusInner
        }
        else -> NODE_HEIGHT
    }
}