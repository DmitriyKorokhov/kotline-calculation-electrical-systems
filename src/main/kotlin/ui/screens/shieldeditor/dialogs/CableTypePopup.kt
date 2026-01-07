package ui.screens.shieldeditor.dialogs

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.database.Cables
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Composable
fun CableTypePopup(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var cableTypes by remember { mutableStateOf(emptyList<String>()) }

    // Загружаем типы кабелей из БД при открытии
    LaunchedEffect(Unit) {
        transaction {
            cableTypes = Cables.selectAll()
                .map { it[Cables.type] }
                .distinct() // На всякий случай уберем дубликаты
        }
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(180.dp).heightIn(max = 200.dp)
    ) {
        if (cableTypes.isEmpty()) {
            DropdownMenuItem(onClick = { }) {
                Text("Нет данных в БД")
            }
        } else {
            cableTypes.forEach { type ->
                DropdownMenuItem(onClick = { onConfirm(type) }) {
                    Text(type)
                }
            }
        }
    }
}