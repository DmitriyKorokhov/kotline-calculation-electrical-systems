package view

object NumberUtils {
    /**
     * Парсит строку в Double.
     * Убирает пробелы, заменяет запятую на точку и удаляет единицы измерения (Вт, W, кВт и т.д.).
     * Возвращает null если парсинг не удался.
     *
     * IMPORTANT: теперь мощность вводится в Ваттах (Вт). Если пользователь ввёл "2300" — это 2300 Вт.
     */
    fun parseDouble(raw: String?): Double? {
        if (raw == null) return null
        val s = raw.trim()
            .replace(" ", "")
            .replace(",", ".")
            .replace(Regex("(?i)кВт|kw|kW"), "") // на всякий случай, если писали с единицами
            .replace(Regex("(?i)Вт|w|W"), "")
        if (s.isEmpty()) return null
        return try {
            s.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    fun formatDoubleTwoDecimals(v: Double): String {
        return String.format("%.2f", v)
    }
}