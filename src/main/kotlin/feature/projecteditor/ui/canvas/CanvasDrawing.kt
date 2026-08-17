package feature.projecteditor.ui.canvas

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
import androidx.compose.ui.graphics.drawscope.Stroke
import feature.projecteditor.ui.utils.toOffset
import feature.projecteditor.ui.utils.toPoint
import kotlin.math.abs
import kotlin.math.floor

private const val NODE_WIDTH = 120f
private const val GRID_WIDTH = 200f
private const val GRID_HEIGHT = 140f

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawProjectCanvas(textMeasurer: TextMeasurer, state: ProjectCanvasState) {
    withTransform({
        translate(left = state.offset.x, top = state.offset.y)
        scale(scale = state.scale, pivot = Offset.Zero)
    }) {
        // Конвертируем Offset Compose в наш Point для запроса к стейту
        val topLeftWorld = state.screenToWorld(Offset.Zero.toPoint())
        val bottomRightWorld = state.screenToWorld(Offset(size.width, size.height).toPoint())

        // Для отрисовки переводим обратно в Offset
        drawGrid(topLeftWorld.toOffset(), bottomRightWorld.toOffset())
        drawLevels(state.levels, topLeftWorld.toOffset(), bottomRightWorld.toOffset(), state.scale)
        drawConnections(state)
        drawNodes(textMeasurer, state.nodes, state.connectingFromNodeId, state.selectedNodeIds)
        drawPins(state)
        drawSelectionBox(state)
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
private fun DrawScope.drawNodes(textMeasurer: TextMeasurer, nodes: List<ProjectNode>, connectingFromNodeId: Int?, selectedIds: List<Int>) {
    nodes.forEach { node ->
        // Модель подсвечивается, если она в массиве выделенных ИЛИ мы тянем от нее линию соединения
        val isSelected = selectedIds.contains(node.id) || node.id == connectingFromNodeId
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
                val height = feature.projecteditor.state.getNodeHeight(node)
                drawRectifierShape(
                    textMeasurer = textMeasurer,
                    topLeft = Offset(node.position.x - NODE_WIDTH / 2, node.position.y - height / 2),
                    size = Size(NODE_WIDTH, height),
                    isSelected = isSelected
                )
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

    val normalRadius = maxOf(6f, 5f / state.scale)
    val hoveredRadius = maxOf(10f, 8f / state.scale)
    val conn = state.selectedConnections.first()

    state.nodes.forEach { node ->
        // Показываем пины ТОЛЬКО на тех моделях, которые соединяет эта линия
        if (node.id != conn.fromId && node.id != conn.toId) return@forEach

        // Скрываем пины на противоположной модели при перетаскивании конца линии
        if (state.isDraggingLineEnd && state.draggingEndpointNodeId != null && node.id != state.draggingEndpointNodeId) return@forEach

        state.getAvailablePins(node).forEach { pinId ->
            val pin = state.getPinPosition(pinId.node, pinId.side, pinId.subId)
            val isHovered = state.hoveredPin == pinId // Сравниваем объекты PinId

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
