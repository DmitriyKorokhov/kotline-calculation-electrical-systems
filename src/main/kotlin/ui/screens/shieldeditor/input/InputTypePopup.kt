package ui.screens.shieldeditor.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

enum class InputType(val title: String, val needsBreakerConfig: Boolean) {
    TWO_INPUTS_ATS_BREAKERS("Два ввода на общую шину с АВР на автоматических выключателях с моторприводом", false),
    TWO_INPUTS_ATS_CONTACTORS("Два ввода на общую шину с АВР на контакторах с моторприводом", false),
    ONE_INPUT_SWITCH("Один ввод с выключателем нагрузки", false),
    ONE_INPUT_BREAKER("Один ввод с автоматическим выключателем", true),
    ATS_BLOCK_TWO_INPUTS("Блок АВР на два ввода на общую шину", false);
}

@Composable
fun InputTypePopup(
    onDismissRequest: () -> Unit,
    onConfirm: (InputType) -> Unit
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .width(750.dp) // Ширина, достаточная для длинных названий
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Заголовок окна
                Text(
                    text = "Выберите тип ввода",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Список вариантов в виде карточек
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InputType.values().forEach { type ->
                        SelectableCard(
                            text = type.title,
                            onClick = { onConfirm(type) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Кнопка отмены внизу, как в RcdSecondWindow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}

/**
 * Вспомогательная карточка для выбора опции, стилизованная под единый дизайн.
 */
@Composable
private fun SelectableCard(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Start,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
