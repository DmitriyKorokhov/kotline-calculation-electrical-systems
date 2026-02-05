package data.database

/**
 * Определение типа потребителя.
 * @param tag Тег блока для AutoCAD (имя файла блока без расширения).
 * @param name Отображаемое название в UI.
 */
data class ConsumerDefinition(
    val tag: String,
    val name: String
)

object ConsumerLibrary {
    // Тег блока, если название не найдено в списке
    const val FALLBACK_TAG = "INDEF_SYMB"

    // Список предопределенных значений согласно ТЗ
    val definitions = listOf(
        ConsumerDefinition("LIGHT", "Освещение"),
        ConsumerDefinition("SOCKET_1P", "Розеточная сеть"),
        ConsumerDefinition("ACC_CONTR", "Контроллер доступа"),
        ConsumerDefinition("SEC_SYST", "Охранная сигнализация")
    )

    /**
     * Поиск подходящих определений по введенному тексту.
     * Сортировка по алфавиту.
     */
    fun search(query: String): List<ConsumerDefinition> {
        if (query.isBlank()) return definitions.sortedBy { it.name }

        return definitions
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }
    }

    /**
     * Получить тег по точному названию для экспорта.
     * Возвращает null, если не найдено (тогда используем FALLBACK_TAG).
     */
    fun getTagByName(name: String): String? {
        return definitions.find { it.name.equals(name, ignoreCase = true) }?.tag
    }
}