package ui.screens.shieldeditor.protection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Non-modal popup для выбора типа защиты.
 *
 * @param initial выбор по-умолчанию (используется при первом показе)
 * @param onDismissRequest вызывается при закрытии/отмене
 * @param onConfirm вызывается при подтверждении (кнопка "Далее"), возвращает выбранный ProtectionType
 */
@Composable
fun ProtectionChooserPopup(
    initial: ProtectionType = ProtectionType.CIRCUIT_BREAKER,
    onDismissRequest: () -> Unit,
    onConfirm: (ProtectionType) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }

    Popup(
        alignment = Alignment.Center,
        // Не делаем фокусируемым — popup не блокирует главное окно
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = 8.dp,
                modifier = Modifier
                    // Компактная карточка: не растягивается на весь экран, но достаточно широка для текста
                    .widthIn(min = 320.dp, max = 480.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Заголовок
                    Text(
                        text = "Выбор типа защиты",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Выберите один из типов:",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Варианты с RadioButton — кликабельная строка для удобства
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selected = ProtectionType.CIRCUIT_BREAKER }
                        ) {
                            RadioButton(
                                selected = selected == ProtectionType.CIRCUIT_BREAKER,
                                onClick = { selected = ProtectionType.CIRCUIT_BREAKER }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ProtectionType.CIRCUIT_BREAKER.displayName(),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selected = ProtectionType.DIFF_CURRENT_BREAKER }
                        ) {
                            RadioButton(
                                selected = selected == ProtectionType.DIFF_CURRENT_BREAKER,
                                onClick = { selected = ProtectionType.DIFF_CURRENT_BREAKER }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ProtectionType.DIFF_CURRENT_BREAKER.displayName(),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selected = ProtectionType.CIRCUIT_BREAKER_AND_RCD }
                        ) {
                            RadioButton(
                                selected = selected == ProtectionType.CIRCUIT_BREAKER_AND_RCD,
                                onClick = { selected = ProtectionType.CIRCUIT_BREAKER_AND_RCD }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = ProtectionType.CIRCUIT_BREAKER_AND_RCD.displayName(),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Кнопки действий
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDismissRequest() }) {
                            Text("Отмена")
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(onClick = { onConfirm(selected) }) {
                            Text("Далее")
                        }
                    }
                }
            }
        }
    }
}
