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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.exporter.AutoCadExporter
import ui.screens.shieldeditor.protection.*
import view.CompactOutlinedTextField
import java.io.File
import javax.swing.JFrame
import kotlin.concurrent.thread
import data.database.Cables
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import androidx.compose.foundation.layout.heightIn
import ui.screens.shieldeditor.input.InputParamsWindow
import ui.screens.shieldeditor.input.InputType
import ui.screens.shieldeditor.input.InputTypePopup

private val LEFT_PANEL_WIDTH: Dp = 300.dp
private val COLUMN_WIDTH: Dp = 220.dp
private val COLUMN_OUTER_PADDING: Dp = 4.dp
private val COLUMN_INNER_PADDING: Dp = 8.dp
private val HEADER_HEIGHT: Dp = 24.dp
private const val HEADER_FONT = 13
private const val FIELD_FONT = 15
private val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val FIELD_VSPACE: Dp = 8.dp
private val SCROLLBAR_HEIGHT: Dp = 22.dp
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

@Composable
fun CableTypePopup(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var cableTypes by remember { mutableStateOf(emptyList<String>()) }

    // Загружаем типы кабелей из БД при открытии
    LaunchedEffect(Unit) {
        transaction {
            cableTypes = Cables.selectAll().map { it[Cables.type] }
        }
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(200.dp).heightIn(max = 300.dp) // Ограничиваем высоту и ширину
    ) {
        if (cableTypes.isEmpty()) {
            DropdownMenuItem(onClick = { }) {
                Text("Нет данных в БД")
            }
        } else {
            cableTypes.forEach { type ->
                DropdownMenuItem(onClick = { onConfirm(type) }) {
                    Text(type)
                }
            }
        }
    }
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
    val manufacturers = listOf("Nader", "Systeme electric", "DEKraft", "Hyundai")

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

    var showRcboSecondWindow by remember { mutableStateOf(false) }
    var showRcboThirdWindow by remember { mutableStateOf(false) }

    var showMoreMenu by remember { mutableStateOf(false) }

    var showAtsSecondWindow by remember { mutableStateOf(false) }
    var showAtsThirdWindow by remember { mutableStateOf(false) }

    data class AtsDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedPoles: String? = null
    )
    val atsState = remember { mutableStateOf(AtsDialogState()) }

    data class BreakerDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedAdditions: List<String> = emptyList(),
        val selectedPoles: String? = null,
        val selectedCurve: String? = null
    )

    data class RcboDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedAdditions: List<String> = emptyList(),
        val selectedPoles: String? = null,
        val selectedCurve: String? = null,
        val selectedResidualCurrent: String? = null
    )

    data class RcdDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedPoles: String? = null,
        val selectedResidualCurrent: String? = null
    )
    var isComboMode by remember { mutableStateOf(false) }

    val rcdDialogState = remember { mutableStateMapOf<Int, RcdDialogState>() }

    var showRcdSecondWindow by remember { mutableStateOf(false) }
    var showRcdThirdWindow by remember { mutableStateOf(false) }

    var tempBreakerResult by remember { mutableStateOf<String?>(null) }

    val rcboDialogState = remember { mutableStateMapOf<Int, RcboDialogState>() }

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

    var showInputTypeDialog by remember { mutableStateOf(false) }
    var selectedInputType by remember { mutableStateOf<InputType?>(null) }

    // Состояния для окон автомата (используем те же переменные для данных, но флаги видимости свои)
    var showInputBreakerSecond by remember { mutableStateOf(false) }
    var showInputBreakerThird by remember { mutableStateOf(false) }

    // Временное хранилище параметров автомата ввода
    val inputBreakerState = remember { mutableStateOf(BreakerDialogState()) }

    // Состояние для выбора кабеля
    var showCableTypeDialog by remember { mutableStateOf(false) }
    var cableDialogConsumerIndex by remember { mutableStateOf<Int?>(null) }

    var showInputParamsWindow by remember { mutableStateOf(false) }
    var inputParamsResult by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        // Top bar (Back only left)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                saveNow()
                onBack()
            }) {
                Text("Назад")
            }

            Spacer(Modifier.width(8.dp))

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
                                javax.swing.JOptionPane.showMessageDialog(
                                    null,
                                    "accoreconsole.exe не выбран",
                                    "Отмена",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                                )
                                return@DropdownMenuItem
                            }
                        }

                        // 3) Найти шаблон DWG
                        var templatePath = ShieldStorage.templateDwgPath
                        if (templatePath.isNullOrBlank()) {
                            val guess = File(System.getProperty("user.dir"), "source_blocks.dwg")
                            if (guess.exists()) {
                                templatePath = guess.absolutePath
                                ShieldStorage.templateDwgPath = templatePath
                            } else {
                                val fc2 = javax.swing.JFileChooser().apply {
                                    dialogTitle = "Выберите source_blocks"
                                    fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
                                }
                                val res2 = fc2.showOpenDialog(null)
                                if (res2 == javax.swing.JFileChooser.APPROVE_OPTION) {
                                    templatePath = fc2.selectedFile.absolutePath
                                    ShieldStorage.templateDwgPath = templatePath
                                } else {
                                    javax.swing.JOptionPane.showMessageDialog(
                                        null,
                                        "Шаблон DWG не выбран",
                                        "Отмена",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                                    )
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
                                val msg = javax.swing.JTextArea(result.output)
                                    .apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
                                val scroll =
                                    javax.swing.JScrollPane(msg).apply { preferredSize = java.awt.Dimension(900, 420) }
                                val title =
                                    if (result.exitCode == 0) "Экспорт успешен" else "Экспорт завершился с ошибкой"
                                val type =
                                    if (result.exitCode == 0) javax.swing.JOptionPane.INFORMATION_MESSAGE else javax.swing.JOptionPane.ERROR_MESSAGE
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
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(360, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(360, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180))
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

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                CalculationEngine.calculateAll(data)
                                PhaseDistributor.distributePhases(data)
                                saveNow()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Распределить нагрузку")
                        }

                        Spacer(Modifier.height(8.dp))

                        BlockPanel(BLOCK_BLUE) {
                            // --- Поля для ввода ---
                            CompactOutlinedTextField(
                                label = "Коэф. спроса",
                                value = data.demandFactor,
                                onValueChange = { data.demandFactor = it }, // обновляем значение в модели
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
                                label = "Коэф. одновременности",
                                value = data.simultaneityFactor,
                                onValueChange = { data.simultaneityFactor = it }, // обновляем значение в модели
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = FIELD_CONTENT_PADDING,
                                textColor = textColor,
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = Color.LightGray,
                                fontSizeSp = FIELD_FONT,
                                singleLine = true
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        BlockPanel(BLOCK_WHITE) {

                            // --- Поля для вывода расчётов ---
                            CompactOutlinedTextField(
                                label = "Установ. мощность, Вт",
                                value = data.totalInstalledPower,
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
                                label = "Расчетная мощность, Вт",
                                value = data.totalCalculatedPower,
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
                                label = "cos(φ)",
                                value = data.averageCosPhi,
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
                                label = "Ток, А",
                                value = data.totalCurrent,
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
                                label = "Коэф. спроса щита",
                                value = data.shieldDemandFactor,
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 4.dp)
            ) {

                // --- БЛОК "ВВОД" (Над таблицей) ---
                Box(modifier = Modifier.padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 12.dp)) {
                    BlockPanel(color = Color(0xFFE8F5E9).copy(alpha = 0.5f)) { // Зеленоватый оттенок
                        Text("Параметры ввода", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))

                        CompactOutlinedTextField(
                            label = "Тип и устройство",
                            value = data.inputInfo,
                            onValueChange = { data.inputInfo = it; saveNow() }, // Позволяем редактировать текст вручную
                            modifier = Modifier.width(300.dp),
                            contentPadding = FIELD_CONTENT_PADDING,
                            fontSizeSp = FIELD_FONT,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 6,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    "Выбрать",
                                    Modifier.clickable { showInputTypeDialog = true }
                                )
                            }
                        )
                    }
                }

                if (showInputTypeDialog) {
                    InputTypePopup(
                        onDismissRequest = { showInputTypeDialog = false },
                        onConfirm = { type ->
                            selectedInputType = type
                            showInputTypeDialog = false

                            if (type == InputType.ONE_INPUT_BREAKER) {
                                showInputParamsWindow = true
                            } else if (type == InputType.TWO_INPUTS_ATS_BREAKERS) {
                                inputBreakerState.value = BreakerDialogState(
                                    manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                )
                                showInputBreakerSecond = true
                            } else if (type.needsBreakerConfig) {
                                // ... старая логика ...
                                inputBreakerState.value = BreakerDialogState(
                                    manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                )
                                showInputBreakerSecond = true
                            } else if (type == InputType.ATS_BLOCK_TWO_INPUTS) {
                                // ... логика АВР ...
                                atsState.value = AtsDialogState(
                                    manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                )
                                showAtsSecondWindow = true
                            } else {
                                data.inputInfo = type.title
                                saveNow()
                            }
                        }
                    )
                }

                // 2. Второе окно (Серия, Кривая) - ТОЛЬКО для Варианта 4
                if (showInputBreakerSecond) {
                    val st = inputBreakerState.value
                    BreakerSecondWindow(
                        initialManufacturer = st.manufacturer,
                        initialSeries = st.series,
                        initialSelectedAdditions = st.selectedAdditions,
                        initialSelectedPoles = st.selectedPoles,
                        initialSelectedCurve = st.selectedCurve,
                        consumerVoltageStr = "400",
                        onBack = { showInputBreakerSecond = false;
                            if (selectedInputType == InputType.ONE_INPUT_BREAKER) {
                                showInputParamsWindow = true
                            } else {
                                showInputTypeDialog = true
                            }
                        },
                        onDismiss = { showInputBreakerSecond = false },
                        onConfirm = { result ->
                            inputBreakerState.value = BreakerDialogState(
                                manufacturer = data.protectionManufacturer,
                                series = result.series,
                                selectedAdditions = result.selectedAdditions,
                                selectedPoles = result.selectedPoles,
                                selectedCurve = result.selectedCurve
                            )
                            showInputBreakerSecond = false
                            showInputBreakerThird = true
                        }
                    )
                }

                // 3. Третье окно (Номинал тока) - ТОЛЬКО для Варианта 4
                if (showInputBreakerThird) {
                    val st = inputBreakerState.value
                    BreakerThirdWindow(
                        maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                        standard = data.protectionStandard,
                        consumerCurrentAStr = data.totalCurrent, // Ток щита для подбора
                        consumerVoltageStr = "400",
                        selectedSeries = st.series,
                        selectedPoles = st.selectedPoles,
                        selectedAdditions = st.selectedAdditions,
                        selectedCurve = st.selectedCurve,
                        onBack = { showInputBreakerThird = false; showInputBreakerSecond = true },
                        onDismiss = { showInputBreakerThird = false },
                        onChoose = { resultFromWindow ->
                            val correctedDeviceStr = resultFromWindow.replace(Regex("(\\d)\\sA"), "$1A")
                            val header = selectedInputType?.title ?: "Ввод"
                            var paramsInfo = ""
                            if (selectedInputType == InputType.ONE_INPUT_BREAKER && inputParamsResult != null) {
                                val (cnt, bypass) = inputParamsResult!!
                                paramsInfo = "\nВводов: $cnt" + if (bypass) ", Байпас: Да" else ""
                            }
                            val separator = "---------------------------------"
                            val fullText = "$header$paramsInfo\n$separator\n$correctedDeviceStr"
                            data.inputInfo = fullText
                            saveNow()
                            showInputBreakerThird = false
                        }
                    )
                }

                // ====== CENTER: таблица ======
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
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
                                                .border(
                                                    width = 1.dp,
                                                    color = borderColor,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
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
                                                                        contextMenuPosition =
                                                                            event.changes.first().position
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
                                                                        selectedColumns.sortedDescending()
                                                                            .forEach { idx ->
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
                                                        onValueChange = {
                                                            consumer.voltage = it
                                                            CalculationEngine.calculateAll(data)
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

                                                    // cosφ
                                                    CompactOutlinedTextField(
                                                        label = "cos(φ)",
                                                        value = consumer.cosPhi,
                                                        onValueChange = {
                                                            consumer.cosPhi = it
                                                            CalculationEngine.calculateAll(data)
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
                                                        onValueChange = {
                                                            consumer.powerKw = it
                                                            CalculationEngine.calculateAll(data)
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
                                                            singleLine = false,
                                                            minLines = 1,
                                                            maxLines = 8,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                        )

                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = "Выбрать тип защиты",
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)   // по вертикали по центру ячейки
                                                                .padding(end = 4.dp)          // небольшой отступ от рамки
                                                                .size(24.dp)                  // компактный размер иконки
                                                                .clickable {
                                                                    protectionDialogForIndex = colIndex
                                                                }
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(FIELD_VSPACE))

                                                BlockPanel(BLOCK_WHITE) {
                                                    // Марка кабеля (начало Блока)
                                                    Box(modifier = Modifier.fillMaxWidth()) {
                                                        CompactOutlinedTextField(
                                                            label = "Марка кабеля",
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
                                                            singleLine = false,
                                                            minLines = 1,
                                                            maxLines = 3,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        // Иконка раскрывающегося списка
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = "Выбрать марку",
                                                            modifier = Modifier
                                                                .align(Alignment.CenterEnd)
                                                                .padding(end = 4.dp)
                                                                .size(24.dp)
                                                                .clickable {
                                                                    cableDialogConsumerIndex = colIndex
                                                                    showCableTypeDialog = true
                                                                }
                                                        )
                                                    }
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
                                    }
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
                                    protectionDialogForIndex = null
                                    breakerDialogConsumerIndex = idx

                                    val manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                    val prev = breakerDialogState[idx]
                                    if (prev == null) {
                                        breakerDialogState[idx] = BreakerDialogState(manufacturer = manufacturer)
                                    } else {
                                        breakerDialogState[idx] = prev.copy(manufacturer = manufacturer ?: prev.manufacturer)
                                    }

                                    if (selected == ProtectionType.CIRCUIT_BREAKER) {
                                        isComboMode = false
                                        showBreakerSecondWindow = true
                                    } else if (selected == ProtectionType.DIFF_CURRENT_BREAKER) {
                                        isComboMode = false // Сбрасываем флаг
                                        showRcboSecondWindow = true
                                    } else if (selected == ProtectionType.CIRCUIT_BREAKER_AND_RCD) {
                                        // !!! ИЗМЕНЕНИЕ: Не пишем текст в ячейку, просто ставим флаг
                                        isComboMode = true
                                        tempBreakerResult = null
                                        showBreakerSecondWindow = true
                                    } else {
                                        // Для остальных типов (рубильник и т.д.)
                                        isComboMode = false
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
                                initialManufacturer = st?.manufacturer
                                    ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
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
                                            manufacturer = st?.manufacturer
                                                ?: data.protectionManufacturer.takeIf { it -> it.isNotBlank() },
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

                        if (showRcboSecondWindow) {
                            val idx = breakerDialogConsumerIndex
                            val consumer = idx?.let { data.consumers.getOrNull(it) }
                            val st = idx?.let { rcboDialogState[it] }
                            RcboSecondWindow(
                                initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                consumerVoltageStr = consumer?.voltage,
                                onBack = {
                                    showRcboSecondWindow = false
                                    protectionDialogForIndex = idx // Вернуться к выбору типа
                                },
                                onDismiss = {
                                    showRcboSecondWindow = false
                                    breakerDialogConsumerIndex = null
                                },
                                onConfirm = { result ->
                                    idx?.let {
                                        rcboDialogState[it] = RcboDialogState(
                                            manufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
                                            series = result.series,
                                            selectedAdditions = result.selectedAdditions,
                                            selectedPoles = result.selectedPoles,
                                            selectedCurve = result.selectedCurve,
                                            selectedResidualCurrent = result.selectedResidualCurrent
                                        )
                                    }
                                    showRcboSecondWindow = false
                                    showRcboThirdWindow = true
                                }
                            )
                        }

                        if (showBreakerThirdWindow) {
                            val idx = breakerDialogConsumerIndex
                            if (idx != null) {
                                val consumer = data.consumers.getOrNull(idx)
                                // Получаем сохраненное состояние выбора (серия, кривая, дополнения)
                                val state = breakerDialogState[idx] ?: BreakerDialogState()

                                if (consumer != null) {
                                    BreakerThirdWindow(
                                        maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                        standard = data.protectionStandard,
                                        consumerCurrentAStr = consumer.currentA,
                                        consumerVoltageStr = consumer.voltage,
                                        selectedSeries = state.series,
                                        selectedPoles = state.selectedPoles,
                                        selectedAdditions = state.selectedAdditions, // Список строк-дополнений
                                        selectedCurve = state.selectedCurve,
                                        onBack = {
                                            showBreakerThirdWindow = false
                                            showBreakerSecondWindow = true
                                        },
                                        onDismiss = {
                                            showBreakerThirdWindow = false
                                            breakerDialogConsumerIndex = null
                                        },
                                        onChoose = { resultFromWindow ->
                                            val correctedString = resultFromWindow.replace(Regex("(\\d)\\sA"), "$1A")

                                            if (isComboMode) {
                                                tempBreakerResult = correctedString

                                                val manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                                val prevRcd = rcdDialogState[idx]
                                                if (prevRcd == null) {
                                                    rcdDialogState[idx] = RcdDialogState(manufacturer = manufacturer)
                                                } else {
                                                    rcdDialogState[idx] = prevRcd.copy(manufacturer = manufacturer ?: prevRcd.manufacturer)
                                                }

                                                showBreakerThirdWindow = false
                                                showRcdSecondWindow = true
                                            } else {

                                                consumer.let {
                                                    it.protectionDevice = correctedString
                                                    it.protectionPoles = state.selectedPoles ?: ""
                                                    saveNow()
                                                }
                                                showBreakerThirdWindow = false
                                                breakerDialogConsumerIndex = null
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (showRcboThirdWindow) {
                            val idx = breakerDialogConsumerIndex
                            if (idx != null) {
                                val consumer = data.consumers.getOrNull(idx)
                                val state = rcboDialogState[idx] ?: RcboDialogState()

                                if (consumer != null) {
                                    RcboThirdWindow(
                                        maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                        standard = data.protectionStandard,
                                        consumerCurrentAStr = consumer.currentA,
                                        consumerVoltageStr = consumer.voltage,
                                        selectedSeries = state.series,
                                        selectedPoles = state.selectedPoles,
                                        selectedAdditions = state.selectedAdditions,
                                        selectedCurve = state.selectedCurve,
                                        selectedResidualCurrent = state.selectedResidualCurrent,
                                        onBack = {
                                            showRcboThirdWindow = false
                                            showRcboSecondWindow = true
                                        },
                                        onDismiss = {
                                            showRcboThirdWindow = false
                                            breakerDialogConsumerIndex = null
                                        },
                                        onChoose = { resultFromWindow ->
                                            val correctedString = resultFromWindow.replace(Regex("(\\d)\\sA"), "$1A")
                                            consumer.let {
                                                it.protectionDevice = correctedString
                                                it.protectionPoles = state.selectedPoles ?: ""
                                                saveNow()
                                            }
                                            showRcboThirdWindow = false
                                            breakerDialogConsumerIndex = null
                                        }
                                    )
                                }
                            }
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

                        // --- Окно 2 для УЗО (выбор серии/параметров) ---
                        if (showRcdSecondWindow) {
                            val idx = breakerDialogConsumerIndex
                            val consumer = idx?.let { data.consumers.getOrNull(it) }
                            val st = idx?.let { rcdDialogState[it] }

                            RcdSecondWindow(
                                initialManufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
                                initialSeries = st?.series,
                                initialSelectedPoles = st?.selectedPoles,
                                initialSelectedResidualCurrent = st?.selectedResidualCurrent,
                                consumerVoltageStr = consumer?.voltage,
                                onBack = {
                                    // Возврат к выбору автомата (3-е окно)
                                    showRcdSecondWindow = false
                                    showBreakerThirdWindow = true
                                },
                                onDismiss = {
                                    showRcdSecondWindow = false
                                    tempBreakerResult = null
                                    breakerDialogConsumerIndex = null
                                },
                                onConfirm = { result ->
                                    idx?.let {
                                        rcdDialogState[it] = RcdDialogState(
                                            manufacturer = st?.manufacturer ?: data.protectionManufacturer.takeIf { it.isNotBlank() },
                                            series = result.series,
                                            selectedPoles = result.selectedPoles,
                                            selectedResidualCurrent = result.selectedResidualCurrent
                                        )
                                    }
                                    showRcdSecondWindow = false
                                    showRcdThirdWindow = true
                                }
                            )
                        }

                        // --- Окно 3 для УЗО (выбор номинала) ---
                        if (showRcdThirdWindow) {
                            val idx = breakerDialogConsumerIndex
                            if (idx != null) {
                                val consumer = data.consumers.getOrNull(idx)
                                val state = rcdDialogState[idx] ?: RcdDialogState()

                                if (consumer != null) {
                                    RcdThirdWindow(
                                        consumerCurrentAStr = consumer.currentA,
                                        consumerVoltageStr = consumer.voltage,
                                        selectedSeries = state.series,
                                        selectedPoles = state.selectedPoles,
                                        selectedResidualCurrent = state.selectedResidualCurrent,
                                        onBack = {
                                            showRcdThirdWindow = false
                                            showRcdSecondWindow = true
                                        },
                                        onDismiss = {
                                            showRcdThirdWindow = false
                                            tempBreakerResult = null
                                            breakerDialogConsumerIndex = null
                                        },
                                        onChoose = { rcdResultFromWindow ->
                                            val correctedRcdString = rcdResultFromWindow.replace(Regex("(\\d)\\sA"), "$1A")

                                            val breakerPart = tempBreakerResult ?: ""

                                            val combinedResult = "$breakerPart\n\n$correctedRcdString"

                                            consumer.let {
                                                it.protectionDevice = combinedResult
                                                it.protectionPoles = state.selectedPoles ?: ""
                                                saveNow()
                                            }

                                            showRcdThirdWindow = false
                                            breakerDialogConsumerIndex = null
                                            tempBreakerResult = null
                                            isComboMode = false
                                        }

                                    )
                                }
                            }
                        }

                        // Окно выбора марки кабеля
                        if (showCableTypeDialog) {
                            CableTypePopup(
                                onDismissRequest = {
                                    showCableTypeDialog = false
                                    cableDialogConsumerIndex = null
                                },
                                onConfirm = { selectedType ->
                                    val idx = cableDialogConsumerIndex
                                    if (idx != null) {
                                        val consumer = data.consumers.getOrNull(idx)
                                        consumer?.let {
                                            it.cableType = selectedType
                                            saveNow()
                                        }
                                    }
                                    showCableTypeDialog = false
                                    cableDialogConsumerIndex = null
                                }
                            )
                        }
                        // --- Окна АВР ---
                        if (showAtsSecondWindow) {
                            val st = atsState.value
                            AtsSecondWindow(
                                initialManufacturer = st.manufacturer,
                                initialSeries = st.series,
                                initialSelectedPoles = st.selectedPoles,
                                consumerVoltageStr = "400", // АВР обычно на 3 фазы
                                onBack = {
                                    showAtsSecondWindow = false
                                    showInputTypeDialog = true
                                },
                                onDismiss = { showAtsSecondWindow = false },
                                onConfirm = { res ->
                                    atsState.value = st.copy(
                                        series = res.series,
                                        selectedPoles = res.selectedPoles
                                    )
                                    showAtsSecondWindow = false
                                    showAtsThirdWindow = true
                                }
                            )
                        }

                        if (showAtsThirdWindow) {
                            val st = atsState.value
                            AtsThirdWindow(
                                maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                                consumerCurrentAStr = data.totalCurrent,
                                selectedSeries = st.series,
                                selectedPoles = st.selectedPoles,
                                onBack = {
                                    showAtsThirdWindow = false
                                    showAtsSecondWindow = true
                                },
                                onDismiss = { showAtsThirdWindow = false },
                                onChoose = { resultStr ->
                                    // Сохраняем результат в ячейку
                                    val header = selectedInputType?.title ?: "АВР"
                                    val separator = "---------------------------------"
                                    val fullText = "$header\n$separator\n$resultStr"
                                    data.inputInfo = fullText
                                    saveNow()
                                    showAtsThirdWindow = false
                                }
                            )
                        }

                        if (showInputParamsWindow) {
                            InputParamsWindow(
                                onBack = {
                                    showInputParamsWindow = false
                                    showInputTypeDialog = true // Возврат к выбору типа устройства
                                },
                                onDismiss = { showInputParamsWindow = false },
                                onNext = { count, bypass ->
                                    inputParamsResult = count to bypass

                                    inputBreakerState.value = BreakerDialogState(
                                        manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                    )
                                    showInputParamsWindow = false
                                    showInputBreakerSecond = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Эффект для автоматического перерасчёта данных щита
    LaunchedEffect(data.consumers.toList(), data.demandFactor, data.simultaneityFactor) {
        // Коэффициенты, вводимые пользователем
        val demandFactor = data.demandFactor.toFloatOrNull() ?: 1.0f
        val simultaneityFactor = data.simultaneityFactor.toFloatOrNull() ?: 1.0f

        // Б) Установленная мощность
        val totalInstalled = data.consumers.sumOf { it.installedPowerW.toDoubleOrNull() ?: 0.0 }
        data.totalInstalledPower = "%.2f".format(totalInstalled)

        // В) Расчетная мощность
        val totalCalculated =
            data.consumers.sumOf { it.powerKw.toDoubleOrNull() ?: 0.0 } * demandFactor * simultaneityFactor
        data.totalCalculatedPower = "%.2f".format(totalCalculated)

        // Г) Средний cos(f)
        val cosPhiValues = data.consumers.mapNotNull { it.cosPhi.toDoubleOrNull() }
        val avgCosPhi = if (cosPhiValues.isNotEmpty()) cosPhiValues.average() else 0.0
        data.averageCosPhi = "%.3f".format(avgCosPhi)

        // Д) Ток
        val totalCurrent = if (avgCosPhi > 0) {
            totalCalculated / (1.732 * 400 * avgCosPhi) // sqrt(3) ≈ 1.732, U = 400В
        } else {
            0.0
        }
        data.totalCurrent = "%.2f".format(totalCurrent)

        // Е) Коэф. спроса щита
        val shieldDemand = if (totalCalculated > 0) totalInstalled / totalCalculated else 0.0
        data.shieldDemandFactor = "%.2f".format(shieldDemand)
    }
}