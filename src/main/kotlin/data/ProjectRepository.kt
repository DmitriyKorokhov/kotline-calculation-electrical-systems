package data

import ui.screens.projecteditor.ProjectCanvasState

/**
 * Singleton object (единый экземпляр) для хранения состояния проекта.
 * Это гарантирует, что состояние холста сохраняется при переключении между экранами.
 */
object ProjectRepository {
    // Единственный экземпляр состояния холста для всего приложения.
    val canvasState = ProjectCanvasState()

    /**
     * Сбрасывает состояние до начального для создания нового проекта.
     */
    fun createNewProject() {
        canvasState.nodes.clear()
        canvasState.connections.clear()
        canvasState.levels.clear()
        canvasState.resetCameraAndId()
    }
}
