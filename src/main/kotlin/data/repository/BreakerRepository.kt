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
import data.database.RcboModels
import data.database.RcboVariants
import data.database.DbRcboModel
import data.database.DbRcboVariant
import data.database.RcdModels
import data.database.RcdVariants
import data.database.DbRcdModel
import data.database.DbRcdVariant

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

suspend fun getDistinctSeriesByManufacturer(manufacturer: String): List<String> = withContext(Dispatchers.IO) {
    transaction {
        BreakerModels.select { BreakerModels.manufacturer eq manufacturer }
            .map { it[BreakerModels.series] }
            .distinct()
    }
}

suspend fun getDistinctRcboSeries(): List<String> = withContext(Dispatchers.IO) {
    transaction {
        RcboModels.selectAll()
            .map { it[RcboModels.series] }
            .distinct()
    }
}

/**
 * Получить список уникальных серий АВДТ по производителю.
 */
suspend fun getDistinctRcboSeriesByManufacturer(manufacturer: String): List<String> = withContext(Dispatchers.IO) {
    transaction {
        RcboModels.select { RcboModels.manufacturer eq manufacturer }
            .map { it[RcboModels.series] }
            .distinct()
    }
}

/**
 * Возвращает пары (model, variant) для заданной серии АВДТ.
 */
suspend fun getRcboVariantsBySeries(series: String): List<Pair<DbRcboModel, DbRcboVariant>> = withContext(Dispatchers.IO) {
    transaction {
        val joined = RcboModels.join(
            RcboVariants,
            JoinType.INNER,
            onColumn = RcboModels.id,
            otherColumn = RcboVariants.modelId
        )

        joined.select { RcboModels.series eq series }.map { row ->
            val model = DbRcboModel(
                id = row[RcboModels.id],
                manufacturer = row[RcboModels.manufacturer],
                series = row[RcboModels.series],
                model = row[RcboModels.model],
                breakingCapacity = row[RcboModels.breakingCapacity]
            )

            val variant = DbRcboVariant(
                id = row[RcboVariants.id],
                modelId = row[RcboVariants.modelId],
                ratedCurrent = row[RcboVariants.ratedCurrent],
                ratedResidualCurrent = row[RcboVariants.ratedResidualCurrent],
                poles = row[RcboVariants.poles],
                additions = row[RcboVariants.additions],
                serviceBreakingCapacity = row[RcboVariants.serviceBreakingCapacity]
            )

            model to variant
        }
    }
}

// --- Функции для УЗО (RCD) ---

suspend fun getDistinctRcdSeries(): List<String> = withContext(Dispatchers.IO) {
    transaction {
        RcdModels.selectAll()
            .map { it[RcdModels.series] }
            .distinct()
    }
}

suspend fun getDistinctRcdSeriesByManufacturer(manufacturer: String): List<String> = withContext(Dispatchers.IO) {
    transaction {
        RcdModels.select { RcdModels.manufacturer eq manufacturer }
            .map { it[RcdModels.series] }
            .distinct()
    }
}

suspend fun getRcdVariantsBySeries(series: String): List<Pair<DbRcdModel, DbRcdVariant>> = withContext(Dispatchers.IO) {
    transaction {
        val joined = RcdModels.join(
            RcdVariants,
            JoinType.INNER,
            onColumn = RcdModels.id,
            otherColumn = RcdVariants.modelId
        )

        joined.select { RcdModels.series eq series }.map { row ->
            val model = DbRcdModel(
                id = row[RcdModels.id],
                manufacturer = row[RcdModels.manufacturer],
                series = row[RcdModels.series],
                model = row[RcdModels.model]
            )

            val variant = DbRcdVariant(
                id = row[RcdVariants.id],
                modelId = row[RcdVariants.modelId],
                ratedCurrent = row[RcdVariants.ratedCurrent],
                ratedResidualCurrent = row[RcdVariants.ratedResidualCurrent],
                // Обратите внимание: если у вас в базе poles это Int, используйте row[RcdVariants.poles].toString()
                poles = row[RcdVariants.poles] // Предполагаем String по вашему запросу исправления БД
            )

            model to variant
        }
    }
}
