package ui.screens.shieldeditor.protection

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.repository.getVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions
import kotlin.math.abs

data class BreakerUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val additionsRaw: String,
    val serviceBreakingCapacityKa: Float?,
    val breakingCapacityKa: Float?,
    val curve: String?
)

/**
 * Список подходящих автоматов (Шаг 2).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreakerThirdWindow(
    maxShortCircuitCurrentStr: String,
    standard: String,
    consumerCurrentAStr: String,
    consumerVoltageStr: String?,
    selectedSeries: String?,
    selectedPoles: String?,
    selectedAdditions: List<String>,
    selectedCurve: String?,
    protectionThreshold: Float,
    protectionFactorLow: Float,
    protectionFactorHigh: Float,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<BreakerUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val maxKA = parseKa(maxShortCircuitCurrentStr) ?: 0f
    val consumerA = parseAmps(consumerCurrentAStr) ?: 0f

    var showAllSeriesDevices by remember { mutableStateOf(false) }

    // Функция для подтверждения выбора
    val confirmSelection = { item: BreakerUiItem ->
        val curveText = selectedCurve ?: item.curve ?: ""
        val ratedText = "${formatRated(item.ratedCurrentA)} A"
        val line1 = item.modelName
        val line2 = if (curveText.isNotBlank()) "$curveText $ratedText" else ratedText
        val itemExtras = parseExtrasFromAdditions(item.additionsRaw).map { it.trim() }
        val shownAdd = selectedAdditions.filter { it in itemExtras }
        val line3 = if (shownAdd.isNotEmpty()) shownAdd.joinToString(", ") else ""
        val resultString = listOf(line1, line2, line3).filter { it.isNotBlank() }.joinToString("\n")
        onChoose(resultString)
    }

    LaunchedEffect(selectedSeries) {
        loading = true
        errorMsg = null
        items = emptyList()
        try {
            if (selectedSeries.isNullOrBlank()) {
                loading = false
                return@LaunchedEffect
            }

            val pairs = getVariantsBySeries(selectedSeries)
            items = pairs.map { (model, variant) ->
                BreakerUiItem(
                    modelId = model.id,
                    variantId = variant.id,
                    manufacturer = model.manufacturer,
                    modelName = model.model,
                    ratedCurrentA = variant.ratedCurrent,
                    polesText = variant.polesText,
                    additionsRaw = variant.additions,
                    serviceBreakingCapacityKa = parseKa(variant.serviceBreakingCapacity),
                    breakingCapacityKa = parseKa(model.breakingCapacity),
                    curve = parseCurveFromAdditions(variant.additions)
                )
            }
            loading = false
        } catch (t: Throwable) {
            loading = false
            errorMsg = t.message ?: "Ошибка"
        }
    }

    // Логика фильтрации и сортировки (полностью сохранена)
    fun passesAll(item: BreakerUiItem): Boolean {
        val passKZ = if (standard.contains("60898", ignoreCase = true)) {
            (item.breakingCapacityKa ?: 0f) >= maxKA
        } else {
            (item.serviceBreakingCapacityKa ?: 0f) >= maxKA
        }
        if (!passKZ) return false

        if (!showAllSeriesDevices) {
            if (item.ratedCurrentA < consumerA) return false
        }

        if (!selectedPoles.isNullOrBlank()) {
            if (item.polesText.split(",").map { it.trim() }.none { it == selectedPoles }) return false
        }

        if (!selectedCurve.isNullOrBlank()) {
            val itemCurve = item.curve ?: ""
            if (itemCurve.isBlank() || itemCurve != selectedCurve) return false
        }

        if (selectedAdditions.isNotEmpty()) {
            val itemExtras = parseExtrasFromAdditions(item.additionsRaw).map { it.trim() }
            if (!selectedAdditions.all { it in itemExtras }) return false
        }

        return true
    }

    val passing = remember(items, maxKA, consumerA, selectedPoles, selectedCurve, selectedAdditions, showAllSeriesDevices) {
        items.filter { passesAll(it) }
    }

    val finalList = remember(
        passing,
        consumerA,
        protectionThreshold,
        protectionFactorLow,
        protectionFactorHigh,
        showAllSeriesDevices,
        standard
    ) {
        if (showAllSeriesDevices) {
            passing
                .groupBy { item ->
                    val modelKey = item.modelName.trim()
                    val currentKey = String.format("%.2f", item.ratedCurrentA)
                    modelKey to currentKey
                }
                .values
                .map { variants -> variants.minBy { it.variantId } } // или любая ваша логика выбора представителя
                .sortedWith(compareBy<BreakerUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
        } else {
            // Стандартная логика подбора (ветка else без изменений)
            val groupedByModel = passing.groupBy { it.modelName }
            val result = mutableListOf<BreakerUiItem>()
            for ((_, variants) in groupedByModel) {
                val candidates = variants.map { it.ratedCurrentA }.distinct().sorted()
                if (candidates.isNotEmpty()) {
                    val firstNominal = candidates[0]
                    var bestCurrent = firstNominal
                    if (firstNominal > 0) {
                        val a = consumerA / firstNominal
                        val targetK = if (consumerA < protectionThreshold) protectionFactorLow else protectionFactorHigh
                        if (a >= targetK) {
                            bestCurrent = if (candidates.size > 1) {
                                candidates[1]
                            } else {
                                firstNominal
                            }
                        }
                    }
                    val bestVariants = variants.filter { abs(it.ratedCurrentA - bestCurrent) < 0.001f }
                    result.addAll(bestVariants.distinctBy { it.breakingCapacityKa })
                }
            }
            result.sortedWith(compareBy<BreakerUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
        }
    }

    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Подбор автоматов — подходящие варианты", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Серия: ${selectedSeries ?: "—"}")
            Spacer(Modifier.width(12.dp))
            Text("Iрасч: ${consumerCurrentAStr.ifBlank { "—" }} A")
            Spacer(Modifier.weight(1f))
            Text("U: ${consumerVoltageStr ?: "—"}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showAllSeriesDevices,
                    onCheckedChange = { showAllSeriesDevices = it }
                )
                Spacer(Modifier.width(4.dp))
                Text("Все устройства серии", style = MaterialTheme.typography.body2)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
        } else {
            if (finalList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Нет подходящих автоматов (по всем параметрам).", color = MaterialTheme.colors.onSurface)
                }
            } else {
                // --- Настройка весов колонок ---
                val wManuf = 1.0f
                val wModel = 1.0f
                val wVolt = 0.5f
                val wCap = 0.6f
                val wCurve = 0.5f
                val wAmp = 0.5f
                val wPole = 0.6f

                val borderColor = Color.LightGray

                // --- Заголовки таблицы ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .border(1.dp, borderColor)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("Произв.", wManuf)
                    VerticalDivider(color = borderColor)
                    HeaderCell("Модель", wModel)
                    VerticalDivider(color = borderColor)
                    HeaderCell("U (В)", wVolt)
                    VerticalDivider(color = borderColor)
                    HeaderCell(if (standard.contains("60898", ignoreCase = true)) "Icn" else "Ics", wCap)

                    if (!selectedCurve.isNullOrBlank()) {
                        VerticalDivider(color = borderColor)
                        HeaderCell("Кривая", wCurve)
                    }

                    VerticalDivider(color = borderColor)
                    HeaderCell("In, A", wAmp)
                    VerticalDivider(color = borderColor)
                    HeaderCell("Полюса", wPole)
                }

                // --- Список элементов ---
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(finalList, key = { it.variantId }) { item ->
                        val isSelected = item.variantId == selectedVariantId

                        val bg =
                            if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                        val borderStroke = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colors.primary)
                        else
                            BorderStroke(1.dp, borderColor)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-1).dp)
                                .combinedClickable(
                                    onClick = { selectedVariantId = item.variantId },
                                    onDoubleClick = {
                                        selectedVariantId = item.variantId
                                        confirmSelection(item)
                                    }
                                ),
                            border = borderStroke,
                            color = bg,
                            elevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(vertical = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(item.manufacturer, wManuf)
                                VerticalDivider(color = borderColor)

                                TableCell(item.modelName, wModel)
                                VerticalDivider(color = borderColor)

                                TableCell(consumerVoltageStr ?: "-", wVolt)
                                VerticalDivider(color = borderColor)

                                val capVal = if (standard.contains("60898", ignoreCase = true))
                                    item.breakingCapacityKa?.toString() ?: "-"
                                else item.serviceBreakingCapacityKa?.toString() ?: "-"
                                TableCell(capVal, wCap)

                                if (!selectedCurve.isNullOrBlank()) {
                                    VerticalDivider(color = borderColor)
                                    TableCell(selectedCurve, wCurve)
                                }

                                VerticalDivider(color = borderColor)
                                TableCell(formatRated(item.ratedCurrentA), wAmp)

                                VerticalDivider(color = borderColor)
                                TableCell(item.polesText, wPole)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onBack() }) { Text("Назад к параметрам") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val chosen = finalList.firstOrNull { it.variantId == selectedVariantId }
                if (chosen != null) {
                    confirmSelection(chosen)
                }
            }) { Text("Выбрать") }
        }
    }
}

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

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RowScope.TableCell(text: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color)
    )
}