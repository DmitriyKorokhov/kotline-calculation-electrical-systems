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
import core.view.CompactOutlinedTextField
import feature.projecteditor.domain.CalloutNode
import feature.projecteditor.domain.TextNode
import feature.projecteditor.state.CanvasToolMode
import feature.projecteditor.state.ProjectCanvasState

@Composable
fun AnnotationsPalette(state: ProjectCanvasState) {
    val selectedNodes = state.nodes.filter { it.id in state.selectedNodeIds }
    val selectedTextNode = selectedNodes.firstOrNull { it is TextNode || it is CalloutNode }

    // Читаем из выделенной модели ИЛИ из настроек по умолчанию (если ничего не выделено)
    val displayFontSize = (selectedTextNode as? TextNode)?.fontSize ?: (selectedTextNode as? CalloutNode)?.fontSize ?: state.defaultFontSize
    val displayIsBold = (selectedTextNode as? TextNode)?.isBold ?: (selectedTextNode as? CalloutNode)?.isBold ?: state.defaultIsBold
    val displayIsItalic = (selectedTextNode as? TextNode)?.isItalic ?: (selectedTextNode as? CalloutNode)?.isItalic ?: state.defaultIsItalic
    val displayIsUnderline = (selectedTextNode as? TextNode)?.isUnderline ?: (selectedTextNode as? CalloutNode)?.isUnderline ?: state.defaultIsUnderline
    val displayIsStrikethrough = (selectedTextNode as? TextNode)?.isStrikethrough ?: (selectedTextNode as? CalloutNode)?.isStrikethrough ?: state.defaultIsStrikethrough
    val displayAlign = (selectedTextNode as? TextNode)?.align ?: state.defaultAlign
    val displayColor = (selectedTextNode as? TextNode)?.colorArgb ?: (selectedTextNode as? CalloutNode)?.colorArgb ?: state.defaultColorArgb
    val displayHasBg = (selectedTextNode as? TextNode)?.hasBackground ?: (selectedTextNode as? CalloutNode)?.hasBackground ?: state.defaultHasBackground
    val displayBgColor = (selectedTextNode as? TextNode)?.backgroundColorArgb ?: (selectedTextNode as? CalloutNode)?.backgroundColorArgb ?: state.defaultBackgroundColorArgb

    var fontDropdownExpanded by remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf("ISOCPEUR") }
    var searchQuery by remember { mutableStateOf("") }

    var sizeInputValue by remember(displayFontSize) { mutableStateOf(displayFontSize.toInt().toString()) }

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
                color = Color.Black, // Иконка черная
                modifier = Modifier.offset(x = 0.dp, y = (-6).dp)
            )
        }

        // --- З: ШРИФТ И ПОИСК ---
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(160.dp)
        ) {
            // Выпадающий список шрифта
            Box(modifier = Modifier.fillMaxWidth()) {
                CompactOutlinedTextField(
                    label = "",
                    value = selectedFont,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    textColor = Color.Black, // Текст шрифта черный
                    focusedBorderColor = Color.LightGray,
                    unfocusedBorderColor = Color.LightGray,
                    fontSizeSp = 13,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black) }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { fontDropdownExpanded = true }
                )
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
            // Поле поиска текста (без анимации, с плейсхолдером)
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = Color.Black // Вводимый текст - черный
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp)
                    ) {
                        // Увеличенная черная лупа
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Плейсхолдер показывается только если поле пустое
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Поиск текста",
                                    color = Color.DarkGray, // Темно-серый, хорошо виден на белом
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
                        .background(MaterialTheme.colors.primary)
                        .clickable {
                            val newSize = maxOf(2f, displayFontSize - 2f)
                            state.defaultFontSize = newSize
                            if (selectedTextNode != null) state.updateSelectedTextProperties(fontSize = newSize)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }

                // Значение
                BasicTextField(
                    value = sizeInputValue,
                    onValueChange = { newValue ->
                        sizeInputValue = newValue
                        val newSize = newValue.toFloatOrNull()
                        if (newSize != null && newSize > 0f) {
                            state.defaultFontSize = newSize
                            if (selectedTextNode != null) state.updateSelectedTextProperties(fontSize = newSize)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, // Цифры размера черные
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .width(40.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { innerTextField() }
                    }
                )

                // Кнопка "+"
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.primary)
                        .clickable {
                            val newSize = displayFontSize + 2f
                            state.defaultFontSize = newSize
                            if (selectedTextNode != null) state.updateSelectedTextProperties(fontSize = newSize)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
        }

        // --- Г: СТИЛИ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "B", isBold = true, isActive = displayIsBold) {
                    state.defaultIsBold = !displayIsBold
                    if (selectedTextNode != null) state.updateSelectedTextProperties(isBold = state.defaultIsBold)
                }
                FormatToggleButton(text = "I", isItalic = true, isActive = displayIsItalic) {
                    state.defaultIsItalic = !displayIsItalic
                    if (selectedTextNode != null) state.updateSelectedTextProperties(isItalic = state.defaultIsItalic)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FormatToggleButton(text = "U", isUnderline = true, isActive = displayIsUnderline) {
                    state.defaultIsUnderline = !displayIsUnderline
                    if (selectedTextNode != null) state.updateSelectedTextProperties(isUnderline = state.defaultIsUnderline)
                }
                FormatToggleButton(text = "S", isStrikethrough = true, isActive = displayIsStrikethrough) {
                    state.defaultIsStrikethrough = !displayIsStrikethrough
                    if (selectedTextNode != null) state.updateSelectedTextProperties(isStrikethrough = state.defaultIsStrikethrough)
                }
            }
        }

        // --- Д: ВЫРАВНИВАНИЕ ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AlignToggleButton(type = 0, isActive = displayAlign == 0) {
                state.defaultAlign = 0
                if (selectedTextNode != null) state.updateSelectedTextProperties(align = 0)
            }
            AlignToggleButton(type = 1, isActive = displayAlign == 1) {
                state.defaultAlign = 1
                if (selectedTextNode != null) state.updateSelectedTextProperties(align = 1)
            }
            AlignToggleButton(type = 2, isActive = displayAlign == 2) {
                state.defaultAlign = 2
                if (selectedTextNode != null) state.updateSelectedTextProperties(align = 2)
            }
        }

        // --- Е: ЦВЕТ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Цвет текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors1 = listOf(0xFFFFFFFF, 0xFF000000, 0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFF757575, 0xFFF57C00)
                    colors1.forEach { colorVal ->
                        ColorButton(colorVal, displayColor) {
                            state.defaultColorArgb = colorVal
                            if (selectedTextNode != null) state.updateSelectedTextProperties(colorArgb = colorVal)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors2 = listOf(0xFF7B1FA2, 0xFFFBC02D, 0xFF00BCD4, 0xFF8D6E63, 0xFFE91E63, 0xFFCDDC39, 0xFF607D8B)
                    colors2.forEach { colorVal ->
                        ColorButton(colorVal, displayColor) {
                            state.defaultColorArgb = colorVal
                            if (selectedTextNode != null) state.updateSelectedTextProperties(colorArgb = colorVal)
                        }
                    }
                }
            }
        }

        // --- Ж: ФОН ДЛЯ ТЕКСТА ---
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Фон для текста", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BgColorButton(isTransparent = true, color = 0xFFFFFFFF, isActive = !displayHasBg) {
                    state.defaultHasBackground = false
                    if (selectedTextNode != null) state.updateSelectedTextProperties(hasBackground = false)
                }
                BgColorButton(isTransparent = false, color = 0xFFFFFFFF, isActive = displayHasBg && displayBgColor == 0xFFFFFFFF) {
                    state.defaultHasBackground = true
                    state.defaultBackgroundColorArgb = 0xFFFFFFFF
                    if (selectedTextNode != null) state.updateSelectedTextProperties(hasBackground = true, backgroundColorArgb = 0xFFFFFFFF)
                }
                BgColorButton(isTransparent = false, color = 0xFF424242, isActive = displayHasBg && displayBgColor == 0xFF424242) {
                    state.defaultHasBackground = true
                    state.defaultBackgroundColorArgb = 0xFF424242
                    if (selectedTextNode != null) state.updateSelectedTextProperties(hasBackground = true, backgroundColorArgb = 0xFF424242)
                }
            }
        }

        // --- Б, З: ВЫНОСКА ---
        SquareToolButton(
            label = "Выноска",
            isActive = state.currentToolMode == CanvasToolMode.ADD_CALLOUT,
            onClick = { state.currentToolMode = if (state.currentToolMode == CanvasToolMode.ADD_CALLOUT) CanvasToolMode.SELECT else CanvasToolMode.ADD_CALLOUT }
        ) {
            val iconColor = Color.Black // Иконка черная
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
    val bgColor = if (isActive) Color(0xFF81D4FA) else Color(0xFFE3F2FD) // Голубой фон
    Column(
        modifier = Modifier
            .size(width = 110.dp, height = 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
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
        Text(label, fontSize = 12.sp, color = Color.Black) // Текст черный
    }
}

@Composable
private fun SquareToolButton(label: String, isActive: Boolean, onClick: () -> Unit, iconContent: @Composable () -> Unit) {
    val bgColor = if (isActive) Color(0xFF81D4FA) else Color(0xFFE3F2FD) // Голубой фон
    Column(
        modifier = Modifier
            .size(width = 80.dp, height = 92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
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
        Text(label, fontSize = 12.sp, color = Color.Black) // Текст черный
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (isUnderline) TextDecoration.Underline else if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None,
            color = MaterialTheme.colors.onSurface,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AlignToggleButton(type: Int, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) MaterialTheme.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val color = Color.Black
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
private fun ColorButton(colorArgb: Long, currentColor: Long, onClick: () -> Unit) {
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

@Composable
private fun BgColorButton(isTransparent: Boolean, color: Long, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = Color(color)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isTransparent) Color.White else bgColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) MaterialTheme.colors.primary else Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isTransparent) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Red.copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
        }
    }
}