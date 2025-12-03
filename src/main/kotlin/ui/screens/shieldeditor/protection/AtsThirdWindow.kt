package ui.screens.shieldeditor.protection

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.database.AtsModels
import data.database.AtsVariants
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

// Класс элемента таблицы
data class AtsResultItem(
    val modelName: String,
    val ratedCurrent: Float,
    val poles: String,
    val breakingCapacityStr: String,
    val breakingCapacityVal: Float
)

// Цвета
private val WINDOW_BG = Color(0xFF2B2B2B)
private val TABLE_HEADER_BG = Color(0xFF424242)
private val ROW_HOVER_BG = Color(0xFF3C3C3C)
private val SELECTED_BG = Color(0xFF1976D2).copy(alpha = 0.3f)
private val TEXT_COLOR = Color(0xFFE0E0E0)
private val BORDER_COLOR = Color(0xFF555555)

@Composable
fun AtsThirdWindow(
    maxShortCircuitCurrentStr: String,
    consumerCurrentAStr: String,
    selectedSeries: String?,
    selectedPoles: String?,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    val requiredCurrent = consumerCurrentAStr.replace(",", ".").toFloatOrNull() ?: 0f
    val requiredBreakingCapacity = maxShortCircuitCurrentStr.replace(",", ".").toFloatOrNull() ?: 0f

    // ЯВНО УКАЗЫВАЕМ ТИПЫ ДЛЯ STATE
    var items by remember { mutableStateOf<List<AtsResultItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<AtsResultItem?>(null) }

    LaunchedEffect(selectedSeries, selectedPoles) {
        if (selectedSeries != null && selectedPoles != null) {
            transaction {
                val models = AtsModels.select { AtsModels.series eq selectedSeries }
                    .map { Triple(it[AtsModels.id], it[AtsModels.model], it[AtsModels.breakingCapacity]) }

                val results = mutableListOf<AtsResultItem>()

                for ((mId, mName, mCapStr) in models) {
                    val mCapVal = mCapStr.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f

                    if (mCapVal < requiredBreakingCapacity) continue

                    val variants = AtsVariants.select {
                        (AtsVariants.modelId eq mId) and (AtsVariants.poles eq selectedPoles)
                    }

                    for (v in variants) {
                        val current = v[AtsVariants.ratedCurrent]
                        if (current >= requiredCurrent) {
                            results.add(
                                AtsResultItem(
                                    modelName = mName,
                                    ratedCurrent = current,
                                    poles = selectedPoles,
                                    breakingCapacityStr = mCapStr,
                                    breakingCapacityVal = mCapVal
                                )
                            )
                        }
                    }
                }

                // ИСПРАВЛЕННЫЙ КОМПАРАТОР С ЯВНЫМИ ТИПАМИ
                items = results.sortedWith(
                    compareBy<AtsResultItem> { it.ratedCurrent }
                        .thenBy { it.breakingCapacityVal }
                )

                selectedItem = items.firstOrNull()
            }
        }
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .width(850.dp)
                .height(650.dp)
                .background(WINDOW_BG, RoundedCornerShape(8.dp))
                .border(1.dp, BORDER_COLOR, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Подбор АВР (Шаг 2: Результат)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Расчетный ток: $requiredCurrent А, Ток КЗ: $requiredBreakingCapacity кА",
                    fontSize = 14.sp, color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))

                // Заголовки таблицы
                Row(Modifier.background(TABLE_HEADER_BG).padding(8.dp)) {
                    Text("Модель", Modifier.weight(2f), fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                    Text("Ном. ток (А)", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                    Text("Полюса", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                    Text("Icu (кА)", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TEXT_COLOR)
                }

                // Список результатов
                Box(Modifier.weight(1f).border(1.dp, BORDER_COLOR)) {
                    val scrollState = rememberScrollState()
                    Column(Modifier.verticalScroll(scrollState)) {
                        if (items.isEmpty()) {
                            Text("Нет подходящих устройств", Modifier.padding(16.dp), color = Color.Gray)
                        }
                        // ЯВНОЕ УКАЗАНИЕ ИТЕРАТОРА НЕ ТРЕБУЕТСЯ, НО ЛУЧШЕ ТАК
                        items.forEach { item ->
                            val isSel = (selectedItem == item)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSel) SELECTED_BG else Color.Transparent)
                                    .clickable { selectedItem = item }
                                    .padding(8.dp)
                            ) {
                                Text(item.modelName, Modifier.weight(2f), color = TEXT_COLOR)
                                Text("${item.ratedCurrent.toInt()}", Modifier.weight(1f), color = TEXT_COLOR)
                                Text(item.poles, Modifier.weight(1f), color = TEXT_COLOR)
                                Text(item.breakingCapacityStr, Modifier.weight(1f), color = TEXT_COLOR)
                            }
                            Divider(color = BORDER_COLOR)
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Кнопка Отмена
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = Color(0xFFEF5350))
                    }

                    Row {
                        TextButton(onClick = onBack) { Text("Назад", color = TEXT_COLOR) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedItem?.let {
                                    val res = "${it.modelName} ${it.ratedCurrent.toInt()}A ${it.poles}"
                                    onChoose(res)
                                }
                            },
                            enabled = selectedItem != null
                        ) {
                            Text("Выбрать")
                        }
                    }
                }
            }
        }
    }
}
