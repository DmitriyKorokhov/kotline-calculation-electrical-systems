package ui.screens.shieldeditor.protection

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.repository.getRcdVariantsBySeries
import kotlin.math.abs

// Модель для UI списка УЗО
data class RcdUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val ratedResidualCurrent: String // Ток утечки
)

@Composable
fun RcdThirdWindow(
    consumerCurrentAStr: String,
    consumerVoltageStr: String?,
    selectedSeries: String?,
    selectedPoles: String?,
    selectedResidualCurrent: String?,
    protectionThreshold: Float,
    protectionFactorLow: Float,
    protectionFactorHigh: Float,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<RcdUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val consumerA = parseAmps(consumerCurrentAStr) ?: 0f

    // Загрузка данных при старте
    LaunchedEffect(selectedSeries) {
        loading = true
        errorMsg = null
        items = emptyList()
        try {
            if (selectedSeries.isNullOrBlank()) {
                loading = false
                return@LaunchedEffect
            }
            // Загружаем все варианты данного производителя
            val pairs = getRcdVariantsBySeries(selectedSeries)
            items = pairs.map { (model, variant) ->
                RcdUiItem(
                    modelId = model.id,
                    variantId = variant.id,
                    manufacturer = model.manufacturer,
                    modelName = model.model,
                    ratedCurrentA = variant.ratedCurrent,
                    polesText = variant.poles,
                    ratedResidualCurrent = variant.ratedResidualCurrent
                )
            }
            loading = false
        } catch (t: Throwable) {
            loading = false
            errorMsg = t.message ?: "Ошибка при загрузке УЗО"
        }
    }

    // Фильтрация элементов
    fun passesAll(item: RcdUiItem): Boolean {
        if (item.ratedCurrentA < consumerA) return false
        if (!selectedPoles.isNullOrBlank()) {
            if (item.polesText.trim() != selectedPoles.trim()) return false
        }
        if (!selectedResidualCurrent.isNullOrBlank()) {
            if (item.ratedResidualCurrent.trim() != selectedResidualCurrent.trim()) return false
        }
        return true
    }

    // Вычисляем список подходящих вариантов
    val passing = remember(items, consumerA, selectedPoles, selectedResidualCurrent) {
        items.filter { passesAll(it) }
    }

    // --- НОВАЯ ЛОГИКА ГРУППИРОВКИ (по modelName) ---
    val finalList = remember(passing, consumerA, protectionThreshold, protectionFactorLow, protectionFactorHigh) {
        val grouped = passing.groupBy { it.modelName }
        val result = mutableListOf<RcdUiItem>()

        for ((_, variants) in grouped) {
            // 1. Уникальные доступные номиналы
            val candidates = variants
                .map { it.ratedCurrentA }
                .distinct()
                .sorted()

            if (candidates.isNotEmpty()) {
                val firstNominal = candidates[0]
                var bestCurrent = firstNominal

                if (firstNominal > 0) {
                    val a = consumerA / firstNominal

                    // Выбираем пороговый коэффициент
                    val targetK = if (consumerA < protectionThreshold) {
                        protectionFactorLow
                    } else {
                        protectionFactorHigh
                    }

                    // Если запас мал, берем следующий номинал
                    if (a >= targetK) {
                        if (candidates.size > 1) {
                            bestCurrent = candidates[1]
                        }
                    }
                }

                // 2. Отбираем варианты с лучшим током
                val bestVariants = variants.filter { abs(it.ratedCurrentA - bestCurrent) < 0.001f }

                // Убираем дубликаты (по току утечки и полюсам, так как Icu тут нет)
                val unique = bestVariants.distinctBy {
                    "${it.ratedResidualCurrent}|${it.polesText}"
                }
                result.addAll(unique)
            }
        }

        // Сортировка: ток, затем модель
        result.sortedWith(compareBy<RcdUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
    }

    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = false)) {
        Card(
            modifier = Modifier
                .widthIn(min = 500.dp, max = 850.dp)
                .heightIn(min = 240.dp, max = 600.dp)
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Подбор УЗО — подходящие варианты", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))

                // Инфо-панель
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Серия: ${selectedSeries ?: "—"}")
                    Spacer(Modifier.width(12.dp))
                    Text("Iрасч: ${consumerCurrentAStr.ifBlank { "—" }} A")
                    Spacer(Modifier.width(12.dp))
                    Text("U: ${consumerVoltageStr ?: "—"}")
                }

                Spacer(Modifier.height(8.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (errorMsg != null) {
                    Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
                } else {
                    if (finalList.isEmpty()) {
                        Text("Нет подходящих УЗО (по параметрам).", color = MaterialTheme.colors.onSurface)
                    } else {
                        // Заголовки таблицы
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("Производитель", modifier = Modifier.weight(1f))
                            Text("Модель", modifier = Modifier.weight(1f))
                            Text("In, A", modifier = Modifier.weight(0.6f))
                            Text("Утечка", modifier = Modifier.weight(0.8f))
                            Text("Полюса", modifier = Modifier.weight(0.6f))
                        }
                        Divider()

                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(finalList, key = { it.variantId }) { item ->
                                val isSelected = item.variantId == selectedVariantId
                                val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                                val borderModifier = if (isSelected) Modifier.border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(6.dp)) else Modifier

                                Card(
                                    elevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .then(borderModifier)
                                        .clickable { selectedVariantId = item.variantId },
                                    backgroundColor = bg
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                        Text(item.manufacturer, modifier = Modifier.weight(1f))
                                        Text(item.modelName, modifier = Modifier.weight(1f))
                                        Text(formatRated(item.ratedCurrentA), modifier = Modifier.weight(0.6f))
                                        Text(item.ratedResidualCurrent, modifier = Modifier.weight(0.8f))
                                        Text(item.polesText, modifier = Modifier.weight(0.6f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onBack() }) { Text("Назад") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onDismiss() }) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val chosen = finalList.firstOrNull { it.variantId == selectedVariantId }
                        if (chosen == null) return@Button

                        // Формирование строки результата
                        val ratedText = "${formatRated(chosen.ratedCurrentA)} A"
                        val residualText = chosen.ratedResidualCurrent
                        val line1 = chosen.modelName
                        val line2 = "$ratedText, $residualText"
                        val resultString = listOf(line1, line2).filter { it.isNotBlank() }.joinToString("\n")
                        onChoose(resultString)
                    }) { Text("Выбрать") }
                }
            }
        }
    }
}

// Вспомогательные функции
private fun parseAmps(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace("[^0-9.,]".toRegex(), "").replace(",", ".")
    return cleaned.toFloatOrNull()
}

private fun formatRated(v: Float): String {
    val i = v.toInt()
    return if (abs(v - i) < 0.001f) "$i" else String.format("%.1f", v)
}
