package ui.screens.shieldeditor.components.topbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.components.topbar.tabs.CableSettingsTab
import ui.screens.shieldeditor.components.topbar.tabs.ProtectionSettingsTab
import ui.screens.shieldeditor.components.topbar.tabs.GeneralSettingsTab
import java.awt.Cursor

private enum class CalculationTab {
    PROTECTION, CABLES, NUMBERING
}

@Composable
fun CalculationWindow(
    data: ShieldData,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onPushHistory: (Boolean) -> Unit,
    historyTrigger: Int
) {
    val density = LocalDensity.current
    val windowState = rememberWindowState(width = 800.dp, height = 500.dp)
    var selectedTab by remember { mutableStateOf(CalculationTab.PROTECTION) }

    val minWidth = 600.dp
    val minHeight = 400.dp

    val draftData = remember {
        ShieldData().apply {
            protectionStandard = data.protectionStandard
            protectionManufacturer = data.protectionManufacturer
            hasOverloadProtection = data.hasOverloadProtection
            numberingOrder = data.numberingOrder
            numberingLeftToRight = data.numberingLeftToRight
            protectionCurrentThreshold = data.protectionCurrentThreshold
            protectionFactorLow = data.protectionFactorLow
            protectionFactorHigh = data.protectionFactorHigh

            cableMaterial = data.cableMaterial
            cableInsulation = data.cableInsulation
            cableDescentPercent = data.cableDescentPercent
            cableTerminationMeters = data.cableTerminationMeters
            maxVoltageDropPercent = data.maxVoltageDropPercent
            cableTemperature = data.cableTemperature
            cableInductiveResistance = data.cableInductiveResistance
            cableIsFlexible = data.cableIsFlexible
            reserveTier1 = data.reserveTier1
            reserveTier2 = data.reserveTier2
            reserveTier3 = data.reserveTier3
            reserveTier4 = data.reserveTier4
            singleCoreThreshold = data.singleCoreThreshold

            groupNumberingOrder = data.groupNumberingOrder
            phaseDistributionMode = data.phaseDistributionMode
        }
    }

    fun applyDraft() {
        onPushHistory(true)

        data.protectionStandard = draftData.protectionStandard
        data.protectionManufacturer = draftData.protectionManufacturer
        data.hasOverloadProtection = draftData.hasOverloadProtection
        data.numberingOrder = draftData.numberingOrder
        data.numberingLeftToRight = draftData.numberingLeftToRight
        data.protectionCurrentThreshold = draftData.protectionCurrentThreshold
        data.protectionFactorLow = draftData.protectionFactorLow
        data.protectionFactorHigh = draftData.protectionFactorHigh

        data.cableMaterial = draftData.cableMaterial
        data.cableInsulation = draftData.cableInsulation
        data.cableDescentPercent = draftData.cableDescentPercent
        data.cableTerminationMeters = draftData.cableTerminationMeters
        data.maxVoltageDropPercent = draftData.maxVoltageDropPercent
        data.cableTemperature = draftData.cableTemperature
        data.cableInductiveResistance = draftData.cableInductiveResistance
        data.cableIsFlexible = draftData.cableIsFlexible
        data.reserveTier1 = draftData.reserveTier1
        data.reserveTier2 = draftData.reserveTier2
        data.reserveTier3 = draftData.reserveTier3
        data.reserveTier4 = draftData.reserveTier4
        data.singleCoreThreshold = draftData.singleCoreThreshold

        data.groupNumberingOrder = draftData.groupNumberingOrder
        data.phaseDistributionMode = draftData.phaseDistributionMode
        // Принудительно запускаем пересчет нумерации и фаз с новыми примененными настройками
        ui.screens.shieldeditor.calculation.ProtectionNumberingEngine.applyNumbering(data)
        ui.screens.shieldeditor.calculation.PhaseDistributor.distributePhases(data)

        onSave()
    }

    Window(
        onCloseRequest = onDismiss,
        state = windowState,
        undecorated = true,
        transparent = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WindowDraggableArea {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.08f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Параметры",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SidebarItem(
                            title = "Устройства защиты",
                            isSelected = selectedTab == CalculationTab.PROTECTION,
                            onClick = { selectedTab = CalculationTab.PROTECTION }
                        )
                        SidebarItem(
                            title = "Кабельные линии",
                            isSelected = selectedTab == CalculationTab.CABLES,
                            onClick = { selectedTab = CalculationTab.CABLES }
                        )
                        SidebarItem(
                            title = "Общее",
                            isSelected = selectedTab == CalculationTab.NUMBERING,
                            onClick = { selectedTab = CalculationTab.NUMBERING }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
                        when (selectedTab) {
                            CalculationTab.PROTECTION -> ProtectionSettingsTab(draftData)
                            CalculationTab.CABLES -> CableSettingsTab(draftData)
                            CalculationTab.NUMBERING -> GeneralSettingsTab(draftData)
                        }
                    }
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = { applyDraft() }) { Text("Принять") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { applyDraft(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White)
                    ) { Text("ОК") }
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newWidth = max(windowState.size.width + with(density) { dragAmount.x.toDp() }, minWidth)
                            val newHeight = max(windowState.size.height + with(density) { dragAmount.y.toDp() }, minHeight)
                            windowState.size = DpSize(newWidth, newHeight)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    val color = Color.Gray.copy(alpha = 0.5f)
                    val w = size.width
                    val h = size.height
                    drawLine(color, Offset(w, h - 4), Offset(w - 4, h), strokeWidth = 2f)
                    drawLine(color, Offset(w, h - 8), Offset(w - 8, h), strokeWidth = 2f)
                    drawLine(color, Offset(w, h - 12), Offset(w - 12, h), strokeWidth = 2f)
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.body2, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
    }
}