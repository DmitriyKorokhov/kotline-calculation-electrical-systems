package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf

/**
 * ShieldData — содержит список потребителей и поля метаданных.
 * Поля используют mutableStateOf, чтобы Compose автоматически перерисовывал UI при изменении.
 */
class ShieldData(
    val consumers: MutableList<ConsumerModel> = mutableStateListOf()
) {
    var shieldName by mutableStateOf("")
    var maxShortCircuitCurrent by mutableStateOf("") // кА — строка
    var protectionStandard by mutableStateOf("")
    var protectionManufacturer by mutableStateOf("")
    var metaExpanded by mutableStateOf(true)

    // Суммарные токи по фазам (в А, строковые представления для показа в UI)
    var phaseL1 by mutableStateOf("0.00")
    var phaseL2 by mutableStateOf("0.00")
    var phaseL3 by mutableStateOf("0.00")
}

object ShieldStorage {
    private val store = mutableMapOf<Int, ShieldData>()

    fun loadOrCreate(shieldId: Int?): ShieldData {
        if (shieldId == null) {
            val list = mutableStateListOf<ConsumerModel>()
            repeat(5) { list.add(ConsumerModel()) } // стартовые 5 потребителей
            return ShieldData(consumers = list)
        }
        return store.getOrPut(shieldId) {
            val list = mutableStateListOf<ConsumerModel>()
            repeat(5) { list.add(ConsumerModel()) }
            ShieldData(consumers = list)
        }
    }

    fun save(shieldId: Int?, data: ShieldData) {
        if (shieldId == null) return
        store[shieldId] = data
    }

    fun clearAll() {
        store.clear()
    }
}
