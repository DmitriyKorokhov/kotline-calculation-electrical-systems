package ui.screens.shieldeditor

// Дополнительные импорты для анимаций/индикаций/ввода мыши
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.protection.*
import view.CompactOutlinedTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset


// Параметры — компактные размеры (подгоняйте при необходимости)
private val LEFT_PANEL_WIDTH: Dp = 300.dp
private val COLUMN_WIDTH: Dp = 340.dp
private val COLUMN_OUTER_PADDING: Dp = 4.dp
private val COLUMN_INNER_PADDING: Dp = 8.dp
private val COLUMN_SPACER: Dp = 6.dp
private val HEADER_HEIGHT: Dp = 24.dp
private const val HEADER_FONT = 13
private const val FIELD_FONT = 15
private val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val FIELD_VSPACE: Dp = 8.dp
private val SCROLLBAR_HEIGHT: Dp = 22.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShieldEditorView(shieldId: Int?, onBack: () -> Unit) {
    // данные щита (ShieldData поля уже mutableStateOf)
    val data = remember { ShieldStorage.loadOrCreate(shieldId) }

    val borderColor = Color.White
    val textColor = Color.White

    fun saveNow() = ShieldStorage.save(shieldId, data)

    // используем сохранённое состояние панели
    var metaExpanded by remember { mutableStateOf(data.metaExpanded) }

    // при изменении — сохраняем в ShieldData
    LaunchedEffect(metaExpanded) {
        data.metaExpanded = metaExpanded
        saveNow()
    }

    // Dropdowns
    var stdMenuExpanded by remember { mutableStateOf(false) }
    var manufMenuExpanded by remember { mutableStateOf(false) }
    val standards = listOf("ГОСТ IEC 60898-1-2020", "ГОСТ IEC 60947-2-2021")
    val manufacturers = listOf("Nader", "Sistem Electric", "Dekraft", "Hyundai")

    // Scroll states
    val hScrollState = rememberScrollState() // горизонтальный для колонок
    val vScrollState = rememberScrollState() // вертикальный для всей таблицы

    // state for selected columns (multiple selection)
    val selectedColumns = remember { mutableStateListOf<Int>() }

    // Анимируем ширину панели — это даёт плавный "сдвиг" таблицы
    val animatedPanelWidth by animateDpAsState(
        targetValue = if (metaExpanded) LEFT_PANEL_WIDTH else 0.dp,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
    )

    // Когда пользователь кликает по ячейке -> показываем первый popup (там у вас protectionDialogForIndex)
    var protectionDialogForIndex by remember { mutableStateOf<Int?>(null) }
    var showBreakerSecondWindow by remember { mutableStateOf(false) }
    var showBreakerThirdWindow by remember { mutableStateOf(false) }
    var breakerDialogConsumerIndex by remember { mutableStateOf<Int?>(null) }

    val csvExporter = remember { CsvExporter() } // Создаем экземпляр экспортера
    var showMoreMenu by remember { mutableStateOf(false) } // Состояние для видимости меню

    // состояние выбора второго окна для каждого потребителя (ключ — индекс колонки)
    data class BreakerDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedAdditions: List<String> = emptyList(),
        val selectedPoles: String? = null,
        val selectedCurve: String? = null
    )

    // map: индекс потребителя -> состояние выбора (если null — параметров ещё не задавали)
    val breakerDialogState = remember { mutableStateMapOf<Int, BreakerDialogState>() }

    // Буфер для копирования выбранных потребителей
    val copiedConsumers = remember { mutableStateListOf<ConsumerModel>() }

    // Какой столбец сейчас показывает контекстное меню (по ПКМ)
    var contextMenuForHeader by remember { mutableStateOf<Int?>(null) }

    // Диалог «Добавить»
    var showAddDialog by remember { mutableStateOf(false) }
    var addCountStr by remember { mutableStateOf("1") }

    // Состояние для хранения координат клика правой кнопкой мыши
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }


    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Top bar (Back only left)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                saveNow()
                onBack()
            }) {
                Text("Назад (сохранить)")
            }

            Spacer(Modifier.width(8.dp))

            Button(onClick = {
                // 1) рассчитываем токи для всех потребителей (заполняется consumer.currentA)
                val success = CalculationEngine.calculateAll(data)

                // 2) распределяем фазы и считаем суммарные токи по фазам
                PhaseDistributor.distributePhases(data)

                // 3) сохраняем
                saveNow()

                // можете показать Snackbar / уведомление, но здесь просто логируем
                println("Расчёт выполнен: $success потребителей рассчитано, суммарные токи: L1=${data.phaseL1}, L2=${data.phaseL2}, L3=${data.phaseL3}")
            }) {
                Text("Произвести расчёт")
            }

            Spacer(Modifier.weight(1f)) // Занимает все доступное пространство

            // Кнопка экспорта и другие действия
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Дополнительные действия")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        showMoreMenu = false
                        val path = csvExporter.chooseSavePath(
                            defaultName = "${data.shieldName.ifBlank { "shield" }}.csv"
                        )
                        if (path != null) {
                            val file = java.io.File(path)
                            csvExporter.export(data, file)
                        }
                    }) {
                        Text("Экспорт схемы в AutoCAD (CSV)")
                    }
                }
            }


            Spacer(Modifier.width(12.dp))
            Text("Щит ID: ${shieldId ?: "-"}", fontSize = 14.sp, color = textColor)
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxSize()) {
            // Блок: контейнер с анимируемой шириной (если ширина 0 — колонка займет 0 место,
            // при этом AnimatedVisibility отвечает за появление контента внутри)
            Box(
                modifier = Modifier
                    .width(animatedPanelWidth)
                    .fillMaxHeight()
                    .zIndex(if (metaExpanded) 1f else 0f)
            ) {
                // Внутри этого Box показываем содержимое панели с плавной анимацией по появлению/исчезновению
                // Вызваем функцию явно через полное имя, чтобы избежать конфликтов расширений.
                androidx.compose.animation.AnimatedVisibility(
                    visible = metaExpanded,
                    enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(360, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(360, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(6.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Кнопка сворачивания: знак '<' в левом углу панели
                            FloatingActionButton(
                                onClick = { metaExpanded = false },
                                modifier = Modifier.size(34.dp),
                                backgroundColor = MaterialTheme.colors.primary
                            ) { Text("<") }

                            Spacer(Modifier.width(8.dp))
                            Text("Данные щита", color = textColor, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                        }

                        Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Color.LightGray)

                        // Поля — теперь value у ShieldData mutableStateOf, ввод работает
                        CompactOutlinedTextField(
                            label = "Наименование щита",
                            value = data.shieldName,
                            onValueChange = {
                                data.shieldName = it
                                saveNow()
                            },
                            contentPadding = FIELD_CONTENT_PADDING,
                            fontSizeSp = 16,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        CompactOutlinedTextField(
                            label = "Макс. ток КЗ, кА",
                            value = data.maxShortCircuitCurrent,
                            onValueChange = {
                                data.maxShortCircuitCurrent = it
                                saveNow()
                            },
                            contentPadding = FIELD_CONTENT_PADDING,
                            fontSizeSp = 16,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        Text("Стандарт испытания", color = textColor)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { stdMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (data.protectionStandard.isBlank()) "Выберите стандарт" else data.protectionStandard)
                            }
                            DropdownMenu(
                                expanded = stdMenuExpanded,
                                onDismissRequest = { stdMenuExpanded = false }
                            ) {
                                standards.forEach { s ->
                                    DropdownMenuItem(onClick = {
                                        data.protectionStandard = s
                                        stdMenuExpanded = false
                                        saveNow()
                                    }) { Text(s) }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        Text("Производитель устройств", color = textColor)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { manufMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (data.protectionManufacturer.isBlank()) "Выберите производителя" else data.protectionManufacturer)
                            }
                            DropdownMenu(
                                expanded = manufMenuExpanded,
                                onDismissRequest = { manufMenuExpanded = false }
                            ) {
                                manufacturers.forEach { m ->
                                    DropdownMenuItem(onClick = {
                                        data.protectionManufacturer = m
                                        manufMenuExpanded = false
                                        saveNow()
                                    }) { Text(m) }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Сверните панель, если она мешает таблице.", color = Color.LightGray, fontSize = 12.sp)

                        // Показать суммарные токи фаз (read-only)
                        OutlinedTextField(
                            value = data.phaseL1,
                            onValueChange = {}, // read-only
                            label = { Text("Нагрузка на фазу L1, А") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = textColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = borderColor
                            )
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = data.phaseL2,
                            onValueChange = {},
                            label = { Text("Нагрузка на фазу L2, А") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = textColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = borderColor
                            )
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = data.phaseL3,
                            onValueChange = {},
                            label = { Text("Нагрузка на фазу L3, А") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = textColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = borderColor
                            )
                        )
                    }
                }
            }

            // Если панель свернута — оставляем небольшой gap и кнопку разворачивания слева от таблицы
            if (!metaExpanded) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    FloatingActionButton(
                        onClick = { metaExpanded = true },
                        modifier = Modifier.size(36.dp),
                        backgroundColor = MaterialTheme.colors.primary
                    ) { Text(">") }
                }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // ====== CENTER: таблица ======
            // Занимает всё оставшееся вертикальное пространство — высота адаптируется к размеру окна
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
            ) {
                // Column, где верхний блок (table area) растянется по высоте,
                // а scrollbar разместится внизу.
                Column(modifier = Modifier.fillMaxSize()) {
                    // Контейнер таблицы — занимает всё доступное пространство (weight = 1)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        // Внешняя вертикальная прокрутка для всей таблицы целиком
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScrollState)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(hScrollState)
                                    .padding(vertical = 6.dp)
                            ) {
                                Spacer(modifier = Modifier.width(6.dp))

                                data.consumers.forEachIndexed { colIndex, consumer ->
                                    Box(
                                        modifier = Modifier
                                            .width(COLUMN_WIDTH)
                                            .padding(COLUMN_OUTER_PADDING)
                                            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(6.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(COLUMN_INNER_PADDING)) {
                                            val isSelected = selectedColumns.contains(colIndex)

                                            // анимируем фон и цвет текста
                                            val targetBg = if (isSelected) Color(0xFF1976D2) else Color.Transparent
                                            val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(durationMillis = 260))
                                            val targetTextColor = if (isSelected) Color.White else textColor
                                            val animatedTextColor by animateColorAsState(targetValue = targetTextColor, animationSpec = tween(durationMillis = 260))

                                            // лёгкая анимация масштаба при выделении
                                            val targetScale = if (isSelected) 1.02f else 1f
                                            val animatedScale by animateFloatAsState(targetValue = targetScale, animationSpec = spring(stiffness = 400f))

                                            // interactionSource для ripple (используем LocalIndication.current, без deprecated rememberRipple)
                                            val interactionSource = remember { MutableInteractionSource() }
                                            Box {
                                                Surface(
                                                    modifier = Modifier
                                                        .height(HEADER_HEIGHT)
                                                        .fillMaxWidth()
                                                        .padding(start = 6.dp)
                                                        .scale(animatedScale)
                                                        // ПКМ на шапке: выбрать столбец (если не выбран) и открыть контекстное меню
                                                        .pointerInput(colIndex) {
                                                            awaitEachGesture {
                                                                while (true) {
                                                                    val event = awaitPointerEvent()
                                                                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                                        // Сохраняем позицию клика относительно шапки
                                                                        contextMenuPosition = event.changes.first().position

                                                                        // Ваша остальная логика остается без изменений
                                                                        if (!selectedColumns.contains(colIndex)) {
                                                                            selectedColumns.clear()
                                                                            selectedColumns.add(colIndex)
                                                                        }
                                                                        contextMenuForHeader = colIndex
                                                                        break
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // ЛКМ (как у вас было): переключение выделения столбца
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = LocalIndication.current
                                                        ) {
                                                            if (selectedColumns.contains(colIndex)) {
                                                                selectedColumns.remove(colIndex)
                                                            } else {
                                                                selectedColumns.add(colIndex)
                                                            }
                                                        },
                                                    color = animatedBg,
                                                    shape = RoundedCornerShape(6.dp),
                                                    elevation = if (isSelected) 4.dp else 0.dp
                                                ) {
                                                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                                                        Text(text = "Потребитель ${colIndex + 1}", fontSize = HEADER_FONT.sp, color = animatedTextColor)
                                                    }
                                                }

                                                // Получаем плотность для конвертации px в dp
                                                val density = LocalDensity.current

                                                DropdownMenu(
                                                    expanded = contextMenuForHeader == colIndex,
                                                    onDismissRequest = { contextMenuForHeader = null },
                                                    // Задаем смещение, равное координатам клика
                                                    offset = with(density) {
                                                        DpOffset(contextMenuPosition.x.toDp(), contextMenuPosition.y.toDp())
                                                    }
                                                ) {
                                                    // 1) Удалить — множественное удаление выделенных
                                                    DropdownMenuItem(onClick = {
                                                        if (selectedColumns.isNotEmpty()) {
                                                            // Удаляем по индексам в порядке убывания, чтобы не сдвигать ещё не удалённые
                                                            selectedColumns.sortedDescending().forEach { idx ->
                                                                if (idx in data.consumers.indices) {
                                                                    data.consumers.removeAt(idx)
                                                                }
                                                            }
                                                            selectedColumns.clear()
                                                            saveNow()
                                                        }
                                                        contextMenuForHeader = null
                                                    }) { Text("Удалить") }

                                                    // 2) Копировать — копируем все выделенные в буфер
                                                    DropdownMenuItem(onClick = {
                                                        copiedConsumers.clear()
                                                        selectedColumns.sorted().forEach { idx ->
                                                            data.consumers.getOrNull(idx)
                                                                ?.let { copiedConsumers.add(it.deepCopy()) }
                                                        }
                                                        contextMenuForHeader = null
                                                    }) { Text("Копировать") }

                                                    // 3) Вставить — после выбранного столбца (ровно 1 выбранный)
                                                    DropdownMenuItem(onClick = {
                                                        val target = selectedColumns.singleOrNull()
                                                        if (target != null && copiedConsumers.isNotEmpty()) {
                                                            var insertPos = target + 1
                                                            // Вставляем копии, чтобы не делить состояние с буфером
                                                            val newlyInsertedIndices = mutableListOf<Int>()
                                                            copiedConsumers.forEach { c ->
                                                                val copy = c.deepCopy()
                                                                if (insertPos <= data.consumers.size) {
                                                                    data.consumers.add(insertPos, copy)
                                                                    newlyInsertedIndices.add(insertPos)
                                                                    insertPos++
                                                                }
                                                            }
                                                            // Обновим выделение на вставленные (удобно пользователю)
                                                            selectedColumns.clear()
                                                            selectedColumns.addAll(newlyInsertedIndices)
                                                            saveNow()
                                                        } else {
                                                            // Можно показать Snackbar/toast, если захотите
                                                            // println("Для вставки должен быть выбран ровно один столбец и буфер копирования не пуст.")
                                                        }
                                                        contextMenuForHeader = null
                                                    }) { Text("Вставить") }

                                                    // 4) Добавить — запросить количество и добавить N пустых после выбранного столбца (ровно 1 выбранный)
                                                    DropdownMenuItem(onClick = {
                                                        if (selectedColumns.size == 1) {
                                                            addCountStr = "1"
                                                            showAddDialog = true
                                                        } else {
                                                            // println("Для добавления должен быть выбран ровно один столбец.")
                                                        }
                                                        contextMenuForHeader = null
                                                    }) { Text("Добавить") }
                                                }
                                            }

                                            Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = borderColor)

                                            // Поля (автосохранение)
                                            CompactOutlinedTextField(
                                                label = "Наименование",
                                                value = consumer.name,
                                                onValueChange = {
                                                    consumer.name = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Напряжение, В",
                                                value = consumer.voltage,
                                                onValueChange = {
                                                    consumer.voltage = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "cos(φ)",
                                                value = consumer.cosPhi,
                                                onValueChange = {
                                                    consumer.cosPhi = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Установленная мощность, кВт",
                                                value = consumer.powerKw,
                                                onValueChange = {
                                                    consumer.powerKw = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Режимы работы",
                                                value = consumer.modes,
                                                onValueChange = {
                                                    consumer.modes = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Кабельная линия",
                                                value = consumer.cableLine,
                                                onValueChange = {
                                                    consumer.cableLine = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            // Блок 2
                                            CompactOutlinedTextField(
                                                label = "Расчетный ток, А",
                                                value = consumer.currentA,
                                                onValueChange = {
                                                    consumer.currentA = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Номер фазы",
                                                value = consumer.phaseNumber,
                                                onValueChange = {
                                                    consumer.phaseNumber = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Наименование линии",
                                                value = consumer.lineName,
                                                onValueChange = {
                                                    consumer.lineName = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            // Блок 3
                                            CompactOutlinedTextField(
                                                label = "Номер автомата",
                                                value = consumer.breakerNumber,
                                                onValueChange = {
                                                    consumer.breakerNumber = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedTextField(
                                                    value = consumer.protectionDevice,
                                                    onValueChange = {
                                                        consumer.protectionDevice = it
                                                        saveNow()
                                                    },
                                                    label = { Text("Устройство защиты и коммутации") },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        // делаем начальную высоту такой же как у других полей — minHeight 48.dp (подберите при необходимости)
                                                        .heightIn(min = 48.dp),
                                                    singleLine = false,   // разрешаем переносы строк
                                                    minLines = 1,         // изначально высота = одна строка (равняется остальным полям)
                                                    maxLines = 10,        // при вводе Enter поле будет увеличиваться до 10 строк
                                                    trailingIcon = {
                                                        // Нажатие на эту иконку откроет окно выбора устройств защиты для колонки colIndex
                                                        IconButton(onClick = { protectionDialogForIndex = colIndex }) {
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать тип защиты")
                                                        }
                                                    },
                                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                                        textColor = textColor,
                                                        focusedBorderColor = borderColor,
                                                        unfocusedBorderColor = Color.LightGray
                                                    )
                                                )

                                            }
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Тип кабеля",
                                                value = consumer.cableType,
                                                onValueChange = {
                                                    consumer.cableType = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(FIELD_VSPACE))

                                            CompactOutlinedTextField(
                                                label = "Падение напряжения на кабель, В",
                                                value = consumer.voltageDropV,
                                                onValueChange = {
                                                    consumer.voltageDropV = it
                                                    saveNow()
                                                },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(COLUMN_SPACER))
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // Горизонтальный scrollbar снизу (толще для удобства)
                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(hScrollState),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SCROLLBAR_HEIGHT)
                            .padding(vertical = 6.dp)
                    )

                    // Если protectionDialogForIndex != null, показываем окно выбора
                    if (protectionDialogForIndex != null) {
                        val idx = protectionDialogForIndex!!
                        val consumer = data.consumers.getOrNull(idx)

                        val initial = protectionTypeFromString(consumer?.protectionDevice)

                        ProtectionChooserPopup(
                            initial = initial,
                            onDismissRequest = { protectionDialogForIndex = null },
                            onConfirm = { selected ->
                                // Закрываем первое окно (ProtectionChooser)
                                protectionDialogForIndex = null

                                if (selected == ProtectionType.CIRCUIT_BREAKER) {
                                    // Для выбранного потребителя сохраняем индекс
                                    breakerDialogConsumerIndex = idx

                                    // Сохраняем производителя из левой панели (если он есть) в state — чтобы 2-е окно знало производителя
                                    val manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                    val prev = breakerDialogState[idx]

                                    // если ранее у нас не было state для этого потребителя — создаём базовый
                                    if (prev == null) {
                                        breakerDialogState[idx] = BreakerDialogState(manufacturer = manufacturer)
                                    } else {
                                        // обновим manufacturer, если он изменился
                                        breakerDialogState[idx] = prev.copy(manufacturer = manufacturer ?: prev.manufacturer)
                                    }

                                    // Всегда открываем второе окно (теперь оно будет предзаполнено из breakerDialogState[idx] если параметры были сохранены ранее)
                                    showBreakerSecondWindow = true
                                } else {
                                    // для других типов защиты — обычная логика (как у вас была)
                                    consumer?.let {
                                        it.protectionDevice = selected.displayName()
                                        saveNow()
                                    }
                                }
                            }
                        )
                    }

                    var breakerDialogSelectedCurve by remember { mutableStateOf<String?>(null) }

                    if (showBreakerSecondWindow) {
                        val idx = breakerDialogConsumerIndex
                        val consumer = idx?.let { data.consumers.getOrNull(it) }
                        val st = idx?.let { breakerDialogState[it] }

                        BreakerSecondWindow(
                            initialManufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
                            initialSeries = st?.series,
                            initialSelectedAdditions = st?.selectedAdditions ?: emptyList(),
                            initialSelectedPoles = st?.selectedPoles,
                            initialSelectedCurve = st?.selectedCurve,
                            consumerVoltageStr = consumer?.voltage,
                            onBack = {
                                // вернуться к первому окну: показываем protection chooser для того же consumer
                                showBreakerSecondWindow = false
                                protectionDialogForIndex = idx
                            },
                            onDismiss = {
                                showBreakerSecondWindow = false
                                breakerDialogConsumerIndex = null
                            },
                            onConfirm = { result ->
                                // сохраняем выбранные параметры в state для данного consumer
                                idx?.let {
                                    breakerDialogState[it] = BreakerDialogState(
                                        manufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        series = result.series,
                                        selectedAdditions = result.selectedAdditions,
                                        selectedPoles = result.selectedPoles,
                                        selectedCurve = result.selectedCurve
                                    )
                                }

                                // переходим в 3-е окно
                                showBreakerSecondWindow = false
                                showBreakerThirdWindow = true
                            }
                        )
                    }

                    if (showBreakerThirdWindow) {
                        val idx = breakerDialogConsumerIndex
                        val consumer = idx?.let { data.consumers.getOrNull(it) }
                        val st = idx?.let { breakerDialogState[it] }

                        BreakerThirdWindow(
                            maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                            standard = data.protectionStandard,
                            consumerCurrentAStr = consumer?.currentA ?: "",
                            consumerVoltageStr = consumer?.voltage,
                            selectedSeries = st?.series,
                            selectedPoles = st?.selectedPoles,
                            selectedAdditions = st?.selectedAdditions ?: emptyList(),
                            selectedCurve = st?.selectedCurve,
                            onBack = {
                                // вернуться в 2-е окно (параметры st останутся)
                                showBreakerThirdWindow = false
                                showBreakerSecondWindow = true
                            },
                            onDismiss = {
                                showBreakerThirdWindow = false
                            },
                            onChoose = { formattedMultilineString ->
                                consumer?.let {
                                    it.protectionDevice = formattedMultilineString
                                    // Сохраняем также полюса, чтобы экспорт мог их взять
                                    it.protectionPoles = st?.selectedPoles ?: ""
                                    saveNow()
                                }
                                showBreakerThirdWindow = false
                            }
                        )
                    }

                    if (showAddDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddDialog = false },
                            title = { Text("Количество новых потребителей") },
                            text = {
                                OutlinedTextField(
                                    value = addCountStr,
                                    onValueChange = { addCountStr = it.filter { ch -> ch.isDigit() } },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val count = addCountStr.toIntOrNull() ?: 0
                                    val target = selectedColumns.singleOrNull()
                                    if (count > 0 && target != null) {
                                        var insertPos = target + 1
                                        val newlyInserted = mutableListOf<Int>()
                                        repeat(count) {
                                            // Пустой потребитель (у вас, судя по коду, есть дефолтный конструктор)
                                            val newConsumer = ConsumerModel(
                                                name = "",
                                                voltage = "",
                                                cosPhi = "",
                                                powerKw = "",
                                                modes = "",
                                                cableLine = "",
                                                currentA = "",
                                                phaseNumber = "",
                                                lineName = "",
                                                breakerNumber = "",
                                                protectionDevice = "",
                                                protectionPoles = "",
                                                cableType = "",
                                                voltageDropV = ""
                                            )
                                            if (insertPos <= data.consumers.size) {
                                                data.consumers.add(insertPos, newConsumer)
                                                newlyInserted.add(insertPos)
                                                insertPos++
                                            }
                                        }
                                        // Выделим добавленные
                                        selectedColumns.clear()
                                        selectedColumns.addAll(newlyInserted)
                                        saveNow()
                                    }
                                    showAddDialog = false
                                }) { Text("OK") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
                            }
                        )
                    }
                }
            }
        }
    }
}
