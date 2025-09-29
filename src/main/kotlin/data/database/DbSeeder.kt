package data.database

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

object DbSeeder {

    fun seedInitialData() {
        transaction {
            seedBreakers()
            seedRcds()
            seedRcbos()
        }
    }

    private fun seedBreakers() {
        // Data from "АВ.xlsx - БазаАВ.csv"
        val breakerData = """
            1,Nader,NDB1,NDB1-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            2,Nader,NDB1,NDB1T-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            3,Nader,NDB1,NDB1GQ-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","2P, 4P","OF1, SD1, MX+OF1, FF1, FS1"
        """.trimIndent().lines()

        breakerData.forEach { line ->
            val parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            if (parts.size < 10) return@forEach

            val model = DbBreakerModel(
                manufacturer = parts[1],
                series = parts[2],
                model = parts[3],
                breakingCapacity = "${parts[5]} кА"
            )

            // ИСПРАВЛЕНИЕ: Убрали .value, так как batchInsert возвращает Int
            val modelId = BreakerModels.batchInsert(listOf(model)) { m ->
                this[BreakerModels.manufacturer] = m.manufacturer
                this[BreakerModels.series] = m.series
                this[BreakerModels.model] = m.model
                this[BreakerModels.breakingCapacity] = m.breakingCapacity
            }.first()[BreakerModels.id]

            val serviceCapacity = "${parts[4]} кА"
            val curves = parseStringToList(parts[6])
            val currents = parseStringToFloatList(parts[7])
            val poles = parsePolesToList(parts[8])
            val additions = parseStringToList(parts[9])

            val variants = mutableListOf<DbBreakerVariant>()
            currents.forEach { current ->
                poles.forEach { pole ->
                    curves.forEach { curve ->
                        variants.add(
                            DbBreakerVariant(
                                modelId = modelId,
                                ratedCurrent = current,
                                poles = pole,
                                additions = "Кривая $curve; ${additions.joinToString()}",
                                serviceBreakingCapacity = serviceCapacity
                            )
                        )
                    }
                }
            }
            BreakerVariants.batchInsert(variants) { v ->
                this[BreakerVariants.modelId] = v.modelId
                this[BreakerVariants.ratedCurrent] = v.ratedCurrent
                this[BreakerVariants.poles] = v.poles
                this[BreakerVariants.additions] = v.additions
                this[BreakerVariants.serviceBreakingCapacity] = v.serviceBreakingCapacity
            }
        }
    }

    private fun seedRcds() {
        // Data from "УЗО.xlsx - БазаАВ.csv"
        val rcdData = """
            1,Nader,NDB6,NDL6M-125,"16, 25, 40, 63, 80, 100",4P," 30, 100, 300"
            2,Nader,NDB6,NDL6M-125,"16, 25, 40, 63, 80, 100",2P," 30, 100, 300"
        """.trimIndent().lines()

        rcdData.forEach { line ->
            val parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            if (parts.size < 7) return@forEach

            val model = DbRcdModel(
                manufacturer = parts[1],
                series = parts[2],
                model = parts[3]
            )

            // ИСПРАВЛЕНИЕ: Убрали .value
            val modelId = RcdModels.batchInsert(listOf(model)) { m ->
                this[RcdModels.manufacturer] = m.manufacturer
                this[RcdModels.series] = m.series
                this[RcdModels.model] = m.model
            }.first()[RcdModels.id]

            val currents = parseStringToFloatList(parts[4])
            val poles = parsePolesToList(parts[5]).firstOrNull() ?: 0
            val residualCurrents = parseStringToList(parts[6])

            val variants = mutableListOf<DbRcdVariant>()
            currents.forEach { current ->
                residualCurrents.forEach { residual ->
                    variants.add(
                        DbRcdVariant(
                            modelId = modelId,
                            ratedCurrent = current,
                            poles = poles,
                            ratedResidualCurrent = "$residual мА"
                        )
                    )
                }
            }
            RcdVariants.batchInsert(variants) { v ->
                this[RcdVariants.modelId] = v.modelId
                this[RcdVariants.ratedCurrent] = v.ratedCurrent
                this[RcdVariants.poles] = v.poles
                this[RcdVariants.ratedResidualCurrent] = v.ratedResidualCurrent
            }
        }
    }

    private fun seedRcbos() {
        // Data from "АВДТ.xlsx - БазаАВ.csv"
        val rcboData = """
            1,Nader,NDB1,NDB1LE-63,6,6," 30, 50, 100","B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            2,Nader,NDB1,NDB1LE-100,10,10,100,"C, D","50, 63, 80, 100","1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1"
            3,Nader,NDB2,NDB2LE-63,10,10," 30, 50, 100, 300","B, C, D","1, 2, 4, 6, 10, 16, 20, 25, 32, 40, 50, 63","2P, 4P","OF2, SD2, MX+OF2, Tm2, Tm2GQ, FF2, FS2"
        """.trimIndent().lines()

        rcboData.forEach { line ->
            val parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            if (parts.size < 11) return@forEach

            val model = DbRcboModel(
                manufacturer = parts[1],
                series = parts[2],
                model = parts[3],
                breakingCapacity = "${parts[5]} кА"
            )

            // ИСПРАВЛЕНИЕ: Убрали .value
            val modelId = RcboModels.batchInsert(listOf(model)) { m ->
                this[RcboModels.manufacturer] = m.manufacturer
                this[RcboModels.series] = m.series
                this[RcboModels.model] = m.model
                this[RcboModels.breakingCapacity] = m.breakingCapacity
            }.first()[RcboModels.id]

            val serviceCapacity = "${parts[4]} кА"
            val residualCurrents = parseStringToList(parts[6])
            val curves = parseStringToList(parts[7])
            val currents = parseStringToFloatList(parts[8])
            val poles = parsePolesToList(parts[9])
            val additions = parseStringToList(parts[10])

            val variants = mutableListOf<DbRcboVariant>()
            currents.forEach { current ->
                poles.forEach { pole ->
                    curves.forEach { curve ->
                        residualCurrents.forEach { residual ->
                            variants.add(
                                DbRcboVariant(
                                    modelId = modelId,
                                    ratedCurrent = current,
                                    poles = pole,
                                    ratedResidualCurrent = "$residual мА",
                                    additions = "Кривая $curve; ${additions.joinToString()}",
                                    serviceBreakingCapacity = serviceCapacity
                                )
                            )
                        }
                    }
                }
            }
            RcboVariants.batchInsert(variants) { v ->
                this[RcboVariants.modelId] = v.modelId
                this[RcboVariants.ratedCurrent] = v.ratedCurrent
                this[RcboVariants.poles] = v.poles
                this[RcboVariants.ratedResidualCurrent] = v.ratedResidualCurrent
                this[RcboVariants.additions] = v.additions
                this[RcboVariants.serviceBreakingCapacity] = v.serviceBreakingCapacity
            }
        }
    }

    // --- Helper Functions for Parsing ---

    private fun parseStringToList(input: String): List<String> {
        return input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseStringToFloatList(input: String): List<Float> {
        return parseStringToList(input).mapNotNull { it.toFloatOrNull() }
    }

    private fun parsePolesToList(input: String): List<Int> {
        return parseStringToList(input).mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
    }
}

