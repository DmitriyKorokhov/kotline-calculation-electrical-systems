package ui.screens.shieldeditor.calculation

import data.database.CableCurrentRatings
import data.database.Cables
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
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
            val query = CableCurrentRatings.select {
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
}


