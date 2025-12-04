package ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import storage.ProjectStorage
import data.ProjectRepository

/**
 * Экран 1: Начальное окно.
 * @param onCreateProject Лямбда-функция для перехода на экран проекта.
 */
@Composable
fun HomeScreen(onNewProject: () -> Unit, onOpenProject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onNewProject) { // ИСПРАВЛЕНО: было onCreateProject
            Text("Создать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (ProjectStorage.loadProject(ProjectRepository.canvasState)) {
                onOpenProject()
            }
        }) {
            Text("Открыть проект")
        }
    }
}