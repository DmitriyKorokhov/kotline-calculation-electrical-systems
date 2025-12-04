import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import data.ProjectRepository
import data.database.DatabaseFactory // <-- НОВЫЙ ИМПОРТ
import kotlinx.coroutines.runBlocking
import ui.screens.home.HomeScreen
import ui.screens.projecteditor.ProjectView
import ui.screens.shieldeditor.ShieldEditorView
import ui.theme.AppDarkColors

sealed class Screen {
    object Home : Screen()
    object ProjectEditor : Screen()
    data class ShieldEditor(val shieldId: Int) : Screen()
}

fun main() = application {
    // --- ИНИЦИАЛИЗАЦИЯ БД (runBlocking покрывает suspend init) ---
    runBlocking {
        try {
            DatabaseFactory.init()
        } catch (ex: Exception) {
            // Логирование/обработка ошибки и продолжение — по вашему сценарию
            println("Ошибка инициализации БД: ${ex.message}")
        }
    }

    Window(
        onCloseRequest = {
            // Здесь можно корректно закрыть БД, если у вас есть метод close()
            // DatabaseFactory.close()
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
                                // Открываем редактор щита с ID
                                currentScreen = Screen.ShieldEditor(shieldId)
                            }
                        )
                    }
                    is Screen.ShieldEditor -> {
                        ShieldEditorView(
                            shieldId = screen.shieldId,
                            onBack = {
                                // при возврате — сохраняем/обновляем состояние проекта, если нужно
                                currentScreen = Screen.ProjectEditor
                            }
                        )
                    }
                }
            }
        }
    }
}
