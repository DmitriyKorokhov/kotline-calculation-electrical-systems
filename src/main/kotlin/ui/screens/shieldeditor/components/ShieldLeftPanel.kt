package ui.screens.shieldeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import view.CompactOutlinedTextField
import ui.screens.shieldeditor.ShieldData
import ui.utils.HistoryAwareCompactTextField

private val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
private val BLOCK_BORDER = Color(0xFFB0BEC5)
private val BLOCK_BLUE = Color(0xFFE3F2FD).copy(alpha = 0.15f)
private val BLOCK_WHITE = Color.White.copy(alpha = 0.15f)

@Composable
fun ShieldLeftPanel(
    data: ShieldData,
    onSave: () -> Unit,
    onCalculate: () -> Unit,
    onOpenInputTypeDialog: () -> Unit,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int
) {
    val textColor = Color.White
    val borderColor = Color.White

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(6.dp)
            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Данные щита", color = textColor, fontSize = 14.sp)
        }

        Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Color.LightGray)

        HistoryAwareCompactTextField(
            label = "Наименование щита",
            value = data.shieldName,
            onValueChange = { data.shieldName = it; onSave() },
            onPushHistory = onPushHistory,
            historyTrigger = historyTrigger,
            contentPadding = FIELD_CONTENT_PADDING,
            fontSizeSp = 16,
            textColor = textColor,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = Color.LightGray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        HistoryAwareCompactTextField(
            label = "Макс. ток КЗ, кА",
            value = data.maxShortCircuitCurrent,
            onValueChange = { data.maxShortCircuitCurrent = it; onSave() },
            onPushHistory = onPushHistory,
            historyTrigger = historyTrigger,
            contentPadding = FIELD_CONTENT_PADDING,
            fontSizeSp = 16,
            textColor = textColor,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = Color.LightGray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // --- БЛОК "ВВОД" ---
        BlockPanel(color = BLOCK_WHITE) {
            Text(
                "Параметры ввода",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(8.dp))

            HistoryAwareCompactTextField(
                label = "Тип и устройство",
                value = data.inputInfo,
                onValueChange = {
                    data.inputInfo = it; onSave()
                },
                onPushHistory = onPushHistory,
                historyTrigger = historyTrigger,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = FIELD_CONTENT_PADDING,
                fontSizeSp = 15,
                singleLine = false,
                minLines = 2,
                maxLines = 6,
                textColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.LightGray,
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        "Выбрать",
                        Modifier.clickable { onOpenInputTypeDialog() },
                        tint = textColor
                    )
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Фазные нагрузки (Read-only)
        listOf("L1" to data.phaseL1, "L2" to data.phaseL2, "L3" to data.phaseL3).forEach { (label, value) ->
            CompactOutlinedTextField(
                label = "Нагрузка на фазу $label, А",
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                contentPadding = FIELD_CONTENT_PADDING,
                textColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.LightGray,
                fontSizeSp = 15,
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onCalculate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Распределить нагрузку")
        }

        Spacer(Modifier.height(8.dp))

        // Блок коэффициентов
        BlockPanel(BLOCK_BLUE) {
            HistoryAwareCompactTextField(
                label = "Коэф. спроса",
                value = data.demandFactor,
                onValueChange = { data.demandFactor = it; onSave() },
                onPushHistory = onPushHistory,
                historyTrigger = historyTrigger,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = FIELD_CONTENT_PADDING,
                textColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.LightGray,
                fontSizeSp = 15,
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            HistoryAwareCompactTextField(
                label = "Коэф. одновременности",
                value = data.simultaneityFactor,
                onValueChange = { data.simultaneityFactor = it; onSave() },
                onPushHistory = onPushHistory,
                historyTrigger = historyTrigger,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = FIELD_CONTENT_PADDING,
                textColor = textColor,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color.LightGray,
                fontSizeSp = 15,
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        // Блок результатов
        BlockPanel(BLOCK_WHITE) {
            val results = listOf(
                "Установ. мощность, кВт" to data.totalInstalledPower,
                "Расчетная мощность, кВт" to data.totalCalculatedPower,
                "cos(φ)" to data.averageCosPhi,
                "Ток, А" to data.totalCurrent,
                "Коэф. спроса щита" to data.shieldDemandFactor
            )

            results.forEach { (label, value) ->
                CompactOutlinedTextField(
                    label = label,
                    value = value,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = FIELD_CONTENT_PADDING,
                    textColor = textColor,
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = Color.LightGray,
                    fontSizeSp = 15,
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// Вспомогательный компонент для панели
@Composable
private fun BlockPanel(
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .border(1.dp, BLOCK_BORDER, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) { content() }
}
