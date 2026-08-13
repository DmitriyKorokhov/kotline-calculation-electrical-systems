package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawSystemShape(center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    // Используем тонкую линию (как у ДГУ), игнорируя isSelected для толщины самого символа
    val symbolStrokeWidth = 1.5f
    val borderStrokeWidth = if (isSelected) 3f else 1.5f

    val drawRadius = radius * 0.85f

    // 1. Белый фон и контур
    drawCircle(color = Color.White, radius = drawRadius, center = center)
    drawCircle(color = borderColor, radius = drawRadius, center = center, style = Stroke(borderStrokeWidth))

    // 2. Рисуем "C" вектором (окружность без сегмента)
    val symbolRadius = drawRadius * 0.6f
    val arcRect = Rect(
        center.x - symbolRadius,
        center.y - symbolRadius,
        center.x + symbolRadius,
        center.y + symbolRadius
    )

    val systemPath = Path().apply {
        // Начинаем дугу примерно с 45 градусов и рисуем 270 градусов
        addArc(arcRect, 45f, 270f)
    }

    drawPath(
        path = systemPath,
        color = borderColor,
        style = Stroke(symbolStrokeWidth) // Всегда тонкая линия
    )
}