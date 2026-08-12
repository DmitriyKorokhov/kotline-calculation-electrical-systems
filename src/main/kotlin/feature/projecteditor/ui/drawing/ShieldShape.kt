package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawShieldShape(topLeft: Offset, size: Size, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    drawRect(Color.White, topLeft, Size(size.width, size.height / 2f))
    drawRect(Color.Black, Offset(topLeft.x, topLeft.y + size.height / 2f), Size(size.width, size.height / 2f))
    drawRect(borderColor, topLeft, size, style = Stroke(strokeWidth))
}
