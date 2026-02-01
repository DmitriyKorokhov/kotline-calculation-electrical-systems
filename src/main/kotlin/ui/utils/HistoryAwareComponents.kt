package ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import view.CompactOutlinedTextField // Убедитесь, что импорт соответствует вашему пакету view

// 1. Стандартный OutlinedTextField (для настроек)
@Composable
fun HistoryAwareOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var historySaved by remember { mutableStateOf(false) }

    LaunchedEffect(historyTrigger) {
        historySaved = false
    }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (!historySaved) {
                onPushHistory(false) // false = непрерывный ввод текста
                historySaved = true
            }
            onValueChange(newValue)
        },
        label = label,
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                historySaved = false
            }
        },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

// 2. Компактный TextField (для таблиц и панелей)
@Composable
fun HistoryAwareCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onPushHistory: (Boolean) -> Unit, // Принимаем (Boolean) -> Unit
    historyTrigger: Int,
    label: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    fontSizeSp: Int,
    textColor: Color,
    focusedBorderColor: Color,
    unfocusedBorderColor: Color,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var historySaved by remember { mutableStateOf(false) }

    LaunchedEffect(historyTrigger) {
        historySaved = false
    }

    CompactOutlinedTextField(
        label = label,
        value = value,
        onValueChange = { newValue ->
            if (!historySaved) {
                onPushHistory(false) // false = непрерывный ввод текста
                historySaved = true
            }
            onValueChange(newValue)
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                historySaved = false
            }
        },
        contentPadding = contentPadding,
        fontSizeSp = fontSizeSp,
        textColor = textColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon = trailingIcon
    )
}