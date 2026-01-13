package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import java.util.prefs.Preferences

class ShieldData(
    val consumers: MutableList<ConsumerModel> = mutableStateListOf()
) {
    var inputInfo by mutableStateOf("") // Информации о Вводе
    var shieldName by mutableStateOf("")
    var maxShortCircuitCurrent by mutableStateOf("")
    var protectionStandard by mutableStateOf("ГОСТ IEC 60898-1-2020")
    var protectionManufacturer by mutableStateOf("Nader")
    // Чекбокс "Наличие защиты от перегрузки"
    var hasOverloadProtection by mutableStateOf(true)
    var metaExpanded by mutableStateOf(true)

    var phaseL1 by mutableStateOf("0.00")
    var phaseL2 by mutableStateOf("0.00")
    var phaseL3 by mutableStateOf("0.00")

    // Вводимые пользователем
    var demandFactor by mutableStateOf("1.0")         // Коэф. спроса
    var simultaneityFactor by mutableStateOf("1.0") // Коэф. одновременности

    // Расчётные
    var totalInstalledPower by mutableStateOf("0.0") // Установ. мощность, Вт
    var totalCalculatedPower by mutableStateOf("0.0")// Расчетная мощность, Вт
    var averageCosPhi by mutableStateOf("0.0")       // cos(f)
    var totalCurrent by mutableStateOf("0.0")        // Ток
    var shieldDemandFactor by mutableStateOf("0.0")  // Коэф.спросащита

    // Порог расчетного тока (по умолчанию 40)
    var protectionCurrentThreshold by mutableStateOf("40")
    // Коэффициент для тока < 40А (по умолчанию 0.87)
    var protectionFactorLow by mutableStateOf("0.87")
    // Коэффициент для тока >= 40А (по умолчанию 0.93)
    var protectionFactorHigh by mutableStateOf("0.93")

    // --- Настройки кабеля ---
    // Материал проводника: "Copper" или "Aluminum"
    var cableMaterial by mutableStateOf("Copper")

    // Изоляция: "PVC" (В), "XLPE" (Пв), "Polymer" (П)
    var cableInsulation by mutableStateOf("PVC")

    // Формирование длины
    var cableReservePercent by mutableStateOf("25") // Запас %
    var cableDescentPercent by mutableStateOf("6")  // Опуск %
    var cableTerminationMeters by mutableStateOf("6") // Разделка, м

    // Падение напряжения
    var maxVoltageDropPercent by mutableStateOf("5") // Допустимое падение %

    // Температура (будет обновляться автоматически при выборе изоляции, но можно редактировать)
    var cableTemperature by mutableStateOf("70")

    var cableInductiveResistance by mutableStateOf("0.08") // мОм/м

    var cableIsFlexible by mutableStateOf(false)
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
