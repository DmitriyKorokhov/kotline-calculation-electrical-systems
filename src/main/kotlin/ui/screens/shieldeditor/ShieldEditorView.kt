package ui.screens.shieldeditor

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.protection.*
import view.CompactOutlinedTextField
import java.io.File
import javax.swing.JFrame
import kotlin.concurrent.thread

// Параметры размеров
private val LEFT_PANEL_WIDTH: Dp = 300.dp
private val COLUMN_WIDTH: Dp = 220.dp
private val COLUMN_OUTER_PADDING: Dp = 4.dp
private val COLUMN_INNER_PADDING: Dp = 8.dp
private val COLUMN_SPACER: Dp = 6.dp
private val HEADER_HEIGHT: Dp = 24.dp
private const val HEADER_FONT = 13
private const val FIELD_FONT = 15
private val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val FIELD_VSPACE: Dp = 8.dp
private val SCROLLBAR_HEIGHT: Dp = 22.dp

private val BlueAccent = Color(0xFF2196F3)

private val BLOCK_BLUE     = Color(0xFFE3F2FD).copy(alpha = 0.15f)  // голубой
private val BLOCK_LAVENDER = Color(0xFFEDE7F6).copy(alpha = 0.15f)  // сиреневый
private val BLOCK_WHITE    = Color.White.copy(alpha = 0.15f)        // белый
private val BLOCK_BORDER   = Color(0xFFB0BEC5) // прежний спокойный серый бордер

@Composable
private fun BlockPanel(
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .border(1.dp, BLOCK_BORDER, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) { content() }
}

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

                        // 1) Выбор места сохранения DWG
                        val frame = JFrame().apply { isAlwaysOnTop = true }
                        val dlg = java.awt.FileDialog(frame, "Сохранить DWG как...", java.awt.FileDialog.SAVE).apply {
                            file = "${data.shieldName.ifBlank { "scheme" }}.dwg"
                            isVisible = true
                        }
                        val outPath = dlg.file?.let { File(dlg.directory, it).absolutePath }
                        frame.dispose()
                        if (outPath == null) return@DropdownMenuItem

                        // 2) Найти accoreconsole
                        var accorePath = ShieldStorage.accoreConsolePath
                        if (accorePath.isNullOrBlank()) {
                            accorePath = AutoCadExporter.tryFindAccoreConsole()
                            if (accorePath != null) ShieldStorage.accoreConsolePath = accorePath
                        }
                        if (accorePath.isNullOrBlank()) {
                            val fc = javax.swing.JFileChooser().apply {
                                dialogTitle = "Укажите путь к accoreconsole.exe"
                                fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
                            }
                            val res = fc.showOpenDialog(null)
                            if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
                                accorePath = fc.selectedFile.absolutePath
                                ShieldStorage.accoreConsolePath = accorePath
                            } else {
                                javax.swing.JOptionPane.showMessageDialog(null, "accoreconsole.exe не выбран", "Отмена", javax.swing.JOptionPane.INFORMATION_MESSAGE)
                                return@DropdownMenuItem
                            }
                        }

                        // 3) Найти шаблон DWG
                        var templatePath = ShieldStorage.templateDwgPath
                        if (templatePath.isNullOrBlank()) {
                            val guess = File(System.getProperty("user.dir"), "template_with_blocks.dwg")
                            if (guess.exists()) {
                                templatePath = guess.absolutePath
                                ShieldStorage.templateDwgPath = templatePath
                            } else {
                                val fc2 = javax.swing.JFileChooser().apply {
                                    dialogTitle = "Выберите template_with_blocks.dwg"
                                    fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
                                }
                                val res2 = fc2.showOpenDialog(null)
                                if (res2 == javax.swing.JFileChooser.APPROVE_OPTION) {
                                    templatePath = fc2.selectedFile.absolutePath
                                    ShieldStorage.templateDwgPath = templatePath
                                } else {
                                    javax.swing.JOptionPane.showMessageDialog(null, "Шаблон DWG не выбран", "Отмена", javax.swing.JOptionPane.INFORMATION_MESSAGE)
                                    return@DropdownMenuItem
                                }
                            }
                        }

                        // 4) Запуск экспорта (асинхронно)
                        thread {
                            val result = AutoCadExporter.exportUsingTrustedStaging(
                                accorePath = accorePath, // можно null — функция сама поищет accoreconsole.exe
                                templateDwgPath = templatePath!!, // путь к вашему source_blocks.dwg
                                outDwgPath = outPath,
                                shieldData = data,
                                baseX = 0,
                                stepX = 50,
                                y = 0,
                                timeoutSec = 300L,
                                useTemplateCopy = false
                            )

                            javax.swing.SwingUtilities.invokeLater {
                                val msg = javax.swing.JTextArea(result.output).apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
                                val scroll = javax.swing.JScrollPane(msg).apply { preferredSize = java.awt.Dimension(900, 420) }
                                val title = if (result.exitCode == 0) "Экспорт успешен" else "Экспорт завершился с ошибкой"
                                val type = if (result.exitCode == 0) javax.swing.JOptionPane.INFORMATION_MESSAGE else javax.swing.JOptionPane.ERROR_MESSAGE
                                javax.swing.JOptionPane.showMessageDialog(null, scroll, title, type)
                            }
                        }
                    }) { Text("Экспорт схемы в AutoCAD (DWG)") }
                }
            }


            Spacer(Modifier.width(12.dp))
            Text("Щит ID: ${shieldId ?: "-"}", fontSize = 14.sp, color = textColor)
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(animatedPanelWidth)
                    .fillMaxHeight()
                    .zIndex(if (metaExpanded) 1f else 0f)
            ) {
                // Внутри этого Box показываем содержимое панели с плавной анимацией по появлению/исчезновению
                // Вызваем функцию явно через полное имя, чтобы избежать конфликтов расширений.
                this@Row.AnimatedVisibility(
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

                        Text("Стандарт испытания", color = textColor, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { stdMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(data.protectionStandard.ifBlank { "Выберите стандарт" })
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

                        Text("Производитель устройств", color = textColor, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Box {
                            OutlinedButton(onClick = { manufMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(data.protectionManufacturer.ifBlank { "Выберите производителя" })
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

                        // Показать суммарные токи фаз (read-only)
                        CompactOutlinedTextField(
                            label = "Нагрузка на фазу L1, А",
                            value = data.phaseL1,
                            onValueChange = {}, // read-only
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = FIELD_CONTENT_PADDING,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            fontSizeSp = FIELD_FONT,
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))

                        CompactOutlinedTextField(
                            label = "Нагрузка на фазу L2, А",
                            value = data.phaseL2,
                            onValueChange = {}, // read-only
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = FIELD_CONTENT_PADDING,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            fontSizeSp = FIELD_FONT,
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))

                        CompactOutlinedTextField(
                            label = "Нагрузка на фазу L3, А",
                            value = data.phaseL3,
                            onValueChange = {}, // read-only
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = FIELD_CONTENT_PADDING,
                            textColor = textColor,
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = Color.LightGray,
                            fontSizeSp = FIELD_FONT,
                            singleLine = true
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
                                            val targetBg = if (isSelected) Color(0xFF1976D2) else Color.Transparent
                                            val animatedBg by animateColorAsState(
                                                targetValue = targetBg,
                                                animationSpec = tween(durationMillis = 260)
                                            )
                                            val targetTextColor = if (isSelected) Color.White else textColor
                                            val animatedTextColor by animateColorAsState(
                                                targetValue = targetTextColor,
                                                animationSpec = tween(durationMillis = 260)
                                            )
                                            val targetScale = if (isSelected) 1.02f else 1f
                                            val animatedScale by animateFloatAsState(
                                                targetValue = targetScale,
                                                animationSpec = spring(stiffness = 400f)
                                            )

                                            // Шапка столбца с выбором и контекстным меню
                                            Box {
                                                Box(
                                                    contentAlignment = Alignment.CenterStart,
                                                    modifier = Modifier
                                                        .height(HEADER_HEIGHT)
                                                        .fillMaxWidth()
                                                        .scale(animatedScale)
                                                        .background(animatedBg, RoundedCornerShape(6.dp))
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 0.dp,
                                                            color = if (isSelected) Color.White else Color.Transparent,
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .pointerInput(colIndex) {
                                                            awaitEachGesture {
                                                                val event = awaitPointerEvent()
                                                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                                                    contextMenuPosition = event.changes.first().position
                                                                    if (!selectedColumns.contains(colIndex)) {
                                                                        selectedColumns.clear()
                                                                        selectedColumns.add(colIndex)
                                                                    }
                                                                    contextMenuForHeader = colIndex
                                                                }
                                                            }
                                                        }
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = LocalIndication.current
                                                        ) {
                                                            if (selectedColumns.contains(colIndex)) {
                                                                selectedColumns.remove(colIndex)
                                                            } else {
                                                                selectedColumns.add(colIndex)
                                                            }
                                                        }
                                                ) {
                                                    Text(
                                                        text = "Потребитель ${colIndex + 1}",
                                                        fontSize = HEADER_FONT.sp,
                                                        color = animatedTextColor,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }

                                                ConsumerContextMenu(
                                                    expanded = contextMenuForHeader == colIndex,
                                                    offset = IntOffset(
                                                        contextMenuPosition.x.toInt(),
                                                        contextMenuPosition.y.toInt()
                                                    ),
                                                    onDismissRequest = { contextMenuForHeader = null },
                                                    isPasteEnabled = copiedConsumers.isNotEmpty() && selectedColumns.size == 1,
                                                    isAddEnabled = selectedColumns.size == 1,
                                                    onAction = { action ->
                                                        when (action) {
                                                            ContextMenuAction.DELETE -> {
                                                                if (selectedColumns.isNotEmpty()) {
                                                                    selectedColumns.sortedDescending().forEach { idx ->
                                                                        if (idx in data.consumers.indices) data.consumers.removeAt(
                                                                            idx
                                                                        )
                                                                    }
                                                                    selectedColumns.clear()
                                                                    saveNow()
                                                                }
                                                            }

                                                            ContextMenuAction.COPY -> {
                                                                copiedConsumers.clear()
                                                                selectedColumns.sorted().forEach { idx ->
                                                                    data.consumers.getOrNull(idx)
                                                                        ?.let { copiedConsumers.add(it.deepCopy()) }
                                                                }
                                                            }

                                                            ContextMenuAction.PASTE -> {
                                                                val target = selectedColumns.singleOrNull()
                                                                if (target != null && copiedConsumers.isNotEmpty()) {
                                                                    var insertPos = target + 1
                                                                    val newlyInsertedIndices = mutableListOf<Int>()
                                                                    copiedConsumers.forEach { c ->
                                                                        val copy = c.deepCopy()
                                                                        if (insertPos <= data.consumers.size) {
                                                                            data.consumers.add(insertPos, copy)
                                                                            newlyInsertedIndices.add(insertPos)
                                                                            insertPos++
                                                                        }
                                                                    }
                                                                    selectedColumns.clear()
                                                                    selectedColumns.addAll(newlyInsertedIndices)
                                                                    saveNow()
                                                                }
                                                            }

                                                            ContextMenuAction.ADD -> {
                                                                if (selectedColumns.size == 1) {
                                                                    addCountStr = "1"
                                                                    showAddDialog = true
                                                                }
                                                            }
                                                        }
                                                        contextMenuForHeader = null
                                                    }
                                                )
                                            }

                                            Divider(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                color = borderColor
                                            )

                                            // ---------------- БЛОК 1: Наименование ... Кабельная линия (голубой) ----------------
                                            BlockPanel(BLOCK_BLUE) {
                                                // Наименование
                                                CompactOutlinedTextField(
                                                    label = "Наим. потребителя",
                                                    value = consumer.name,
                                                    onValueChange = { consumer.name = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = false,
                                                    minLines = 1,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                // Номер помещения (новое поле)
                                                CompactOutlinedTextField(
                                                    label = "Номер помещения",
                                                    value = consumer.roomNumber,
                                                    onValueChange = {
                                                        consumer.roomNumber = it
                                                        saveNow()
                                                    },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                // Напряжение
                                                CompactOutlinedTextField(
                                                    label = "Напряжение, В",
                                                    value = consumer.voltage,
                                                    onValueChange = { consumer.voltage = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                // cosφ
                                                CompactOutlinedTextField(
                                                    label = "cos(φ)",
                                                    value = consumer.cosPhi,
                                                    onValueChange = { consumer.cosPhi = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Установ. мощность, Вт",
                                                    value = consumer.installedPowerW,
                                                    onValueChange = {
                                                        consumer.installedPowerW = it
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
                                                label = "Расчетная мощность, Вт",
                                                value = consumer.powerKw,
                                                onValueChange = { consumer.powerKw = it; saveNow() },
                                                contentPadding = FIELD_CONTENT_PADDING,
                                                fontSizeSp = FIELD_FONT,
                                                textColor = textColor,
                                                focusedBorderColor = borderColor,
                                                unfocusedBorderColor = Color.LightGray,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                   }

                                            Spacer(Modifier.height(8.dp))

                                            // ---------------- БЛОК 2: Расчетный ток, А ... Наименование линии (сиреневый) ----------------
                                            BlockPanel(BLOCK_WHITE) {
                                                CompactOutlinedTextField(
                                                    label = "Расчетный ток, А",
                                                    value = consumer.currentA,
                                                    onValueChange = {}, // read-only
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Номер фазы",
                                                    value = consumer.phaseNumber,
                                                    onValueChange = { consumer.phaseNumber = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Номер группы",
                                                    value = consumer.lineName,
                                                    onValueChange = { consumer.lineName = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = false,
                                                    minLines = 1,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            // ---------------- БЛОК 3: Номер автомата ... Падение напряжения на кабель, В (белый) ----------------
                                            BlockPanel(BLOCK_LAVENDER) {
                                                CompactOutlinedTextField(
                                                    label = "Номер автомата",
                                                    value = consumer.breakerNumber,
                                                    onValueChange = { consumer.breakerNumber = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    CompactOutlinedTextField(
                                                        label = "Устройство защиты",
                                                        value = consumer.protectionDevice,
                                                        onValueChange = {
                                                            consumer.protectionDevice = it
                                                            saveNow()
                                                        },
                                                        contentPadding = FIELD_CONTENT_PADDING,
                                                        fontSizeSp = FIELD_FONT,
                                                        textColor = textColor,
                                                        focusedBorderColor = borderColor,
                                                        unfocusedBorderColor = Color.LightGray,
                                                        singleLine = true,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                    )

                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDropDown,
                                                        contentDescription = "Выбрать тип защиты",
                                                        modifier = Modifier
                                                            .align(Alignment.CenterEnd)   // по вертикали по центру ячейки
                                                            .padding(end = 4.dp)          // небольшой отступ от рамки
                                                            .size(18.dp)                  // компактный размер иконки
                                                            .clickable {
                                                                protectionDialogForIndex = colIndex
                                                            }
                                                    )
                                                }
                                            }

                                            BlockPanel(BLOCK_WHITE) {
                                                // Марка кабеля (начало Блока)
                                                CompactOutlinedTextField(
                                                    label = "Марка кабеля",
                                                    value = consumer.cableLine,
                                                    onValueChange = { consumer.cableLine = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = false,
                                                    minLines = 1,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Способ прокладки",
                                                    value = consumer.layingMethod,
                                                    onValueChange = { consumer.layingMethod = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = false,
                                                    minLines = 1,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Число жил, сечение",
                                                    value = consumer.cableType,
                                                    onValueChange = { consumer.cableType = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    singleLine = false,
                                                    minLines = 1,
                                                    maxLines = 3,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                CompactOutlinedTextField(
                                                    label = "Падение напряжения, В",
                                                    value = consumer.voltageDropV,
                                                    onValueChange = { consumer.voltageDropV = it; saveNow() },
                                                    contentPadding = FIELD_CONTENT_PADDING,
                                                    fontSizeSp = FIELD_FONT,
                                                    textColor = textColor,
                                                    focusedBorderColor = borderColor,
                                                    unfocusedBorderColor = Color.LightGray,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
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
                                        manufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it -> it.isNotBlank() },
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
                                            // Создание пустого потребителя
                                            val newConsumer = ConsumerModel(
                                                name = "", voltage = "", cosPhi = "", powerKw = "", modes = "",
                                                cableLine = "", currentA = "", phaseNumber = "", lineName = "",
                                                breakerNumber = "", protectionDevice = "", protectionPoles = "",
                                                cableType = "", voltageDropV = ""
                                            )
                                            if (insertPos <= data.consumers.size) {
                                                data.consumers.add(insertPos, newConsumer)
                                                newlyInserted.add(insertPos)
                                                insertPos++
                                            }
                                        }
                                        // Выделяем новые столбцы
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
