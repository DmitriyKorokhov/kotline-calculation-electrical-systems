package data.database

/**
 * Этот файл описывает data-классы, которые представляют объекты из базы данных.
 */

// --- Модели для Автоматических выключателей ---

data class DbBreakerModel(
    val id: Int,
    val manufacturer: String,
    val series: String,
    val model: String
)

data class DbBreakerVariant(
    val id: Int,
    val modelId: Int,
    val nominalCurrent: Float,
    val poles: Int,
    val ultimateBreakingCapacity: Float,
    val serviceBreakingCapacity: Float,
    val additions: String?
)

// --- Модели для УЗО ---

data class DbRcdModel(
    val id: Int,
    val manufacturer: String,
    val series: String,
    val model: String
)

data class DbRcdVariant(
    val id: Int,
    val modelId: Int,
    val nominalCurrent: Float,
    val leakageCurrent: Int,
    val poles: Int
)

// --- Модели для АВДТ ---

data class DbRcboModel(
    val id: Int,
    val manufacturer: String,
    val series: String,
    val model: String
)

data class DbRcboVariant(
    val id: Int,
    val modelId: Int,
    val nominalCurrent: Float,
    val poles: Int,
    val leakageCurrent: Int,
    val ultimateBreakingCapacity: Float,
    val serviceBreakingCapacity: Float,
    val additions: String?
)


// --- Модель для Кабеля ---

data class DbCable(
    val id: Int,
    val type: String,
    val crossSection: Float,
    val maxCurrent: Float
)
