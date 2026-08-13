package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawGeneratorShape(center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    // 1. Делаем модель немного меньше по радиусу
    val drawRadius = radius * 0.85f

    // 2. Закрашиваем внутреннюю часть белым (чтобы скрыть всё, что под ней)
    drawCircle(color = Color.White, radius = drawRadius, center = center)

    // 3. Рисуем контур
    drawCircle(color = borderColor, radius = drawRadius, center = center, style = Stroke(strokeWidth))

    // 4. Отрисовка знака "~" (синусоиды)
    val wavePath = Path().apply {
        val w = drawRadius * 0.5f
        val h = drawRadius * 0.25f

        moveTo(center.x - w, center.y)
        quadraticTo(center.x - w / 2, center.y - h, center.x, center.y)
        quadraticTo(center.x + w / 2, center.y + h, center.x + w, center.y)
    }

    drawPath(
        path = wavePath,
        color = borderColor,
        style = Stroke(strokeWidth)
    )
}