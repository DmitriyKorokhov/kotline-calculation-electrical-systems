package ui.screens.shieldeditor.calculation

import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import view.NumberUtils
import kotlin.math.abs

object PhaseDistributor {
    private fun nearlyEquals(a: Double, b: Double, eps: Double = 1.0): Boolean {
        return abs(a - b) <= eps
    }

    fun distributePhases(shieldData: ShieldData) {
        var totalL1 = 0.0
        var totalL2 = 0.0
        var totalL3 = 0.0

        fun parseCurrent(c: ConsumerModel): Double {
            return NumberUtils.parseDouble(c.currentA) ?: 0.0
        }

        if (shieldData.phaseDistributionMode == "Auto") {
            // ==========================================
            // РЕЖИМ 1: АВТОМАТИЧЕСКАЯ БАЛАНСИРОВКА
            // ==========================================
            shieldData.consumers.forEach { it.phaseNumber = "" }

            // 1. Сначала распределяем все трехфазные потребители
            shieldData.consumers.forEach { c ->
                val u = NumberUtils.parseDouble(c.voltage) ?: 0.0
                if (nearlyEquals(u, 400.0)) {
                    val current = parseCurrent(c)
                    c.phaseNumber = "L1, L2, L3"
                    totalL1 += current
                    totalL2 += current
                    totalL3 += current
                }
            }

            // 2. Отбираем однофазные потребители и сортируем их по убыванию тока
            val singlePhaseConsumers = shieldData.consumers.filter { c ->
                val u = NumberUtils.parseDouble(c.voltage) ?: 0.0
                !nearlyEquals(u, 400.0)
            }.sortedByDescending { parseCurrent(it) }

            // 3. Распределяем однофазные потребители
            singlePhaseConsumers.forEach { c ->
                val current = parseCurrent(c)
                val minPhase = minOf(totalL1, totalL2, totalL3)

                when (minPhase) {
                    totalL1 -> {
                        c.phaseNumber = "L1"
                        totalL1 += current
                    }
                    totalL2 -> {
                        c.phaseNumber = "L2"
                        totalL2 += current
                    }
                    else -> {
                        c.phaseNumber = "L3"
                        totalL3 += current
                    }
                }
            }
        } else {
            // ==========================================
            // РЕЖИМ 2: РУЧНОЙ ВВОД ("Other")
            // ==========================================
            // Не перезаписываем фазы, а просто суммируем токи на основе пользовательского ввода
            shieldData.consumers.forEach { c ->
                val current = parseCurrent(c)
                val phaseStr = c.phaseNumber.uppercase()

                // Проверяем наличие подстрок "L1", "L2", "L3" во введенном тексте
                // Если потребитель трехфазный и пользователь ввел "L1, L2, L3", ток добавится ко всем трем
                if (phaseStr.contains("L1")) totalL1 += current
                if (phaseStr.contains("L2")) totalL2 += current
                if (phaseStr.contains("L3")) totalL3 += current
            }
        }

        // Записываем итоговые значения суммарных токов в ShieldData для отображения в левой панели
        shieldData.phaseL1 = NumberUtils.formatDoubleTwoDecimals(totalL1)
        shieldData.phaseL2 = NumberUtils.formatDoubleTwoDecimals(totalL2)
        shieldData.phaseL3 = NumberUtils.formatDoubleTwoDecimals(totalL3)
    }
}