package data.storage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

/** Базовый сериализуемый узел. */
@Serializable
sealed class SerializableNode {
    abstract val id: Int
    abstract val name: String
    abstract val x: Float
    abstract val y: Float
}

@Serializable
@SerialName("shield")
data class SerializableShieldNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float
) : SerializableNode()

@Serializable
@SerialName("power_source")
data class SerializablePowerSourceNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float
) : SerializableNode()

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

@Serializable
@SerialName("generator")
data class SerializableGeneratorNode(
    override val id: Int,
    override val name: String,
    override val x: Float,
    override val y: Float,
    val radius: Float
) : SerializableNode()

@Serializable
data class SerializableConnection(
    val fromId: Int,
    val toId: Int
)

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
    val voltageDropV: String,
    val cableLength: String = "",
    val shortCircuitCurrentkA: String = ""
)