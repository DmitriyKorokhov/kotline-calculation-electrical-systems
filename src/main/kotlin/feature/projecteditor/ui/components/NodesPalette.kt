package feature.projecteditor.ui.components

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

enum class PaletteNodeType { SHIELD, TRANSFORMER, GENERATOR, UPS, BATTERY, SOLAR_PANEL, INVERTER }

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
        modifier = Modifier.fillMaxWidth().padding(10.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Start
    ) {
        PaletteItem(textMeasurer, "Щит", 130.dp, cellHeight, PaletteNodeType.SHIELD, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "Трансформатор", 160.dp, 110.dp, PaletteNodeType.TRANSFORMER, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "Генератор", 130.dp, cellHeight, PaletteNodeType.GENERATOR, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "ИБП", 130.dp, cellHeight, PaletteNodeType.UPS, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "АКБ", 130.dp, cellHeight, PaletteNodeType.BATTERY, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "Солн. Панель", 140.dp, cellHeight, PaletteNodeType.SOLAR_PANEL, onStartDrag, onDrag, onEndDrag, onCancel)
        Spacer(Modifier.width(16.dp))
        PaletteItem(textMeasurer, "Инвертор", 130.dp, cellHeight, PaletteNodeType.INVERTER, onStartDrag, onDrag, onEndDrag, onCancel)
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
    Box(
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
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width * 0.8f
            val h = size.height * 0.7f
            val centerOffset = Offset((size.width - w) / 2f, (size.height - h) / 2f)
            when (drawType) {
                PaletteNodeType.SHIELD -> drawShieldShape(centerOffset, Size(w, h))
                PaletteNodeType.TRANSFORMER -> drawTransformerShape(Offset(size.width / 2f, size.height / 2f), size.width * 0.25f)
                PaletteNodeType.GENERATOR -> drawGeneratorShape(Offset(size.width / 2f, size.height / 2f), size.width * 0.35f)
                PaletteNodeType.UPS -> drawUpsShape(textMeasurer, centerOffset, Size(w, h))
                PaletteNodeType.BATTERY -> drawBatteryShape(centerOffset, Size(w, h))
                PaletteNodeType.SOLAR_PANEL -> drawSolarPanelShape(centerOffset, Size(w, h))
                PaletteNodeType.INVERTER -> drawInverterShape(textMeasurer, centerOffset, Size(w, h))
            }
        }
        Text(text = label, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
