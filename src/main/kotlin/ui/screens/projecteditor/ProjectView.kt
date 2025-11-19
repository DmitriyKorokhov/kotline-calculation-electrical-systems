package ui.screens.projecteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.GeneratorNode
import data.PowerSourceNode
import data.ShieldNode
import data.TransformerNode
import ui.screens.shieldeditor.ShieldStorage

private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f
private const val POWER_SOURCE_WIDTH = NODE_WIDTH
private const val TRANSFORMER_RADIUS = 40f
internal const val GENERATOR_RADIUS = 50f

private val PALETTE_HEIGHT_DP = 128.dp
private val PALETTE_CELL_HEIGHT_DP = 110.dp

private enum class PaletteNodeType { SHIELD, POWER_SOURCE, TRANSFORMER, GENERATOR }

@OptIn(ExperimentalTextApi::class)
@Composable
fun ProjectView(
    state: ProjectCanvasState,
    onOpenShield: (shieldId: Int) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var paletteDragType by remember { mutableStateOf<PaletteNodeType?>(null) }
    var palettePreviewWorldPos by remember { mutableStateOf<Offset?>(null) }
    var canvasTopLeft by remember { mutableStateOf(Offset.Zero) }
    var dragTarget by remember { mutableStateOf<Any?>(null) }
    var nodeDragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = PALETTE_HEIGHT_DP)
                .onGloballyPositioned { coords ->
                    canvasTopLeft = coords.positionInRoot()
                }
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
                        onDragCancel = { dragTarget = null },
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawProjectCanvas(textMeasurer, state)
            }

            state.nodes.forEach { node ->
                if (node.name.isNotBlank() || node is ShieldNode) {
                    val screenPos = state.worldToScreen(node.position)
                    val scale = state.scale
                    val nodeHeight = if (node is PowerSourceNode) getNodeHeight(node) * scale else NODE_HEIGHT * scale
                    when (node) {
                        is ShieldNode -> {
                            val displayName = ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name }
                            NodeNameText(displayName, screenPos, NODE_WIDTH * scale, nodeHeight)
                        }
                        is PowerSourceNode -> PowerSourceNameText(node.name, screenPos, POWER_SOURCE_WIDTH * scale, nodeHeight, scale)
                        is TransformerNode -> {
                            val screenCenter = state.worldToScreen(node.position)
                            val r = node.radiusOuter * state.scale
                            TransformerNameText(node.name, screenCenter, r)
                        }
                        is GeneratorNode -> {
                            val screenCenter = state.worldToScreen(node.position)
                            val r = node.radius * state.scale
                            GeneratorNameText(node.name, screenCenter, r)
                        }
                    }
                }
            }


            if (paletteDragType != null && palettePreviewWorldPos != null) {
                val previewScreen = state.worldToScreen(palettePreviewWorldPos!!)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (paletteDragType) {
                        PaletteNodeType.SHIELD -> {
                            val width = NODE_WIDTH * state.scale
                            val height = NODE_HEIGHT * state.scale
                            drawShieldShape(Offset(previewScreen.x - width / 2, previewScreen.y - height / 2), Size(width, height))
                        }
                        PaletteNodeType.POWER_SOURCE -> {
                            val width = POWER_SOURCE_WIDTH * state.scale
                            val height = getNodeHeight(PowerSourceNode(0, "", Offset.Zero)) * state.scale
                            drawPowerSourceShape(Offset(previewScreen.x - width / 2, previewScreen.y - height / 2), Size(width, height))
                        }
                        PaletteNodeType.TRANSFORMER -> {
                            val radius = TRANSFORMER_RADIUS * state.scale
                            drawTransformerShape(previewScreen, radius)
                        }
                        PaletteNodeType.GENERATOR -> {
                            val radius = GENERATOR_RADIUS * state.scale
                            drawGeneratorShape(textMeasurer, previewScreen, radius)
                        }
                        null -> {}
                    }
                }
            }

            NodeContextMenu(state, onOpenShield)
            CanvasContextMenu(state)
            RenameNodeDialog(state)
        }

        Surface(
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(PALETTE_HEIGHT_DP)
                .align(Alignment.TopStart),
            color = MaterialTheme.colors.surface
        ) {
            PaletteRow(
                textMeasurer = textMeasurer,
                cellHeight = PALETTE_CELL_HEIGHT_DP,
                onStartDrag = { type, globalPos ->
                    paletteDragType = type
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
                        PaletteNodeType.TRANSFORMER -> state.addTransformerNode(worldPos)
                        PaletteNodeType.GENERATOR -> state.addGeneratorNode(worldPos)
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
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun PaletteRow(
    textMeasurer: TextMeasurer,
    cellHeight: androidx.compose.ui.unit.Dp,
    onStartDrag: (PaletteNodeType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit,
    onCancel: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Start) {
        PaletteItem(textMeasurer = textMeasurer, label = "Щит", widthDp = 130.dp, heightDp = cellHeight, drawType = PaletteNodeType.SHIELD, onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
        Spacer(modifier = Modifier.width(16.dp))
        PaletteItem(textMeasurer = textMeasurer, label = "Шина", widthDp = 160.dp, heightDp = cellHeight, drawType = PaletteNodeType.POWER_SOURCE, onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
        Spacer(modifier = Modifier.width(16.dp))
        PaletteItem(textMeasurer = textMeasurer, label = "Трансформатор", widthDp = 160.dp, heightDp = 110.dp, drawType = PaletteNodeType.TRANSFORMER, onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
        Spacer(modifier = Modifier.width(16.dp))
        PaletteItem(textMeasurer = textMeasurer, label = "Генератор", widthDp = 130.dp, heightDp = cellHeight, drawType = PaletteNodeType.GENERATOR, onStartDrag = onStartDrag, onDrag = onDrag, onEndDrag = onEndDrag, onCancel = onCancel)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun PaletteItem(
    textMeasurer: TextMeasurer,
    label: String,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp,
    drawType: PaletteNodeType,
    onStartDrag: (PaletteNodeType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit,
    onCancel: () -> Unit
) {
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
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width * 0.8f
            val h = size.height * 0.7f
            when (drawType) {
                PaletteNodeType.SHIELD -> drawShieldShape(Offset((size.width - w) / 2f, (size.height - h) / 2f), Size(w, h))
                PaletteNodeType.POWER_SOURCE -> {
                    val correctHeight = h / 4f
                    drawPowerSourceShape(Offset((size.width - w) / 2f, (size.height - correctHeight) / 2f), Size(w, correctHeight))
                }
                PaletteNodeType.TRANSFORMER -> {
                    val radius = size.width * 0.25f
                    drawTransformerShape(Offset(size.width / 2f, size.height / 2f), radius)
                }
                PaletteNodeType.GENERATOR -> {
                    val radius = size.width * 0.35f
                    drawGeneratorShape(textMeasurer, Offset(size.width / 2f, size.height / 2f), radius)
                }
            }
        }
        Text(text = label, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun NodeNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float) {
    Box(
        modifier = Modifier
            .offset((screenPos.x - nodeWidth / 2f).dp, (screenPos.y - nodeHeight / 2f).dp)
            .size(width = nodeWidth.dp, height = nodeHeight.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, color = MaterialTheme.colors.onSurface, fontSize = (14 * LocalDensity.current.fontScale).sp, textAlign = TextAlign.Center, softWrap = true)
    }
}

@Composable
private fun PowerSourceNameText(name: String, screenPos: Offset, nodeWidth: Float, nodeHeight: Float, scale: Float) {
    Text(
        text = name,
        modifier = Modifier.offset((screenPos.x - nodeWidth / 2f).dp, (screenPos.y - nodeHeight / 2f - (25 * scale)).dp),
        color = MaterialTheme.colors.onSurface,
        fontSize = (14 * LocalDensity.current.fontScale).sp
    )
}

@Composable
private fun TransformerNameText(name: String, screenCenter: Offset, radiusScreen: Float) {
    val x = screenCenter.x + radiusScreen + 10f
    val y = screenCenter.y - 8f
    Text(
        text = name,
        modifier = Modifier.offset(x.dp, y.dp),
        color = MaterialTheme.colors.onSurface,
        fontSize = (14 * LocalDensity.current.fontScale).sp
    )
}

@Composable
private fun GeneratorNameText(name: String, screenCenter: Offset, radiusScreen: Float) {
    val x = screenCenter.x + radiusScreen + 10f
    val y = screenCenter.y - 8f
    Text(
        text = name,
        modifier = Modifier.offset(x.dp, y.dp),
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
            confirmButton = { Button(onClick = { val sel = state.selectedNode
                if (sel is ShieldNode) {
                    // синхронизируем имя с данными щита
                    val data = ShieldStorage.loadOrCreate(sel.id)
                    data.shieldName = newName
                    ShieldStorage.save(sel.id, data)
                }
                state.updateSelectedNodeName(newName);
                state.showRenameDialog = false })
            { Text("Сохранить") } },
            dismissButton = { Button(onClick = { state.showRenameDialog = false }) { Text("Отмена") } }
        )
    }
}
