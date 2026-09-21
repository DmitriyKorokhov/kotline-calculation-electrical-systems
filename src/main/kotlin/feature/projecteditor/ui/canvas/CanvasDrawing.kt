package feature.projecteditor.ui.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import feature.projecteditor.domain.*
import feature.projecteditor.state.ProjectCanvasState
import feature.projecteditor.state.getNodeHeight
import feature.projecteditor.ui.drawing.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import feature.projecteditor.ui.utils.toOffset
import feature.projecteditor.ui.utils.toPoint
import kotlin.math.abs
import kotlin.math.floor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import feature.projecteditor.state.CanvasToolMode
import kotlin.math.sqrt

private const val NODE_WIDTH = 120f
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawProjectCanvas(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    withTransform({
        translate(left = state.offset.x, top = state.offset.y)
        scale(scale = state.scale, pivot = Offset.Zero)
    }) {
        val topLeftWorld = state.screenToWorld(Offset.Zero.toPoint())
        val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height).toPoint())

        drawGrid(topLeftWorld.toOffset(), bottomRightWorld.toOffset())
        drawLevels(state.levels, topLeftWorld.toOffset(), bottomRightWorld.toOffset(), state.scale)
        drawConnections(state)
        drawNodes(textMeasurer, state)
        drawPins(state)
        drawSelectionBox(state)

        // --- БЛОК ВИЗУАЛЬНОГО ФИЛЬТРА (РЕНТГЕН) ---
        if (state.searchQuery.isNotBlank()) {
            drawRect(Color.White.copy(alpha = 0.8f), topLeft = topLeftWorld.toOffset(), size = Size(bottomRightWorld.x - topLeftWorld.x, bottomRightWorld.y - topLeftWorld.y))
            if (state.searchResults.isNotEmpty()) {
                drawNodes(textMeasurer, state, state.searchResults)
                val currentTarget = state.searchResults.getOrNull(state.currentSearchIndex)
                if (currentTarget != null) {
                    val box = feature.projecteditor.ui.selection.getBoundingBox(currentTarget)
                    drawRoundRect(
                        color = Color.Red,
                        topLeft = Offset(box.left - 4f, box.top - 4f),
                        size = Size(box.width + 8f, box.height + 8f),
                        style = Stroke(3f / state.scale),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        // --- ИСПРАВЛЕНИЕ: ПРЕВЬЮ РИСУЕТСЯ ВНУТРИ withTransform ---
        val mousePos = state.currentMousePos
        if (mousePos != null && state.tempPoints.isNotEmpty()) {
            val stroke = getCustomStroke(state.currentLineWeight, state.currentLineType, state.scale)
            val color = Color(state.currentLineColor).copy(alpha = 0.6f) // Полупрозрачное превью

            when (state.currentToolMode) {
                CanvasToolMode.DRAW_CIRCLE -> {
                    val center = state.tempPoints[0]
                    val radius = kotlin.math.sqrt(1.0 * (mousePos.x - center.x) * (mousePos.x - center.x) + (mousePos.y - center.y) * (mousePos.y - center.y)).toFloat()
                    drawCircle(color, radius, center.toOffset(), style = stroke)
                }
                CanvasToolMode.DRAW_RECTANGLE -> {
                    val p1 = state.tempPoints[0]
                    val w = kotlin.math.abs(mousePos.x - p1.x)
                    val h = kotlin.math.abs(mousePos.y - p1.y)
                    val rectTopLeft = Offset(minOf(p1.x, mousePos.x), minOf(p1.y, mousePos.y))
                    drawRect(color, rectTopLeft, Size(w, h), style = stroke)
                }
                CanvasToolMode.DRAW_POLYLINE -> {
                    val path = Path().apply {
                        moveTo(state.tempPoints.first().x, state.tempPoints.first().y)
                        for (i in 1 until state.tempPoints.size) lineTo(state.tempPoints[i].x, state.tempPoints[i].y)
                        lineTo(mousePos.x, mousePos.y)
                    }
                    drawPath(path, color, style = stroke)

                    // Кружок на первой точке для замыкания
                    val firstPoint = state.tempPoints.first()
                    drawCircle(
                        color = Color.Red,
                        radius = 8f / state.scale,
                        center = firstPoint.toOffset(),
                        style = Stroke(2f / state.scale)
                    )
                }
                else -> {}
            }
        }
    }
}

private fun DrawScope.drawGrid(topLeft: Offset, bottomRight: Offset) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val left = (topLeft.x - GRID_WIDTH).toInt() - ((topLeft.x - GRID_WIDTH).toInt() % GRID_WIDTH.toInt())
    val top = (topLeft.y - GRID_HEIGHT).toInt() - ((topLeft.y - GRID_HEIGHT).toInt() % GRID_HEIGHT.toInt())
    val right = (bottomRight.x + GRID_WIDTH).toInt()
    val bottom = (bottomRight.y + GRID_HEIGHT).toInt()

    for (i in left..right step GRID_WIDTH.toInt()) {
        drawLine(gridColor, start = Offset(i.toFloat(), top.toFloat()), end = Offset(i.toFloat(), bottom.toFloat()))
    }
    for (i in top..bottom step GRID_HEIGHT.toInt()) {
        drawLine(gridColor, start = Offset(left.toFloat(), i.toFloat()), end = Offset(right.toFloat(), i.toFloat()))
    }
}

private fun DrawScope.drawLevels(levels: List<LevelLine>, topLeft: Offset, bottomRight: Offset, scale: Float) {
    levels.forEach { level ->
        drawLine(
            color = Color.Gray,
            start = Offset(topLeft.x, level.yPosition),
            end = Offset(bottomRight.x, level.yPosition),
            strokeWidth = 2f / scale,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

private data class LineSegment(
    val connection: Connection,
    val start: Offset,
    val end: Offset,
    val isHorizontal: Boolean
)

private fun DrawScope.drawConnections(state: ProjectCanvasState) {
    val strokeWidth = 2f / state.scale
    // 1. Убрали зависимость от state.scale. Теперь радиус жестко привязан к миру (как ширина узлов).
    val jumpRadius = 15f

    val allSegments = mutableListOf<LineSegment>()
    state.connections.forEach { conn ->
        val pts = state.calculateConnectionPoints(conn)
        for (i in 0 until pts.size - 1) {
            val p1 = pts[i].toOffset()
            val p2 = pts[i+1].toOffset()
            val isHorizontal = kotlin.math.abs(p1.y - p2.y) < kotlin.math.abs(p1.x - p2.x)
            allSegments.add(LineSegment(conn, p1, p2, isHorizontal))
        }
    }

    val verticalSegments = allSegments.filter { !it.isHorizontal }
    val horizontalSegments = allSegments.filter { it.isHorizontal }

    val normalColor = Color.Gray
    val selectedColor = Color.Blue

    verticalSegments.forEach { seg ->
        val color = if (state.selectedConnections.contains(seg.connection)) selectedColor else normalColor
        drawLine(color = color, start = seg.start, end = seg.end, strokeWidth = strokeWidth)
    }

    horizontalSegments.forEach { seg ->
        val color = if (state.selectedConnections.contains(seg.connection)) selectedColor else normalColor
        val startX = seg.start.x
        val endX = seg.end.x
        val y = seg.start.y

        val dir = if (endX > startX) 1f else -1f

        val intersections = verticalSegments.filter { vSeg ->
            if (vSeg.connection == seg.connection) return@filter false

            val vX = vSeg.start.x
            val vMinY = minOf(vSeg.start.y, vSeg.end.y)
            val vMaxY = maxOf(vSeg.start.y, vSeg.end.y)

            val isXIntersect = if (dir > 0) vX in startX..endX else vX in endX..startX
            val isYIntersect = y in vMinY..vMaxY

            isXIntersect && isYIntersect
        }
            .map { it.start.x }
            .distinct() // 2. Защита от дубликатов на одной оси
            .sortedBy { it * dir }

        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(startX, y)

        var currentX = startX

        intersections.forEach { intersectX ->
            val arcStartX = intersectX - jumpRadius * dir
            val arcEndX = intersectX + jumpRadius * dir

            // 3. Проверка наслоения: если пересечения слишком близко (или идентичны из-за погрешностей)
            val isOverlapping = (arcStartX - currentX) * dir <= 0

            if (!isOverlapping) {
                // Если наслоения нет, рисуем честную прямую линию до начала прыжка
                path.lineTo(arcStartX, y)
            }

            val rectLeft = minOf(intersectX - jumpRadius, intersectX + jumpRadius)
            val rectRight = maxOf(intersectX - jumpRadius, intersectX + jumpRadius)

            path.arcTo(
                rect = Rect(
                    left = rectLeft,
                    top = y - jumpRadius,
                    right = rectRight,
                    bottom = y + jumpRadius
                ),
                startAngleDegrees = if (dir > 0) 180f else 0f,
                sweepAngleDegrees = if (dir > 0) 180f else -180f,
                forceMoveTo = isOverlapping // Если наслоились, начинаем новую дугу БЕЗ прямой линии назад
            )

            // Двигаем currentX вперед с учетом направления
            currentX = if (dir > 0) maxOf(currentX, arcEndX) else minOf(currentX, arcEndX)
        }

        // Рисуем остаток линии до конца сегмента
        if ((endX - currentX) * dir > 0) {
            path.lineTo(endX, y)
        }

        drawPath(path, color = color, style = Stroke(strokeWidth))
    }

    // Отрисовка маркеров выделения...
    state.connections.forEach { conn ->
        val pts = state.calculateConnectionPoints(conn)
        if (pts.size >= 2 && state.selectedConnections.contains(conn)) {
            // Кружочки на внутренних углах
            for (i in 1 until pts.size - 1) {
                drawCircle(Color.Blue, radius = 6f / state.scale, center = pts[i].toOffset())
            }

            val pFirst = pts.first().toOffset()
            val pLast = pts.last().toOffset()
            val handleRadius = 6f / state.scale

            drawCircle(Color.Red, radius = handleRadius, center = pFirst)
            drawCircle(Color.Red, radius = handleRadius, center = pLast)
            for (i in 0 until pts.size - 1) {
                val p1 = pts[i]
                val p2 = pts[i+1]
                val mid = (p1 + p2) / 2f
                val isHorizontal = kotlin.math.abs(p1.y - p2.y) < kotlin.math.abs(p1.x - p2.x)
                val lineLen = 24f / state.scale
                val midThick = 8f / state.scale
                if (isHorizontal) {
                    drawLine(Color.Blue, start = Offset(mid.x - lineLen/2, mid.y), end = Offset(mid.x + lineLen/2, mid.y), strokeWidth = midThick)
                } else {
                    drawLine(Color.Blue, start = Offset(mid.x, mid.y - lineLen/2), end = Offset(mid.x, mid.y + lineLen/2), strokeWidth = midThick)
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawNodes(textMeasurer: TextMeasurer, state: ProjectCanvasState, nodesToDraw: List<ProjectNode> = state.nodes) {
    nodesToDraw.forEach { node ->
        // Модель подсвечивается, если она в массиве выделенных ИЛИ мы тянем от нее линию соединения
        val isSelected = state.selectedNodeIds.contains(node.id) || node.id == state.connectingFromNodeId
        when (node) {
            is ShieldNode -> {
                val height = getNodeHeight(node)
                drawShieldShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is UpsNode -> {
                val height = getNodeHeight(node)
                drawUpsShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
            }
            is BatteryNode -> {
                val height = getNodeHeight(node)
                drawBatteryShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is SolarPanelNode -> {
                val height = getNodeHeight(node)
                drawSolarPanelShape(Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2), Size(NODE_WIDTH, height), isSelected)
            }
            is InverterNode -> {
                val height = getNodeHeight(node)
                drawInverterShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
            }
            is TransformerNode -> drawTransformerShape(node.position.toOffset(), node.radiusOuter, isSelected)
            is GeneratorNode -> drawGeneratorShape(node.position.toOffset(), node.radius, isSelected)
            is SystemNode -> drawSystemShape( node.position.toOffset(), node.radius, isSelected)
            is ItRackRowNode -> {
                drawItRackRowShape(
                    centerOffset = node.position.toOffset(),
                    node = node,
                    isSelected = isSelected
                )
            }
            is RectifierNode -> {
                val height = getNodeHeight(node)
                drawRectifierShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
            }
            is CalloutNode -> {
                val textLayoutResult = textMeasurer.measure(
                    text = node.name.ifEmpty { "Текст выноски" },
                    style = TextStyle(
                        fontSize = node.fontSize.sp,
                        color = Color(node.colorArgb),
                        fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = TextDecoration.combine(
                            listOfNotNull(
                                if (node.isUnderline) TextDecoration.Underline else null,
                                if (node.isStrikethrough) TextDecoration.LineThrough else null
                            )
                        ),
                        // Убрали node.align, так как у выноски всегда левое выравнивание
                        textAlign = TextAlign.Left
                    )
                )

                val textW = textLayoutResult.size.width.toFloat()
                val textH = textLayoutResult.size.height.toFloat()
                val textTopLeft = Offset(node.position.x - textW / 2f, node.position.y - textH / 2f)
                val target = node.targetPoint.toOffset()
                val color = Color(node.colorArgb)

                // 1. Отрисовка линии от текста к цели
                val closestTextEdgeX = if (target.x < node.position.x) textTopLeft.x else textTopLeft.x + textW
                drawLine(color = color, start = target, end = Offset(closestTextEdgeX, node.position.y), strokeWidth = 2f / state.scale)

                // 2. Отрисовка наконечника (убрали / state.scale для радиуса и длины, чтобы масштабировалось вместе с чертежом)
                when (node.startStyle) {
                    0 -> { // Стрелка
                        val arrowLen = 15f
                        val angle = kotlin.math.atan2(node.position.y - target.y, closestTextEdgeX - target.x)
                        val p1 = Offset(target.x + arrowLen * kotlin.math.cos(angle - Math.PI/6).toFloat(), target.y + arrowLen * kotlin.math.sin(angle - Math.PI/6).toFloat())
                        val p2 = Offset(target.x + arrowLen * kotlin.math.cos(angle + Math.PI/6).toFloat(), target.y + arrowLen * kotlin.math.sin(angle + Math.PI/6).toFloat())
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(target.x, target.y)
                            lineTo(p1.x, p1.y)
                            moveTo(target.x, target.y)
                            lineTo(p2.x, p2.y)
                        }
                        // Но толщину линии (Stroke) оставляем неизменной для четкости
                        drawPath(path, color, style = Stroke(2f / state.scale))
                    }
                    1 -> drawCircle(color = color, radius = 7f, center = target, style = Stroke(2f / state.scale))
                    2 -> drawCircle(color = color, radius = 5f, center = target)
                }

                // 3. Отрисовка подложки текста
                if (node.hasBackground) {
                    drawRoundRect(
                        color = Color(node.backgroundColorArgb),
                        topLeft = textTopLeft,
                        size = Size(textW, textH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // 4. Отрисовка выделения (подсвечиваем и текст, и целевую точку)
                if (isSelected) {
                    if (state.inlineEditingNodeId != node.id) { // Рамку текста прячем при редактировании
                        drawRoundRect(
                            color = Color(0x339C27B0),
                            topLeft = textTopLeft,
                            size = Size(textW, textH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0xFF9C27B0),
                            topLeft = textTopLeft,
                            size = Size(textW, textH),
                            style = Stroke(2f / state.scale),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    // кружок на конце выноски оставляем всегда
                    drawCircle(color = Color(0xFF9C27B0), radius = 8f / state.scale, center = target, style = Stroke(2f / state.scale))
                }

                // 5. Отрисовка самого текста (если не редактируем)
                if (state.inlineEditingNodeId != node.id) {
                    drawText(textLayoutResult = textLayoutResult, topLeft = textTopLeft)
                }
            }

            is TextNode -> {
                val textLayoutResult = textMeasurer.measure(
                    text = node.name.ifEmpty { "Текст" }, // Плейсхолдер виден, пока нет реального текста
                    style = TextStyle(
                        fontSize = node.fontSize.sp,
                        color = Color(node.colorArgb),
                        fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = TextDecoration.combine(
                            listOfNotNull(
                                if (node.isUnderline) TextDecoration.Underline else null,
                                if (node.isStrikethrough) TextDecoration.LineThrough else null
                            )
                        ),
                        textAlign = when (node.align) {
                            1 -> TextAlign.Center
                            2 -> TextAlign.Right
                            else -> TextAlign.Left
                        }
                    )
                )

                val width = textLayoutResult.size.width.toFloat()
                val height = textLayoutResult.size.height.toFloat()
                val topLeft = Offset(node.position.x - width / 2f, node.position.y - height / 2f)

                if (node.hasBackground) {
                    drawRoundRect(
                        color = Color(node.backgroundColorArgb),
                        topLeft = topLeft,
                        size = Size(width, height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                if (isSelected && state.inlineEditingNodeId != node.id) {
                    drawRoundRect(
                        color = Color(0x339C27B0),
                        topLeft = topLeft,
                        size = Size(width, height),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF9C27B0), // Яркая фиолетовая рамка
                        topLeft = topLeft,
                        size = Size(width, height),
                        style = Stroke(2f / state.scale),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                if (state.inlineEditingNodeId != node.id) {
                    drawText(textLayoutResult = textLayoutResult, topLeft = topLeft)
                }
            }
            is CircleNode -> {
                drawCircle(
                    color = Color(node.colorArgb),
                    radius = node.radius,
                    center = node.position.toOffset(),
                    style = getCustomStroke(node.lineWeight, node.lineType, state.scale)
                )
                if (isSelected) drawCircle(Color(0xFF9C27B0), node.radius, node.position.toOffset(), style = Stroke(3f / state.scale))
                val showHandles = isSelected && state.selectedNodeIds.size == 1 && !state.isDraggingNode
                if (showHandles) {
                    val r = node.radius
                    val handleRadius = 6f / state.scale
                    val cx = node.position.x
                    val cy = node.position.y
                    drawCircle(Color.Red, handleRadius, Offset(cx, cy - r))
                    drawCircle(Color.Red, handleRadius, Offset(cx, cy + r))
                    drawCircle(Color.Red, handleRadius, Offset(cx - r, cy))
                    drawCircle(Color.Red, handleRadius, Offset(cx + r, cy))
                }
            }
            is RectangleNode -> {
                withTransform({
                    rotate(node.rotationDegrees, node.position.toOffset())
                }) {
                    val topLeft = Offset(node.position.x - node.width / 2, node.position.y - node.height / 2)
                    drawRect(
                        color = Color(node.colorArgb),
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(node.width, node.height),
                        style = getCustomStroke(node.lineWeight, node.lineType, state.scale)
                    )
                    if (isSelected) drawRect(Color(0xFF9C27B0), topLeft, androidx.compose.ui.geometry.Size(node.width, node.height), style = Stroke(3f / state.scale))

                    // РУЧКИ
                    val showHandles = isSelected && state.selectedNodeIds.size == 1 && !state.isDraggingNode
                    if (showHandles) {
                        val thick = 8f / state.scale
                        val lineLen = 24f / state.scale
                        val cx = node.position.x
                        val cy = node.position.y
                        val hw = node.width / 2
                        val hh = node.height / 2

                        // ИЗМЕНЕНО НА Color.Red (Задача 5)
                        drawLine(Color.Red, Offset(cx - lineLen/2, cy - hh), Offset(cx + lineLen/2, cy - hh), strokeWidth = thick)
                        drawLine(Color.Red, Offset(cx - lineLen/2, cy + hh), Offset(cx + lineLen/2, cy + hh), strokeWidth = thick)
                        drawLine(Color.Red, Offset(cx - hw, cy - lineLen/2), Offset(cx - hw, cy + lineLen/2), strokeWidth = thick)
                        drawLine(Color.Red, Offset(cx + hw, cy - lineLen/2), Offset(cx + hw, cy + lineLen/2), strokeWidth = thick)
                    }
                }
            }
            is PolylineNode -> {
                val path = Path().apply {
                    if (node.points.isNotEmpty()) {
                        moveTo(node.points.first().x, node.points.first().y)
                        for (i in 1 until node.points.size) {
                            lineTo(node.points[i].x, node.points[i].y)
                        }
                    }
                }

                drawPath(path, Color(node.colorArgb), style = getCustomStroke(node.lineWeight, node.lineType, state.scale))
                if (isSelected) drawPath(path, Color(0xFF9C27B0), style = Stroke(3f / state.scale))

                val showHandles = isSelected && state.selectedNodeIds.size == 1 && !state.isDraggingNode
                if (showHandles) {
                    val handleRadius = 6f / state.scale
                    node.points.distinct().forEach { pt ->
                        drawCircle(Color.Red, handleRadius, pt.toOffset())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawGridHeaders(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    val headerSize =24.dp.toPx()
    val headerHeight = headerSize
    val headerWidth = headerSize

    val bgColor = Color(0xFFF5F5F5)
    val lineColor = Color.Gray
    val textColor = Color.DarkGray

    // 1. Рисуем фоны панелей
    drawRect(color = bgColor, topLeft = Offset(0f, 0f), size = Size(size.width, headerHeight))
    drawRect(color = bgColor, topLeft = Offset(0f, 0f), size = Size(headerWidth, size.height))

    // Угловой квадрат (он всегда будет чистым)
    drawRect(color = Color(0xFFE0E0E0), topLeft = Offset(0f, 0f), size = Size(headerWidth, headerHeight))

    // Линии-границы самих панелей
    drawLine(lineColor, Offset(0f, headerHeight), Offset(size.width, headerHeight))
    drawLine(lineColor, Offset(headerWidth, 0f), Offset(headerWidth, size.height))

    // 2. Вычисляем видимую зону
    val topLeftWorld = state.screenToWorld(Offset(0f, 0f).toPoint())
    val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height).toPoint())

    val startCol = floor(topLeftWorld.x / GRID_WIDTH).toInt() - 1
    val endCol = floor(bottomRightWorld.x / GRID_WIDTH).toInt() + 1

    val startRow = floor(topLeftWorld.y / GRID_HEIGHT).toInt() - 1
    val endRow = floor(bottomRightWorld.y / GRID_HEIGHT).toInt() + 1

    val textStyle = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

    // 3. Рисуем столбцы (Буквы) — используем clipRect для жесткой обрезки!
    clipRect(left = headerWidth, top = 0f, right = size.width, bottom = headerHeight) {
        for (col in startCol..endCol) {
            val worldX = col * GRID_WIDTH
            val screenX = (worldX * state.scale) + state.offset.x

            // Линия деления колонки
            drawLine(lineColor, Offset(screenX, 0f), Offset(screenX, headerHeight))

            // Отрисовка текста
            val text = getExcelColumnName(col)
            val layoutResult = textMeasurer.measure(text, textStyle)

            val cellWidthOnScreen = GRID_WIDTH * state.scale
            val textX = screenX + (cellWidthOnScreen - layoutResult.size.width) / 2
            val textY = (headerHeight - layoutResult.size.height) / 2

            drawText(layoutResult, topLeft = Offset(textX, textY))
        }
    }

    // 4. Рисуем строки (Цифры) — используем clipRect для жесткой обрезки!
    clipRect(left = 0f, top = headerHeight, right = headerWidth, bottom = size.height) {
        for (row in startRow..endRow) {
            val worldY = row * GRID_HEIGHT
            val screenY = (worldY * state.scale) + state.offset.y

            // Линия деления строки
            drawLine(lineColor, Offset(0f, screenY), Offset(headerWidth, screenY))

            // Отрисовка текста
            val text = row.toString()
            val layoutResult = textMeasurer.measure(text, textStyle)

            val cellHeightOnScreen = GRID_HEIGHT * state.scale
            val textX = (headerWidth - layoutResult.size.width) / 2
            val textY = screenY + (cellHeightOnScreen - layoutResult.size.height) / 2

            drawText(layoutResult, topLeft = Offset(textX, textY))
        }
    }
}

private fun DrawScope.drawPins(state: ProjectCanvasState) {
    if (state.selectedConnections.size != 1 || state.selectedNodeIds.isNotEmpty()) return

    val normalRadius = 6f / state.scale
    val hoveredRadius = 10f / state.scale
    val conn = state.selectedConnections.first()

    state.nodes.forEach { node ->
        if (node.id != conn.fromId && node.id != conn.toId) return@forEach
        if (state.isDraggingLineEnd && state.draggingEndpointNodeId != null && node.id != state.draggingEndpointNodeId) return@forEach

        state.getAvailablePins(node).forEach { pinId ->
            val pin = state.getPinPosition(pinId.node, pinId.side, pinId.subId)
            val isHovered = state.hoveredPin == pinId

            drawCircle(
                color = if (isHovered) Color.Red else Color.Blue.copy(alpha = 0.5f),
                radius = if (isHovered) hoveredRadius else normalRadius,
                center = pin.toOffset()
            )
        }
    }
}

// Вспомогательная функция для генерации букв (A, B... Z, AA...).
// Поддерживает и отрицательные индексы (на случай если пользователь ушел влево: -A, -B)
private fun getExcelColumnName(index: Int): String {
    var num = abs(index)
    var name = ""
    while (num >= 0) {
        name = ('A' + (num % 26)) + name
        num = (num / 26) - 1
        if (num < 0) break
    }
    return if (index < 0) "-$name" else name
}

private fun DrawScope.drawSelectionBox(state: ProjectCanvasState) {
    val start = state.selectionStartScreen ?: return
    val end = state.selectionEndScreen ?: return

    val isLeftToRight = start.x < end.x

    // Цвета в стиле AutoCAD
    val fillColor = if (isLeftToRight) Color(0, 85, 255, 30) else Color(0, 255, 0, 30)
    val strokeColor = if (isLeftToRight) Color(0, 85, 255, 255) else Color(0, 255, 0, 255)

    val startWorld = state.screenToWorld(start).toOffset()
    val endWorld = state.screenToWorld(end).toOffset()

    val rect = Rect(startWorld, endWorld)
    drawRect(color = fillColor, topLeft = rect.topLeft, size = rect.size)

    if (isLeftToRight) {
        // Сплошная линия для Окна
        drawRect(color = strokeColor, topLeft = rect.topLeft, size = rect.size, style = Stroke(1.5f / state.scale))
    } else {
        // Пунктирная линия для Секущей
        drawRect(
            color = strokeColor,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(
                1.5f / state.scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f / state.scale, 10f / state.scale))
            )
        )
    }
}

private fun getCustomStroke(weight: Int, type: Int, scale: Float): Stroke {
    val strokeWidth = when (weight) { 0 -> 1.5f; 1 -> 4f; 2 -> 8f; else -> 4f } / scale
    val pathEffect = when (type) {
        1 -> PathEffect.dashPathEffect(floatArrayOf(15f / scale, 10f / scale), 0f)
        2 -> PathEffect.dashPathEffect(floatArrayOf(15f / scale, 8f / scale, 3f / scale, 8f / scale), 0f)
        else -> null
    }
    return Stroke(width = strokeWidth, pathEffect = pathEffect)
}
