package feature.projecteditor.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
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
import feature.projecteditor.state.ConnectionHit
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import feature.projecteditor.state.CanvasToolMode
import java.awt.Cursor

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
    var isPanning by remember { mutableStateOf(false) }
    var isZooming by remember { mutableStateOf(false) }
    // Вычисляем текущий курсор в зависимости от модификаторов и действий
    val currentCursor = remember(isPanning, isZooming) {
        when {
            isPanning -> PointerIcon(Cursor(Cursor.MOVE_CURSOR))
            isZooming -> PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))
            else -> PointerIcon(Cursor(Cursor.CROSSHAIR_CURSOR)) // Всегда перекрестие на холсте
        }
    }

    Box(
        modifier = modifier
            .pointerHoverIcon(currentCursor)
            // 1. КЛИКИ (ЛКМ - выделение/соединение, Двойной клик - открытие щита)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        if (node is ShieldNode) {
                            onOpenShield(node.id)
                        } else if (node is ItRackRowNode) {
                            // Выделяем узел, чтобы диалог знал, чьи данные редактировать
                            state.clearSelection()
                            state.selectedNodeIds.add(node.id)
                            state.selectedNode = node
                            // Открываем диалог
                            state.showRackSettingsDialog = true
                        }
                    },
                    onTap = { offset ->
                        val worldPos = state.screenToWorld(offset.toPoint())

                        // 1. ПЕРЕХВАТ КЛИКА ДЛЯ ВСТАВКИ ИНСТРУМЕНТОВ АННОТАЦИЙ
                        when (state.currentToolMode) {
                            CanvasToolMode.ADD_TEXT -> {
                                state.saveHistory()
                                val newNode = TextNode(id = state.nextId++, name = "Текст", position = worldPos)
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                // Сразу переходим в режим редактирования
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = newNode.name
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_CALLOUT -> {
                                state.saveHistory()
                                // Текст выноски ставим чуть правее и выше, а саму стрелку (targetPoint) - куда кликнули
                                val textPos = Point(worldPos.x + 80f, worldPos.y - 80f)
                                val newNode = CalloutNode(id = state.nextId++, name = "Выноска", position = textPos, targetPoint = worldPos)
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                // Сразу переходим в режим редактирования
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = newNode.name
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.SELECT -> { /* продолжаем обычную обработку */ }
                        }
                        // Если мы в режиме редактирования текста - завершаем его при клике куда угодно
                        if (state.inlineEditingNodeId != null) {
                            state.finishInlineEditing()
                        }
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        val connHit = state.hitTestConnections(offset.toPoint())

                        if (state.connectingFromNodeId != null) {
                            val hovered = state.hoveredPin
                            val fromNode = state.nodes.find { it.id == state.connectingFromNodeId }

                            // ИСПРАВЛЕНИЕ: используем hovered.node вместо hovered.first
                            if (hovered != null && hovered.node.id != state.connectingFromNodeId && fromNode != null) {
                                state.saveHistory()
                                // Умный расчет ближайшей стороны выхода для первой модели
                                val (autoFromSide, _) = state.getClosestSides(fromNode, hovered.node)

                                state.connections.add(
                                    Connection(
                                        fromId = state.connectingFromNodeId!!,
                                        toId = hovered.node.id,
                                        fromSide = autoFromSide,
                                        toSide = hovered.side,
                                        toSubId = hovered.subId // Сохраняем пин при первом клике!
                                    )
                                )
                                state.connectingFromNodeId = null
                            } else {
                                state.tryFinishConnecting(node) // fallback (полностью автоматический расчет)
                            }
                        } else if (node != null) {
                            if (state.isCtrlPressed) {
                                state.selectedNodeIds.remove(node.id)
                            } else if (state.isShiftPressed) {
                                if (!state.selectedNodeIds.contains(node.id)) state.selectedNodeIds.add(node.id)
                            } else {
                                state.clearSelection()
                                state.selectedNodeIds.add(node.id)
                            }
                        } else if (connHit != null) {
                            if (state.isCtrlPressed) {
                                state.selectedConnections.remove(connHit.connection)
                            } else if (state.isShiftPressed) {
                                if (!state.selectedConnections.contains(connHit.connection)) state.selectedConnections.add(
                                    connHit.connection
                                )
                            } else {
                                state.clearSelection()
                                state.selectedConnections.add(connHit.connection)
                            }
                        } else {
                            // Клик в пустоту снимает выделение только если не зажаты модификаторы
                            if (!state.isCtrlPressed && !state.isShiftPressed) {
                                state.clearSelection()
                            }
                        }
                    }
                )
            }
            // 2. СКРОЛЛ И СКМ (Колесико) + ПКМ (Контекстное меню)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // Считываем клавиатуру
                        state.isCtrlPressed = event.keyboardModifiers.isCtrlPressed
                        state.isShiftPressed = event.keyboardModifiers.isShiftPressed
                        // Проверяем, зажато ли колесико
                        isPanning = event.buttons.isTertiaryPressed
                        val position = event.changes.first().position
                        // ЗУМ
                        if (event.type == PointerEventType.Scroll) {
                            isZooming = true // Включаем курсор масштаба на момент скролла
                            state.onZoom(event.changes.first().scrollDelta.y, position.toPoint())
                            event.changes.first().consume()
                        } else if (event.type == PointerEventType.Move && !isPanning) {
                            isZooming = false // Сбрасываем курсор масштаба, если просто двигаем мышью
                            state.updateHoveredPin(position.toPoint())
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

                            if (pressedNode != null && !state.selectedNodeIds.contains(pressedNode.id)) {
                                state.clearSelection()
                                state.selectedNodeIds.add(pressedNode.id)
                            }

                            if (state.selectedNodeIds.size > 1) {
                                state.showMultiSelectMenu = true
                            } else if (state.selectedNodeIds.size == 1 && pressedNode != null) {
                                state.selectedNode = pressedNode
                                state.showNodeContextMenu = true
                            } else {
                                // ПКМ по линии
                                val connHit = state.hitTestConnections(position.toPoint())
                                if (connHit != null) {
                                    state.clearSelection()
                                    state.selectedConnections.add(connHit.connection) // <-- ИСПРАВЛЕНО
                                    state.clickedConnectionHit = connHit
                                    state.showConnectionContextMenu = true
                                } else if (state.clipboardNodes.isNotEmpty()) {
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
                        val connHit = state.hitTestConnections(position.toPoint())

                        if (node != null) {
                            state.saveHistory()
                            if (!state.selectedNodeIds.contains(node.id)) {
                                if (!state.isCtrlPressed && !state.isShiftPressed) state.clearSelection()
                                state.selectedNodeIds.add(node.id)
                            }
                            dragTarget = "Nodes"
                        } else if (connHit != null) {
                            state.saveHistory()
                            if (!state.selectedConnections.contains(connHit.connection)) {
                                if (!state.isCtrlPressed && !state.isShiftPressed) state.clearSelection()
                                state.selectedConnections.add(connHit.connection)
                            }
                            when (connHit) {
                                is ConnectionHit.Endpoint -> {
                                    state.isDraggingLineEnd = true
                                    // Запоминаем ID модели, к которой изначально привязан этот конец линии
                                    state.draggingEndpointNodeId =
                                        if (connHit.isSource) connHit.connection.fromId else connHit.connection.toId
                                    dragTarget = connHit
                                }

                                is ConnectionHit.Waypoint -> {
                                    var conn = connHit.connection
                                    if (conn.waypoints.isEmpty()) {
                                        val pts = state.calculateConnectionPoints(conn)
                                        conn = conn.copy(waypoints = pts.subList(1, pts.size - 1))
                                        state.updateConnection(connHit.connection, conn)
                                    }
                                    dragTarget = connHit.copy(connection = conn)
                                }

                                is ConnectionHit.Midpoint, is ConnectionHit.Segment -> {
                                    val index =
                                        if (connHit is ConnectionHit.Midpoint) connHit.index else (connHit as ConnectionHit.Segment).index
                                    var conn = connHit.connection

                                    if (conn.waypoints.isEmpty()) {
                                        val pts = state.calculateConnectionPoints(conn)
                                        conn = conn.copy(waypoints = pts.subList(1, pts.size - 1))
                                        state.updateConnection(connHit.connection, conn)
                                    }
                                    val pts = state.calculateConnectionPoints(conn)
                                    val newWaypoints = pts.subList(1, pts.size - 1).toMutableList()
                                    var w1Index = index - 1
                                    var w2Index = index

                                    if (index == 0) {
                                        if (pts.size >= 2) {
                                            newWaypoints.add(0, pts[1].copy())
                                            newWaypoints.add(0, pts[0].copy())
                                        }
                                        w1Index = 0
                                        w2Index = 1
                                    } else if (index == pts.size - 2) {
                                        if (pts.size >= 2) {
                                            newWaypoints.add(pts[pts.lastIndex - 1].copy())
                                            newWaypoints.add(pts.last().copy())
                                        }
                                        w1Index = newWaypoints.lastIndex - 1
                                        w2Index = newWaypoints.lastIndex
                                    }

                                    val updatedConn = conn.copy(waypoints = newWaypoints)
                                    state.updateConnection(conn, updatedConn)
                                    dragTarget = ConnectionHit.SegmentDrag(updatedConn, w1Index, w2Index)
                                }

                                is ConnectionHit.SegmentDrag -> {}
                            }
                        } else {
                            dragTarget = "SelectionBox"
                            state.selectionStartScreen = position.toPoint()
                            state.selectionEndScreen = position.toPoint()
                        }
                    },
                    onDragEnd = {
                        if (dragTarget is ConnectionHit.Endpoint) {
                            val target = dragTarget as ConnectionHit.Endpoint
                            val pin = state.hoveredPin
                            if (pin != null) {
                                state.saveHistory()
                                val newConn = if (target.isSource) {
                                    target.connection.copy(
                                        fromId = pin.node.id,
                                        fromSide = pin.side,
                                        fromSubId = pin.subId,
                                    )
                                } else {
                                    target.connection.copy(
                                        toId = pin.node.id,
                                        toSide = pin.side,
                                        toSubId = pin.subId,
                                    )
                                }
                                state.updateConnection(target.connection, newConn)
                            }
                        }

                        state.isDraggingLineEnd = false

                        if (dragTarget == "SelectionBox") {
                            state.applySelectionBox()
                        } else if (dragTarget == "Nodes") {
                            state.selectedNodeIds.forEach { state.snapNodeToEndPosition(it) }
                        }
                        // --- Очистка и объединение точек при отпускании мыши ---
                        if (dragTarget is ConnectionHit.Waypoint || dragTarget is ConnectionHit.SegmentDrag) {
                            val hit = dragTarget as ConnectionHit
                            val cleaned = state.cleanupConnection(hit.connection)
                            state.updateConnection(hit.connection, cleaned)
                        }

                        dragTarget = null
                        state.selectionStartScreen = null
                        state.selectionEndScreen = null
                    },
                    onDragCancel = {
                        state.isDraggingLineEnd = false
                        state.draggingEndpointNodeId = null
                        dragTarget = null
                        state.selectionStartScreen = null
                        state.selectionEndScreen = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val deltaScreen = change.position - change.previousPosition
                        val scale = state.scale
                        val deltaWorld = Point(deltaScreen.x / scale, deltaScreen.y / scale)

                        if (dragTarget == "Nodes") {
                            state.selectedNodeIds.forEach { id ->
                                val n = state.nodes.find { it.id == id }
                                if (n != null) state.updateNodePosition(
                                    id,
                                    Point(n.position.x + deltaWorld.x, n.position.y + deltaWorld.y)
                                )
                            }
                        } else if (dragTarget is ConnectionHit.SegmentDrag) {
                            // === Перетаскивание целого сегмента (строго ортогонально) ===
                            val target = dragTarget as ConnectionHit.SegmentDrag
                            val conn = target.connection
                            val newWaypoints = conn.waypoints.toMutableList()

                            val p1 = newWaypoints[target.w1Index]
                            val p2 = newWaypoints[target.w2Index]
                            val isHorizontal = kotlin.math.abs(p1.y - p2.y) < kotlin.math.abs(p1.x - p2.x)

                            if (isHorizontal) {
                                newWaypoints[target.w1Index] = Point(p1.x, p1.y + deltaWorld.y)
                                newWaypoints[target.w2Index] = Point(p2.x, p2.y + deltaWorld.y)
                            } else {
                                newWaypoints[target.w1Index] = Point(p1.x + deltaWorld.x, p1.y)
                                newWaypoints[target.w2Index] = Point(p2.x + deltaWorld.x, p2.y)
                            }

                            val updated = conn.copy(waypoints = newWaypoints)
                            state.updateConnection(conn, updated)
                            dragTarget = target.copy(connection = updated)

                        } else if (dragTarget is ConnectionHit.Waypoint) {
                            val target = dragTarget as ConnectionHit.Waypoint
                            val conn = target.connection
                            val wpIndex = target.index
                            val pts = state.calculateConnectionPoints(conn)
                            val newWaypoints = pts.subList(1, pts.size - 1).toMutableList()
                            val currentPt = newWaypoints[wpIndex]

                            var newX = currentPt.x + deltaWorld.x
                            var newY = currentPt.y + deltaWorld.y

                            // Правильно блокируем ось в зависимости от грани выхода ===
                            val isFirstHorizontal =
                                conn.fromSide == AnchorSide.LEFT || conn.fromSide == AnchorSide.RIGHT
                            val isLastHorizontal = conn.toSide == AnchorSide.LEFT || conn.toSide == AnchorSide.RIGHT

                            if (wpIndex == 0) {
                                if (isFirstHorizontal) newY = currentPt.y else newX = currentPt.x
                            }
                            if (wpIndex == newWaypoints.lastIndex) {
                                if (isLastHorizontal) newY = currentPt.y else newX = currentPt.x
                            }
                            // Сдвигаем соседние точки, чтобы сохранить прямые углы
                            if (wpIndex > 0) {
                                val prev = newWaypoints[wpIndex - 1]
                                val dx = kotlin.math.abs(prev.x - currentPt.x)
                                val dy = kotlin.math.abs(prev.y - currentPt.y)
                                // Определяем ориентацию прилегающего сегмента
                                val isPrevHorizontal = if (dx == 0f && dy == 0f) wpIndex % 2 != 0 else dy < dx

                                if (isPrevHorizontal) newWaypoints[wpIndex - 1] = Point(prev.x, newY)
                                else newWaypoints[wpIndex - 1] = Point(newX, prev.y)
                            }

                            if (wpIndex < newWaypoints.lastIndex) {
                                val next = newWaypoints[wpIndex + 1]
                                val dx = kotlin.math.abs(next.x - currentPt.x)
                                val dy = kotlin.math.abs(next.y - currentPt.y)
                                val isNextHorizontal = if (dx == 0f && dy == 0f) wpIndex % 2 == 0 else dy < dx

                                if (isNextHorizontal) newWaypoints[wpIndex + 1] = Point(next.x, newY)
                                else newWaypoints[wpIndex + 1] = Point(newX, next.y)
                            }

                            newWaypoints[wpIndex] = Point(newX, newY)
                            val updated = conn.copy(waypoints = newWaypoints)
                            state.updateConnection(conn, updated)
                            dragTarget = target.copy(connection = updated)

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

                val nodeWidthForLabel = when (node) {
                    is TransformerNode -> node.radiusOuter * 2f
                    is GeneratorNode -> node.radius * 2f
                    is BatteryNode -> NODE_WIDTH * 0.5f
                    is UpsNode -> NODE_HEIGHT
                    is SolarPanelNode -> NODE_WIDTH * 0.8f
                    is SystemNode -> node.radius * 2f
                    is ItRackRowNode -> feature.projecteditor.ui.selection.getItRackRowSize(node).first
                    else -> NODE_WIDTH
                }

                val displayName = if (node is ShieldNode) {
                    ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name }
                } else {
                    node.name
                }

                val isEditing = state.inlineEditingNodeId == node.id

                RightSideNameText(
                    name = displayName,
                    screenPos = screenPos,
                    nodeWidthOnScreen = nodeWidthForLabel * scale,
                    nodeHeight = nodeHeight,
                    scale = scale,
                    isEditing = isEditing,
                    editingText = if (isEditing) state.inlineEditingText else "",
                    onEditingTextChanged = { state.inlineEditingText = it },
                    onStartEdit = {
                        state.inlineEditingNodeId = node.id
                        state.inlineEditingText = displayName
                        // АВТО-ПЕРЕКЛЮЧЕНИЕ ВКЛАДКИ
                        state.selectedTab = feature.projecteditor.ui.components.EditorTab.ANNOTATIONS
                    },
                    onFinishEdit = {
                        state.finishInlineEditing()
                    }
                )
            }
        }
    }
}