package data.database

import org.jetbrains.exposed.sql.Table

/**
 * Object defining the schema for the BreakerModels table.
 */
object BreakerModels : Table() {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 128)
    val series = varchar("series", 128)
    val model = varchar("model", 128)
    val breakingCapacity = varchar("breaking_capacity", 64) // Наибольшая отключающая способность
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the BreakerVariants table.
 */
object BreakerVariants : Table() {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(BreakerModels.id)
    val ratedCurrent = float("rated_current")
    val polesText = varchar("poles_text", 256) // хранит "1P, 1P+N, 2P" или одиночное "3P"
    val additions = varchar("additions", 256)
    val serviceBreakingCapacity = varchar("service_breaking_capacity", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the RcdModels table.
 */
object RcdModels : Table() {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 128)
    val series = varchar("series", 128)
    val model = varchar("model", 128)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the RcdVariants table.
 */
object RcdVariants : Table() {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(RcdModels.id)
    val ratedCurrent = float("rated_current")
    val ratedResidualCurrent = varchar("rated_residual_current", 64)
    val poles = varchar("poles", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the RcboModels table.
 */
object RcboModels : Table() {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 128)
    val series = varchar("series", 128)
    val model = varchar("model", 128)
    val breakingCapacity = varchar("breaking_capacity", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the RcboVariants table.
 */
object RcboVariants : Table() {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(RcboModels.id)
    val ratedCurrent = float("rated_current")
    val ratedResidualCurrent = varchar("rated_residual_current", 64)
    val poles = varchar("poles", 64)
    val additions = varchar("additions", 256)
    val serviceBreakingCapacity = varchar("service_breaking_capacity", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the Cables table.
 */
object Cables : Table() {
    val id = integer("id").autoIncrement()
    val type = varchar("type", 128) // Марка кабеля
    val material = varchar("material", 32) // материал жилы
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the AtsModels table (АВР).
 */
object AtsModels : Table() {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 128)
    val series = varchar("series", 128)
    val model = varchar("model", 128)
    val breakingCapacity = varchar("breaking_capacity", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the AtsVariants table (АВР).
 */
object AtsVariants : Table() {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(AtsModels.id)
    val ratedCurrent = float("rated_current")
    val poles = varchar("poles", 64)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Object defining the schema for the CableCurrentRatings table.
 * Хранит допустимые токовые нагрузки в зависимости от сечения и способа прокладки.
 */
object CableCurrentRatings : Table() {
    val id = integer("id").autoIncrement()
    val material = varchar("material", 32) // "Cu" или "Al"
    val insulation = varchar("insulation", 128) // "PVC", "HALOGEN_FREE" и т.д.
    val crossSection = float("cross_section")   // Сечение в мм²
    val currentInAir = float("current_in_air")  // Допустимый ток в воздухе (А)
    val currentInGround = float("current_in_ground") // Допустимый ток в земле (А)

    override val primaryKey = PrimaryKey(id)
}