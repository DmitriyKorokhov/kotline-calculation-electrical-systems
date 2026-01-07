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
import data.database.AtsModels
import data.database.AtsVariants
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs

data class AtsUiItem(
    val modelId: Int,
    val variantId: Int,
    val manufacturer: String,
    val modelName: String,
    val ratedCurrentA: Float,
    val polesText: String,
    val breakingCapacityStr: String, // Icu
    val breakingCapacityVal: Float // числовое значение для сортировки
)

// АВР
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
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<AtsUiItem>>(emptyList()) }
    var selectedVariantId by remember { mutableStateOf<Int?>(null) }

    val consumerA = parseAmps(consumerCurrentAStr) ?: 0f
    val requiredBreakingCapacity = parseAmps(maxShortCircuitCurrentStr) ?: 0f

    LaunchedEffect(selectedSeries, selectedPoles) {
        loading = true
        errorMsg = null
        items = emptyList()

        try {
            if (selectedSeries.isNullOrBlank() || selectedPoles.isNullOrBlank()) {
                loading = false
                return@LaunchedEffect
            }

            transaction {
                val models = AtsModels.select { AtsModels.series eq selectedSeries }
                    .map {
                        Triple(
                            it[AtsModels.id],
                            it[AtsModels.model],
                            it[AtsModels.breakingCapacity]
                        )
                    }
                val manufacturer = AtsModels.slice(AtsModels.manufacturer)
                    .select { AtsModels.series eq selectedSeries }
                    .limit(1)
                    .map { it[AtsModels.manufacturer] }
                    .singleOrNull() ?: ""

                val results = mutableListOf<AtsUiItem>()

                for ((mId, mName, mCapStr) in models) {
                    val mCapVal = parseAmps(mCapStr) ?: 0f

                    if (requiredBreakingCapacity > 0 && mCapVal < requiredBreakingCapacity) continue

                    val variants = AtsVariants.select {
                        (AtsVariants.modelId eq mId) and (AtsVariants.poles eq selectedPoles)
                    }

                    for (v in variants) {
                        val current = v[AtsVariants.ratedCurrent]
                        val vId = v[AtsVariants.id]

                        if (current >= consumerA) {
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
                }

                // 1. Группируем по НАЗВАНИЮ МОДЕЛИ (modelName)
                val groupedByModel = results.groupBy { it.modelName }

                val finalItems = mutableListOf<AtsUiItem>()

                // 2. Для каждой модели ищем лучший вариант (минимальный подходящий ток)
                for ((_, variants) in groupedByModel) {
                    val bestCurrent = variants.map { it.ratedCurrentA }.minOrNull()
                    if (bestCurrent != null) {
                        // Берем все варианты этой модели с лучшим током (обычно он один, но вдруг дубли)
                        val bestVariants = variants.filter { abs(it.ratedCurrentA - bestCurrent) < 0.001f }
                        // Чтобы не дублировать совсем одинаковые, делаем distinct
                        finalItems.addAll(bestVariants.distinctBy { it.breakingCapacityVal })
                    }
                }

                // Сортировка: по току, потом по названию
                items = finalItems.sortedWith(
                    compareBy<AtsUiItem> { it.ratedCurrentA }
                        .thenBy { it.modelName }
                )

                selectedVariantId = items.firstOrNull()?.variantId
            }
            loading = false

        } catch (t: Throwable) {
            loading = false
            errorMsg = t.message ?: "Ошибка при загрузке АВР"
            t.printStackTrace()
        }
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 600.dp, max = 900.dp)
                .heightIn(min = 300.dp, max = 700.dp)
                .padding(12.dp),
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Подбор АВР — подходящие варианты", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Серия: ${selectedSeries ?: "—"}", style = MaterialTheme.typography.body2)
                    Spacer(Modifier.width(16.dp))
                    Text("Iрасч: ${consumerCurrentAStr.ifBlank { "0" }} A", style = MaterialTheme.typography.body2)
                    Spacer(Modifier.width(16.dp))
                    Text("Icu треб: ${maxShortCircuitCurrentStr.ifBlank { "0" }} кА", style = MaterialTheme.typography.body2)
                }

                Spacer(Modifier.height(12.dp))

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (errorMsg != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Ошибка: $errorMsg", color = MaterialTheme.colors.error)
                    }
                } else if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Нет подходящих устройств (проверьте параметры тока и Icu).", color = MaterialTheme.colors.onSurface)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Модель", modifier = Modifier.weight(2f), style = MaterialTheme.typography.subtitle2)
                        Text("In, A", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.subtitle2)
                        Text("Полюса", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.subtitle2)
                        Text("Icu, кА", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.subtitle2)
                    }
                    Divider()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(items, key = { it.variantId }) { item ->
                            val isSelected = item.variantId == selectedVariantId
                            val bg = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else MaterialTheme.colors.surface
                            val borderModifier = if (isSelected)
                                Modifier.border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(6.dp))
                            else
                                Modifier

                            Card(
                                elevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .then(borderModifier)
                                    .clickable { selectedVariantId = item.variantId },
                                backgroundColor = bg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.modelName, modifier = Modifier.weight(2f))
                                    Text(formatRated(item.ratedCurrentA), modifier = Modifier.weight(0.8f))
                                    Text(item.polesText, modifier = Modifier.weight(0.8f))
                                    Text(item.breakingCapacityStr, modifier = Modifier.weight(0.8f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onBack) { Text("Назад") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val chosen = items.firstOrNull { it.variantId == selectedVariantId }
                            if (chosen != null) {
                                val res = "${chosen.modelName} ${formatRated(chosen.ratedCurrentA)}A ${chosen.polesText}"
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
