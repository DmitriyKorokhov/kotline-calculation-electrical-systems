package ui.screens.shieldeditor.protection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import data.repository.getDistinctRcdSeries
import data.repository.getDistinctRcdSeriesByManufacturer
import data.repository.getRcdVariantsBySeries
import androidx.compose.ui.graphics.Color

data class RcdSelectionResult(
    val series: String,
    val selectedPoles: String,
    val selectedResidualCurrent: String
)

/**
 * Второе окно — выбор параметров УЗО (RCD).
 * Поля: Серия, Количество полюсов, Остаточный ток.
 */
@Composable
fun RcdSecondWindow(
    initialManufacturer: String? = null,
    initialSeries: String? = null,
    initialSelectedPoles: String? = null,
    initialSelectedResidualCurrent: String? = null,
    consumerVoltageStr: String? = null,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (RcdSelectionResult) -> Unit
) {
    var seriesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var seriesLoadingError by remember { mutableStateOf<String?>(null) }

    var selectedSeries by remember { mutableStateOf(initialSeries ?: "") }

    // Опции
    var polesOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var residualCurrentOptions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Выбранные значения
    var selectedPoles by remember { mutableStateOf(initialSelectedPoles) }
    var selectedResidualCurrent by remember { mutableStateOf(initialSelectedResidualCurrent) }

    // Фильтр полюсов по напряжению
    fun allowedPolesForVoltage(voltage: String?): Set<String>? {
        if (voltage.isNullOrBlank()) return null
        val v = voltage.filter { it.isDigit() }
        return when {
            v.startsWith("230") -> setOf("2P") // Для 1 фазы УЗО обычно 2P (фаза + ноль)
            v.startsWith("400") -> setOf("4P") // Для 3 фаз УЗО обычно 4P
            else -> null
        }
    }

    // 1. Загрузка серий
    LaunchedEffect(initialManufacturer) {
        try {
            seriesList = if (!initialManufacturer.isNullOrBlank()) {
                getDistinctRcdSeriesByManufacturer(initialManufacturer)
            } else {
                getDistinctRcdSeries()
            }

            // Предвыбор серии
            if (initialSeries != null && seriesList.contains(initialSeries)) {
                selectedSeries = initialSeries
            } else if (selectedSeries.isBlank() && seriesList.isNotEmpty()) {
                selectedSeries = seriesList.first()
            }
        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке серий УЗО"
        }
    }

    // 2. Загрузка вариантов при выборе серии
    LaunchedEffect(selectedSeries, consumerVoltageStr) {
        if (selectedSeries.isBlank()) return@LaunchedEffect
        try {
            val loaded = getRcdVariantsBySeries(selectedSeries)

            val polesSet = mutableSetOf<String>()
            val residualSet = mutableSetOf<String>()

            loaded.forEach { pair ->
                val variant = pair.second

                // Полюса
                // Если в базе poles хранится как "4P" или "2P"
                val p = variant.poles
                if (p.isNotBlank()) polesSet.add(p)

                // Остаточный ток (например "30 мА")
                val res = variant.ratedResidualCurrent
                if (res.isNotBlank()) residualSet.add(res)
            }

            // Фильтрация полюсов по напряжению
            val allowed = allowedPolesForVoltage(consumerVoltageStr)
            val filteredPoles = if (allowed != null) polesSet.filter { it in allowed } else polesSet.toList()

            // Сортировка
            polesOptions = filteredPoles.sortedWith(compareBy {
                it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            })

            residualCurrentOptions = residualSet.toList().sortedBy {
                it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            }

            // Автовыбор первого варианта, если ничего не выбрано
            if (selectedPoles == null && polesOptions.isNotEmpty()) selectedPoles = polesOptions.first()
            if (selectedResidualCurrent == null && residualCurrentOptions.isNotEmpty()) selectedResidualCurrent = residualCurrentOptions.first()

        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке вариантов УЗО"
        }
    }

    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = true)) {
        Card(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 480.dp)
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Параметры УЗО (RCD)", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))

                // --- Серия ---
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

                // Полюса
                Text("Количество полюсов", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (polesOptions.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = Color.Gray)
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

                // Остаточный ток
                Text("Остаточный ток (Утечка)", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(4.dp))
                if (residualCurrentOptions.isEmpty()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = Color.Gray)
                } else {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        residualCurrentOptions.forEach { res ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp).clickable { selectedResidualCurrent = res }
                            ) {
                                RadioButton(selected = selectedResidualCurrent == res, onClick = { selectedResidualCurrent = res })
                                Text(res)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Кнопки
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onBack) { Text("Назад") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val res = RcdSelectionResult(
                            series = selectedSeries,
                            selectedPoles = selectedPoles ?: polesOptions.firstOrNull() ?: "",
                            selectedResidualCurrent = selectedResidualCurrent ?: ""
                        )
                        onConfirm(res)
                    }) { Text("Далее") }
                }

                if (seriesLoadingError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Ошибка: $seriesLoadingError", color = MaterialTheme.colors.error)
                }
            }
        }
    }
}


