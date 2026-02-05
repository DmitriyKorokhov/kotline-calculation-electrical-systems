package ui.screens.shieldeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

@Composable
fun ShieldTopBar(
    shieldId: Int?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportDwg: () -> Unit,
    onCalculationClick: () -> Unit
) {
    var showFileMenu by remember { mutableStateOf(false) }

    val menuBackgroundColor = Color.White.copy(alpha = 0.15f)
        .compositeOver(MaterialTheme.colors.surface)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(menuBackgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Иконка "Назад"
        IconButton(
            onClick = {
                onSave()
                onBack()
            },
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colors.onSurface
            )
        }

        // 2. Меню "Файл"
        Box(modifier = Modifier.padding(start = 8.dp)) {
            MenuTextItem(
                text = "Файл",
                onClick = { showFileMenu = true }
            )

            DropdownMenu(
                expanded = showFileMenu,
                onDismissRequest = { showFileMenu = false },
                // focusable = false иногда помогает, если меню перехватывает фокус,
                // но стандартного поведения должно хватать для отображения "снизу" в рамках Box
            ) {
                // Пункт: Сохранить
                DropdownMenuItem(onClick = {
                    showFileMenu = false
                    onSave() // Вызываем сохранение (или оставьте пустым для заглушки)
                }) {
                    Text("Сохранить")
                }

                // Пункт: Экспорт
                DropdownMenuItem(onClick = {
                    showFileMenu = false
                    onExportDwg()
                }) {
                    Text("Экспорт")
                }

                // Пункт: Настройки (Заглушка)
                DropdownMenuItem(onClick = {
                    showFileMenu = false
                    // TODO: Реализовать открытие настроек
                }) {
                    Text("Настройки")
                }
            }
        }

        // 3. Пункт "Расчет"
        MenuTextItem(
            text = "Расчет",
            onClick = {
                showFileMenu = false
                onCalculationClick()
            }
        )

        // 4. Пункт "Помощь"
        MenuTextItem(
            text = "Помощь",
            onClick = { /* TODO: Справка */ }
        )

        Spacer(Modifier.weight(1f))

        // Информационное поле справа
        Text(
            text = "Щит ID: ${shieldId ?: "-"}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 12.dp)
        )
    }
}

@Composable
private fun MenuTextItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurface
    )
}