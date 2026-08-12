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
fun DrawScope.drawInverterShape(textMeasurer: TextMeasurer, topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFD32F2F)
    val strokeWidth = if (isSelected) 3f else 1.5f

    drawRect(Color.White, topLeft, size)
    drawRect(borderColor, topLeft, size, style = Stroke(strokeWidth))

    drawLine(borderColor, Offset(topLeft.x, topLeft.y + size.height), Offset(topLeft.x + size.width, topLeft.y), strokeWidth)

    val dcCenterX = topLeft.x + size.width * 0.25f
    val dcCenterY = topLeft.y + size.height * 0.25f
    val lineLength = size.width * 0.2f
    drawLine(borderColor, Offset(dcCenterX - lineLength / 2, dcCenterY), Offset(dcCenterX + lineLength / 2, dcCenterY), strokeWidth)

    val style = TextStyle(color = borderColor, fontSize = (size.height * 0.45f).sp, fontWeight = FontWeight.Normal)
    val acLayoutResult = textMeasurer.measure("~", style)

    drawText(
        textLayoutResult = acLayoutResult,
        topLeft = Offset(
            x = topLeft.x + size.width * 0.75f - acLayoutResult.size.width / 2,
            y = topLeft.y + size.height * 0.75f - acLayoutResult.size.height / 2 - (size.height * 0.08f)
        )
    )
}