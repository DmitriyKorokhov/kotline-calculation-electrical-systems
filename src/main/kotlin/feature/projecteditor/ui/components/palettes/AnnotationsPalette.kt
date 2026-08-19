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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import feature.projecteditor.domain.CalloutNode
import feature.projecteditor.domain.TextNode
import feature.projecteditor.state.CanvasToolMode
import feature.projecteditor.state.ProjectCanvasState

@Composable
fun AnnotationsPalette(state: ProjectCanvasState) {
    val selectedNodes = state.nodes.filter { it.id in state.selectedNodeIds }
    val selectedTextNode = selectedNodes.firstOrNull { it is TextNode || it is CalloutNode }
    val isTextSelect = selectedTextNode is TextNode

    val currentFontSize = (selectedTextNode as? TextNode)?.fontSize ?: (selectedTextNode as? CalloutNode)?.fontSize ?: 14f
    val isBold = (selectedTextNode as? TextNode)?.isBold ?: (selectedTextNode as? CalloutNode)?.isBold ?: false
    val isItalic = (selectedTextNode as? TextNode)?.isItalic ?: (selectedTextNode as? CalloutNode)?.isItalic ?: false
    val isUnderline = (selectedTextNode as? TextNode)?.isUnderline ?: (selectedTextNode as? CalloutNode)?.isUnderline ?: false
    val isStrikethrough = (selectedTextNode as? TextNode)?.isStrikethrough ?: (selectedTextNode as? CalloutNode)?.isStrikethrough ?: false
    val align = (selectedTextNode as? TextNode)?.align ?: 0
    val currentColor = (selectedTextNode as? TextNode)?.colorArgb ?: (selectedTextNode as? CalloutNode)?.colorArgb ?: 0xFFFFFFFF
    val hasBg = (selectedTextNode as? TextNode)?.hasBackground ?: (selectedTextNode as? CalloutNode)?.hasBackground ?: false
    val bgColor = (selectedTextNode as? TextNode)?.backgroundColorArgb ?: (selectedTextNode as? CalloutNode)?.backgroundColorArgb ?: 0xFFFFFFFF

    val formattingEnabled = selectedTextNode != null

    var fontDropdownExpanded by remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf("ISOCPEUR") }
    var searchQuery by remember { mutableStateOf("") }

    // Состояние для текстового поля ввода размера шрифта
    var sizeInputValue by remember(currentFontSize) { mutableStateOf(currentFontSize.toInt().toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- А: ТЕКСТ ---
        RectangularToolButton(
            label = "Текст",
            isActive = state.currentToolMode == CanvasToolMode.ADD_TEXT,
            onClick = { state.currentToolMode = if (state.currentToolMode == CanvasToolMode.ADD_TEXT) CanvasToolMode.SELECT else CanvasToolMode.ADD_TEXT }
        ) {
            Text(
                text = "АБС",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.offset(x = 0.dp, y = (-6).dp)
            )
        }

        // --- З: ШРИФТ И ПОИСК ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(160.dp)
        ) {
            // Выпадающий список шрифта
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .clickable { fontDropdownExpanded = true },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = selectedFont,
                        fontSize = 13.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                }
                DropdownMenu(
                    expanded = fontDropdownExpanded,
                    onDismissRequest = { fontDropdownExpanded = false },
                    modifier = Modifier.width(160.dp)
                ) {
                    DropdownMenuItem(onClick = { selectedFont = "ISOCPEUR"; fontDropdownExpanded = false }) { Text("ISOCPEUR") }
                    DropdownMenuItem(onClick = { selectedFont = "Standard"; fontDropdownExpanded = false }) { Text("Standard") }
                }
            }

            // Поле поиска текста
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Поиск текста",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }

        // --- В: РАЗМЕР ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Размер текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка "-"
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (formattingEnabled) MaterialTheme.colors.primary else Color.LightGray)
                        .clickable(enabled = formattingEnabled) { state.updateSelectedTextProperties(fontSize = currentFontSize - 2f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }

                // Значение (с вводом с клавиатуры)
                BasicTextField(
                    value = sizeInputValue,
                    onValueChange = { newValue ->
                        sizeInputValue = newValue
                        val newSize = newValue.toFloatOrNull()
                        if (newSize != null && newSize > 0f && formattingEnabled) {
                            state.updateSelectedTextProperties(fontSize = newSize)
                        }
                    },
                    singleLine = true,
                    enabled = formattingEnabled,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .width(40.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            innerTextField()
                        }
                    }
                )

                // Кнопка "+"
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (formattingEnabled) MaterialTheme.colors.primary else Color.LightGray)
                        .clickable(enabled = formattingEnabled) { state.updateSelectedTextProperties(fontSize = currentFontSize + 2f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
        }

        // --- Г: СТИЛИ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "B", isBold = true, isActive = isBold, enabled = formattingEnabled) { state.updateSelectedTextProperties(isBold = !isBold) }
                FormatToggleButton(text = "I", isItalic = true, isActive = isItalic, enabled = formattingEnabled) { state.updateSelectedTextProperties(isItalic = !isItalic) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "U", isUnderline = true, isActive = isUnderline, enabled = formattingEnabled) { state.updateSelectedTextProperties(isUnderline = !isUnderline) }
                FormatToggleButton(text = "S", isStrikethrough = true, isActive = isStrikethrough, enabled = formattingEnabled) { state.updateSelectedTextProperties(isStrikethrough = !isStrikethrough) }
            }
        }

        // --- Д: ВЫРАВНИВАНИЕ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AlignToggleButton(type = 0, isActive = align == 0, enabled = formattingEnabled && isTextSelect) { state.updateSelectedTextProperties(align = 0) }
            AlignToggleButton(type = 1, isActive = align == 1, enabled = formattingEnabled && isTextSelect) { state.updateSelectedTextProperties(align = 1) }
            AlignToggleButton(type = 2, isActive = align == 2, enabled = formattingEnabled && isTextSelect) { state.updateSelectedTextProperties(align = 2) }
        }

        // --- Е: ЦВЕТ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Цвет текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColorButton(0xFFFFFFFF, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFFFFFFF) }
                    ColorButton(0xFF000000, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF000000) }
                    ColorButton(0xFFD32F2F, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFD32F2F) }
                    ColorButton(0xFF1976D2, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF1976D2) }
                    ColorButton(0xFF388E3C, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF388E3C) }
                    ColorButton(0xFF757575, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF757575) }
                    ColorButton(0xFFF57C00, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFF57C00) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColorButton(0xFF7B1FA2, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF7B1FA2) }
                    ColorButton(0xFFFBC02D, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFFBC02D) }
                    ColorButton(0xFF00BCD4, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF00BCD4) }
                    ColorButton(0xFF8D6E63, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF8D6E63) }
                    ColorButton(0xFFE91E63, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFE91E63) }
                    ColorButton(0xFFCDDC39, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFFCDDC39) }
                    ColorButton(0xFF607D8B, currentColor, formattingEnabled) { state.updateSelectedTextProperties(colorArgb = 0xFF607D8B) }
                }
            }
        }

        // --- Ж: ФОН ДЛЯ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Фон для текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BgColorButton(isTransparent = true, color = 0xFFFFFFFF, isActive = !hasBg, enabled = formattingEnabled) {
                    state.updateSelectedTextProperties(hasBackground = false)
                }
                BgColorButton(isTransparent = false, color = 0xFFFFFFFF, isActive = hasBg && bgColor == 0xFFFFFFFF, enabled = formattingEnabled) {
                    state.updateSelectedTextProperties(hasBackground = true, backgroundColorArgb = 0xFFFFFFFF)
                }
                BgColorButton(isTransparent = false, color = 0xFF424242, isActive = hasBg && bgColor == 0xFF424242, enabled = formattingEnabled) {
                    state.updateSelectedTextProperties(hasBackground = true, backgroundColorArgb = 0xFF424242)
                }
            }
        }

        // --- Б, З: ВЫНОСКА ---
        SquareToolButton(
            label = "Выноска",
            isActive = state.currentToolMode == CanvasToolMode.ADD_CALLOUT,
            onClick = { state.currentToolMode = if (state.currentToolMode == CanvasToolMode.ADD_CALLOUT) CanvasToolMode.SELECT else CanvasToolMode.ADD_CALLOUT }
        ) {
            val iconColor = MaterialTheme.colors.onSurface
            Canvas(modifier = Modifier.size(32.dp)) {
                val r = 4.dp.toPx()
                val start = Offset(r + 2f, size.height - r - 2f)
                val end = Offset(size.width, 0f)
                drawCircle(color = iconColor, radius = r, center = start, style = Stroke(2.5f))
                drawLine(color = iconColor, start = start, end = end, strokeWidth = 2.5f)
                drawLine(color = iconColor, start = end, end = Offset(size.width - 10f, 2f), strokeWidth = 2.5f)
                drawLine(color = iconColor, start = end, end = Offset(size.width - 2f, 10f), strokeWidth = 2.5f)
            }
        }
    }
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================

@Composable
private fun RectangularToolButton(label: String, isActive: Boolean, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 110.dp, height = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { iconContent() }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colors.onSurface)
    }
}

@Composable
private fun SquareToolButton(label: String, isActive: Boolean, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { iconContent() }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colors.onSurface)
    }
}

@Composable
private fun FormatToggleButton(
    text: String,
    isBold: Boolean = false,
    isItalic: Boolean = false,
    isUnderline: Boolean = false,
    isStrikethrough: Boolean = false,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (isUnderline) TextDecoration.Underline else if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None,
            color = if (enabled) MaterialTheme.colors.onSurface else Color.LightGray,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AlignToggleButton(type: Int, isActive: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val color = if (enabled) Color.Black else Color.LightGray
            val stroke = 1.5f
            val y1 = size.height * 0.2f
            val y2 = size.height * 0.5f
            val y3 = size.height * 0.8f

            when (type) {
                0 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(0f, y2), Offset(size.width * 0.6f, y2), strokeWidth = stroke)
                    drawLine(color, Offset(0f, y3), Offset(size.width * 0.8f, y3), strokeWidth = stroke)
                }
                1 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.2f, y2), Offset(size.width * 0.8f, y2), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.1f, y3), Offset(size.width * 0.9f, y3), strokeWidth = stroke)
                }
                2 -> {
                    drawLine(color, Offset(0f, y1), Offset(size.width, y1), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.4f, y2), Offset(size.width, y2), strokeWidth = stroke)
                    drawLine(color, Offset(size.width * 0.2f, y3), Offset(size.width, y3), strokeWidth = stroke)
                }
            }
        }
    }
}

@Composable
private fun ColorButton(colorArgb: Long, currentColor: Long, enabled: Boolean, onClick: () -> Unit) {
    val isActive = colorArgb == currentColor && enabled
    val color = Color(colorArgb)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (enabled) color else color.copy(alpha = 0.3f))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun BgColorButton(isTransparent: Boolean, color: Long, isActive: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bgColor = Color(color)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isTransparent) Color.White else if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isTransparent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red.copy(alpha = if (enabled) 0.6f else 0.3f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
        }
    }
}