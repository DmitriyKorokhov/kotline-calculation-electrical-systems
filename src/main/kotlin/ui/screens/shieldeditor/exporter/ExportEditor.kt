package ui.screens.shieldeditor.exporter

import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.ShieldStorage
import java.io.File
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

object ExportEditor {

    fun startDwgExport(data: ShieldData, format: String, stampPath: String?) {
        // 1) Выбор места сохранения DWG
        val frame = JFrame().apply { isAlwaysOnTop = true }
        val dlg = java.awt.FileDialog(frame, "Сохранить DWG как...", java.awt.FileDialog.SAVE).apply {
            file = "${data.shieldName.ifBlank { "scheme" }}.dwg"
            isVisible = true
        }
        val outPath = dlg.file?.let { File(dlg.directory, it).absolutePath }
        frame.dispose()

        if (outPath == null) return

        // 2) Найти accoreconsole
        var accorePath = ShieldStorage.accoreConsolePath
        if (accorePath.isNullOrBlank()) {
            accorePath = AutoCadExporter.tryFindAccoreConsole()
            if (accorePath != null) ShieldStorage.accoreConsolePath = accorePath
        }

        if (accorePath.isNullOrBlank()) {
            val fc = javax.swing.JFileChooser().apply {
                dialogTitle = "Укажите путь к accoreconsole.exe"
                fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
            }
            val res = fc.showOpenDialog(null)
            if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
                accorePath = fc.selectedFile.absolutePath
                ShieldStorage.accoreConsolePath = accorePath
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "accoreconsole.exe не выбран",
                    "Отмена",
                    JOptionPane.INFORMATION_MESSAGE
                )
                return
            }
        }

        // 3) Найти шаблон DWG
        var templatePath = ShieldStorage.templateDwgPath
        if (templatePath.isNullOrBlank()) {
            val guess = File(System.getProperty("user.dir"), "source_blocks.dwg")
            if (guess.exists()) {
                templatePath = guess.absolutePath
                ShieldStorage.templateDwgPath = templatePath
            } else {
                val fc2 = javax.swing.JFileChooser().apply {
                    dialogTitle = "Выберите source_blocks"
                    fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY
                }
                val res2 = fc2.showOpenDialog(null)
                if (res2 == javax.swing.JFileChooser.APPROVE_OPTION) {
                    templatePath = fc2.selectedFile.absolutePath
                    ShieldStorage.templateDwgPath = templatePath
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        "Шаблон DWG не выбран",
                        "Отмена",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    return
                }
            }
        }

        // 4) Запуск экспорта (асинхронно)
        thread {
            val result = AutoCadExporter.exportUsingTrustedStaging(
                accorePath = accorePath,
                templateDwgPath = templatePath!!,
                outDwgPath = outPath,
                shieldData = data,
                baseX = 0,
                stepX = 50,
                y = 0,
                timeoutSec = 300L,
                useTemplateCopy = false,
                format = format,
                stampPath = stampPath
            )

            SwingUtilities.invokeLater {
                val msg = javax.swing.JTextArea(result.output)
                    .apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
                val scroll = javax.swing.JScrollPane(msg).apply { preferredSize = java.awt.Dimension(900, 420) }
                val title = if (result.exitCode == 0) "Экспорт успешен" else "Экспорт завершился с ошибкой"
                val type = if (result.exitCode == 0) JOptionPane.INFORMATION_MESSAGE else JOptionPane.ERROR_MESSAGE
                JOptionPane.showMessageDialog(null, scroll, title, type)
            }
        }
    }
}
