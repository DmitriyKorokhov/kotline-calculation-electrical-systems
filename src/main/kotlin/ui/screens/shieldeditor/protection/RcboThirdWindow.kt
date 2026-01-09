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
import data.repository.getRcboVariantsBySeries
import data.repository.parseCurveFromAdditions
import data.repository.parseExtrasFromAdditions
import kotlin.math.abs

data class RcboUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val ratedResidualCurrent: String,
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
    var items by remember { mutableStateOf<List<RcboUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val maxKA = parseKa(maxShortCircuitCurrentStr) ?: 0f
    val consumerA = parseAmps(consumerCurrentAStr) ?: 0f

    LaunchedEffect(selectedSeries) {
        loading = true
        errorMsg = null
        items = emptyList()
        try {
            if (selectedSeries.isNullOrBlank()) {
                loading = false
                return@LaunchedEffect
            }
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

    // Фильтрация
    fun passesAll(item: RcboUiItem): Boolean {
        val passKZ = if (standard.contains("60898", ignoreCase = true)) {
            (item.breakingCapacityKa ?: 0f) >= maxKA
        } else {
            (item.serviceBreakingCapacityKa ?: 0f) >= maxKA
        }
        if (!passKZ) return false
        if (item.ratedCurrentA < consumerA) return false

        if (!selectedPoles.isNullOrBlank()) {
            if (item.polesText.split(",").map { it.trim() }.none { it == selectedPoles }) return false
        }
        if (!selectedCurve.isNullOrBlank()) {
            val itemCurve = item.curve ?: ""
            if (itemCurve.isBlank() || itemCurve != selectedCurve) return false
        }
        if (!selectedResidualCurrent.isNullOrBlank()) {
            if (item.ratedResidualCurrent.trim() != selectedResidualCurrent.trim()) return false
        }
        if (selectedAdditions.isNotEmpty()) {
            val itemExtras = parseExtrasFromAdditions(item.additionsRaw).map { it.trim() }
            if (!selectedAdditions.all { it in itemExtras }) return false
        }
        return true
    }

    val passing = remember(items, maxKA, consumerA, selectedPoles, selectedCurve, selectedResidualCurrent, selectedAdditions) {
        items.filter { passesAll(it) }
    }

    val finalList = remember(passing, consumerA, protectionThreshold, protectionFactorLow, protectionFactorHigh) {
        val grouped = passing.groupBy { it.modelName }
        val result = mutableListOf<RcboUiItem>()

        for ((_, variants) in grouped) {
            val candidates = variants.map { it.ratedCurrentA }.distinct().sorted()
            if (candidates.isNotEmpty()) {
                val firstNominal = candidates[0]
                var bestCurrent = firstNominal

                if (firstNominal > 0) {
                    val a = consumerA / firstNominal
                    val targetK = if (consumerA < protectionThreshold) protectionFactorLow else protectionFactorHigh
                    if (a >= targetK) {
                        if (candidates.size > 1) bestCurrent = candidates[1]
                    }
                }
                val bestVariants = variants.filter { abs(it.ratedCurrentA - bestCurrent) < 0.001f }

                // Фильтрация дубликатов по визуальным полям
                val uniqueVisuals = bestVariants.distinctBy {
                    DataClassKey(
                        it.ratedCurrentA,
                        it.ratedResidualCurrent,
                        it.breakingCapacityKa,
                        it.curve,
                        it.polesText
                    )
                }
                result.addAll(uniqueVisuals)
            }
        }
        result.sortedWith(compareBy<RcboUiItem> { it.ratedCurrentA }.thenBy { it.modelName })
    }

    // UI Content
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Подбор АВДТ — подходящие варианты", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
        } else {
            if (finalList.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Нет подходящих АВДТ (по всем параметрам).", color = MaterialTheme.colors.onSurface)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text("Производитель", modifier = Modifier.weight(1f))
                    Text("Модель", modifier = Modifier.weight(1f))
                    Text(if (standard.contains("60898", ignoreCase = true)) "Icn" else "Ics", modifier = Modifier.weight(0.6f))
                    if (!selectedCurve.isNullOrBlank()) Text("Кривая", modifier = Modifier.weight(0.6f))
                    Text("In, A", modifier = Modifier.weight(0.6f))
                    Text("Утечка", modifier = Modifier.weight(0.8f))
                    Text("Полюса", modifier = Modifier.weight(0.8f))
                }
                Divider()

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                val capVal = if (standard.contains("60898", ignoreCase = true))
                                    item.breakingCapacityKa?.toString() ?: "-"
                                else item.serviceBreakingCapacityKa?.toString() ?: "-"
                                Text(capVal, modifier = Modifier.weight(0.6f))
                                if (!selectedCurve.isNullOrBlank()) Text(selectedCurve, modifier = Modifier.weight(0.6f))
                                Text(formatRated(item.ratedCurrentA), modifier = Modifier.weight(0.6f))
                                Text(item.ratedResidualCurrent, modifier = Modifier.weight(0.8f))
                                Text(item.polesText, modifier = Modifier.weight(0.8f))
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
                    val residualText = chosen.ratedResidualCurrent
                    val line1 = chosen.modelName
                    val line2 = if (curveText.isNotBlank()) "$curveText $ratedText, $residualText" else "$ratedText, $residualText"
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

private data class DataClassKey(
    val current: Float,
    val residual: String,
    val cap: Float?,
    val curve: String?,
    val poles: String
)
