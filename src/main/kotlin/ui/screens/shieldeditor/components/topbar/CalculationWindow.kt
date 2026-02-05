package ui.screens.shieldeditor.components.topbar

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import ui.screens.shieldeditor.ShieldData
import ui.screens.shieldeditor.components.topbar.tabs.CableSettingsTab
import ui.screens.shieldeditor.components.topbar.tabs.ProtectionSettingsTab
import java.awt.Cursor
import kotlin.math.roundToInt

private enum class CalculationTab {
    PROTECTION, CABLES
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

    // Начальные размеры и позиция
    var offsetX by remember { mutableStateOf(100f) }
    var offsetY by remember { mutableStateOf(100f) }
    var widthDp by remember { mutableStateOf(800.dp) } // Чуть шире по умолчанию для 2 колонок
    var heightDp by remember { mutableStateOf(500.dp) }

    // Состояние выбранной вкладки
    var selectedTab by remember { mutableStateOf(CalculationTab.PROTECTION) }

    // Минимальные размеры
    val minWidth = 600.dp
    val minHeight = 400.dp
    val resizeHandleSize = 8.dp

    // Основной контейнер окна
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(widthDp, heightDp)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface, RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- ШАПКА ОКНА (Draggable Zone) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp) // Чуть выше шапка
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
                    text = "Параметры",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            // --- ОСНОВНОЙ КОНТЕНТ (Разделен на 2 части) ---
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // ЧАСТЬ 1: Сайдбар (Меню параметров)
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.05f))
                        .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.1f)) // Граница справа
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Пункт А: Устройства защиты
                    SidebarItem(
                        title = "Устройства защиты",
                        isSelected = selectedTab == CalculationTab.PROTECTION,
                        onClick = { selectedTab = CalculationTab.PROTECTION }
                    )

                    // Пункт Б: Кабельные линии
                    SidebarItem(
                        title = "Кабельные линии",
                        isSelected = selectedTab == CalculationTab.CABLES,
                        onClick = { selectedTab = CalculationTab.CABLES }
                    )
                }

                // ЧАСТЬ 2: Область контента (Заглушки)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    when (selectedTab) {
                        CalculationTab.PROTECTION -> ProtectionSettingsTab(
                            data,
                            onSave,
                            onPushHistory = onPushHistory,
                            historyTrigger = historyTrigger
                        )
                        CalculationTab.CABLES -> CableSettingsTab(
                            data = data,
                            onSave,
                            onPushHistory = onPushHistory,
                            historyTrigger = historyTrigger
                        )
                    }
                }
            }
        }

        /// 1. Правая грань
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthDp = max(widthDp + with(density) { dragAmount.x.toDp() }, minWidth)
                    }
                }
        )

        // 2. Нижняя грань
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        heightDp = max(heightDp + with(density) { dragAmount.y.toDp() }, minHeight)
                    }
                }
        )

        // 3. Правый нижний угол (с отрисовкой полосок)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp) // Увеличенный размер для удобного захвата
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthDp = max(widthDp + with(density) { dragAmount.x.toDp() }, minWidth)
                        heightDp = max(heightDp + with(density) { dragAmount.y.toDp() }, minHeight)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val color = Color.Gray.copy(alpha = 0.5f)
                val w = size.width
                val h = size.height
                // Рисуем 3 диагональные линии
                drawLine(color, Offset(w, h - 4), Offset(w - 4, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 8), Offset(w - 8, h), strokeWidth = 2f)
                drawLine(color, Offset(w, h - 12), Offset(w - 12, h), strokeWidth = 2f)
            }
        }
    }
}

// --- Компоненты интерфейса ---
@Composable
private fun SidebarItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}
