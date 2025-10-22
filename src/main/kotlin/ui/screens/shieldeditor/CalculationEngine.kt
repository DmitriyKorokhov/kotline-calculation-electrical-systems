// CalculationEngine.kt
package ui.screens.shieldeditor

import kotlin.math.sqrt
import kotlin.math.abs

object CalculationEngine {

    // Единая функция расчёта тока из мощности в кВт, с выбором одно/трёхфазной формулы
    private fun calcI(u: Double, cosPhi: Double, pKwStr: String?): Double? {
        val pW = NumberUtils.parseDouble(pKwStr) ?: return null
        val i = if (nearlyEquals(u, 400.0)) {
            pW / (u * cosPhi * sqrt(3.0))
        } else {
            pW / (u * cosPhi)
        }
        return if (i.isFinite()) i else null
    }

    // Главная кнопка "Произвести расчёт"
    fun calculateAll(shieldData: ShieldData): Int {
        var success = 0
        shieldData.consumers.forEach { c ->
            val u = NumberUtils.parseDouble(c.voltage)
            val cosPhi = NumberUtils.parseDouble(c.cosPhi) ?: 1.0
            if (u == null) {
                c.currentAMode1 = ""
                c.currentAMode2 = ""
                c.currentA = ""
                return@forEach
            }

            val i1 = calcI(u, cosPhi, c.powerKw)
            val i2 = if (c.dualMode) calcI(u, cosPhi, c.powerKwMode2) else null

            // Заполняем токи режимов только здесь
            c.currentAMode1 = i1?.let { NumberUtils.formatDoubleTwoDecimals(it) } ?: ""
            c.currentAMode2 = if (c.dualMode) (i2?.let { NumberUtils.formatDoubleTwoDecimals(it) } ?: "") else ""

            // Агрегируем для выбора защиты/экспорта
            val imax = listOfNotNull(i1, i2).maxOrNull()
            if (imax != null) {
                c.currentA = NumberUtils.formatDoubleTwoDecimals(imax)
                success++
            } else {
                c.currentA = ""
            }
        }
        return success
    }

    private fun nearlyEquals(a: Double, b: Double, eps: Double = 1.0): Boolean {
        return abs(a - b) <= eps
    }
}
