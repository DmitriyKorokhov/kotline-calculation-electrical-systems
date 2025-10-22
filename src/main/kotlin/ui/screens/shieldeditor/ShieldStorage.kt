package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import java.util.prefs.Preferences

class ShieldData(
    val consumers: MutableList<ConsumerModel> = mutableStateListOf()
) {
    var shieldName by mutableStateOf("")
    var maxShortCircuitCurrent by mutableStateOf("")
    var protectionStandard by mutableStateOf("")
    var protectionManufacturer by mutableStateOf("")
    var metaExpanded by mutableStateOf(true)

    var phaseL1 by mutableStateOf("0.00")
    var phaseL2 by mutableStateOf("0.00")
    var phaseL3 by mutableStateOf("0.00")
}

object ShieldStorage {
    private val store = mutableMapOf<Int, ShieldData>()

    // Preferences для сохранения путей
    private val prefs: Preferences = Preferences.userNodeForPackage(ShieldStorage::class.java)
    private const val KEY_ACCORE = "accoreconsole.path"
    private const val KEY_TEMPLATE = "template_with_blocks.path"

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

    // Settings accessors
    var accoreConsolePath: String?
        get() = prefs.get(KEY_ACCORE, null)
        set(value) {
            if (value == null) prefs.remove(KEY_ACCORE) else prefs.put(KEY_ACCORE, value)
        }

    var templateDwgPath: String?
        get() = prefs.get(KEY_TEMPLATE, null)
        set(value) {
            if (value == null) prefs.remove(KEY_TEMPLATE) else prefs.put(KEY_TEMPLATE, value)
        }
}
