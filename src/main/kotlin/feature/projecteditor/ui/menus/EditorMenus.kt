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
import feature.projecteditor.domain.Point
import feature.projecteditor.state.ConnectionHit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.domain.ItRackRowNode
import feature.projecteditor.domain.Rack
import feature.projecteditor.domain.RackFeed

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
            if (state.selectedNode is ItRackRowNode) {
                DropdownMenuItem(onClick = {
                    state.showRackSettingsDialog = true
                    state.showNodeContextMenu = false
                }) {
                    Text("Настроить ряд")
                }
            }

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

@Composable
fun ConnectionContextMenu(state: ProjectCanvasState) {
    val density = LocalDensity.current
    val xDp = with(density) { state.contextMenuPosition.x.toDp() }
    val yDp = with(density) { state.contextMenuPosition.y.toDp() }

    Box(modifier = Modifier.offset(x = xDp, y = yDp)) {
        DropdownMenu(
            expanded = state.showConnectionContextMenu,
            onDismissRequest = { state.showConnectionContextMenu = false }
        ) {
            DropdownMenuItem(onClick = {
                state.saveHistory()
                state.connections.removeAll(state.selectedConnections)
                state.selectedConnections.clear()
                state.showConnectionContextMenu = false
            }) { Text("Удалить") }

            DropdownMenuItem(onClick = {
                val hit = state.clickedConnectionHit
                if (hit is ConnectionHit.Segment || hit is ConnectionHit.Midpoint) {
                    val index = if (hit is ConnectionHit.Segment) hit.index else (hit as ConnectionHit.Midpoint).index
                    var conn = hit.connection

                    if (conn.waypoints.isEmpty()) {
                        val initialPts = state.calculateConnectionPoints(conn)
                        conn = conn.copy(waypoints = initialPts.subList(1, initialPts.size - 1))
                        state.updateConnection(hit.connection, conn)
                    }

                    val pts = state.calculateConnectionPoints(conn)
                    val p1 = pts[index]
                    val p2 = pts[index+1]
                    val isHorizontal = kotlin.math.abs(p1.y - p2.y) < kotlin.math.abs(p1.x - p2.x)

                    val newWaypoints = conn.waypoints.toMutableList()
                    val clickPos = state.screenToWorld(state.contextMenuPosition)

                    val stepSize = 40f
                    val insertIndex = index

                    if (isHorizontal) {
                        val midX = clickPos.x
                        newWaypoints.add(insertIndex, Point(midX, p1.y))
                        newWaypoints.add(insertIndex + 1, Point(midX, p1.y + stepSize))
                        newWaypoints.add(insertIndex + 2, Point(p2.x, p1.y + stepSize))
                    } else {
                        val midY = clickPos.y
                        newWaypoints.add(insertIndex, Point(p1.x, midY))
                        newWaypoints.add(insertIndex + 1, Point(p1.x + stepSize, midY))
                        newWaypoints.add(insertIndex + 2, Point(p1.x + stepSize, p2.y))
                    }

                    val updated = conn.copy(waypoints = newWaypoints)
                    state.saveHistory()
                    state.updateConnection(conn, updated)
                }
                state.showConnectionContextMenu = false
            }) { Text("Добавить угол") }
        }
    }
}