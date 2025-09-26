import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ui.screens.HomeScreen
import ui.screens.ProjectView
import ui.screens.ShieldEditorView
import ui.theme.AppDarkColors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp

/**
 * Определяет текущий отображаемый экран.
 */
enum class Screen {
    HOME,
    PROJECT,
    SHIELD_EDITOR
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Редактор электрических систем",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        // --- NAVIGATION CONTROLLER ---
        var currentScreen by remember { mutableStateOf(Screen.HOME) }
        var activeShieldId by remember { mutableStateOf<Int?>(null) }

        MaterialTheme(colors = AppDarkColors) {
            Surface(modifier = Modifier.fillMaxSize()) {
                // В зависимости от текущего состояния, показываем нужный экран
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(
                        onCreateProject = { currentScreen = Screen.PROJECT }
                    )
                    Screen.PROJECT -> ProjectView(
                        onOpenShield = { shieldId ->
                            activeShieldId = shieldId
                            currentScreen = Screen.SHIELD_EDITOR
                        }
                    )
                    Screen.SHIELD_EDITOR -> ShieldEditorView(
                        shieldId = activeShieldId,
                        onBack = { currentScreen = Screen.PROJECT }
                    )
                }
            }
        }
    }
}
