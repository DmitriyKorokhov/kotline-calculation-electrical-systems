package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран 1: Начальное окно.
 * @param onCreateProject Лямбда-функция для перехода на экран проекта.
 */
@Composable
fun HomeScreen(onCreateProject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onCreateProject) {
            Text("Создать")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO */ }, enabled = false) {
            Text("Открыть...")
        }
    }
}