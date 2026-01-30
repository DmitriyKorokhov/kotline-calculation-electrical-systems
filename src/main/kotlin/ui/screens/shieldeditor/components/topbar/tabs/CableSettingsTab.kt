package ui.screens.shieldeditor.components.topbar.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.screens.shieldeditor.ShieldData

@Composable
fun CableSettingsTab(
    data: ShieldData,
    onSave: () -> Unit,
    onPushHistory: () -> Unit
) {
    val scrollState = rememberScrollState()
    val fieldWidth = 350.dp

    // Вспомогательная функция для обновления температуры при смене изоляции
    fun updateTemp(insulation: String) {
        data.cableInsulation = insulation
        val newTemp = when (insulation) {
            "PVC" -> "20"
            "XLPE" -> "20"
            "Polymer" -> "20"
            else -> "20"
        }
        data.cableTemperature = newTemp
        onSave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 8.dp)
    ) {
        Text("Кабельные линии", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.primary)
        Spacer(Modifier.height(16.dp))

        // --- Группа А: Материал проводника ---
        Text("Материал проводника", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = data.cableMaterial == "Copper", onClick = { onPushHistory(); data.cableMaterial = "Copper"; onSave() })
            Text("Медь")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = data.cableMaterial == "Aluminum", onClick = { onPushHistory(); data.cableMaterial = "Aluminum"; onSave() })
            Text("Алюминий")
        }

        Spacer(Modifier.width(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onPushHistory()
                data.cableIsFlexible = !data.cableIsFlexible
                onSave()
            }
        ) {
            Text(
                text = "Гибкий (КГ)",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Checkbox(
                checked = data.cableIsFlexible,
                onCheckedChange = {
                    onPushHistory()
                    data.cableIsFlexible = it
                    onSave()
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Группа Б: Материал изоляции ---
        Text("Материал изоляции", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "PVC", onClick = { onPushHistory(); updateTemp("PVC") })
                Text("Поливинилхлоридный пластикат (В)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "Polymer", onClick = { onPushHistory(); updateTemp("Polymer") })
                Text("Полимерная композиция (П)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "XLPE", onClick = { onPushHistory(); updateTemp("XLPE") })
                Text("Сшитый полиэтилен (Пв)")
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Группа В: Формирование длины ---
        Text("Формирование длины", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // 1. Основные параметры (Опуск и Разделка) - теперь СВЕРХУ
        Row(Modifier.width(fieldWidth), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = data.cableDescentPercent,
                onValueChange = { data.cableDescentPercent = it; onSave() },
                label = { Text("Опуск + Подъем (%)") },
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
            )

            OutlinedTextField(
                value = data.cableTerminationMeters,
                onValueChange = { data.cableTerminationMeters = it; onSave() },
                label = { Text("Разделка (м)") },
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
            )
        }

        Spacer(Modifier.height(8.dp))

        // 2. Кнопка "Дополнительные параметры" (Раскрывашка)
        var showAdvancedSettings by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onPushHistory(); showAdvancedSettings = !showAdvancedSettings }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            // Иконка меняется (Плюс или Стрелка вверх/Минус)
            Icon(
                imageVector = if (showAdvancedSettings) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF1976D2) // Синий цвет
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (showAdvancedSettings) "Скрыть дополнительные параметры" else "Дополнительные параметры",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Medium
            )
        }

        AnimatedVisibility(visible = showAdvancedSettings) {
            Column(
                modifier = Modifier.fillMaxWidth()
                // Убрали background и padding(8.dp), чтобы не ломать выравнивание
            ) {
                // Небольшой отступ от кнопки раскрытия
                Spacer(Modifier.height(8.dp))

                Text(
                    "Запас кабеля в зависимости от длины трассы:",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Ряд 1: Малые длины
                Row(Modifier.width(fieldWidth), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = data.reserveTier1,
                        onValueChange = { data.reserveTier1 = it; onSave() },
                        label = { Text("0 - 20 м (%)") },
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
                    )
                    OutlinedTextField(
                        value = data.reserveTier2,
                        onValueChange = { data.reserveTier2 = it; onSave() },
                        label = { Text("20 - 50 м (%)") },
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Ряд 2: Большие длины
                Row(Modifier.width(fieldWidth), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = data.reserveTier3,
                        onValueChange = { data.reserveTier3 = it; onSave() },
                        label = { Text("50 - 90 м (%)") },
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
                    )
                    OutlinedTextField(
                        value = data.reserveTier4,
                        onValueChange = { data.reserveTier4 = it; onSave() },
                        label = { Text("> 90 м (%)") },
                        modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onPushHistory() }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Г) Падение напряжения ---
        Text("Падение напряжения", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)

        // 1. Удельное индуктивное сопротивление (редактируемое)
        OutlinedTextField(
            value = data.cableInductiveResistance,
            onValueChange = { data.cableInductiveResistance = it; onSave() },
            label = { Text("Удельное индуктивное сопр. (мОм/м)") },
            modifier = Modifier.width(fieldWidth).onFocusChanged { if (it.isFocused) onPushHistory() }
        )

        Spacer(Modifier.height(8.dp))


        // 2. Температура
        OutlinedTextField(
            value = data.cableTemperature,
            onValueChange = { data.cableTemperature = it; onSave() },
            label = { Text("Температура кабеля (°C)") },
            modifier = Modifier.width(fieldWidth).onFocusChanged { if (it.isFocused) onPushHistory() }
        )

        Spacer(Modifier.height(8.dp))

        // 3. Удельное сопротивление (расчетное отображение)
        val t = data.cableTemperature.toDoubleOrNull() ?: 20.0
        val rho = if (data.cableMaterial == "Copper") {
            0.018 * (1 + 0.00393 * (t - 20))
        } else {
            0.028 * (1 + 0.00403 * (t - 20))
        }
        Text("Удельное активное сопротивление: ${String.format("%.4f", rho)} Ом*мм²/м", style = MaterialTheme.typography.body2)

        Spacer(Modifier.height(8.dp))

        // 4. Допустимое падение
        OutlinedTextField(
            value = data.maxVoltageDropPercent,
            onValueChange = { data.maxVoltageDropPercent = it; onSave() },
            label = { Text("Допустимое падение напряжения (%)") },
            modifier = Modifier.width(fieldWidth).onFocusChanged { if (it.isFocused) onPushHistory() }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Конфигурация жил",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = data.singleCoreThreshold,
            onValueChange = { data.singleCoreThreshold = it; onSave() },
            label = { Text("Смена многожильного на одножильный кабель при длине трассы (м)") },
            modifier = Modifier.width(fieldWidth).onFocusChanged { if (it.isFocused) onPushHistory() }
        )
    }
}
