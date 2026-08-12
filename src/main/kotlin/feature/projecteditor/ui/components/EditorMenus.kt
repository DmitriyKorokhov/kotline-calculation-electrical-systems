package feature.projecteditor.ui.menus

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import feature.projecteditor.domain.ShieldNode
import feature.projecteditor.state.ProjectCanvasState
import feature.shieldeditor.state.ShieldStorage

@Composable
fun NodeContextMenu(state: ProjectCanvasState, onOpenShield: (Int) -> Unit) {
    val density = LocalDensity.current
    val menuOffset = with(density) { DpOffset(state.contextMenuPosition.x.toDp(), state.contextMenuPosition.y.toDp()) }

    DropdownMenu(expanded = state.showNodeContextMenu, onDismissRequest = { state.showNodeContextMenu = false }, offset = menuOffset) {
        DropdownMenuItem(onClick = { state.showRenameDialog = true; state.showNodeContextMenu = false }) { Text("Изменить название") }
        if (state.selectedNode is ShieldNode) {
            DropdownMenuItem(onClick = { state.selectedNode?.let { onOpenShield(it.id) }; state.showNodeContextMenu = false }) { Text("Открыть") }
        }
        DropdownMenuItem(onClick = { state.startConnecting(); state.showNodeContextMenu = false }) { Text("Соединить") }
        DropdownMenuItem(onClick = { state.deleteSelectedNode(); state.showNodeContextMenu = false }) { Text("Удалить") }
    }
}

@Composable
fun CanvasContextMenu(state: ProjectCanvasState) {
    val density = LocalDensity.current
    val menuOffset = with(density) { DpOffset(state.contextMenuPosition.x.toDp(), state.contextMenuPosition.y.toDp()) }

    DropdownMenu(expanded = state.showCanvasContextMenu, onDismissRequest = { state.showCanvasContextMenu = false }, offset = menuOffset) {
        val worldPos = state.screenToWorld(state.contextMenuPosition)
        DropdownMenuItem(onClick = { state.addLevelLine(worldPos); state.showCanvasContextMenu = false }) { Text("Добавить уровень") }
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
