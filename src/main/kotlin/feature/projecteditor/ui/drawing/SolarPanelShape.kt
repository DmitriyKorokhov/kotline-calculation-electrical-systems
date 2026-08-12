package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawSolarPanelShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color(0xFFF57C00)
    val strokeWidth = if (isSelected) 3f else 1.5f

    val panelWidth = size.width * 0.65f
    val panelHeight = size.height * 0.85f
    val delta = size.height * 0.1f

    val startX = topLeft.x + (size.width - panelWidth - delta * 2) / 2
    val startY = topLeft.y + delta * 2

    val backTopLeft = Offset(startX + delta * 2, startY - delta * 2)
    drawRect(Color.White, backTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, backTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    val midTopLeft = Offset(startX + delta, startY - delta)
    drawRect(Color.White, midTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, midTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    val frontTopLeft = Offset(startX, startY)
    drawRect(Color.White, frontTopLeft, Size(panelWidth, panelHeight))
    drawRect(borderColor, frontTopLeft, Size(panelWidth, panelHeight), style = Stroke(strokeWidth))

    val cols = 3
    val rows = 4
    val gridStroke = 1f

    for (i in 1 until cols) {
        val x = frontTopLeft.x + (panelWidth / cols) * i
        drawLine(borderColor, Offset(x, frontTopLeft.y), Offset(x, frontTopLeft.y + panelHeight), gridStroke)
    }

    for (i in 1 until rows) {
        val y = frontTopLeft.y + (panelHeight / rows) * i
        drawLine(borderColor, Offset(frontTopLeft.x, y), Offset(frontTopLeft.x + panelWidth, y), gridStroke)
    }
}
