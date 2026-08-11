package feature.projecteditor.domain

sealed interface ProjectNode {
    val id: Int
    val name: String
    val position: Point // Изменено с Offset
}

data class ShieldNode(
    override val id: Int,
    override var name: String,
    override var position: Point
) : ProjectNode

data class PowerSourceNode(
    override val id: Int,
    override var name: String,
    override var position: Point
) : ProjectNode

data class TransformerNode(
    override val id: Int,
    override var name: String = "T",
    override var position: Point,
    var radiusOuter: Float = 40f,
    var radiusInner: Float = 30f
) : ProjectNode

data class GeneratorNode(
    override val id: Int,
    override val name: String,
    override val position: Point,
    val radius: Float = 50f
) : ProjectNode

data class Connection(val fromId: Int, val toId: Int)
data class LevelLine(val id: Int, val yPosition: Float)
