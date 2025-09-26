package data

import androidx.compose.ui.geometry.Offset

/**
 * Запечатанный (sealed) интерфейс для всех узлов на схеме.
 * Гарантирует, что узел может быть только одного из известных типов (ShieldNode или PowerSourceNode).
 */
sealed interface ProjectNode {
    val id: Int
    val name: String
    val position: Offset
}

/**
 * Модель данных для щита (прямоугольник).
 */
data class ShieldNode(
    override val id: Int,
    override var name: String,
    override var position: Offset
) : ProjectNode

/**
 * Модель данных для источника питания.
 */
data class PowerSourceNode(
    override val id: Int,
    override var name: String,
    override var position: Offset
) : ProjectNode

/**
 * Модель данных для соединения между любыми двумя узлами.
 * @param fromId ID узла, от которого идет соединение.
 * @param toId ID узла, к которому идет соединение.
 */
data class Connection(val fromId: Int, val toId: Int)

/**
 * --- НОВОЕ: Модель данных для горизонтальной линии уровня ---
 * @param id Уникальный ID.
 * @param yPosition Позиция по оси Y.
 */
data class LevelLine(val id: Int, val yPosition: Float)
