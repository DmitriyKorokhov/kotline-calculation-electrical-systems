package ui.screens.projecteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.PowerSourceNode
import data.ShieldNode

// Константы размеров для позиционирования текста
private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH

/**
 * Типы узлов для палетки.
 */
private enum class PaletteNodeType { SHIELD, POWER_SOURCE }

/**
 * Интегрированный ProjectView: сохранена оригинальная логика работы с полем,
 * добавлена сверху статичная палетка (непрозрачная, увеличенная).
 *
 * Важное: обе pointerInput (awaitPointerEventScope для scroll/press и detectDragGestures для drag)
 * находятся на одном и том же Box, который содержит Canvas — чтобы координаты были в одной системе.
 */
@Composable
fun ProjectView(
    state: ProjectCanvasState,
    onOpenShield: (shieldId: Int) -> Unit
) {
    // Переменные для drag-from-palette
    var paletteDragType by remember { mutableStateOf<PaletteNodeType?>(null) }
    var palettePreviewWorldPos by remember { mutableStateOf<Offset?>(null) }

    // Позиция верхнего левого угла канваса в глобальных координатах (root)
    var canvasTopLeft by remember { mutableStateOf(Offset.Zero) }

    // Состояния для перемещения существующих узлов (как в оригинале)
    var dragTarget by remember { mutableStateOf<Any?>(null) } // Int id или "Canvas"
    var nodeDragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ---- Палетка сверху: большая и непрозрачная ----
        Surface(elevation = 6.dp, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
            PaletteRow(
                cellHeight = 110.dp,
                cellWidthShield = 120.dp,
                cellWidthSource = 150.dp,
                onStartDrag = { type, globalPos ->
                    // Начало drag из палетки: сохраняем тип и устанавливаем preview
                    paletteDragType = type
                    // Переведём глобальную позицию в локальную канваса и в мир
                    val local = globalPos - canvasTopLeft
                    palettePreviewWorldPos = state.screenToWorld(local)
                },
                onDrag = { globalPos ->
                    val local = globalPos - canvasTopLeft
                    palettePreviewWorldPos = state.screenToWorld(local)
                },
                onEndDrag = { globalPos ->
                    val local = globalPos - canvasTopLeft
                    val worldPos = state.screenToWorld(local)
                    when (paletteDragType) {
                        PaletteNodeType.SHIELD -> state.addShieldNode(worldPos)
                        PaletteNodeType.POWER_SOURCE -> state.addPowerSourceNode(worldPos)
                        null -> {}
                    }
                    paletteDragType = null
                    palettePreviewWorldPos = null
                },
                onCancel = {
                    paletteDragType = null
                    palettePreviewWorldPos = null
                }
            )
        }

        // ---- Канвас и обработчики событий (в одном Box, чтобы координаты совпадали) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    // Позиция канваса в корневой системе координат, чтобы палетка могла отдавать глобальные координаты.
                    canvasTopLeft = coords.positionInRoot()
                }
                // Оригинальный обработчик для Scroll и Press (контекстные меню, соединения)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.first().position

                            if (event.type == PointerEventType.Scroll) {
                                val scrollDelta = event.changes.first().scrollDelta.y
                                state.onZoom(scrollDelta, position)
                                event.changes.first().consume()
                            }

                            if (event.type == PointerEventType.Press) {
                                val pressedNode = state.findNodeAtScreenPosition(position)
                                if (event.buttons.isSecondaryPressed) {
                                    state.contextMenuPosition = position
                                    state.selectedNode = pressedNode
                                    if (pressedNode != null) {
                                        state.showNodeContextMenu = true
                                    } else {
                                        state.showCanvasContextMenu = true
                                    }
                                } else if (event.buttons.isPrimaryPressed) {
                                    if (state.connectingFromNodeId != null) {
                                        state.tryFinishConnecting(pressedNode)
                                    }
                                }
                            }
                        }
                    }
                }
                // Оригинальный drag-обработчик: перемещение узлов и панорамирование
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            val node = state.findNodeAtScreenPosition(position)
                            if (node != null) {
                                dragTarget = node.id
                                nodeDragStartOffset = node.position - state.screenToWorld(position)
                            } else {
                                dragTarget = "Canvas"
                            }
                        },
                        onDragEnd = {
                            val nodeId = dragTarget as? Int
                            nodeId?.let { state.snapNodeToEndPosition(it) }
                            dragTarget = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            when (val target = dragTarget) {
                                is Int -> {
                                    val newWorldPos = state.screenToWorld(change.position) + nodeDragStartOffset
                                    state.updateNodePosition(target, newWorldPos)
                                }
                                is String -> {
                                    state.onPan(dragAmount)
                                }
                            }
                        }
                    )
                }
        ) {
            // Отрисовка содержимого канваса
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawProjectCanvas(state)
            }

            // Названия узлов (как в оригинале)
            state.nodes.forEach { node ->
                if (node.name.isNotBlank()) {
                    val screenPos = state.worldToScreen(node.position)
                    val scale = state.scale
                    val nodeHeight = if (node is PowerSourceNode) getNodeHeight(node) * scale else NODE_HEIGHT * scale
                    if (node is ShieldNode) {
                        NodeNameText(node.name, screenPos, NODE_WIDTH * scale, nodeHeight)
                    } else if (node is PowerSourceNode) {
                        PowerSourceNameText(node.name, screenPos, POWER_SOURCE_WIDTH * scale, nodeHeight, scale)
                    }
                }
            }

            // Превью блока при перетаскивании из палетки
            if (paletteDragType != null && palettePreviewWorldPos != null) {
                val previewScreen = state.worldToScreen(palettePreviewWorldPos!!)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = if (paletteDragType == PaletteNodeType.POWER_SOURCE) POWER_SOURCE_WIDTH * state.scale else NODE_WIDTH * state.scale
                    val height = if (paletteDragType == PaletteNodeType.POWER_SOURCE) getNodeHeight(PowerSourceNode(0, "", Offset.Zero)) * state.scale else NODE_HEIGHT * state.scale
                    val topLeft = Offset(previewScreen.x - width / 2, previewScreen.y - height / 2)

                    val fillColor = if (paletteDragType == PaletteNodeType.POWER_SOURCE) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                    val borderColor = if (paletteDragType == PaletteNodeType.POWER_SOURCE) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Gray

                    drawRect(color = fillColor, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(width, height))
                    drawRect(color = borderColor, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(width, height), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                }
            }

            // Контекстные меню и диалоги (оригинальные функции)
            NodeContextMenu(state, onOpenShield)
            CanvasContextMenu(state)
            RenameNodeDialog(state)

            val statusText = if (state.connectingFromNodeId != null) "Выберите объект для соединения..." else null
            if (statusText != null) {
                Text(
                    text = statusText,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

/** ---------------- Palette (palette row + items) ----------------
 * Палетка отдаёт глобальные координаты курсора (в root), мы конвертируем их в локальные
 * координаты канваса вычитая canvasTopLeft (см. использование в onStartDrag/onDrag/onEndDrag выше).
 */
@Composable
private fun PaletteRow(
    cellHeight: androidx.compose.ui.unit.Dp = 110.dp,
    cellWidthShield: androidx.compose.ui.unit.Dp = 120.dp,
    cellWidthSource: androidx.compose.ui.unit.Dp = 150.dp,
    onStartDrag: (PaletteNodeType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit,
    onCancel: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp), horizontalArrangement = Arrangement.Start) {
        PaletteItem(label = "Щит", widthDp = cellWidthShield, heightDp = cellHeight, drawType = PaletteNodeType.SHIELD,
            onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
        Spacer(modifier = Modifier.width(12.dp))
        PaletteItem(label = "Ист. питания", widthDp = cellWidthSource, heightDp = cellHeight, drawType = PaletteNodeType.POWER_SOURCE,
            onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
    }
}

@Composable
private fun PaletteItem(
    label: String,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp,
    drawType: PaletteNodeType,
    onStartDrag: (PaletteNodeType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit,
    onCancel: () -> Unit
) {
    // позиция верхнего левого угла ячейки в координатах корневого окна
    var itemTopLeft by remember { mutableStateOf(Offset.Zero) }
    var lastGlobalPointer by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .onGloballyPositioned { coords -> itemTopLeft = coords.positionInRoot() }
            .pointerInput(drawType) {
                detectDragGestures(
                    onDragStart = {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val globalCenter = itemTopLeft + center
                        lastGlobalPointer = globalCenter
                        onStartDrag(drawType, globalCenter)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val global = itemTopLeft + change.position
                        lastGlobalPointer = global
                        onDrag(global)
                    },
                    onDragEnd = {
                        val pos = lastGlobalPointer ?: (itemTopLeft + Offset(size.width / 2f, size.height / 2f))
                        onEndDrag(pos)
                        lastGlobalPointer = null
                    },
                    onDragCancel = {
                        onCancel()
                        lastGlobalPointer = null
                    }
                )
            }
            .padding(8.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width * 0.78f
            val h = size.height * 0.62f
            val topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2f, (size.height - h) / 2f)
            if (drawType == PaletteNodeType.POWER_SOURCE) {
                drawRect(color = androidx.compose.ui.graphics.Color.Black, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(w, h))
            } else {
                drawRect(color = androidx.compose.ui.graphics.Color.White, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(w, h))
                drawRect(color = androidx.compose.ui.graphics.Color.Gray, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(w, h), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
            }
        }
        Text(text = label, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** ---------------- Вспомогательные @Composable (как в оригинале) ---------------- */
@Composable
private fun NodeNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float) {
    Box(
        modifier = Modifier
            .offset(
                (screenPos.x - nodeWidth / 2).dp,
                (screenPos.y - nodeHeight / 2).dp
            )
            .size(width = nodeWidth.dp, height = nodeHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = MaterialTheme.colors.onSurface,
            fontSize = (14 * LocalDensity.current.fontScale).sp,
            textAlign = TextAlign.Center,
            softWrap = true
        )
    }
}

@Composable
private fun PowerSourceNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float, scale: Float) {
    Text(
        text = name,
        modifier = Modifier
            .offset(
                (screenPos.x - nodeWidth / 2).dp,
                (screenPos.y - nodeHeight / 2 - (25 * scale)).dp
            ),
        color = MaterialTheme.colors.onSurface,
        fontSize = (14 * LocalDensity.current.fontScale).sp
    )
}

@Composable
private fun NodeContextMenu(state: ProjectCanvasState, onOpenShield: (shieldId: Int) -> Unit) {
    DropdownMenu(
        expanded = state.showNodeContextMenu,
        onDismissRequest = { state.showNodeContextMenu = false },
        offset = DpOffset(state.contextMenuPosition.x.dp, state.contextMenuPosition.y.dp)
    ) {
        DropdownMenuItem(onClick = { state.showRenameDialog = true; state.showNodeContextMenu = false }) { Text("Изменить название") }
        if (state.selectedNode is ShieldNode) {
            DropdownMenuItem(onClick = { state.selectedNode?.let { onOpenShield(it.id) }; state.showNodeContextMenu = false }) { Text("Открыть") }
        }
        DropdownMenuItem(onClick = { state.startConnecting(); state.showNodeContextMenu = false }) { Text("Соединить") }
        DropdownMenuItem(onClick = { state.deleteSelectedNode(); state.showNodeContextMenu = false }) { Text("Удалить") }
    }
}

@Composable
private fun CanvasContextMenu(state: ProjectCanvasState) {
    DropdownMenu(
        expanded = state.showCanvasContextMenu,
        onDismissRequest = { state.showCanvasContextMenu = false },
        offset = DpOffset(state.contextMenuPosition.x.dp, state.contextMenuPosition.y.dp)
    ) {
        val worldPos = state.screenToWorld(state.contextMenuPosition)
        DropdownMenuItem(onClick = { state.addShieldNode(worldPos); state.showCanvasContextMenu = false }) { Text("Добавить щит") }
        DropdownMenuItem(onClick = { state.addPowerSourceNode(worldPos); state.showCanvasContextMenu = false }) { Text("Добавить ист. питания") }
        DropdownMenuItem(onClick = { state.addLevelLine(worldPos); state.showCanvasContextMenu = false }) { Text("Добавить уровень") }
    }
}

@Composable
private fun RenameNodeDialog(state: ProjectCanvasState) {
    if (state.showRenameDialog && state.selectedNode != null) {
        var newName by remember(state.selectedNode) { mutableStateOf(state.selectedNode!!.name) }
        AlertDialog(
            onDismissRequest = { state.showRenameDialog = false },
            title = { Text("Изменить название") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Новое название") }, singleLine = true) },
            confirmButton = { Button(onClick = { state.updateSelectedNodeName(newName); state.showRenameDialog = false }) { Text("Сохранить") } },
            dismissButton = { Button(onClick = { state.showRenameDialog = false }) { Text("Отмена") } }
        )
    }
}
