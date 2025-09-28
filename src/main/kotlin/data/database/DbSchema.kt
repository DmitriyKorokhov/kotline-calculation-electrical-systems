package data.database

import org.jetbrains.exposed.sql.Table

/**
 * Этот файл описывает структуру таблиц базы данных с помощью Exposed.
 */

// --- Таблицы для Автоматических выключателей (Breakers) ---

object BreakerModels : Table("breaker_models") {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 255)
    val series = varchar("series", 255)
    val model = varchar("model", 255)
    override val primaryKey = PrimaryKey(id)
}

object BreakerVariants : Table("breaker_variants") {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(BreakerModels.id)
    val nominalCurrent = float("nominal_current") // Номинальный ток
    val poles = integer("poles") // Полюса
    val ultimateBreakingCapacity = float("ultimate_breaking_capacity") // Наибольшая отключающая способность
    val serviceBreakingCapacity = float("service_breaking_capacity") // Рабочая отключающая способность
    val additions = varchar("additions", 512).nullable() // Дополнения
    override val primaryKey = PrimaryKey(id)
}


// --- Таблицы для УЗО (RCD) ---

object RcdModels : Table("rcd_models") {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 255)
    val series = varchar("series", 255)
    val model = varchar("model", 255)
    override val primaryKey = PrimaryKey(id)
}

object RcdVariants : Table("rcd_variants") {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(RcdModels.id)
    val nominalCurrent = float("nominal_current")
    val leakageCurrent = integer("leakage_current_ma") // Отключающий дифф. ток в мА
    val poles = integer("poles")
    override val primaryKey = PrimaryKey(id)
}


// --- Таблицы для АВДТ (RCBO) ---

object RcboModels : Table("rcbo_models") {
    val id = integer("id").autoIncrement()
    val manufacturer = varchar("manufacturer", 255)
    val series = varchar("series", 255)
    val model = varchar("model", 255)
    override val primaryKey = PrimaryKey(id)
}

object RcboVariants : Table("rcbo_variants") {
    val id = integer("id").autoIncrement()
    val modelId = integer("model_id").references(RcboModels.id)
    val nominalCurrent = float("nominal_current")
    val poles = integer("poles")
    val leakageCurrent = integer("leakage_current_ma")
    val ultimateBreakingCapacity = float("ultimate_breaking_capacity")
    val serviceBreakingCapacity = float("service_breaking_capacity")
    val additions = varchar("additions", 512).nullable()
    override val primaryKey = PrimaryKey(id)
}


// --- Таблица для Кабелей ---
// Здесь структура проще, можно обойтись одной таблицей

object Cables : Table("cables") {
    val id = integer("id").autoIncrement()
    val type = varchar("type", 255) // Тип (марка)
    val crossSection = float("cross_section_sqmm") // Сечение в мм²
    val maxCurrent = float("max_current_a") // Допустимый ток в Амперах
    override val primaryKey = PrimaryKey(id)
}
