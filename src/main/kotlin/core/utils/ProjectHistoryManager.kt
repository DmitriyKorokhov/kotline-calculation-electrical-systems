package core.utils

import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectCanvasState

/**
 * Иммутабельный слепок состояния холста.
 */
data class CanvasSnapshot(
    val nodes: List<ProjectNode>,
    val connections: List<Connection>,
    val levels: List<LevelLine>,
    val nextId: Int
)

class ProjectHistoryManager(private val maxHistorySize: Int = 50) {
    private val undoStack = ArrayDeque<CanvasSnapshot>()
    private val redoStack = ArrayDeque<CanvasSnapshot>()

    fun pushState(currentState: ProjectCanvasState) {
        redoStack.clear()
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(currentState.createSnapshot())
    }

    fun undo(currentState: ProjectCanvasState) {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeLast()
            redoStack.addLast(currentState.createSnapshot())
            currentState.restoreFrom(previousState)
        }
    }

    fun redo(currentState: ProjectCanvasState) {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeLast()
            undoStack.addLast(currentState.createSnapshot())
            currentState.restoreFrom(nextState)
        }
    }

    private fun ProjectCanvasState.createSnapshot(): CanvasSnapshot {
        // Глубокое копирование узлов (так как их координаты меняются)
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
            }
        }
        return CanvasSnapshot(
            nodes = copiedNodes,
            connections = this.connections.toList(), // Связи иммутабельны, достаточно shallow copy
            levels = this.levels.toList(),           // Уровни иммутабельны
            nextId = this.nextId                     // Сохраняем генератор ID
        )
    }

    private fun ProjectCanvasState.restoreFrom(snapshot: CanvasSnapshot) {
        this.nodes.clear()
        // Опять глубокое копирование при восстановлении, чтобы не связать стейт со слепком в истории
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
            }
        })

        this.connections.clear()
        this.connections.addAll(snapshot.connections)

        this.levels.clear()
        this.levels.addAll(snapshot.levels)

        this.nextId = snapshot.nextId
    }
}
