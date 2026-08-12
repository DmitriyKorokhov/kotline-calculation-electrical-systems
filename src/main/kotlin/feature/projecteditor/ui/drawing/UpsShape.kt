package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawUpsShape(textMeasurer: TextMeasurer, topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFF1976D2)
    val strokeWidth = if (isSelected) 3f else 1.5f

    val squareSize = size.height * 0.8f
    val delta = size.height * 0.1f

    val startX = topLeft.x + (size.width - squareSize - delta * 2) / 2
    val startY = topLeft.y + delta * 2

    val backTopLeft = Offset(startX + delta * 2, startY - delta * 2)
    drawRect(Color.White, backTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, backTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    val midTopLeft = Offset(startX + delta, startY - delta)
    drawRect(Color.White, midTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, midTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    val frontTopLeft = Offset(startX, startY)
    drawRect(Color.White, frontTopLeft, Size(squareSize, squareSize))
    drawRect(borderColor, frontTopLeft, Size(squareSize, squareSize), style = Stroke(strokeWidth))

    drawLine(
        borderColor,
        Offset(frontTopLeft.x, frontTopLeft.y + squareSize),
        Offset(frontTopLeft.x + squareSize, frontTopLeft.y),
        strokeWidth
    )

    val style = TextStyle(color = borderColor, fontSize = (squareSize * 0.45f).sp, fontWeight = FontWeight.Normal)
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
