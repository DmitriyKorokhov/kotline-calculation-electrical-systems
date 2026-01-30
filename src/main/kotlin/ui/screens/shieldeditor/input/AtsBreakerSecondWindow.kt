package ui.screens.shieldeditor.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import data.repository.getDistinctSeries
import data.repository.getDistinctSeriesByManufacturer
import data.repository.getVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions
import ui.screens.shieldeditor.protection.BreakerSelectionResult

/**
 * Окно выбора параметров автомата для АВР (Шаг 1).
 * Логика идентична BreakerSecondWindow, но напряжение всегда считается 3-фазным (400В).
 */
@Composable
fun AtsBreakerSecondWindow(
    initialManufacturer: String? = null,
    initialSeries: String? = null,
    initialSelectedAdditions: List<String> = emptyList(),
    initialSelectedPoles: String? = null,
    initialSelectedCurve: String? = null,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (BreakerSelectionResult) -> Unit
) {
    var seriesList by remember { mutableStateOf<List<String>>(emptyList()) }
    var seriesLoadingError by remember { mutableStateOf<String?>(null) }
    var selectedSeries by remember { mutableStateOf(initialSeries ?: "") }
    var curvesOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var additionsOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var polesOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCurve by remember { mutableStateOf(initialSelectedCurve) }
    var selectedPoles by remember { mutableStateOf(initialSelectedPoles) }
    var selectedAdditions by remember { mutableStateOf(initialSelectedAdditions.toSet()) }

    // Жестко задаем логику для 400В (АВР ввода обычно трехфазный)
    fun allowedPolesForAts(): Set<String> {
        return setOf("3P", "3P+N", "4P")
    }

    LaunchedEffect(initialManufacturer) {
        try {
            seriesList = if (!initialManufacturer.isNullOrBlank()) {
                getDistinctSeriesByManufacturer(initialManufacturer)
            } else {
                getDistinctSeries()
            }
            if (initialSeries != null && seriesList.contains(initialSeries)) {
                selectedSeries = initialSeries
            } else if (selectedSeries.isBlank() && seriesList.isNotEmpty()) {
                selectedSeries = seriesList.first()
            }
        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке серий"
        }
    }

    LaunchedEffect(selectedSeries) {
        if (selectedSeries.isBlank()) return@LaunchedEffect
        try {
            val loaded = getVariantsBySeries(selectedSeries)
            val curves = mutableSetOf<String>()
            val additions = mutableSetOf<String>()
            val polesSet = mutableSetOf<String>()

            loaded.forEach { pair ->
                val variant = pair.second
                parseCurveFromAdditions(variant.additions)?.let { curves.add(it) }
                parseExtrasFromAdditions(variant.additions).forEach { additions.add(it) }
                val pt = variant.polesText
                if (!pt.isNullOrBlank()) {
                    pt.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { polesSet.add(it) }
                }
            }

            // ИЗМЕНЕНИЕ 2: Фильтруем полюса через allowedPolesForAts
            val allowed = allowedPolesForAts()
            val filteredPoles = polesSet.filter { it in allowed } // Только 3P/4P

            curvesOptions = curves.toList().sorted()
            additionsOptions = additions.toList().sorted()
            polesOptions = filteredPoles.sortedWith(compareBy {
                it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            })

            if (selectedCurve == null && curvesOptions.isNotEmpty()) selectedCurve = curvesOptions.first()
            if (selectedPoles == null && polesOptions.isNotEmpty()) selectedPoles = polesOptions.first()
            selectedAdditions = selectedAdditions.intersect(additionsOptions.toSet())

        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке вариантов"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Параметры автоматического выключателя", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(16.dp))

        Text("Серия", style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(6.dp))
        var expanded by remember { mutableStateOf(false) }
        var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

        Box {
            OutlinedTextField(
                value = selectedSeries,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .width(300.dp)
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
                modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() }),
                properties = PopupProperties(focusable = true)
            ) {
                seriesList.forEach { s ->
                    DropdownMenuItem(onClick = {
                        selectedSeries = s
                        expanded = false
                    }) {
                        Text(s)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Дополнения", style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(6.dp))
        if (additionsOptions.isEmpty()) {
            Text("Нет доступных дополнений", style = MaterialTheme.typography.body2, color = Color.Gray)
        } else {
            Column {
                additionsOptions.forEach { add ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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

        Text("Количество полюсов", style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(6.dp))
        if (polesOptions.isEmpty()) {
            Text("Нет данных по полюсам", style = MaterialTheme.typography.body2, color = Color.Gray)
        } else {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                polesOptions.forEach { p ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp).clickable { selectedPoles = p }
                    ) {
                        RadioButton(selected = selectedPoles == p, onClick = { selectedPoles = p })
                        Spacer(Modifier.width(6.dp))
                        Text(p)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Кривая отключения", style = MaterialTheme.typography.subtitle2)
        Spacer(Modifier.height(6.dp))
        if (curvesOptions.isEmpty()) {
            Text("Нет данных по кривым", style = MaterialTheme.typography.body2, color = Color.Gray)
        } else {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                curvesOptions.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp).clickable { selectedCurve = c }
                    ) {
                        RadioButton(selected = selectedCurve == c, onClick = { selectedCurve = c })
                        Spacer(Modifier.width(6.dp))
                        Text(c)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                val poles = selectedPoles ?: polesOptions.firstOrNull() ?: ""
                val res = BreakerSelectionResult(
                    series = selectedSeries,
                    selectedAdditions = selectedAdditions.toList(),
                    selectedPoles = poles,
                    selectedCurve = selectedCurve
                )
                onConfirm(res)
            }) {
                Text("Далее")
            }
        }

        seriesLoadingError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text("Ошибка: $err", color = MaterialTheme.colors.error)
        }
    }
}
