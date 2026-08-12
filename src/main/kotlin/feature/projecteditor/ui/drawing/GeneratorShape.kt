package feature.projecteditor.ui.drawing

import androidx.compose.ui.geometry.Offset
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
fun DrawScope.drawGeneratorShape(textMeasurer: TextMeasurer, center: Offset, radius: Float, isSelected: Boolean = false) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Gray
    val strokeWidth = if (isSelected) 3f else 1.5f

    drawCircle(borderColor, radius, center, style = Stroke(strokeWidth))

    val style = TextStyle(color = borderColor, fontSize = (radius * 1.5).sp, fontWeight = FontWeight.Normal)
    val textLayoutResult = textMeasurer.measure("~", style)

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = center.x - textLayoutResult.size.width / 2,
            y = center.y - textLayoutResult.size.height / 2 - (radius * 0.15f)
        )
    )
}
