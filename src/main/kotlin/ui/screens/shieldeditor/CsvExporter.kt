package ui.screens.shieldeditor

import java.io.File
import java.nio.charset.StandardCharsets
import ui.screens.shieldeditor.protection.ProtectionType
import ui.screens.shieldeditor.protection.protectionTypeFromString

/**
 * Экспорт в CSV для AutoCAD / Lisp.
 */
class CsvExporter {

    data class ExportEntry(
        val blockTypePrefix: String,
        val polesText: String?,
        val x: Int,
        val y: Int,
        val attributeText: String
    )

    /**
     * Экспорт произвольного списка записей в CSV.
     * Всегда перезаписывает целевой файл.
     */
    fun exportEntries(entries: List<ExportEntry>, file: File) {
        val lines = entries.map { entry ->
            val poles = normalizePoles(entry.polesText)
            val block = (entry.blockTypePrefix.takeIf { it.isNotBlank() } ?: "AV") + "_" + poles

            // заменяем реальные переводы строк на буквальную последовательность "\n"
            val attrEscaped = entry.attributeText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n")

            listOf(block, entry.x.toString(), entry.y.toString(), attrEscaped).joinToString(";")
        }

        val csv = lines.joinToString("\r\n")
        writeFileOverwrite(file, csv)
    }

    /**
     * Удобная обёртка: экспорт по ShieldData.
     * Всегда перезаписывает целевой файл.
     */
    fun export(shieldData: ShieldData, file: File, baseX: Int = 0, stepX: Int = 300, y: Int = 0) {
        val rows = mutableListOf<String>()

        shieldData.consumers.forEachIndexed { index, c ->
            if (c.protectionDevice.isNullOrBlank()) return@forEachIndexed

            val prefix = typePrefixForProtection(c.protectionDevice)
            val polesFromModel = c.protectionPoles.takeIf { it.isNotBlank() } ?: extractPolesFromText(c.protectionDevice)
            val poles = normalizePoles(polesFromModel)
            val block = "${prefix}_${poles}"

            val attrEscaped = c.protectionDevice
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n")

            val x = baseX + index * stepX
            rows += listOf(block, x.toString(), y.toString(), attrEscaped).joinToString(";")
        }

        val csv = rows.joinToString("\r\n")
        writeFileOverwrite(file, csv)
    }

    private fun writeFileOverwrite(file: File, content: String) {
        try {
            // убедимся, что родительская папка существует
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            // Если файл существует - удаляем его (гарантированно)
            if (file.exists()) {
                // Иногда файл может быть открыт в другом приложении — тогда delete() вернёт false.
                // Мы попытаемся удалить и в случае неудачи бросим полезное исключение.
                val deleted = file.delete()
                if (!deleted) {
                    // Если не смогли удалить, попробуем перезаписать через FileOutputStream с truncate
                    // Но всё-таки лучше предупредить пользователя — файл может быть занят.
                    // Попытаемся всё равно переписать:
                    file.outputStream().use { it.write(content.toByteArray(StandardCharsets.UTF_8)) }
                    return
                }
            }

            // Создаём новый файл и записываем (writeText затрёт содержимое, если файл уже существовал)
            file.writeText(content, StandardCharsets.UTF_8)
        } catch (ex: Exception) {
            // Пробуем альтернативный метод записи (если что-то пошло не так)
            try {
                file.outputStream().use { it.write(content.toByteArray(StandardCharsets.UTF_8)) }
            } catch (inner: Exception) {
                throw RuntimeException("Не удалось записать CSV-файл: ${ex.message}; ${inner.message}", inner)
            }
        }
    }

    private fun typePrefixForProtection(protectionText: String?): String {
        return when (protectionTypeFromString(protectionText)) {
            ProtectionType.CIRCUIT_BREAKER -> "AV"
            ProtectionType.DIFF_CURRENT_BREAKER -> "AVDT"
            ProtectionType.CIRCUIT_BREAKER_AND_RCD -> "AV_UZO"
        }
    }

    private fun normalizePoles(raw: String?): String {
        if (raw.isNullOrBlank()) return "1P"
        val s = raw.trim()
        val rPlus = "(\\dP\\+N)".toRegex(RegexOption.IGNORE_CASE).find(s)?.groupValues?.getOrNull(1)
        if (!rPlus.isNullOrBlank()) return rPlus.uppercase()
        val rSimple = "(\\dP)".toRegex(RegexOption.IGNORE_CASE).find(s)?.groupValues?.getOrNull(1)
        if (!rSimple.isNullOrBlank()) return rSimple.uppercase()
        return "1P"
    }

    private fun extractPolesFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val plusMatch = "(\\dP\\+N)".toRegex(RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
        if (!plusMatch.isNullOrBlank()) return plusMatch.uppercase()
        val simpleMatch = "(\\dP)".toRegex(RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
        if (!simpleMatch.isNullOrBlank()) return simpleMatch.uppercase()
        return null
    }

    fun chooseSavePath(defaultName: String): String? {
        val frame = javax.swing.JFrame()
        frame.isAlwaysOnTop = true
        val dialog = java.awt.FileDialog(frame, "Экспорт схемы в AutoCAD (CSV)", java.awt.FileDialog.SAVE).apply {
            file = defaultName
            isVisible = true
        }
        val dir = dialog.directory
        val name = dialog.file
        frame.dispose()
        return if (name != null) java.io.File(dir, name).absolutePath else null
    }
}
