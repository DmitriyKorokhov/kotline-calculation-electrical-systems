package ui.screens.projecteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer


/**
 * Отрисовывает фигуру для ShieldNode.
 */
fun DrawScope.drawShieldShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    drawRect(
        color = Color.White,
        topLeft = topLeft,
        size = size
    )
    drawRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Отрисовывает фигуру для PowerSourceNode.
 */
fun DrawScope.drawPowerSourceShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Black
    val strokeWidth = if (isSelected) 3f else 1.5f

    drawRect(
        color = Color.Black,
        topLeft = topLeft,
        size = size
    )
    drawRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Отрисовывает фигуру для TransformerNode.
 */
fun DrawScope.drawTransformerShape(center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    // Рассчитываем центры двух окружностей
    val c1 = Offset(center.x, center.y - radius / 2)
    val c2 = Offset(center.x, center.y + radius / 2)

    // Убрали заливку (drawCircle с белым цветом)
    // Рисуем только обводку для обеих окружностей

    // Первая окружность
    drawCircle(
        color = borderColor,
        radius = radius,
        center = c1,
        style = Stroke(width = strokeWidth)
    )

    // Вторая окружность
    drawCircle(
        color = borderColor,
        radius = radius,
        center = c2,
        style = Stroke(width = strokeWidth)
    )
}

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawGeneratorShape(
    textMeasurer: TextMeasurer,
    center: Offset,
    radius: Float,
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    // 1. Рисуем круг
    drawCircle(
        color = borderColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    // 2. Готовим стиль и измеряем текст
    val style = TextStyle(
        color = borderColor,
        fontSize = (radius * 1.5).sp,
        fontWeight = FontWeight.Normal
    )
    val textLayoutResult = textMeasurer.measure("~", style)

    // 3. Рисуем текст с точным центрированием
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = center.x - textLayoutResult.size.width / 2,
            y = center.y - textLayoutResult.size.height / 2 - (radius * 0.15f)
        )
    )
}