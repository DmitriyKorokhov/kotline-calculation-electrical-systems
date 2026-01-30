package ui.utils

import androidx.compose.runtime.mutableStateListOf
import ui.screens.shieldeditor.ConsumerModel
import ui.screens.shieldeditor.ShieldData

class HistoryManager(private val maxHistorySize: Int = 50) {
    private val undoStack = ArrayDeque<ShieldData>()
    private val redoStack = ArrayDeque<ShieldData>()

    /**
     * Сохраняет текущее состояние в стек Undo.
     * Вызывать ПЕРЕД любым изменением данных.
     */
    fun pushState(currentState: ShieldData) {
        redoStack.clear()
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        // Делаем глубокую копию текущего состояния
        undoStack.addLast(currentState.createSnapshot())
    }

    fun undo(currentState: ShieldData) {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeLast()

            // Текущее состояние сохраняем в Redo перед тем как перезаписать
            redoStack.addLast(currentState.createSnapshot())

            // Восстанавливаем
            currentState.restoreFrom(previousState)
        }
    }

    fun redo(currentState: ShieldData) {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeLast()

            // Текущее состояние сохраняем в Undo
            undoStack.addLast(currentState.createSnapshot())

            // Восстанавливаем
            currentState.restoreFrom(nextState)
        }
    }

    // --- Вспомогательные функции копирования ---

    private fun ShieldData.createSnapshot(): ShieldData {
        // 1. Копируем список потребителей
        val newConsumers = mutableStateListOf<ConsumerModel>()
        this.consumers.forEach {
            newConsumers.add(it.deepCopy()) // Используем ваш метод deepCopy из ConsumerModel
        }

        // 2. Создаем новый объект
        val snapshot = ShieldData(consumers = newConsumers)

        // 3. Копируем ВСЕ поля (Primitive / String properties)
        snapshot.inputInfo = this.inputInfo
        snapshot.shieldName = this.shieldName
        snapshot.maxShortCircuitCurrent = this.maxShortCircuitCurrent
        snapshot.protectionStandard = this.protectionStandard
        snapshot.protectionManufacturer = this.protectionManufacturer
        snapshot.hasOverloadProtection = this.hasOverloadProtection
        snapshot.metaExpanded = this.metaExpanded

        snapshot.phaseL1 = this.phaseL1
        snapshot.phaseL2 = this.phaseL2
        snapshot.phaseL3 = this.phaseL3

        snapshot.demandFactor = this.demandFactor
        snapshot.simultaneityFactor = this.simultaneityFactor

        snapshot.totalInstalledPower = this.totalInstalledPower
        snapshot.totalCalculatedPower = this.totalCalculatedPower
        snapshot.averageCosPhi = this.averageCosPhi
        snapshot.totalCurrent = this.totalCurrent
        snapshot.shieldDemandFactor = this.shieldDemandFactor

        snapshot.protectionCurrentThreshold = this.protectionCurrentThreshold
        snapshot.protectionFactorLow = this.protectionFactorLow
        snapshot.protectionFactorHigh = this.protectionFactorHigh

        snapshot.cableMaterial = this.cableMaterial
        snapshot.cableInsulation = this.cableInsulation
        snapshot.cableReservePercent = this.cableReservePercent
        snapshot.cableDescentPercent = this.cableDescentPercent
        snapshot.cableTerminationMeters = this.cableTerminationMeters
        snapshot.maxVoltageDropPercent = this.maxVoltageDropPercent
        snapshot.cableTemperature = this.cableTemperature
        snapshot.cableInductiveResistance = this.cableInductiveResistance
        snapshot.cableIsFlexible = this.cableIsFlexible

        snapshot.reserveTier1 = this.reserveTier1
        snapshot.reserveTier2 = this.reserveTier2
        snapshot.reserveTier3 = this.reserveTier3
        snapshot.reserveTier4 = this.reserveTier4
        snapshot.singleCoreThreshold = this.singleCoreThreshold

        return snapshot
    }

    private fun ShieldData.restoreFrom(snapshot: ShieldData) {
        // 1. Восстанавливаем поля
        this.inputInfo = snapshot.inputInfo
        this.shieldName = snapshot.shieldName
        this.maxShortCircuitCurrent = snapshot.maxShortCircuitCurrent
        this.protectionStandard = snapshot.protectionStandard
        this.protectionManufacturer = snapshot.protectionManufacturer
        this.hasOverloadProtection = snapshot.hasOverloadProtection
        this.metaExpanded = snapshot.metaExpanded

        this.phaseL1 = snapshot.phaseL1
        this.phaseL2 = snapshot.phaseL2
        this.phaseL3 = snapshot.phaseL3

        this.demandFactor = snapshot.demandFactor
        this.simultaneityFactor = snapshot.simultaneityFactor

        this.totalInstalledPower = snapshot.totalInstalledPower
        this.totalCalculatedPower = snapshot.totalCalculatedPower
        this.averageCosPhi = snapshot.averageCosPhi
        this.totalCurrent = snapshot.totalCurrent
        this.shieldDemandFactor = snapshot.shieldDemandFactor

        this.protectionCurrentThreshold = snapshot.protectionCurrentThreshold
        this.protectionFactorLow = snapshot.protectionFactorLow
        this.protectionFactorHigh = snapshot.protectionFactorHigh

        this.cableMaterial = snapshot.cableMaterial
        this.cableInsulation = snapshot.cableInsulation
        this.cableReservePercent = snapshot.cableReservePercent
        this.cableDescentPercent = snapshot.cableDescentPercent
        this.cableTerminationMeters = snapshot.cableTerminationMeters
        this.maxVoltageDropPercent = snapshot.maxVoltageDropPercent
        this.cableTemperature = snapshot.cableTemperature
        this.cableInductiveResistance = snapshot.cableInductiveResistance
        this.cableIsFlexible = snapshot.cableIsFlexible

        this.reserveTier1 = snapshot.reserveTier1
        this.reserveTier2 = snapshot.reserveTier2
        this.reserveTier3 = snapshot.reserveTier3
        this.reserveTier4 = snapshot.reserveTier4
        this.singleCoreThreshold = snapshot.singleCoreThreshold

        // 2. Восстанавливаем список потребителей
        this.consumers.clear()
        snapshot.consumers.forEach {
            this.consumers.add(it.deepCopy())
        }
    }
}

