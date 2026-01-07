package ui.screens.shieldeditor.dialogs

import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*

@Composable
fun AddConsumerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var addCountStr by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Количество новых потребителей") },
        text = {
            OutlinedTextField(
                value = addCountStr,
                onValueChange = { input ->
                    // Разрешаем только цифры
                    if (input.all { it.isDigit() }) {
                        addCountStr = input
                    }
                },
                label = { Text("Количество") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val count = addCountStr.toIntOrNull() ?: 0
                onConfirm(count)
            }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

