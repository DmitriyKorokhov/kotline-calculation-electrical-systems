package feature.shieldeditor.ui.components.topbar.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import feature.shieldeditor.state.ShieldData

@Composable
fun GeneralSettingsTab(data: ShieldData) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 8.dp)
    ) {
        // Меняем главный заголовок вкладки
        Text("Общие настройки", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.primary)
        Spacer(modifier = Modifier.height(24.dp))

        // ==========================================
        // НОВЫЙ БЛОК: РАСПРЕДЕЛЕНИЕ ФАЗ
        // ==========================================
        Text("Распределение нагрузки по фазам", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.phaseDistributionMode = "Auto" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.phaseDistributionMode == "Auto", onClick = { data.phaseDistributionMode = "Auto" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Автоматическое распределние", style = MaterialTheme.typography.body1)
                    Text("Равномерное распределение по фазам", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.phaseDistributionMode = "Other" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.phaseDistributionMode == "Other", onClick = { data.phaseDistributionMode = "Other" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Другой...", style = MaterialTheme.typography.body1)
                    Text("Автоматическая расстановка отключена, токи суммируются по введенным значениям", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // БЛОК: НУМЕРАЦИЯ (Из старого кода)
        // ==========================================
        Text("Направление нумерации", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { data.numberingLeftToRight = !data.numberingLeftToRight }.padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.ArrowForward, "Слева направо", tint = if (data.numberingLeftToRight) MaterialTheme.colors.primary else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = !data.numberingLeftToRight,
                onCheckedChange = { isRightToLeft -> data.numberingLeftToRight = !isRightToLeft },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colors.primary,
                    uncheckedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ArrowBack, "Справа налево", tint = if (!data.numberingLeftToRight) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(if (data.numberingLeftToRight) "Слева направо" else "Справа налево", style = MaterialTheme.typography.body1, color = if (data.numberingLeftToRight) MaterialTheme.colors.primary else Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Порядок нумерации устройств защиты", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.numberingOrder = "Parallel" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.numberingOrder == "Parallel", onClick = { data.numberingOrder = "Parallel" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Параллельный", style = MaterialTheme.typography.body1)
                    Text("Отдельная нумерация для каждого типа устройств", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.numberingOrder = "Sequential" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.numberingOrder == "Sequential", onClick = { data.numberingOrder = "Sequential" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Последовательный", style = MaterialTheme.typography.body1)
                    Text("Сквозная нумерация устройств", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.numberingOrder = "Other" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.numberingOrder == "Other", onClick = { data.numberingOrder = "Other" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Другой...", style = MaterialTheme.typography.body1)
                    Text("Автоматическая расстановка отключена", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Порядок нумерации групп", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.groupNumberingOrder = "Auto" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.groupNumberingOrder == "Auto", onClick = { data.groupNumberingOrder = "Auto" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Автоматическая расстановка", style = MaterialTheme.typography.body1)
                    Text("Формат нумерации Название щита.Порядковый номер", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { data.groupNumberingOrder = "Other" }.padding(vertical = 4.dp)
            ) {
                RadioButton(selected = data.groupNumberingOrder == "Other", onClick = { data.groupNumberingOrder = "Other" })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Другой...", style = MaterialTheme.typography.body1)
                    Text("Автоматическая расстановка отключена", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}