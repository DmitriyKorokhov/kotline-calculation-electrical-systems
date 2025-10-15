package ui.screens.shieldeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Кастомный пункт меню для использования внутри Popup.
 */
@Composable
private fun CustomMenuItem(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Меняем цвет текста, если пункт неактивен
        val textColor = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        Text(text = text, style = MaterialTheme.typography.body1, color = textColor)
    }
}

/**
 * Контекстное меню для управления потребителями (колонками).
 *
 * @param expanded Показать или скрыть меню.
 * @param offset Позиция меню на экране.
 * @param onDismissRequest Запрос на закрытие меню.
 * @param onAction Функция, вызываемая при выборе действия.
 * @param isPasteEnabled Включает пункт "Вставить".
 * @param isAddEnabled Включает пункт "Добавить".
 */
@Composable
fun ConsumerContextMenu(
    expanded: Boolean,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
    onAction: (ContextMenuAction) -> Unit,
    isPasteEnabled: Boolean,
    isAddEnabled: Boolean
) {
    if (expanded) {
        Popup(
            alignment = Alignment.TopStart,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Card(
                modifier = Modifier.width(190.dp),
                shape = RoundedCornerShape(10.dp),
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    CustomMenuItem("Удалить") { onAction(ContextMenuAction.DELETE) }
                    CustomMenuItem("Копировать") { onAction(ContextMenuAction.COPY) }
                    CustomMenuItem("Вставить", enabled = isPasteEnabled) { onAction(ContextMenuAction.PASTE) }
                    CustomMenuItem("Добавить", enabled = isAddEnabled) { onAction(ContextMenuAction.ADD) }
                }
            }
        }
    }
}

/**
 * Перечисление для действий контекстного меню, чтобы сделать код чище.
 */
enum class ContextMenuAction {
    DELETE,
    COPY,
    PASTE,
    ADD
}

