package data

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ui.screens.projecteditor.ProjectCanvasState
import ui.screens.shieldeditor.ShieldStorage
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.ConsumerModel

/**
 * Корневой объект, который сохраняется в файл проекта.
 */
@Serializable
data class ProjectFile(
    val version: Int = 1,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val nodes: List<SerializableNode>,
    val connections: List<SerializableConnection>,
    val levels: List<SerializableLevelLine>,
    val shieldsData: Map<Int, SerializableShieldData> = emptyMap()
)
/**
 * Базовый сериализуемый узел.
 * Позиция хранится как x/y вместо Offset, чтобы упростить сериализацию.
 */
@Serializable
sealed class SerializableNode {
    abstract val id: Int
    abstract val name: String
    abstract val x: Float
    abstract val y: Float
}

/** Щит. */
@Serializable
@SerialName("shield")
data class SerializableShieldNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float
) : SerializableNode()

/** Источник питания. */
@Serializable
@SerialName("power_source")
data class SerializablePowerSourceNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float
) : SerializableNode()

/** Трансформатор. */
@Serializable
@SerialName("transformer")
data class SerializableTransformerNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float,
    val radiusOuter: Float,
    val radiusInner: Float
) : SerializableNode()

/** Генератор. */
@Serializable
@SerialName("generator")
data class SerializableGeneratorNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float,
    val radius: Float
) : SerializableNode()

/** Соединение между узлами. */
@Serializable
data class SerializableConnection(
    val fromId: Int,
    val toId: Int
)

/** Линия уровня. */
@Serializable
data class SerializableLevelLine(
    val id: Int,
    val yPosition: Float
)

@Serializable
data class SerializableShieldData(
    val consumers: List<SerializableConsumerModel>,
    val shieldName: String,
    val inputInfo: String,
    val maxShortCircuitCurrent: String,
    val protectionStandard: String,
    val protectionManufacturer: String,
    val phaseL1: String,
    val phaseL2: String,
    val phaseL3: String,
    val demandFactor: String,
    val simultaneityFactor: String,
    val totalInstalledPower: String,
    val totalCalculatedPower: String,
    val averageCosPhi: String,
    val totalCurrent: String,
    val shieldDemandFactor: String
)

@Serializable
data class SerializableConsumerModel(
    val name: String,
    val roomNumber: String,
    val voltage: String,
    val cosPhi: String,
    val powerKw: String,
    val installedPowerW: String,
    val modes: String,
    val cableLine: String,
    val layingMethod: String,
    val currentA: String,
    val phaseNumber: String,
    val lineName: String,
    val breakerNumber: String,
    val protectionDevice: String,
    val protectionPoles: String,
    val cableType: String,
    val voltageDropV: String
)

/**
 * Преобразование состояния холста в сериализуемый ProjectFile.
 */
fun ProjectCanvasState.toProjectFile(): ProjectFile {
    val serializableNodes = nodes.map { node ->
        when (node) {
            is ShieldNode -> SerializableShieldNode(
                id = node.id,
                name = node.name,
                x = node.position.x,
                y = node.position.y
            )
            is PowerSourceNode -> SerializablePowerSourceNode(
                id = node.id,
                name = node.name,
                x = node.position.x,
                y = node.position.y
            )
            is TransformerNode -> SerializableTransformerNode(
                id = node.id,
                name = node.name,
                x = node.position.x,
                y = node.position.y,
                radiusOuter = node.radiusOuter,
                radiusInner = node.radiusInner
            )
            is GeneratorNode -> SerializableGeneratorNode(
                id = node.id,
                name = node.name,
                x = node.position.x,
                y = node.position.y,
                radius = node.radius
            )
        }
    }

    val serializableConnections = connections.map {
        SerializableConnection(fromId = it.fromId, toId = it.toId)
    }

    val serializableLevels = levels.map {
        SerializableLevelLine(id = it.id, yPosition = it.yPosition)
    }

    val shieldsMap = mutableMapOf<Int, SerializableShieldData>()
    nodes.filterIsInstance<data.ShieldNode>().forEach { node ->
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
        shieldsData = shieldsMap // <-- Передаем собранную карту
    )
}


/**
 * Загрузка данных из ProjectFile в существующий ProjectCanvasState.
 * nextId позже можно будет пересчитывать как (maxId + 1) внутри ProjectCanvasState.
 */
fun ProjectCanvasState.loadFromProjectFile(file: ProjectFile) {
    // 1. Загружаем канвас (как было)
    this.scale = file.scale
    this.offset = Offset(file.offsetX, file.offsetY)
    nodes.clear()
    connections.clear()
    levels.clear()
    nodes.addAll(file.nodes.map { it.toDomainNode() })
    connections.addAll(file.connections.map { data.Connection(fromId = it.fromId, toId = it.toId) })
    levels.addAll(file.levels.map { data.LevelLine(id = it.id, yPosition = it.yPosition) })

    // 2. НОВОЕ: Восстанавливаем данные щитов
    // Сначала очистим старые данные в памяти
    ShieldStorage.clearAll()

    // Заполняем новыми
    file.shieldsData.forEach { (id, sData) ->
        val shieldData = ShieldData() // Создаем пустой объект

        // Заполняем поля
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

        // Заполняем потребителей
        shieldData.consumers.clear() // на всякий случай
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
                    voltageDropV = c.voltageDropV
                )
            )
        }

        // Сохраняем в ShieldStorage
        ShieldStorage.save(id, shieldData)
    }
}


/* Вспомогательная функция: SerializableNode -> ProjectNode */
private fun SerializableNode.toDomainNode(): ProjectNode {
    val pos = Offset(x, y)
    return when (this) {
        is SerializableShieldNode -> ShieldNode(
            id = id,
            name = name,
            position = pos
        )

        is SerializablePowerSourceNode -> PowerSourceNode(
            id = id,
            name = name,
            position = pos
        )

        is SerializableTransformerNode -> TransformerNode(
            id = id,
            name = name,
            position = pos,
            radiusOuter = radiusOuter,
            radiusInner = radiusInner
        )

        is SerializableGeneratorNode -> GeneratorNode(
            id = id,
            name = name,
            position = pos,
            radius = radius
        )
    }
}

// Extension для конвертации ShieldData -> SerializableShieldData
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

// Extension для конвертации ConsumerModel -> SerializableConsumerModel
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
        voltageDropV = voltageDropV
    )
}
