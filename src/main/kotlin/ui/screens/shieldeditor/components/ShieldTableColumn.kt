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
    data: ShieldData
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
                CompactOutlinedTextField(
                    label = "Наим. потребителя",
                    value = consumer.name,
                    onValueChange = { consumer.name = it; onDataChanged() },
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
                CompactOutlinedTextField(
                    label = "№ Помещения",
                    value = consumer.roomNumber,
                    onValueChange = { consumer.roomNumber = it; onDataChanged() },
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
                CompactOutlinedTextField(
                    label = "Напряжение, В",
                    value = consumer.voltage,
                    onValueChange = { consumer.voltage = it; onCalculationRequired() },
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Cos Phi
                CompactOutlinedTextField(
                    label = "cos(ϕ)",
                    value = consumer.cosPhi,
                    onValueChange = { consumer.cosPhi = it; onCalculationRequired() },
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Установленная мощность
                CompactOutlinedTextField(
                    label = "Установ. мощность, Вт",
                    value = consumer.installedPowerW,
                    onValueChange = { consumer.installedPowerW = it; onCalculationRequired() }, // Обычно влияет на итоги
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Расчетная мощность
                CompactOutlinedTextField(
                    label = "Расчетная мощность, Вт",
                    value = consumer.powerKw,
                    onValueChange = { consumer.powerKw = it; onCalculationRequired() },
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
                // Ток (Read-only)
                CompactOutlinedTextField(
                    label = "Расчетный ток, А",
                    value = consumer.currentA,
                    onValueChange = {},
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
                CompactOutlinedTextField(
                    label = "Номер фазы",
                    value = consumer.phaseNumber,
                    onValueChange = { consumer.phaseNumber = it; onDataChanged() },
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FIELDVSPACE))

                // Номер группы
                CompactOutlinedTextField(
                    label = "Номер группы",
                    value = consumer.lineName,
                    onValueChange = { consumer.lineName = it; onDataChanged() },
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
                CompactOutlinedTextField(
                    label = "Номер автомата",
                    value = consumer.breakerNumber,
                    onValueChange = { consumer.breakerNumber = it; onDataChanged() },
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
                    CompactOutlinedTextField(
                        label = "Устройство защиты",
                        value = consumer.protectionDevice,
                        onValueChange = { consumer.protectionDevice = it; onCalculationRequired() },
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
                    CompactOutlinedTextField(
                        label = "Марка кабеля",
                        value = consumer.cableType,
                        onValueChange = { consumer.cableType = it; onDataChanged() },
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
                                consumer.cableType = selectedType
                                onCalculationRequired()
                                showCableMenu = false
                            },
                            targetMaterial = if (data.cableMaterial == "Copper") "Copper" else "Aluminum",
                            targetInsulation = data.cableInsulation
                        )
                    }
                }

                Spacer(Modifier.height(FIELDVSPACE))

                // --- Ячейка 13: Способ прокладки и Длина ---
                BlockPanel(color = BLOCKWHITE) { // Можно выделить отдельным цветом
                    // 1. Выбор способа (Dropdown)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CompactOutlinedTextField(
                            label = "Способ прокладки",
                            value = consumer.layingMethod.ifBlank { "Воздух" }, // Значение по умолчанию
                            onValueChange = {}, // ReadOnly, меняем через меню
                            contentPadding = FIELDCONTENTPADDING,
                            fontSizeSp = FIELDFONT,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { /* logic for dropdown handled below */ }
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
                                consumer.layingMethod = "Воздух"
                                methodMenuExpanded = false
                                onCalculationRequired() // Пересчитываем кабель
                            }) { Text("Воздух") }

                            DropdownMenuItem(onClick = {
                                consumer.layingMethod = "Земля"
                                methodMenuExpanded = false
                                onCalculationRequired() // Пересчитываем кабель
                            }) { Text("Земля") }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 2. Ввод длины
                    CompactOutlinedTextField(
                        label = "Длина, м",
                        value = consumer.cableLength,
                        onValueChange = { consumer.cableLength = it; onCalculationRequired() },
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
                    CompactOutlinedTextField(
                        label = "Число жил, сечение",
                        value = consumer.cableLine,
                        onValueChange = {}, // ReadOnly
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
                            changeCableSection(consumer, data, +1)
                            onRecalculateDropOnly() // <---
                        })

                        Text("▼", Modifier.clickable {
                            changeCableSection(consumer, data, -1)
                            onRecalculateDropOnly() // <---
                        })
                    }
                }



                Spacer(Modifier.height(FIELDVSPACE))

                // Падение напряжения
                CompactOutlinedTextField(
                    label = "Падение напряжения, В",
                    value = consumer.voltageDropV, // Поле результата
                    onValueChange = {}, // Read-only, так как это результат расчета
                    contentPadding = FIELDCONTENTPADDING,
                    fontSizeSp = FIELDFONT,
                    textColor = textColor, // Можно выделить красным, если есть warning (реализуйте логику цвета выше)
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

    // 6. Формируем строку
    val newSection = sections[newIndex]
    val cores = if ((consumer.voltage.toIntOrNull() ?: 230) >= 380) 5 else 3
    val sectionStr = if (newSection % 1.0 == 0.0) newSection.toInt().toString() else newSection.toString()

    consumer.cableLine = "${cores}x$sectionStr"
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

