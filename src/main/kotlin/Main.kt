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
import feature.projecteditor.ui.ProjectView
import feature.shieldeditor.ui.ShieldEditorView
import core.theme.AppDarkColors
import feature.projecteditor.storage.ProjectStorage

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

    Window(
        onCloseRequest = {
            exitApplication()
        },
        title = "Редактор электрических систем",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        MaterialTheme(colors = AppDarkColors) {
            Surface(modifier = Modifier.fillMaxSize()) {
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
                            onBack = {
                                currentScreen = Screen.ProjectEditor
                            },
                            onSaveProject = {
                                ProjectStorage.saveProject(ProjectRepository.canvasState)
                            }
                        )
                    }
                }
            }
        }
    }
}
