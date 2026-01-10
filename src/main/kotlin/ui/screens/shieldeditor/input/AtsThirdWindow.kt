package ui.screens.shieldeditor.input

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
 * Панель списка подходящих устройств АВР.
 * Встраивается в правую часть InputTypePopup ниже параметров.
 */
@Composable
fun AtsThirdWindow(
    maxShortCircuitCurrentStr: String,
    // consumerCurrentAStr больше не используется для фильтрации, но можно оставить в сигнатуре для совместимости
    consumerCurrentAStr: String,
    selectedSeries: String?,
    selectedPoles: String?,
    onChoose: (String) -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<AtsUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val requiredBreakingCapacity = parseAmps(maxShortCircuitCurrentStr) ?: 0f

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
                    // Фильтр по Icu оставляем (безопасность)
                    if (requiredBreakingCapacity > 0 && mCapVal < requiredBreakingCapacity) continue

                    val variants = AtsVariants.select {
                        (AtsVariants.modelId eq mId) and (AtsVariants.poles eq selectedPoles)
                    }

                    for (v in variants) {
                        val current = v[AtsVariants.ratedCurrent]
                        val vId = v[AtsVariants.id]

                        // УБРАНО: фильтрация по току потребителя (if current >= consumerA)

                        results.add(
                            AtsUiItem(
                                modelId = mId,
                                variantId = vId,
                                manufacturer = manufacturer,
                                modelName = mName,
                                ratedCurrentA = current,
                                polesText = selectedPoles!!,
                                breakingCapacityStr = mCapStr,
                                breakingCapacityVal = mCapVal
                            )
                        )
                    }
                }

                // УБРАНО: группировка и поиск "лучшего" (minOrNull)

                // Просто сортируем список: сначала по имени модели, затем по току
                items = results.sortedWith(
                    compareBy<AtsUiItem> { it.modelName }
                        .thenBy { it.ratedCurrentA }
                )

                selectedVariantId = items.firstOrNull()?.variantId
            }
        } catch (t: Throwable) {
            errorMsg = t.message ?: "Ошибка загрузки"
            t.printStackTrace()
        } finally {
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Подходящие устройства", style = MaterialTheme.typography.subtitle2, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMsg != null) {
            Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
        } else if (selectedSeries.isNullOrBlank() || selectedPoles.isNullOrBlank()) {
            Text("Выберите серию и полюса для поиска.", color = Color.Gray)
        } else if (items.isEmpty()) {
            Text("Нет подходящих устройств (проверьте Icu).", color = MaterialTheme.colors.onSurface)
        } else {
            // Заголовки таблицы
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Модель", modifier = Modifier.weight(2f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text("In, A", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text("Полюса", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                Text("Icu, кА", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            }
            Divider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(items, key = { it.variantId }) { item ->
                    val isSelected = item.variantId == selectedVariantId
                    val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent

                    // Элемент списка
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(bg)
                            .clickable {
                                selectedVariantId = item.variantId
                                val res = "${item.modelName} ${formatRated(item.ratedCurrentA)}A ${item.polesText}"
                                onChoose(res)
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.modelName, modifier = Modifier.weight(2f), style = MaterialTheme.typography.body2)
                        Text(formatRated(item.ratedCurrentA), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.body2)
                        Text(item.polesText, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.body2)
                        Text(item.breakingCapacityStr, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.body2)
                    }
                }
            }
        }
    }
}

private fun parseAmps(s: String?): Float? {
    if (s.isNullOrBlank()) return null
    val cleaned = s.replace(Regex("[^0-9.,]"), "").replace(",", ".")
    return cleaned.toFloatOrNull()
}

private fun formatRated(v: Float): String {
    val i = v.toInt()
    return if (abs(v - i) < 0.001f) "$i" else String.format("%.1f", v)
}
