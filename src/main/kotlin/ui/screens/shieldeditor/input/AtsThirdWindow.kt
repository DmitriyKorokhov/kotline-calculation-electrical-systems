package ui.screens.shieldeditor.input

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.database.AtsModels
import data.database.AtsVariants
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs

private data class AtsUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val breakingCapacityStr: String,
    val breakingCapacityVal: Float
)

/**
 * Панель списка подходящих устройств АВР (блочные).
 * ATS_BLOCK_TWO_INPUTS("Блок АВР на два ввода на общую шину")
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtsThirdWindow(
    maxShortCircuitCurrentStr: String,
    consumerCurrentAStr: String,
    selectedSeries: String?,
    selectedPoles: String?,
    onBack: () -> Unit,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<AtsUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val requiredBreakingCapacity = parseAmps(maxShortCircuitCurrentStr) ?: 0f

    // --- Настройка колонок (веса) ---
    val wManuf = 0.7f
    val wModel = 1.2f
    val wAmps = 0.5f
    val wPoles = 0.5f
    val wIcu = 0.6f

    val borderColor = Color.LightGray

    // Реактивная загрузка при изменении фильтров
    LaunchedEffect(selectedSeries, selectedPoles) {
        if (selectedSeries.isNullOrBlank() || selectedPoles.isNullOrBlank()) {
            items = emptyList()
            return@LaunchedEffect
        }

        loading = true
        errorMsg = null
        try {
            transaction {
                val models = AtsModels.select { AtsModels.series eq selectedSeries }
                    .map { Triple(it[AtsModels.id], it[AtsModels.model], it[AtsModels.breakingCapacity]) }

                val manufacturer = AtsModels.slice(AtsModels.manufacturer)
                    .select { AtsModels.series eq selectedSeries }
                    .limit(1)
                    .map { it[AtsModels.manufacturer] }
                    .singleOrNull() ?: ""

                val results = mutableListOf<AtsUiItem>()

                for ((mId, mName, mCapStr) in models) {
                    val mCapVal = parseAmps(mCapStr) ?: 0f
                    // Фильтр по Icu (безопасность)
                    if (requiredBreakingCapacity > 0 && mCapVal < requiredBreakingCapacity) continue

                    val variants = AtsVariants.select {
                        (AtsVariants.modelId eq mId) and (AtsVariants.poles eq selectedPoles)
                    }

                    for (v in variants) {
                        val current = v[AtsVariants.ratedCurrent]
                        val vId = v[AtsVariants.id]

                        results.add(
                            AtsUiItem(
                                modelId = mId,
                                variantId = vId,
                                manufacturer = manufacturer,
                                modelName = mName,
                                ratedCurrentA = current,
                                polesText = selectedPoles,
                                breakingCapacityStr = mCapStr,
                                breakingCapacityVal = mCapVal
                            )
                        )
                    }
                }

                items = results.sortedWith(
                    compareBy<AtsUiItem> { it.modelName }
                        .thenBy { it.ratedCurrentA }
                )

                selectedVariantId = null
            }
        } catch (t: Throwable) {
            errorMsg = t.message ?: "Ошибка загрузки"
            t.printStackTrace()
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("Подбор АВР — подходящие варианты", style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Серия: ${selectedSeries ?: "—"}")
            Spacer(Modifier.width(12.dp))
            if (requiredBreakingCapacity > 0) {
                Text("Мин. Icu: $maxShortCircuitCurrentStr", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
        } else if (selectedSeries.isNullOrBlank() || selectedPoles.isNullOrBlank()) {
            Text("Выберите серию и полюса для поиска.", color = Color.Gray)
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Нет подходящих устройств (проверьте Icu).", color = MaterialTheme.colors.onSurface)
            }
        } else {

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
                HeaderCell("In, A", wAmps)
                VerticalDivider(color = borderColor)
                HeaderCell("Полюса", wPoles)
                VerticalDivider(color = borderColor)
                HeaderCell("Icu, кА", wIcu)
            }

            // --- Список ---
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(items, key = { it.variantId }) { item ->
                    val isSelected = item.variantId == selectedVariantId

                    // Цвет фона и границы
                    val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                    val borderStroke = if (isSelected)
                        BorderStroke(2.dp, MaterialTheme.colors.primary)
                    else
                        BorderStroke(1.dp, borderColor)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-1).dp) // "Схлопывание" границ
                            .combinedClickable(
                                onClick = { selectedVariantId = item.variantId },
                                onDoubleClick = {
                                    selectedVariantId = item.variantId
                                    val res = "${item.manufacturer} ${item.modelName} ${formatRated(item.ratedCurrentA)}A ${item.polesText}"
                                    onChoose(res)
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

                            TableCell(text = formatRated(item.ratedCurrentA), weight = wAmps)
                            VerticalDivider(color = borderColor)

                            TableCell(text = item.polesText, weight = wPoles)
                            VerticalDivider(color = borderColor)

                            TableCell(text = item.breakingCapacityStr, weight = wIcu)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onBack() }) {
                    Text("Назад к параметрам")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val chosen = items.firstOrNull { it.variantId == selectedVariantId }
                        if (chosen != null) {
                            val res = "${chosen.manufacturer} ${chosen.modelName} ${formatRated(chosen.ratedCurrentA)}A ${chosen.polesText}"
                            onChoose(res)
                        }
                    },
                    enabled = selectedVariantId != null
                ) {
                    Text("Выбрать")
                }
            }
        }
    }
}

// --- Утилиты парсинга (оставляем private, так как в соседнем файле они тоже могут быть private) ---

private fun parseAmps(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace(Regex("[^0-9.,]"), "").replace(",", ".")
    return cleaned.toFloatOrNull()
}

private fun formatRated(v: Float): String {
    val i = v.toInt()
    return if (abs(v - i) < 0.001f) "$i" else String.format("%.1f", v)
}