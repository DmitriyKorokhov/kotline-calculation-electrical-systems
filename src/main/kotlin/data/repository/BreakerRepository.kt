package data.repository

import data.database.BreakerModels
import data.database.BreakerVariants
import data.database.DbBreakerModel
import data.database.DbBreakerVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun getDistinctSeries(): List<String> = withContext(Dispatchers.IO) {
    transaction {
        BreakerModels.selectAll()
            .map { it[BreakerModels.series] }
            .distinct()
    }
}

/**
 * Возвращает пары (model, variant) для заданной серии.
 */
suspend fun getVariantsBySeries(series: String): List<Pair<DbBreakerModel, DbBreakerVariant>> = withContext(Dispatchers.IO) {
    transaction {
        val joined = BreakerModels.join(
            BreakerVariants,
            JoinType.INNER,
            onColumn = BreakerModels.id,
            otherColumn = BreakerVariants.modelId
        )

        joined.select { BreakerModels.series eq series }.map { row ->
            val model = DbBreakerModel(
                id = row[BreakerModels.id],
                manufacturer = row[BreakerModels.manufacturer],
                series = row[BreakerModels.series],
                model = row[BreakerModels.model],
                breakingCapacity = row[BreakerModels.breakingCapacity]
            )
            val variant = DbBreakerVariant(
                id = row[BreakerVariants.id],
                modelId = row[BreakerVariants.modelId],
                ratedCurrent = row[BreakerVariants.ratedCurrent],
                polesText = row[BreakerVariants.polesText],
                additions = row[BreakerVariants.additions],
                serviceBreakingCapacity = row[BreakerVariants.serviceBreakingCapacity]
            )
            model to variant
        }
    }
}

/**
 * Утилиты для парсинга данных additions и т.п.
 */
fun parseCurveFromAdditions(additionsField: String?): String? {
    if (additionsField.isNullOrBlank()) return null
    val parts = additionsField.split(";").map { it.trim() }
    if (parts.isEmpty()) return null
    val first = parts[0]
    return if (first.startsWith("Кривая", ignoreCase = true)) {
        first.removePrefix("Кривая").trim().trimStart(':', '-', ' ')
    } else null
}

fun parseExtrasFromAdditions(additionsField: String?): List<String> {
    if (additionsField.isNullOrBlank()) return emptyList()
    val parts = additionsField.split(";").map { it.trim() }
    if (parts.size <= 1) return emptyList()
    val rest = parts.subList(1, parts.size).joinToString(";")
    return rest.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/**
 * Парсер polesText -> список отдельных вариантов полюсов.
 * Пример: "1P, 1P+N, 2P" -> ["1P","1P+N","2P"]
 */
fun parsePolesTextToList(polesText: String?): List<String> {
    if (polesText.isNullOrBlank()) return emptyList()
    return polesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

suspend fun getDistinctSeriesByManufacturer(manufacturer: String): List<String> = withContext(Dispatchers.IO) {
    transaction {
        BreakerModels.select { BreakerModels.manufacturer eq manufacturer }
            .map { it[BreakerModels.series] }
            .distinct()
    }
}
