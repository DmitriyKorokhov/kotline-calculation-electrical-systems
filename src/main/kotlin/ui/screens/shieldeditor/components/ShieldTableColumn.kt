package ui.screens.shieldeditor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.database.CableCurrentRatings
import org.jetbrains.exposed.sql.transactions.transaction
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import view.CompactOutlinedTextField
import ui.screens.shieldeditor.dialogs.CableTypePopup
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.and
import androidx.compose.ui.focus.onFocusChanged

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
    onOpenProtectionDialog: () -> Unit,
    data: ShieldData,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int
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
                HistoryAwareTextField(
                    label = "Наим. потребителя",
                    value = consumer.name,
                    onValueChange = { consumer.name = it; onDataChanged() },
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
                Spacer(Modifier.height(FIELDVSPACE))

                // Помещение
                HistoryAwareTextField(
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
                HistoryAwareTextField(
                    label = "Напряжение, В",
                    value = consumer.voltage,
                    onValueChange = { consumer.voltage = it; onCalculationRequired() },
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

                // Cos Phi
                HistoryAwareTextField(
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
                    HistoryAwareTextField(
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
                HistoryAwareTextField(
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
                HistoryAwareTextField(
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
                HistoryAwareTextField(
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
                HistoryAwareTextField(
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

            // --- БЛОК 3: Защита и Кабель (Lavender) ---
            BlockPanel(color = BLOCKLAVENDER) {
                // Номер автомата
                HistoryAwareTextField(
                    label = "Номер защиты",
                    value = consumer.breakerNumber,
                    onValueChange = { consumer.breakerNumber = it; onDataChanged() },
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

                // Устройство защиты (с иконкой выбора)
                Box(modifier = Modifier.fillMaxWidth()) {
                    HistoryAwareTextField(
                        label = "Устройство защиты",
                        value = consumer.protectionDevice,
                        onValueChange = { consumer.protectionDevice = it; onCalculationRequired() },
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
                            .clickable { onOpenProtectionDialog() }
                    )
                }
            }

            Spacer(Modifier.height(FIELDVSPACE))

            BlockPanel(color = BLOCKWHITE) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    HistoryAwareTextField(
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
                        HistoryAwareTextField(
                            label = "Способ прокладки",
                            value = consumer.layingMethod.ifBlank { "Воздух" }, // Значение по умолчанию
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
                    HistoryAwareTextField(
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
                    HistoryAwareTextField(
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
                HistoryAwareTextField(
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
                HistoryAwareTextField(
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
private fun HistoryAwareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onPushHistory: () -> Unit,
    historyTrigger: Int,
    label: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    fontSizeSp: Int,
    textColor: Color,
    focusedBorderColor: Color,
    unfocusedBorderColor: Color,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    var historySaved by remember { mutableStateOf(false) }

    LaunchedEffect(historyTrigger) {
        historySaved = false
    }

    CompactOutlinedTextField(
        label = label,
        value = value,
        onValueChange = { newValue ->
            if (!historySaved) {
                onPushHistory()
                historySaved = true
            }
            onValueChange(newValue)
        },
        contentPadding = contentPadding,
        fontSizeSp = fontSizeSp,
        textColor = textColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                historySaved = false
            }
        }
    )
}

