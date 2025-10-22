// ShieldModels.kt
package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ConsumerModel(
    name: String = "",
    voltage: String = "",
    cosPhi: String = "",
    powerKw: String = "",
    modes: String = "",
    cableLine: String = "",
    currentA: String = "",
    phaseNumber: String = "",
    lineName: String = "",
    breakerNumber: String = "",
    protectionDevice: String = "",
    protectionPoles: String = "",
    cableType: String = "",
    voltageDropV: String = ""
) {
    var name by mutableStateOf(name)
    var voltage by mutableStateOf(voltage)
    var cosPhi by mutableStateOf(cosPhi)

    // Режим 1 — существующее поле
    var powerKw by mutableStateOf(powerKw)

    // Новый дополнительный режим (только один)
    var dualMode by mutableStateOf(false)
    var powerKwMode2 by mutableStateOf("")       // Мощность режима 2
    var currentAMode1 by mutableStateOf("")      // Ток из P1 (вычисляется при расчёте)
    var currentAMode2 by mutableStateOf("")      // Ток из P2 (вычисляется при расчёте)

    var modes by mutableStateOf(modes)
    var cableLine by mutableStateOf(cableLine)

    // Агрегированный ток: максимум из I1/I2 для выбора защиты и экспорта
    var currentA by mutableStateOf(currentA)

    var phaseNumber by mutableStateOf(phaseNumber)
    var lineName by mutableStateOf(lineName)
    var breakerNumber by mutableStateOf(breakerNumber)
    var protectionDevice by mutableStateOf(protectionDevice)
    var protectionPoles by mutableStateOf(protectionPoles)
    var cableType by mutableStateOf(cableType)
    var voltageDropV by mutableStateOf(voltageDropV)

    // Названия режимов (для dualMode)
    var modeName1 by mutableStateOf("")
    var modeName2 by mutableStateOf("")

    fun deepCopy(): ConsumerModel {
        val copy = ConsumerModel(
            name = name,
            voltage = voltage,
            cosPhi = cosPhi,
            powerKw = powerKw,
            modes = modes,
            cableLine = cableLine,
            currentA = currentA,
            phaseNumber = phaseNumber,
            lineName = lineName,
            breakerNumber = breakerNumber,
            protectionDevice = protectionDevice,
            protectionPoles = protectionPoles,
            cableType = cableType,
            voltageDropV = voltageDropV
        )
        // Новые поля
        copy.dualMode = dualMode
        copy.powerKwMode2 = powerKwMode2
        copy.currentAMode1 = currentAMode1
        copy.currentAMode2 = currentAMode2
        copy.modeName1 = modeName1
        copy.modeName2 = modeName2
        return copy
    }
}