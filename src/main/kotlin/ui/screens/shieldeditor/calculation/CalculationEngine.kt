package ui.screens.shieldeditor.calculation

import ui.screens.shieldeditor.ShieldData
import view.NumberUtils
import kotlin.math.abs

object CalculationEngine {

    private fun parse(value: String?): Double = NumberUtils.parseDouble(value) ?: 0.0

    /**
     * Выполняет полный пересчет данных щита.
     */
    fun calculateAll(data: ShieldData) {
        // 1. Расчет токов для каждого потребителя
        data.consumers.forEach { c ->
            val u = parse(c.voltage)
            val cosPhi = NumberUtils.parseDouble(c.cosPhi) ?: 1.0 // Если null, берем 1.0
            val pW = parse(c.powerKw) * 1000

            if (u > 0) {
                // Формула тока: I = P / (U * cosPhi * [sqrt(3) для 3 фаз])
                // Если U ~ 400В, считаем это 3 фазы
                val isThreePhase = abs(u - 400.0) < 5.0
                val denominator = if (isThreePhase) u * cosPhi * 1.732 else u * cosPhi

                val i = if (denominator > 0.001) pW / denominator else 0.0
                c.currentA = NumberUtils.formatDoubleTwoDecimals(i)
            } else {
                c.currentA = ""
            }
        }

        // 2. Расчет общих параметров щита
        val demandFactor = data.demandFactor.toFloatOrNull() ?: 1.0f
        val simultaneityFactor = data.simultaneityFactor.toFloatOrNull() ?: 1.0f

        // А) Установленная мощность (сумма мощностей всех потребителей в кВт)
        val totalInstalledW = data.consumers.sumOf { parse(it.installedPowerW) }
        data.totalInstalledPower = "%.2f".format(totalInstalledW)

        // Б) Расчетная мощность (Вт)
        // P_calc = P_installed * Kc * Ko
        val totalCalculatedW = data.consumers.sumOf { parse(it.powerKw) } * demandFactor * simultaneityFactor
        data.totalCalculatedPower = "%.2f".format(totalCalculatedW)

        // В) Средний cos(φ)
        val cosPhiValues = data.consumers.mapNotNull { NumberUtils.parseDouble(it.cosPhi) }
        val avgCosPhi = if (cosPhiValues.isNotEmpty()) cosPhiValues.average() else 0.0
        data.averageCosPhi = "%.3f".format(avgCosPhi)

        // Г) Общий ток щита (А)
        // I_total = P_calc / (1.732 * 400 * cos_avg)
        val totalCurrent = if (avgCosPhi > 0.001) {
            totalCalculatedW * 1000 / (1.732 * 400.0 * avgCosPhi)
        } else {
            0.0
        }
        data.totalCurrent = "%.2f".format(totalCurrent)

        // Д) Коэф. спроса щита
        // Кс_щита = Р_расч / Р_уст
        val shieldDemand = if (totalInstalledW > 0.001) totalCalculatedW / totalInstalledW else 0.0
        data.shieldDemandFactor = "%.2f".format(shieldDemand)
    }
}