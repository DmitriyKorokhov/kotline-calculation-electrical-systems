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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ui.screens.shieldeditor.calculation.CalculationEngine
import ui.screens.shieldeditor.calculation.PhaseDistributor
import ui.screens.shieldeditor.components.*
import ui.screens.shieldeditor.components.topbar.CalculationWindow
import ui.screens.shieldeditor.dialogs.AddConsumerDialog
import ui.screens.shieldeditor.dialogs.AtsDialogState
import ui.screens.shieldeditor.exporter.ExportEditor
import ui.screens.shieldeditor.input.AtsSecondWindow
import ui.screens.shieldeditor.input.AtsThirdWindow
import ui.screens.shieldeditor.input.InputParamsWindow
import ui.screens.shieldeditor.input.InputType
import ui.screens.shieldeditor.input.InputTypePopup
import ui.screens.shieldeditor.protection.*

private val LEFT_PANEL_WIDTH: Dp = 300.dp
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

    // Когда пользователь кликает по ячейке -> показываем первый popup (там у вас protectionDialogForIndex)
    var protectionDialogForIndex by remember { mutableStateOf<Int?>(null) }

    var showAtsSecondWindow by remember { mutableStateOf(false) }
    var showAtsThirdWindow by remember { mutableStateOf(false) }

    val atsState = remember { mutableStateOf(AtsDialogState()) }

    var currentSidebarTab by remember { mutableStateOf(SidebarTab.PROJECT) }

    data class BreakerDialogState(
        val manufacturer: String? = null,
        val series: String? = null,
        val selectedAdditions: List<String> = emptyList(),
        val selectedPoles: String? = null,
        val selectedCurve: String? = null
    )

    // Буфер для копирования выбранных потребителей
    val copiedConsumers = remember { mutableStateListOf<ConsumerModel>() }

    // Диалог «Добавить»
    var showAddDialog by remember { mutableStateOf(false) }

    var showInputTypeDialog by remember { mutableStateOf(false) }
    var selectedInputType by remember { mutableStateOf<InputType?>(null) }

    // Состояния для окон автомата (используем те же переменные для данных, но флаги видимости свои)
    var showInputBreakerSecond by remember { mutableStateOf(false) }
    var showInputBreakerThird by remember { mutableStateOf(false) }

    // Временное хранилище параметров автомата ввода
    val inputBreakerState = remember { mutableStateOf(BreakerDialogState()) }

    var showInputParamsWindow by remember { mutableStateOf(false) }
    var inputParamsResult by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    var showCalculationWindow by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(Modifier.fillMaxSize()) {
            // Top bar сверху
            ShieldTopBar(
                shieldId = shieldId,
                onBack = onBack,
                onSave = { saveNow() },
                onExportDwg = { ExportEditor.startDwgExport(data) },
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
                                        saveNow()
                                    },
                                    onOpenInputTypeDialog = { showInputTypeDialog = true }
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
                                                        onShowAddDialog = {
                                                            showAddDialog = true
                                                        }
                                                    )
                                                },
                                                onHeaderClick = {
                                                    if (selectedColumns.contains(colIndex)) selectedColumns.remove(
                                                        colIndex
                                                    )
                                                    else selectedColumns.add(colIndex)
                                                },
                                                onDataChanged = { saveNow() },
                                                onCalculationRequired = { CalculationEngine.calculateAll(data); saveNow() },
                                                onOpenProtectionDialog = { protectionDialogForIndex = colIndex }
                                            )
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
                                            inputBreakerState.value = BreakerDialogState(
                                                manufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() }
                                            )
                                            showInputBreakerSecond = true
                                        } else if (type == InputType.ATS_BLOCK_TWO_INPUTS) {
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
                                    onBack = {
                                        showInputBreakerSecond = false
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
                                protectionThreshold = data.protectionCurrentThreshold.toFloatOrNull() ?: 40f,
                                protectionFactorLow = data.protectionFactorLow.toFloatOrNull() ?: 0.87f,
                                protectionFactorHigh = data.protectionFactorHigh.toFloatOrNull() ?: 0.93f,
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

                        if (showAddDialog) {
                            AddConsumerDialog(
                                onDismiss = { showAddDialog = false },
                                onConfirm = { countStr ->
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
                                        saveNow()
                                    }
                                    showAddDialog = false
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

        if (showCalculationWindow) {
            CalculationWindow(
                data = data,
                onSave = { saveNow() },
                onDismiss = { showCalculationWindow = false }
            )
        }

        if (protectionDialogForIndex != null) {
            val idx = protectionDialogForIndex!!
            val consumer = data.consumers.getOrNull(idx)

            if (consumer != null) {
                // Определяем начальный тип (если уже выбрано, пытаемся распарсить)
                val initialType = protectionTypeFromString(consumer.protectionDevice)

                ProtectionSelectionWindow(
                    data = data,
                    initialType = initialType,
                    consumerCurrentAStr = consumer.currentA,
                    consumerVoltageStr = consumer.voltage,
                    maxShortCircuitCurrentStr = data.maxShortCircuitCurrent,
                    onDismiss = { protectionDialogForIndex = null },
                    onSelect = { resultString, poles ->
                        // Сохраняем результат
                        val correctedString = resultString.replace(Regex("(\\d)\\sA"), "$1A")
                        consumer.protectionDevice = correctedString
                        consumer.protectionPoles = poles

                        // Сброс и сохранение
                        protectionDialogForIndex = null
                        saveNow()
                    }
                )
            } else {
                protectionDialogForIndex = null
            }
        }
    }


    // Эффект для автоматического перерасчёта данных щита
    LaunchedEffect(data.consumers.toList(), data.demandFactor, data.simultaneityFactor) {
        CalculationEngine.calculateAll(data)
        saveNow()
    }
}

private fun handleContextAction(
    action: ContextMenuAction,
    data: ShieldData,
    selectedColumns: MutableList<Int>,
    copiedConsumers: MutableList<ConsumerModel>,
    saveNow: () -> Unit,
    onShowAddDialog: () -> Unit
) {
    when (action) {
        ContextMenuAction.DELETE -> {
            if (selectedColumns.isNotEmpty()) {
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