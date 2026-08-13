package feature.projecteditor.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectCanvasState
import feature.projecteditor.ui.utils.toOffset
import feature.projecteditor.ui.utils.toPoint
import feature.projecteditor.ui.labels.RightSideNameText
import feature.shieldeditor.state.ShieldStorage
import androidx.compose.ui.input.pointer.isTertiaryPressed

private const val NODE_WIDTH = 120f
private const val NODE_HEIGHT = 80f

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveCanvas(
    state: ProjectCanvasState,
    textMeasurer: TextMeasurer,
    onOpenShield: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragTarget by remember { mutableStateOf<Any?>(null) }
    var nodeDragStartOffset by remember { mutableStateOf(Point.Zero) }

    Box(
        modifier = modifier
            // 1. КЛИКИ (ЛКМ - выделение/соединение, Двойной клик - открытие щита)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        if (node is ShieldNode) onOpenShield(node.id)
                    },
                    onTap = { offset ->
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        if (state.connectingFromNodeId != null) {
                            // Если мы в режиме соединения линий (Routing)
                            state.tryFinishConnecting(node)
                        } else if (node != null) {
                            // Клик по модели
                            state.toggleOrAddSelection(node.id)
                        } else {
                            // Клик в пустоту - сброс
                            state.clearSelection()
                        }
                    }
                )
            }
            // 2. СКРОЛЛ И СКМ (Колесико) + ПКМ (Контекстное меню)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position

                        // ЗУМ
                        if (event.type == PointerEventType.Scroll) {
                            state.onZoom(event.changes.first().scrollDelta.y, position.toPoint())
                            event.changes.first().consume()
                        }

                        // ПЕРЕМЕЩЕНИЕ ХОЛСТА (Средняя кнопка мыши / Tertiary)
                        if (event.buttons.isTertiaryPressed && event.type == PointerEventType.Move) {
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val dragAmount = change.position - change.previousPosition
                                if (dragAmount.x != 0f || dragAmount.y != 0f) {
                                    state.onPan(dragAmount.toPoint())
                                    change.consume()
                                }
                            }
                        }

                        // МЕНЮ (ПКМ / Secondary)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val pressedNode = state.findNodeAtScreenPosition(position.toPoint())
                            state.contextMenuPosition = position.toPoint()

                            // 1. Умное выделение при клике ПКМ
                            if (pressedNode != null && !state.selectedNodeIds.contains(pressedNode.id)) {
                                // Если кликнули ПКМ по НЕвыделенной модели - сбрасываем старое и выделяем только её
                                state.clearSelection()
                                state.selectedNodeIds.add(pressedNode.id)
                            }
                            // (Если кликнули в пустоту, мы намеренно не сбрасываем выделение, чтобы пользователь мог нажать "Копировать" / "Удалить")

                            // 2. Решаем, какое меню открыть
                            if (state.selectedNodeIds.size > 1) {
                                // Если выделено несколько объектов (или мы кликнули ПКМ в пустоту при мультивыделении)
                                state.showMultiSelectMenu = true
                            } else if (state.selectedNodeIds.size == 1 && pressedNode != null) {
                                // Если выделена одна модель и клик пришелся прямо по ней
                                state.selectedNode = pressedNode
                                state.showNodeContextMenu = true
                            } else {
                                // Если кликнули в пустоту и ничего не выделено
                                // (Показываем меню, только если есть что вставлять из буфера)
                                if (state.clipboardNodes.isNotEmpty()) {
                                    state.showMultiSelectMenu = true
                                }
                            }
                        }
                    }
                }
            }
            // 3. ПЕРЕТАСКИВАНИЕ (ЛКМ) - Рамка выделения ИЛИ перемещение моделей
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val node = state.findNodeAtScreenPosition(position.toPoint())
                        if (node != null) {
                            state.saveHistory()
                            // Если потянули за невыделенный узел, выделяем только его
                            if (!state.selectedNodeIds.contains(node.id)) {
                                state.clearSelection()
                                state.selectedNodeIds.add(node.id)
                            }
                            dragTarget = "Nodes"
                        } else {
                            // Тянем за пустое место - рисуем рамку
                            dragTarget = "SelectionBox"
                            state.selectionStartScreen = position.toPoint()
                            state.selectionEndScreen = position.toPoint()
                        }
                    },
                    onDragEnd = {
                        if (dragTarget == "SelectionBox") {
                            state.applySelectionBox()
                        } else if (dragTarget == "Nodes") {
                            state.selectedNodeIds.forEach { state.snapNodeToEndPosition(it) }
                        }
                        dragTarget = null
                        state.selectionStartScreen = null
                        state.selectionEndScreen = null
                    },
                    onDragCancel = {
                        dragTarget = null
                        state.selectionStartScreen = null
                        state.selectionEndScreen = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (dragTarget == "Nodes") {
                            // Двигаем сразу все выделенные узлы
                            val deltaScreen = change.position - change.previousPosition
                            val scale = state.scale
                            val deltaWorld = Point(deltaScreen.x / scale, deltaScreen.y / scale)

                            state.selectedNodeIds.forEach { id ->
                                val n = state.nodes.find { it.id == id }
                                if (n != null) {
                                    state.updateNodePosition(id, Point(n.position.x + deltaWorld.x, n.position.y + deltaWorld.y))
                                }
                            }
                        } else if (dragTarget == "SelectionBox") {
                            state.selectionEndScreen = change.position.toPoint()
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawProjectCanvas(textMeasurer, state)
            drawGridHeaders(textMeasurer, state)
        }

        // УНИФИЦИРОВАННАЯ ОТРИСОВКА ПОДПИСЕЙ (Всегда справа)
        state.nodes.forEach { node ->
            if (node.name.isNotBlank() || node is ShieldNode) {
                val screenPos = state.worldToScreen(node.position).toOffset()
                val scale = state.scale
                val nodeHeight = NODE_HEIGHT * scale

                // Вычисляем ширину узла для правильного отступа текста
                val nodeWidthForLabel = when (node) {
                    is TransformerNode -> node.radiusOuter * 2f
                    is GeneratorNode -> node.radius * 2f
                    is BatteryNode -> NODE_WIDTH * 0.5f
                    is UpsNode -> NODE_HEIGHT
                    is SolarPanelNode -> NODE_WIDTH * 0.8f
                    else -> NODE_WIDTH
                }

                val displayName = if (node is ShieldNode) ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name } else node.name

                RightSideNameText(displayName, screenPos, nodeWidthForLabel * scale, nodeHeight, scale)
            }
        }
    }
}