package ui.screens.shieldeditor.calculation

import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import view.NumberUtils
import kotlin.math.abs

/**
 * Логика распределения фаз:
 *
 * Правила:
 * 1) Если у потребителя U ≈ 400 -> пометить "L1, L2, L3" и добавить его ток (currentA) ко всем трем суммарным фазам.
 * 2) Если U ≈ 230 -> потребитель однофазный. Для распределения:
 *    - Для первых трех однофазных подряд потребителей применяем последовательность L1, L2, L3.
 *    - Для всех последующих однофазных потребителей выбираем фазу с минимальной текущей нагрузкой (L1/L2/L3)
 *      и добавляем туда ток.
 * 3) После распределения заполняем в ShieldData суммарные поля phaseL1/phaseL2/phaseL3 (строки, два знака).
 *
 * Требует, чтобы CalculationEngine.calculateAll() уже заполнил consumer.currentA.
 */

object PhaseDistributor {
    private fun nearlyEquals(a: Double, b: Double, eps: Double = 1.0): Boolean {
        return abs(a - b) <= eps
    }

    fun distributePhases(shieldData: ShieldData) {
        var totalL1 = 0.0
        var totalL2 = 0.0
        var totalL3 = 0.0

        var singleIndex = 0 // счетчик однофазных встреченных для применения правила первых 3

        // helper to parse consumer.currentA
        fun parseCurrent(c: ConsumerModel): Double {
            return NumberUtils.parseDouble(c.currentA) ?: 0.0
        }

        // сначала сбросим назначения (на случай повторных вызовов)
        shieldData.consumers.forEach { it.phaseNumber = "" }

        shieldData.consumers.forEach { c ->
            val u = NumberUtils.parseDouble(c.voltage) ?: 0.0
            val current = parseCurrent(c)

            if (nearlyEquals(u, 400.0)) {
                // Трехфазный — присвоим все фазы
                c.phaseNumber = "L1, L2, L3"
                // добавляем ток в каждую фазу (для трехфазной формулы current это фазный/линейный ток — его несут все фазы)
                totalL1 += current
                totalL2 += current
                totalL3 += current
            } else {
                // Однофазный — распределяем
                when (singleIndex) {
                    0 -> {
                        c.phaseNumber = "L1"
                        totalL1 += current
                    }
                    1 -> {
                        c.phaseNumber = "L2"
                        totalL2 += current
                    }
                    2 -> {
                        c.phaseNumber = "L3"
                        totalL3 += current
                    }
                    else -> {
                        // выбираем фазу с минимальной нагрузки
                        val minPhase = minOf(totalL1, totalL2, totalL3)
                        when (minPhase) {
                            totalL1 -> { c.phaseNumber = "L1"; totalL1 += current }
                            totalL2 -> { c.phaseNumber = "L2"; totalL2 += current }
                            else -> { c.phaseNumber = "L3"; totalL3 += current }
                        }
                    }
                }
                singleIndex++
            }
        }

        // записываем суммарные значения в ShieldData в виде строк (2 знака)
        shieldData.phaseL1 = NumberUtils.formatDoubleTwoDecimals(totalL1)
        shieldData.phaseL2 = NumberUtils.formatDoubleTwoDecimals(totalL2)
        shieldData.phaseL3 = NumberUtils.formatDoubleTwoDecimals(totalL3)
    }
}