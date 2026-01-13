package data.database

/**
 * Data class representing a record in the BreakerModels table.
 */
data class DbBreakerModel(
    val id: Int = 0,
    val manufacturer: String,
    val series: String,
    val model: String,
    val breakingCapacity: String
)

/**
 * Data class representing a record in the BreakerVariants table.
 */
data class DbBreakerVariant(
    val id: Int = 0,
    val modelId: Int,
    val ratedCurrent: Float,
    val polesText: String,
    val additions: String,
    val serviceBreakingCapacity: String
)

/**
 * Data class representing a record in the RcdModels table.
 */
data class DbRcdModel(
    val id: Int = 0,
    val manufacturer: String,
    val series: String,
    val model: String
)

/**
 * Data class representing a record in the RcdVariants table.
 */
data class DbRcdVariant(
    val id: Int = 0,
    val modelId: Int,
    val ratedCurrent: Float,
    val ratedResidualCurrent: String,
    val poles: String
)

/**
 * Data class representing a record in the RcboModels table.
 */
data class DbRcboModel(
    val id: Int = 0,
    val manufacturer: String,
    val series: String,
    val model: String,
    val breakingCapacity: String
)

/**
 * Data class representing a record in the RcboVariants table.
 */
data class DbRcboVariant(
    val id: Int = 0,
    val modelId: Int,
    val ratedCurrent: Float,
    val ratedResidualCurrent: String,
    val poles: String,
    val additions: String,
    val serviceBreakingCapacity: String
)

/**
 * Data class representing a record in the Cables table.
 */
data class DbCable(
    val id: Int = 0,
    val type: String,
    val material: String // "Cu" (Медь) или "Al" (Алюминий)
)


/**
 * Data class representing a record in the AtsModels table (АВР).
 */
data class DbAtsModel(
    val id: Int = 0,
    val manufacturer: String,
    val series: String,
    val model: String,
    val breakingCapacity: String
)

/**
 * Data class representing a record in the AtsVariants table (АВР).
 */
data class DbAtsVariant(
    val id: Int = 0,
    val modelId: Int,
    val ratedCurrent: Float,
    val poles: String
)

/**
 * Data class representing a record in the CableCurrentRatings table.
 */
data class DbCableCurrentRating(
    val id: Int = 0,
    val material: String,      // "Cu" или "Al"
    val insulation: String,    // "PVC", "HALOGEN_FREE" и т.д.
    val crossSection: Float,   // Сечение в мм²
    val currentInAir: Float,   // Ток в воздухе (А)
    val currentInGround: Float, // Ток в земле (А)
    val structure: String
)

