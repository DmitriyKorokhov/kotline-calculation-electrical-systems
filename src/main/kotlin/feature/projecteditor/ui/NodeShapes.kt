package feature.projecteditor.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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

    // 1. Верхняя половина (белая)
    drawRect(
        color = Color.White,
        topLeft = topLeft,
        size = Size(size.width, size.height / 2f)
    )

    // 2. Нижняя половина (черная)
    drawRect(
        color = Color.Black,
        topLeft = Offset(topLeft.x, topLeft.y + size.height / 2f),
        size = Size(size.width, size.height / 2f)
    )

    // 3. Общая рамка поверх
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
fun DrawScope.drawUpsShape(
    textMeasurer: TextMeasurer,
    topLeft: Offset,
    size: Size,
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFF1976D2)
    val strokeWidth = if (isSelected) 3f else 1.5f

    // Рассчитываем размер квадрата так, чтобы осталось место для каскада (уголков)
    val squareSize = size.height * 0.8f
    val delta = size.height * 0.1f // Смещение для эффекта наслоения 3D

    // Центрируем общую фигуру по ширине выделенной области
    val startX = topLeft.x + (size.width - squareSize - delta * 2) / 2
    val startY = topLeft.y + delta * 2 // Опускаем первый квадрат, чтобы влезли задние

    // 1. Задний квадрат (самый дальний)
    val backTopLeft = Offset(startX + delta * 2, startY - delta * 2)
    drawRect(Color.White, backTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, backTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    // 2. Средний квадрат
    val midTopLeft = Offset(startX + delta, startY - delta)
    drawRect(Color.White, midTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, midTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    // 3. Передний квадрат (основной)
    val frontTopLeft = Offset(startX, startY)
    drawRect(Color.White, frontTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, frontTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    // 4. Диагональ в переднем квадрате (снизу-слева направо-вверх)
    drawLine(
        color = borderColor,
        start = Offset(frontTopLeft.x, frontTopLeft.y + squareSize),
        end = Offset(frontTopLeft.x + squareSize, frontTopLeft.y),
        strokeWidth = strokeWidth
    )

    // 5. Рисуем значки синусоиды (~)
    val style = TextStyle(
        color = borderColor,
        fontSize = (squareSize * 0.45f).sp,
        fontWeight = FontWeight.Normal
    )
    val textLayoutResult = textMeasurer.measure("~", style)
    val tw = textLayoutResult.size.width
    val th = textLayoutResult.size.height

    // Верхний левый треугольник
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = frontTopLeft.x + squareSize * 0.25f - tw / 2,
            y = frontTopLeft.y + squareSize * 0.25f - th / 2 - (squareSize * 0.1f)
        )
    )

    // Нижний правый треугольник
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = frontTopLeft.x + squareSize * 0.75f - tw / 2,
            y = frontTopLeft.y + squareSize * 0.75f - th / 2 - (squareSize * 0.1f)
        )
    )
}

// В файле feature/projecteditor/ui/NodeShapes.kt

fun DrawScope.drawBatteryShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFF388E3C)
    val strokeWidth = if (isSelected) 3f else 1.5f

    // 1. Делаем вертикальный прямоугольник ("нижняя сторона меньше боковой")
    val rectWidth = size.width * 0.5f
    val rectHeight = size.height * 0.9f
    // Центрируем его внутри выделенной области узла
    val startX = topLeft.x + (size.width - rectWidth) / 2
    val startY = topLeft.y + (size.height - rectHeight) / 2

    // 2. Рисуем пунктирный контур (массив АКБ)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f) // 10px линия, 5px пропуск
    drawRect(Color.White, Offset(startX, startY), Size(rectWidth, rectHeight))
    drawRect(
        color = borderColor,
        topLeft = Offset(startX, startY),
        size = Size(rectWidth, rectHeight),
        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
    )

    // Параметры для внутренних элементов (самих АКБ)
    val midX = startX + rectWidth / 2
    val plateGap = rectHeight * 0.05f // Расстояние между плюсом и минусом
    val longPlateW = rectWidth * 0.6f  // Длинная линия (Плюс)
    val shortPlateW = rectWidth * 0.3f // Короткая линия (Минус)

    // Центры для первой и второй АКБ по вертикали
    val y1 = startY + rectHeight * 0.3f
    val y2 = startY + rectHeight * 0.7f

    // 3. Рисуем вертикальные провода
    // Провод сверху до первой АКБ
    drawLine(borderColor, Offset(midX, startY + rectHeight * 0.1f), Offset(midX, y1 - plateGap), strokeWidth)
    // Перемычка между АКБ 1 и АКБ 2
    drawLine(borderColor, Offset(midX, y1 + plateGap), Offset(midX, y2 - plateGap), strokeWidth)
    // Провод от второй АКБ вниз
    drawLine(borderColor, Offset(midX, y2 + plateGap), Offset(midX, startY + rectHeight * 0.9f), strokeWidth)

    // 4. Рисуем пластины 1-й АКБ
    // Плюс (длинная, тонкая)
    drawLine(borderColor, Offset(midX - longPlateW / 2, y1 - plateGap), Offset(midX + longPlateW / 2, y1 - plateGap), strokeWidth)
    // Минус (короткая, толстая)
    drawLine(borderColor, Offset(midX - shortPlateW / 2, y1 + plateGap), Offset(midX + shortPlateW / 2, y1 + plateGap), strokeWidth * 2.5f)

    // 5. Рисуем пластины 2-й АКБ
    // Плюс
    drawLine(borderColor, Offset(midX - longPlateW / 2, y2 - plateGap), Offset(midX + longPlateW / 2, y2 - plateGap), strokeWidth)
    // Минус
    drawLine(borderColor, Offset(midX - shortPlateW / 2, y2 + plateGap), Offset(midX + shortPlateW / 2, y2 + plateGap), strokeWidth * 2.5f)
}

// В файле feature/projecteditor/ui/NodeShapes.kt

fun DrawScope.drawSolarPanelShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFF57C00) // Оранжевый
    val strokeWidth = if (isSelected) 3f else 1.5f

    // Размеры одной панели (делаем ее слегка вытянутой)
    val panelWidth = size.width * 0.65f
    val panelHeight = size.height * 0.85f
    val delta = size.height * 0.1f // Смещение для эффекта массива

    // Центрируем общую группу с учетом смещения слоев
    val startX = topLeft.x + (size.width - panelWidth - delta * 2) / 2
    val startY = topLeft.y + delta * 2

    // 1. Задняя панель
    val backTopLeft = Offset(startX + delta * 2, startY - delta * 2)
    drawRect(Color.White, backTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, backTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    // 2. Средняя панель
    val midTopLeft = Offset(startX + delta, startY - delta)
    drawRect(Color.White, midTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, midTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    // 3. Передняя панель (основная)
    val frontTopLeft = Offset(startX, startY)
    drawRect(Color.White, frontTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, frontTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    // 4. Рисуем сетку (фотоэлементы) на передней панели
    val cols = 3 // Количество колонок
    val rows = 4 // Количество рядов (строк)
    val gridStroke = 1f // Тонкие линии для сетки

    // Вертикальные разделители
    for (i in 1 until cols) {
        val x = frontTopLeft.x + (panelWidth / cols) * i
        drawLine(
            color = borderColor,
            start = Offset(x, frontTopLeft.y),
            end = Offset(x, frontTopLeft.y + panelHeight),
            strokeWidth = gridStroke
        )
    }

    // Горизонтальные разделители
    for (i in 1 until rows) {
        val y = frontTopLeft.y + (panelHeight / rows) * i
        drawLine(
            color = borderColor,
            start = Offset(frontTopLeft.x, y),
            end = Offset(frontTopLeft.x + panelWidth, y),
            strokeWidth = gridStroke
        )
    }
}

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawInverterShape(
    textMeasurer: TextMeasurer,
    topLeft: Offset,
    size: Size,
    isSelected: Boolean = false
) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFD32F2F) // Красный
    val strokeWidth = if (isSelected) 3f else 1.5f

    // 1. Рисуем основной прямоугольник
    drawRect(Color.White, topLeft, size)
    drawRect(borderColor, topLeft, size, style = Stroke(strokeWidth))

    // 2. Диагональная линия (с левого нижнего угла в правый верхний)
    drawLine(
        color = borderColor,
        start = Offset(topLeft.x, topLeft.y + size.height),
        end = Offset(topLeft.x + size.width, topLeft.y),
        strokeWidth = strokeWidth
    )

    // 3. DC (Постоянный ток) в левом верхнем треугольнике
    // Рисуем прямую горизонтальную линию
    val dcCenterX = topLeft.x + size.width * 0.25f
    val dcCenterY = topLeft.y + size.height * 0.25f
    val lineLength = size.width * 0.2f
    drawLine(
        color = borderColor,
        start = Offset(dcCenterX - lineLength / 2, dcCenterY),
        end = Offset(dcCenterX + lineLength / 2, dcCenterY),
        strokeWidth = strokeWidth
    )

    // 4. AC (Переменный ток) в правом нижнем треугольнике
    // Используем знак синусоиды (~)
    val style = TextStyle(
        color = borderColor,
        fontSize = (size.height * 0.45f).sp, // Размер символа
        fontWeight = FontWeight.Normal
    )
    val acLayoutResult = textMeasurer.measure("~", style)

    // Центрируем синусоиду (немного приподнимаем вверх, так как у символа ~ есть отступ снизу)
    drawText(
        textLayoutResult = acLayoutResult,
        topLeft = Offset(
            x = topLeft.x + size.width * 0.75f - acLayoutResult.size.width / 2,
            y = topLeft.y + size.height * 0.75f - acLayoutResult.size.height / 2 - (size.height * 0.08f)
        )
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