package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Модель потребителя — каждое поле хранит своё состояние через mutableStateOf,
 * чтобы Compose корректно реагировал на изменения и поля были редактируемыми.
 */
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
    // Новое поле: текстовое представление количества полюсов, например "1P", "1P+N", "3P"
    protectionPoles: String = "",
    cableType: String = "",
    voltageDropV: String = ""
) {
    var name by mutableStateOf(name)
    var voltage by mutableStateOf(voltage)
    var cosPhi by mutableStateOf(cosPhi)
    var powerKw by mutableStateOf(powerKw)
    var modes by mutableStateOf(modes)
    var cableLine by mutableStateOf(cableLine)

    var currentA by mutableStateOf(currentA)
    var phaseNumber by mutableStateOf(phaseNumber)
    var lineName by mutableStateOf(lineName)
    var breakerNumber by mutableStateOf(breakerNumber)
    var protectionDevice by mutableStateOf(protectionDevice)
    // Здесь хранится выбранное пользователем количество полюсов (используется для экспорта CSV и т.д.)
    var protectionPoles by mutableStateOf(protectionPoles)
    var cableType by mutableStateOf(cableType)
    var voltageDropV by mutableStateOf(voltageDropV)

    fun deepCopy(): ConsumerModel {
        return ConsumerModel(
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
    }
}
