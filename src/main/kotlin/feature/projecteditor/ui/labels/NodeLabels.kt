package feature.projecteditor.ui.labels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RightSideNameText(name: String, screenPos: Offset, nodeWidthOnScreen: Float, nodeHeight: Float, scale: Float) {
    val density = LocalDensity.current

    // Масштабируем отступ (gap) от элемента, чтобы он был пропорциональным
    val gap = 15f * scale
    val offsetX = with(density) { (screenPos.x + nodeWidthOnScreen / 2f + gap).toDp() }

    // Масштабируем размеры контейнера текста
    val boxWidth = (200f * scale).dp
    val boxHeight = (40f * scale).dp
    val offsetY = with(density) { screenPos.y.toDp() } - (boxHeight / 2)

    Box(
        modifier = Modifier
            .offset(offsetX, offsetY)
            .size(width = boxWidth, height = boxHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name,
            color = MaterialTheme.colors.onSurface,
            fontSize = (14f * scale).sp,
            lineHeight = (16f * scale).sp, // Масштабируем межстрочный интервал для переносов
            textAlign = TextAlign.Start,
            softWrap = true,
            maxLines = 2
        )
    }
}