package ui.screens.shieldeditor.exporter

import data.database.ConsumerLibrary
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.protection.ProtectionType
import ui.screens.shieldeditor.protection.protectionTypeFromString
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Экспорт в CSV для AutoCAD.
 */
class CsvExporter {

    data class ExportEntry(
        val blockTypePrefix: String,
        val polesText: String?,
        val x: Int,
        val y: Int,
        val attributeText: String,
        val explicitBlockName: String? = null
    )

    /**
     * Экспорт произвольного списка записей в CSV.
     * Всегда перезаписывает целевой файл.
     */
    fun exportEntries(entries: List<ExportEntry>, file: File) {
        val lines = entries.map { entry ->
            val blockName = entry.explicitBlockName ?: run {
                val poles = normalizePoles(entry.polesText)
                (entry.blockTypePrefix.takeIf { it.isNotBlank() } ?: "AV") + "_" + poles
            }
            val attrEscaped = entry.attributeText
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\P")

            listOf(blockName, entry.x.toString(), entry.y.toString(), attrEscaped)
                .joinToString(";")
        }
        val csv = lines.joinToString("\r\n")
        writeFileOverwrite(file, csv)
    }

    /**
     * Удобная обёртка: экспорт по ShieldData.
     * Всегда перезаписывает целевой файл.
     */
    fun export(
        shieldData: ShieldData,
        file: File,
        baseX: Int = 0,
        stepX: Int = 50,
        y: Int = 0,
        format: String = ""
    ) {
        val entries = mutableListOf<ExportEntry>()

        if (shieldData.inputInfo.contains("Два ввода", ignoreCase = true) &&
            shieldData.inputInfo.contains("АВР", ignoreCase = true) &&
            !shieldData.inputInfo.contains("Блок АВР", ignoreCase = true)) {

            val lines = shieldData.inputInfo.split("\n")
            val sepIndex = lines.indexOfFirst { it.contains("-") }
            val deviceLines = if (sepIndex >= 0 && sepIndex + 1 < lines.size) {
                lines.subList(sepIndex + 1, lines.size)
            } else {
                lines.drop(1)
            }
            val deviceText = deviceLines
                .filter { it.isNotBlank() }
                .joinToString("\\P")
            val is4P = deviceText.contains("4P", ignoreCase = true) || deviceText.contains("3P+N", ignoreCase = true)
            val blockName = if (is4P) "AVR_AV_4P" else "AVR_AV_3P"

            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = 32,
                y = 45,
                attributeText = deviceText,
                explicitBlockName = blockName
            )
        }


        if (shieldData.inputInfo.contains("Блок АВР", ignoreCase = true)) {
            val lines = shieldData.inputInfo.split("\n")
            val deviceLine = lines.lastOrNull {
                it.isNotBlank() && !it.contains("---") && !it.contains("Блок АВР")
            } ?: ""
            val is4P = deviceLine.contains("4P", ignoreCase = true) || deviceLine.contains("3P+N", ignoreCase = true)
            val blockName = if (is4P) "B_AVR_4P" else "B_AVR_3P"

            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = 53,
                y = 45,
                attributeText = deviceLine,
                explicitBlockName = blockName
            )
        }

        // 1) Устройства защиты и коммутации (как раньше)
        val protected = shieldData.consumers.withIndex()
            .filter { it.value.protectionDevice.isNotBlank() }

        protected.forEachIndexed { idx, (_, c) ->
            val prefix = typePrefixForProtection(c.protectionDevice)
            val polesFromModel =
                c.protectionPoles.takeIf { it.isNotBlank() } ?: extractPolesFromText(c.protectionDevice)
            val x = baseX + idx * stepX

            // сам автомат
            entries += ExportEntry(
                blockTypePrefix = prefix,
                polesText = polesFromModel,
                x = x,
                y = y,
                attributeText = c.protectionDevice
            )

            // 2. Кабель
            val cableMark = c.cableType
            val cableSection = c.cableLine
            val laying = c.layingMethod
            val length = c.cableLength

            // Очищаем падение напряжения от процентов (берем всё до открывающей скобки)
            val rawDrop = c.voltageDropV
            val cleanDrop = if (rawDrop.contains("(")) {
                rawDrop.substringBefore("(").trim()
            } else {
                rawDrop.trim()
            }

            // Проверяем, есть ли хоть какие-то данные
            val hasAnyCableData = listOf(cableMark, cableSection, laying, length, cleanDrop).any { it.isNotBlank() }

            if (hasAnyCableData) {
                // Добавляем "мм²" к сечению, если оно не пустое
                val sectionWithUnit = if (cableSection.isNotBlank()) "$cableSection мм²" else ""

                // Первая строка: "Марка кабеля Сечение мм²"
                val line1 = listOf(cableMark, sectionWithUnit)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")

                // Вторая строка: "способ прокладки, L=Длина м, ΔU = падение В"
                val partsLine2 = mutableListOf<String>()

                if (laying.isNotBlank()) {
                    partsLine2.add(laying)
                }
                if (length.isNotBlank()) {
                    partsLine2.add("L=$length м")
                }
                if (cleanDrop.isNotBlank()) {
                    partsLine2.add("ΔU=$cleanDrop")
                }

                val line2 = partsLine2.joinToString(", ")

                val rawText = if (line2.isNotBlank()) "$line1\n$line2" else line1

                val cableAttr = "INFO=$rawText"

                entries += ExportEntry(
                    blockTypePrefix = "",
                    polesText = null,
                    x = x,
                    y = y,
                    attributeText = cableAttr,
                    explicitBlockName = "cable"
                )
            }

            val wrappedName = wrapText(c.name)

            val tableAttr = listOf(
                "NAME=$wrappedName",
                "NOMROM=${c.roomNumber}",
                "PINST=${c.installedPowerW}",
                "PEST=${c.powerKw}",
                "ICALC=${c.currentA}",
                "GROUP=${c.lineName}"
            ).joinToString("|")

            val tableY = y - 116

            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = x,              // тот же X, что у автомата и кабеля
                y = tableY,         // сразу за концом кабеля
                attributeText = tableAttr,
                explicitBlockName = "consumer_table"
                )

            // === Добавление условного обозначения потребителя ===
            val symbolBlockName = ConsumerLibrary.getTagByName(c.name)
                ?: ConsumerLibrary.FALLBACK_TAG
            val symbolY = y - 100

            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = x,
                y = symbolY,
                attributeText = "",
                explicitBlockName = symbolBlockName
            )
        }

        // 2) Линия (startLine, middleLine, endLine)
        if (protected.isNotEmpty()) {
            val lineY = y + 45
            val firstX = baseX

            // startLine один раз
            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = firstX,
                y = lineY,
                attributeText = "",
                explicitBlockName = "startLine"
            )

            // middleLine — только до предпоследнего автомата
            protected.dropLast(1).forEachIndexed { idx, _ ->
                val xLine = baseX + idx * stepX
                entries += ExportEntry(
                    blockTypePrefix = "",
                    polesText = null,
                    x = xLine,
                    y = lineY,
                    attributeText = "",
                    explicitBlockName = "middleLine"
                )
            }

            // endLine — на координате последнего автомата
            val endX = baseX + (protected.size - 1) * stepX
            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = endX,
                y = lineY,
                attributeText = "",
                explicitBlockName = "endLine"
            )

        }

        // 3) Боковая панель sidebar
        entries += ExportEntry(
            blockTypePrefix = "",
            polesText = null,
            x = baseX - 110,   // x = -110 при baseX = 0
            y = y + 95,       // y = 90 при y = 0
            attributeText = "",
            explicitBlockName = "sidebar"
        )

        // 4) Шапка щита shield_cap с данными из ShieldData
        val shieldCapAttr = listOf(
            "PINSTSH=${shieldData.totalInstalledPower}",     // Установ. мощность, Вт
            "PESTSH=${shieldData.totalCalculatedPower}",     // Расчетная мощность, Вт
            "ICALCSH=${shieldData.totalCurrent}",            // Ток
            "PFACTSH=${shieldData.averageCosPhi}",           // cos(f)
            "DEMRATSH=${shieldData.shieldDemandFactor}",     // Коэф. спроса щита
            "IL1=${shieldData.phaseL1}",                     // Нагрузка на фазу L1
            "IL2=${shieldData.phaseL2}",                     // Нагрузка на фазу L2
            "IL3=${shieldData.phaseL3}"                      // Нагрузка на фазу L3
        ).joinToString("|")

        entries += ExportEntry(
            blockTypePrefix = "",
            polesText = null,
            x = 0,
            y = 120,
            attributeText = shieldCapAttr,
            explicitBlockName = "shield_cap"
        )

        // 5) Вставка формата листа
        // Координаты вставки: x=152, y=163
        entries += ExportEntry(
            blockTypePrefix = "",
            polesText = null,
            x = -152,
            y = 163,
            attributeText = "",
            explicitBlockName = format
        )

        exportEntries(entries, file)
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
            ProtectionType.RCD -> "AV_UZO"
        }
    }

    private fun normalizePoles(raw: String?): String {
        if (raw.isNullOrBlank()) return "1P"
        val s = raw.trim().uppercase()
        if (s.contains("1P+N")) return "2P"
        if (s.contains("3P+N")) return "4P"
        if (s.contains("1P")) return "1P"
        if (s.contains("2P")) return "2P"
        if (s.contains("3P")) return "3P"
        if (s.contains("4P")) return "4P"
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

    private fun wrapText(text: String): String {
        val maxWidth = 50.0
        val charWidth = 1.3
        val letterSpacing = 1.6
        val wordSpacing = 4.5

        val words = text.split(" ")
        val sb = StringBuilder()
        var currentLineWidth = 0.0

        for (word in words) {
            if (word.isEmpty()) continue

            // 1. Считаем ширину слова:
            // N букв * 1.3 + (N-1) промежутков * 1.6
            val len = word.length
            val wordWidth = (len * charWidth) + ((len - 1).coerceAtLeast(0) * letterSpacing)

            // Если это первое слово в строке, просто добавляем его
            if (currentLineWidth == 0.0) {
                sb.append(word)
                currentLineWidth = wordWidth
                continue
            }

            // 2. Считаем ширину добавления (пробел между словами + само слово)
            val addedWidth = wordSpacing + wordWidth

            if (currentLineWidth + addedWidth <= maxWidth) {
                // Помещается в текущую строку
                sb.append(" ").append(word)
                currentLineWidth += addedWidth
            } else {
                // Не помещается -> перенос на новую строку
                sb.append("\n").append(word)
                currentLineWidth = wordWidth
            }
        }
        return sb.toString()
    }
}