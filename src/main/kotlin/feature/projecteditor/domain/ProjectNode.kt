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

data class UpsNode(
    override val id: Int,
    override var name: String,
    override var position: Point,
    // Задел для расчетов
    var activePowerW: Float = 0f,
    var batteryVoltageV: Float = 48f
) : ProjectNode

data class BatteryNode(
    override val id: Int,
    override var name: String,
    override var position: Point,
    // Задел для расчетов
    var capacityAh: Float = 100f,
    var voltageV: Float = 12f
) : ProjectNode

data class SolarPanelNode(
    override val id: Int,
    override var name: String,
    override var position: Point,
    // Задел для расчетов
    var maxPowerW: Float = 400f,
    var vocV: Float = 37f // Напряжение холостого хода
) : ProjectNode

data class InverterNode(
    override val id: Int,
    override var name: String,
    override var position: Point,
    // Задел для расчетов
    var nominalPowerW: Float = 5000f,
    var isGridTie: Boolean = true
) : ProjectNode

data class SystemNode(
    override val id: Int,
    override val name: String,
    override var position: Point,
    val radius: Float = 50f, // Такой же, как у генератора
    val nominalVoltageV: Float = 400f,
    val shortCircuitPowerMVA: Float = 500f
) : ProjectNode

data class Connection(
    val fromId: Int,
    val toId: Int,
    val waypoints: List<Point> = emptyList()
)

data class Rack(
    val index: Int,
    var powerW: Float = 5f
)

data class RackFeed(
    val name: String,
    val connectedRacks: Set<Int> = emptySet(),
    val isTop: Boolean = true,
    val colorArgb: Long = if (isTop) 0xFFD32F2F else 0xFF1976D2 // Сохраняем цвет луча (ARGB)
)

data class ItRackRowNode(
    override val id: Int,
    override var name: String = "ИТ-стойки",
    override var position: Point,
    val racks: List<Rack> = listOf(Rack(1), Rack(2), Rack(3)),
    val feeds: List<RackFeed> = listOf(RackFeed("Луч А", setOf(1, 2, 3), true), RackFeed("Луч B", setOf(1, 2, 3), false))
) : ProjectNode

data class RectifierNode(
    override val id: Int,
    override var name: String,
    override var position: Point,
    var nominalPowerW: Float = 5000f
) : ProjectNode

data class LevelLine(
    val id: Int, val yPosition: Float
)
