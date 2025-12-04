package storage

import data.ProjectFile
import data.loadFromProjectFile
import data.toProjectFile
import kotlinx.serialization.json.Json
import ui.screens.projecteditor.ProjectCanvasState
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Объект-хранилище для всего проекта.
 * Управляет сериализацией, десериализацией и диалогами выбора файлов.
 */
object ProjectStorage {

    // Настраиваем Json для красивого вывода в файл
    private val json = Json {
        prettyPrint = true      // Человекочитаемый JSON
        encodeDefaults = true   // Включать значения по умолчанию
    }

    /**
     * Открывает диалог сохранения и записывает текущее состояние проекта в файл.
     * @param state Текущее состояние холста для сохранения.
     */
    fun saveProject(state: ProjectCanvasState) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Сохранить проект как..."
            fileFilter = FileNameExtensionFilter("Файлы проекта (*.project)", "project")
        }

        val result = fileChooser.showSaveDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.selectedFile
            // Убедимся, что у файла есть правильное расширение
            if (!selectedFile.name.endsWith(".project")) {
                selectedFile = File(selectedFile.absolutePath + ".project")
            }

            // 1. Конвертируем состояние в сериализуемую модель
            val projectFile = state.toProjectFile()

            // 2. Сериализуем в JSON строку
            val jsonString = json.encodeToString(ProjectFile.serializer(), projectFile)

            // 3. Записываем в файл
            selectedFile.writeText(jsonString)
        }
    }

    /**
     * Открывает диалог загрузки и читает состояние проекта из файла.
     * @param state Текущее состояние холста, которое будет обновлено.
     * @return true, если проект успешно загружен, иначе false.
     */
    fun loadProject(state: ProjectCanvasState): Boolean {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Открыть проект"
            fileFilter = FileNameExtensionFilter("Файлы проекта (*.project)", "project")
        }

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            if (selectedFile.exists() && selectedFile.canRead()) {
                try {
                    // 1. Читаем JSON из файла
                    val jsonString = selectedFile.readText()

                    // 2. Десериализуем в объект ProjectFile
                    val projectFile = json.decodeFromString(ProjectFile.serializer(), jsonString)

                    // 3. Загружаем данные в текущее состояние
                    state.loadFromProjectFile(projectFile)

                    return true // Успех
                } catch (e: Exception) {
                    e.printStackTrace() // Здесь можно показать диалог с ошибкой
                    return false
                }
            }
        }
        return false // Диалог отменен
    }
}
