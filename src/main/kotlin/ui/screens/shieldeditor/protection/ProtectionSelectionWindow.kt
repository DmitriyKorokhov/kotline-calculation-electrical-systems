package ui.screens.shieldeditor.protection

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
import java.awt.Cursor

@Composable
fun ProtectionSelectionWindow(
    data: ShieldData,
    initialType: ProtectionType = ProtectionType.CIRCUIT_BREAKER,
    consumerCurrentAStr: String,
    consumerVoltageStr: String?,
    maxShortCircuitCurrentStr: String,
    onSelect: (resultString: String, poles: String, type: ProtectionType) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val windowState = rememberWindowState(width = 800.dp, height = 600.dp) // Немного увеличил размер по умолчанию

    val minWidth = 600.dp
    val minHeight = 450.dp

    // --- Состояния (перенесены из вашего старого кода) ---
    var selectedType by remember { mutableStateOf(initialType) }

    var breakerStep by remember { mutableStateOf(1) }
    var breakerParams by remember { mutableStateOf<BreakerSelectionResult?>(null) }

    var rcboStep by remember { mutableStateOf(1) }
    var rcboParams by remember { mutableStateOf<RcboSelectionResult?>(null) }

    var rcdStep by remember { mutableStateOf(1) }
    var rcdParams by remember { mutableStateOf<RcdSelectionResult?>(null) }
    // -----------------------------------------------------

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
                // 1. Область для перетаскивания окна
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
                            text = "Выбор аппарата защиты",
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

                // 2. Основной контент (сайдбар + рабочая область)
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Sidebar
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.05f))
                            .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Тип защиты",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                        )
                        ProtectionType.entries.forEach { type ->
                            SidebarItem(
                                title = type.displayName(),
                                isSelected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                        }
                    }

                    // Content (Right Side)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                    ) {
                        when (selectedType) {
                            ProtectionType.CIRCUIT_BREAKER -> {
                                if (breakerStep == 1) {
                                    BreakerSecondWindow(
                                        initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        initialSeries = breakerParams?.series,
                                        initialSelectedAdditions = breakerParams?.selectedAdditions ?: emptyList(),
                                        initialSelectedPoles = breakerParams?.selectedPoles,
                                        initialSelectedCurve = breakerParams?.selectedCurve,
                                        consumerVoltageStr = consumerVoltageStr,
                                        onBack = {},
                                        onDismiss = onDismiss,
                                        onConfirm = { res -> breakerParams = res; breakerStep = 2 }
                                    )
                                } else {
                                    val p = breakerParams!!
                                    BreakerThirdWindow(
                                        maxShortCircuitCurrentStr = maxShortCircuitCurrentStr,
                                        standard = data.protectionStandard,
                                        consumerCurrentAStr = consumerCurrentAStr,
                                        consumerVoltageStr = consumerVoltageStr,
                                        selectedSeries = p.series,
                                        selectedPoles = p.selectedPoles,
                                        selectedAdditions = p.selectedAdditions,
                                        selectedCurve = p.selectedCurve,
                                        protectionThreshold = data.protectionCurrentThreshold.toFloatOrNull() ?: 40f,
                                        protectionFactorLow = data.protectionFactorLow.toFloatOrNull() ?: 0.87f,
                                        protectionFactorHigh = data.protectionFactorHigh.toFloatOrNull() ?: 0.93f,
                                        onBack = { breakerStep = 1 },
                                        onDismiss = onDismiss,
                                        onChoose = { resStr -> onSelect(resStr, p.selectedPoles, selectedType) }
                                    )
                                }
                            }

                            ProtectionType.DIFF_CURRENT_BREAKER -> {
                                if (rcboStep == 1) {
                                    RcboSecondWindow(
                                        initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        initialSeries = rcboParams?.series,
                                        initialSelectedAdditions = rcboParams?.selectedAdditions ?: emptyList(),
                                        initialSelectedPoles = rcboParams?.selectedPoles,
                                        initialSelectedCurve = rcboParams?.selectedCurve,
                                        initialSelectedResidualCurrent = rcboParams?.selectedResidualCurrent,
                                        consumerVoltageStr = consumerVoltageStr,
                                        onBack = {},
                                        onDismiss = onDismiss,
                                        onConfirm = { res -> rcboParams = res; rcboStep = 2 }
                                    )
                                } else {
                                    val p = rcboParams!!
                                    RcboThirdWindow(
                                        maxShortCircuitCurrentStr = maxShortCircuitCurrentStr,
                                        standard = data.protectionStandard,
                                        consumerCurrentAStr = consumerCurrentAStr,
                                        consumerVoltageStr = consumerVoltageStr,
                                        selectedSeries = p.series,
                                        selectedPoles = p.selectedPoles,
                                        selectedAdditions = p.selectedAdditions,
                                        selectedCurve = p.selectedCurve,
                                        selectedResidualCurrent = p.selectedResidualCurrent,
                                        protectionThreshold = data.protectionCurrentThreshold.toFloatOrNull() ?: 40f,
                                        protectionFactorLow = data.protectionFactorLow.toFloatOrNull() ?: 0.87f,
                                        protectionFactorHigh = data.protectionFactorHigh.toFloatOrNull() ?: 0.93f,
                                        onBack = { rcboStep = 1 },
                                        onDismiss = onDismiss,
                                        onChoose = { resStr -> onSelect(resStr, p.selectedPoles, selectedType) }
                                    )
                                }
                            }

                            ProtectionType.RCD -> {
                                if (rcdStep == 1) {
                                    RcdSecondWindow(
                                        initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        initialSeries = rcdParams?.series,
                                        initialSelectedPoles = rcdParams?.selectedPoles,
                                        initialSelectedResidualCurrent = rcdParams?.selectedResidualCurrent,
                                        consumerVoltageStr = consumerVoltageStr,
                                        onBack = {},
                                        onDismiss = onDismiss,
                                        onConfirm = { res -> rcdParams = res; rcdStep = 2 }
                                    )
                                } else {
                                    val p = rcdParams!!
                                    RcdThirdWindow(
                                        consumerCurrentAStr = consumerCurrentAStr,
                                        consumerVoltageStr = consumerVoltageStr,
                                        selectedSeries = p.series,
                                        selectedPoles = p.selectedPoles,
                                        selectedResidualCurrent = p.selectedResidualCurrent,
                                        protectionThreshold = data.protectionCurrentThreshold.toFloatOrNull() ?: 40f,
                                        protectionFactorLow = data.protectionFactorLow.toFloatOrNull() ?: 0.87f,
                                        protectionFactorHigh = data.protectionFactorHigh.toFloatOrNull() ?: 0.93f,
                                        onBack = { rcdStep = 1 },
                                        onDismiss = onDismiss,
                                        onChoose = { resStr -> onSelect(resStr, p.selectedPoles, selectedType) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Элемент для изменения размера окна (в правом нижнем углу)
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