package ui.screens.shieldeditor

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.animateDpAsState
import view.CompactOutlinedTextField

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

    // Анимируем ширину панели — это даёт плавный "сдвиг" таблицы
    val animatedPanelWidth by animateDpAsState(
        targetValue = if (metaExpanded) LEFT_PANEL_WIDTH else 0.dp,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
    )

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

            Spacer(Modifier.weight(1f))

            Text("Данные щита", color = textColor) // заменил заголовок
            Spacer(Modifier.width(12.dp))
            Text("Щит ID: ${shieldId ?: "-"}", fontSize = 14.sp, color = textColor)
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxSize()) {
            // Блок: контейнер с анимируемой шириной (если ширина 0 — колонка займет 0 места,
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
                                            Box(
                                                modifier = Modifier
                                                    .height(HEADER_HEIGHT)
                                                    .fillMaxWidth()
                                                    .padding(start = 6.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(text = "Потребитель ${colIndex + 1}", fontSize = HEADER_FONT.sp, color = textColor)
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

                                            CompactOutlinedTextField(
                                                label = "Устройство защиты и коммутации",
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
                                                modifier = Modifier.fillMaxWidth()
                                            )
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
                }
            }
        }
    }
}
