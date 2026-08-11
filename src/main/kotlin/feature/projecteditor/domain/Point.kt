package feature.projecteditor.domain

data class Point(val x: Float, val y: Float) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(operand: Float) = Point(x * operand, y * operand)
    operator fun div(operand: Float) = Point(x / operand, y / operand)

    fun getDistanceSquared(): Float = x * x + y * y

    companion object {
        val Zero = Point(0f, 0f)
    }
}
