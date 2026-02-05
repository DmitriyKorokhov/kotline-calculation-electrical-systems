package ui.screens.shieldeditor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import data.database.ConsumerLibrary
import data.database.CableCurrentRatings
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ui.screens.shieldeditor.AdditionalProtection
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.dialogs.CableTypePopup
import ui.utils.HistoryAwareCompactTextField

private val COLUMNWIDTH: Dp = 220.dp
private val COLUMNOUTERPADDING: Dp = 4.dp
private val COLUMNINNERPADDING: Dp = 8.dp
private val HEADERHEIGHT: Dp = 24.dp
private const val HEADERFONT = 13
private const val FIELDFONT = 15
private val FIELDCONTENTPADDING = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val FIELDVSPACE: Dp = 8.dp
private val BLOCKBLUE = Color(0xFFE3F2FD).copy(alpha = 0.15f)
private val BLOCKLAVENDER = Color(0xFFEDE7F6).copy(alpha = 0.15f)
private val BLOCKWHITE = Color.White.copy(alpha = 0.15f)
private val BLOCKBORDER = Color(0xFFB0BEC5)

@Composable
fun ShieldTableColumn(
    index: Int,
    consumer: ConsumerModel,
    isSelected: Boolean,
    borderColor: Color,
    textColor: Color,
    isPasteEnabled: Boolean,
    isAddEnabled: Boolean,
    onContextAction: (ContextMenuAction) -> Unit,
    onHeaderClick: () -> Unit,
    onDataChanged: () -> Unit,
    onCalculationRequired: () -> Unit,
    onRecalculateDropOnly: () -> Unit,
    onOpenProtectionDialog:(Int) -> Unit,
    data: ShieldData,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int,
) {
    // --- Анимации выделения ---
    val targetBg = if (isSelected) Color(0xFF1976D2) else Color.Transparent
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(durationMillis = 260))

    val targetTextColor = if (isSelected) Color.White else textColor
    val animatedTextColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(durationMillis = 260))
    // Состояние для меню ВНУТРИ колонки
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }

    var showCableMenu by remember { mutableStateOf(false) }

    val targetScale = if (isSelected) 1.02f else 1f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = 400f)
    )

    val pInst = consumer.installedPowerW.toDoubleOrNull() ?: 0.0
    val pCalc = consumer.powerKw.toDoubleOrNull() ?: 0.0
    // Ошибка, если Установленная строго меньше Расчетной
    val isPowerError = pInst < pCalc

    // Определяем цвета на основе ошибки
    val currentUnfocusedBorder = if (isPowerError) Color.Red else Color.LightGray
    val currentFocusedBorder = if (isPowerError) Color.Red else borderColor

    Box(
        modifier = Modifier
            .width(COLUMNWIDTH)
            .padding(COLUMNOUTERPADDING)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(COLUMNINNERPADDING)) {
            // --- ЗАГОЛОВОК (Header) ---
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(HEADERHEIGHT)
                    .fillMaxWidth()
                    .scale(animatedScale)
                    .background(animatedBg, RoundedCornerShape(6.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .pointerInput(index) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                if (event.buttons.isSecondaryPressed) {
                                    menuOffset = event.changes.first().position
                                    showMenu = true
                                } else {
                                    onHeaderClick()
                                }
                            }
                        }
                    }
            ) {
                Text(
                    text = "Потребитель ${index + 1}",
                    fontSize = HEADERFONT.sp,
                    color = animatedTextColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
                if (showMenu) {
                    ConsumerContextMenu(
                        expanded = true,
                        offset = IntOffset(menuOffset.x.toInt(), menuOffset.y.toInt()),
                        onDismissRequest = { showMenu = false },
                        isPasteEnabled = isPasteEnabled,
                        isAddEnabled = isAddEnabled,
                        onAction = { action ->
                            showMenu = false
                            onContextAction(action)
                        }
                    )
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                color = borderColor
            )

            Spacer(Modifier.height(8.dp))

            // --- БЛОК 1: Входные параметры (Blue) ---
            BlockPanel(color = BLOCKBLUE) {
                // Название
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expanded by remember { mutableStateOf(false) }

                    val filteredOptions = remember(consumer.name) {
                        ConsumerLibrary.search(consumer.name)
                    }

                    HistoryAwareCompactTextField(
                        label = "Наим. потребителя",
                        value = consumer.name,
                        onValueChange = { newValue ->
                            consumer.name = newValue
                            onDataChanged()
                            expanded = true
                        },
                        onPushHistory = { onPushHistory(false) },
                        historyTrigger = historyTrigger,
                        contentPadding = FIELDCONTENTPADDING,
                        fontSizeSp = FIELDFONT,
                        textColor = textColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = Color.LightGray,
                        singleLine = false, minLines = 1, maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) expanded = false
                            }
                    )

                    if (expanded && filteredOptions.isNotEmpty()) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },

                            properties = PopupProperties(focusable = false),
                            modifier = Modifier
                                .width(COLUMNWIDTH - COLUMNOUTERPADDING * 2 - 16.dp)
                                .requiredHeightIn(max = 200.dp)
                                .background(Color.White)
                        ) {
                            filteredOptions.forEach { definition ->
                                DropdownMenuItem(
                                    onClick = {
                                        onPushHistory(true)
                                        consumer.name = definition.name
                                        onDataChanged()
                                        expanded = false
                                    }
                                ) {
                                    Text(
                                        text = definition.name,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(FIELDVSPACE))

                // Помещение
                HistoryAwareCompactTextField(
                    label = "Номер помещения",
                    value = consumer.roomNumber,
                    onValueChange = { consumer.roomNumber = it; onDataChanged() },
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Напряжение
                // --- Напряжение (Выбор: 230 или 400) ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    var voltageMenuExpanded by remember { mutableStateOf(false) }
                    val voltageOptions = listOf("230", "400")

                    // Используем текстовое поле для отображения, но блокируем ручной ввод
                    // Перекрываем его прозрачным Box с clickable, чтобы клик открывал меню
                    Box {
                        HistoryAwareCompactTextField(
                            label = "Напряжение, В",
                            value = consumer.voltage,
                            onValueChange = { }, // Игнорируем прямой ввод
                            onPushHistory = { }, // История обрабатывается в меню
                            historyTrigger = historyTrigger,
                            contentPadding = FIELDCONTENTPADDING,
                            fontSizeSp = FIELDFONT,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Прозрачная "крышка", которая ловит клики
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { voltageMenuExpanded = true }
                        )
                    }

                    // Иконка стрелочки
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать напряжение",
                        tint = textColor,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clickable { voltageMenuExpanded = true }
                    )

                    // Выпадающее меню
                    DropdownMenu(
                        expanded = voltageMenuExpanded,
                        onDismissRequest = { voltageMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        voltageOptions.forEach { option ->
                            DropdownMenuItem(
                                onClick = {
                                    if (consumer.voltage != option) {
                                        onPushHistory(true)
                                        consumer.voltage = option
                                        onCalculationRequired()
                                    }
                                    voltageMenuExpanded = false
                                }
                            ) {
                                Text(text = option, color = Color.Black)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(FIELDVSPACE))

                // Cos Phi
                HistoryAwareCompactTextField(
                    label = "cos(ϕ)",
                    value = consumer.cosPhi,
                    onValueChange = { consumer.cosPhi = it; onCalculationRequired() },
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Установленная мощность
                Column(modifier = Modifier.fillMaxWidth()) {
                    HistoryAwareCompactTextField(
                        label = "Установ. мощность, кВт",
                        value = consumer.installedPowerW,
                        onValueChange = {
                            consumer.installedPowerW = it
                            onDataChanged()
                        },
                        onPushHistory = { onPushHistory(false) },
                        historyTrigger = historyTrigger,
                        contentPadding = FIELDCONTENTPADDING,
                        fontSizeSp = FIELDFONT,
                        textColor = textColor,
                        focusedBorderColor = currentFocusedBorder,
                        unfocusedBorderColor = currentUnfocusedBorder,
                        singleLine = false, minLines = 1, maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPowerError) {
                        Text(
                            text = "Внимание: Pуст < Pрасч",
                            color = textColor,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(FIELDVSPACE))

                // Расчетная мощность
                HistoryAwareCompactTextField(
                    label = "Расчетная мощность, кВт",
                    value = consumer.powerKw,
                    onValueChange = { consumer.powerKw = it; onCalculationRequired() },
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))
            }

            Spacer(Modifier.height(8.dp))

            // --- БЛОК 2: Результаты расчета (White) ---
            BlockPanel(color = BLOCKWHITE) {
                // Ток
                HistoryAwareCompactTextField(
                    label = "Расчетный ток, А",
                    value = consumer.currentA,
                    onValueChange = {},
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Номер фазы
                HistoryAwareCompactTextField(
                    label = "Номер фазы",
                    value = consumer.phaseNumber,
                    onValueChange = { consumer.phaseNumber = it; onDataChanged() },
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Номер группы
                HistoryAwareCompactTextField(
                    label = "Номер группы",
                    value = consumer.lineName,
                    onValueChange = { consumer.lineName = it; onDataChanged() },
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    singleLine = false, minLines = 1, maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- БЛОК 3: Защита ---
            BlockPanel(color = BLOCKLAVENDER) {
                // Основная защита
                ProtectionSubBlock(
                    labelSuffix = "", // Без суффикса для первой
                    breakerNumVal = consumer.breakerNumber,
                    onBreakerNumChange = { consumer.breakerNumber = it; onDataChanged() },
                    deviceVal = consumer.protectionDevice,
                    onDeviceChange = { consumer.protectionDevice = it; onCalculationRequired() },
                    onOpenDialog = { onOpenProtectionDialog(-1) }, // Открывает диалог для основной
                    onPushHistory = onPushHistory,
                    historyTrigger = historyTrigger,
                    textColor = textColor,
                    borderColor = borderColor
                )

                // Дополнительные защиты (Цикл)
                consumer.additionalProtections.forEachIndexed { index, addProt ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Кнопка УДАЛЕНИЯ (-)
                        OutlinedButton(
                            onClick = {
                                onPushHistory(true)
                                consumer.additionalProtections.removeAt(index)
                                onDataChanged()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Red.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Удалить защиту",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // --- ПОЛЯ ВВОДА ---
                        ProtectionSubBlock(
                            labelSuffix = "",
                            breakerNumVal = addProt.breakerNumber,
                            onBreakerNumChange = { addProt.breakerNumber = it; onDataChanged() },
                            deviceVal = addProt.protectionDevice,
                            onDeviceChange = { addProt.protectionDevice = it; onCalculationRequired() },
                            onOpenDialog = { onOpenProtectionDialog(index) },
                            onPushHistory = onPushHistory,
                            historyTrigger = historyTrigger,
                            textColor = textColor,
                            borderColor = borderColor
                        )
                    }
                }
                // Кнопка Добавить (+)
                if (consumer.additionalProtections.size < 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onPushHistory(true)
                            consumer.additionalProtections.add(AdditionalProtection())
                            onDataChanged()
                        },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Protection",
                            tint = textColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(FIELDVSPACE))

            BlockPanel(color = BLOCKWHITE) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    HistoryAwareCompactTextField(
                        label = "Марка кабеля",
                        value = consumer.cableType,
                        onValueChange = { consumer.cableType = it; onDataChanged() },
                        onPushHistory = { onPushHistory(false) },
                        historyTrigger = historyTrigger,
                        contentPadding = FIELDCONTENTPADDING,
                        fontSizeSp = FIELDFONT,
                        textColor = textColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = Color.LightGray,
                        singleLine = false, minLines = 1, maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clickable { showCableMenu = true }
                    )

                    if (showCableMenu) {
                        CableTypePopup(
                            onDismissRequest = { showCableMenu = false },
                            onConfirm = { selectedType ->
                                onPushHistory(true)
                                consumer.cableType = selectedType
                                onCalculationRequired()
                                showCableMenu = false
                            },
                            targetMaterial = if (data.cableMaterial == "Copper") "Copper" else "Aluminum",
                            targetInsulation = data.cableInsulation,
                            isFlexible = data.cableIsFlexible
                        )
                    }
                }

                Spacer(Modifier.height(FIELDVSPACE))

                // --- Ячейка 13: Способ прокладки и Длина ---
                BlockPanel(color = BLOCKWHITE) { // Можно выделить отдельным цветом
                    // 1. Выбор способа (Dropdown)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HistoryAwareCompactTextField(
                            label = "Способ прокладки",
                            value = consumer.layingMethod,
                            onValueChange = {},
                            onPushHistory = { onPushHistory(false) },
                            historyTrigger = historyTrigger,
                            contentPadding = FIELDCONTENTPADDING,
                            fontSizeSp = FIELDFONT,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { }
                        )

                        // Меню выбора
                        var methodMenuExpanded by remember { mutableStateOf(false) }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Выбрать",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(24.dp)
                                .clickable { methodMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = methodMenuExpanded,
                            onDismissRequest = { methodMenuExpanded = false }
                        ) {
                            DropdownMenuItem(onClick = {
                                onPushHistory(true)
                                consumer.layingMethod = "Воздух"
                                methodMenuExpanded = false
                                onCalculationRequired()
                            }) { Text("Воздух") }

                            DropdownMenuItem(onClick = {
                                onPushHistory(true)
                                consumer.layingMethod = "Земля"
                                methodMenuExpanded = false
                                onCalculationRequired()
                            }) { Text("Земля") }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 2. Ввод длины
                    HistoryAwareCompactTextField(
                        label = "Длина, м",
                        value = consumer.cableLength,
                        onValueChange = { consumer.cableLength = it; onCalculationRequired() },
                        onPushHistory = { onPushHistory(false) },
                        historyTrigger = historyTrigger,
                        contentPadding = FIELDCONTENTPADDING,
                        fontSizeSp = FIELDFONT,
                        textColor = textColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = Color.LightGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(FIELDVSPACE))

                // --- Ячейка 14: Число жил, сечение (Авторасчет) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryAwareCompactTextField(
                        label = "Число жил, сечение",
                        value = consumer.cableLine,
                        onValueChange = {},
                        onPushHistory = { onPushHistory(false) },
                        historyTrigger = historyTrigger,
                        contentPadding = FIELDCONTENTPADDING,
                        fontSizeSp = FIELDFONT,
                        textColor = textColor,
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = Color.LightGray,
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Gray.copy(alpha = 0.05f))
                    )

                    Spacer(Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("▲", Modifier.clickable {
                            onPushHistory(true)
                            changeCableSection(consumer, data, +1)
                            onRecalculateDropOnly()
                        })

                        Text("▼", Modifier.clickable {
                            onPushHistory(true)
                            changeCableSection(consumer, data, -1)
                            onRecalculateDropOnly()
                        })
                    }
                }

                Spacer(Modifier.height(FIELDVSPACE))

                // Падение напряжения
                HistoryAwareCompactTextField(
                    label = "Падение напряжения, В",
                    value = consumer.voltageDropV,
                    onValueChange = {},
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(FIELDVSPACE))

                // Ток КЗ в конце линии
                HistoryAwareCompactTextField(
                    label = "Ток КЗ в конце КЛ, кА",
                    value = consumer.shortCircuitCurrentkA,
                    onValueChange = {},
                    onPushHistory = { onPushHistory(false) },
                    historyTrigger = historyTrigger,
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val FALLBACK_SECTIONS = listOf(
    1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0
)

private fun changeCableSection(consumer: ConsumerModel, data: ShieldData, direction: Int) {
    // 1. Пытаемся получить список из БД
    var sections = try {
        val cableType = consumer.cableType
        if (cableType.isNotBlank()) {
            val isAluminum = cableType.startsWith("А", ignoreCase = true)
            val materialCode = if (isAluminum) "Al" else "Cu"
            val typeWithoutConductor = if (isAluminum) cableType.substring(1) else cableType
            val insulationCode = when {
                typeWithoutConductor.startsWith("Пв", ignoreCase = true) -> "XLPE"
                typeWithoutConductor.startsWith("В", ignoreCase = true) ||
                        typeWithoutConductor.startsWith("П", ignoreCase = true) -> "PVC"
                else -> "PVC"
            }

            val dbSections = mutableListOf<Double>()
            transaction {
                CableCurrentRatings
                    .selectAll()
                    .where {
                        (CableCurrentRatings.material eq materialCode) and
                                (CableCurrentRatings.insulation eq insulationCode)
                    }
                    .forEach { row ->
                        dbSections += row[CableCurrentRatings.crossSection].toDouble()
                    }
            }
            dbSections.distinct().sorted()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    // 2. Если БД вернула пустоту (или ошибка) — используем стандартный список
    if (sections.isEmpty()) {
        sections = FALLBACK_SECTIONS
    }

    // 3. Парсим текущее значение (поддержка x, х, X, Х)
    val regex = Regex("""[xхXХ]\s*(\d+[.,]?\d*)""")
    val match = regex.find(consumer.cableLine)
    val current = match?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()

    // 4. Находим индекс (ближайший)
    val currentIndex = if (current == null) {
        -1
    } else {
        sections.indices.minByOrNull { i -> kotlin.math.abs(sections[i] - current) } ?: -1
    }

    // 5. Вычисляем новый индекс
    val newIndex = if (currentIndex == -1) {
        0 // Если не нашли — ставим первое
    } else {
        (currentIndex + direction).coerceIn(0, sections.lastIndex)
    }

    // Если пытаемся уйти за границы и уже там — выходим
    if (currentIndex != -1 && newIndex == currentIndex) return

    // 6. Формируем строку с учетом типа структуры
    val newSection = sections[newIndex]
    val cores = if ((consumer.voltage.toIntOrNull() ?: 230) >= 380) 5 else 3
    val sectionStr = if (newSection % 1.0 == 0.0) newSection.toInt().toString() else newSection.toString()

    // ПРОВЕРКА: Нужен ли формат одножильного кабеля (SingleCore)?
    val inputLength = consumer.cableLength.replace(",", ".").toDoubleOrNull() ?: 0.0
    val threshold = data.singleCoreThreshold.toDoubleOrNull() ?: 30.0

    // Если длина больше порога — используем формат с (1xS)
    if (inputLength > threshold) {
        consumer.cableLine = "${cores}x(1x$sectionStr)"
    } else {
        consumer.cableLine = "${cores}x$sectionStr"
    }
}

@Composable
private fun BlockPanel(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .border(1.dp, BLOCKBORDER, RoundedCornerShape(6.dp))
            .padding(8.dp),
        content = content
    )
}

@Composable
private fun ProtectionSubBlock(
    labelSuffix: String,
    breakerNumVal: String,
    onBreakerNumChange: (String) -> Unit,
    deviceVal: String,
    onDeviceChange: (String) -> Unit,
    onOpenDialog: () -> Unit,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int,
    textColor: Color,
    borderColor: Color
) {
    Column {
        // Номер автомата
        HistoryAwareCompactTextField(
            label = "Номер аппарата защиты$labelSuffix",
            value = breakerNumVal,
            onValueChange = onBreakerNumChange,
            onPushHistory = { onPushHistory(false) },
            historyTrigger = historyTrigger,
            contentPadding = FIELDCONTENTPADDING,
            fontSizeSp = FIELDFONT,
            textColor = textColor,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = Color.LightGray,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(FIELDVSPACE))

        // Устройство защиты
        Box(modifier = Modifier.fillMaxWidth()) {
            HistoryAwareCompactTextField(
                label = "Устройство защиты$labelSuffix",
                value = deviceVal,
                onValueChange = onDeviceChange,
                onPushHistory = { onPushHistory(false) },
                historyTrigger = historyTrigger,
                contentPadding = FIELDCONTENTPADDING,
                fontSizeSp = FIELDFONT,
                textColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.LightGray,
                singleLine = false, minLines = 1, maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Выбрать",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(24.dp)
                    .clickable { onOpenDialog() },
                tint = textColor
            )
        }
    }
}
