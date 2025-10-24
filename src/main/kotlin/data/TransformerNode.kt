package data

import androidx.compose.ui.geometry.Offset

data class TransformerNode(
    override val id: Int,
    override var name: String = "T",
    override var position: Offset,
    // радиусы в мировых единицах (по умолчанию, можно менять)
    var radiusOuter: Float = 40f,
    var radiusInner: Float = 30f
) : ProjectNode
