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
    TWO_INPUTS_AVR("Два ввода на общую шину с АВР", false),
    TWO_INPUTS_VR("Два ввода на общую шину с ВР", false),
    ONE_INPUT_SWITCH("Один ввод на общую шину с рубильником", false),
    ONE_INPUT_AUTO("Один ввод на общую шину с автоматом", true)
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
                .width(600.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Выберите тип ввода",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val options = InputType.values()

                // Простая сетка 2x2
                Column {
                    options.toList().chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { type ->
                                InputTypeCard(
                                    type = type,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(100.dp)
                                        .clickable { onConfirm(type) }
                                )
                            }
                            // Если нечетное количество, добавляем пустой вес
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = type.title,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}