package feature.projecteditor.ui.menus

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import feature.projecteditor.domain.ShieldNode
import feature.projecteditor.state.ProjectCanvasState
import feature.shieldeditor.state.ShieldStorage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier

@Composable
fun NodeContextMenu(state: ProjectCanvasState, onOpenShield: (Int) -> Unit) {
    val density = LocalDensity.current
    val xDp = with(density) { state.contextMenuPosition.x.toDp() }
    val yDp = with(density) { state.contextMenuPosition.y.toDp() }

    Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
        DropdownMenu(
            expanded = state.showNodeContextMenu,
            onDismissRequest = { state.showNodeContextMenu = false }
        ) {
            DropdownMenuItem(onClick = { state.showRenameDialog = true; state.showNodeContextMenu = false }) { Text("Изменить название") }

            if (state.selectedNode is ShieldNode) {
                DropdownMenuItem(onClick = { state.selectedNode?.let { onOpenShield(it.id) }; state.showNodeContextMenu = false }) { Text("Открыть") }
            }

            DropdownMenuItem(onClick = { state.startConnecting(); state.showNodeContextMenu = false }) { Text("Соединить") }
            DropdownMenuItem(onClick = {
                state.copySelectedNodes()
                state.showNodeContextMenu = false
            }) { Text("Копировать") }

            DropdownMenuItem(onClick = { state.deleteSelectedNode(); state.showNodeContextMenu = false }) { Text("Удалить") }
        }
    }
}

@Composable
fun RenameNodeDialog(state: ProjectCanvasState) {
    if (state.showRenameDialog && state.selectedNode != null) {
        var newName by remember(state.selectedNode) { mutableStateOf(state.selectedNode!!.name) }
        AlertDialog(
            onDismissRequest = { state.showRenameDialog = false },
            title = { Text("Изменить название") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Новое название") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    val sel = state.selectedNode
                    if (sel is ShieldNode) {
                        val data = ShieldStorage.loadOrCreate(sel.id)
                        data.shieldName = newName
                        ShieldStorage.save(sel.id, data)
                    }
                    state.updateSelectedNodeName(newName)
                    state.showRenameDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { Button(onClick = { state.showRenameDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun MultiSelectContextMenu(state: ProjectCanvasState) {
    val density = LocalDensity.current
    val xDp = with(density) { state.contextMenuPosition.x.toDp() }
    val yDp = with(density) { state.contextMenuPosition.y.toDp() }

    Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
        DropdownMenu(
            expanded = state.showMultiSelectMenu,
            onDismissRequest = { state.showMultiSelectMenu = false }
        ) {
            // Кнопки "Удалить" и "Копировать" показываются, только если есть выделенные элементы
            if (state.selectedNodeIds.isNotEmpty()) {
                DropdownMenuItem(onClick = {
                    state.deleteSelectedNodes()
                    state.showMultiSelectMenu = false
                }) { Text("Удалить") }

                DropdownMenuItem(onClick = {
                    state.copySelectedNodes()
                    state.showMultiSelectMenu = false
                }) { Text("Копировать") }
            }

            DropdownMenuItem(onClick = {
                state.pasteNodes(state.contextMenuPosition)
                state.showMultiSelectMenu = false
            }) { Text("Вставить") }
        }
    }
}
