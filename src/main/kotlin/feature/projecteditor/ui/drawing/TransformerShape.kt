package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawTransformerShape(center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    // 1. Уменьшаем общий размер модели
    val drawRadius = radius * 0.8f

    // 2. Сдвигаем круги ближе друг к другу (уменьшаем offsetY)
    val offsetY = drawRadius * 0.6f
    val c1 = Offset(center.x, center.y - offsetY)
    val c2 = Offset(center.x, center.y + offsetY)

    // Закрашиваем фоны белым и рисуем контуры
    drawCircle(Color.White, drawRadius, c1)
    drawCircle(Color.White, drawRadius, c2)
    drawCircle(borderColor, drawRadius, c1, style = Stroke(strokeWidth))
    drawCircle(borderColor, drawRadius, c2, style = Stroke(strokeWidth))

    // 3. Треугольник (сдвигаем вверх к краю верхнего круга)
    val tc = Offset(c1.x, c1.y - drawRadius * 0.35f) // Центр треугольника
    val tr = drawRadius * 0.35f // Размер треугольника
    val trianglePath = Path().apply {
        moveTo(tc.x, tc.y - tr)
        lineTo(tc.x + tr * 0.866f, tc.y + tr * 0.5f)
        lineTo(tc.x - tr * 0.866f, tc.y + tr * 0.5f)
        close()
    }
    drawPath(trianglePath, color = borderColor, style = Stroke(strokeWidth))

    // 4. Звезда (сдвигаем вниз к краю нижнего круга)
    val sc = Offset(c2.x, c2.y + drawRadius * 0.35f) // Центр звезды
    val sr = drawRadius * 0.35f // Размер звезды
    drawLine(borderColor, sc, Offset(sc.x, sc.y - sr), strokeWidth) // Вверх
    drawLine(borderColor, sc, Offset(sc.x - sr * 0.866f, sc.y + sr * 0.5f), strokeWidth) // Вниз-влево
    drawLine(borderColor, sc, Offset(sc.x + sr * 0.866f, sc.y + sr * 0.5f), strokeWidth) // Вниз-вправо

    // 5. Линия заземления (прямой угол: влево, затем вниз)
    val groundX = sc.x - drawRadius * 1.3f // Выносим линию влево за пределы круга
    val groundY1 = sc.y                    // Горизонтальная линия идет на уровне центра звезды
    val groundY2 = groundY1 + drawRadius * 0.7f // Опускаем линию вниз

    // Горизонтальный отрезок (от центра звезды влево)
    drawLine(borderColor, sc, Offset(groundX, groundY1), strokeWidth)

    // Вертикальный отрезок (вниз)
    drawLine(borderColor, Offset(groundX, groundY1), Offset(groundX, groundY2), strokeWidth)

    // Три горизонтальные полоски (знак заземления)
    val dashGap = drawRadius * 0.15f
    drawLine(borderColor, Offset(groundX - drawRadius * 0.25f, groundY2), Offset(groundX + drawRadius * 0.25f, groundY2), strokeWidth)
    drawLine(borderColor, Offset(groundX - drawRadius * 0.15f, groundY2 + dashGap), Offset(groundX + drawRadius * 0.15f, groundY2 + dashGap), strokeWidth)
    drawLine(borderColor, Offset(groundX - drawRadius * 0.05f, groundY2 + dashGap * 2), Offset(groundX + drawRadius * 0.05f, groundY2 + dashGap * 2), strokeWidth)
}