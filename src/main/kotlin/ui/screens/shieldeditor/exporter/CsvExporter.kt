package ui.screens.shieldeditor.exporter

import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.protection.ProtectionType
import ui.screens.shieldeditor.protection.protectionTypeFromString
import java.awt.FileDialog
import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.JFrame

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
        y: Int = 0
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
                y = 40,
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
                y = 40,
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

            // 4) Кабель под тем же X,Y
            val cableBrand = c.cableLine
            val cableType = c.cableType        // число жил, сечение
            val laying = c.layingMethod
            val drop = c.voltageDropV

            val hasAnyCableData = listOf(cableBrand, cableType, laying, drop).any { it.isNotBlank() }

            if (hasAnyCableData) {
                val line1 = listOf(cableType, cableBrand)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")                  // "Марка кабеля Число жил, сечение"

                val line2 = listOf(laying, drop)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")                  // "Способ прокладки Падение напряжения..."

                val cableAttr = if (line2.isNotBlank()) "$line1\n$line2" else line1

                entries += ExportEntry(
                    blockTypePrefix = "",
                    polesText = null,
                    x = x,
                    y = y,
                    attributeText = cableAttr,
                    explicitBlockName = "cable"
                )
            }

            val tableAttr = listOf(
                "NAME=${c.name}",
                "NOMROM=${c.roomNumber}",
                "PINST=${c.installedPowerW}",
                "PEST=${c.powerKw}",
                "ICALC=${c.currentA}",
                "GROUP=${c.lineName}"
            ).joinToString("|")

            val tableY = y - 104

            entries += ExportEntry(
                blockTypePrefix = "",
                polesText = null,
                x = x,              // тот же X, что у автомата и кабеля
                y = tableY,         // сразу за концом кабеля
                attributeText = tableAttr,
                explicitBlockName = "consumer_table"
                )
        }

        // 2) Линия (startLine, middleLine, endLine)
        if (protected.isNotEmpty()) {
            val lineY = y + 40
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
            x = baseX - 85,   // x = -85 при baseX = 0
            y = y + 90,       // y = 90 при y = 0
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
            ProtectionType.CIRCUIT_BREAKER_AND_RCD -> "AV_UZO"
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

    fun chooseSavePath(defaultName: String): String? {
        val frame = JFrame()
        frame.isAlwaysOnTop = true
        val dialog = FileDialog(frame, "Экспорт схемы в AutoCAD (CSV)", FileDialog.SAVE).apply {
            file = defaultName
            isVisible = true
        }
        val dir = dialog.directory
        val name = dialog.file
        frame.dispose()
        return if (name != null) File(dir, name).absolutePath else null
    }
}