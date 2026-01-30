package ui.screens.shieldeditor.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// Нужно изменить название класса
// N-вводов с автоматическим выключателем и байпасом
@Composable
fun InputParamsWindow(
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onNext: (inputCount: Int, hasBypass: Boolean) -> Unit
) {
    var inputCount by remember { mutableStateOf(1) }
    var hasBypass by remember { mutableStateOf(false) }
    var countMenuExpanded by remember { mutableStateOf(false) }
    val countOptions = listOf(1, 2, 3, 4, 5)

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(550.dp) // Чуть шире для удобства
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Дополнительные параметры ввода",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 1. Выбор количества вводов
                Text(
                    text = "Количество вводов",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(6.dp))
                Box {
                    OutlinedButton(
                        onClick = { countMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = inputCount.toString(), color = MaterialTheme.colors.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = countMenuExpanded,
                        onDismissRequest = { countMenuExpanded = false }
                    ) {
                        countOptions.forEach { count ->
                            DropdownMenuItem(onClick = {
                                inputCount = count
                                countMenuExpanded = false
                            }) {
                                Text(count.toString())
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 2. Байпас
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { hasBypass = !hasBypass }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasBypass,
                        onCheckedChange = { hasBypass = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Байпас", fontSize = 16.sp)
                }

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) {
                        Text("Назад")
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onNext(inputCount, hasBypass) }) {
                            Text("Далее")
                        }
                    }
                }
            }
        }
    }
}
