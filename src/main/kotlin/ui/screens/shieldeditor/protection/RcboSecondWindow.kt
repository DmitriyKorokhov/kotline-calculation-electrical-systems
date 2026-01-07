package ui.screens.shieldeditor.protection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.repository.getDistinctRcboSeries
import data.repository.getDistinctRcboSeriesByManufacturer
import data.repository.getRcboVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions

data class RcboSelectionResult(
    val series: String,
    val selectedAdditions: List<String>,
    val selectedPoles: String,
    val selectedCurve: String?,
    val selectedResidualCurrent: String // поле для остаточного тока
)

/**
 * Второе окно — выбор параметров АВДТ (RCBO).
 *
 * Аналогично BreakerSecondWindow, но добавляется выбор остаточного тока (Residual Current).
 */
@Composable
fun RcboSecondWindow(
    initialManufacturer: String? = null,
    initialSeries: String? = null,
    initialSelectedAdditions: List<String> = emptyList(),
    initialSelectedPoles: String? = null,
    initialSelectedCurve: String? = null,
    initialSelectedResidualCurrent: String? = null,
    consumerVoltageStr: String? = null,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (RcboSelectionResult) -> Unit
) {
    var seriesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var seriesLoadingError by remember { mutableStateOf<String?>(null) }

    var selectedSeries by remember { mutableStateOf(initialSeries ?: "") }

    // Списки опций
    var curvesOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var additionsOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var polesOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var residualCurrentOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Выбранные значения
    var selectedCurve by remember { mutableStateOf(initialSelectedCurve) }
    var selectedPoles by remember { mutableStateOf(initialSelectedPoles) }
    var selectedResidualCurrent by remember { mutableStateOf(initialSelectedResidualCurrent) }
    var selectedAdditions by remember { mutableStateOf(initialSelectedAdditions.toSet()) }

    // Фильтр полюсов по напряжению
    fun allowedPolesForVoltage(voltage: String?): Set<String>? {
        if (voltage.isNullOrBlank()) return null
        val v = voltage.filter { it.isDigit() }
        return when {
            v.startsWith("230") -> setOf("1P+N", "2P") // Для 1 фазы обычно 1P+N или 2P у дифов
            v.startsWith("400") -> setOf("3P", "3P+N", "4P")
            else -> null
        }
    }

    // Загрузка серий
    LaunchedEffect(initialManufacturer) {
        try {
            seriesList = if (!initialManufacturer.isNullOrBlank()) {
                getDistinctRcboSeriesByManufacturer(initialManufacturer)
            } else {
                getDistinctRcboSeries()
            }
            if (initialSeries != null && seriesList.contains(initialSeries)) {
                selectedSeries = initialSeries
            } else if (selectedSeries.isBlank() && seriesList.isNotEmpty()) {
                selectedSeries = seriesList.first()
            }
        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке серий АВДТ"
        }
    }

    // Загрузка вариантов при смене серии
    LaunchedEffect(selectedSeries, consumerVoltageStr) {
        if (selectedSeries.isBlank()) return@LaunchedEffect
        try {
            val loaded = getRcboVariantsBySeries(selectedSeries)

            val curves = mutableSetOf<String>()
            val additions = mutableSetOf<String>()
            val polesSet = mutableSetOf<String>()
            val residualSet = mutableSetOf<String>()

            loaded.forEach { pair ->
                val variant = pair.second

                // Парсим кривую и дополнения из поля additions
                parseCurveFromAdditions(variant.additions)?.let { curves.add(it) }
                parseExtrasFromAdditions(variant.additions).forEach { additions.add(it) }

                // Полюса (теперь это varchar)
                val pt = variant.poles
                if (pt.isNotBlank()) {
                    pt.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { polesSet.add(it) }
                }

                // Остаточный ток
                if (variant.ratedResidualCurrent.isNotBlank()) {
                    residualSet.add(variant.ratedResidualCurrent)
                }
            }

            // Фильтрация полюсов
            val allowed = allowedPolesForVoltage(consumerVoltageStr)
            val filteredPoles = if (allowed != null) polesSet.filter { it in allowed } else polesSet.toList()

            curvesOptions = curves.toList().sorted()
            additionsOptions = additions.toList().sorted()

            // Сортировка полюсов: сначала цифры
            polesOptions = filteredPoles.sortedWith(compareBy {
                it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            })

            // Сортировка тока утечки (парсим число из строки "30 мА")
            residualCurrentOptions = residualSet.toList().sortedBy {
                it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            }

            // Восстановление или сброс выбора
            if (selectedCurve == null && curvesOptions.isNotEmpty()) selectedCurve = curvesOptions.first()
            if (selectedPoles == null && polesOptions.isNotEmpty()) selectedPoles = polesOptions.first()
            if (selectedResidualCurrent == null && residualCurrentOptions.isNotEmpty()) selectedResidualCurrent = residualCurrentOptions.first()

            selectedAdditions = selectedAdditions.intersect(additionsOptions.toSet())

        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке вариантов АВДТ"
        }
    }

    // UI Окна
    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = true)) {
        Card(
            modifier = Modifier
                .widthIn(min = 340.dp, max = 500.dp)
                .padding(12.dp)
                .heightIn(max = 700.dp), // Ограничиваем высоту, чтобы влезло на экран
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) // Скролл для всего содержимого
            ) {
                Text("Параметры АВДТ", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))

                // --- Серия (Dropdown) ---
                Text("Серия", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                var expanded by remember { mutableStateOf(false) }
                var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

                Box {
                    OutlinedTextField(
                        value = selectedSeries,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates -> textFieldSize = coordinates.size.toSize() }
                            .clickable { expanded = true },
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                    ) {
                        seriesList.forEach { s ->
                            DropdownMenuItem(onClick = {
                                selectedSeries = s
                                expanded = false
                            }) { Text(s) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Остаточный ток
                Text("Остаточный ток (Утечка)", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (residualCurrentOptions.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        residualCurrentOptions.forEach { resCur ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { selectedResidualCurrent = resCur }
                            ) {
                                RadioButton(
                                    selected = (selectedResidualCurrent == resCur),
                                    onClick = { selectedResidualCurrent = resCur }
                                )
                                Text(text = resCur, style = MaterialTheme.typography.body1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Полюса
                Text("Количество полюсов", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (polesOptions.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        polesOptions.forEach { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp).clickable { selectedPoles = p }
                            ) {
                                RadioButton(selected = selectedPoles == p, onClick = { selectedPoles = p })
                                Text(p)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Кривая
                Text("Кривая отключения", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (curvesOptions.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        curvesOptions.forEach { c ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp).clickable { selectedCurve = c }
                            ) {
                                RadioButton(selected = selectedCurve == c, onClick = { selectedCurve = c })
                                Text(c)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Дополнения
                Text("Дополнения", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (additionsOptions.isEmpty()) {
                    Text("Нет доступных дополнений", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                } else {
                    Column {
                        additionsOptions.forEach { add ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .toggleable(
                                        value = selectedAdditions.contains(add),
                                        onValueChange = { checked ->
                                            selectedAdditions = if (checked) selectedAdditions + add else selectedAdditions - add
                                        }
                                    )
                            ) {
                                Checkbox(checked = selectedAdditions.contains(add), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(add)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Кнопки
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onBack) { Text("Назад") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val res = RcboSelectionResult(
                            series = selectedSeries,
                            selectedAdditions = selectedAdditions.toList(),
                            selectedPoles = selectedPoles ?: polesOptions.firstOrNull() ?: "",
                            selectedCurve = selectedCurve,
                            selectedResidualCurrent = selectedResidualCurrent ?: ""
                        )
                        onConfirm(res)
                    }) { Text("Далее") }
                }

                seriesLoadingError?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text("Ошибка: $err", color = MaterialTheme.colors.error)
                }
            }
        }
    }
}

