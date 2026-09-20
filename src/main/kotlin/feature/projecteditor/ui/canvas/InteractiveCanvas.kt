package feature.projecteditor.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.input.pointer.isTertiaryPressed
import feature.projecteditor.state.ConnectionHit
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import feature.projecteditor.state.CanvasToolMode
import feature.projecteditor.state.HandleHitType
import feature.projecteditor.state.ProjectRepository
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
    var skipNextTap by remember { mutableStateOf(false) }
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
            .onSizeChanged { state.canvasSize = Point(it.width.toFloat(), it.height.toFloat()) }
            .pointerHoverIcon(currentCursor)
            // 1. КЛИКИ (ЛКМ - выделение/соединение, Двойной клик - открытие щита)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val node = state.findNodeAtScreenPosition(offset.toPoint())
                        if (node is ShieldNode) {
                            onOpenShield(node.id)
                        } else if (node is ItRackRowNode) {
                            state.clearSelection()
                            state.selectedNodeIds.add(node.id)
                            state.selectedNode = node
                            state.showRackSettingsDialog = true
                        } else if (node is TextNode || node is CalloutNode) {
                            state.inlineEditingNodeId = node.id
                            state.inlineEditingText = node.name
                            state.previousTab = state.selectedTab // ЗАПОМИНАЕМ ПАНЕЛЬ
                            state.selectedTab = feature.projecteditor.ui.components.EditorTab.ANNOTATIONS
                        }
                    },
                    onPress = { offset ->
                        val worldPos = state.screenToWorld(offset.toPoint())

                        when (state.currentToolMode) {
                            CanvasToolMode.DRAW_CIRCLE -> {
                                if (state.tempPoints.isEmpty()) {
                                    state.tempPoints.add(worldPos)
                                } else {
                                    val center = state.tempPoints[0]
                                    val radius = kotlin.math.sqrt(1.0 * (worldPos.x - center.x) * (worldPos.x - center.x) + (worldPos.y - center.y) * (worldPos.y - center.y)).toFloat()
                                    state.saveHistory()
                                    feature.projecteditor.state.ProjectRepository.addNode(CircleNode(state.nextId++, "", center, radius, state.currentLineColor, state.currentLineWeight, state.currentLineType))
                                    state.tempPoints.clear()
                                    skipNextTap = true // ПРОПУСКАЕМ СЛЕДУЮЩИЙ КЛИК
                                    state.currentToolMode = CanvasToolMode.SELECT
                                }
                                return@detectTapGestures
                            }
                            CanvasToolMode.DRAW_RECTANGLE -> {
                                if (state.tempPoints.isEmpty()) {
                                    state.tempPoints.add(worldPos)
                                } else {
                                    val p1 = state.tempPoints[0]
                                    val width = kotlin.math.abs(worldPos.x - p1.x)
                                    val height = kotlin.math.abs(worldPos.y - p1.y)
                                    val center = Point((p1.x + worldPos.x) / 2, (p1.y + worldPos.y) / 2)
                                    state.saveHistory()
                                    feature.projecteditor.state.ProjectRepository.addNode(RectangleNode(state.nextId++, "", center, width, height, state.currentLineColor, state.currentLineWeight, state.currentLineType))
                                    state.tempPoints.clear()
                                    skipNextTap = true // ПРОПУСКАЕМ СЛЕДУЮЩИЙ КЛИК
                                    state.currentToolMode = CanvasToolMode.SELECT
                                }
                                return@detectTapGestures
                            }
                            CanvasToolMode.DRAW_POLYLINE -> {
                                if (state.tempPoints.isNotEmpty()) {
                                    val firstPoint = state.tempPoints.first()
                                    // Радиус замыкания
                                    val snapThreshold = (15f / state.scale)
                                    if ((worldPos - firstPoint).getDistanceSquared() < snapThreshold * snapThreshold && state.tempPoints.size > 2) {
                                        // Замыкаем
                                        state.tempPoints.add(firstPoint)
                                        state.finishPolyline() // Создаст ноду и очистит tempPoints
                                        skipNextTap = true
                                        return@detectTapGestures
                                    }
                                }
                                state.tempPoints.add(worldPos)
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_LEVEL -> {
                                state.saveHistory()
                                ProjectRepository.levels.add(LevelLine(state.nextId++, worldPos.y))
                                ProjectRepository.addLevel(LevelLine(state.nextId++, worldPos.y))
                                skipNextTap = true
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_TEXT -> {
                                state.saveHistory()
                                val newNode = TextNode(
                                    id = state.nextId++,
                                    name = "",
                                    position = worldPos,
                                    fontSize = state.defaultFontSize,
                                    colorArgb = state.defaultColorArgb,
                                    isBold = state.defaultIsBold,
                                    isItalic = state.defaultIsItalic,
                                    isUnderline = state.defaultIsUnderline,
                                    isStrikethrough = state.defaultIsStrikethrough,
                                    align = state.defaultAlign,
                                    hasBackground = state.defaultHasBackground,
                                    backgroundColorArgb = state.defaultBackgroundColorArgb
                                )
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = ""
                                ProjectRepository.addNode(newNode)
                                skipNextTap = true
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_CALLOUT -> {
                                state.saveHistory()
                                val textPos = Point(worldPos.x + 80f, worldPos.y - 80f)
                                val newNode = CalloutNode(
                                    id = state.nextId++,
                                    name = "",
                                    position = textPos,
                                    targetPoint = worldPos,
                                    fontSize = state.defaultFontSize,
                                    colorArgb = state.defaultColorArgb,
                                    isBold = state.defaultIsBold,
                                    isItalic = state.defaultIsItalic,
                                    isUnderline = state.defaultIsUnderline,
                                    isStrikethrough = state.defaultIsStrikethrough,
                                    hasBackground = state.defaultHasBackground,
                                    backgroundColorArgb = state.defaultBackgroundColorArgb,
                                    startStyle = state.defaultCalloutStartStyle
                                )
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = ""
                                ProjectRepository.addNode(newNode)
                                skipNextTap = true
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.SELECT -> { /* Оставляем обработку для onTap */ }
                        }
                    },
                    onTap = { offset ->
                        if (skipNextTap) {
                            skipNextTap = false
                            return@detectTapGestures
                        }
                        // ВАЖНО: Если включен инструмент, onTap игнорируется (все обработано в onPress)
                        if (state.currentToolMode != CanvasToolMode.SELECT) return@detectTapGestures

                        val worldPos = state.screenToWorld(offset.toPoint())

                        // Если мы в режиме редактирования текста - завершаем его при клике куда угодно
                        if (state.inlineEditingNodeId != null) {
                            state.finishInlineEditing()
                        }

                        when (state.currentToolMode) {
                            CanvasToolMode.DRAW_CIRCLE -> {
                                if (state.tempPoints.isEmpty()) {
                                    state.tempPoints.add(worldPos)
                                } else {
                                    val center = state.tempPoints[0]
                                    val radius = kotlin.math.sqrt(1.0 * (worldPos.x - center.x) * (worldPos.x - center.x) + (worldPos.y - center.y) * (worldPos.y - center.y)).toFloat()
                                    state.saveHistory()
                                    feature.projecteditor.state.ProjectRepository.addNode(CircleNode(state.nextId++, "", center, radius, state.currentLineColor, state.currentLineWeight, state.currentLineType))
                                    state.tempPoints.clear()
                                    state.currentToolMode = CanvasToolMode.SELECT
                                }
                                return@detectTapGestures
                            }
                            CanvasToolMode.DRAW_RECTANGLE -> {
                                if (state.tempPoints.isEmpty()) {
                                    state.tempPoints.add(worldPos)
                                } else {
                                    val p1 = state.tempPoints[0]
                                    val width = kotlin.math.abs(worldPos.x - p1.x)
                                    val height = kotlin.math.abs(worldPos.y - p1.y)
                                    val center = Point((p1.x + worldPos.x) / 2, (p1.y + worldPos.y) / 2)
                                    state.saveHistory()
                                    feature.projecteditor.state.ProjectRepository.addNode(RectangleNode(state.nextId++, "", center, width, height, state.currentLineColor, state.currentLineWeight, state.currentLineType))
                                    state.tempPoints.clear()
                                    state.currentToolMode = CanvasToolMode.SELECT
                                }
                                return@detectTapGestures
                            }
                            CanvasToolMode.DRAW_POLYLINE -> {
                                state.tempPoints.add(worldPos)
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_LEVEL -> {
                                state.saveHistory()
                                feature.projecteditor.state.ProjectRepository.levels.add(LevelLine(state.nextId++, worldPos.y))
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_TEXT -> {
                                state.saveHistory()
                                val newNode = TextNode(
                                    id = state.nextId++,
                                    name = "", // Изначально пустая строка (без слова "Текст")
                                    position = worldPos,
                                    fontSize = state.defaultFontSize,
                                    colorArgb = state.defaultColorArgb,
                                    isBold = state.defaultIsBold,
                                    isItalic = state.defaultIsItalic,
                                    isUnderline = state.defaultIsUnderline,
                                    isStrikethrough = state.defaultIsStrikethrough,
                                    align = state.defaultAlign,
                                    hasBackground = state.defaultHasBackground,
                                    backgroundColorArgb = state.defaultBackgroundColorArgb
                                )
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = ""
                                state.currentToolMode = CanvasToolMode.SELECT
                                return@detectTapGestures
                            }
                            CanvasToolMode.ADD_CALLOUT -> {
                                state.saveHistory()
                                // Текст появится со смещением, а сама стрелка останется там, куда кликнули
                                val textPos = Point(worldPos.x + 80f, worldPos.y - 80f)
                                val newNode = CalloutNode(
                                    id = state.nextId++,
                                    name = "",
                                    position = textPos,
                                    targetPoint = worldPos,
                                    fontSize = state.defaultFontSize,
                                    colorArgb = state.defaultColorArgb,
                                    isBold = state.defaultIsBold,
                                    isItalic = state.defaultIsItalic,
                                    isUnderline = state.defaultIsUnderline,
                                    isStrikethrough = state.defaultIsStrikethrough,
                                    hasBackground = state.defaultHasBackground,
                                    backgroundColorArgb = state.defaultBackgroundColorArgb,
                                    startStyle = state.defaultCalloutStartStyle
                                )
                                state.nodes.add(newNode)
                                state.clearSelection()
                                state.selectedNodeIds.add(newNode.id)
                                state.inlineEditingNodeId = newNode.id
                                state.inlineEditingText = ""
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
                            isZooming = true
                            state.onZoom(event.changes.first().scrollDelta.y, position.toPoint())
                            event.changes.first().consume()
                        } else if (event.type == PointerEventType.Move && !isPanning) {
                            isZooming = false
                            state.updateHoveredPin(position.toPoint())
                        }
                        // ФИКС РЕЗИНКИ: Всегда обновляем мировые координаты курсора,
                        // даже во время скролла или панорамирования!
                        state.currentMousePos = state.screenToWorld(position.toPoint())
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
                        val worldPos = state.screenToWorld(position.toPoint())

                        // Проверяем, тянем ли мы за маркер выноски
                        if (state.selectedNodeIds.size == 1) {
                            val singleNode = state.nodes.find { it.id == state.selectedNodeIds.first() }
                            if (singleNode is CalloutNode) {
                                val distSq = (worldPos - singleNode.targetPoint).getDistanceSquared()
                                val threshold = (15f / state.scale)
                                if (distSq < threshold * threshold) {
                                    state.saveHistory()
                                    dragTarget = "CalloutTargetPoint"
                                    return@detectDragGestures
                                }
                            }
                        }

                        val handleThresholdSq = (12f / state.scale) * (12f / state.scale)
                        var foundHandleHit = false

                        for (id in state.selectedNodeIds) {
                            val selectedNode = state.nodes.find { it.id == id } ?: continue

                            if (selectedNode is PolylineNode) {
                                for ((index, pt) in selectedNode.points.withIndex()) {
                                    if ((worldPos - pt).getDistanceSquared() < handleThresholdSq) {
                                        state.saveHistory()
                                        state.activeHandleHit = HandleHitType.POLYLINE_POINT
                                        state.activeHandleNodeId = selectedNode.id
                                        state.activeHandleIndex = index
                                        dragTarget = "GeometryHandle"
                                        foundHandleHit = true
                                        break
                                    }
                                }
                            } else if (selectedNode is CircleNode) {
                                val r = selectedNode.radius
                                val cx = selectedNode.position.x
                                val cy = selectedNode.position.y
                                val points = listOf(Point(cx, cy - r), Point(cx, cy + r), Point(cx - r, cy), Point(cx + r, cy))
                                if (points.any { (worldPos - it).getDistanceSquared() < handleThresholdSq }) {
                                    state.saveHistory()
                                    state.activeHandleHit = HandleHitType.CIRCLE_RADIUS
                                    state.activeHandleNodeId = selectedNode.id
                                    dragTarget = "GeometryHandle"
                                    foundHandleHit = true
                                    break
                                }
                            } else if (selectedNode is RectangleNode) {
                                val cx = selectedNode.position.x
                                val cy = selectedNode.position.y
                                val hw = selectedNode.width / 2
                                val hh = selectedNode.height / 2

                                val topPt = Point(cx, cy - hh)
                                val botPt = Point(cx, cy + hh)
                                val leftPt = Point(cx - hw, cy)
                                val rightPt = Point(cx + hw, cy)

                                if ((worldPos - topPt).getDistanceSquared() < handleThresholdSq) {
                                    state.activeHandleHit = HandleHitType.RECTANGLE_TOP
                                } else if ((worldPos - botPt).getDistanceSquared() < handleThresholdSq) {
                                    state.activeHandleHit = HandleHitType.RECTANGLE_BOTTOM
                                } else if ((worldPos - leftPt).getDistanceSquared() < handleThresholdSq) {
                                    state.activeHandleHit = HandleHitType.RECTANGLE_LEFT
                                } else if ((worldPos - rightPt).getDistanceSquared() < handleThresholdSq) {
                                    state.activeHandleHit = HandleHitType.RECTANGLE_RIGHT
                                }

                                if (state.activeHandleHit != HandleHitType.NONE) {
                                    state.saveHistory()
                                    state.activeHandleNodeId = selectedNode.id
                                    dragTarget = "GeometryHandle"
                                    foundHandleHit = true
                                    break
                                }
                            }
                            if (foundHandleHit) break
                        }

                        if (foundHandleHit) return@detectDragGestures

                        // ИСПРАВЛЕНИЕ ЗАДАЧИ 4: Если кликнули в Bounding Box УЖЕ ВЫДЕЛЕННОГО объекта — тянем выделение!
                        val clickedSelectedNodeId = state.selectedNodeIds.find { id ->
                            val n = state.nodes.find { it.id == id } ?: return@find false
                            val box = feature.projecteditor.ui.selection.getBoundingBox(n)
                            worldPos.x >= box.left && worldPos.x <= box.right && worldPos.y >= box.top && worldPos.y <= box.bottom
                        }

                        if (clickedSelectedNodeId != null) {
                            state.saveHistory()
                            state.isDraggingNode = true
                            dragTarget = "Nodes"
                            return@detectDragGestures
                        }

                        // Иначе проверяем строгое попадание в контуры (чтобы выделить новый объект)
                        val node = state.findNodeAtScreenPosition(position.toPoint())
                        val connHit = state.hitTestConnections(position.toPoint())
                        if (node != null) {
                            state.saveHistory()
                            if (!state.selectedNodeIds.contains(node.id)) {
                                if (!state.isCtrlPressed && !state.isShiftPressed) state.clearSelection()
                                state.selectedNodeIds.add(node.id)
                            }
                            state.isDraggingNode = true
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
                        state.isDraggingNode = false
                        state.activeHandleHit = HandleHitType.NONE
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
                        state.activeHandleHit = HandleHitType.NONE
                        state.activeHandleNodeId = null
                    },
                    onDragCancel = {
                        state.isDraggingNode = false
                        state.activeHandleHit = HandleHitType.NONE
                        state.isDraggingLineEnd = false
                        state.draggingEndpointNodeId = null
                        dragTarget = null
                        state.selectionStartScreen = null
                        state.selectionEndScreen = null
                        state.activeHandleHit = HandleHitType.NONE
                        state.activeHandleNodeId = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val deltaScreen = change.position - change.previousPosition
                        val scale = state.scale
                        val deltaWorld = Point(deltaScreen.x / scale, deltaScreen.y / scale)

                        if (dragTarget == "CalloutTargetPoint") {
                            val id = state.selectedNodeIds.firstOrNull() ?: return@detectDragGestures
                            val node = state.nodes.find { it.id == id } as? CalloutNode ?: return@detectDragGestures
                            val newTarget = Point(node.targetPoint.x + deltaWorld.x, node.targetPoint.y + deltaWorld.y)
                            val index = state.nodes.indexOf(node)
                            if (index != -1) {
                                state.nodes[index] = node.copy(targetPoint = newTarget)
                            }
                        }
                        else if (dragTarget == "GeometryHandle") {
                            val handleNode = state.nodes.find { it.id == state.activeHandleNodeId }
                            if (handleNode != null) {
                                when (state.activeHandleHit) {
                                    HandleHitType.POLYLINE_POINT -> {
                                        val poly = handleNode as PolylineNode
                                        val newPoints = poly.points.toMutableList()

                                        // Проверяем, замкнута ли полилиния
                                        val isClosed = newPoints.size > 2 && newPoints.first() == newPoints.last()

                                        val oldPt = newPoints[state.activeHandleIndex]
                                        val newPt = Point(oldPt.x + deltaWorld.x, oldPt.y + deltaWorld.y)

                                        newPoints[state.activeHandleIndex] = newPt

                                        // Если замкнута, тянем начальную и конечную точку как единый угол
                                        if (isClosed) {
                                            if (state.activeHandleIndex == 0) {
                                                newPoints[newPoints.lastIndex] = newPt
                                            } else if (state.activeHandleIndex == newPoints.lastIndex) {
                                                newPoints[0] = newPt
                                            }
                                        }

                                        feature.projecteditor.state.ProjectRepository.updateNode(poly.copy(points = newPoints))
                                    }
                                    HandleHitType.CIRCLE_RADIUS -> {
                                        val circle = handleNode as CircleNode
                                        val worldPos = state.screenToWorld(change.position.toPoint()) // Текущая позиция курсора
                                        val newRadius = kotlin.math.sqrt((worldPos - circle.position).getDistanceSquared().toDouble()).toFloat()
                                        feature.projecteditor.state.ProjectRepository.updateNode(circle.copy(radius = newRadius))
                                    }
                                    HandleHitType.RECTANGLE_TOP -> {
                                        val rect = handleNode as RectangleNode
                                        feature.projecteditor.state.ProjectRepository.updateNode(rect.copy(height = maxOf(1f, rect.height - deltaWorld.y), position = Point(rect.position.x, rect.position.y + deltaWorld.y / 2)))
                                    }
                                    HandleHitType.RECTANGLE_BOTTOM -> {
                                        val rect = handleNode as RectangleNode
                                        feature.projecteditor.state.ProjectRepository.updateNode(rect.copy(height = maxOf(1f, rect.height + deltaWorld.y), position = Point(rect.position.x, rect.position.y + deltaWorld.y / 2)))
                                    }
                                    HandleHitType.RECTANGLE_LEFT -> {
                                        val rect = handleNode as RectangleNode
                                        feature.projecteditor.state.ProjectRepository.updateNode(rect.copy(width = maxOf(1f, rect.width - deltaWorld.x), position = Point(rect.position.x + deltaWorld.x / 2, rect.position.y)))
                                    }
                                    HandleHitType.RECTANGLE_RIGHT -> {
                                        val rect = handleNode as RectangleNode
                                        feature.projecteditor.state.ProjectRepository.updateNode(rect.copy(width = maxOf(1f, rect.width + deltaWorld.x), position = Point(rect.position.x + deltaWorld.x / 2, rect.position.y)))
                                    }
                                    HandleHitType.NONE -> {}
                                }
                            }
                        }
                        else if (dragTarget == "Nodes") {
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
            if (state.draggingLabelNodeId != null) {
                val dragNode = state.nodes.find { it.id == state.draggingLabelNodeId }
                if (dragNode != null) {
                    val bounds = feature.projecteditor.ui.selection.getBoundingBox(dragNode)

                    val labelPins = listOf(
                        Point(bounds.left + bounds.width / 2f, bounds.top),
                        Point(bounds.left + bounds.width / 2f, bounds.bottom),
                        Point(bounds.left, bounds.top + bounds.height / 2f),
                        Point(bounds.right, bounds.top + bounds.height / 2f)
                    )

                    labelPins.forEach { pt ->
                        val screenPt = state.worldToScreen(pt).toOffset()
                        drawCircle(
                            color = Color.Blue.copy(alpha = 0.5f),
                            radius = maxOf(10f, 8f / state.scale),
                            center = screenPt
                        )
                    }
                }
            }
        }
        // УНИФИЦИРОВАННАЯ ОТРИСОВКА ПОДПИСЕЙ И РЕДАКТОРОВ
        state.nodes.forEach { node ->

            if (state.searchQuery.isNotBlank() && !state.searchResults.contains(node)) return@forEach

            val isEditing = state.inlineEditingNodeId == node.id

            // Теперь редактор открывается И для текста, И для выноски!
            if (node is TextNode || node is CalloutNode) {
                if (isEditing) {
                    val screenPos = state.worldToScreen(node.position).toOffset()
                    feature.projecteditor.ui.labels.AnnotationTextEditor(
                        node = node,
                        screenPos = screenPos,
                        scale = state.scale,
                        editingText = state.inlineEditingText,
                        onEditingTextChanged = { state.inlineEditingText = it },
                        onFinishEdit = { state.finishInlineEditing() }
                    )
                }
            } else if (node.name.isNotBlank() || node is ShieldNode) {
                // Для всех остальных узлов (оборудование) рисуем стандартный ярлык справа
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
                    feature.shieldeditor.state.ShieldStorage.loadOrCreate(node.id).shieldName.ifBlank { node.name }
                } else {
                    node.name
                }

                val labelSide = when (node) {
                    is ShieldNode -> node.labelSide
                    is TransformerNode -> node.labelSide
                    is GeneratorNode -> node.labelSide
                    is UpsNode -> node.labelSide
                    is BatteryNode -> node.labelSide
                    is SolarPanelNode -> node.labelSide
                    is InverterNode -> node.labelSide
                    is SystemNode -> node.labelSide
                    is ItRackRowNode -> node.labelSide
                    is RectifierNode -> node.labelSide
                    else -> AnchorSide.RIGHT
                }

                // ВЫЗЫВАЕМ НОВЫЙ КОМПОНЕНТ
                feature.projecteditor.ui.labels.NodeLabelText(
                    name = displayName,
                    nodePosScreen = screenPos,
                    nodeWidthOnScreen = nodeWidthForLabel * scale,
                    nodeHeightOnScreen = nodeHeight,
                    scale = scale,
                    labelSide = labelSide,
                    isEditing = isEditing,
                    editingText = if (isEditing) state.inlineEditingText else "",
                    onEditingTextChanged = { state.inlineEditingText = it },
                    onStartEdit = {
                        state.inlineEditingNodeId = node.id
                        state.inlineEditingText = displayName
                        state.previousTab = state.selectedTab
                        state.selectedTab = feature.projecteditor.ui.components.EditorTab.ANNOTATIONS
                    },
                    onFinishEdit = { state.finishInlineEditing() },
                    onDragStart = { state.draggingLabelNodeId = node.id },
                    onDragEnd = { dropPosScreen ->
                        state.snapLabelToClosestSide(node.id, dropPosScreen)
                        state.draggingLabelNodeId = null
                    }
                )
            }
        }
    }
}