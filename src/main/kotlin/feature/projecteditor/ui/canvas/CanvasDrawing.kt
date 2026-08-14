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
        drawNodes(textMeasurer, state.nodes, state.connectingFromNodeId, state.selectedNodeIds, state.scale)
        // Добавляем отрисовку рамки выделения
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

private fun DrawScope.drawConnections(state: ProjectCanvasState) {
    state.connections.forEach { conn ->
        val pts = state.calculateConnectionPoints(conn)
        if (pts.size >= 2) {
            val isSelected = state.selectedConnections.contains(conn)
            // Линия всегда серая
            val color = Color.Gray
            val strokeWidth = 2f / state.scale

            for (i in 0 until pts.size - 1) {
                drawLine(color = color, start = pts[i].toOffset(), end = pts[i+1].toOffset(), strokeWidth = strokeWidth)
            }

            // Отрисовка маркеров поверх выделенной линии
            if (isSelected) {
                // Кружочки на всех внутренних углах
                for (i in 1 until pts.size - 1) {
                    drawCircle(Color.Blue, radius = 6f / state.scale, center = pts[i].toOffset())
                }

                // Утолщения на ВСЕХ участках
                for (i in 0 until pts.size - 1) {
                    val p1 = pts[i]
                    val p2 = pts[i+1]
                    val mid = (p1 + p2) / 2f

                    val isHorizontal = kotlin.math.abs(p1.y - p2.y) < kotlin.math.abs(p1.x - p2.x)
                    val lineLen = 24f / state.scale
                    val midThick = 8f / state.scale // Сделано толще (было 6)

                    if (isHorizontal) {
                        drawLine(Color.Blue, start = Offset(mid.x - lineLen/2, mid.y), end = Offset(mid.x + lineLen/2, mid.y), strokeWidth = midThick)
                    } else {
                        drawLine(Color.Blue, start = Offset(mid.x, mid.y - lineLen/2), end = Offset(mid.x, mid.y + lineLen/2), strokeWidth = midThick)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawNodes(textMeasurer: TextMeasurer, nodes: List<ProjectNode>, connectingFromNodeId: Int?, selectedIds: List<Int>, scale: Float) {
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
        }
    }
}

private fun calculateConnectionX(node: ProjectNode, connectionIndex: Int, totalConnections: Int): Float {
    if (totalConnections <= 1) return node.position.x
    val span = when (node) {
        is TransformerNode -> node.radiusOuter * 1.5f
        is GeneratorNode -> node.radius * 1.5f
        is SystemNode -> node.radius * 1.5f
        else -> NODE_WIDTH * 0.8f
    }
    return (node.position.x - span / 2) + connectionIndex * (span / (totalConnections - 1))
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
