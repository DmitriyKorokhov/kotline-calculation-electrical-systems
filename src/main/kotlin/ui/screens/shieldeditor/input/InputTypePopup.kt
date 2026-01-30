package ui.screens.shieldeditor.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import ui.screens.shieldeditor.ShieldData
import java.awt.Cursor
import kotlin.math.roundToInt

// --- Enum Definition ---
enum class InputType(val title: String, val needsBreakerConfig: Boolean) {
    TWO_INPUTS_ATS_BREAKERS("Два ввода на общую шину с АВР на автоматических выключателях с моторприводом", false),
    TWO_INPUTS_ATS_CONTACTORS("Два ввода на общую шину с АВР на контакторах с моторприводом", false),
    ONE_INPUT_SWITCH("N-вводов с выключателями нагрузки", false),
    ONE_INPUT_BREAKER("N-вводов с автоматическими выключателями", true),
    ATS_BLOCK_TWO_INPUTS("Блок АВР на два ввода на общую шину", false);
}

// --- State Holders ---
private data class AtsBreakerState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedAdditions: List<String> = emptyList(),
    val selectedPoles: String? = null,
    val selectedCurve: String? = null
)

private data class AtsBlockState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedPoles: String? = null
)

@Composable
fun InputTypePopup(
    data: ShieldData,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val density = LocalDensity.current

    // Window Position & Size State
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(100f) }
    var widthDp by remember { mutableStateOf(950.dp) }
    var heightDp by remember { mutableStateOf(650.dp) }

    val minWidth = 700.dp
    val minHeight = 500.dp
    val resizeHandleSize = 8.dp

    var selectedType by remember { mutableStateOf(InputType.TWO_INPUTS_ATS_BREAKERS) }

    // --- Workflow States ---

    // 1. Для АВР на автоматах
    var atsBreakerStep by remember { mutableStateOf(1) }
    var atsBreakerParams by remember {
        mutableStateOf(AtsBreakerState(manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }))
    }

    // 2. Для блочного АВР
    var atsBlockStep by remember { mutableStateOf(1) }
    var atsBlockParams by remember {
        mutableStateOf(AtsBlockState(manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }))
    }

    // Сброс шагов при смене типа
    LaunchedEffect(selectedType) {
        atsBreakerStep = 1
        atsBlockStep = 1
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Header (Draggable) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Выбор вводного устройства",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // --- Sidebar (Left) ---
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.05f))
                        .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Тип вводного устройства",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )

                    InputType.entries.forEach { type ->
                        SidebarItem(
                            text = type.title,
                            isSelected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                    }
                }

                // --- Content (Right) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    when (selectedType) {
                        // === СЦЕНАРИЙ 1: АВР НА АВТОМАТАХ ===
                        InputType.TWO_INPUTS_ATS_BREAKERS -> {
                            if (atsBreakerStep == 1) {
                                AtsBreakerSecondWindow(
                                    initialManufacturer = atsBreakerParams.manufacturer,
                                    initialSeries = atsBreakerParams.series,
                                    initialSelectedAdditions = atsBreakerParams.selectedAdditions,
                                    initialSelectedPoles = atsBreakerParams.selectedPoles,
                                    initialSelectedCurve = atsBreakerParams.selectedCurve,
                                    onBack = { /* Нет шага назад, это первый шаг */ },
                                    onDismiss = onDismissRequest,
                                    onConfirm = { res ->
                                        atsBreakerParams = atsBreakerParams.copy(
                                            series = res.series,
                                            selectedAdditions = res.selectedAdditions,
                                            selectedPoles = res.selectedPoles,
                                            selectedCurve = res.selectedCurve
                                        )
                                        atsBreakerStep = 2
                                    }
                                )
                            } else {
                                AtsBreakerThirdWindow(
                                    maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                    standard = data.protectionStandard,
                                    selectedSeries = atsBreakerParams.series,
                                    selectedPoles = atsBreakerParams.selectedPoles,
                                    selectedAdditions = atsBreakerParams.selectedAdditions,
                                    selectedCurve = atsBreakerParams.selectedCurve,
                                    onBack = { atsBreakerStep = 1 },
                                    onDismiss = onDismissRequest,
                                    onChoose = { resultString ->
                                        data.inputInfo = "${InputType.TWO_INPUTS_ATS_BREAKERS.title}\n$resultString"
                                        onSave()
                                        onDismissRequest()
                                    }
                                )
                            }
                        }

                        // === СЦЕНАРИЙ 2: БЛОЧНЫЙ АВР ===
                        InputType.ATS_BLOCK_TWO_INPUTS -> {
                            if (atsBlockStep == 1) {
                                AtsSecondWindow(
                                    initialManufacturer = atsBlockParams.manufacturer,
                                    initialSeries = atsBlockParams.series,
                                    initialSelectedPoles = atsBlockParams.selectedPoles,
                                    consumerVoltageStr = "400", // По умолчанию для ввода
                                    onParamsChanged = { man, ser, pol ->
                                        atsBlockParams = atsBlockParams.copy(
                                            manufacturer = man,
                                            series = ser,
                                            selectedPoles = pol
                                        )
                                    }
                                )
                                // Кнопка "Далее" для AtsSecondWindow, так как он может не иметь встроенной кнопки
                                Box(Modifier.fillMaxSize()) {
                                    Button(
                                        onClick = { atsBlockStep = 2 },
                                        enabled = !atsBlockParams.series.isNullOrBlank() && !atsBlockParams.selectedPoles.isNullOrBlank(),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Text("Далее")
                                    }
                                }
                            } else {
                                AtsThirdWindow(
                                    maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                    consumerCurrentAStr = data.totalCurrent,
                                    selectedSeries = atsBlockParams.series,
                                    selectedPoles = atsBlockParams.selectedPoles,
                                    onBack = { atsBlockStep = 1 },
                                    onChoose = { resultStr ->
                                        val fullText = "${InputType.ATS_BLOCK_TWO_INPUTS.title}\n---------------------------------\n$resultStr"
                                        data.inputInfo = fullText
                                        onSave()
                                        onDismissRequest()
                                    }
                                )
                                // Кнопка Назад для AtsThirdWindow, если она не встроенна
                                Box(Modifier.fillMaxSize()) {
                                    IconButton(
                                        onClick = { atsBlockStep = 1 },
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        // Обычно иконка назад внутри окна, но если нет — можно добавить тут
                                        // Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                                    }
                                }
                            }
                        }

                        // === ЗАГЛУШКИ ДЛЯ ОСТАЛЬНЫХ ===
                        else -> {
                            StandardInputPlaceholder(
                                type = selectedType,
                                onConfigure = {
                                    if (selectedType == InputType.ONE_INPUT_BREAKER) {
                                        // Здесь можно открыть простое окно параметров, если нужно
                                        // showInputParamsWindow = true (логику можно добавить по требованию)
                                    } else {
                                        // Просто сохраняем название
                                        data.inputInfo = selectedType.title
                                        onSave()
                                        onDismissRequest()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).size(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp) // Увеличенный размер для удобного захвата и отрисовки
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthDp = max(widthDp + with(density) { dragAmount.x.toDp() }, minWidth)
                        heightDp = max(heightDp + with(density) { dragAmount.y.toDp() }, minHeight)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val color = Color.Gray.copy(alpha = 0.5f)
                val w = size.width
                val h = size.height
                // Рисуем 3 диагональные линии
                drawLine(color, Offset(w, h - 4), Offset(w - 4, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 8), Offset(w - 8, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 12), Offset(w - 12, h), strokeWidth = 2f)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StandardInputPlaceholder(
    type: InputType,
    onConfigure: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = type.title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Divider(modifier = Modifier.padding(bottom = 16.dp))
            Text(
                text = "Для данного типа ввода нажмите кнопку «Настроить» (или «Выбрать»), чтобы применить параметры по умолчанию или ввести дополнительные данные.",
                color = Color.Gray,
                style = MaterialTheme.typography.body1
            )
        }
        Button(
            onClick = onConfigure,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Настроить / Выбрать")
        }
    }
}