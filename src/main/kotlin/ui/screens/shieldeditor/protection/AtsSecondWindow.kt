package ui.screens.shieldeditor.protection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.database.AtsModels
import data.database.AtsVariants
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

// Класс данных для результата выбора, должен быть виден в ShieldEditorView
data class AtsSelectionResult(
    val series: String?,
    val selectedPoles: String?
)

// Цвета
private val WINDOW_BG = Color(0xFF2B2B2B)
private val FIELD_BG = Color(0xFF3C3C3C)
private val TEXT_COLOR = Color(0xFFE0E0E0)
private val BORDER_COLOR = Color(0xFF555555)

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
        Box(
            modifier = Modifier
                .width(520.dp)
                .background(WINDOW_BG, RoundedCornerShape(8.dp))
                .border(1.dp, BORDER_COLOR, RoundedCornerShape(8.dp))
                .padding(20.dp)
        ) {
            Column {
                Text("Выбор АВР (Шаг 1: Параметры)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                Spacer(Modifier.height(20.dp))

                // Производитель
                AtsDropdown(
                    label = "Производитель",
                    value = manufacturer,
                    options = manufacturersList,
                    onValueChange = { manufacturer = it }
                )
                Spacer(Modifier.height(12.dp))

                // Серия
                AtsDropdown(
                    label = "Серия",
                    value = series,
                    options = seriesList,
                    onValueChange = { series = it },
                    enabled = manufacturer.isNotBlank()
                )
                Spacer(Modifier.height(16.dp))

                // Полюса
                Text("Число полюсов:", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (polesList.isEmpty() && manufacturer.isNotBlank() && series.isNotBlank()) {
                        Text("Нет данных", fontSize = 13.sp, color = Color.Gray)
                    }
                    polesList.forEach { pole ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedPoles = pole }) {
                            RadioButton(
                                selected = (pole == selectedPoles),
                                onClick = { selectedPoles = pole },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colors.primary,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Text(text = pole, fontSize = 14.sp, color = TEXT_COLOR)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Кнопка Отмена
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color(0xFFEF5350))
                    }

                    Row {
                        TextButton(onClick = onBack) { Text("Назад", color = TEXT_COLOR) }
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
}

@Composable
private fun AtsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = FIELD_BG,
                contentColor = TEXT_COLOR,
                disabledContentColor = Color.Gray
            ),
            border = BorderStroke(1.dp, BORDER_COLOR)
        ) {
            Text(if (value.isBlank()) "Выберите $label" else value)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FIELD_BG).border(1.dp, BORDER_COLOR)
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onValueChange(option)
                    expanded = false
                }) {
                    Text(option, color = TEXT_COLOR)
                }
            }
        }
    }
}
