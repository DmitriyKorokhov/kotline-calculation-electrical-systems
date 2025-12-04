package data

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ui.screens.projecteditor.ProjectCanvasState

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
    val levels: List<SerializableLevelLine>
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

/* ===================== */
/* Маппинг туда-обратно  */
/* ===================== */

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

    return ProjectFile(
        scale = this.scale,
        offsetX = this.offset.x,
        offsetY = this.offset.y,
        nodes = serializableNodes,
        connections = serializableConnections,
        levels = serializableLevels
    )
}

/**
 * Загрузка данных из ProjectFile в существующий ProjectCanvasState.
 * nextId позже можно будет пересчитывать как (maxId + 1) внутри ProjectCanvasState.
 */
fun ProjectCanvasState.loadFromProjectFile(file: ProjectFile) {
    // Камера
    this.scale = file.scale
    this.offset = Offset(file.offsetX, file.offsetY)

    // Узлы, соединения и уровни
    nodes.clear()
    connections.clear()
    levels.clear()

    nodes.addAll(
        file.nodes.map { it.toDomainNode() }
    )

    connections.addAll(
        file.connections.map { Connection(fromId = it.fromId, toId = it.toId) }
    )

    levels.addAll(
        file.levels.map { LevelLine(id = it.id, yPosition = it.yPosition) }
    )

    // Здесь позже добавим пересчет nextId внутри ProjectCanvasState.
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
