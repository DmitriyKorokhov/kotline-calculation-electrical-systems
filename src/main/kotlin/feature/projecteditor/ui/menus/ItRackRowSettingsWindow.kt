package feature.projecteditor.ui.menus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import core.view.CompactOutlinedTextField
import feature.projecteditor.domain.ItRackRowNode
import feature.projecteditor.domain.Rack
import feature.projecteditor.domain.RackFeed
import feature.projecteditor.state.ProjectCanvasState
import java.awt.Cursor

private val BLOCKBORDER = Color(0xFFB0BEC5)

// Палитра доступных цветов для лучей
private val FeedColorPalette = listOf(
    0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFFF57C00,
    0xFF7B1FA2, 0xFF0097A7, 0xFFFBC02D, 0xFF455A64
)

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .border(0.5.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f))
            .background(Color.Gray.copy(alpha = 0.05f))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun HeaderFeedCell(
    feed: RackFeed,
    onValueChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    width: Dp
) {
    var colorMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .border(0.5.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f))
            .background(Color.Gray.copy(alpha = 0.05f))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CompactOutlinedTextField(
                label = "",
                value = feed.name,
                onValueChange = onValueChange,
                textColor = MaterialTheme.colors.onSurface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.LightGray,
                fontSizeSp = 13,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Кружок выбора цвета
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(feed.colorArgb), CircleShape)
                    .border(1.dp, Color.Gray, CircleShape)
                    .clickable { colorMenuExpanded = true }
            )

            // Меню выбора цвета
            DropdownMenu(
                expanded = colorMenuExpanded,
                onDismissRequest = { colorMenuExpanded = false }
            ) {
                FeedColorPalette.forEach { colorVal ->
                    DropdownMenuItem(onClick = {
                        onColorChange(colorVal)
                        colorMenuExpanded = false
                    }) {
                        Box(modifier = Modifier.size(24.dp).background(Color(colorVal), CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyCell(width: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            .padding(4.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ItRackRowSettingsWindow(
    state: ProjectCanvasState,
    onDismiss: () -> Unit
) {
    val node = state.selectedNode as? ItRackRowNode ?: return

    val density = LocalDensity.current
    val windowState = rememberWindowState(width = 520.dp, height = 650.dp)
    val minWidth = 400.dp
    val minHeight = 450.dp

    var draftRacks by remember { mutableStateOf(node.racks.toList()) }
    var draftFeeds by remember { mutableStateOf(node.feeds.toList()) }

    fun applyDraft() {
        state.saveHistory()
        val index = state.nodes.indexOfFirst { it.id == node.id }
        if (index != -1) {
            state.nodes[index] = node.copy(racks = draftRacks, feeds = draftFeeds)
        }
    }

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        undecorated = true,
        transparent = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ВЕРХНЯЯ ПАНЕЛЬ С КРЕСТИКОМ
                WindowDraggableArea {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.08f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Настройка ряда ИТ-стоек",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                // ОСНОВНАЯ ОБЛАСТЬ
                Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {

                    // БЛОК КНОПОК УПРАВЛЕНИЯ
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Text("Стойка", modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Button(
                                onClick = { draftRacks = draftRacks + Rack(index = (draftRacks.maxOfOrNull { it.index } ?: 0) + 1, powerW = 5f) },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) { Text("+", fontSize = 18.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { if (draftRacks.isNotEmpty()) draftRacks = draftRacks.dropLast(1) },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF5F5F5), contentColor = Color.Black)
                            ) { Text("-", fontSize = 18.sp) }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Луч", modifier = Modifier.width(80.dp), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Button(
                                onClick = {
                                    val newName = "Луч ${('A' + draftFeeds.size)}"
                                    val isTopFeed = draftFeeds.size % 2 == 0
                                    val defaultColor = if (isTopFeed) FeedColorPalette[0] else FeedColorPalette[1]
                                    draftFeeds = draftFeeds + RackFeed(newName, emptySet(), isTop = isTopFeed, colorArgb = defaultColor)
                                },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) { Text("+", fontSize = 18.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { if (draftFeeds.isNotEmpty()) draftFeeds = draftFeeds.dropLast(1) },
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF5F5F5), contentColor = Color.Black)
                            ) { Text("-", fontSize = 18.sp) }
                        }
                    }

                    // ТАБЛИЦА С ДАННЫМИ И СКРОЛЛБАРАМИ
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, BLOCKBORDER)) {
                        val hScroll = rememberScrollState()
                        val vScroll = rememberScrollState()

                        Column(modifier = Modifier.fillMaxSize().horizontalScroll(hScroll)) {

                            // ШАПКА ТАБЛИЦЫ
                            Row(modifier = Modifier.height(48.dp)) {
                                HeaderCell("Нумерация", 100.dp)
                                HeaderCell("Мощность, кВт", 120.dp)
                                draftFeeds.forEachIndexed { feedIndex, feed ->
                                    HeaderFeedCell(
                                        feed = feed,
                                        onValueChange = { newName ->
                                            val newFeeds = draftFeeds.toMutableList()
                                            newFeeds[feedIndex] = feed.copy(name = newName)
                                            draftFeeds = newFeeds
                                        },
                                        onColorChange = { newColor ->
                                            val newFeeds = draftFeeds.toMutableList()
                                            newFeeds[feedIndex] = feed.copy(colorArgb = newColor)
                                            draftFeeds = newFeeds
                                        },
                                        width = 130.dp
                                    )
                                }
                            }

                            // СТРОКИ ТАБЛИЦЫ (Скроллятся по вертикали)
                            Column(modifier = Modifier.weight(1f).verticalScroll(vScroll)) {
                                draftRacks.forEachIndexed { rackIndex, rack ->
                                    Row(modifier = Modifier.height(48.dp)) {
                                        BodyCell(100.dp) {
                                            Text("Стойка ${rack.index}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        BodyCell(120.dp) {
                                            CompactOutlinedTextField(
                                                label = "",
                                                value = if (rack.powerW % 1f == 0f) rack.powerW.toInt().toString() else rack.powerW.toString(),
                                                onValueChange = { newVal ->
                                                    val newRacks = draftRacks.toMutableList()
                                                    newRacks[rackIndex] = rack.copy(powerW = newVal.toFloatOrNull() ?: 0f)
                                                    draftRacks = newRacks
                                                },
                                                textColor = MaterialTheme.colors.onSurface,
                                                focusedBorderColor = MaterialTheme.colors.primary,
                                                unfocusedBorderColor = Color.LightGray,
                                                fontSizeSp = 13,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                            )
                                        }
                                        draftFeeds.forEachIndexed { feedIndex, feed ->
                                            BodyCell(130.dp) {
                                                Checkbox(
                                                    checked = feed.connectedRacks.contains(rack.index),
                                                    onCheckedChange = { isChecked ->
                                                        val newFeeds = draftFeeds.toMutableList()
                                                        val newSet = if (isChecked) {
                                                            feed.connectedRacks + rack.index
                                                        } else {
                                                            feed.connectedRacks - rack.index
                                                        }
                                                        newFeeds[feedIndex] = feed.copy(connectedRacks = newSet)
                                                        draftFeeds = newFeeds
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(feed.colorArgb))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // СКРОЛЛБАРЫ
                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = 48.dp),
                            adapter = rememberScrollbarAdapter(vScroll)
                        )
                        HorizontalScrollbar(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            adapter = rememberScrollbarAdapter(hScroll)
                        )
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                // НИЖНЯЯ ПАНЕЛЬ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = { applyDraft() }) { Text("Принять") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { applyDraft(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White)
                    ) { Text("ОК") }
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }

            // РЕСАЙЗ ОКНА
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newWidth = max(windowState.size.width + with(density) { dragAmount.x.toDp() }, minWidth)
                            val newHeight = max(windowState.size.height + with(density) { dragAmount.y.toDp() }, minHeight)
                            windowState.size = DpSize(newWidth, newHeight)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val color = Color.Gray.copy(alpha = 0.5f)
                    val w = size.width
                    val h = size.height
                    drawLine(color, Offset(w, h - 4), Offset(w - 4, h), strokeWidth = 2f)
                    drawLine(color, Offset(w, h - 8), Offset(w - 8, h), strokeWidth = 2f)
                    drawLine(color, Offset(w, h - 12), Offset(w - 12, h), strokeWidth = 2f)
                }
            }
        }
    }
}