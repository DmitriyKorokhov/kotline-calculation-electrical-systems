package ui.screens.shieldeditor.calculation

import data.database.CableCurrentRatings
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData

object CableCalculator {

    fun calculateCable(consumer: ConsumerModel, data: ShieldData) {
        val cableType = consumer.cableType
        val protectionStr = consumer.protectionDevice

        // 1. Если марка кабеля или автомат не выбраны — выходим
        if (cableType.isBlank() || protectionStr.isBlank()) return

        // 2. Парсим номинальный ток автомата (In)
        // Ищем число перед "A" (например "C 16 A" -> 16.0)
        val regex = Regex("""(\d+[.,]?\d*)\s*A""")
        val match = regex.find(protectionStr) ?: return
        val inAmps = match.groupValues[1].replace(",", ".").toFloatOrNull() ?: return

        // 3. Определяем коэффициент запаса (1.45 или 1.13)
        val kSafety = if (data.hasOverloadProtection) 1.45f else 1.13f
        val requiredCurrent = inAmps * kSafety

        // 4. Определяем материал кабеля (Al/Cu) по марке
        // 4.1. Определяем материал жилы (Al/Cu)
        val isAluminum = cableType.startsWith("А", ignoreCase = true)
        val materialCode = if (isAluminum) "Al" else "Cu"

        // 4.2. Определяем тип изоляции
        // Убираем префикс "А", если он есть, чтобы смотреть на код изоляции
        val typeWithoutConductor = if (isAluminum) cableType.substring(1) else cableType

        val insulationCode = when {
            // Если начинается на "Пв" -> Сшитый полиэтилен
            typeWithoutConductor.startsWith("Пв", ignoreCase = true) -> "XLPE"

            // Если начинается на "П" (но не Пв) -> Полимерная композиция (обычно приравнивается к PVC по токам)
            // ИЛИ начинается на "В" -> ПВХ
            typeWithoutConductor.startsWith("В", ignoreCase = true) ||
                    typeWithoutConductor.startsWith("П", ignoreCase = true) -> "PVC"

            // По умолчанию (например КГ...)
            else -> "PVC"
        }

        // 5. Определяем число жил (3 или 5)
        val voltage = consumer.voltage.toIntOrNull() ?: 230
        val cores = if (voltage >= 380) 5 else 3

        // 6. Коэффициент снижения тока для многожильных кабелей (0.93 для 5 жил)
        val kCores = if (cores == 5) 0.93f else 1.0f

        // 7. Способ прокладки
        val isGround = consumer.layingMethod.contains("Земля", ignoreCase = true)

        // 8. Подбор сечения из БД
        // Условие: (TableCurrent * kCores) >= requiredCurrent
        // => TableCurrent >= requiredCurrent / kCores
        val targetTableCurrent = requiredCurrent / kCores

        transaction {
            // Ищем подходящие записи: тот же материал, сортируем по току
            val query = CableCurrentRatings
                .selectAll()
                .where {
                    (CableCurrentRatings.material eq materialCode) and
                            (CableCurrentRatings.insulation eq insulationCode)
                }

            val rating = query.map { row ->
                val section = row[CableCurrentRatings.crossSection]
                val current = if (isGround) row[CableCurrentRatings.currentInGround]
                else row[CableCurrentRatings.currentInAir]
                section to current
            }
                .filter { (_, current) -> current >= targetTableCurrent } // Фильтруем те, что держат нагрузку
                .minByOrNull { (section, _) -> section } // Берем минимальное сечение

            // 9. Записываем результат
            if (rating != null) {
                val (bestSection, _) = rating
                // Форматируем: "3x2.5" или "5x4"
                // Удаляем .0 если целое число
                val sectionStr = if (bestSection % 1.0 == 0.0) bestSection.toInt().toString() else bestSection.toString()
                consumer.cableLine = "${cores}x$sectionStr"
            } else {
                consumer.cableLine = "Нет сечения" // Не нашли подходящего (ток слишком велик)
            }
        }
    }

    fun calculateVoltageDrop(consumer: ConsumerModel, data: ShieldData) {
        // 1. Считываем длину из ячейки и настройки добавок
        val lengthInput = consumer.cableLength.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (lengthInput == 0.0) {
            consumer.voltageDropV = ""
            return
        }

        val reserve = data.cableReservePercent.toDoubleOrNull() ?: 0.0
        val descent = data.cableDescentPercent.toDoubleOrNull() ?: 0.0
        val termination = data.cableTerminationMeters.toDoubleOrNull() ?: 0.0

        // Полная длина L (в метрах)
        val L = lengthInput * (1 + (reserve + descent) / 100) + termination

        // 2. Параметры нагрузки
        val U_nom = consumer.voltage.toDoubleOrNull() ?: 230.0
        val cosPhi = consumer.cosPhi.replace(",", ".").toDoubleOrNull() ?: 0.95
        val sinPhi = Math.sqrt(1 - cosPhi * cosPhi)
        val I = consumer.currentA.replace(",", ".").toDoubleOrNull() ?: 0.0

        // 3. Сечение S (мм²)
        // Парсим строку вида "3x2.5" или "5x16"
        val sectionRegex = Regex("""x(\d+[.,]?\d*)""")
        val match = sectionRegex.find(consumer.cableLine)
        val S = match?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()

        if (S == null || S == 0.0) {
            consumer.voltageDropV = "Нет сечения"
            return
        }

        // 4. Удельное сопротивление p (rho)
        val t = data.cableTemperature.toDoubleOrNull() ?: 65.0 // Температура из настроек
        val isCopper = data.cableMaterial == "Copper" // Материал из настроек

        val rho = if (isCopper) {
            0.018 * (1 + 0.00393 * (t - 20))
        } else {
            0.028 * (1 + 0.00403 * (t - 20))
        }

        // 5. Активное сопротивление всей линии R = p * L / S
        val R_line = (rho * L) / S

        // 6. Реактивное сопротивление X0 (Ом/м)
        val xMilli = data.cableInductiveResistance.replace(",", ".").toDoubleOrNull() ?: 0.08
        val X0 = xMilli / 1000.0 // мОм/м → Ом/м

        // 7. Расчет падения напряжения
        val isThreePhase = U_nom >= 380
        val dU = if (isThreePhase) {
            // 400В: sqrt(3) * I * (R*cos + X0*L*sin)
            // (X0 * L — это полное реактивное сопротивление линии)
            1.732 * I * (R_line * cosPhi + X0 * L * sinPhi)
        } else {
            // 230В: 2 * I * (R*cos + X0*L*sin)
            2.0 * I * (R_line * cosPhi + X0 * L * sinPhi)
        }

        // 8. Проверка на допустимый %
        val dU_Percent = (dU / U_nom) * 100
        val maxDrop = data.maxVoltageDropPercent.toDoubleOrNull() ?: 5.0

        val warning = if (dU_Percent > maxDrop) " (!)" else ""

        // Форматирование: "5.2 В (2.1%)"
        consumer.voltageDropV = String.format("%.2f В (%.2f%%)%s", dU, dU_Percent, warning)
    }

    fun calculateShortCircuitCurrent(consumer: ConsumerModel, data: ShieldData) {
        // 1. Считываем Макс. ток КЗ системы (кА -> А)
        val ikMaxKA = data.maxShortCircuitCurrent.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (ikMaxKA <= 0.0) {
            consumer.shortCircuitCurrentkA = ""
            return
        }

        // 2. Определяем напряжение и тип сети
        // Если 380 или 400 и выше — считаем трехфазное КЗ (множитель 1)
        // Иначе (220, 230) — считаем однофазное КЗ петли фаза-ноль (множитель 2)
        val voltageInput = consumer.voltage.toIntOrNull() ?: 230
        val isThreePhase = voltageInput >= 380

        // Множитель сопротивления кабеля:
        // Для 3-фазного (Iкз(3)): учитываем сопротивление одной жилы (фазы).
        // Для 1-фазного (Iкз(1)): учитываем петлю фаза-нуль (2 жилы).
        val loopMultiplier = if (isThreePhase) 1.0 else 2.0

        // Напряжение для расчета всегда фазное (230В), т.к. эквивалентная схема считается на фазу
        val uPhase = 230.0

        // 3. Сопротивление системы (Xсист), приведенное к 230В
        // Xсист = 230 / Iкз_сист(А)
        val xSystem = uPhase / (ikMaxKA * 1000)

        // 4. Параметры кабеля
        val lengthInput = consumer.cableLength.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (lengthInput == 0.0) {
            consumer.shortCircuitCurrentkA = ""
            return
        }

        val reserve = data.cableReservePercent.toDoubleOrNull() ?: 0.0
        val descent = data.cableDescentPercent.toDoubleOrNull() ?: 0.0
        val termination = data.cableTerminationMeters.toDoubleOrNull() ?: 0.0
        val L = lengthInput * (1 + (reserve + descent) / 100) + termination

        // Сечение S
        val sectionRegex = Regex("""[xхXХ]\s*(\d+[.,]?\d*)""")
        val match = sectionRegex.find(consumer.cableLine)
        val S = match?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()

        if (S == null || S <= 0.0) {
            consumer.shortCircuitCurrentkA = ""
            return
        }

        // Удельное сопротивление (rho)
        val t = data.cableTemperature.toDoubleOrNull() ?: 65.0
        val isCopper = data.cableMaterial == "Copper"
        val rho = if (isCopper) {
            0.018 * (1 + 0.00393 * (t - 20))
        } else {
            0.028 * (1 + 0.00403 * (t - 20))
        }

        // 5. Расчет R и X кабеля с учетом множителя (1 или 2)
        val rCable = (rho * L / S) * loopMultiplier

        val xMilli = data.cableInductiveResistance.replace(",", ".").toDoubleOrNull() ?: 0.08
        val xCable = (xMilli / 1000.0 * L) * loopMultiplier

        // 6. Полное сопротивление Z
        // Z = корень(R^2 + (Xкабеля + Xсистемы)^2)
        val rTotal = rCable
        val xTotal = xCable + xSystem // Xсистемы добавляется к реактивному сопротивлению

        val Z = kotlin.math.sqrt(rTotal * rTotal + xTotal * xTotal)

        // 7. Находим ток КЗ
        if (Z > 0) {
            val ikEndA = uPhase / Z
            val ikEndKA = ikEndA / 1000.0
            consumer.shortCircuitCurrentkA = String.format("%.3f", ikEndKA)
        } else {
            consumer.shortCircuitCurrentkA = "∞"
        }
    }
}


