package ui.screens.shieldeditor.components.topbar

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
import ui.screens.shieldeditor.components.topbar.tabs.ProtectionSettingsTab
import ui.screens.shieldeditor.components.topbar.tabs.CableSettingsTab

private enum class CalculationTab {
    PROTECTION, CABLES
}

@Composable
fun CalculationWindow(
    data: ShieldData,
    onSave: () -> Unit,
    onDismiss: () -> Unit
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
                    text = "Настройки расчета",
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

                    Text(
                        text = "Параметры",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )

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
                        CalculationTab.PROTECTION -> ProtectionSettingsTab(data, onSave)
                        CalculationTab.CABLES -> CableSettingsTab()
                    }
                }
            }
        }

        // --- РЕСАЙЗЕРЫ (Те же, что и были) ---
        // 1. Правая грань
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newWidth = widthDp + with(density) { dragAmount.x.toDp() }
                        widthDp = max(newWidth, minWidth)
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
                        val newHeight = heightDp + with(density) { dragAmount.y.toDp() }
                        heightDp = max(newHeight, minHeight)
                    }
                }
        )

        // 3. Левая грань
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(resizeHandleSize)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val currentWidthPx = with(density) { widthDp.toPx() }
                        val newWidthPx = max(with(density) { minWidth.toPx() }, currentWidthPx - dragAmount.x)
                        if (newWidthPx != currentWidthPx) {
                            widthDp = with(density) { newWidthPx.toDp() }
                            offsetX += dragAmount.x
                        }
                    }
                }
        )

        // 4. Верхняя грань
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(resizeHandleSize)
                .offset(y = (-4).dp)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val currentHeightPx = with(density) { heightDp.toPx() }
                        val newHeightPx = max(with(density) { minHeight.toPx() }, currentHeightPx - dragAmount.y)
                        if (newHeightPx != currentHeightPx) {
                            heightDp = with(density) { newHeightPx.toDp() }
                            offsetY += dragAmount.y
                        }
                    }
                }
        )
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
