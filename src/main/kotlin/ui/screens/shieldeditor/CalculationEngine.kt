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


    fun calculateAll(shieldData: ShieldData): Int {
        var success = 0
        shieldData.consumers.forEach { c ->
            val u = NumberUtils.parseDouble(c.voltage)
            val cosPhi = NumberUtils.parseDouble(c.cosPhi) ?: 1.0
            if (u == null) {
                c.currentA = ""
                return@forEach
            }

            val i = calcI(u, cosPhi, c.powerKw)

            // Заполняем токи режимов только здесь
            c.currentA = i?.let { NumberUtils.formatDoubleTwoDecimals(it) } ?: ""


            // Агрегируем для выбора защиты/экспорта
            val imax = listOfNotNull(i).maxOrNull()
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
