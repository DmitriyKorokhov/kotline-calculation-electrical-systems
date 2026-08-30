package feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import core.storage.ProjectStorage
import feature.projecteditor.state.ProjectCanvasState

@Composable
fun HomeScreen(
    canvasState: ProjectCanvasState, // ДОБАВЛЕН ПАРАМЕТР
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onNewProject) {
            Text("Создать")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (ProjectStorage.loadProject(canvasState)) {
                onOpenProject()
            }
        }) {
            Text("Открыть..")
        }
    }
}