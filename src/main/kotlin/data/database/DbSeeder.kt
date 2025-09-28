package data.database

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Singleton object for seeding the database with initial data.
 * This runs only on the first launch of the application.
 */
object DbSeeder {

    fun seedInitialData() {
        transaction {
            // --- 1. Seed Breaker Models (Автоматические выключатели) ---
            val breakerModelId = BreakerModels.batchInsert(
                listOf(
                    DbBreakerModel(manufacturer = "Schneider Electric", series = "Easy9", model = "EZ9F341", breakingCapacity = "4.5 кА"),
                    DbBreakerModel(manufacturer = "EKF", series = "Averes", model = "VA 47-63", breakingCapacity = "6 кА")
                )
            ) { model ->
                this[BreakerModels.manufacturer] = model.manufacturer
                this[BreakerModels.series] = model.series
                this[BreakerModels.model] = model.model
                this[BreakerModels.breakingCapacity] = model.breakingCapacity
            }.first()[BreakerModels.id].value

            // Seed Variants for the first Breaker Model
            BreakerVariants.batchInsert(
                listOf(
                    DbBreakerVariant(modelId = breakerModelId, ratedCurrent = 6f, poles = 1, additions = "", serviceBreakingCapacity = "100%"),
                    DbBreakerVariant(modelId = breakerModelId, ratedCurrent = 10f, poles = 1, additions = "", serviceBreakingCapacity = "100%"),
                    DbBreakerVariant(modelId = breakerModelId, ratedCurrent = 16f, poles = 1, additions = "", serviceBreakingCapacity = "100%"),
                    DbBreakerVariant(modelId = breakerModelId, ratedCurrent = 25f, poles = 3, additions = "", serviceBreakingCapacity = "75%")
                )
            ) { variant ->
                this[BreakerVariants.modelId] = variant.modelId
                this[BreakerVariants.ratedCurrent] = variant.ratedCurrent
                this[BreakerVariants.poles] = variant.poles
                this[BreakerVariants.additions] = variant.additions
                this[BreakerVariants.serviceBreakingCapacity] = variant.serviceBreakingCapacity
            }

            // --- 2. Seed RCD Models (УЗО) ---
            val rcdModelId = RcdModels.batchInsert(
                listOf(
                    DbRcdModel(manufacturer = "Schneider Electric", series = "Easy9", model = "EZ9R342")
                )
            ) { model ->
                this[RcdModels.manufacturer] = model.manufacturer
                this[RcdModels.series] = model.series
                this[RcdModels.model] = model.model
            }.first()[RcdModels.id].value

            // Seed Variants for RCD Model
            RcdVariants.batchInsert(
                listOf(
                    DbRcdVariant(modelId = rcdModelId, ratedCurrent = 25f, ratedResidualCurrent = "30 мА", poles = 2),
                    DbRcdVariant(modelId = rcdModelId, ratedCurrent = 40f, ratedResidualCurrent = "30 мА", poles = 2),
                    DbRcdVariant(modelId = rcdModelId, ratedCurrent = 63f, ratedResidualCurrent = "100 мА", poles = 4)
                )
            ) { variant ->
                this[RcdVariants.modelId] = variant.modelId
                this[RcdVariants.ratedCurrent] = variant.ratedCurrent
                this[RcdVariants.ratedResidualCurrent] = variant.ratedResidualCurrent
                this[RcdVariants.poles] = variant.poles
            }

            // --- 3. Seed Cables (Кабели) ---
            Cables.batchInsert(
                listOf(
                    DbCable(type = "ВВГнг(А)-LS 3х", crossSection = 1.5f, continuousCurrent = 21f),
                    DbCable(type = "ВВГнг(А)-LS 3х", crossSection = 2.5f, continuousCurrent = 27f),
                    DbCable(type = "ВВГнг(А)-LS 3х", crossSection = 4f, continuousCurrent = 38f),
                    DbCable(type = "ВВГнг(А)-LS 3х", crossSection = 6f, continuousCurrent = 49f),
                    DbCable(type = "ВВГнг(А)-LS 5х", crossSection = 1.5f, continuousCurrent = 19f),
                    DbCable(type = "ВВГнг(А)-LS 5х", crossSection = 2.5f, continuousCurrent = 25f)
                )
            ) { cable ->
                this[Cables.type] = cable.type
                this[Cables.crossSection] = cable.crossSection
                this[Cables.continuousCurrent] = cable.continuousCurrent
            }

            // RCBOs (АВДТ) can be added here in the same way
        }
    }
}
