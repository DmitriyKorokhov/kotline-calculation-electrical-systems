package core.utils

import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectRepository

/**
 * Иммутабельный слепок фундаментальных данных проекта (без UI-состояния).
 */
data class ProjectSnapshot(
    val nodes: List<ProjectNode>,
    val connections: List<Connection>,
    val levels: List<LevelLine>,
    val nextId: Int
)

class ProjectHistoryManager(private val maxHistorySize: Int = 50) {
    private val undoStack = ArrayDeque<ProjectSnapshot>()
    private val redoStack = ArrayDeque<ProjectSnapshot>()

    fun pushState(repository: ProjectRepository) {
        redoStack.clear()
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(repository.createSnapshot())
    }

    fun undo(repository: ProjectRepository) {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeLast()
            redoStack.addLast(repository.createSnapshot())
            repository.restoreFrom(previousState)
        }
    }

    fun redo(repository: ProjectRepository) {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeLast()
            undoStack.addLast(repository.createSnapshot())
            repository.restoreFrom(nextState)
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    private fun ProjectRepository.createSnapshot(): ProjectSnapshot {
        // Глубокое копирование узлов, так как их координаты и свойства меняются
        val copiedNodes = this.nodes.map { node ->
            when (node) {
                is ShieldNode -> node.copy()
                is TransformerNode -> node.copy()
                is GeneratorNode -> node.copy()
                is UpsNode -> node.copy()
                is BatteryNode -> node.copy()
                is SolarPanelNode -> node.copy()
                is InverterNode -> node.copy()
                is SystemNode -> node.copy()
                is ItRackRowNode -> node.copy(
                    racks = node.racks.map { it.copy() },
                    feeds = node.feeds.map { it.copy(connectedRacks = it.connectedRacks.toSet()) }
                )
                is RectifierNode -> node.copy()
                is TextNode -> node.copy()
                is CalloutNode -> node.copy(targetPoint = node.targetPoint.copy())
                is CircleNode -> node.copy()
                is RectangleNode -> node.copy()
                is PolylineNode -> node.copy(points = node.points.toList())
            }
        }
        return ProjectSnapshot(
            nodes = copiedNodes,
            connections = this.connections.toList(), // Связи иммутабельны, достаточно shallow copy
            levels = this.levels.toList(),           // Уровни иммутабельны
            nextId = this.nextId                     // Сохраняем генератор ID
        )
    }

    private fun ProjectRepository.restoreFrom(snapshot: ProjectSnapshot) {
        this.nodes.clear()
        // Снова глубокое копирование при восстановлении, чтобы не связать стейт со слепком в истории
        this.nodes.addAll(snapshot.nodes.map { node ->
            when (node) {
                is ShieldNode -> node.copy()
                is TransformerNode -> node.copy()
                is GeneratorNode -> node.copy()
                is UpsNode -> node.copy()
                is BatteryNode -> node.copy()
                is SolarPanelNode -> node.copy()
                is InverterNode -> node.copy()
                is SystemNode -> node.copy()
                is ItRackRowNode -> node.copy(
                    racks = node.racks.map { it.copy() },
                    feeds = node.feeds.map { it.copy(connectedRacks = it.connectedRacks.toSet()) }
                )
                is RectifierNode -> node.copy()
                is TextNode -> node.copy()
                is CalloutNode -> node.copy(targetPoint = node.targetPoint.copy())
                is CircleNode -> node.copy()
                is RectangleNode -> node.copy()
                is PolylineNode -> node.copy(points = node.points.toList())
            }
        })

        this.connections.clear()
        this.connections.addAll(snapshot.connections)

        this.levels.clear()
        this.levels.addAll(snapshot.levels)

        this.nextId = snapshot.nextId
    }
}