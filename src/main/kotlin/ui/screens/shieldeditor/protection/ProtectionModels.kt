package ui.screens.shieldeditor.protection

/**
 * Типы устройств защиты/коммутации.
 */
enum class ProtectionType {
    CIRCUIT_BREAKER,                 // Автоматический выключатель
    DIFF_CURRENT_BREAKER,            // Автоматический выключатель дифференциального тока
    RCD        // Автоматический выключатель и Устройство защитного отключения
}

/**
 * Отображаемое имя для каждого типа.
 */
fun ProtectionType.displayName(): String = when (this) {
    ProtectionType.CIRCUIT_BREAKER -> "Автоматический выключатель"
    ProtectionType.DIFF_CURRENT_BREAKER -> "Автоматический выключатель дифференциального тока"
    ProtectionType.RCD -> "Устройство защитного отключения"
}

/**
 * Попытаться сопоставить строку из consumer.protectionDevice с ProtectionType.
 * Если не удалось — вернёт значение по умолчанию (CIRCUIT_BREAKER).
 */
fun protectionTypeFromString(s: String?): ProtectionType {
    if (s.isNullOrBlank()) return ProtectionType.CIRCUIT_BREAKER

    if (s == ProtectionType.DIFF_CURRENT_BREAKER.displayName()) return ProtectionType.DIFF_CURRENT_BREAKER
    if (s == ProtectionType.RCD.displayName()) return ProtectionType.RCD

    if (s.contains("мА", ignoreCase = true) ||
        s.contains("mA", ignoreCase = true) ||
        s.contains("АВДТ", ignoreCase = true) ||
        s.contains("Diff", ignoreCase = true)) {
        return ProtectionType.DIFF_CURRENT_BREAKER
    }

    if (s.contains("УЗО", ignoreCase = true) && s.contains("AV", ignoreCase = true)) {
        return ProtectionType.RCD
    }

    return ProtectionType.CIRCUIT_BREAKER
}

