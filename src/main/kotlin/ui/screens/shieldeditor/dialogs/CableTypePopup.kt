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
    onConfirm: (String) -> Unit,
    targetMaterial: String,   // "Copper" / "Aluminum"
    targetInsulation: String  // "PVC" / "XLPE" / "Polymer"
) {
    var cableTypes by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(targetMaterial, targetInsulation) {
        transaction {
            val all = Cables
                .selectAll()
                .map { it[Cables.type] }
                .distinct()

            cableTypes = all.filter { typeName ->
                val isAl = typeName.startsWith("А", ignoreCase = true)
                val mat = if (isAl) "Aluminum" else "Copper"

                val coreName = if (isAl) typeName.drop(1) else typeName
                val insulationCode = when {
                    coreName.startsWith("Пв", ignoreCase = true) -> "XLPE"
                    coreName.startsWith("В", ignoreCase = true) -> "PVC"
                    coreName.startsWith("П", ignoreCase = true) -> "Polymer"
                    else -> "PVC"
                }

                mat == targetMaterial && insulationCode == targetInsulation
            }
        }
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(180.dp)
            .heightIn(max = 200.dp)
    ) {
        if (cableTypes.isEmpty()) {
            DropdownMenuItem(onClick = { }) {
                Text("Нет подходящих типов")
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
