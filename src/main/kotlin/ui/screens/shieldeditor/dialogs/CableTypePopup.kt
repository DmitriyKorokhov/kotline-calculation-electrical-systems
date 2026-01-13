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
    targetInsulation: String,
    isFlexible: Boolean
) {
    var cableTypes by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(targetMaterial, targetInsulation, isFlexible) { // Добавьте isFlexible в аргументы функции и сюда
        transaction {
            val all = Cables.selectAll().map { it[Cables.type] }.distinct()

            cableTypes = all.filter { typeName ->
                // 1. Проверяем гибкость
                val isKGFlex = typeName.startsWith("КГ", ignoreCase = true)

                // Убираем префикс "КГ" для анализа состава
                val cleanName = if (isKGFlex) typeName.substring(2) else typeName

                // 2. Определяем материал (А = Алюминий)
                val isAl = cleanName.startsWith("А", ignoreCase = true)
                val mat = if (isAl) "Aluminum" else "Copper"

                // 3. Определяем изоляцию (убираем "А" если была)
                val coreName = if (isAl) cleanName.drop(1) else cleanName

                val insulationCode = when {
                    coreName.startsWith("Пв", ignoreCase = true) -> "XLPE" // Сшитый полиэтилен
                    coreName.startsWith("В", ignoreCase = true) -> "PVC"   // Винил (ПВХ)
                    coreName.startsWith("П", ignoreCase = true) -> "Polymer" // Полимер
                    else -> "PVC"
                }
                val flexMatch = if (isFlexible) isKGFlex else !isKGFlex

                mat == targetMaterial && insulationCode == targetInsulation && flexMatch
            }.sortedBy { it }
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
