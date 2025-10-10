package ui.screens.shieldeditor

import kotlin.math.sqrt
import kotlin.math.abs

object CalculationEngine {
    /**
     * Рассчитать ток для одного потребителя.
     * - powerRaw: берётся из consumer.powerKw (теперь это Вт).
     * - cosPhi: consumer.cosPhi (если неверно — принимаем 1.0)
     * - U: consumer.voltage (ожидается 230 или 400, но можно и другое значение)
     *
     * Формулы:
     *  - U == 230 -> I = P / (U * cosf)
     *  - U == 400 -> I = P / (U * cosf * sqrt(3))
     *
     * Возвращает Double? — значение тока в амперах, либо null при ошибке парсинга.
     */
    fun calculateCurrentFor(consumer: ConsumerModel): Double? {
        val p = NumberUtils.parseDouble(consumer.powerKw) ?: return null // теперь мощность в Вт
        val cosPhi = NumberUtils.parseDouble(consumer.cosPhi) ?: 1.0
        val u = NumberUtils.parseDouble(consumer.voltage) ?: return null

        val i = if (nearlyEquals(u, 400.0)) {
            // трехфазный
            p / (u * cosPhi * sqrt(3.0))
        } else {
            // однофазный (или другое напряжение)
            p / (u * cosPhi)
        }

        return if (i.isFinite()) i else null
    }

    /**
     * Произвести расчёт для всех потребителей: заполнить consumer.currentA (строка)
     * Возвращает количество успешно рассчитанных потребителей.
     */
    fun calculateAll(shieldData: ShieldData): Int {
        var success = 0
        shieldData.consumers.forEach { c ->
            val i = calculateCurrentFor(c)
            if (i != null) {
                c.currentA = NumberUtils.formatDoubleTwoDecimals(i)
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
