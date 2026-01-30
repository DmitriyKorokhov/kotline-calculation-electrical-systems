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
import data.repository.getRcdVariantsBySeries
import kotlin.math.abs

data class RcdUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val ratedResidualCurrent: String
)

@OptIn(ExperimentalFoundationApi::class)
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

    var showAllSeriesDevices by remember { mutableStateOf(false) }

    // Функция подтверждения выбора
    fun confirmSelection(item: RcdUiItem) {
        val ratedText = "${formatRated(item.ratedCurrentA)} A"
        val residualText = item.ratedResidualCurrent
        val line1 = item.modelName
        val line2 = "$ratedText, $residualText"
        val resultString = listOf(line1, line2).filter { it.isNotBlank() }.joinToString("\n")
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

    // Фильтрация
    fun passesAll(item: RcdUiItem): Boolean {
        if (!showAllSeriesDevices) {
            if (item.ratedCurrentA < consumerA) return false
        }
        if (!selectedPoles.isNullOrBlank()) {
            if (item.polesText.trim() != selectedPoles.trim()) return false
        }
        if (!selectedResidualCurrent.isNullOrBlank()) {
            if (item.ratedResidualCurrent.trim() != selectedResidualCurrent.trim()) return false
        }
        return true
    }

    val passing = remember(items, consumerA, selectedPoles, selectedResidualCurrent, showAllSeriesDevices) {
        items.filter { passesAll(it) }
    }

    val finalList = remember(
        passing,
        consumerA,
        protectionThreshold,
        protectionFactorLow,
        protectionFactorHigh,
        showAllSeriesDevices
    ) {
        if (showAllSeriesDevices) {
            passing
                // схлопываем до уникальных строк (как у вас уже сделано в else)
                .distinctBy { "${it.modelName.trim()}|${it.ratedCurrentA}|${it.ratedResidualCurrent.trim()}|${it.polesText.trim()}" }
                .sortedWith(compareBy<RcdUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
        } else {
            val grouped = passing.groupBy { it.modelName }
            val result = mutableListOf<RcdUiItem>()

            for ((_, variants) in grouped) {
                val candidates = variants.map { it.ratedCurrentA }.distinct().sorted()
                if (candidates.isNotEmpty()) {
                    val firstNominal = candidates[0]
                    var bestCurrent = firstNominal

                    if (firstNominal > 0) {
                        val a = consumerA / firstNominal
                        val targetK = if (consumerA < protectionThreshold) protectionFactorLow else protectionFactorHigh
                        if (a >= targetK && candidates.size > 1) {
                            bestCurrent = candidates[1]
                        }
                    }

                    val bestVariants = variants.filter { abs(it.ratedCurrentA - bestCurrent) < 0.001f }
                    val unique = bestVariants.distinctBy { it.ratedResidualCurrent + it.polesText }
                    result.addAll(unique)
                }
            }

            result.sortedWith(compareBy<RcdUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
        }
    }

    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Подбор УЗО — подходящие варианты", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Серия: ${selectedSeries ?: "—"}")
            Spacer(Modifier.width(12.dp))
            Text("Iрасч: ${consumerCurrentAStr.ifBlank { "—" }} A")
            Spacer(Modifier.width(12.dp))
            Text("U: ${consumerVoltageStr ?: "—"}")
            Spacer(Modifier.weight(1f))

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
                    Text("Нет подходящих УЗО (по параметрам).", color = MaterialTheme.colors.onSurface)
                }
            } else {
                // --- Таблица ---

                // Веса колонок
                val wManuf = 1.0f
                val wModel = 1.0f
                val wAmp = 0.6f
                val wRes = 0.8f
                val wPole = 0.6f

                val borderColor = Color.LightGray

                // Заголовок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .border(1.dp, borderColor)
                        .background(Color.LightGray.copy(alpha = 0.2f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("Произв.", wManuf)
                    VerticalDivider(borderColor)
                    HeaderCell("Модель", wModel)
                    VerticalDivider(borderColor)
                    HeaderCell("In, A", wAmp)
                    VerticalDivider(borderColor)
                    HeaderCell("Утечка", wRes)
                    VerticalDivider(borderColor)
                    HeaderCell("Полюса", wPole)
                }

                // Список
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(finalList, key = { it.variantId }) { item ->
                        val isSelected = item.variantId == selectedVariantId

                        val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                        val borderStroke = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colors.primary)
                        else
                            BorderStroke(1.dp, borderColor)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-1).dp) // Схлопывание границ
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
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(item.manufacturer, wManuf)
                                VerticalDivider(borderColor)
                                TableCell(item.modelName, wModel)
                                VerticalDivider(borderColor)
                                TableCell(formatRated(item.ratedCurrentA), wAmp)
                                VerticalDivider(borderColor)
                                TableCell(item.ratedResidualCurrent, wRes)
                                VerticalDivider(borderColor)
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
