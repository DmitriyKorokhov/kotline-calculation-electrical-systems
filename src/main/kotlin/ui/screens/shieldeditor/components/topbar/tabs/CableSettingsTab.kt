package ui.screens.shieldeditor.components.topbar.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CableSettingsTab() {
    val scrollState = rememberScrollState()

    var material by remember { mutableStateOf("Copper") }
    var maxVoltageDrop by remember { mutableStateOf("5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 8.dp)
    ) {
        Text(
            text = "Кабельные линии",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Группа 1: Материал
        Text("Материал проводника", style = MaterialTheme.typography.subtitle2)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Checkbox(checked = material == "Copper", onCheckedChange = { material = "Copper" })
            Text("Медь", modifier = Modifier.padding(top = 12.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(checked = material == "Aluminum", onCheckedChange = { material = "Aluminum" })
            Text("Алюминий", modifier = Modifier.padding(top = 12.dp))
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Группа 2: Падение напряжения
        Text("Допустимое падение напряжения (%)", style = MaterialTheme.typography.subtitle2)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = maxVoltageDrop,
            onValueChange = { maxVoltageDrop = it },
            label = { Text("Макс. dU %") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Здесь можно добавить настройки групповой прокладки, температуры грунта и т.д.",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}
