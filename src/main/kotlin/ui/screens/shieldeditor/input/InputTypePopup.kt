package ui.screens.shieldeditor.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.dialogs.AtsDialogState
import kotlin.math.roundToInt

// Перечисление типов ввода
enum class InputType(val title: String, val needsBreakerConfig: Boolean) {
    TWO_INPUTS_ATS_BREAKERS("Два ввода на общую шину с АВР на автоматических выключателях с моторприводом", false),
    TWO_INPUTS_ATS_CONTACTORS("Два ввода на общую шину с АВР на контакторах с моторприводом", false),
    ONE_INPUT_SWITCH("Один ввод с выключателем нагрузки", false),
    ONE_INPUT_BREAKER("Один ввод с автоматическим выключателем", true),
    ATS_BLOCK_TWO_INPUTS("Блок АВР на два ввода на общую шину", false);
}

private data class BreakerDialogState(
    val manufacturer: String? = null,
    val series: String? = null,
    val selectedAdditions: List<String> = emptyList(),
    val selectedPoles: String? = null,
    val selectedCurve: String? = null
)

@Composable
fun InputTypePopup(
    data: ShieldData,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    // Начальная позиция
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(100f) }

    var atsStep by remember { mutableStateOf(0) }
    // Размеры окна (как в ProtectionSelectionWindow)
    var currentWidth by remember { mutableStateOf(950.dp) }
    var currentHeight by remember { mutableStateOf(650.dp) } // Было 600, стало 650

    val density = LocalDensity.current
    var selectedType by remember { mutableStateOf(InputType.values().first()) }

    // --- Состояния логики переходов ---
    var showInputParamsWindow by remember { mutableStateOf(false) }
    var showInputBreakerSecond by remember { mutableStateOf(false) }
    var showInputBreakerThird by remember { mutableStateOf(false) }
    var showAtsSecondWindow by remember { mutableStateOf(false) }
    var showAtsThirdWindow by remember { mutableStateOf(false) }

    val inputBreakerState = remember { mutableStateOf(BreakerDialogState()) }
    val atsState = remember { mutableStateOf(AtsDialogState()) }

    fun proceedToConfiguration() {
        val type = selectedType
        if (type == InputType.ONE_INPUT_BREAKER) {
            showInputParamsWindow = true
        } else if (type == InputType.TWO_INPUTS_ATS_BREAKERS || type.needsBreakerConfig) {
            inputBreakerState.value = BreakerDialogState(manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() })
            showInputBreakerSecond = true
        } else {
            data.inputInfo = type.title
            onSave()
            onDismissRequest()
        }
    }

    // Основной контейнер окна
    Box(
        modifier = Modifier
            .zIndex(10f)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(currentWidth, currentHeight)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface)
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- Заголовок (Draggable Area) ---
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
                    text = "Тип вводного устройства",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                IconButton(onClick = onDismissRequest, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))

            Row(modifier = Modifier.weight(1f)) {
                // --- Левая панель (Sidebar) ---
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.05f))
                        .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Доступные варианты",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(InputType.entries.toTypedArray()) { type ->
                            SidebarItem(
                                text = type.title,
                                isSelected = type == selectedType,
                                onClick = {
                                    selectedType = type
                                    // АВТОМАТИЧЕСКИЙ ПЕРЕХОД: Если выбран АВР, сразу ставим шаг 1
                                    if (type == InputType.ATS_BLOCK_TWO_INPUTS) {
                                        atsState.value = AtsDialogState(
                                            manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                        )
                                        atsStep = 1
                                    } else {
                                        atsStep = 0
                                    }
                                }
                            )
                        }
                    }
                }

                // --- Правая панель (Main Content) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    // Ветвление логики отображения
                    if (selectedType == InputType.ATS_BLOCK_TWO_INPUTS && atsStep > 0) {
                        // --- ВСТРОЕННЫЙ ИНТЕРФЕЙС АВР ---
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Заголовок шага с кнопкой Назад (если шаг 2)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (atsStep == 2) {
                                    IconButton(onClick = { atsStep = 1 }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                    }
                                }
                                Text(
                                    text = if (atsStep == 1) "Параметры АВР" else "Выбор устройства",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                            // Контент шагов
                            if (atsStep == 1) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AtsSecondWindow(
                                        initialManufacturer = atsState.value.manufacturer,
                                        initialSeries = atsState.value.series,
                                        initialSelectedPoles = atsState.value.selectedPoles,
                                        consumerVoltageStr = "400",
                                        onParamsChanged = { man, ser, pol ->
                                            atsState.value = atsState.value.copy(
                                                manufacturer = man,
                                                series = ser,
                                                selectedPoles = pol
                                            )
                                        }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { atsStep = 2 },
                                        enabled = !atsState.value.series.isNullOrBlank() && !atsState.value.selectedPoles.isNullOrBlank()
                                    ) {
                                        Text("Далее")
                                    }
                                }
                            } else {
                                // ШАГ 2: AtsThirdWindow (список)
                                AtsThirdWindow(
                                    maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                    consumerCurrentAStr = data.totalCurrent,
                                    selectedSeries = atsState.value.series,
                                    selectedPoles = atsState.value.selectedPoles,
                                    onChoose = { resultStr ->
                                        val fullText = selectedType.title + "\n---------------------------------\n" + resultStr
                                        data.inputInfo = fullText
                                        onSave()
                                        onDismissRequest()
                                    }
                                )
                            }
                        }

                    } else {
                        // --- СТАНДАРТНЫЙ ИНТЕРФЕЙС (для остальных типов) ---
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = selectedType.title,
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Divider(modifier = Modifier.padding(bottom = 16.dp))
                                Text(
                                    text = "Нажмите кнопку «Настроить» для ввода параметров выбранного типа ввода.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.body1
                                )
                            }
                            Button(
                                onClick = { proceedToConfiguration() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Настроить")
                            }
                        }
                    }
                }
            }
        }

        // --- Resize Handle (без изменений) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newW = (currentWidth + dragAmount.x.toDp()).coerceAtLeast(700.dp)
                        val newH = (currentHeight + dragAmount.y.toDp()).coerceAtLeast(500.dp)
                        currentWidth = newW
                        currentHeight = newH
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val color = Color.Gray.copy(alpha = 0.5f)
                val w = size.width
                val h = size.height
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), // Отступы уменьшены под стиль caption
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 3,
                fontSize = 13.sp
            )
        }
    }
}