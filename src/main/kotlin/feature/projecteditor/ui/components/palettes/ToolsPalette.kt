package feature.projecteditor.ui.components.palettes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolsPalette() {
    var selectedLineType by remember { mutableStateOf(0) }
    var selectedLineWeight by remember { mutableStateOf(1) }
    var selectedColor by remember { mutableStateOf(0xFF000000) }

    var lineTypeExpanded by remember { mutableStateOf(false) }
    var lineWeightExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Полилиния
        ToolButton(
            icon = Icons.Default.Timeline,
            label = "Полилиния",
            onClick = { /* TODO */ },
            width = 80.dp
        )

        // 2. Круг
        ToolButton(
            icon = Icons.Default.RadioButtonUnchecked,
            label = "Круг",
            onClick = { /* TODO */ },
            width = 80.dp
        )

        // 3. Прямоугольник
        ToolButton(
            icon = Icons.Default.CheckBoxOutlineBlank,
            label = "Прямоугольник",
            onClick = { /* TODO */ },
            width = 96.dp
        )

        // 4. Маленькие кнопки
        Column(
            modifier = Modifier.height(92.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallToolButton(icon = Icons.Default.FormatPaint, tooltip = "Свойства", onClick = { /* TODO */ })
            SmallToolButton(icon = Icons.Default.ContentCopy, tooltip = "Копировать", onClick = { /* TODO */ })
            SmallToolButton(icon = Icons.Outlined.Refresh, tooltip = "Поворот", onClick = { /* TODO */ })
        }

        // --- БЛОК СВОЙСТВ ЛИНИЙ ---

        // 5. Стиль линии (Тип и Вес)
        Column(
            modifier = Modifier.height(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Стиль линии", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)

            // 💡 ОТСТУП 1: Между текстом "Стиль линии" и первым списком (Тип линии)
            Spacer(modifier = Modifier.height(4.dp))

            // Первый список: Тип линии
            LinePropertyDropdown(
                expanded = lineTypeExpanded,
                onExpandChange = { lineTypeExpanded = it },
                width = 100.dp,  // 💡 РАЗМЕР: Ширина первого списка
                height = 26.dp,  // 💡 РАЗМЕР: Высота первого списка
                currentDraw = { drawLineType(selectedLineType) }
            ) {
                DropdownMenuItem(onClick = { selectedLineType = 0; lineTypeExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineType(0) }
                }
                DropdownMenuItem(onClick = { selectedLineType = 1; lineTypeExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineType(1) }
                }
                DropdownMenuItem(onClick = { selectedLineType = 2; lineTypeExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineType(2) }
                }
            }

            // 💡 ОТСТУП 2: Между первым (Тип) и вторым (Вес) списками
            Spacer(modifier = Modifier.height(6.dp))

            // Второй список: Вес линии
            LinePropertyDropdown(
                expanded = lineWeightExpanded,
                onExpandChange = { lineWeightExpanded = it },
                width = 100.dp,  // 💡 РАЗМЕР: Ширина второго списка (сейчас равна первому)
                height = 26.dp,  // 💡 РАЗМЕР: Высота второго списка (сейчас равна первому)
                currentDraw = { drawLineWeight(selectedLineWeight) }
            ) {
                DropdownMenuItem(onClick = { selectedLineWeight = 0; lineWeightExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineWeight(0) }
                }
                DropdownMenuItem(onClick = { selectedLineWeight = 1; lineWeightExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineWeight(1) }
                }
                DropdownMenuItem(onClick = { selectedLineWeight = 2; lineWeightExpanded = false }) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) { drawLineWeight(2) }
                }
            }
        }

        // 6. Цвета линий
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.height(92.dp)
        ) {
            Text("Цвет", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val colors1 = listOf(0xFFFFFFFF, 0xFF000000, 0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFF757575, 0xFFF57C00)
                    colors1.forEach { colorVal ->
                        ToolsColorButton(colorVal, selectedColor) { selectedColor = colorVal }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val colors2 = listOf(0xFF7B1FA2, 0xFFFBC02D, 0xFF00BCD4, 0xFF8D6E63, 0xFFE91E63, 0xFFCDDC39, 0xFF607D8B)
                    colors2.forEach { colorVal ->
                        ToolsColorButton(colorVal, selectedColor) { selectedColor = colorVal }
                    }
                }
            }
        }

        // 7. Уровень
        RectangularToolButtonWithCustomIcon(
            label = "Уровень",
            onClick = { /* TODO */ },
            width = 88.dp,
            iconDraw = {
                val lineColor = Color.Gray
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            }
        )
    }
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit, width: Dp = 80.dp) {
    Column(
        modifier = Modifier
            .size(width = width, height = 92.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(42.dp),
                tint = Color.Black
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false
        )
    }
}

@Composable
private fun RectangularToolButtonWithCustomIcon(
    label: String,
    onClick: () -> Unit,
    width: Dp = 88.dp,
    iconDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = width, height = 92.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f).width(width - 20.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize(), onDraw = iconDraw)
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SmallToolButton(icon: ImageVector, tooltip: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            modifier = Modifier.size(18.dp),
            tint = Color.Black
        )
    }
}

@Composable
private fun LinePropertyDropdown(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    width: Dp = 100.dp,
    height: Dp = 26.dp,
    currentDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
    menuItems: @Composable () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .width(width)
                .height(height)
                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable { onExpandChange(true) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(modifier = Modifier.weight(1f).height(16.dp), onDraw = currentDraw)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.width(width)
        ) {
            menuItems()
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineType(type: Int) {
    val color = Color.Black
    val y = size.height / 2
    when (type) {
        0 -> drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        1 -> drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
        2 -> drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 8f, 3f, 8f), 0f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineWeight(weight: Int) {
    val color = Color.Black
    val y = size.height / 2
    val stroke = when (weight) {
        0 -> 1f
        1 -> 3f
        2 -> 6f
        else -> 3f
    }
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
}

@Composable
private fun ToolsColorButton(colorArgb: Long, currentColor: Long, onClick: () -> Unit) {
    val isActive = colorArgb == currentColor
    val color = Color(colorArgb)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}