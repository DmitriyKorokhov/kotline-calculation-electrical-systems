package ui.screens.shieldeditor.protection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.repository.getDistinctSeries
import data.repository.getDistinctSeriesByManufacturer
import data.repository.getVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions

data class BreakerSelectionResult(
    val series: String,
    val selectedAdditions: List<String>,
    val selectedPoles: String,
    val selectedCurve: String?
)

/**
 * Второе окно — выбор параметров Автоматического выключателя.
 *
 * Новые параметры:
 * - initialManufacturer: если задано, подгрузим только серии этого производителя;
 * - initialSelectedAdditions / initialSelectedPoles / initialSelectedCurve — начальный выбор, чтобы при возврате он сохранялся.
 *
 * @param consumerVoltageStr напряжение потребителя ("230" или "400" или "230V" и т.п.)
 */
@Composable
fun BreakerSecondWindow(
    initialManufacturer: String? = null,
    initialSeries: String? = null,
    initialSelectedAdditions: List<String> = emptyList(),
    initialSelectedPoles: String? = null,
    initialSelectedCurve: String? = null,
    consumerVoltageStr: String? = null,
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

    var selectedCurve by remember { mutableStateOf<String?>(initialSelectedCurve) }
    var selectedPoles by remember { mutableStateOf<String?>(initialSelectedPoles) }
    var selectedAdditions by remember { mutableStateOf(initialSelectedAdditions.toSet()) }

    // allowed poles by voltage
    fun allowedPolesForVoltage(voltage: String?): Set<String>? {
        if (voltage.isNullOrBlank()) return null
        val v = voltage.filter { it.isDigit() }
        return when {
            v.startsWith("230") -> setOf("1P", "1P+N", "2P")
            v.startsWith("400") -> setOf("3P", "3P+N", "4P")
            else -> null
        }
    }

    // load series: if initialManufacturer provided -> load only series of that manufacturer
    LaunchedEffect(initialManufacturer) {
        try {
            seriesList = if (!initialManufacturer.isNullOrBlank()) {
                getDistinctSeriesByManufacturer(initialManufacturer)
            } else {
                getDistinctSeries()
            }
            // ставим selectedSeries только если он присутствует в db
            if (initialSeries != null && seriesList.contains(initialSeries)) {
                selectedSeries = initialSeries
            } else if (selectedSeries.isBlank() && seriesList.isNotEmpty()) {
                selectedSeries = seriesList.first()
            }
        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке серий"
        }
    }

    // when series or voltage changes -> load variants and compute options (and apply voltage-based filtering)
    LaunchedEffect(selectedSeries, consumerVoltageStr) {
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

            val allowed = allowedPolesForVoltage(consumerVoltageStr)
            val filteredPoles = if (allowed != null) polesSet.filter { it in allowed } else polesSet.toList()

            curvesOptions = curves.toList().sorted()
            additionsOptions = additions.toList().sorted()
            polesOptions = filteredPoles.sortedWith(compareBy { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE })

            // restore selections if still valid
            if (selectedCurve == null && curvesOptions.isNotEmpty()) selectedCurve = curvesOptions.first()
            if (selectedPoles == null && polesOptions.isNotEmpty()) selectedPoles = polesOptions.first()
            selectedAdditions = selectedAdditions.intersect(additionsOptions.toSet())
        } catch (t: Throwable) {
            seriesLoadingError = t.message ?: "Ошибка при загрузке вариантов"
        }
    }

    // UI
    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = false)) {
        Card(modifier = Modifier.widthIn(min = 320.dp, max = 480.dp).padding(12.dp), elevation = 8.dp, shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Параметры автоматического выключателя", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))

                // Серия (dropdown)
                Text("Серия", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(6.dp))
                var expanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = selectedSeries,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                    trailingIcon = { IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать") } }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, properties = PopupProperties(focusable = true)) {
                    seriesList.forEach { s ->
                        DropdownMenuItem(onClick = { selectedSeries = s; expanded = false }) { Text(s) }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Дополнения
                Text("Дополнения (можно несколько)", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(6.dp))
                if (additionsOptions.isEmpty()) Text("Нет доступных дополнений", style = MaterialTheme.typography.body2) else {
                    Column {
                        additionsOptions.forEach { add ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).toggleable(
                                value = selectedAdditions.contains(add),
                                onValueChange = { checked -> selectedAdditions = if (checked) selectedAdditions + add else selectedAdditions - add }
                            )) {
                                Checkbox(checked = selectedAdditions.contains(add), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(add)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Полюса
                Text("Количество полюсов", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(6.dp))
                if (polesOptions.isEmpty()) Text("Нет данных по полюсам", style = MaterialTheme.typography.body2) else {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        polesOptions.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp).clickable { selectedPoles = p }) {
                                RadioButton(selected = selectedPoles == p, onClick = { selectedPoles = p })
                                Spacer(Modifier.width(6.dp))
                                Text(p)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Кривая
                Text("Кривая отключения", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(6.dp))
                if (curvesOptions.isEmpty()) Text("Нет данных по кривым", style = MaterialTheme.typography.body2) else {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        curvesOptions.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp).clickable { selectedCurve = c }) {
                                RadioButton(selected = selectedCurve == c, onClick = { selectedCurve = c })
                                Spacer(Modifier.width(6.dp))
                                Text(c)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onBack() }) { Text("Назад") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onDismiss() }) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val poles = selectedPoles ?: polesOptions.firstOrNull() ?: ""
                        val res = BreakerSelectionResult(
                            series = selectedSeries,
                            selectedAdditions = selectedAdditions.toList(),
                            selectedPoles = poles,
                            selectedCurve = selectedCurve
                        )
                        onConfirm(res)
                    }) { Text("Далее") }
                }

                seriesLoadingError?.let { err -> Spacer(Modifier.height(8.dp)); Text("Ошибка: $err", color = MaterialTheme.colors.error) }
            }
        }
    }
}
