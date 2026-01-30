package ui.screens.shieldeditor.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import ui.screens.shieldeditor.protection.BreakerUiItem
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtsBreakerThirdWindow(
    maxShortCircuitCurrentStr: String,
    standard: String,
    selectedSeries: String?,
    selectedPoles: String?,
    selectedAdditions: List<String>,
    selectedCurve: String?,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<BreakerUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val maxKA = parseKa(maxShortCircuitCurrentStr) ?: 0f

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

    fun passesAll(item: BreakerUiItem): Boolean {
        val passKZ = if (standard.contains("60898", ignoreCase = true)) {
            (item.breakingCapacityKa ?: 0f) >= maxKA
        } else {
            (item.serviceBreakingCapacityKa ?: 0f) >= maxKA
        }
        if (!passKZ) return false

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

    val passing = remember(items, maxKA, selectedPoles, selectedCurve, selectedAdditions) {
        items.filter { passesAll(it) }
    }

    val finalList = remember(passing) {
        passing
            .groupBy { item ->
                val modelKey = item.modelName.trim()
                val currentKey = String.format("%.2f", item.ratedCurrentA)
                modelKey to currentKey
            }
            .values
            .map { variants -> variants.minByOrNull { it.variantId }!! }
            .sortedWith(compareBy<BreakerUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
    }

    // --- Настройка колонок ---
    val showCurve = !selectedCurve.isNullOrBlank()
    // 1. Производитель
    val wManuf = 0.65f
    // 2. Модель
    val wModel = 1.2f
    // 3. Ток КЗ
    val wIcu = 0.7f
    // 4. Кривая
    val wCurve = if (showCurve) 0.5f else 0f
    // 5. Номинал
    val wAmps = 0.6f
    // 6. Полюса
    val wPoles = 0.6f

    val borderColor = Color.LightGray

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Подбор автоматов — подходящие варианты", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Серия: ${selectedSeries ?: "—"}")
            Spacer(Modifier.width(12.dp))
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
                // --- Шапка таблицы ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min) // Важно для вертикальных разделителей
                        .border(1.dp, borderColor)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("Произв.", wManuf)
                    VerticalDivider(color = borderColor)
                    HeaderCell("Модель", wModel)
                    VerticalDivider(color = borderColor)
                    HeaderCell(if (standard.contains("60898", ignoreCase = true)) "Icn, кА" else "Ics, кА", wIcu)

                    if (showCurve) {
                        VerticalDivider(color = borderColor)
                        HeaderCell("Кривая", wCurve)
                    }

                    VerticalDivider(color = borderColor)
                    HeaderCell("In, A", wAmps)
                    VerticalDivider(color = borderColor)
                    HeaderCell("Полюса", wPoles)
                }

                // --- Список ---
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(finalList, key = { it.variantId }) { item ->
                        val isSelected = item.variantId == selectedVariantId

                        // Цвет фона: выделение или чередование
                        val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface

                        // Граница карточки меняется при выделении
                        val borderStroke = if (isSelected)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colors.primary)
                        else
                            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp)
                                .offset(y = (-1).dp)
                                .combinedClickable(
                                    onClick = { selectedVariantId = item.variantId },
                                    onDoubleClick = {
                                        selectedVariantId = item.variantId
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
                                ),
                            border = borderStroke,
                            color = bg,
                            elevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(text = item.manufacturer, weight = wManuf)
                                VerticalDivider(color = borderColor)

                                TableCell(text = item.modelName, weight = wModel)
                                VerticalDivider(color = borderColor)

                                val capVal = if (standard.contains("60898", ignoreCase = true))
                                    item.breakingCapacityKa?.toString() ?: "-"
                                else item.serviceBreakingCapacityKa?.toString() ?: "-"
                                TableCell(text = capVal, weight = wIcu)

                                if (showCurve) {
                                    VerticalDivider(color = borderColor)
                                    TableCell(text = selectedCurve ?: "-", weight = wCurve)
                                }

                                VerticalDivider(color = borderColor)
                                TableCell(text = formatRated(item.ratedCurrentA), weight = wAmps)

                                VerticalDivider(color = borderColor)
                                TableCell(text = item.polesText, weight = wPoles)
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
                    val curveText = selectedCurve ?: chosen.curve ?: ""
                    val ratedText = "${formatRated(chosen.ratedCurrentA)} A"
                    val line1 = chosen.modelName
                    val line2 = if (curveText.isNotBlank()) "$curveText $ratedText" else ratedText
                    val itemExtras = parseExtrasFromAdditions(chosen.additionsRaw).map { it.trim() }
                    val shownAdd = selectedAdditions.filter { it in itemExtras }
                    val line3 = if (shownAdd.isNotEmpty()) shownAdd.joinToString(", ") else ""
                    val resultString = listOf(line1, line2, line3).filter { it.isNotBlank() }.joinToString("\n")
                    onChoose(resultString)
                }
            }) { Text("Выбрать") }
        }
    }
}

// --- Вспомогательные компоненты ---
@Composable
fun RowScope.HeaderCell(text: String, weight: Float) {
    if (weight <= 0) return
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float) {
    if (weight <= 0) return
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(vertical = 8.dp, horizontal = 4.dp),
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
fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color)
    )
}


private fun parseKa(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace("кА", "", true).replace("kA", "", true).replace(" ", "").replace(",", ".").trim()
    return cleaned.toFloatOrNull()
}

private fun formatRated(v: Float): String {
    val i = v.toInt()
    return if (abs(v - i) < 0.001f) "$i" else String.format("%.1f", v)
}