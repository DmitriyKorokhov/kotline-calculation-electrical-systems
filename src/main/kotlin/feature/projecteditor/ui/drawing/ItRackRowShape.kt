package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import feature.projecteditor.domain.ItRackRowNode
import feature.projecteditor.ui.selection.*

fun DrawScope.drawItRackRowShape(
    centerOffset: Offset,
    node: ItRackRowNode,
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.DarkGray
    val strokeWidth = if (isSelected) 3f else 1.5f

    val (totalWidth, totalHeight) = getItRackRowSize(node)
    val topLeft = Offset(centerOffset.x - totalWidth / 2, centerOffset.y - totalHeight / 2)

    val racksWidth = (node.racks.size * RACK_WIDTH) + ((node.racks.size - 1) * RACK_GAP)
    val startX = centerOffset.x - racksWidth / 2

    // 1. Получаем автоматическое распределение лучей
    val assignments = calculateFeedAssignments(node.feeds, node.racks)
    val topTracksCount = assignments.values.filter { it.isTop }.maxOfOrNull { it.trackIndex + 1 } ?: 0
    val racksTopY = topLeft.y + (if (topTracksCount > 0) FEED_MARGIN + (topTracksCount - 1) * FEED_LINE_SPACING else 0f)

    // 2. Отрисовка стоек
    node.racks.forEachIndexed { index, _ ->
        val x = startX + index * (RACK_WIDTH + RACK_GAP)

        drawRect(color = Color.White, topLeft = Offset(x, racksTopY), size = Size(RACK_WIDTH, RACK_HEIGHT))
        drawRect(color = borderColor, topLeft = Offset(x, racksTopY), size = Size(RACK_WIDTH, RACK_HEIGHT), style = Stroke(strokeWidth))

        val innerLineColor = if (isSelected) borderColor.copy(alpha = 0.6f) else Color.Gray
        val innerStroke = if (isSelected) 2f else 1f
        drawLine(innerLineColor, Offset(x, racksTopY + 20f), Offset(x + RACK_WIDTH, racksTopY + 20f), strokeWidth = innerStroke)
        drawLine(innerLineColor, Offset(x, racksTopY + 40f), Offset(x + RACK_WIDTH, racksTopY + 40f), strokeWidth = innerStroke)
    }

    // 3. Отрисовка упакованных лучей
    node.feeds.forEachIndexed { feedIndex, feed ->
        val assignment = assignments[feedIndex] ?: return@forEachIndexed

        val connectedIndices = node.racks.mapIndexedNotNull { index, rack ->
            if (feed.connectedRacks.contains(rack.index)) index else null
        }

        if (connectedIndices.isEmpty()) return@forEachIndexed

        val minIdx = connectedIndices.minOrNull() ?: 0
        val maxIdx = connectedIndices.maxOrNull() ?: 0

        // Вычисляем Y на основе присвоенного трека (автоматического уровня)
        val feedY = if (assignment.isTop) {
            topLeft.y + (assignment.trackIndex * FEED_LINE_SPACING)
        } else {
            racksTopY + RACK_HEIGHT + FEED_MARGIN + (assignment.trackIndex * FEED_LINE_SPACING)
        }

        val feedColor = if (isSelected) Color(0xFF6200EE) else Color(feed.colorArgb)
        val feedStroke = if (isSelected) 4f else 3f

        val feedStartX = startX + minIdx * (RACK_WIDTH + RACK_GAP) + RACK_WIDTH / 2
        val feedEndX = startX + maxIdx * (RACK_WIDTH + RACK_GAP) + RACK_WIDTH / 2

        // Рисуем горизонтальную линию только между крайними стойками этого луча
        if (minIdx != maxIdx) {
            drawLine(color = feedColor, start = Offset(feedStartX, feedY), end = Offset(feedEndX, feedY), strokeWidth = feedStroke)
        }

        // Вертикальные отводы
        connectedIndices.forEach { rackIdx ->
            val rackCenterX = startX + rackIdx * (RACK_WIDTH + RACK_GAP) + RACK_WIDTH / 2
            val targetY = if (assignment.isTop) racksTopY else racksTopY + RACK_HEIGHT

            drawLine(color = feedColor, start = Offset(rackCenterX, feedY), end = Offset(rackCenterX, targetY), strokeWidth = if (isSelected) 3f else 2f)
            drawCircle(color = feedColor, radius = if (isSelected) 5f else 4f, center = Offset(rackCenterX, feedY))
        }
    }
}