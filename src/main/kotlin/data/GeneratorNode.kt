package data

import androidx.compose.ui.geometry.Offset

data class GeneratorNode(
    override val id: Int,
    override val name: String,
    override val position: Offset,
    val radius: Float = 50f
) : ProjectNode
