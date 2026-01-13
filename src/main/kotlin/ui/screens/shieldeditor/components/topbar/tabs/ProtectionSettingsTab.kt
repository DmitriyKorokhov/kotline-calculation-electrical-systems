package ui.screens.shieldeditor.components.topbar.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ui.screens.shieldeditor.ShieldData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ProtectionSettingsTab(
    data: ShieldData,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    val fieldWidth = 350.dp

    // Данные для выпадающих списков
    val standards = listOf("ГОСТ IEC 60898-1-2020", "ГОСТ IEC 60947-2-2021")
    val manufacturers = listOf("Nader", "Systeme electric", "DEKraft")

    // Состояния раскрытия меню
    var stdMenuExpanded by remember { mutableStateOf(false) }
    var manufMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 8.dp)
    ) {
        Text(
            text = "Устройства защиты",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Блок 1: Стандарт ---
        Text("Стандарт испытания", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { stdMenuExpanded = true },
                modifier = Modifier.width(fieldWidth)
            ) {
                Text(
                    text = data.protectionStandard.ifBlank { "Выберите стандарт" },
                    color = MaterialTheme.colors.onSurface
                )
            }
            DropdownMenu(
                expanded = stdMenuExpanded,
                onDismissRequest = { stdMenuExpanded = false },
                modifier = Modifier.width(fieldWidth)
            ) {
                standards.forEach { std ->
                    DropdownMenuItem(onClick = {
                        data.protectionStandard = std
                        stdMenuExpanded = false
                        onSave()
                    }) {
                        Text(std)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Блок 2: Производитель ---
        Text("Производитель устройств", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { manufMenuExpanded = true },
                modifier = Modifier.width(fieldWidth)
            ) {
                Text(
                    text = data.protectionManufacturer.ifBlank { "Выберите производителя" },
                    color = MaterialTheme.colors.onSurface
                )
            }
            DropdownMenu(
                expanded = manufMenuExpanded,
                onDismissRequest = { manufMenuExpanded = false },
                modifier = Modifier.width(fieldWidth)
            ) {
                manufacturers.forEach { manuf ->
                    DropdownMenuItem(onClick = {
                        data.protectionManufacturer = manuf
                        manufMenuExpanded = false
                        onSave()
                    }) {
                        Text(manuf)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Блок 3: Защита от перегрузки ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = data.hasOverloadProtection,
                onCheckedChange = {
                    data.hasOverloadProtection = it
                    onSave()
                }
            )
            Text(
                text = "Наличие защиты от перегрузки",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Блок 4: Условие выбора защиты (Новое) ---
        // Кнопка "Дополнительные параметры"
        var showAdvancedProtection by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { showAdvancedProtection = !showAdvancedProtection }
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = if (showAdvancedProtection) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF1976D2) // Синий цвет
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (showAdvancedProtection) "Скрыть дополнительные параметры" else "Дополнительные параметры",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF1976D2),
                fontWeight = FontWeight.Medium
            )
        }

        // Скрываемая секция
        AnimatedVisibility(visible = showAdvancedProtection) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Выбор номинального тока аппарата защиты",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Порог тока (40 А)
                OutlinedTextField(
                    value = data.protectionCurrentThreshold,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            data.protectionCurrentThreshold = it
                            onSave()
                        }
                    },
                    label = { Text("Пороговый расчетный ток (А)") },
                    modifier = Modifier.width(fieldWidth),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text(
                    text = "Ток, при котором меняются коэффициенты",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 2. Коэффициент для тока < Порога (0.87)
                OutlinedTextField(
                    value = data.protectionFactorLow,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            data.protectionFactorLow = it
                            onSave()
                        }
                    },
                    label = { Text("Iрасч < порога") },
                    modifier = Modifier.width(fieldWidth),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text(
                    text = "Отношение расчетного тока к номинальному току защиты",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // 3. Коэффициент для тока >= Порога (0.93)
                OutlinedTextField(
                    value = data.protectionFactorHigh,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                            data.protectionFactorHigh = it
                            onSave()
                        }
                    },
                    label = { Text("Iрасч ≥ порога") },
                    modifier = Modifier.width(fieldWidth),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text(
                    text = "Отношение расчетного тока к номинальному току защиты",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

