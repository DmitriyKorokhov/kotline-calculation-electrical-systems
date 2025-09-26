package org.example.project

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
import ui.screens.home.HomeScreen
import ui.screens.projecteditor.ProjectView
import ui.screens.shieldeditor.ShieldEditorView
import ui.theme.AppDarkColors

/**
 * Sealed class для управления навигацией.
 */
sealed class Screen {
    object Home : Screen()
    object ProjectEditor : Screen()
    data class ShieldEditor(val shieldId: Int) : Screen()
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Редактор электрических систем",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        MaterialTheme(colors = AppDarkColors) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (val screen = currentScreen) {
                    is Screen.Home -> {
                        HomeScreen(
                            onCreateProject = {
                                ProjectRepository.createNewProject()
                                currentScreen = Screen.ProjectEditor
                            }
                        )
                    }
                    is Screen.ProjectEditor -> {
                        // ИСПРАВЛЕНИЕ: Передаем обязательный параметр 'state' из репозитория.
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
                            }
                        )
                    }
                }
            }
        }
    }
}

