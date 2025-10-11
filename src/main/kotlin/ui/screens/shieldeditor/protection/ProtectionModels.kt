package ui.screens.shieldeditor.protection

/**
 * Типы устройств защиты/коммутации.
 */
enum class ProtectionType {
    CIRCUIT_BREAKER,                 // Автоматический выключатель
    DIFF_CURRENT_BREAKER,            // Автоматический выключатель дифференциального тока
    CIRCUIT_BREAKER_AND_RCD         // Автоматический выключатель и Устройство защитного отключения
}

/**
 * Отображаемое имя (русское) для каждого типа.
 */
fun ProtectionType.displayName(): String = when (this) {
    ProtectionType.CIRCUIT_BREAKER -> "Автоматический выключатель"
    ProtectionType.DIFF_CURRENT_BREAKER -> "Автоматический выключатель дифференциального тока"
    ProtectionType.CIRCUIT_BREAKER_AND_RCD -> "Автоматический выключатель и Устройство защитного отключения"
}

/**
 * Попытаться сопоставить строку из consumer.protectionDevice с ProtectionType.
 * Если не удалось — вернёт значение по умолчанию (CIRCUIT_BREAKER).
 */
fun protectionTypeFromString(s: String?): ProtectionType {
    return when (s) {
        ProtectionType.CIRCUIT_BREAKER.displayName() -> ProtectionType.CIRCUIT_BREAKER
        ProtectionType.DIFF_CURRENT_BREAKER.displayName() -> ProtectionType.DIFF_CURRENT_BREAKER
        ProtectionType.CIRCUIT_BREAKER_AND_RCD.displayName() -> ProtectionType.CIRCUIT_BREAKER_AND_RCD
        else -> ProtectionType.CIRCUIT_BREAKER
    }
}
