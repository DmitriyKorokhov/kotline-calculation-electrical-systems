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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.database.AtsModels
import data.database.AtsVariants
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class AtsSelectionResult(
    val series: String?,
    val selectedPoles: String?
)

@Composable
fun AtsSecondWindow(
    initialManufacturer: String? = null,
    initialSeries: String? = null,
    initialSelectedPoles: String? = null,
    consumerVoltageStr: String? = null,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AtsSelectionResult) -> Unit
) {
    var manufacturer by remember { mutableStateOf(initialManufacturer ?: "") }
    var series by remember { mutableStateOf(initialSeries ?: "") }
    var selectedPoles by remember { mutableStateOf(initialSelectedPoles ?: "") }

    var manufacturersList by remember { mutableStateOf(listOf<String>()) }
    var seriesList by remember { mutableStateOf(listOf<String>()) }
    var polesList by remember { mutableStateOf(listOf<String>()) }

    // 1. Загрузка производителей
    LaunchedEffect(Unit) {
        transaction {
            manufacturersList = AtsModels.slice(AtsModels.manufacturer)
                .selectAll()
                .withDistinct()
                .map { it[AtsModels.manufacturer] }
                .sorted()
        }
    }

    // 2. Загрузка серий
    LaunchedEffect(manufacturer) {
        if (manufacturer.isNotBlank()) {
            transaction {
                seriesList = AtsModels.slice(AtsModels.series)
                    .select { AtsModels.manufacturer eq manufacturer }
                    .withDistinct()
                    .map { it[AtsModels.series] }
                    .sorted()
            }
            if (series !in seriesList) series = ""
        } else {
            seriesList = emptyList()
            series = ""
        }
    }

    // 3. Загрузка полюсов
    LaunchedEffect(manufacturer, series) {
        if (manufacturer.isNotBlank() && series.isNotBlank()) {
            transaction {
                val modelIds = AtsModels.slice(AtsModels.id)
                    .select { (AtsModels.manufacturer eq manufacturer) and (AtsModels.series eq series) }
                    .map { it[AtsModels.id] }

                if (modelIds.isNotEmpty()) {
                    polesList = AtsVariants.slice(AtsVariants.poles)
                        .select { AtsVariants.modelId inList modelIds }
                        .withDistinct()
                        .map { it[AtsVariants.poles] }
                        .sorted()
                } else {
                    polesList = emptyList()
                }
            }

            if (selectedPoles !in polesList) selectedPoles = ""

            // Автовыбор полюсов
            if (selectedPoles.isBlank() && polesList.isNotEmpty()) {
                val isThreePhase = consumerVoltageStr?.contains("380") == true || consumerVoltageStr?.contains("400") == true
                if (isThreePhase) {
                    if ("4P" in polesList) selectedPoles = "4P"
                    else if ("3P" in polesList) selectedPoles = "3P"
                } else {
                    if ("2P" in polesList) selectedPoles = "2P"
                }
            }
        } else {
            polesList = emptyList()
            selectedPoles = ""
        }
    }

    val canProceed = manufacturer.isNotBlank() && series.isNotBlank() && selectedPoles.isNotBlank()

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(520.dp)
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Выбор АВР (Шаг 1: Параметры)", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(20.dp))

                // Производитель
                AtsDropdownField(
                    label = "Производитель",
                    value = manufacturer,
                    options = manufacturersList,
                    onValueChange = { manufacturer = it }
                )

                Spacer(Modifier.height(12.dp))

                // Серия
                AtsDropdownField(
                    label = "Серия",
                    value = series,
                    options = seriesList,
                    onValueChange = { series = it },
                    enabled = manufacturer.isNotBlank()
                )

                Spacer(Modifier.height(16.dp))

                // Полюса
                Text("Число полюсов:", style = MaterialTheme.typography.subtitle2)
                Spacer(Modifier.height(8.dp))

                if (polesList.isEmpty() && manufacturer.isNotBlank() && series.isNotBlank()) {
                    Text("Нет данных", style = MaterialTheme.typography.body2, color = Color.Gray)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        polesList.forEach { pole ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedPoles = pole }
                            ) {
                                RadioButton(
                                    selected = (pole == selectedPoles),
                                    onClick = { selectedPoles = pole }
                                )
                                Text(text = pole, style = MaterialTheme.typography.body1)
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
                    Button(
                        onClick = { onConfirm(AtsSelectionResult(series, selectedPoles)) },
                        enabled = canProceed
                    ) {
                        Text("Далее")
                    }
                }
            }
        }
    }
}

@Composable
private fun AtsDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates -> textFieldSize = coordinates.size.toSize() }
                .clickable(enabled = enabled) { expanded = true },
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                IconButton(onClick = { if (enabled) expanded = !expanded }, enabled = enabled) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onValueChange(option)
                    expanded = false
                }) {
                    Text(option)
                }
            }
        }
    }
}
