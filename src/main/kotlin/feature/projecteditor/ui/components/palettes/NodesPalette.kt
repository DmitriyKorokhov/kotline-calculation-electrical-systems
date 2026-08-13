package feature.projecteditor.ui.components.palettes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.ui.drawing.*

enum class PaletteNodeType { SHIELD, TRANSFORMER, GENERATOR, UPS, BATTERY, SOLAR_PANEL, INVERTER, SYSTEM }

@OptIn(ExperimentalTextApi::class)
@Composable
fun NodesPalette(
    textMeasurer: TextMeasurer,
    cellHeight: Dp,
    onStartDrag: (PaletteNodeType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEndDrag: (Offset) -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Уменьшен вертикальный отступ (было 10.dp со всех сторон)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        // Используем spacedBy для равномерного и уменьшенного расстояния между элементами (вместо Spacer)
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Ширина слегка скорректирована для единообразия, а высота теперь везде одинаковая (cellHeight)
        PaletteItem(textMeasurer, "Щит", 120.dp, cellHeight, PaletteNodeType.SHIELD, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "Трансформатор", 130.dp, cellHeight, PaletteNodeType.TRANSFORMER, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "Генератор", 120.dp, cellHeight, PaletteNodeType.GENERATOR, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "ИБП", 120.dp, cellHeight, PaletteNodeType.UPS, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "АКБ", 120.dp, cellHeight, PaletteNodeType.BATTERY, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "Солнечная панель", 130.dp, cellHeight, PaletteNodeType.SOLAR_PANEL, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "Инвертор", 120.dp, cellHeight, PaletteNodeType.INVERTER, onStartDrag, onDrag, onEndDrag, onCancel)
        PaletteItem(textMeasurer, "Система", 120.dp, cellHeight, PaletteNodeType.SYSTEM, onStartDrag, onDrag, onEndDrag, onCancel)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun PaletteItem(
    textMeasurer: TextMeasurer, label: String, widthDp: Dp, heightDp: Dp,
    drawType: PaletteNodeType,
    onStartDrag: (PaletteNodeType, Offset) -> Unit, onDrag: (Offset) -> Unit, onEndDrag: (Offset) -> Unit, onCancel: () -> Unit
) {
    var itemTopLeft by remember { mutableStateOf(Offset.Zero) }
    var lastGlobalPointer by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier
            .size(widthDp, heightDp)
            .onGloballyPositioned { itemTopLeft = it.positionInRoot() }
            .pointerInput(drawType) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        val globalPointer = itemTopLeft + localOffset
                        lastGlobalPointer = globalPointer
                        onStartDrag(drawType, globalPointer)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val global = itemTopLeft + change.position
                        lastGlobalPointer = global
                        onDrag(global)
                    },
                    onDragEnd = {
                        val pos = lastGlobalPointer ?: (itemTopLeft + Offset(size.width / 2f, size.height / 2f))
                        onEndDrag(pos)
                        lastGlobalPointer = null
                    },
                    onDragCancel = {
                        onCancel()
                        lastGlobalPointer = null
                    }
                )
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDimension = minOf(size.width, size.height)
                val center = Offset(size.width / 2f, size.height / 2f)

                // Вспомогательная функция для отрисовки прямоугольных моделей с заданным масштабом
                fun drawRectShape(
                    scale: Float,
                    drawBlock: (topLeft: Offset, shapeSize: Size) -> Unit
                ) {
                    val w = size.width * scale
                    val h = size.height * scale
                    val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f)
                    drawBlock(topLeft, Size(w, h))
                }

                when (drawType) {
                    PaletteNodeType.SHIELD -> drawRectShape(0.65f) { topLeft, s ->
                        drawShieldShape(topLeft, s)
                    }

                    PaletteNodeType.TRANSFORMER ->
                        drawTransformerShape(center, minDimension * 0.38f)

                    PaletteNodeType.GENERATOR ->
                        drawGeneratorShape(center, minDimension * 0.45f)

                    PaletteNodeType.UPS -> drawRectShape(0.8f) { topLeft, s ->
                        drawUpsShape(textMeasurer, topLeft, s)
                    }
                    PaletteNodeType.BATTERY -> drawRectShape(0.8f) { topLeft, s ->
                        drawBatteryShape(topLeft, s)
                    }

                    PaletteNodeType.SOLAR_PANEL -> drawRectShape(0.65f) { topLeft, s ->
                        drawSolarPanelShape(topLeft, s)
                    }

                    PaletteNodeType.INVERTER -> drawRectShape(0.65f) { topLeft, s ->
                        drawInverterShape(textMeasurer, topLeft, s)
                    }

                    PaletteNodeType.SYSTEM -> drawSystemShape(center, minDimension * 0.45f)
                }
            }
        }

        Text(
            text = label,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}