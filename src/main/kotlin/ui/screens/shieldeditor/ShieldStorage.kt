package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf

/**
 * ShieldData — свойства, влияющие на UI, хранятся как mutableStateOf,
 * чтобы Compose реагировал на их изменения (включая поля панели).
 */
class ShieldData(
    val consumers: MutableList<ConsumerModel> = mutableStateListOf()
) {
    var shieldName by mutableStateOf("")
    var maxShortCircuitCurrent by mutableStateOf("")
    var protectionStandard by mutableStateOf("")
    var protectionManufacturer by mutableStateOf("")
    var metaExpanded by mutableStateOf(true)
}

object ShieldStorage {
    private val store = mutableMapOf<Int, ShieldData>()

    fun loadOrCreate(shieldId: Int?): ShieldData {
        if (shieldId == null) {
            val list = mutableStateListOf<ConsumerModel>()
            repeat(5) { list.add(ConsumerModel()) }
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
