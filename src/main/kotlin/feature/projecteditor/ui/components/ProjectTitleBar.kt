package feature.projecteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState

@Composable
fun WindowScope.ProjectTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    onSaveProject: () -> Unit,
    onLoadProject: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    WindowDraggableArea {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colors.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка меню Файл
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Меню", tint = MaterialTheme.colors.onSurface)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(onClick = { showMenu = false; onSaveProject() }) { Text("Сохранить как..") }
                    DropdownMenuItem(onClick = { showMenu = false; onLoadProject() }) { Text("Открыть") }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Кнопки управления окном
            Box(modifier = Modifier.size(40.dp).clickable { windowState.isMinimized = true }, contentAlignment = Alignment.Center) {
                Text("—", color = MaterialTheme.colors.onSurface)
            }
            Box(modifier = Modifier.size(40.dp).clickable {
                windowState.placement = if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized
            }, contentAlignment = Alignment.Center) {
                Text("☐", color = MaterialTheme.colors.onSurface)
            }
            Box(modifier = Modifier.size(40.dp).clickable { onClose() }, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", modifier = Modifier.size(18.dp), tint = MaterialTheme.colors.onSurface)
            }
        }
    }
}