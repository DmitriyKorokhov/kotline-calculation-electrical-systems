package ui.screens.shieldeditor.components.topbar.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.screens.shieldeditor.ShieldData

@Composable
fun CableSettingsTab(
    data: ShieldData,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Вспомогательная функция для обновления температуры при смене изоляции
    fun updateTemp(insulation: String) {
        data.cableInsulation = insulation
        val newTemp = when (insulation) {
            "PVC" -> "70"
            "XLPE" -> "90"
            "Polymer" -> "70"
            else -> "70"
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
            RadioButton(selected = data.cableMaterial == "Copper", onClick = { data.cableMaterial = "Copper"; onSave() })
            Text("Медь")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = data.cableMaterial == "Aluminum", onClick = { data.cableMaterial = "Aluminum"; onSave() })
            Text("Алюминий")
        }

        Spacer(Modifier.height(16.dp))

        // --- Группа Б: Материал изоляции ---
        Text("Материал изоляции", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "PVC", onClick = { updateTemp("PVC") })
                Text("Поливинилхлоридный пластикат (В)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "Polymer", onClick = { updateTemp("Polymer") })
                Text("Полимерная композиция (П)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = data.cableInsulation == "XLPE", onClick = { updateTemp("XLPE") })
                Text("Сшитый полиэтилен (Пв)")
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Группа В: Формирование длины ---
        Text("Формирование длины", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = data.cableReservePercent,
                onValueChange = { data.cableReservePercent = it; onSave() },
                label = { Text("Запас %") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = data.cableDescentPercent,
                onValueChange = { data.cableDescentPercent = it; onSave() },
                label = { Text("Опуск + Подъем %") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = data.cableTerminationMeters,
                onValueChange = { data.cableTerminationMeters = it; onSave() },
                label = { Text("Разделка (м)") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Г) Падение напряжения ---
        Text("Падение напряжения", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)

        // 1. Удельное индуктивное сопротивление (редактируемое)
        OutlinedTextField(
            value = data.cableInductiveResistance,
            onValueChange = { data.cableInductiveResistance = it; onSave() },
            label = { Text("Удельное индуктивное сопр. (мОм/м)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))


        // 2. Температура
        OutlinedTextField(
            value = data.cableTemperature,
            onValueChange = { data.cableTemperature = it; onSave() },
            label = { Text("Температура кабеля (°C)") },
            modifier = Modifier.fillMaxWidth()
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}
