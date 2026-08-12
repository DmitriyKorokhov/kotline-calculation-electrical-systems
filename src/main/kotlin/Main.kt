import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import feature.projecteditor.state.ProjectRepository
import data.DatabaseFactory
import kotlinx.coroutines.runBlocking
import feature.home.HomeScreen
import feature.home.HomeTitleBar
import feature.projecteditor.ui.ProjectView
import feature.projecteditor.ui.components.ProjectTitleBar
import feature.shieldeditor.ui.ShieldEditorView
import core.theme.AppDarkColors
import core.storage.ProjectStorage

sealed class Screen {
    object Home : Screen()
    object ProjectEditor : Screen()
    data class ShieldEditor(val shieldId: Int) : Screen()
}

fun main() = application {
    runBlocking {
        try {
            DatabaseFactory.init()
        } catch (ex: Exception) {
            println("Ошибка инициализации БД: ${ex.message}")
        }
    }

    val windowState = rememberWindowState(width = 1200.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true // ОТКЛЮЧАЕМ СИСТЕМНУЮ РАМКУ!
    ) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        MaterialTheme(colors = AppDarkColors) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. ДИНАМИЧЕСКАЯ ШАПКА ОКНА
                    when (currentScreen) {
                        is Screen.Home -> {
                            HomeTitleBar(windowState, onClose = ::exitApplication)
                        }
                        is Screen.ProjectEditor -> {
                            ProjectTitleBar(
                                windowState = windowState,
                                onClose = ::exitApplication,
                                onSaveProject = { ProjectStorage.saveProject(ProjectRepository.canvasState) },
                                onLoadProject = { ProjectStorage.loadProject(ProjectRepository.canvasState) }
                            )
                        }
                        is Screen.ShieldEditor -> {
                            // Временно ставим шапку от Home, чтобы окно можно было таскать и закрывать
                            HomeTitleBar(windowState, onClose = ::exitApplication)
                        }
                    }

                    // 2. ОСНОВНОЙ КОНТЕНТ (занимает всё оставшееся место)
                    Box(modifier = Modifier.weight(1f)) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    onNewProject = {
                                        ProjectRepository.createNewProject()
                                        currentScreen = Screen.ProjectEditor
                                    },
                                    onOpenProject = {
                                        currentScreen = Screen.ProjectEditor
                                    }
                                )
                            }
                            is Screen.ProjectEditor -> {
                                ProjectView(
                                    state = ProjectRepository.canvasState,
                                    onOpenShield = { shieldId ->
                                        currentScreen = Screen.ShieldEditor(shieldId)
                                    }
                                )
                            }
                            is Screen.ShieldEditor -> {
                                ShieldEditorView(
                                    shieldId = screen.shieldId,
                                    onBack = { currentScreen = Screen.ProjectEditor },
                                    onSaveProject = { ProjectStorage.saveProject(ProjectRepository.canvasState) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}