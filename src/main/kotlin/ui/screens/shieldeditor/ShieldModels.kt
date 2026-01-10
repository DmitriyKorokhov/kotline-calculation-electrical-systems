package ui.screens.shieldeditor

import androidx.compose.runtime.getValue
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
    cableLength: String = ""
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

    fun deepCopy(): ConsumerModel {
        return ConsumerModel(
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
            cableLength = cableLength
        )
    }
}