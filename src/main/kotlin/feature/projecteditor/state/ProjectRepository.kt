package feature.projecteditor.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import feature.projecteditor.domain.Connection
import feature.projecteditor.domain.LevelLine
import feature.projecteditor.domain.ProjectNode
import core.utils.ProjectHistoryManager

/**
 * Единый источник истины (Single Source of Truth) для данных проекта.
 */
object ProjectRepository {
    // 1. ФУНДАМЕНТАЛЬНЫЕ ДАННЫЕ ПРОЕКТА
    val nodes = mutableStateListOf<ProjectNode>()
    val connections = mutableStateListOf<Connection>()
    val levels = mutableStateListOf<LevelLine>()
    var nextId by mutableStateOf(1)

    // 2. ИСТОРИЯ
    private val historyManager = ProjectHistoryManager()

    fun saveHistory() {
        historyManager.pushState(this) // Теперь сохраняет слепок репозитория, а не UI
    }

    fun undo() {
        historyManager.undo(this)
    }

    fun redo() {
        historyManager.redo(this)
    }

    // 3. БАЗОВЫЕ CRUD-ОПЕРАЦИИ (Создание, Чтение, Обновление, Удаление)
    fun addNode(node: ProjectNode) {
        saveHistory()
        nodes.add(node)
    }

    fun addConnection(connection: Connection) {
        saveHistory()
        connections.add(connection)
    }

    fun removeNodes(nodeIds: Set<Int>) {
        saveHistory()
        nodes.removeAll { it.id in nodeIds }
        // Каскадное удаление связей, привязанных к удаленным узлам
        connections.removeAll { it.fromId in nodeIds || it.toId in nodeIds }
    }

    fun removeConnections(connectionsToRemove: List<Connection>) {
        saveHistory()
        connections.removeAll(connectionsToRemove)
    }

    fun updateNode(updatedNode: ProjectNode) {
        val index = nodes.indexOfFirst { it.id == updatedNode.id }
        if (index != -1) {
            nodes[index] = updatedNode
        }
    }

    fun updateConnection(oldConn: Connection, newConn: Connection) {
        val index = connections.indexOf(oldConn)
        if (index != -1) {
            connections[index] = newConn
        }
    }

    fun createNewProject() {
        nodes.clear()
        connections.clear()
        levels.clear()
        nextId = 1
        historyManager.clear()
    }
}