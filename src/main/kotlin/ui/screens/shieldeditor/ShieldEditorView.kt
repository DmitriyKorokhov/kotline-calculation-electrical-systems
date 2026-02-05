package ui.screens.shieldeditor

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.calculation.CableCalculator
import ui.screens.shieldeditor.calculation.CalculationEngine
import ui.screens.shieldeditor.calculation.PhaseDistributor
import ui.screens.shieldeditor.components.*
import ui.screens.shieldeditor.components.topbar.CalculationWindow
import ui.screens.shieldeditor.dialogs.AddConsumerDialog
import ui.screens.shieldeditor.exporter.ExportDialog
import ui.screens.shieldeditor.exporter.ExportEditor
import ui.screens.shieldeditor.input.InputTypePopup
import ui.screens.shieldeditor.protection.ProtectionSelectionWindow
import ui.screens.shieldeditor.protection.protectionTypeFromString
import ui.utils.HistoryManager

private val LEFT_PANEL_WIDTH: Dp = 300.dp
private val SCROLLBAR_HEIGHT: Dp = 22.dp

private data class DialogTarget(val colIndex: Int, val protectionIndex: Int)

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ShieldEditorView(shieldId: Int?, onBack: () -> Unit) {
    // данные щита (ShieldData поля уже mutableStateOf)
    val data = remember { ShieldStorage.loadOrCreate(shieldId) }
    // Менеджер истории
    val historyManager = remember { HistoryManager() }
    // Триггер для сброса флагов в текстовых полях
    var historyTrigger by remember { mutableStateOf(0) }

    fun pushHistory(isDiscrete: Boolean = false) {
        historyManager.pushState(data)
        if (isDiscrete) {
            historyTrigger++
        }
    }

    val borderColor = Color.White
    val textColor = Color.White

    fun saveNow() = ShieldStorage.save(shieldId, data)

    // используем сохранённое состояние панели
    var metaExpanded by remember { mutableStateOf(false) }

    // при изменении — сохраняем в ShieldData
    LaunchedEffect(metaExpanded) {
        data.metaExpanded = metaExpanded
        saveNow()
    }

    // Scroll states
    val hScrollState = rememberScrollState() // горизонтальный для колонок
    val vScrollState = rememberScrollState() // вертикальный для всей таблицы
    val selectedColumns = remember { mutableStateListOf<Int>() }
    // Анимируем ширину панели — это даёт плавный "сдвиг" таблицы
    val animatedPanelWidth by animateDpAsState(
        targetValue = if (metaExpanded) LEFT_PANEL_WIDTH else 0.dp,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
    )

    var protectionDialogTarget by remember { mutableStateOf<DialogTarget?>(null) }

    var currentSidebarTab by remember { mutableStateOf(SidebarTab.PROJECT) }

    // Буфер для копирования выбранных потребителей
    val copiedConsumers = remember { mutableStateListOf<ConsumerModel>() }

    // Диалог «Добавить»
    var showAddDialog by remember { mutableStateOf(false) }

    var showInputTypeDialog by remember { mutableStateOf(false) }

    var showCalculationWindow by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var showExportDialog by remember { mutableStateOf(false) }

    // Запрашиваем фокус при запуске экрана, чтобы ловить нажатия клавиш
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    // Если нажали ESC и есть выделенные колонки -> очищаем
                    if (selectedColumns.isNotEmpty()) {
                        selectedColumns.clear()
                        return@onPreviewKeyEvent true
                    }
                }
                // ОБРАБОТКА CTRL+Z / CTRL+SHIFT+Z
                if (event.key == Key.Z && event.type == KeyEventType.KeyDown) {
                    if (event.isCtrlPressed) {
                        if (event.isShiftPressed) {
                            historyManager.redo(data)
                            historyTrigger++
                            CalculationEngine.calculateAll(data)
                            data.consumers.forEach {
                                CableCalculator.calculateCable(it, data)
                                CableCalculator.calculateVoltageDrop(it, data)
                                CableCalculator.calculateShortCircuitCurrent(it, data)
                            }
                            saveNow()
                        } else {
                            historyManager.undo(data)
                            historyTrigger++
                            CalculationEngine.calculateAll(data)
                            data.consumers.forEach {
                                CableCalculator.calculateCable(it, data)
                                CableCalculator.calculateVoltageDrop(it, data)
                                CableCalculator.calculateShortCircuitCurrent(it, data)
                            }
                            saveNow()
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top bar сверху
            ShieldTopBar(
                shieldId = shieldId,
                onBack = onBack,
                onSave = { saveNow() },
                onExportDwg = { showExportDialog = true },
                onCalculationClick = { showCalculationWindow = !showCalculationWindow }
            )

            Row(Modifier.fillMaxSize()) {
                // 1. Сайдбар слева
                ShieldLeftSidebar(
                    selectedTab = if (metaExpanded) currentSidebarTab else null,

                    onTabSelected = { newTab ->
                        if (currentSidebarTab == newTab && metaExpanded) {
                            metaExpanded = false
                        } else {
                            currentSidebarTab = newTab
                            metaExpanded = true
                        }
                    }
                )

                // 2. Выезжающая панель
                Box(
                    modifier = Modifier
                        .width(animatedPanelWidth)
                        .fillMaxHeight()
                        .zIndex(if (metaExpanded) 1f else 0f)
                ) {
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
                        when (currentSidebarTab) {
                            SidebarTab.SHIELD_DATA -> {
                                ShieldLeftPanel(
                                    data = data,
                                    onSave = { saveNow() },
                                    onCalculate = {
                                        CalculationEngine.calculateAll(data)
                                        PhaseDistributor.distributePhases(data)
                                        data.consumers.forEach { consumer ->
                                            CableCalculator.calculateVoltageDrop(consumer, data)
                                            CableCalculator.calculateShortCircuitCurrent(consumer, data)
                                        }
                                        saveNow()
                                    },
                                    onOpenInputTypeDialog = { showInputTypeDialog = true },
                                    onPushHistory = { isDiscrete -> pushHistory(isDiscrete) },
                                    historyTrigger = historyTrigger
                                )
                            }

                            SidebarTab.PROJECT -> {
                                // Здесь будет панель проекта
                                // ProjectLeftPanel(...)
                                // Пока заглушка
                                Box(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.1f))) {
                                    Text("Панель проекта", Modifier.align(Alignment.Center), color = Color.White)
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 4.dp)
                ) {
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
                                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
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
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        if (event.buttons.isTertiaryPressed) {
                                                            val change = event.changes.firstOrNull()
                                                            if (change != null) {
                                                                val dragAmount = change.position - change.previousPosition
                                                                if (dragAmount.x != 0f) {
                                                                    hScrollState.dispatchRawDelta(-dragAmount.x)
                                                                    change.consume()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Spacer(modifier = Modifier.width(6.dp))

                                        data.consumers.forEachIndexed { colIndex, consumer ->
                                            ShieldTableColumn(
                                                index = colIndex,
                                                consumer = consumer,
                                                isSelected = selectedColumns.contains(colIndex),
                                                borderColor = borderColor,
                                                textColor = textColor,
                                                isPasteEnabled = copiedConsumers.isNotEmpty() && selectedColumns.size == 1,
                                                isAddEnabled = selectedColumns.size == 1,
                                                onContextAction = { action ->
                                                    handleContextAction(
                                                        action = action,
                                                        data = data,
                                                        selectedColumns = selectedColumns,
                                                        copiedConsumers = copiedConsumers,
                                                        saveNow = { saveNow() },
                                                        onShowAddDialog = { showAddDialog = true },
                                                        onPushHistory = { pushHistory(true) }
                                                    )
                                                },
                                                onHeaderClick = {
                                                    if (selectedColumns.contains(colIndex)) selectedColumns.remove(
                                                        colIndex
                                                    )
                                                    else selectedColumns.add(colIndex)
                                                },
                                                onDataChanged = { saveNow() },
                                                onCalculationRequired = {
                                                    CalculationEngine.calculateAll(data)
                                                    data.consumers.forEach { consumer ->
                                                        CableCalculator.calculateCable(consumer, data)
                                                        CableCalculator.calculateVoltageDrop(consumer, data)
                                                        CableCalculator.calculateShortCircuitCurrent(consumer, data)
                                                    }
                                                    saveNow()
                                                },
                                                onRecalculateDropOnly = {
                                                    CableCalculator.calculateShortCircuitCurrent(consumer, data)
                                                    CableCalculator.calculateVoltageDrop(consumer, data)
                                                    saveNow()
                                                },
                                                onOpenProtectionDialog = { protIndex ->
                                                    protectionDialogTarget = DialogTarget(colIndex, protIndex)
                                                },
                                                data = data,
                                                onPushHistory = { isDiscrete -> pushHistory(isDiscrete) },
                                                historyTrigger = historyTrigger
                                            )
                                        }
                                    }
                                }
                            }

                            // Горизонтальный scrollbar снизу
                            HorizontalScrollbar(
                                adapter = rememberScrollbarAdapter(hScrollState),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(SCROLLBAR_HEIGHT)
                                    .padding(vertical = 6.dp)
                            )
                        }

                        if (showAddDialog) {
                            AddConsumerDialog(
                                onDismiss = { showAddDialog = false },
                                onConfirm = { countStr ->
                                    pushHistory(true)
                                    val targetIndex = selectedColumns.singleOrNull()
                                    if (countStr > 0 && targetIndex != null) {
                                        var insertPos = targetIndex + 1
                                        val newIndices = mutableListOf<Int>()
                                        repeat(countStr) {
                                            val newConsumer = ConsumerModel()
                                            if (insertPos <= data.consumers.size) {
                                                data.consumers.add(insertPos, newConsumer)
                                                newIndices.add(insertPos)
                                                insertPos++
                                            }
                                        }
                                        selectedColumns.clear()
                                        selectedColumns.addAll(newIndices)

                                        CalculationEngine.calculateAll(data)
                                        data.consumers.forEach { consumer ->
                                            CableCalculator.calculateCable(consumer, data)
                                        }
                                        saveNow()
                                    }
                                    showAddDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showCalculationWindow) {
            CalculationWindow(
                data = data,
                onSave = { saveNow() },
                onDismiss = { showCalculationWindow = false },
                onPushHistory = { isDiscrete -> pushHistory(isDiscrete) },
                historyTrigger = historyTrigger
            )
        }

        if (protectionDialogTarget != null) {
            val target = protectionDialogTarget!!
            val consumer = data.consumers.getOrNull(target.colIndex)

            if (consumer != null) {
                // 1. Определяем текущее устройство и куда сохранять
                // Если индекс -1, работаем с основной защитой, иначе — с дополнительной
                val currentDeviceString = if (target.protectionIndex == -1) {
                    consumer.protectionDevice
                } else {
                    consumer.additionalProtections.getOrNull(target.protectionIndex)?.protectionDevice ?: ""
                }

                // Парсим начальный тип для диалога
                val initialType = protectionTypeFromString(currentDeviceString)

                ProtectionSelectionWindow(
                    data = data,
                    initialType = initialType,
                    consumerCurrentAStr = consumer.currentA,
                    consumerVoltageStr = consumer.voltage,
                    maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                    onDismiss = { protectionDialogTarget = null },
                    onSelect = { resultString, poles ->
                        pushHistory(true)

                        // Корректировка формата ампер (как было у вас)
                        val correctedString = resultString.replace(Regex("(\\d)\\sA"), "$1A")

                        // 2. Сохраняем в зависимости от индекса
                        if (target.protectionIndex == -1) {
                            // Основная
                            consumer.protectionDevice = correctedString
                            consumer.protectionPoles = poles
                        } else {
                            // Дополнительная
                            // Проверяем, существует ли еще эта защита (вдруг удалили пока окно было открыто)
                            val addProt = consumer.additionalProtections.getOrNull(target.protectionIndex)
                            if (addProt != null) {
                                addProt.protectionDevice = correctedString
                                addProt.protectionPoles = poles
                            }
                        }

                        protectionDialogTarget = null

                        // Пересчет
                        CalculationEngine.calculateAll(data)
                        CableCalculator.calculateCable(consumer, data)
                        CableCalculator.calculateShortCircuitCurrent(consumer, data)
                        saveNow()
                    }
                )
            } else {
                protectionDialogTarget = null
            }
        }

        if (showInputTypeDialog) {
            InputTypePopup(
                data = data,
                onDismissRequest = { showInputTypeDialog = false },
                onSave = { saveNow() }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                data = data,
                onDismissRequest = { showExportDialog = false },
                onExportAction = { type, format ->
                    if (type == "DWG") {
                        // Передаем пока без формата, или можно доработать ExportEditor, чтобы он учитывал формат
                        ExportEditor.startDwgExport(data, format)
                    } else {
                        // Заглушка для PDF
                        javax.swing.JOptionPane.showMessageDialog(null, "Экспорт в PDF пока не реализован.\nВыбран формат: $format")
                    }
                }
            )
        }
    }

    // Эффект для автоматического перерасчёта данных щита
    LaunchedEffect(data.consumers.toList(), data.demandFactor, data.simultaneityFactor) {
        CalculationEngine.calculateAll(data)
        data.consumers.forEach { consumer ->
            CableCalculator.calculateCable(consumer, data)
        }
        saveNow()
    }
}

private fun handleContextAction(
    action: ContextMenuAction,
    data: ShieldData,
    selectedColumns: MutableList<Int>,
    copiedConsumers: MutableList<ConsumerModel>,
    saveNow: () -> Unit,
    onShowAddDialog: () -> Unit,
    onPushHistory: () -> Unit
) {
    when (action) {
        ContextMenuAction.DELETE -> {
            if (selectedColumns.isNotEmpty()) {
                onPushHistory()
                selectedColumns.sortedDescending().forEach { idx ->
                    if (idx in data.consumers.indices) {
                        data.consumers.removeAt(idx)
                    }
                }
                selectedColumns.clear()
                saveNow()
            }
        }
        ContextMenuAction.COPY -> {
            copiedConsumers.clear()
            selectedColumns.sorted().forEach { idx ->
                data.consumers.getOrNull(idx)?.let { copiedConsumers.add(it.deepCopy()) }
            }
        }
        ContextMenuAction.PASTE -> {
            val target = selectedColumns.singleOrNull()
            if (target != null && copiedConsumers.isNotEmpty()) {
                onPushHistory()
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
                onShowAddDialog()
            }
        }
    }
}