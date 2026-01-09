package ui.screens.shieldeditor.protection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import java.awt.Cursor
import kotlin.math.max
import kotlin.math.roundToInt
import ui.screens.shieldeditor.ShieldData

@Composable
fun ProtectionSelectionWindow(
    data: ShieldData,
    initialType: ProtectionType = ProtectionType.CIRCUIT_BREAKER,
    consumerCurrentAStr: String,
    consumerVoltageStr: String?,
    maxShortCircuitCurrentStr: String,
    onSelect: (resultString: String, poles: String) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(100f) }
    var widthDp by remember { mutableStateOf(950.dp) }
    var heightDp by remember { mutableStateOf(650.dp) }
    val minWidth = 700.dp
    val minHeight = 500.dp
    val resizeHandleSize = 8.dp

    var selectedType by remember { mutableStateOf(initialType) }

    // --- State Machines ---
    var breakerStep by remember { mutableStateOf(1) }
    var breakerParams by remember { mutableStateOf<BreakerSelectionResult?>(null) }

    var rcboStep by remember { mutableStateOf(1) }
    var rcboParams by remember { mutableStateOf<RcboSelectionResult?>(null) }

    var comboStep by remember { mutableStateOf(1) }
    var comboBreakerParams by remember { mutableStateOf<BreakerSelectionResult?>(null) }
    var comboBreakerResultStr by remember { mutableStateOf<String?>(null) }
    var comboRcdParams by remember { mutableStateOf<RcdSelectionResult?>(null) }

    // Reset steps
    LaunchedEffect(selectedType) {
        breakerStep = 1
        rcboStep = 1
        comboStep = 1
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.08f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Выбор устройства защиты",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.2f))

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
                    ProtectionType.values().forEach { type ->
                        SidebarItem(
                            title = type.displayName(),
                            isSelected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                    }
                }

                // Content (Right Side)
                // ВАЖНО: Убрал Column с verticalScroll. Теперь контент сам решает, как скроллиться.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    when (selectedType) {
                        // --- 1. Автоматический выключатель ---
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
                                    onChoose = { resStr -> onSelect(resStr, p.selectedPoles) }
                                )
                            }
                        }

                        // --- 2. АВДТ ---
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
                                    onChoose = { resStr -> onSelect(resStr, p.selectedPoles) }
                                )
                            }
                        }

                        // --- 3. Автомат + УЗО ---
                        ProtectionType.CIRCUIT_BREAKER_AND_RCD -> {
                            when (comboStep) {
                                1 -> {
                                    BreakerSecondWindow(
                                        initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        initialSeries = comboBreakerParams?.series,
                                        initialSelectedAdditions = comboBreakerParams?.selectedAdditions ?: emptyList(),
                                        initialSelectedPoles = comboBreakerParams?.selectedPoles,
                                        initialSelectedCurve = comboBreakerParams?.selectedCurve,
                                        consumerVoltageStr = consumerVoltageStr,
                                        onBack = {},
                                        onDismiss = onDismiss,
                                        onConfirm = { res -> comboBreakerParams = res; comboStep = 2 }
                                    )
                                }
                                2 -> {
                                    val p = comboBreakerParams!!
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
                                        onBack = { comboStep = 1 },
                                        onDismiss = onDismiss,
                                        onChoose = { resStr ->
                                            comboBreakerResultStr = resStr
                                            comboStep = 3
                                        }
                                    )
                                }
                                3 -> {
                                    RcdSecondWindow(
                                        initialManufacturer = data.protectionManufacturer.takeIf { it.isNotBlank() },
                                        initialSeries = comboRcdParams?.series,
                                        initialSelectedPoles = comboRcdParams?.selectedPoles,
                                        initialSelectedResidualCurrent = comboRcdParams?.selectedResidualCurrent,
                                        consumerVoltageStr = consumerVoltageStr,
                                        onBack = { comboStep = 2 },
                                        onDismiss = onDismiss,
                                        onConfirm = { res -> comboRcdParams = res; comboStep = 4 }
                                    )
                                }
                                4 -> {
                                    val p = comboRcdParams!!
                                    RcdThirdWindow(
                                        consumerCurrentAStr = consumerCurrentAStr,
                                        consumerVoltageStr = consumerVoltageStr,
                                        selectedSeries = p.series,
                                        selectedPoles = p.selectedPoles,
                                        selectedResidualCurrent = p.selectedResidualCurrent,
                                        protectionThreshold = data.protectionCurrentThreshold.toFloatOrNull() ?: 40f,
                                        protectionFactorLow = data.protectionFactorLow.toFloatOrNull() ?: 0.87f,
                                        protectionFactorHigh = data.protectionFactorHigh.toFloatOrNull() ?: 0.93f,
                                        onBack = { comboStep = 3 },
                                        onDismiss = onDismiss,
                                        onChoose = { rcdResStr ->
                                            val combined = "$comboBreakerResultStr\n\n$rcdResStr"
                                            val poles = comboRcdParams?.selectedPoles ?: ""
                                            onSelect(combined, poles)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Resizers
        Box(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).size(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); widthDp = max(widthDp + with(density){drag.x.toDp()}, minWidth); heightDp = max(heightDp + with(density){drag.y.toDp()}, minHeight) } }
        )
    }
}

@Composable
private fun SidebarItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(backgroundColor).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.body2, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
    }
}
