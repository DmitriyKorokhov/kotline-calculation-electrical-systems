package ui.screens.shieldeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

enum class InputType(val title: String, val needsBreakerConfig: Boolean) {
    // 1) Два ввода на общую шину с АВР на автоматических выключателях с моторприводом
    TWO_INPUTS_ATS_BREAKERS("Два ввода на общую шину с АВР на автоматических выключателях с моторприводом", false),

    // 2) Два ввода на общую шину с АВР на контакторах с моторприводом
    TWO_INPUTS_ATS_CONTACTORS("Два ввода на общую шину с АВР на контакторах с моторприводом", false),

    // 3) Один ввод с выключателем нагрузки
    ONE_INPUT_SWITCH("Один ввод с выключателем нагрузки", false),

    // 4) Один ввод с автоматическим выключателем (нужна настройка автомата - true)
    ONE_INPUT_BREAKER("Один ввод с автоматическим выключателем", true),

    // 5) Блок АВР на два ввода на общую шину
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
        Box(
            modifier = Modifier
                .width(700.dp) // Увеличили ширину для длинных названий
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Выберите тип ввода",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val options = InputType.values()

                // Вертикальный список для лучшей читаемости длинных названий
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEach { type ->
                        InputTypeCard(
                            type = type,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onConfirm(type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputTypeCard(
    type: InputType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
            .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
            .padding(16.dp), // Чуть больше padding
        contentAlignment = Alignment.CenterStart // Выравнивание по левому краю
    ) {
        Text(
            text = type.title,
            textAlign = TextAlign.Start,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}