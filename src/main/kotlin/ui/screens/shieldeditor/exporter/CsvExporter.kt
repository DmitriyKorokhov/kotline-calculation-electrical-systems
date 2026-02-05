package ui.screens.shieldeditor.exporter

import data.database.ConsumerLibrary
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.protection.ProtectionType
import ui.screens.shieldeditor.protection.protectionTypeFromString
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Экспорт в CSV для AutoCAD с поддержкой многолистовых схем.
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
     * Основная функция экспорта.
     */
    fun export(
        shieldData: ShieldData,
        file: File,
        baseX: Int = 0,
        stepX: Int = 50,
        y: Int = 0,
        format: String = "A3x3",
        xrefPathString: String? = null
    ) {
        val entries = mutableListOf<ExportEntry>()

        // 1. Параметры листа и лимиты
        // Парсим формат, например "A3x3" -> множитель 3
        val sheetMultiplier = format.lastOrNull()?.digitToIntOrNull() ?: 3
        val sheetHeight = 420 // Высота согласно ТЗ (для A3x3 высота 420)

        val a3BaseWidth = 297
        val currentSheetWidth = a3BaseWidth * sheetMultiplier // Например 891 для A3x3

        // Лимит по оси X на одном листе (согласно ТЗ)
        // Вставка листа по X = -152.
        // Нужно оставить 170 до конца.
        // Условие: X_local < Width - 152 - 170
        val xLimit = currentSheetWidth - 152 - 170

        // Фильтрация потребителей (только те, у которых есть защита)
        val protectedConsumers = shieldData.consumers.filter { it.protectionDevice.isNotBlank() }

        // --- Глобальные переменные цикла ---
        var currentSheetIndex = 0
        var globalOffsetX = baseX // Глобальный сдвиг координат (0 для первого листа)

        // Буфер для потребителей текущего листа
        val currentSheetConsumers = mutableListOf<ConsumerModel>()

        // Добавляем вводное устройство, Sidebar и Шапку (только на первом листе)
        addInputDevice(shieldData, entries, baseX, y)
        addSideCap(shieldData, entries, baseX)

        val totalConsumers = protectedConsumers.size

        // Если потребителей нет, просто рисуем один пустой лист
        if (totalConsumers == 0) {
            drawSheetFrame(entries, 0, format, currentSheetWidth, sheetHeight, xrefPathString)
        }

        var i = 0
        while (i < totalConsumers) {
            val consumer = protectedConsumers[i]

            // Рассчитываем, где встанет следующий автомат на ТЕКУЩЕМ листе
            val nextLocalX = currentSheetConsumers.size * stepX

            // Если добавление следующего автомата превысит лимит...
            if (nextLocalX > xLimit && currentSheetConsumers.isNotEmpty()) {
                // === ЗАВЕРШАЕМ ТЕКУЩИЙ ЛИСТ ===

                // 1. Рамка и штамп
                drawSheetFrame(entries, globalOffsetX, format, currentSheetWidth, sheetHeight, xrefPathString)

                // 2. Потребители и линии (с endContLine, так как это не последний лист)
                drawConsumersOnSheet(
                    entries = entries,
                    consumers = currentSheetConsumers,
                    globalOffsetX = globalOffsetX,
                    y = y,
                    isFirstSheet = (currentSheetIndex == 0),
                    isLastSheet = false // Будет продолжение
                )

                // 3. Переход к следующему листу
                currentSheetIndex++

                // Сдвигаем глобальный X: Ширина листа + 100 отступа
                globalOffsetX += (currentSheetWidth + 100)

                // Очищаем буфер
                currentSheetConsumers.clear()
            }

            // Добавляем потребителя в буфер
            currentSheetConsumers.add(consumer)
            i++
        }

        // === ЗАВЕРШАЕМ ПОСЛЕДНИЙ ЛИСТ ===
        if (currentSheetConsumers.isNotEmpty()) {
            drawSheetFrame(entries, globalOffsetX, format, currentSheetWidth, sheetHeight, xrefPathString)
            drawConsumersOnSheet(
                entries = entries,
                consumers = currentSheetConsumers,
                globalOffsetX = globalOffsetX,
                y = y,
                isFirstSheet = (currentSheetIndex == 0),
                isLastSheet = true
            )
        }

        exportEntries(entries, file)
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ---

    private fun drawSheetFrame(
        entries: MutableList<ExportEntry>,
        globalOffsetX: Int,
        format: String,
        sheetWidth: Int,
        sheetHeight: Int,
        xrefPathString: String?
    ) {
        val frameX = globalOffsetX - 152
        val frameY = 163

        // 1. Формат (Рамка)
        entries += ExportEntry(
            blockTypePrefix = "", polesText = null,
            x = frameX, y = frameY, attributeText = "",
            explicitBlockName = format
        )

        // 2. Sidebar (Боковая панель) - добавляем на каждый лист
        // Координаты по вашему ТЗ: x = 0x - 152, y = 0y + 163
        // Примечание: предполагаем, что 0y — это базовый Y (обычно 0), а 0x — globalOffsetX
        entries += ExportEntry(
            blockTypePrefix = "", polesText = null,
            x = globalOffsetX - 110,
            y = 95, // Если базовый y всегда 0, то ставим жестко 163. Если y меняется, то (y + 163)
            attributeText = "",
            explicitBlockName = "sidebar"
        )

        // 3. Штамп (XREF)
        if (!xrefPathString.isNullOrBlank()) {
            val frameOffset = 5
            val stampX = frameX + sheetWidth - frameOffset
            val stampY = frameY - sheetHeight + frameOffset

            entries += ExportEntry(
                blockTypePrefix = "", polesText = null,
                x = stampX, y = stampY, attributeText = "",
                explicitBlockName = "XREF=$xrefPathString"
            )
        }
    }

    private fun drawConsumersOnSheet(
        entries: MutableList<ExportEntry>,
        consumers: List<ConsumerModel>,
        globalOffsetX: Int,
        y: Int,
        isFirstSheet: Boolean,
        isLastSheet: Boolean
    ) {
        val stepX = 50

        // 1. Отрисовка потребителей
        consumers.forEachIndexed { idx, consumer ->
            val localX = idx * stepX
            val absX = globalOffsetX + localX

            // -- Автомат / Защита --
            if (consumer.additionalProtections.isNotEmpty()) {
                // === ЛОГИКА ДЛЯ РАЗДЕЛЕННОЙ ЗАЩИТЫ (PART) ===

                // 1. Дополнительная защита (УЗО) - вставляется в Y
                val addProt = consumer.additionalProtections[0]
                val addPolesRaw = addProt.protectionPoles.takeIf { !it.isNullOrBlank() }
                    ?: extractPolesFromText(addProt.protectionDevice)
                val addPoles = normalizePoles(addPolesRaw) // 2P, 4P...

                val addBlockName = when (addPoles) {
                    "4P" -> "UZO_4P_PART"
                    else -> "UZO_2P_PART"
                }

                // Формируем текст: Номер + Enter + Название
                val addInfoText = "${addProt.breakerNumber}\n${addProt.protectionDevice}"

                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = absX, y = y,
                    attributeText = addInfoText, // <-- Изменено
                    explicitBlockName = addBlockName
                )

                // 2. Основная защита (Автомат) - вставляется в Y + 17
                val mainPolesRaw = consumer.protectionPoles.takeIf { it.isNotBlank() }
                    ?: extractPolesFromText(consumer.protectionDevice)
                val mainPoles = normalizePoles(mainPolesRaw) // 1P, 2P, 3P, 4P

                val mainBlockName = "AV_${mainPoles}_PART"

                // Формируем текст: Номер + Enter + Название
                val mainInfoText = "${consumer.breakerNumber}\n${consumer.protectionDevice}"

                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = absX, y = y + 17,
                    attributeText = mainInfoText, // <-- Изменено
                    explicitBlockName = mainBlockName
                )

            } else {
                // === СТАНДАРТНАЯ ЛОГИКА (Один блок) ===
                val prefix = typePrefixForProtection(consumer.protectionDevice)
                val polesFromModel = consumer.protectionPoles.takeIf { it.isNotBlank() }
                    ?: extractPolesFromText(consumer.protectionDevice)

                // Формируем текст: Номер + Enter + Название
                val infoText = "${consumer.breakerNumber}\n${consumer.protectionDevice}"

                entries += ExportEntry(
                    blockTypePrefix = prefix, polesText = polesFromModel,
                    x = absX, y = y,
                    attributeText = infoText // <-- Изменено
                )
            }

            // -- Кабель --
            drawCableInfo(entries, consumer, absX, y)

            // -- Таблица --
            drawConsumerTable(entries, consumer, absX, y)

            // -- УГО --
            val symbolBlockName = ConsumerLibrary.getTagByName(consumer.name) ?: ConsumerLibrary.FALLBACK_TAG
            entries += ExportEntry(
                blockTypePrefix = "", polesText = null,
                x = absX, y = y - 100, attributeText = "",
                explicitBlockName = symbolBlockName
            )
        }

        // 2. Отрисовка линий (Шины)
        val lineY = y + 45
        val count = consumers.size
        if (count > 0) {
            // Если это НЕ первый лист, добавляем startContLine
            if (!isFirstSheet) {
                // ТЗ: startContLine рисуется по координатам: x = 0x, y = 0y + 45
                // 0x = globalOffsetX
                // 0y = y (так как ynew = 163, 0y = 163-163 = 0, а передаваемый y=0)
                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = globalOffsetX,
                    y = lineY, attributeText = "",
                    explicitBlockName = "startContLine"
                )
            } else {
                // Если первый лист - обычный startLine
                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = globalOffsetX,
                    y = lineY, attributeText = "",
                    explicitBlockName = "startLine"
                )
            }

            // middleLine (между автоматами)
            // Рисуем СТРОГО между автоматами: от 0 до предпоследнего.
            // Последний сегмент (от последнего автомата вправо) рисует либо endLine, либо endContLine.
            val loopLimit = count - 1

            for (i in 0 until loopLimit) {
                val absX = globalOffsetX + (i * stepX)
                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = absX, y = lineY, attributeText = "",
                    explicitBlockName = "middleLine"
                )
            }

            // Завершение линии на листе
            val lastX = globalOffsetX + (count - 1) * stepX

            if (isLastSheet) {
                // Если последний лист схемы - endLine
                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = lastX, y = lineY, attributeText = "",
                    explicitBlockName = "endLine"
                )
            } else {
                // Если будет продолжение - endContLine
                entries += ExportEntry(
                    blockTypePrefix = "", polesText = null,
                    x = lastX, y = lineY, attributeText = "",
                    explicitBlockName = "endContLine"
                )
            }
        }
    }

    // --- Вынесенные методы отрисовки компонентов ---

    private fun addInputDevice(data: ShieldData, entries: MutableList<ExportEntry>, baseX: Int, y: Int) {
        if (data.inputInfo.contains("Два ввода", ignoreCase = true) &&
            data.inputInfo.contains("АВР", ignoreCase = true) &&
            !data.inputInfo.contains("Блок АВР", ignoreCase = true)) {

            val lines = data.inputInfo.split("\n")
            // ИСПРАВЛЕНИЕ: Ищем "---", а не "-", так как "-" может быть частью названия (например, NDB1T-63)
            val sepIndex = lines.indexOfFirst { it.contains("---") }

            // Если разделитель найден — берем всё после него.
            // Если нет — просто отбрасываем первую строку (заголовок).
            val deviceLines = if (sepIndex >= 0 && sepIndex + 1 < lines.size) {
                lines.subList(sepIndex + 1, lines.size)
            } else {
                lines.drop(1)
            }

            val deviceText = deviceLines.filter { it.isNotBlank() }.joinToString("\\P")
            val is4P = deviceText.contains("4P", ignoreCase = true) || deviceText.contains("3P+N", ignoreCase = true)
            val blockName = if (is4P) "AVR_AV_4P" else "AVR_AV_3P"

            entries += ExportEntry("", null, baseX + 32, y + 45, deviceText, blockName)
        }

        if (data.inputInfo.contains("Блок АВР", ignoreCase = true)) {
            val lines = data.inputInfo.split("\n")
            val deviceLine = lines.lastOrNull { it.isNotBlank() && !it.contains("---") && !it.contains("Блок АВР") } ?: ""
            val is4P = deviceLine.contains("4P", ignoreCase = true) || deviceLine.contains("3P+N", ignoreCase = true)
            val blockName = if (is4P) "B_AVR_4P" else "B_AVR_3P"

            entries += ExportEntry("", null, baseX + 53, y + 45, deviceLine, blockName)
        }
    }

    private fun addSideCap(data: ShieldData, entries: MutableList<ExportEntry>, baseX: Int) {
        // Шапка щита
        val shieldCapAttr = listOf(
            "PINSTSH=${data.totalInstalledPower}",
            "PESTSH=${data.totalCalculatedPower}",
            "ICALCSH=${data.totalCurrent}",
            "PFACTSH=${data.averageCosPhi}",
            "DEMRATSH=${data.shieldDemandFactor}",
            "IL1=${data.phaseL1}",
            "IL2=${data.phaseL2}",
            "IL3=${data.phaseL3}"
        ).joinToString("|")

        entries += ExportEntry("", null, baseX, 120, shieldCapAttr, "shield_cap")
    }

    private fun drawCableInfo(entries: MutableList<ExportEntry>, c: ConsumerModel, x: Int, y: Int) {
        val cableMark = c.cableType
        val cableSection = c.cableLine
        val laying = c.layingMethod
        val length = c.cableLength
        val rawDrop = c.voltageDropV
        val cleanDrop = if (rawDrop.contains("(")) rawDrop.substringBefore("(").trim() else rawDrop.trim()

        val hasAnyCableData = listOf(cableMark, cableSection, laying, length, cleanDrop).any { it.isNotBlank() }

        if (hasAnyCableData) {
            val sectionWithUnit = if (cableSection.isNotBlank()) "$cableSection мм²" else ""
            val line1 = listOf(cableMark, sectionWithUnit).filter { it.isNotBlank() }.joinToString(" ")
            val partsLine2 = mutableListOf<String>()
            if (laying.isNotBlank()) partsLine2.add(laying)
            if (length.isNotBlank()) partsLine2.add("L=$length м")
            if (cleanDrop.isNotBlank()) partsLine2.add("ΔU=$cleanDrop")
            val line2 = partsLine2.joinToString(", ")
            val rawText = if (line2.isNotBlank()) "$line1\n$line2" else line1

            entries += ExportEntry("", null, x, y, "INFO=$rawText", "cable")
        }
    }

    private fun drawConsumerTable(entries: MutableList<ExportEntry>, c: ConsumerModel, x: Int, y: Int) {
        val wrappedName = wrapText(c.name)
        val tableAttr = listOf(
            "NAME=$wrappedName",
            "NOMROM=${c.roomNumber}",
            "PINST=${c.installedPowerW}",
            "PEST=${c.powerKw}",
            "ICALC=${c.currentA}",
            "GROUP=${c.lineName}"
        ).joinToString("|")

        entries += ExportEntry("", null, x, y - 116, tableAttr, "consumer_table")
    }

    private fun writeFileOverwrite(file: File, content: String) {
        try {
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    file.outputStream().use { it.write(content.toByteArray(StandardCharsets.UTF_8)) }
                    return
                }
            }
            file.writeText(content, StandardCharsets.UTF_8)
        } catch (ex: Exception) {
            try {
                file.outputStream().use { it.write(content.toByteArray(StandardCharsets.UTF_8)) }
            } catch (inner: Exception) {
                throw RuntimeException("Не удалось записать CSV-файл: ${ex.message}", inner)
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
            val len = word.length
            val wordWidth = (len * charWidth) + ((len - 1).coerceAtLeast(0) * letterSpacing)
            if (currentLineWidth == 0.0) {
                sb.append(word)
                currentLineWidth = wordWidth
                continue
            }
            val addedWidth = wordSpacing + wordWidth
            if (currentLineWidth + addedWidth <= maxWidth) {
                sb.append(" ").append(word)
                currentLineWidth += addedWidth
            } else {
                sb.append("\n").append(word)
                currentLineWidth = wordWidth
            }
        }
        return sb.toString()
    }
}