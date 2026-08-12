package feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState

@Composable
fun WindowScope.HomeTitleBar(windowState: WindowState, onClose: () -> Unit) {
    WindowDraggableArea {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colors.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Название приложения
            Text(
                text = "Редактор электрических систем",
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface
            )

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

