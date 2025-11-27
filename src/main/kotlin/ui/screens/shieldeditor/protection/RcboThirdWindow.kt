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
import data.repository.getRcboVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions
import kotlin.math.abs

// Модель для UI списка АВДТ
data class RcboUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val ratedResidualCurrent: String, // Новое поле для остаточного тока
    val additionsRaw: String,
    val serviceBreakingCapacityKa: Float?,
    val breakingCapacityKa: Float?,
    val curve: String?
)

@Composable
fun RcboThirdWindow(
    maxShortCircuitCurrentStr: String,
    standard: String,
    consumerCurrentAStr: String,
    consumerVoltageStr: String?,
    selectedSeries: String?,
    selectedPoles: String?,
    selectedAdditions: List<String>,
    selectedCurve: String?,
    selectedResidualCurrent: String?, // Выбранный остаточный ток для фильтрации
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<RcboUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val maxKA = parseKa(maxShortCircuitCurrentStr) ?: 0f
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

            // Запрос к репозиторию АВДТ
            val pairs = getRcboVariantsBySeries(selectedSeries)

            items = pairs.map { (model, variant) ->
                RcboUiItem(
                    modelId = model.id,
                    variantId = variant.id,
                    manufacturer = model.manufacturer,
                    modelName = model.model,
                    ratedCurrentA = variant.ratedCurrent,
                    polesText = variant.poles,
                    ratedResidualCurrent = variant.ratedResidualCurrent,
                    additionsRaw = variant.additions,
                    serviceBreakingCapacityKa = parseKa(variant.serviceBreakingCapacity),
                    breakingCapacityKa = parseKa(model.breakingCapacity),
                    curve = parseCurveFromAdditions(variant.additions)
                )
            }
            loading = false
        } catch (t: Throwable) {
            loading = false
            errorMsg = t.message ?: "Ошибка при загрузке АВДТ"
        }
    }

    // Фильтрация элементов
    fun passesAll(item: RcboUiItem): Boolean {
        val passKZ = if (standard.contains("60898", ignoreCase = true)) {
            (item.breakingCapacityKa ?: 0f) >= maxKA
        } else {
            (item.serviceBreakingCapacityKa ?: 0f) >= maxKA
        }

        if (!passKZ) return false
        if (item.ratedCurrentA < consumerA) return false

        // Фильтр по полюсам
        if (!selectedPoles.isNullOrBlank()) {
            // Проверяем, содержится ли выбранный полюс в списке полюсов варианта (через запятую)
            if (item.polesText.split(",").map { it.trim() }.none { it == selectedPoles }) return false
        }

        // Фильтр по кривой
        if (!selectedCurve.isNullOrBlank()) {
            val itemCurve = item.curve ?: ""
            if (itemCurve.isBlank() || itemCurve != selectedCurve) return false
        }

        // Фильтр по остаточному току (утечке)
        if (!selectedResidualCurrent.isNullOrBlank()) {
            // Сравниваем строки (например "30 мА")
            if (item.ratedResidualCurrent.trim() != selectedResidualCurrent.trim()) return false
        }

        // Фильтр по дополнениям
        if (selectedAdditions.isNotEmpty()) {
            val itemExtras = parseExtrasFromAdditions(item.additionsRaw).map { it.trim() }
            if (!selectedAdditions.all { it in itemExtras }) return false
        }

        return true
    }

    // Вычисляем список подходящих вариантов
    val passing = remember(items, maxKA, consumerA, selectedPoles, selectedCurve, selectedResidualCurrent, selectedAdditions) {
        items.filter { passesAll(it) }
    }

    // Находим минимальный подходящий номинал тока
    val nearestRated = remember(passing, consumerA) {
        passing.map { it.ratedCurrentA }.filter { it >= consumerA }.minOrNull()
    }

    // Итоговый список для отображения (только с оптимальным номиналом)
    val finalList = remember(passing, nearestRated) {
        if (nearestRated == null) emptyList()
        else passing.filter { kotlin.math.abs(it.ratedCurrentA - nearestRated) < 0.0001f }
            .sortedBy { it.ratedCurrentA }
    }

    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = false)) {
        Card(
            modifier = Modifier
                .widthIn(min = 500.dp, max = 1000.dp) // Чуть шире из-за доп. колонки
                .heightIn(min = 240.dp, max = 600.dp)
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Подбор АВДТ — подходящие варианты", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))

                // Инфо-панель
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Серия: ${selectedSeries ?: "—"}")
                    Spacer(Modifier.width(12.dp))
                    Text("Ток КЗ: ${maxShortCircuitCurrentStr.ifBlank { "—" }} кА")
                    Spacer(Modifier.width(12.dp))
                    Text("Iрасч: ${consumerCurrentAStr.ifBlank { "—" }} A")
                    Spacer(Modifier.weight(1f))
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
                        Text("Нет подходящих АВДТ (по всем параметрам).", color = MaterialTheme.colors.onSurface)
                    } else {
                        // Заголовки таблицы
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("Производитель", modifier = Modifier.weight(1f))
                            Text("Модель", modifier = Modifier.weight(1f))
                            Text(if (standard.contains("60898", ignoreCase = true)) "Icn" else "Ics", modifier = Modifier.weight(0.6f))
                            if (!selectedCurve.isNullOrBlank()) Text("Кривая", modifier = Modifier.weight(0.6f))
                            Text("In, A", modifier = Modifier.weight(0.6f))
                            Text("Утечка", modifier = Modifier.weight(0.8f)) // Новая колонка
                            Text("Полюса", modifier = Modifier.weight(0.8f))
                            if (selectedAdditions.isNotEmpty()) Text("Дополн.", modifier = Modifier.weight(1f))
                        }
                        Divider()

                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(finalList) { item ->
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

                                        val capVal = if (standard.contains("60898", ignoreCase = true))
                                            item.breakingCapacityKa?.toString() ?: "-"
                                        else
                                            item.serviceBreakingCapacityKa?.toString() ?: "-"
                                        Text(capVal, modifier = Modifier.weight(0.6f))

                                        if (!selectedCurve.isNullOrBlank()) Text(selectedCurve ?: "-", modifier = Modifier.weight(0.6f))

                                        Text("${formatRated(item.ratedCurrentA)}", modifier = Modifier.weight(0.6f))
                                        Text(item.ratedResidualCurrent, modifier = Modifier.weight(0.8f)) // Значение утечки
                                        Text(item.polesText, modifier = Modifier.weight(0.8f))

                                        if (selectedAdditions.isNotEmpty()) {
                                            val itemExtras = parseExtrasFromAdditions(item.additionsRaw).map { it.trim() }
                                            val shown = selectedAdditions.filter { it in itemExtras }
                                            Text(shown.joinToString(", "), modifier = Modifier.weight(1f))
                                        }
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
                        val curveText = selectedCurve ?: chosen.curve ?: ""
                        val ratedText = "${formatRated(chosen.ratedCurrentA)} A"
                        val residualText = chosen.ratedResidualCurrent // Добавляем ток утечки в описание

                        val line1 = chosen.modelName
                        // Например: "C 16 A, 30 мА"
                        val line2 = if (curveText.isNotBlank()) "$curveText $ratedText, $residualText" else "$ratedText, $residualText"

                        val itemExtras = parseExtrasFromAdditions(chosen.additionsRaw).map { it.trim() }
                        val shownAdd = selectedAdditions.filter { it in itemExtras }
                        val line3 = if (shownAdd.isNotEmpty()) shownAdd.joinToString(", ") else ""

                        val resultString = listOf(line1, line2, line3).filter { it.isNotBlank() }.joinToString("\n")
                        onChoose(resultString)
                    }) { Text("Выбрать") }
                }
            }
        }
    }
}

// Вспомогательные функции парсинга (можно вынести в utils, но для краткости здесь)
private fun parseKa(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace("кА", "", true).replace("kA", "", true).replace(" ", "").replace(",", ".").trim()
    return cleaned.toFloatOrNull()
}

private fun parseAmps(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace("[^0-9.,]".toRegex(), "").replace(",", ".")
    return cleaned.toFloatOrNull()
}

private fun formatRated(v: Float): String {
    val i = v.toInt()
    return if (abs(v - i) < 0.001f) "$i" else String.format("%.1f", v)
}

