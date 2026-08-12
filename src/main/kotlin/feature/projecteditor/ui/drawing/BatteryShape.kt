package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawBatteryShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFF388E3C)
    val strokeWidth = if (isSelected) 3f else 1.5f

    val rectWidth = size.width * 0.5f
    val rectHeight = size.height * 0.9f
    val startX = topLeft.x + (size.width - rectWidth) / 2
    val startY = topLeft.y + (size.height - rectHeight) / 2

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
    drawRect(Color.White, Offset(startX, startY), Size(rectWidth, rectHeight))
    drawRect(borderColor, Offset(startX, startY), Size(rectWidth, rectHeight), style = Stroke(strokeWidth, pathEffect = dashEffect))

    val midX = startX + rectWidth / 2
    val plateGap = rectHeight * 0.05f
    val longPlateW = rectWidth * 0.6f
    val shortPlateW = rectWidth * 0.3f

    val y1 = startY + rectHeight * 0.3f
    val y2 = startY + rectHeight * 0.7f

    drawLine(borderColor, Offset(midX, startY + rectHeight * 0.1f), Offset(midX, y1 - plateGap), strokeWidth)
    drawLine(borderColor, Offset(midX, y1 + plateGap), Offset(midX, y2 - plateGap), strokeWidth)
    drawLine(borderColor, Offset(midX, y2 + plateGap), Offset(midX, startY + rectHeight * 0.9f), strokeWidth)

    drawLine(borderColor, Offset(midX - longPlateW / 2, y1 - plateGap), Offset(midX + longPlateW / 2, y1 - plateGap), strokeWidth)
    drawLine(borderColor, Offset(midX - shortPlateW / 2, y1 + plateGap), Offset(midX + shortPlateW / 2, y1 + plateGap), strokeWidth * 2.5f)

    drawLine(borderColor, Offset(midX - longPlateW / 2, y2 - plateGap), Offset(midX + longPlateW / 2, y2 - plateGap), strokeWidth)
    drawLine(borderColor, Offset(midX - shortPlateW / 2, y2 + plateGap), Offset(midX + shortPlateW / 2, y2 + plateGap), strokeWidth * 2.5f)
}
