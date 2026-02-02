package data.storage

import androidx.compose.ui.geometry.Offset
import data.*
import kotlinx.serialization.json.Json
import ui.screens.projecteditor.ProjectCanvasState
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.ShieldStorage
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Объект-хранилище для всего проекта.
 * Содержит логику I/O и мапперы для конвертации данных.
 */
object ProjectStorage {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true // Полезно для совместимости версий
    }

    fun saveProject(state: ProjectCanvasState) {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Сохранить проект как..."
            fileFilter = FileNameExtensionFilter("Файлы проекта (*.project)", "project")
        }

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var selectedFile = fileChooser.selectedFile
            if (!selectedFile.name.endsWith(".project")) {
                selectedFile = File(selectedFile.absolutePath + ".project")
            }

            // Конвертация и сохранение
            val projectFile = state.toProjectFile()
            val jsonString = json.encodeToString(ProjectFile.serializer(), projectFile)
            selectedFile.writeText(jsonString)
        }
    }

    fun loadProject(state: ProjectCanvasState): Boolean {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Открыть проект"
            fileFilter = FileNameExtensionFilter("Файлы проекта (*.project)", "project")
        }

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            if (selectedFile.exists() && selectedFile.canRead()) {
                try {
                    val jsonString = selectedFile.readText()
                    val projectFile = json.decodeFromString(ProjectFile.serializer(), jsonString)

                    // Загрузка данных в State
                    state.loadFromProjectFile(projectFile)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
            }
        }
        return false
    }
}

// ==========================================
// MAPPERS (Функции конвертации)
// ==========================================

/** Сохранение: State -> DTO */
private fun ProjectCanvasState.toProjectFile(): ProjectFile {
    val serializableNodes = nodes.map { node ->
        when (node) {
            is ShieldNode -> SerializableShieldNode(node.id, node.name, node.position.x, node.position.y)
            is PowerSourceNode -> SerializablePowerSourceNode(node.id, node.name, node.position.x, node.position.y)
            is TransformerNode -> SerializableTransformerNode(node.id, node.name, node.position.x, node.position.y, node.radiusOuter, node.radiusInner)
            is GeneratorNode -> SerializableGeneratorNode(node.id, node.name, node.position.x, node.position.y, node.radius)
        }
    }

    val serializableConnections = connections.map { SerializableConnection(it.fromId, it.toId) }
    val serializableLevels = levels.map { SerializableLevelLine(it.id, it.yPosition) }

    // Сохраняем данные щитов
    val shieldsMap = mutableMapOf<Int, SerializableShieldData>()
    nodes.filterIsInstance<ShieldNode>().forEach { node ->
        val data = ShieldStorage.loadOrCreate(node.id)
        shieldsMap[node.id] = data.toSerializable()
    }

    return ProjectFile(
        scale = this.scale,
        offsetX = this.offset.x,
        offsetY = this.offset.y,
        nodes = serializableNodes,
        connections = serializableConnections,
        levels = serializableLevels,
        shieldsData = shieldsMap
    )
}

/** Загрузка: DTO -> State */
private fun ProjectCanvasState.loadFromProjectFile(file: ProjectFile) {
    this.scale = file.scale
    this.offset = Offset(file.offsetX, file.offsetY)

    nodes.clear()
    connections.clear()
    levels.clear()

    nodes.addAll(file.nodes.map { it.toDomainNode() })
    connections.addAll(file.connections.map { Connection(it.fromId, it.toId) })
    levels.addAll(file.levels.map { LevelLine(it.id, it.yPosition) })

    // Восстанавливаем данные щитов
    ShieldStorage.clearAll()
    file.shieldsData.forEach { (id, sData) ->
        val shieldData = ShieldData()

        // Простые поля
        shieldData.shieldName = sData.shieldName
        shieldData.inputInfo = sData.inputInfo
        shieldData.maxShortCircuitCurrent = sData.maxShortCircuitCurrent
        shieldData.protectionStandard = sData.protectionStandard
        shieldData.protectionManufacturer = sData.protectionManufacturer
        shieldData.phaseL1 = sData.phaseL1
        shieldData.phaseL2 = sData.phaseL2
        shieldData.phaseL3 = sData.phaseL3
        shieldData.demandFactor = sData.demandFactor
        shieldData.simultaneityFactor = sData.simultaneityFactor
        shieldData.totalInstalledPower = sData.totalInstalledPower
        shieldData.totalCalculatedPower = sData.totalCalculatedPower
        shieldData.averageCosPhi = sData.averageCosPhi
        shieldData.totalCurrent = sData.totalCurrent
        shieldData.shieldDemandFactor = sData.shieldDemandFactor

        // Потребители
        shieldData.consumers.clear()
        sData.consumers.forEach { c ->
            shieldData.consumers.add(
                ConsumerModel(
                    name = c.name,
                    roomNumber = c.roomNumber,
                    voltage = c.voltage,
                    cosPhi = c.cosPhi,
                    powerKw = c.powerKw,
                    installedPowerW = c.installedPowerW,
                    modes = c.modes,
                    cableLine = c.cableLine,
                    layingMethod = c.layingMethod,
                    currentA = c.currentA,
                    phaseNumber = c.phaseNumber,
                    lineName = c.lineName,
                    breakerNumber = c.breakerNumber,
                    protectionDevice = c.protectionDevice,
                    protectionPoles = c.protectionPoles,
                    cableType = c.cableType,
                    voltageDropV = c.voltageDropV,
                    // Восстановление потерянных полей
                    cableLength = c.cableLength,
                    shortCircuitCurrentkA = c.shortCircuitCurrentkA
                )
            )
        }
        ShieldStorage.save(id, shieldData)
    }
}

// Вспомогательные функции конвертации

private fun SerializableNode.toDomainNode(): ProjectNode {
    val pos = Offset(x, y)
    return when (this) {
        is SerializableShieldNode -> ShieldNode(id, name, pos)
        is SerializablePowerSourceNode -> PowerSourceNode(id, name, pos)
        is SerializableTransformerNode -> TransformerNode(id, name, pos, radiusOuter, radiusInner)
        is SerializableGeneratorNode -> GeneratorNode(id, name, pos, radius)
    }
}

private fun ShieldData.toSerializable(): SerializableShieldData {
    return SerializableShieldData(
        consumers = this.consumers.map { it.toSerializable() },
        shieldName = this.shieldName,
        inputInfo = this.inputInfo,
        maxShortCircuitCurrent = this.maxShortCircuitCurrent,
        protectionStandard = this.protectionStandard,
        protectionManufacturer = this.protectionManufacturer,
        phaseL1 = this.phaseL1,
        phaseL2 = this.phaseL2,
        phaseL3 = this.phaseL3,
        demandFactor = this.demandFactor,
        simultaneityFactor = this.simultaneityFactor,
        totalInstalledPower = this.totalInstalledPower,
        totalCalculatedPower = this.totalCalculatedPower,
        averageCosPhi = this.averageCosPhi,
        totalCurrent = this.totalCurrent,
        shieldDemandFactor = this.shieldDemandFactor
    )
}

private fun ConsumerModel.toSerializable(): SerializableConsumerModel {
    return SerializableConsumerModel(
        name = name,
        roomNumber = roomNumber,
        voltage = voltage,
        cosPhi = cosPhi,
        powerKw = powerKw,
        installedPowerW = installedPowerW,
        modes = modes,
        cableLine = cableLine,
        layingMethod = layingMethod,
        currentA = currentA,
        phaseNumber = phaseNumber,
        lineName = lineName,
        breakerNumber = breakerNumber,
        protectionDevice = protectionDevice,
        protectionPoles = protectionPoles,
        cableType = cableType,
        voltageDropV = voltageDropV,
        cableLength = cableLength,
        shortCircuitCurrentkA = shortCircuitCurrentkA
    )
}