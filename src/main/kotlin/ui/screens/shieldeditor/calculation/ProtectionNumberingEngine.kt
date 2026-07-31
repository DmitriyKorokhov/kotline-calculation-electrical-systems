package ui.screens.shieldeditor.calculation

import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.protection.ProtectionType

object ProtectionNumberingEngine {

    fun applyNumbering(data: ShieldData) {
        // 1. Нумерация устройств защиты
        when (data.numberingOrder) {
            "Parallel" -> applyParallelNumbering(data)
            "Sequential" -> applySequentialNumbering(data)
            "Other" -> {
                // Ничего не делаем, пользователь вводит номера вручную
            }
            else -> applyParallelNumbering(data)
        }

        // 2. Нумерация групп (линий)
        applyGroupNumbering(data)
    }

    private fun applyGroupNumbering(data: ShieldData) {
        if (data.groupNumberingOrder == "Auto") {
            val shieldPrefix = data.shieldName.ifBlank { "Щит" }

            // Определяем порядок обхода списка для групп
            val consumersToProcess = if (data.numberingLeftToRight) data.consumers else data.consumers.reversed()

            var groupIndex = 1
            for (consumer in consumersToProcess) {
                consumer.lineName = "$shieldPrefix.$groupIndex"
                groupIndex++
            }
        }
    }

    private fun applyParallelNumbering(data: ShieldData) {
        var countQF = 1
        var countQFD = 1
        var countQD = 1

        // Определяем порядок обхода списка
        val consumersToProcess = if (data.numberingLeftToRight) data.consumers else data.consumers.reversed()

        for (consumer in consumersToProcess) {
            // 1. Нумерация основной защиты
            if (consumer.protectionDevice.isNotBlank()) {
                val mainType = try {
                    ProtectionType.valueOf(consumer.protectionType)
                } catch (e: Exception) {
                    ProtectionType.CIRCUIT_BREAKER
                }

                consumer.breakerNumber = when (mainType) {
                    ProtectionType.CIRCUIT_BREAKER -> "QF${countQF++}"
                    ProtectionType.DIFF_CURRENT_BREAKER -> "QFD${countQFD++}"
                    ProtectionType.RCD -> "QD${countQD++}"
                }
            } else {
                consumer.breakerNumber = ""
            }

            // 2. Нумерация дополнительных защит (сверху вниз остается неизменным)
            for (addProt in consumer.additionalProtections) {
                if (addProt.protectionDevice.isNotBlank()) {
                    val addType = try {
                        ProtectionType.valueOf(addProt.protectionType)
                    } catch (e: Exception) {
                        ProtectionType.CIRCUIT_BREAKER
                    }

                    addProt.breakerNumber = when (addType) {
                        ProtectionType.CIRCUIT_BREAKER -> "QF${countQF++}"
                        ProtectionType.DIFF_CURRENT_BREAKER -> "QFD${countQFD++}"
                        ProtectionType.RCD -> "QD${countQD++}"
                    }
                } else {
                    addProt.breakerNumber = ""
                }
            }
        }
    }

    private fun applySequentialNumbering(data: ShieldData) {
        var globalCount = 1

        // Определяем порядок обхода списка
        val consumersToProcess = if (data.numberingLeftToRight) data.consumers else data.consumers.reversed()

        for (consumer in consumersToProcess) {
            // 1. Нумерация основной защиты
            if (consumer.protectionDevice.isNotBlank()) {
                val mainType = try {
                    ProtectionType.valueOf(consumer.protectionType)
                } catch (e: Exception) {
                    ProtectionType.CIRCUIT_BREAKER
                }

                consumer.breakerNumber = when (mainType) {
                    ProtectionType.CIRCUIT_BREAKER -> "QF${globalCount++}"
                    ProtectionType.DIFF_CURRENT_BREAKER -> "QFD${globalCount++}"
                    ProtectionType.RCD -> "QD${globalCount++}"
                }
            } else {
                consumer.breakerNumber = ""
            }

            // 2. Нумерация дополнительных защит
            for (addProt in consumer.additionalProtections) {
                if (addProt.protectionDevice.isNotBlank()) {
                    val addType = try {
                        ProtectionType.valueOf(addProt.protectionType)
                    } catch (e: Exception) {
                        ProtectionType.CIRCUIT_BREAKER
                    }

                    addProt.breakerNumber = when (addType) {
                        ProtectionType.CIRCUIT_BREAKER -> "QF${globalCount++}"
                        ProtectionType.DIFF_CURRENT_BREAKER -> "QFD${globalCount++}"
                        ProtectionType.RCD -> "QD${globalCount++}"
                    }
                } else {
                    addProt.breakerNumber = ""
                }
            }
        }
    }
}