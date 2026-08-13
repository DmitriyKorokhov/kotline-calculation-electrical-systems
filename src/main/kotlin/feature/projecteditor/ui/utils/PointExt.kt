package feature.projecteditor.ui.utils

import androidx.compose.ui.geometry.Offset
import feature.projecteditor.domain.Point

fun Point.toOffset(): Offset = Offset(this.x, this.y)
fun Offset.toPoint(): Point = Point(this.x, this.y)