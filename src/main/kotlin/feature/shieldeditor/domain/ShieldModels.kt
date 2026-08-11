package feature.shieldeditor.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ConsumerModel(
    name: String = "",
    roomNumber: String = "",
    voltage: String = "",
    cosPhi: String = "",
    powerKw: String = "",
    installedPowerW: String = "",
    modes: String = "",
    cableLine: String = "",
    currentA: String = "",
    phaseNumber: String = "",
    lineName: String = "",
    breakerNumber: String = "",
    protectionDevice: String = "",
    protectionPoles: String = "",
    cableType: String = "",
    layingMethod: String = "",
    voltageDropV: String = "",
    cableLength: String = "",
    shortCircuitCurrentkA: String = "",
    protectionType: String = ""
) {
    var name by mutableStateOf(name)
    var roomNumber by mutableStateOf(roomNumber)
    var voltage by mutableStateOf(voltage)
    var cosPhi by mutableStateOf(cosPhi)
    var powerKw by mutableStateOf(powerKw)
    var installedPowerW by mutableStateOf(installedPowerW)
    var modes by mutableStateOf(modes)
    var cableLine by mutableStateOf(cableLine)
    var layingMethod by mutableStateOf(layingMethod)
    var currentA by mutableStateOf(currentA)
    var phaseNumber by mutableStateOf(phaseNumber)
    var lineName by mutableStateOf(lineName)
    var breakerNumber by mutableStateOf(breakerNumber)
    var protectionDevice by mutableStateOf(protectionDevice)
    var protectionPoles by mutableStateOf(protectionPoles)
    var cableType by mutableStateOf(cableType)
    var voltageDropV by mutableStateOf(voltageDropV)
    var cableLength by mutableStateOf(cableLength)
    var shortCircuitCurrentkA by mutableStateOf(shortCircuitCurrentkA)
    val additionalProtections = mutableStateListOf<AdditionalProtection>()
    var protectionType by mutableStateOf(protectionType)

    fun deepCopy(): ConsumerModel {
        val newModel = ConsumerModel(
            name = name,
            roomNumber = roomNumber,
            voltage = voltage,
            cosPhi = cosPhi,
            powerKw = powerKw,
            installedPowerW = installedPowerW,
            modes = modes,
            cableLine = cableLine,
            currentA = currentA,
            phaseNumber = phaseNumber,
            lineName = lineName,
            breakerNumber = breakerNumber,
            protectionDevice = protectionDevice,
            protectionPoles = protectionPoles,
            cableType = cableType,
            layingMethod = layingMethod,
            voltageDropV = voltageDropV,
            cableLength = cableLength,
            shortCircuitCurrentkA = shortCircuitCurrentkA,
            protectionType = protectionType
        )

        this.additionalProtections.forEach {
            newModel.additionalProtections.add(it.deepCopy())
        }

        return newModel
    }
}

class AdditionalProtection(
    breakerNumber: String = "",
    protectionDevice: String = "",
    protectionPoles: String = "",
    protectionType: String = ""
) {
    var breakerNumber by mutableStateOf(breakerNumber)
    var protectionDevice by mutableStateOf(protectionDevice)
    var protectionPoles by mutableStateOf(protectionPoles)
    var protectionType by mutableStateOf(protectionType)

    fun deepCopy(): AdditionalProtection {
        return AdditionalProtection(
            breakerNumber = breakerNumber,
            protectionDevice = protectionDevice,
            protectionPoles = protectionPoles,
            protectionType = protectionType
        )
    }
}
