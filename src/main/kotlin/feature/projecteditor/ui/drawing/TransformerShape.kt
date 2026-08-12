package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawTransformerShape(center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    val c1 = Offset(center.x, center.y - radius / 2)
    val c2 = Offset(center.x, center.y + radius / 2)

    drawCircle(borderColor, radius, c1, style = Stroke(strokeWidth))
    drawCircle(borderColor, radius, c2, style = Stroke(strokeWidth))
}
