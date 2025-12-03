package data.database

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

object DbSeeder {

    fun seedInitialData() {
        transaction {
            seedBreakers()
            seedRcds()
            seedRcbos()
            seedCables()
        }
    }

    private fun seedBreakers() {
        // Data from "АВ.xlsx - БазаАВ.csv"
        val breakerData = """
            1,Nader,NDB1,NDB1-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            2,Nader,NDB1,NDB1T-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            3,Nader,NDB1,NDB1GQ-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","2P, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            4,Nader,NDB1,NDB1PT-63,6,6,A,"3, 6, 10","1P, 2P, 3P, 4P","OF1, SD1, MX+OF1"
            5,Nader,NDB1,NDB1-125,10,10,"C, D","50, 63, 80, 100, 125","1P, 2P, 3P, 4P","OF1, SD1, MX+OF1"
            6,Nader,NDB1,NDB1-40,6,6,"B, C, D","2, 4, 6, 10, 16, 20, 25, 32, 40",1P+N,"OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            7,Nader,NDB6,NDB6-125,15,15,"C, D","63, 80, 100, 125","1P, 2P, 3P, 4P",
            8,Nader,NDB2,NDB2-63,10,10,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P","OF2, SD2, MX+OF2, Tm2, Tm2GQ, NGQ2(A), JS1-11Y, B2-63"
            9,Nader,NDB2,NDB2N-63,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","OF2, SD2, MX+OF2, Tm2, Tm2GQ, JS1-11Y, B2-63"
            10,Systeme electric,Systeme9,S9FN,7.5,6,"B, C, D","1, 1.6, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P",
            11,Systeme electric,Systeme9,S9FH,11.25,10,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P",
            12,Systeme electric,Systeme9,S9HH,7.5,10,"B, C, D","63, 80, 100, 125","1P, 2P, 3P, 4P",
            13,DEKraft,BA-101,BA-101 4.5 кА,4.5,4.5,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 1P+N, 2P, 3P, 3P+N, 4P","КЗ-101, ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
            14,DEKraft,BA-103,BA-103 6 кА,6,6,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P","КЗ-101, ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
            15,DEKraft,BA-103,BA-103M 6 кА,6,6,"B, C, D","6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P","КЗ-101, ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
            16,DEKraft,BA-105,BA-105 10 кА,10,10,"B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P, 2P, 3P, 4P","КЗ-105, ДК-105, СК-105, РМК-105, РМН-105, РММ-105"
            17,DEKraft,BA-201,BA-201 10 кА,10,10,"B, C, D","63, 80, 100, 125","1P, 2P, 3P, 4P","КЗ-201, ДК-201, СК-201, РМК-201, РМН-201, РММ-201"
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

            val modelId = BreakerModels.batchInsert(listOf(model)) { m ->
                this[BreakerModels.manufacturer] = m.manufacturer
                this[BreakerModels.series] = m.series
                this[BreakerModels.model] = m.model
                this[BreakerModels.breakingCapacity] = m.breakingCapacity
            }.first()[BreakerModels.id]

            val serviceCapacity = "${parts[4]} кА"
            val curves = parseStringToList(parts[6])
            val currents = parseStringToFloatList(parts[7])
            val polesRaw = parts[8]
            val polesTextList = parseStringToList(polesRaw)
            val additions = parseStringToList(parts[9])

            val variants = mutableListOf<DbBreakerVariant>()
            currents.forEach { current ->
                curves.forEach { curve ->
                    polesTextList.forEach { poleText ->
                        variants.add(
                            DbBreakerVariant(
                                modelId = modelId,
                                ratedCurrent = current,
                                polesText = poleText,
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
                this[BreakerVariants.polesText] = v.polesText
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
            3,Systeme electric,Systeme9,S9R,"16, 25, 40, 63, 80, 100","2P, 4P","10, 30, 100, 300"
            4,DEKraft,УЗО,УЗО-03 6 кА,"10, 16, 25, 40, 63, 80, 100","2P, 4P","10, 30, 100, 300"
        """.trimIndent().lines()

        rcdData.forEach { line ->
            val parts = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            if (parts.size < 7) return@forEach

            val model = DbRcdModel(
                manufacturer = parts[1],
                series = parts[2],
                model = parts[3]
            )

            val modelId = RcdModels.batchInsert(listOf(model)) { m ->
                this[RcdModels.manufacturer] = m.manufacturer
                this[RcdModels.series] = m.series
                this[RcdModels.model] = m.model
            }.first()[RcdModels.id]

            val currents = parseStringToFloatList(parts[4])
            val poles = parts[5]
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
        val rcboData = """
            1,Nader,NDB1,NDB1LE-63,6,6,"30, 50, 100","B, C, D","1, 2, 3, 4, 5, 6, 10, 16, 20, 25, 32, 40, 50, 63","1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1, MX+OF1, GQ1A, FF1, FS1"
            2,Nader,NDB1,NDB1LE-100,10,10,100,"C, D","50, 63, 80, 100","1P+N, 2P, 3P, 3P+N, 4P","OF1, SD1"
            3,Nader,NDB2,NDB2LE-63,10,10,"30, 50, 100, 300","B, C, D","1, 2, 4, 6, 10, 16, 20, 25, 32, 40, 50, 63","2P, 4P","OF2, SD2, MX+OF2, Tm2, Tm2GQ, NGQ2(A), FS2, FF2"
            4,Systeme electric,Systeme9,S9D,6,6,"10, 30, 100, 300","B, C","4, 6, 10, 16, 25, 32, 40",1P+N,
            5,DEKraft,ДИФ-101,ДИФ-101 4.5 кА,4.5,4.5,"30, 100, 300","C, D","6, 10, 16, 20, 25, 32, 40, 50, 63","1P+N, 2P, 3P, 3P+N, 4P","ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
            6,DEKraft,ДИФ-103,ДИФ-103 4.5 кА,4.5,4.5,30,"C, D","6, 10, 16, 20, 25, 32, 40, 50, 63",1P+N,"ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
            7,DEKraft,ДИФ-103,ДИФ-103 6 кА,6,6,"10, 30, 100, 300",C,"6, 10, 16, 20, 25, 32, 40, 50, 63","1P+N, 3P+N","ДК-101, СК-101, РМК-101, РМН-101, РММ-101"
        """.trimIndent().lines()

        rcboData.forEach { line ->
            val parts = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)".toRegex()).map { it.replace("\"", "").trim() }
            if (parts.size < 11) return@forEach

            val model = DbRcboModel(
                manufacturer = parts[1],
                series = parts[2],
                model = parts[3],
                breakingCapacity = "${parts[5]} кА"
            )

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
            val poles = parseStringToList(parts[9])
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

    private fun seedCables() {
        val cableData = """
            ВВГнг(А)
            ВВГнг(А)-LS
            ВВГнг(А)-FRLS
            ПвПГнг(А)
            ПвПГнг(А)-HF
            ПвПГнг(А)-FRHF
            ППГнг(А)
            ППГнг(А)-HF
            ППГнг(А)-FRHF
            KГВВнг(А)
            KГВВнг(А)-LS
            KГВВнг(А)-FRLS
            КГППнг(А)
            КГППнг(А)-HF
            КГППнг(А)-FRHF
        """.trimIndent().lines()

        val cables = cableData
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { DbCable(type = it) }

        Cables.batchInsert(cables) { cable ->
            this[Cables.type] = cable.type
        }
    }

    private fun parseStringToList(input: String): List<String> {
        return input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseStringToFloatList(input: String): List<Float> {
        return parseStringToList(input).mapNotNull { it.toFloatOrNull() }
    }
}

